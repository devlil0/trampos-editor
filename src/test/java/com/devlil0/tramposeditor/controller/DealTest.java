package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.repository.IConversationRepository;
import com.devlil0.tramposeditor.database.repository.IDealRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bloco 6 — trampo fechado e avaliação (ESBOCO §7 item 4).
 * <p>
 * O teste central é {@link #notaDoTrampoVoltaParaAVitrine}: é ele que prova que o laço
 * de reputação dá a volta inteira, do aceite até a ordenação da página pública.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DealTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IDealRepository dealRepository;

    @Autowired
    private IConversationRepository conversationRepository;

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
    void notaDoTrampoVoltaParaAVitrine() throws Exception {
        String cliente = registrar("cliente@teste.com", "Cliente Maria");
        String editor = registrar("editor@teste.com", "Editor Bruno");
        publicarPerfil(editor);
        String slug = slugDoEditor(editor);

        // A vitrine começa sem nota nenhuma — é o "★ 0.0" que o §7 item 5 quer esconder.
        mockMvc.perform(get("/v1/editors/" + slug))
                .andExpect(jsonPath("$.ratingCount").value(0))
                .andExpect(jsonPath("$.ratingAvg").value(0.0));

        String dealId = aceitarInteresseECapturarTrampo(cliente, editor);

        // Aceitar já abriu o trampo, e ele nasce aberto.
        mockMvc.perform(get("/v1/me/deals").header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].title").value("Corte de podcast"))
                .andExpect(jsonPath("$[0].iAmClient").value(false));

        // Avaliar antes de encerrar não pode: não há trabalho entregue pra avaliar.
        mockMvc.perform(post("/v1/me/deals/" + dealId + "/review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 5, "comment": "cedo demais"}
                                """))
                .andExpect(status().isConflict());

        // Quem marca como concluído é quem contratou.
        mockMvc.perform(post("/v1/me/deals/" + dealId + "/done")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/v1/me/deals/" + dealId + "/done")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());

        // O cliente avalia o editor.
        mockMvc.perform(post("/v1/me/deals/" + dealId + "/review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 4, "comment": "Entregou no prazo, corte limpo."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewedByMe").value(true));

        // E aqui está o laço fechando: a nota aparece na página pública do editor.
        mockMvc.perform(get("/v1/editors/" + slug))
                .andExpect(jsonPath("$.ratingCount").value(1))
                .andExpect(jsonPath("$.ratingAvg").value(4.0));

        // Avaliar duas vezes não: a nota não é urna aberta.
        mockMvc.perform(post("/v1/me/deals/" + dealId + "/review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 1, "comment": "mudei de ideia"}
                                """))
                .andExpect(status().isConflict());

        // O editor também avalia o cliente — o ethos vale pros dois lados (§6).
        mockMvc.perform(post("/v1/me/deals/" + dealId + "/review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 5, "comment": "Briefou direito e pagou em dia."}
                                """))
                .andExpect(status().isCreated());

        // A nota do cliente não mexe em vitrine nenhuma: ele não tem perfil público.
        mockMvc.perform(get("/v1/editors/" + slug))
                .andExpect(jsonPath("$.ratingCount").value(1))
                .andExpect(jsonPath("$.ratingAvg").value(4.0));

        // Cada lado vê a avaliação que recebeu.
        mockMvc.perform(get("/v1/me/deals").header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(jsonPath("$[0].reviewOfMe.rating").value(4))
                .andExpect(jsonPath("$[0].reviewOfMe.authorName").value("Cliente Maria"));
    }

    @Test
    void mediaDeDuasNotasEArredondadaParaUmaCasa() throws Exception {
        String editor = registrar("editor@teste.com", "Editor Bruno");
        publicarPerfil(editor);
        String slug = slugDoEditor(editor);

        // 5 e 4 => 4.5; 5, 4 e 4 => 4.333... => 4.3
        avaliarComNota(registrar("c1@teste.com", "Cliente Um"), editor, (short) 5);
        avaliarComNota(registrar("c2@teste.com", "Cliente Dois"), editor, (short) 4);

        mockMvc.perform(get("/v1/editors/" + slug))
                .andExpect(jsonPath("$.ratingCount").value(2))
                .andExpect(jsonPath("$.ratingAvg").value(4.5));

        avaliarComNota(registrar("c3@teste.com", "Cliente Tres"), editor, (short) 4);

        mockMvc.perform(get("/v1/editors/" + slug))
                .andExpect(jsonPath("$.ratingCount").value(3))
                .andExpect(jsonPath("$.ratingAvg").value(4.3));
    }

    @Test
    void trampoDaVitrineNasceNaConversaEPodeSerCancelado() throws Exception {
        String cliente = registrar("cliente@teste.com", "Cliente Maria");
        String editor = registrar("editor@teste.com", "Editor Bruno");
        publicarPerfil(editor);
        String slug = slugDoEditor(editor);

        // Conversa direta da vitrine: não há pedido nenhum por trás.
        String conversa = mockMvc.perform(post("/v1/me/conversations/with-editor/" + slug)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Topa editar uns Reels?"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String conversaId = JsonPath.read(conversa, "$.id");

        // O editor não abre trampo: quem combina é quem contrata.
        mockMvc.perform(post("/v1/me/deals/from-conversation/" + conversaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Reels da semana"}
                                """))
                .andExpect(status().isForbidden());

        String deal = mockMvc.perform(post("/v1/me/deals/from-conversation/" + conversaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Reels da semana"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.counterpartSlug").value(slug))
                .andReturn().getResponse().getContentAsString();
        String dealId = JsonPath.read(deal, "$.id");

        // Dois abertos na mesma conversa seria ambiguidade sobre qual está sendo avaliado.
        mockMvc.perform(post("/v1/me/deals/from-conversation/" + conversaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Outro trampo"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/v1/me/deals/" + dealId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Cancelado não vira avaliação: não houve trabalho pra avaliar.
        mockMvc.perform(post("/v1/me/deals/" + dealId + "/review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 1, "comment": "não vingou"}
                                """))
                .andExpect(status().isConflict());

        // Encerrado o anterior, a mesma conversa pode gerar outro trampo — é a
        // repetição de fornecedor que o §3.2 quer premiar.
        mockMvc.perform(post("/v1/me/deals/from-conversation/" + conversaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Segunda leva"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void trampoDeOutroNaoApareceNemSeDeixaTocar() throws Exception {
        String cliente = registrar("cliente@teste.com", "Cliente Maria");
        String editor = registrar("editor@teste.com", "Editor Bruno");
        publicarPerfil(editor);
        String dealId = aceitarInteresseECapturarTrampo(cliente, editor);

        String estranho = registrar("estranho@teste.com", "Alguem Mais");

        mockMvc.perform(get("/v1/me/deals").header(HttpHeaders.AUTHORIZATION, "Bearer " + estranho))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(post("/v1/me/deals/" + dealId + "/done")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + estranho))
                .andExpect(status().isForbidden());
    }

    // ---------- Helpers ----------

    /** Roda o caminho do feed até o aceite e devolve o id do trampo que nasceu dele. */
    private String aceitarInteresseECapturarTrampo(String cliente, String editor) throws Exception {
        String pedido = mockMvc.perform(post("/v1/me/job-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
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
        String jobId = JsonPath.read(pedido, "$.id");

        mockMvc.perform(post("/v1/job-requests/" + jobId + "/interests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Topo o trampo."}
                                """))
                .andExpect(status().isCreated());

        String meusPedidos = mockMvc.perform(get("/v1/me/job-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andReturn().getResponse().getContentAsString();
        String interestId = JsonPath.read(meusPedidos, "$[0].interests[0].id");

        mockMvc.perform(post("/v1/me/job-requests/" + jobId + "/interests/" + interestId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(status().isOk());

        String deals = mockMvc.perform(get("/v1/me/deals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(deals, "$[0].id");
    }

    /** Um trampo inteiro, do aceite à nota, só pra mexer na média do editor. */
    private void avaliarComNota(String cliente, String editor, short nota) throws Exception {
        String dealId = aceitarInteresseECapturarTrampo(cliente, editor);

        mockMvc.perform(post("/v1/me/deals/" + dealId + "/done")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/me/deals/" + dealId + "/review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": %d}
                                """.formatted(nota)))
                .andExpect(status().isCreated());
    }

    private String registrar(String email, String displayName) throws Exception {
        String body = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "senhaforte123",
                                  "displayName": "%s",
                                  "roles": ["CLIENT"]
                                }
                                """.formatted(email, displayName)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.accessToken");
    }

    private void publicarPerfil(String token) throws Exception {
        mockMvc.perform(put("/v1/me/editor-profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"niches": ["REELS"], "tools": ["PREMIERE"]}
                                """))
                .andExpect(status().isOk());
    }

    private String slugDoEditor(String token) throws Exception {
        String body = mockMvc.perform(get("/v1/me/editor-profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.slug");
    }
}
