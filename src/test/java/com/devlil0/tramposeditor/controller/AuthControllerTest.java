package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.repository.IPendingSignupRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.service.JwtService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.crypto.KeyGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IPendingSignupRepository pendingSignupRepository;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clean(){
        userRepository.deleteAll();
    }

    @AfterEach
    void limparPendencias() {
        pendingSignupRepository.deleteAll();
    }

    /**
     * O passo 1 não cria nada — e é isso que a feature de 2026-08-06 comprou.
     * <p>
     * Enquanto o código não voltar não há conta, não há sessão e não há linha em
     * {@code users}. Um bot que só sabe mandar POST para exatamente aqui.
     */
    @Test
    void cadastroNaoCriaContaAntesDoCodigo() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "Editor@Teste.com",
                                  "password": "senhaforte123",
                                  "displayName": "Editor Teste",
                                  "username": "EditorTeste",
                                  "roles": ["EDITOR"]
                                }
                                """))
                // 202, não 201: aceito pra processar, nada criado.
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.email").value("editor@teste.com"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                // A ausência é o teste: nenhuma sessão sai daqui.
                .andExpect(jsonPath("$.accessToken").doesNotExist());

        assertThat(userRepository.findByEmail("editor@teste.com")).isEmpty();
    }

    @Test
    void codigoCorretoCriaAContaComEmailJaVerificado() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "Editor@Teste.com",
                                  "password": "senhaforte123",
                                  "displayName": "Editor Teste",
                                  "username": "EditorTeste",
                                  "roles": ["EDITOR"]
                                }
                                """))
                .andExpect(status().isAccepted());

        confirmar("Editor@Teste.com", TestcontainersConfiguration.CODIGO_DE_CADASTRO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                // e-mail normalizado pra minúsculo no cadastro
                .andExpect(jsonPath("$.user.email").value("editor@teste.com"))
                // username também: a forma canônica é minúscula, e é ela que vira o endereço
                .andExpect(jsonPath("$.user.username").value("editorteste"))
                // cadastro por senha escolhe o nome no formulário — nasce escolhido
                .andExpect(jsonPath("$.user.usernameChosen").value(true))
                .andExpect(jsonPath("$.user.roles[0]").value("EDITOR"))
                .andExpect(jsonPath("$.user.createdAt").isNotEmpty());

        // O código É a prova de posse: a conta não precisa do link da V17 depois disso.
        assertThat(userRepository.findByEmail("editor@teste.com").orElseThrow().isEmailVerified())
                .isTrue();
    }

    @Test
    void codigoErradoNaoCriaConta() throws Exception {
        pedirCadastro("chute@teste.com");

        confirmar("chute@teste.com", "999999")
                .andExpect(status().isConflict());

        assertThat(userRepository.findByEmail("chute@teste.com")).isEmpty();
    }

    /**
     * Seis dígitos são um milhão de combinações — pouco, se der pra chutar à vontade. O que
     * fecha essa porta é o contador, não o tamanho do número.
     */
    @Test
    void cadastroMorreDepoisDeCincoChutes() throws Exception {
        pedirCadastro("bruto@teste.com");

        for (short attempt = 1; attempt <= 4; attempt++) {
            confirmar("bruto@teste.com", "111111").andExpect(status().isConflict());
            assertThat(pendingSignupRepository.findByEmail("bruto@teste.com").orElseThrow().getAttempts())
                    .isEqualTo(attempt);
        }
        confirmar("bruto@teste.com", "111111").andExpect(status().isConflict());
        assertThat(pendingSignupRepository.findByEmail("bruto@teste.com")).isEmpty();

        // Na quinta o pendente foi descartado — nem o código certo vale mais.
        confirmar("bruto@teste.com", TestcontainersConfiguration.CODIGO_DE_CADASTRO)
                .andExpect(status().isConflict());

        assertThat(userRepository.findByEmail("bruto@teste.com")).isEmpty();
    }

    /** Confirmar duas vezes não cria duas contas nem devolve uma segunda sessão. */
    @Test
    void codigoValeUmaVezSo() throws Exception {
        pedirCadastro("unico@teste.com");

        confirmar("unico@teste.com", TestcontainersConfiguration.CODIGO_DE_CADASTRO)
                .andExpect(status().isCreated());

        confirmar("unico@teste.com", TestcontainersConfiguration.CODIGO_DE_CADASTRO)
                .andExpect(status().isConflict());
    }

    @Test
    void naoDevePermitirEmailDuplicado() throws Exception {
        registrar("dup@teste.com", "senhaforte123", "EDITOR");

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "dup@teste.com",
                                  "password": "outrasenha123",
                                  "displayName": "Outro",
                                  "username": "outro-qualquer",
                                  "roles": ["CLIENT"]
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void naoDevePermitirAutoAtribuicaoDeAdmin() throws Exception {
        String token = registrar("admin@teste.com", "senhaforte123", "ADMIN", "CLIENT");

        // ADMIN foi filtrado no cadastro — sobrou apenas CLIENT.
        mockMvc.perform(get("/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("CLIENT"));
    }

    @Test
    void deveRejeitarSenhaErradaNoLogin() throws Exception {
        registrar("login@teste.com", "senhaforte123", "CLIENT");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "login@teste.com", "password": "senhaerrada123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveLogarERetornarMe() throws Exception {
        registrar("me@teste.com", "senhaforte123", "EDITOR");

        String body = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "me@teste.com", "password": "senhaforte123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(body, "$.accessToken");

        mockMvc.perform(get("/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@teste.com"))
                .andExpect(jsonPath("$.displayName").value("Fulano"));
    }

    @Test
    void deveBloquearMeSemToken() throws Exception {
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void contaDesativadaNaoConsegueConsultarOuEditarComTokenAnterior() throws Exception {
        String email = "inativo@teste.com";
        String bearer = "Bearer " + registrar(email, "senhaforte123", "CLIENT");
        mockMvc.perform(get("/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());
        var user = userRepository.findByEmail(email).orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        mockMvc.perform(get("/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"instagram":"novo"}
                                """))
                .andExpect(status().isUnauthorized());
        assertThat(userRepository.findByEmail(email).orElseThrow().getInstagram()).isNull();

        mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"inativo@teste.com","password":"senhaforte123"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtLegadoAssinadoSemVersaoExigeNovoLogin() throws Exception {
        String email = "legado@teste.com";
        registrar(email, "senhaforte123", "CLIENT");
        var user = userRepository.findByEmail(email).orElseThrow();
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("tramposeditor").subject(user.getId().toString())
                .issuedAt(now).expiresAt(now.plusSeconds(300)).build();
        assertTokenRejected(assinar(jwtEncoder, claims));
    }

    @Test
    void assinaturaEExpiracaoContinuamObrigatoriasComVersaoValida() throws Exception {
        String email = "validacao@teste.com";
        String validToken = registrar(email, "senhaforte123", "CLIENT");
        mockMvc.perform(get("/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk());
        var user = userRepository.findByEmail(email).orElseThrow();
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("tramposeditor").subject(user.getId().toString())
                .claim(JwtService.SESSION_VERSION_CLAIM, user.getSessionVersion());

        var otherKey = KeyGenerator.getInstance("HmacSHA256");
        otherKey.init(256);
        JwtEncoder wrongEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(otherKey.generateKey()));
        assertTokenRejected(assinar(wrongEncoder, claims.issuedAt(now).expiresAt(now.plusSeconds(300)).build()));
        assertTokenRejected(assinar(jwtEncoder, claims.issuedAt(now.minusSeconds(600))
                .expiresAt(now.minusSeconds(300)).build()));
    }

    @Test
    void revogacoesConcorrentesPreservamOsDoisIncrementos() throws Exception {
        String email = "concorrente@teste.com";
        String oldToken = registrar(email, "senhaforte123", "CLIENT");
        var user = userRepository.findByEmail(email).orElseThrow();
        long initialVersion = user.getSessionVersion();
        var transaction = new TransactionTemplate(transactionManager);
        var firstUpdated = new CountDownLatch(1);
        var secondRead = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> transaction.executeWithoutResult(status -> {
                assertThat(userRepository.updatePasswordAndRevokeSessions(
                        user.getId(), user.getPasswordHash(), LocalDateTime.now())).isEqualTo(1);
                firstUpdated.countDown();
                aguardar(secondRead);
            }));
            var second = workers.submit(() -> {
                aguardar(firstUpdated);
                transaction.executeWithoutResult(status -> {
                    // As transações se sobrepõem: a primeira ainda não gravou o commit.
                    assertThat(userRepository.findById(user.getId()).orElseThrow().getSessionVersion())
                            .isEqualTo(initialVersion);
                    secondRead.countDown();
                    assertThat(userRepository.updatePasswordAndRevokeSessions(
                            user.getId(), user.getPasswordHash(), LocalDateTime.now())).isEqualTo(1);
                });
            });
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        }
        assertThat(userRepository.findById(user.getId()).orElseThrow().getSessionVersion())
                .isEqualTo(initialVersion + 2);
        assertTokenRejected(oldToken);

        Instant now = Instant.now();
        var previousVersion = JwtClaimsSet.builder().issuer("tramposeditor").subject(user.getId().toString())
                .issuedAt(now).expiresAt(now.plusSeconds(300))
                .claim(JwtService.SESSION_VERSION_CLAIM, initialVersion + 1).build();
        assertTokenRejected(assinar(jwtEncoder, previousVersion));
    }

    private static void aguardar(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Execução concorrente interrompida", e);
        }
    }

    private String assinar(JwtEncoder encoder, JwtClaimsSet claims) {
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private void assertTokenRejected(String token) throws Exception {
        mockMvc.perform(get("/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarPayloadInvalido() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nao-e-email", "password": "123", "displayName": "", "roles": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    /** Passo 1 com o mínimo preenchido — pros testes que só querem um pendente de pé. */
    private void pedirCadastro(String email) throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "senhaforte123",
                                  "displayName": "Fulano",
                                  "username": "u-%s",
                                  "roles": ["CLIENT"]
                                }
                                """.formatted(email, email.split("@")[0])))
                .andExpect(status().isAccepted());
    }

    private ResultActions confirmar(String email, String code) throws Exception {
        return mockMvc.perform(post("/v1/auth/register/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "code": "%s"
                        }
                        """.formatted(email, code)));
    }

    private String registrar(String email, String password, String... roles) throws Exception {
        String rolesJson = Arrays.stream(roles)
                .map(role -> "\"" + role + "\"")
                .collect(Collectors.joining(", "));

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "Fulano",
                                  "username": "u-%s",
                                  "roles": [%s]
                                }
                                """.formatted(email, password, email.split("@")[0], rolesJson)))
                .andExpect(status().isAccepted());

        String body = confirmar(email, TestcontainersConfiguration.CODIGO_DE_CADASTRO)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(body, "$.accessToken");
        assertThat(token).isNotBlank();

        return token;
    }
}
