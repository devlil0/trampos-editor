package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.repository.*;
import com.devlil0.tramposeditor.notification.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "tramposeditor.notifications.base-url=https://teste.example.com")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RequiredEmailTest {
    @Autowired MockMvc mvc;
    @Autowired IUserRepository users;
    @Autowired IPendingSignupRepository pending;
    @Autowired IPasswordResetTokenRepository resets;
    @Autowired IEmailVerificationTokenRepository verifications;
    @MockitoBean EmailSender sender;

    @BeforeEach
    void clean() {
        resets.deleteAll();
        verifications.deleteAll();
        pending.deleteAll();
        users.deleteAll();
        reset(sender);
    }

    private ResultActions start() throws Exception {
        return mvc.perform(post("/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"t01@example.com","password":"senhaforte123","displayName":"Teste",
                 "username":"testet01","roles":["CLIENT"]}
                """));
    }

    private String account() throws Exception {
        start().andExpect(status().isAccepted());
        assertThat(users.findByEmail("t01@example.com")).isEmpty();
        String body = mvc.perform(post("/v1/auth/register/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"t01@example.com\",\"code\":\"000000\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        reset(sender);
        return JsonPath.read(body, "$.accessToken");
    }

    @Test
    void falhaNoCadastroRetorna503SemCriarContaEPodeReiniciar() throws Exception {
        doThrow(new IllegalStateException("938471 token=privado")).when(sender).send(any(), any());
        start().andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Não foi possível enviar o e-mail agora. Tente novamente mais tarde."));
        assertThat(users.count()).isZero();
        assertThat(pending.count()).isZero();
        reset(sender);
        account();
    }

    @Test
    void falhaNoReenvioNaoConfirmaEnvioNemDestroiPendencia() throws Exception {
        start().andExpect(status().isAccepted());
        String hash = pending.findByEmail("t01@example.com").orElseThrow().getCodeHash();
        doThrow(new IllegalStateException("falha simulada")).when(sender).send(any(), any());
        mvc.perform(post("/v1/auth/register/resend").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"t01@example.com\"}")).andExpect(status().isServiceUnavailable());
        assertThat(pending.findByEmail("t01@example.com").orElseThrow().getCodeHash()).isEqualTo(hash);
        assertThat(users.count()).isZero();
    }

    @Test
    void resetMantemPrivacidadeEReverteTokenNaFalha() throws Exception {
        account();
        mvc.perform(post("/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"t01@example.com\"}")).andExpect(status().isNoContent());
        String original = resets.findAll().getFirst().getTokenHash();
        doThrow(new IllegalStateException("token=privado")).when(sender).send(any(), any());
        for (String email : new String[]{"t01@example.com", "inexistente@example.com"}) {
            mvc.perform(post("/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\"}"))
                    .andExpect(status().isNoContent()).andExpect(content().string(""));
        }
        assertThat(resets.findAll()).hasSize(1);
        assertThat(resets.findAll().getFirst().getTokenHash()).isEqualTo(original);
    }

    @Test
    void resetEVerificacaoUsamUrlPublicaEVerificacaoFalhaCom503() throws Exception {
        String jwt = account();
        mvc.perform(post("/v1/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"t01@example.com\"}")).andExpect(status().isNoContent());
        var content = ArgumentCaptor.forClass(EmailContent.class);
        verify(sender).send(any(), content.capture());
        assertThat(content.getValue().toPlainText()).contains("https://teste.example.com/redefinir-senha?token=")
                .doesNotContain("localhost");
        var user = users.findByEmail("t01@example.com").orElseThrow();
        user.setEmailVerifiedAt(null);
        users.save(user);
        reset(sender);
        mvc.perform(post("/v1/users/me/email-verification/resend").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());
        verify(sender).send(any(), content.capture());
        assertThat(content.getValue().toPlainText()).contains("https://teste.example.com/confirmar-email?token=");
        String original = verifications.findAll().getFirst().getTokenHash();
        doThrow(new IllegalStateException("falha simulada")).when(sender).send(any(), any());
        mvc.perform(post("/v1/users/me/email-verification/resend").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isServiceUnavailable());
        assertThat(verifications.findAll()).hasSize(1);
        assertThat(verifications.findAll().getFirst().getTokenHash()).isEqualTo(original);
    }
}