package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.repository.IJobRequestRepository;
import com.devlil0.tramposeditor.database.repository.IDealRepository;
import com.devlil0.tramposeditor.database.repository.IConversationRepository;
import com.devlil0.tramposeditor.notification.NotificationService;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

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

    @Autowired private IDealRepository dealRepository;
    @Autowired private IConversationRepository conversationRepository;
    @MockitoSpyBean private NotificationService notificationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IJobRequestRepository jobRequestRepository;

    @Autowired
    private IUserRepository userRepository;

    @BeforeEach
    void limpar(){
        dealRepository.deleteAll();
        conversationRepository.deleteAll();
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

    @ParameterizedTest
    @ValueSource(strings = {"done", "cancel"})
    void replayDoAceiteEncerradoNaoReabreNemRepeteAviso(String outcome) throws Exception {
        String cliente = registrar("cliente@teste.com", "Cliente");
        String editor = registrar("editor@teste.com", "Editor");
        publicarPerfilDeEditor(editor);
        String job = criarPedido(cliente);
        String interest = candidatar(job, editor, cliente);
        clearInvocations(notificationService);
        aceitar(job, interest, cliente, 200);
        var original = dealRepository.findAll().getFirst();
        mockMvc.perform(post("/v1/me/deals/" + original.getId() + "/" + outcome)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)).andExpect(status().isOk());
        aceitar(job, interest, cliente, 200);
        aceitar(job, interest, cliente, 200);
        assertThat(dealRepository.findAll()).extracting(d -> d.getId()).containsExactly(original.getId());
        assertThat(dealRepository.findById(original.getId()).orElseThrow().getStatus().name())
                .isEqualTo(outcome.equals("done") ? "DONE" : "CANCELLED");
        assertThat(conversationRepository.count()).isEqualTo(1);
        verify(notificationService, times(1)).interestAccepted(any(), any(), anyString());
    }

    @Test
    void aceiteEscolheUmEditorFechaPedidoEProtegePropriedade() throws Exception {
        String cliente = registrar("cliente@teste.com", "Cliente");
        String editor = registrar("editor@teste.com", "Editor");
        String outro = registrar("outro@teste.com", "Outro");
        publicarPerfilDeEditor(editor);
        publicarPerfilDeEditor(outro);
        String job = criarPedido(cliente);
        String first = candidatar(job, editor, cliente);
        String second = candidatar(job, outro, cliente);
        aceitar(job, first, editor, 403);
        aceitar(job, first, outro, 403);
        aceitar(job, first, cliente, 200);
        aceitar(job, second, cliente, 409);
        aceitar(job, first, cliente, 200);
        assertThat(dealRepository.count()).isEqualTo(1);
        assertThat(conversationRepository.count()).isEqualTo(1);
        assertThat(jobRequestRepository.findById(java.util.UUID.fromString(job)).orElseThrow().getStatus().name())
                .isEqualTo("CLOSED");
        mockMvc.perform(get("/v1/job-requests").header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void fecharPedidoComInteressadoImpedeAceiteSemApagarHistorico() throws Exception {
        String cliente = registrar("cliente@teste.com", "Cliente");
        String editor = registrar("editor@teste.com", "Editor");
        publicarPerfilDeEditor(editor);
        String job = criarPedido(cliente);
        String interest = candidatar(job, editor, cliente);
        mockMvc.perform(post("/v1/me/job-requests/" + job + "/close")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)).andExpect(status().isOk());
        aceitar(job, interest, cliente, 409);
        assertThat(dealRepository.count()).isZero();
        mockMvc.perform(get("/v1/me/job-requests").header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(jsonPath("$[0].id").value(job))
                .andExpect(jsonPath("$[0].interests.length()").value(1))
                .andExpect(jsonPath("$[0].interests[0].status").value("PENDING"));
    }

    private String candidatar(String job, String editor, String cliente) throws Exception {
        mockMvc.perform(post("/v1/job-requests/" + job + "/interests")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor)
                .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Topo\"}"))
                .andExpect(status().isCreated());
        String json = mockMvc.perform(get("/v1/me/job-requests")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$[0].interests[-1].id");
    }

    @Test
    void doisAceitesSimultaneosEscolhemSomenteUmEditor() throws Exception {
        String cliente = registrar("cliente@teste.com", "Cliente");
        String editor = registrar("editor@teste.com", "Editor");
        String outro = registrar("outro@teste.com", "Outro");
        publicarPerfilDeEditor(editor);
        publicarPerfilDeEditor(outro);
        String job = criarPedido(cliente);
        String first = candidatar(job, editor, cliente);
        String second = candidatar(job, outro, cliente);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var futures = java.util.List.of(first, second).stream().map(interest -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("Sem início");
                return mockMvc.perform(post("/v1/me/job-requests/" + job + "/interests/" + interest + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                        .andReturn().getResponse().getStatus();
            })).toList();
            assertThat(ready.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(java.util.List.of(futures.get(0).get(20, java.util.concurrent.TimeUnit.SECONDS),
                    futures.get(1).get(20, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        } finally {
            start.countDown();
        }
        assertThat(dealRepository.count()).isEqualTo(1);
        assertThat(conversationRepository.count()).isEqualTo(1);
    }

    private void aceitar(String job, String interest, String token, int expected) throws Exception {
        mockMvc.perform(post("/v1/me/job-requests/" + job + "/interests/" + interest + "/accept")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().is(expected));
    }

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
