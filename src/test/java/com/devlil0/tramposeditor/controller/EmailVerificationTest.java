package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.repository.IEditorProfileRepository;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verificação de e-mail (ESBOCO §2, fase 1.5).
 * <p>
 * O que estes testes prendem é <b>o quanto</b> a falta de verificação bloqueia — porque a
 * tentação, aqui, é sempre bloquear mais. A régua é uma só: só entra na lista o que alcança
 * outra pessoa. Se alguém um dia exigir e-mail confirmado pra conversar ou pra publicar
 * pedido, eles quebram, e isso é de propósito: seria fechar a porta aberta do
 * {@code FUNDACAO} §11.9 por burocracia.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EmailVerificationTest {

    private static final Pattern TOKEN_NO_LINK = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IEditorProfileRepository editorProfileRepository;

    @MockitoBean
    private EmailSender emailSender;

    @BeforeEach
    void limpar(){
        editorProfileRepository.deleteAll();
        userRepository.deleteAll();
        reset(emailSender);
    }

    // ---------- O ciclo ----------

    @Test
    void cadastroPorSenhaNasceNaoVerificadoEJaRecebeOLink() throws Exception {
        String token = registrar("novo@teste.com");

        mockMvc.perform(get("/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(false));

        ArgumentCaptor<EmailContent> enviado = ArgumentCaptor.forClass(EmailContent.class);
        verify(emailSender).send(eq("novo@teste.com"), enviado.capture());
        assertThat(enviado.getValue().subject()).isEqualTo("Confirme seu e-mail");
    }

    @Test
    void oLinkConfirmaEValeUmaVezSo() throws Exception {
        String sessao = registrar("novo@teste.com");
        String token = capturarTokenDoEmail("novo@teste.com");

        confirmar(token).andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(true));

        // Uso único: o mesmo link de novo não vale.
        confirmar(token).andExpect(status().isConflict());
    }

    @Test
    void linkInventadoNaoConfirmaNinguem() throws Exception {
        registrar("novo@teste.com");

        confirmar("token-que-nunca-existiu").andExpect(status().isConflict());
    }

    @Test
    void reenviarInvalidaOLinkAnterior() throws Exception {
        String sessao = registrar("novo@teste.com");
        String primeiro = capturarTokenDoEmail("novo@teste.com");

        reset(emailSender);
        mockMvc.perform(post("/v1/users/me/email-verification/resend")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sessao))
                .andExpect(status().isNoContent());

        String segundo = capturarTokenDoEmail("novo@teste.com");
        assertThat(segundo).isNotEqualTo(primeiro);

        // Um link vivo por conta: o antigo morreu quando o novo nasceu.
        confirmar(primeiro).andExpect(status().isConflict());
        confirmar(segundo).andExpect(status().isNoContent());
    }

    // ---------- O que a falta de verificação bloqueia ----------

    @Test
    void semConfirmarNaoPublicaPerfilDeEditor() throws Exception {
        String sessao = registrar("editor@teste.com");

        // Publicar perfil é entrar na vitrine indexada — é o único gesto que a falta de
        // verificação impede.
        publicarPerfil(sessao).andExpect(status().isConflict());

        confirmar(capturarTokenDoEmail("editor@teste.com")).andExpect(status().isNoContent());

        publicarPerfil(sessao).andExpect(status().isOk());
    }

    @Test
    void semConfirmarNaoRecebeAvisoAutomatico() throws Exception {
        String editor = registrar("editor@teste.com");
        confirmar(capturarTokenDoEmail("editor@teste.com"));
        publicarPerfil(editor).andExpect(status().isOk());
        String slug = slugDoEditor(editor);

        // O cliente NÃO confirma: ele age normalmente, mas não pode ser destinatário de
        // aviso automático — endereço não provado é o caminho curto pra reclamação de spam,
        // que custa a reputação do domínio inteiro.
        String cliente = registrar("cliente@teste.com");
        reset(emailSender);

        // Conversar continua liberado pra quem não confirmou: o que se bloqueia é o e-mail
        // SAIR pra ele, não a pessoa usar o produto.
        mockMvc.perform(post("/v1/me/conversations/with-editor/" + slug)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body": "Topa editar uns Reels?"}
                                """))
                .andExpect(status().isCreated());

        // O editor confirmou, então ele recebe.
        verify(emailSender).send(eq("editor@teste.com"), any());
        // E o cliente não verificado não recebe nada de automático.
        verify(emailSender, never()).send(eq("cliente@teste.com"), any());
    }

    // ---------- Helpers ----------

    private org.springframework.test.web.servlet.ResultActions confirmar(String token) throws Exception {
        return mockMvc.perform(post("/v1/auth/email-verification/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token": "%s"}
                        """.formatted(token)));
    }

    private org.springframework.test.web.servlet.ResultActions publicarPerfil(String token) throws Exception {
        return mockMvc.perform(put("/v1/me/editor-profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"niches": ["REELS"], "tools": ["PREMIERE"]}
                        """));
    }

    private String capturarTokenDoEmail(String email) throws Exception {
        ArgumentCaptor<EmailContent> enviado = ArgumentCaptor.forClass(EmailContent.class);
        verify(emailSender).send(eq(email), enviado.capture());

        Matcher matcher = TOKEN_NO_LINK.matcher(enviado.getValue().ctaUrl());
        assertThat(matcher.find()).as("o e-mail precisa carregar o link com o token").isTrue();
        return matcher.group(1);
    }

    private String slugDoEditor(String token) throws Exception {
        String body = mockMvc.perform(get("/v1/me/editor-profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.slug");
    }

    private String registrar(String email) throws Exception {
        String body = mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "senhaforte123",
                                  "displayName": "Fulano de Teste",
                                  "username": "u-%s",
                                  "roles": ["CLIENT"]
                                }
                                """.formatted(email, email.split("@")[0])))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$.accessToken");
    }
}
