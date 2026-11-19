package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.repository.IConversationRepository;
import com.devlil0.tramposeditor.database.repository.IJobRequestRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.MessageCreateDTO;
import com.devlil0.tramposeditor.notification.EmailContent;
import com.devlil0.tramposeditor.service.ConversationService;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
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
@SpringBootTest(properties = "tramposeditor.notifications.notice-workers=1")
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

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private TransactionTemplate transactions;

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
        verify(emailSender, timeout(3000)).send(eq("editor@teste.com"), any());

        reset(emailSender);

        // 2ª e 3ª sem o editor ter aberto: ele já sabe que tem gente esperando.
        // Insistir não acrescenta informação — não pode sair e-mail.
        String conversaId = primeiraConversaDe(cliente);
        responder(cliente, conversaId, "Posso mandar o material hoje.");
        responder(cliente, conversaId, "Ou amanhã, como preferir.");
        verify(emailSender, after(150).never()).send(eq("editor@teste.com"), any());

        // O editor abre a conversa — abrir é o que marca como lida.
        mockMvc.perform(get("/v1/me/conversations/" + conversaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(status().isOk());

        // Agora ele voltou ao zero, e a próxima mensagem avisa de novo.
        responder(cliente, conversaId, "Fechou?");
        verify(emailSender, timeout(3000).times(1)).send(eq("editor@teste.com"), any());
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
        verify(emailSender, timeout(3000)).send(eq("cliente@teste.com"), any());

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
        verify(emailSender, timeout(3000)).send(eq("editor@teste.com"), enviado.capture());

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

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        doAnswer(invocation -> {
            started.countDown();
            try {
                assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
                throw new IllegalStateException("falha fictícia no SMTP");
            } finally {
                finished.countDown();
            }
        }).when(emailSender).send(any(), any());

        try {
            falarComEditor(cliente, slug, "Topa editar uns Reels?");
            assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(finished.getCount()).isEqualTo(1);
            assertThat(conversationRepository.count()).isEqualTo(1);
        } finally {
            release.countDown();
        }
        assertThat(finished.await(3, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void rollbackDaConversaNaoEnviaAviso() throws Exception {
        registrar("cliente@teste.com", "Cliente Maria");
        String editor = registrar("editor@teste.com", "Editor Bruno");
        publicarPerfil(editor);
        String slug = slugDoEditor(editor);
        var clienteId = userRepository.findByEmail("cliente@teste.com").orElseThrow().getId();

        transactions.executeWithoutResult(status -> {
            conversationService.startWithEditor(clienteId, slug, new MessageCreateDTO("Mensagem revertida."));
            verify(emailSender, after(150).never()).send(any(), any());
            status.setRollbackOnly();
        });

        assertThat(conversationRepository.count()).isZero();
        conversationService.startWithEditor(clienteId, slug, new MessageCreateDTO("Mensagem confirmada."));
        verify(emailSender, timeout(3000).times(1)).send(eq("editor@teste.com"), any());
        verify(emailSender, after(150).times(1)).send(any(), any());
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

        // O cadastro dispara o e-mail de confirmação (V17). Zerar aqui deixa cada teste
        // contando só os avisos que ELE provocou — senão todo verify() acusaria uma chamada
        // a mais, vinda do setup.
        reset(emailSender);



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
    /** O clique no link de confirmação, sem passar pelo e-mail. Ver o helper de cadastro. */
    private void confirmarEmail(String email){
        var user = userRepository.findByEmail(email.trim().toLowerCase()).orElseThrow();
        user.setEmailVerifiedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

}
