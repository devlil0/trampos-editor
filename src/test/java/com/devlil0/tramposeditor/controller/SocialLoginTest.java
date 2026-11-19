package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.repository.IAuthExchangeCodeRepository;
import com.devlil0.tramposeditor.database.repository.IUserIdentityRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.exception.SocialLoginFailure;
import com.devlil0.tramposeditor.exception.SocialLoginFailure.Reason;
import com.devlil0.tramposeditor.service.SocialLoginService;
import com.devlil0.tramposeditor.service.BrowserLoginAttempt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private static final String PROOF = "a".repeat(64);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SocialLoginService socialLoginService;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IUserIdentityRepository identityRepository;

    @Autowired
    private IAuthExchangeCodeRepository codeRepository;

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

        String code = completar("google", "sub-123", "mesma@teste.com", "Maria G.", "teste.com");

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
        completar("github", "gh-42", "antigo@teste.com", "Bruno", null);
        assertThat(userRepository.count()).isEqualTo(1);

        // A pessoa trocou de e-mail no GitHub. O `subject` é o que segura o vínculo.
        String code = completar("github", "gh-42", null, "Bruno", null);
        trocar(code).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("antigo@teste.com"));

        assertThat(userRepository.count())
                .as("o subject do provedor manda, não o e-mail — senão trocar de e-mail perderia a conta")
                .isEqualTo(1);
        assertThat(userRepository.findByEmail("antigo@teste.com")).isPresent();
    }

    @Test
    void mesmoEmailEmDoisProvedoresContinuaSendoUmaContaSo() throws Exception {
        completar("google", "sub-1", "pessoa@teste.com", "Pessoa", "teste.com");
        completar("github", "gh-1", "pessoa@teste.com", "Pessoa", null);

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(identityRepository.count())
                .as("dois vínculos, uma conta")
                .isEqualTo(2);
    }

    @Test
    void codigoDeLoginServeUmaVezSo() throws Exception {
        String code = completar("google", "sub-9", "unica@teste.com", "Uma Vez", "teste.com");

        trocar(code).andExpect(status().isOk());
        trocar(code).andExpect(status().isUnauthorized());
    }

    @Test
    void codigoInventadoNaoAbreSessao() throws Exception {
        trocar("codigo-que-nunca-existiu").andExpect(status().isUnauthorized());
    }

    @Test
    void codigoExpiradoNaoAbreSessao() throws Exception {
        String code = completar("github", "gh-exp", "exp@teste.com", "Teste", null);
        var entity = codeRepository.findAll().getFirst();
        entity.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        codeRepository.save(entity);
        trocar(code).andExpect(status().isUnauthorized());
    }

    @Test
    void codigoSozinhoOuComProvaErradaNaoCriaSessaoNemConsomeCodigo() throws Exception {
        String code = completar("github", "gh-bound", "bound@teste.com", "Teste", null);
        mockMvc.perform(post("/v1/auth/social/exchange").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s"}
                                """.formatted(code)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
        trocar(code, "invalida").andExpect(status().isBadRequest());
        trocar(code, "b".repeat(64)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
        trocar(code).andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
        trocar(code).andExpect(status().isUnauthorized());
    }

    @Test
    void tentativaVencidaNaoEmiteCodigoETrocaRespeitaPrazoDaTentativa() throws Exception {
        String challenge = BrowserLoginAttempt.challengeOf(PROOF);
        assertThatThrownBy(() -> socialLoginService.completeLogin("github", "gh-exp-attempt",
                "attempt@teste.com", "Teste", null, new BrowserLoginAttempt(challenge, LocalDateTime.now().minusSeconds(1))))
                .isInstanceOfSatisfying(SocialLoginFailure.class, e -> assertThat(e.reason()).isEqualTo(Reason.EXPIRED));
        assertThat(userRepository.count()).isZero();
        assertThat(codeRepository.count()).isZero();

        LocalDateTime deadline = LocalDateTime.now().plusSeconds(30).withNano(0);
        String code = socialLoginService.completeLogin("github", "gh-exp-attempt",
                "attempt@teste.com", "Teste", null, new BrowserLoginAttempt(challenge, deadline));
        var saved = codeRepository.findAll().getFirst();
        assertThat(saved.getExpiresAt()).isEqualTo(deadline);
        saved.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        codeRepository.save(saved);
        trocar(code).andExpect(status().isUnauthorized());
    }

    @Test
    void somenteUmaTrocaSimultaneaAbreSessao() throws Exception {
        String code = completar("github", "gh-race", "race@teste.com", "Teste", null);
        var start = new CyclicBarrier(2);
        Callable<Integer> exchange = () -> {
            start.await(10, TimeUnit.SECONDS);
            var response = trocar(code).andReturn().getResponse();
            if (response.getStatus() == 200) {
                assertThat(com.jayway.jsonpath.JsonPath.<String>read(response.getContentAsString(), "$.accessToken"))
                        .isNotBlank();
            } else {
                assertThat(response.getContentAsString()).doesNotContain("accessToken");
            }
            return response.getStatus();
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(exchange);
            var second = pool.submit(exchange);
            assertThat(java.util.List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 401);
        }
    }

    @Test
    void emailDeTerceiroNoGoogleNaoVinculaContaExistente() throws Exception {
        registrarComSenha("terceiro@teste.com", "Conta Existente");
        assertThatThrownBy(() -> completar(
                "google", "sub-terceiro", "terceiro@teste.com", "Perfil Google", null))
                .isInstanceOfSatisfying(SocialLoginFailure.class, e -> assertThat(e.reason()).isEqualTo(Reason.LINK_NOT_ALLOWED));
        assertThat(identityRepository.count()).isZero();
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void emailDeTerceiroNoGoogleNaoCriaContaNova() {
        assertThatThrownBy(() -> completar(
                "google", "sub-terceiro", "terceiro@teste.com", "Perfil Google", null))
                .isInstanceOfSatisfying(SocialLoginFailure.class, e -> assertThat(e.reason()).isEqualTo(Reason.LINK_NOT_ALLOWED));

        assertThat(userRepository.count()).isZero();
        assertThat(identityRepository.count()).isZero();
        assertThat(codeRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"google", "github"})
    void emailVerificadoNoProvedorNaoVinculaContaLegadaSemConfirmacao(String provider) throws Exception {
        String email = "legada@teste.com";
        registrarComSenha(email, "Conta Legada");
        var user = userRepository.findByEmail(email).orElseThrow();
        user.setEmailVerifiedAt(null);
        userRepository.saveAndFlush(user);

        String hostedDomain = "google".equals(provider) ? "teste.com" : null;
        assertThatThrownBy(() -> completar(provider, "id-novo", email, "Perfil Provedor", hostedDomain))
                .isInstanceOfSatisfying(SocialLoginFailure.class, e -> assertThat(e.reason()).isEqualTo(Reason.LINK_NOT_ALLOWED));

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.findByEmail(email).orElseThrow().isEmailVerified()).isFalse();
        assertThat(identityRepository.count()).isZero();
        assertThat(codeRepository.count()).isZero();
    }

    @Test
    void gmailVerificadoCriaContaEIdJaVinculadoContinuaValidoComEmailDeTerceiro() throws Exception {
        String email = "pessoa@gmail.com";
        String code = completar("google", "sub-gmail", email, "Perfil Google", null);
        trocar(code).andExpect(status().isOk()).andExpect(jsonPath("$.user.email").value(email));
        assertThat(userRepository.findByEmail(email).orElseThrow().isEmailVerified()).isTrue();

        String codeDepoisDaMudanca = completar("google", "sub-gmail", "novo@teste.com", "Perfil Google", null);
        trocar(codeDepoisDaMudanca).andExpect(status().isOk()).andExpect(jsonPath("$.user.email").value(email));
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(identityRepository.count()).isEqualTo(1);
    }

    @Test
    void semProvaNaoVinculaContaExistente() throws Exception {
        registrarComSenha("existente@teste.com", "Conta Existente");
        assertThatThrownBy(() -> completar("github", "gh-novo", null, "Perfil GitHub", null))
                .isInstanceOfSatisfying(SocialLoginFailure.class, e -> assertThat(e.reason()).isEqualTo(Reason.EMAIL_UNVERIFIED));
        assertThat(identityRepository.count()).isZero();
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void contaDesativadaJaVinculadaNaoRecebeCodigoNemTrocaCodigoAnterior() throws Exception {
        String email = "inativa@teste.com";
        String code = completar("google", "sub-inativo", email, "Conta Inativa", "teste.com");
        var user = userRepository.findByEmail(email).orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        assertThatThrownBy(() -> completar("google", "sub-inativo", email, "Conta Inativa", "teste.com"))
                .isInstanceOfSatisfying(SocialLoginFailure.class, e -> assertThat(e.reason()).isEqualTo(Reason.ACCOUNT_INACTIVE));
        trocar(code).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void provedorSemEmailNaoCriaConta(){
        // GitHub com e-mail privado chega aqui sem e-mail. Criar conta assim produziria
        // exatamente a duplicata que a conta única existe pra evitar.
        assertThatThrownBy(() -> completar("github", "gh-7", null, "Sem Email", null))
                .isInstanceOfSatisfying(SocialLoginFailure.class, e -> assertThat(e.reason()).isEqualTo(Reason.EMAIL_UNVERIFIED));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void contaCriadaPeloProvedorNaoEntraPorSenha() throws Exception {
        completar("google", "sub-5", "sosocial@teste.com", "Só Social", "teste.com");

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

    private String completar(String provider, String subject, String email, String name, String hostedDomain) {
        return socialLoginService.completeLogin(provider, subject, email, name, hostedDomain,
                new BrowserLoginAttempt(BrowserLoginAttempt.challengeOf(PROOF), LocalDateTime.now().plusMinutes(5)));
    }

    private org.springframework.test.web.servlet.ResultActions trocar(String code) throws Exception {
        return trocar(code, PROOF);
    }

    private org.springframework.test.web.servlet.ResultActions trocar(String code, String proof) throws Exception {
        return mockMvc.perform(post("/v1/auth/social/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code": "%s", "browserProof": "%s"}
                        """.formatted(code, proof)));
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
