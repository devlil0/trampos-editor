package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.repository.IUserIdentityRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.service.SocialLoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login com Google e GitHub.
 * <p>
 * O vaivém com o provedor não dá pra exercitar sem credencial de verdade — mas o que
 * importa aqui não é o handshake, é o que acontece <b>depois</b> dele: a conta única do
 * {@code FUNDACAO} §11.9 e o código de uso único que fecha o login.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SocialLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SocialLoginService socialLoginService;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IUserIdentityRepository identityRepository;

    @BeforeEach
    void limpar(){
        identityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void semCredencialConfiguradaNaoHaProvedorNenhum() throws Exception {
        // O ambiente de teste não tem client-id. A lista vazia é o que faz o frontend
        // não desenhar botão que levaria a erro.
        mockMvc.perform(get("/v1/auth/social/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void entrarPeloGoogleNumEmailQueJaTemContaVinculaEmVezDeDuplicar() throws Exception {
        registrarComSenha("mesma@teste.com", "Maria da Conta");
        assertThat(userRepository.count()).isEqualTo(1);

        String code = socialLoginService.completeLogin("google", "sub-123", "mesma@teste.com", "Maria G.");

        // A regra do FUNDACAO §11.9: mesmo e-mail é a mesma pessoa.
        assertThat(userRepository.count())
                .as("entrar pelo Google não pode criar uma segunda conta com o mesmo e-mail")
                .isEqualTo(1);

        // E a sessão que sai disso é a da conta que já existia, com o nome dela.
        trocar(code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("mesma@teste.com"))
                .andExpect(jsonPath("$.user.displayName").value("Maria da Conta"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void mesmaIdentidadeVoltaNaMesmaContaMesmoComEmailNovo() throws Exception {
        socialLoginService.completeLogin("github", "gh-42", "antigo@teste.com", "Bruno");
        assertThat(userRepository.count()).isEqualTo(1);

        // A pessoa trocou de e-mail no GitHub. O `subject` é o que segura o vínculo.
        socialLoginService.completeLogin("github", "gh-42", "novo@teste.com", "Bruno");

        assertThat(userRepository.count())
                .as("o subject do provedor manda, não o e-mail — senão trocar de e-mail perderia a conta")
                .isEqualTo(1);
        assertThat(userRepository.findByEmail("antigo@teste.com")).isPresent();
    }

    @Test
    void mesmoEmailEmDoisProvedoresContinuaSendoUmaContaSo() throws Exception {
        socialLoginService.completeLogin("google", "sub-1", "pessoa@teste.com", "Pessoa");
        socialLoginService.completeLogin("github", "gh-1", "pessoa@teste.com", "Pessoa");

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(identityRepository.count())
                .as("dois vínculos, uma conta")
                .isEqualTo(2);
    }

    @Test
    void codigoDeLoginServeUmaVezSo() throws Exception {
        String code = socialLoginService.completeLogin("google", "sub-9", "unica@teste.com", "Uma Vez");

        trocar(code).andExpect(status().isOk());
        trocar(code).andExpect(status().isUnauthorized());
    }

    @Test
    void codigoInventadoNaoAbreSessao() throws Exception {
        trocar("codigo-que-nunca-existiu").andExpect(status().isUnauthorized());
    }

    @Test
    void provedorSemEmailNaoCriaConta(){
        // GitHub com e-mail privado chega aqui sem e-mail. Criar conta assim produziria
        // exatamente a duplicata que a conta única existe pra evitar.
        assertThatThrownBy(() -> socialLoginService.completeLogin("github", "gh-7", null, "Sem Email"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("e-mail");

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void contaCriadaPeloProvedorNaoEntraPorSenha() throws Exception {
        socialLoginService.completeLogin("google", "sub-5", "sosocial@teste.com", "Só Social");

        // Sem senha definida, e a resposta é a mesma de credencial errada: dizer "essa
        // conta usa Google" contaria a um estranho como a pessoa entra.
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "sosocial@teste.com", "password": "chutando12345"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Helpers ----------

    private org.springframework.test.web.servlet.ResultActions trocar(String code) throws Exception {
        return mockMvc.perform(post("/v1/auth/social/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code": "%s"}
                        """.formatted(code)));
    }

    private void registrarComSenha(String email, String displayName) throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "senhaforte123",
                                  "displayName": "%s",
                                  "username": "u-%s",
                                  "roles": ["CLIENT"]
                                }
                                """.formatted(email, displayName, email.split("@")[0])))
                .andExpect(status().isAccepted());

        // O cadastro virou dois passos (V18): sem esta confirmação a conta não existe.
        mockMvc.perform(post("/v1/auth/register/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "code": "%s"
                                }
                                """.formatted(email, TestcontainersConfiguration.CODIGO_DE_CADASTRO)))
                .andExpect(status().isCreated());
    }
}
