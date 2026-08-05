package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.repository.IConversationRepository;
import com.devlil0.tramposeditor.database.repository.IJobRequestRepository;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O canal de notificação (ESBOCO §7 item 3).
 * <p>
 * O que estes testes protegem não é "o e-mail sai" — é a regra que impede o produto de
 * virar spam: <b>um aviso por vez, não um por evento</b>.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class NotificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IConversationRepository conversationRepository;

    @Autowired
    private IJobRequestRepository jobRequestRepository;

    @Autowired
    private IUserRepository userRepository;

    /** Substitui o LogEmailSender: aqui o interesse é o que foi mandado, não o log. */
    @MockitoBean
    private EmailSender emailSender;

    @BeforeEach
    void limpar(){
        conversationRepository.deleteAll();
        jobRequestRepository.deleteAll();
        userRepository.deleteAll();
        reset(emailSender);
    }

    @Test
    void mensagemNovaAvisaOOutroLadoUmaVezSo() throws Exception {
        String cliente = registrar("cliente@teste.com", "Cliente Maria");
        String editor = registrar("editor@teste.com", "Editor Bruno");
        publicarPerfil(editor);
        String slug = slugDoEditor(editor);

        // 1ª mensagem: o editor não tinha nada pendente, então é avisado.
        falarComEditor(cliente, slug, "Topa editar uns Reels?");
        verify(emailSender).send(eq("editor@teste.com"), any());

        reset(emailSender);

        // 2ª e 3ª sem o editor ter aberto: ele já sabe que tem gente esperando.
        // Insistir não acrescenta informação — não pode sair e-mail.
        String conversaId = primeiraConversaDe(cliente);
        responder(cliente, conversaId, "Posso mandar o material hoje.");
        responder(cliente, conversaId, "Ou amanhã, como preferir.");
        verify(emailSender, never()).send(eq("editor@teste.com"), any());

        // O editor abre a conversa — abrir é o que marca como lida.
        mockMvc.perform(get("/v1/me/conversations/" + conversaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(status().isOk());

        // Agora ele voltou ao zero, e a próxima mensagem avisa de novo.
        responder(cliente, conversaId, "Fechou?");
        verify(emailSender, times(1)).send(eq("editor@teste.com"), any());
    }

    @Test
    void aceiteAvisaOEditorPorqueAConversaNasceMuda() throws Exception {
        String cliente = registrar("cliente@teste.com", "Cliente Maria");
        String editor = registrar("editor@teste.com", "Editor Bruno");
        publicarPerfil(editor);

        String jobId = criarPedido(cliente);

        // Candidatar avisa o cliente, que é quem decide.
        mockMvc.perform(post("/v1/job-requests/" + jobId + "/interests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Topo o trampo."}
                                """))
                .andExpect(status().isCreated());
        verify(emailSender).send(eq("cliente@teste.com"), any());

        String meusPedidos = mockMvc.perform(get("/v1/me/job-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andReturn().getResponse().getContentAsString();
        String interestId = JsonPath.read(meusPedidos, "$[0].interests[0].id");

        reset(emailSender);

        mockMvc.perform(post("/v1/me/job-requests/" + jobId + "/interests/" + interestId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente))
                .andExpect(status().isOk());

        // A conversa do aceite nasce sem mensagem: sem este e-mail o editor nunca
        // descobriria que foi escolhido.
        ArgumentCaptor<EmailContent> enviado = ArgumentCaptor.forClass(EmailContent.class);
        verify(emailSender).send(eq("editor@teste.com"), enviado.capture());

        assertThat(enviado.getValue().subject()).contains("Corte de podcast");
        assertThat(enviado.getValue().ctaUrl())
                .as("o link precisa apontar pro frontend, não pra API")
                .isEqualTo("http://localhost:3000/painel/conversas");

        // O texto puro vai junto do HTML no envio real, e é o que aparece em quem bloqueia
        // markup. Se ele parar de sair completo, o e-mail chega vazio pra essa gente.
        assertThat(enviado.getValue().toPlainText())
                .contains("Editor")
                .contains("http://localhost:3000/painel/conversas")
                .endsWith("— TramposEditor");
    }

    @Test
    void falhaDeEnvioNaoDerrubaAAcaoDoUsuario() throws Exception {
        String cliente = registrar("cliente@teste.com", "Cliente Maria");
        String editor = registrar("editor@teste.com", "Editor Bruno");
        publicarPerfil(editor);
        String slug = slugDoEditor(editor);

        org.mockito.Mockito.doThrow(new RuntimeException("SMTP caiu"))
                .when(emailSender).send(any(), any());

        // A mensagem tem que ser salva mesmo assim: perder o aviso é ruim, perder o que
        // a pessoa escreveu é pior.
        falarComEditor(cliente, slug, "Topa editar uns Reels?");
        assertThat(conversationRepository.count()).isEqualTo(1);
    }

    // ---------- Helpers ----------

    private String registrar(String email, String displayName) throws Exception {
        String body = mockMvc.perform(post("/v1/auth/register")
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

    private void falarComEditor(String token, String slug, String texto) throws Exception {
        mockMvc.perform(post("/v1/me/conversations/with-editor/" + slug)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "%s"}
                                """.formatted(texto)))
                .andExpect(status().isCreated());
    }

    private String primeiraConversaDe(String token) throws Exception {
        String body = mockMvc.perform(get("/v1/me/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[0].id");
    }

    private void responder(String token, String conversaId, String texto) throws Exception {
        mockMvc.perform(post("/v1/me/conversations/" + conversaId + "/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "%s"}
                                """.formatted(texto)))
                .andExpect(status().isCreated());
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
}
