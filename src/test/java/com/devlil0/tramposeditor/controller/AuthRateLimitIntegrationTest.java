package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.config.AuthClientOrigin;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.notification.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "test.auth-limits.enabled=true",
        "tramposeditor.auth-limits.trusted-proxies[0]=192.0.2.1",
        "tramposeditor.auth-limits.login.per-origin=1",
        "tramposeditor.auth-limits.login.window=10s"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthRateLimitIntegrationTest {
    private static final AtomicReference<Instant> TIME = new AtomicReference<>(Instant.parse("2025-01-01T00:00:00Z"));
    private static final String RESET = "/v1/auth/password-reset/request";
    private static final String RESEND = "/v1/auth/register/resend";

    @Autowired MockMvc mvc;
    @Autowired IUserRepository users;
    @MockitoBean EmailSender sender;
    @MockitoBean(name = "authRateLimitClock") Clock clock;

    @BeforeEach
    void configureClock() {
        advance(3600);
        when(clock.instant()).thenAnswer(invocation -> TIME.get());
    }

    @Test
    void inicioEReenvioCompartilhamCooldownSemDuplicarEmail() throws Exception {
        mvc.perform(post("/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"pendente@teste.com","password":"senha-ficticia","displayName":"Pessoa",
                 "username":"pendente-t05","roles":["CLIENT"]}
                """)).andExpect(status().isAccepted());

        limit(email(RESEND, "Pendente@teste.com"), 60);
        verify(sender, times(1)).send(any(), any());
        advance(60);
        email(RESEND, "pendente@teste.com").andExpect(status().isNoContent());
        verify(sender, times(2)).send(any(), any());
    }

    @Test
    void resetMantemRespostaIgualParaContaAtivaInativaOuAusente() throws Exception {
        saveUser("ativa", true);
        saveUser("inativa", false);
        for (String username : new String[]{"ativa", "inativa", "ausente"}) {
            String email = username + "@teste.com";
            email(RESET, email).andExpect(status().isNoContent());
            limit(email(RESET, email), 60);
        }
        verify(sender).send(any(), any());
        verifyNoMoreInteractions(sender);
    }

    @Test
    void headerForjadoNaoMudaOrigemDiretaEProxyConfiavelDistingueClientes() throws Exception {
        login("direto-a", "198.51.100.1", "203.0.113.1").andExpect(status().isUnauthorized());
        limit(login("direto-b", "198.51.100.1", "203.0.113.2"), 10);

        login("proxy-a", "192.0.2.1", "203.0.113.1").andExpect(status().isUnauthorized());
        login("proxy-b", "192.0.2.1", "203.0.113.2").andExpect(status().isUnauthorized());
        limit(login("proxy-c", "192.0.2.1", "203.0.113.1"), 10);
        advance(10);
        login("proxy-c", "192.0.2.1", "203.0.113.1").andExpect(status().isUnauthorized());
    }

    @Test
    void cotaPadraoDeOitoLoginsPorContaValeEntreOrigensDiferentes() throws Exception {
        for (int i = 1; i <= 8; i++) {
            login("mesma-conta", "198.51.100." + i, "203.0.113.1")
                    .andExpect(status().isUnauthorized());
        }
        limit(login("mesma-conta", "198.51.100.9", "203.0.113.1"), 10);
        advance(10);
        login("mesma-conta", "198.51.100.9", "203.0.113.1").andExpect(status().isUnauthorized());
    }

    private void saveUser(String username, boolean active) {
        users.save(UserEntity.builder().email(username + "@teste.com").username(username)
                .displayName(username).active(active).build());
    }

    private ResultActions email(String path, String email) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email)));
    }

    private ResultActions login(String name, String peer, String claimed) throws Exception {
        return mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .with(request -> {
                    request.setRemoteAddr(peer);
                    return request;
                })
                .header(AuthClientOrigin.CLIENT_IP_HEADER, claimed)
                .header("X-Forwarded-For", claimed)
                .header("Forwarded", "for=" + claimed)
                .content("{\"email\":\"%s@teste.com\",\"password\":\"ficticia\"}".formatted(name)));
    }

    private void limit(ResultActions response, int seconds) throws Exception {
        response.andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", Integer.toString(seconds)))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.error").value("Muitas tentativas. Aguarde e tente novamente."));
    }

    private void advance(long seconds) {
        TIME.updateAndGet(now -> now.plusSeconds(seconds));
    }
}
