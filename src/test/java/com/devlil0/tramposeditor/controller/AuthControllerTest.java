package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @BeforeEach
    void clean(){
        userRepository.deleteAll();
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

        for (int i = 0; i < 5; i++) {
            confirmar("bruto@teste.com", "111111").andExpect(status().isConflict());
        }

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
