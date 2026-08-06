package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.repository.IJobRequestRepository;
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

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class JobRequestTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IJobRequestRepository jobRequestRepository;

    @Autowired
    private IUserRepository userRepository;

    @BeforeEach
    void limpar(){
        jobRequestRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void clientePostaPedidoEditorSeCandidataEContatoSoSaiDepoisDoAceite() throws Exception {
        String cliente = registrar("cliente@teste.com", "Cliente Maria");
        String editor = registrar("editor@teste.com", "Editor Bruno");
        publicarPerfilDeEditor(editor);

        String pedido = mockMvc.perform(post("/v1/me/job-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "3 Reels por semana",
                                  "description": "Preciso de cortes verticais com legenda.",
                                  "niche": "REELS",
                                  "budgetCents": 30000,
                                  "deadlineDays": 3
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();

        String jobId = JsonPath.read(pedido, "$.id");

        // O editor vê o pedido no feed, ainda sem contato do cliente.
        mockMvc.perform(get("/v1/job-requests").header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].clientDisplayName").value("Cliente Maria"))
                .andExpect(jsonPath("$.content[0].clientEmail").value(nullValue()))
                .andExpect(jsonPath("$.content[0].myInterestStatus").value(nullValue()));

        String candidatura = mockMvc.perform(post("/v1/job-requests/" + jobId + "/interests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Faço Reels com legenda animada, topo o teste."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.myInterestStatus").value("PENDING"))
                .andExpect(jsonPath("$.clientEmail").value(nullValue()))
                .andReturn().getResponse().getContentAsString();

        // O cliente vê a candidatura, mas o e-mail do editor ainda não.
        String meusPedidos = mockMvc.perform(get("/v1/me/job-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].interests.length()").value(1))
                .andExpect(jsonPath("$[0].interests[0].editorDisplayName").value("Editor Bruno"))
                .andExpect(jsonPath("$[0].interests[0].editorEmail").value(nullValue()))
                .andReturn().getResponse().getContentAsString();

        String interestId = JsonPath.read(meusPedidos, "$[0].interests[0].id");

        // Aceite libera o contato dos dois lados.
        mockMvc.perform(post("/v1/me/job-requests/" + jobId + "/interests/" + interestId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interests[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.interests[0].editorEmail").value("editor@teste.com"));

        mockMvc.perform(get("/v1/me/job-interests").header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].myInterestStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$[0].clientEmail").value("cliente@teste.com"));

        // Candidatura duplicada não passa.
        mockMvc.perform(post("/v1/job-requests/" + jobId + "/interests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "de novo"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void semPerfilDeEditorNaoDaPraSeCandidatar() throws Exception {
        String cliente = registrar("dono@teste.com", "Dono");
        String curioso = registrar("curioso@teste.com", "Curioso");

        String jobId = criarPedido(cliente);

        mockMvc.perform(post("/v1/job-requests/" + jobId + "/interests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + curioso)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "quero tentar"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void pedidoFechadoSaiDoFeedENaoAceitaCandidatura() throws Exception {
        String cliente = registrar("fecha@teste.com", "Fecha");
        String editor = registrar("edita@teste.com", "Edita");
        publicarPerfilDeEditor(editor);

        String jobId = criarPedido(cliente);

        mockMvc.perform(post("/v1/me/job-requests/" + jobId + "/close")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get("/v1/job-requests").header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(post("/v1/job-requests/" + jobId + "/interests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "cheguei tarde"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void naoDaPraSeCandidatarAoProprioPedidoNemMexerNoPedidoDosOutros() throws Exception {
        String dono = registrar("eu@teste.com", "Eu Mesmo");
        publicarPerfilDeEditor(dono);
        String estranho = registrar("outro@teste.com", "Outro");

        String jobId = criarPedido(dono);

        mockMvc.perform(post("/v1/job-requests/" + jobId + "/interests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + dono)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "eu mesmo edito"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/v1/me/job-requests/" + jobId + "/close")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + estranho))
                .andExpect(status().isForbidden());
    }

    // ---------- Helpers ----------

    @Test
    void feedDeTramposPaginaEEscondeOPedidoDeQuemEstaLendo() throws Exception {
        String cliente = registrar("cliente@teste.com", "Cliente Maria");
        String editor = registrar("editor@teste.com", "Editor Bruno");
        publicarPerfilDeEditor(editor);

        criarPedido(cliente);
        criarPedido(cliente);
        criarPedido(cliente);
        // O editor também é cliente de si mesmo — mas o próprio pedido não entra no
        // feed dele, e não pode contar no total nem furar o tamanho da página.
        criarPedido(editor);

        mockMvc.perform(get("/v1/job-requests?size=2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/v1/job-requests?size=2&page=1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void feedDeTramposFiltraPorCategoria() throws Exception {
        String cliente = registrar("cliente@teste.com", "Cliente Maria");
        String editor = registrar("editor@teste.com", "Editor Bruno");
        publicarPerfilDeEditor(editor);

        criarPedido(cliente); // PODCAST

        mockMvc.perform(get("/v1/job-requests?niche=PODCAST")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/v1/job-requests?niche=TIKTOK")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private String registrar(String email, String displayName) throws Exception {
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

        // O cadastro virou dois passos (V18): o POST acima só guarda o pendente e manda o
        // código. A conta — e a sessão — nascem aqui, e já com o e-mail provado.
        String body = mockMvc.perform(post("/v1/auth/register/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "code": "%s"
                                }
                                """.formatted(email, TestcontainersConfiguration.CODIGO_DE_CADASTRO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();



        return JsonPath.read(body, "$.accessToken");
    }

    private void publicarPerfilDeEditor(String token) throws Exception {
        mockMvc.perform(put("/v1/me/editor-profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"niches": ["REELS"], "tools": ["PREMIERE"]}
                                """))
                .andExpect(status().isOk());
    }

    private String criarPedido(String token) throws Exception {
        String body = mockMvc.perform(post("/v1/me/job-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Corte de podcast",
                                  "description": "Uma hora de gravação virando 5 cortes.",
                                  "niche": "PODCAST"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.id");
    }
    /** O clique no link de confirmação, sem passar pelo e-mail. Ver o helper de cadastro. */
    private void confirmarEmail(String email){
        var user = userRepository.findByEmail(email.trim().toLowerCase()).orElseThrow();
        user.setEmailVerifiedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

}
