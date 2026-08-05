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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O username como identidade escolhida, e não como enfeite derivado (V15).
 * <p>
 * O que estes testes prendem é a decisão de 2026-08-05: <b>o nome é pedido no cadastro,
 * verificado antes e trocável depois</b> — com o preço de que a troca não deixa rastro. Se
 * alguém um dia fizer o cadastro voltar a gerar o nome sozinho, ou tornar a troca livre,
 * eles quebram.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UsernameTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUserRepository userRepository;

    @BeforeEach
    void limpar(){
        userRepository.deleteAll();
    }

    // ---------- A escolha, no cadastro ----------

    @Test
    void cadastroUsaONomeEscolhidoENaoUmDerivadoDoDisplayName() throws Exception {
        registrar("pessoa@teste.com", "Maria Fernanda Souza", "corta-tudo");

        // Se o nome ainda fosse gerado, isto seria "maria-fernanda-souza".
        mockMvc.perform(get("/v1/profiles/corta-tudo"))
                .andExpect(status().isOk());
    }

    @Test
    void cadastroRecusaNomeJaEmUso() throws Exception {
        registrar("primeiro@teste.com", "Primeiro", "disputado");

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("segundo@teste.com", "Segundo", "disputado")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Esse nome de usuário já está em uso."));
    }

    @Test
    void cadastroRecusaRotaDoProdutoComoNome() throws Exception {
        // A raiz é namespace de usuário: um "/entrar" de alguém tornaria o login inalcançável.
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("esperto@teste.com", "Esperto", "entrar")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        "Esse nome é reservado pela plataforma. Escolha outro."));
    }

    // ---------- A verificação, antes de aprovar ----------

    @Test
    void checagemDeDisponibilidadeRespondeSemToken() throws Exception {
        registrar("dono@teste.com", "Dono", "ja-tomado");

        // Público de propósito: no cadastro ainda não existe token, e é justamente lá que
        // a pergunta é feita.
        mockMvc.perform(get("/v1/usernames/availability").param("username", "ja-tomado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.message").value("Esse nome de usuário já está em uso."));

        mockMvc.perform(get("/v1/usernames/availability").param("username", "ninguem-tem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void checagemNormalizaMaiusculaAntesDeResponder() throws Exception {
        registrar("dono@teste.com", "Dono", "ja-tomado");

        // "JA-TOMADO" e "ja-tomado" são o mesmo endereço — dizer que o primeiro está livre
        // faria a pessoa escolher um nome que o submit recusaria em seguida.
        mockMvc.perform(get("/v1/usernames/availability").param("username", "JA-TOMADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    // ---------- A troca, depois ----------

    @Test
    void trocaMudaOEnderecoEDerrubaOAntigo() throws Exception {
        String token = registrar("mudei@teste.com", "Quem Muda", "nome-velho");

        mockMvc.perform(put("/v1/users/me/username")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "nome-novo"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("nome-novo"));

        mockMvc.perform(get("/v1/profiles/nome-novo")).andExpect(status().isOk());

        // O antigo não redireciona: some. É a decisão de 2026-08-05, e é o que o cooldown
        // abaixo existe pra segurar.
        mockMvc.perform(get("/v1/profiles/nome-velho")).andExpect(status().isNotFound());

        // E volta a ficar livre pra qualquer um.
        mockMvc.perform(get("/v1/usernames/availability").param("username", "nome-velho"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void segundaTrocaSeguidaBateNoCooldown() throws Exception {
        String token = registrar("apressado@teste.com", "Apressado", "primeiro-nome");

        // A primeira troca passa: quem nunca trocou não tem cooldown pra cumprir.
        trocar(token, "segundo-nome").andExpect(status().isOk());

        trocar(token, "terceiro-nome")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        "Você trocou seu nome de usuário há pouco. Poderá trocar de novo em 14 dias."));

        // E o endereço continua sendo o da troca que passou.
        mockMvc.perform(get("/v1/profiles/segundo-nome")).andExpect(status().isOk());
    }

    @Test
    void reenviarOProprioNomeNaoGastaOCooldown() throws Exception {
        String token = registrar("teimoso@teste.com", "Teimoso", "mesmo-nome");

        // Duplo submit não pode custar 14 dias à pessoa.
        trocar(token, "mesmo-nome").andExpect(status().isOk());
        trocar(token, "nome-de-verdade")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("nome-de-verdade"));
    }

    @Test
    void trocaRecusaNomeDeOutraPessoa() throws Exception {
        registrar("dono@teste.com", "Dono", "nome-do-dono");
        String token = registrar("invejoso@teste.com", "Invejoso", "nome-proprio");

        trocar(token, "nome-do-dono")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Esse nome de usuário já está em uso."));
    }

    @Test
    void trocaExigeToken() throws Exception {
        mockMvc.perform(put("/v1/users/me/username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "qualquer-um"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    private org.springframework.test.web.servlet.ResultActions trocar(String token, String username)
            throws Exception {
        return mockMvc.perform(put("/v1/users/me/username")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "%s"}
                        """.formatted(username)));
    }

    private String registrar(String email, String displayName, String username) throws Exception {
        String body = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(email, displayName, username)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.accessToken");
    }

    private String payload(String email, String displayName, String username){
        return """
                {
                  "email": "%s",
                  "password": "senhaforte123",
                  "displayName": "%s",
                  "username": "%s",
                  "roles": ["CLIENT"]
                }
                """.formatted(email, displayName, username);
    }
}
