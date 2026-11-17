package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.model.PasswordResetTokenEntity;
import com.devlil0.tramposeditor.database.repository.IPasswordResetTokenRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.notification.EmailContent;
import com.devlil0.tramposeditor.notification.EmailSender;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Esqueci a senha (ESBOCO §7 item 6).
 * <p>
 * O que estes testes protegem é o princípio do serviço: o e-mail é a única prova de posse
 * que existe, então pedir redefinição não pode virar uma forma de descobrir quais contas
 * existem, e o link não pode servir duas vezes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PasswordResetTest {

    private static final Pattern TOKEN_NO_LINK = Pattern.compile("redefinir-senha\\?token=(\\S+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IPasswordResetTokenRepository tokenRepository;

    @Autowired
    private IUserRepository userRepository;

    @MockitoBean
    private EmailSender emailSender;

    @BeforeEach
    void limpar(){
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        reset(emailSender);
    }

    @Test
    void pedirParaEmailDesconhecidoRespondeIgualENaoMandaNada() throws Exception {
        pedirReset("nao-existe@teste.com").andExpect(status().isNoContent());

        // Nenhum e-mail, e — o que importa — nenhuma resposta diferente da do e-mail
        // que existe. Enumerar contas cadastradas seria de graça.
        verify(emailSender, never()).send(any(), any());
    }

    @Test
    void linkRedefineASenhaEValeUmaVezSo() throws Exception {
        registrar("dono@teste.com");
        String oldJwt = jwtDe(entrar("dono@teste.com", "senhaforte123"));
        consultarConta(oldJwt).andExpect(status().isOk());

        pedirReset("dono@teste.com").andExpect(status().isNoContent());
        String token = capturarTokenDoEmail();

        // A senha nova entra...
        confirmar(token, "senhanova12345").andExpect(status().isNoContent());
        consultarConta(oldJwt).andExpect(status().isUnauthorized());
        String newJwt = jwtDe(entrar("dono@teste.com", "senhanova12345"));
        consultarConta(newJwt).andExpect(status().isOk());

        // ...e a antiga morre junto.
        entrar("dono@teste.com", "senhaforte123").andExpect(status().isUnauthorized());

        // O mesmo link não serve de novo.
        confirmar(token, "outrasenha12345").andExpect(status().isConflict());
    }

    @Test
    void linkEmitidoAntesDaDesativacaoNaoAlteraSenha() throws Exception {
        registrar("dono@teste.com");
        pedirReset("dono@teste.com").andExpect(status().isNoContent());
        String token = capturarTokenDoEmail();
        var user = userRepository.findByEmail("dono@teste.com").orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        confirmar(token, "senhanova12345").andExpect(status().isConflict());
        var current = userRepository.findById(user.getId()).orElseThrow();
        assertThat(current.getPasswordHash()).isEqualTo(user.getPasswordHash());
        assertThat(current.getSessionVersion()).isEqualTo(user.getSessionVersion());
    }

    private String jwtDe(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return JsonPath.read(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "$.accessToken");
    }

    private org.springframework.test.web.servlet.ResultActions consultarConta(String jwt) throws Exception {
        return mockMvc.perform(get("/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt));
    }

    @Test
    void pedirDeNovoDerrubaOLinkAnterior() throws Exception {
        registrar("dono@teste.com");

        pedirReset("dono@teste.com").andExpect(status().isNoContent());
        String primeiro = capturarTokenDoEmail();

        reset(emailSender);
        pedirReset("dono@teste.com").andExpect(status().isNoContent());
        String segundo = capturarTokenDoEmail();

        assertThat(segundo).isNotEqualTo(primeiro);
        confirmar(primeiro, "senhanova12345").andExpect(status().isConflict());
        confirmar(segundo, "senhanova12345").andExpect(status().isNoContent());
    }

    @Test
    void linkExpiradoNaoServe() throws Exception {
        registrar("dono@teste.com");
        pedirReset("dono@teste.com").andExpect(status().isNoContent());
        String token = capturarTokenDoEmail();

        // Envelhece o token na marra: uma hora e um minuto atrás.
        PasswordResetTokenEntity entity = tokenRepository.findAll().get(0);
        entity.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        tokenRepository.save(entity);

        confirmar(token, "senhanova12345").andExpect(status().isConflict());
        entrar("dono@teste.com", "senhaforte123").andExpect(status().isOk());
    }

    @Test
    void senhaCurtaNaoPassaPelaPortaDosFundos() throws Exception {
        registrar("dono@teste.com");
        pedirReset("dono@teste.com").andExpect(status().isNoContent());
        String token = capturarTokenDoEmail();

        // O cadastro exige 8 caracteres; redefinir não pode aceitar menos.
        confirmar(token, "curta").andExpect(status().isBadRequest());
    }

    // ---------- Helpers ----------

    private String capturarTokenDoEmail(){
        ArgumentCaptor<EmailContent> enviado = ArgumentCaptor.forClass(EmailContent.class);
        verify(emailSender).send(eq("dono@teste.com"), enviado.capture());

        // Casa o token no texto puro, e não só na URL: é ele que o cliente sem HTML
        // recebe, e um token que só existisse no markup deixaria essa pessoa presa.
        Matcher matcher = TOKEN_NO_LINK.matcher(enviado.getValue().toPlainText());
        assertThat(matcher.find()).as("o e-mail precisa carregar o link com o token").isTrue();
        return matcher.group(1);
    }

    private org.springframework.test.web.servlet.ResultActions pedirReset(String email) throws Exception {
        return mockMvc.perform(post("/v1/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s"}
                        """.formatted(email)));
    }

    private org.springframework.test.web.servlet.ResultActions confirmar(String token, String senha) throws Exception {
        return mockMvc.perform(post("/v1/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token": "%s", "password": "%s"}
                        """.formatted(token, senha)));
    }

    private org.springframework.test.web.servlet.ResultActions entrar(String email, String senha) throws Exception {
        return mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, senha)));
    }

    private void registrar(String email) throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "senhaforte123",
                                  "displayName": "Dono da Conta",
                                  "username": "u-%s",
                                  "roles": ["CLIENT"]
                                }
                                """.formatted(email, email.split("@")[0])))
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

        // O cadastro dispara o e-mail de confirmação de endereço (V17). Zerar aqui deixa o
        // `capturarTokenDoEmail` abaixo pegar o e-mail de SENHA, e não o do setup — os dois
        // vão pro mesmo endereço e carregam um `token=` cada.
        reset(emailSender);
    }
}
