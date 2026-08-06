package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.repository.IConversationRepository;
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

/**
 * Bloco 5 — a caixa de entrada única. O que estes testes prendem é justamente a
 * decisão de arquitetura: <b>duas portas, uma conversa</b>. Se alguém um dia fizer o
 * aceite abrir um fluxo paralelo ao do "Falar com", eles quebram.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ConversationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IConversationRepository conversationRepository;

    @Autowired
    private IJobRequestRepository jobRequestRepository;

    @Autowired
    private IUserRepository userRepository;

    @BeforeEach
    void limpar(){
        conversationRepository.deleteAll();
        jobRequestRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------- Porta 1: vitrine ----------

    @Test
    void clienteFalaComEditorPelaVitrineEOsDoisVeemAConversa() throws Exception {
        String cliente = registrar("cliente@teste.com", "Cliente Maria");
        String editor = registrar("editor@teste.com", "Editor Bruno");
        String slug = publicarPerfilDeEditor(editor);

        String conversa = mockMvc.perform(post("/v1/me/conversations/with-editor/" + slug)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Oi Bruno, tenho 4 Reels por semana. Topa?"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.counterpartDisplayName").value("Editor Bruno"))
                // Conversa nascida do perfil não tem pedido associado.
                .andExpect(jsonPath("$.jobRequestId").value(nullValue()))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].mine").value(true))
                .andReturn().getResponse().getContentAsString();

        String conversaId = JsonPath.read(conversa, "$.id");

        // Do lado do editor, a mesma conversa aparece com o cliente como contraparte
        // e marcada como não lida.
        mockMvc.perform(get("/v1/me/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].counterpartDisplayName").value("Cliente Maria"))
                .andExpect(jsonPath("$[0].unread").value(true))
                .andExpect(jsonPath("$[0].jobTitle").value(nullValue()));

        // Quem escreveu não vê a própria mensagem como não lida.
        mockMvc.perform(get("/v1/me/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].unread").value(false));

        // Abrir marca como lida.
        mockMvc.perform(get("/v1/me/conversations/" + conversaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].mine").value(false));

        mockMvc.perform(get("/v1/me/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(jsonPath("$[0].unread").value(false));

        // O editor responde e a bola volta pro cliente.
        mockMvc.perform(post("/v1/me/conversations/" + conversaId + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Topo sim. Me manda o material bruto."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mine").value(true));

        mockMvc.perform(get("/v1/me/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(jsonPath("$[0].unread").value(true))
                .andExpect(jsonPath("$[0].lastMessagePreview").value("Topo sim. Me manda o material bruto."));
    }

    @Test
    void falarDuasVezesComOMesmoEditorNaoAbreConversaNova() throws Exception {
        String cliente = registrar("repete@teste.com", "Repete");
        String editor = registrar("alvo@teste.com", "Alvo");
        String slug = publicarPerfilDeEditor(editor);

        for (String texto : new String[]{"primeira", "segunda"}) {
            mockMvc.perform(post("/v1/me/conversations/with-editor/" + slug)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"body\": \"%s\"}".formatted(texto)))
                    .andExpect(status().isCreated());
        }

        // Uma conversa só, com as duas mensagens dentro.
        mockMvc.perform(get("/v1/me/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].lastMessagePreview").value("segunda"));
    }

    // ---------- Porta 2: aceite de interesse ----------

    @Test
    void aceitarInteresseAbreAConversaDoPedido() throws Exception {
        String cliente = registrar("dono@teste.com", "Dono Carlos");
        String editor = registrar("cand@teste.com", "Candidata Ana");
        publicarPerfilDeEditor(editor);

        String jobId = criarPedido(cliente);

        mockMvc.perform(post("/v1/job-requests/" + jobId + "/interests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Faço cortes de podcast, topo."}
                                """))
                .andExpect(status().isCreated());

        // Antes do aceite não existe conversa nenhuma.
        mockMvc.perform(get("/v1/me/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(jsonPath("$.length()").value(0));

        String meusPedidos = mockMvc.perform(get("/v1/me/job-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andReturn().getResponse().getContentAsString();
        String interestId = JsonPath.read(meusPedidos, "$[0].interests[0].id");

        mockMvc.perform(post("/v1/me/job-requests/" + jobId + "/interests/" + interestId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(status().isOk());

        // Agora os dois lados têm a conversa, e ela carrega o pedido de origem.
        mockMvc.perform(get("/v1/me/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].counterpartDisplayName").value("Dono Carlos"))
                .andExpect(jsonPath("$[0].jobTitle").value("Corte de podcast"))
                // Ninguém falou ainda: o sistema abre a porta, não puxa assunto.
                .andExpect(jsonPath("$[0].lastMessagePreview").value(nullValue()))
                .andExpect(jsonPath("$[0].unread").value(false));

        mockMvc.perform(get("/v1/me/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].counterpartDisplayName").value("Candidata Ana"));
    }

    @Test
    void aceitarDuasVezesNaoDuplicaAConversa() throws Exception {
        String cliente = registrar("d2@teste.com", "Dono Dois");
        String editor = registrar("c2@teste.com", "Cand Dois");
        publicarPerfilDeEditor(editor);

        String jobId = criarPedido(cliente);
        mockMvc.perform(post("/v1/job-requests/" + jobId + "/interests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "topo"}
                                """))
                .andExpect(status().isCreated());

        String meusPedidos = mockMvc.perform(get("/v1/me/job-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andReturn().getResponse().getContentAsString();
        String interestId = JsonPath.read(meusPedidos, "$[0].interests[0].id");

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/v1/me/job-requests/" + jobId + "/interests/" + interestId + "/accept")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/v1/me/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---------- Cercas ----------

    @Test
    void terceiroNaoLeNemEscreveNaConversaDosOutros() throws Exception {
        String cliente = registrar("a@teste.com", "A");
        String editor = registrar("b@teste.com", "B");
        String bisbilhoteiro = registrar("c@teste.com", "C");
        String slug = publicarPerfilDeEditor(editor);

        String conversa = mockMvc.perform(post("/v1/me/conversations/with-editor/" + slug)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "particular"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String conversaId = JsonPath.read(conversa, "$.id");

        mockMvc.perform(get("/v1/me/conversations/" + conversaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bisbilhoteiro))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/v1/me/conversations/" + conversaId + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bisbilhoteiro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "oi"}
                                """))
                .andExpect(status().isForbidden());

        // E a caixa dele segue vazia.
        mockMvc.perform(get("/v1/me/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer " + bisbilhoteiro))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void naoDaPraConversarComSigoMesmoNemComQuemNaoPublicouPerfil() throws Exception {
        String eu = registrar("eu@teste.com", "Eu Mesmo");
        String meuSlug = publicarPerfilDeEditor(eu);

        mockMvc.perform(post("/v1/me/conversations/with-editor/" + meuSlug)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + eu)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "oi eu"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/v1/me/conversations/with-editor/nao-existe")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + eu)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "alguem ai"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void mensagemVaziaNaoPassa() throws Exception {
        String cliente = registrar("v1@teste.com", "V1");
        String editor = registrar("v2@teste.com", "V2");
        String slug = publicarPerfilDeEditor(editor);

        mockMvc.perform(post("/v1/me/conversations/with-editor/" + slug)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "   "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void conversaExigeLogin() throws Exception {
        mockMvc.perform(get("/v1/me/conversations")).andExpect(status().isUnauthorized());
    }

    // ---------- Helpers ----------

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

    /** Publica o perfil e devolve o slug, que é como a vitrine referencia o editor. */
    private String publicarPerfilDeEditor(String token) throws Exception {
        String body = mockMvc.perform(put("/v1/me/editor-profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"niches": ["REELS"], "tools": ["PREMIERE"]}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.slug");
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
