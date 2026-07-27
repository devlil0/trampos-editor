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

    @Test
    void deveCadastrarEditorERetornarToken() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "Editor@Teste.com",
                                  "password": "senhaforte123",
                                  "displayName": "Editor Teste",
                                  "roles": ["EDITOR"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                // e-mail normalizado pra minúsculo no cadastro
                .andExpect(jsonPath("$.user.email").value("editor@teste.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("EDITOR"))
                .andExpect(jsonPath("$.user.createdAt").isNotEmpty());
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

    private String registrar(String email, String password, String... roles) throws Exception {
        String rolesJson = Arrays.stream(roles)
                .map(role -> "\"" + role + "\"")
                .collect(Collectors.joining(", "));

        String body = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "Fulano",
                                  "roles": [%s]
                                }
                                """.formatted(email, password, rolesJson)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(body, "$.accessToken");
        assertThat(token).isNotBlank();

        return token;
    }
}
