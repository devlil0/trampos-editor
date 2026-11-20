package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.config.AuthClientOrigin;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IPendingSignupRepository;
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
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.devlil0.tramposeditor.TestcontainersConfiguration.CODIGO_DE_CADASTRO;
import static org.assertj.core.api.Assertions.assertThat;
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
    @Autowired IPendingSignupRepository pendings;
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

    @Test
    void rajadaConcorrenteAplicaCotaPadraoSemAlterarPendenciaBloqueada() throws Exception {
        String peer = "198.51.100.50";
        String address = "volume-confirm@teste.com";
        startSignup(address, "volume-confirm", peer).andExpect(status().isAccepted());
        confirm(address, "111111", peer).andExpect(status().isConflict());

        var start = new CountDownLatch(1);
        var results = new ArrayList<Future<Integer>>();
        try (var pool = Executors.newFixedThreadPool(12)) {
            for (int i = 0; i < 47; i++) {
                int index = i;
                results.add(pool.submit(() -> {
                    start.await();
                    return confirm("ausente-" + index + "@teste.com", "111111", peer,
                            "203.0.113." + (index + 1)).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            var statuses = new ArrayList<Integer>();
            for (var result : results) {
                statuses.add(result.get(30, TimeUnit.SECONDS));
            }
            assertThat(statuses).containsOnly(409, 429);
            assertThat(statuses.stream().filter(value -> value == 409).count()).isEqualTo(39);
            assertThat(statuses.stream().filter(value -> value == 429).count()).isEqualTo(8);
        }

        var before = pendings.findByEmail(address).orElseThrow();
        assertThat(before.getAttempts()).isEqualTo((short) 1);
        limit(confirm(address, "111111", peer, "203.0.113.99"), 300);
        limit(confirm(address, CODIGO_DE_CADASTRO, peer), 300);
        assertThat(pendings.findByEmail(address).orElseThrow())
                .usingRecursiveComparison().isEqualTo(before);
        assertThat(users.findByEmail(address)).isEmpty();

        startSignup("volume-outro@teste.com", "volume-outro", peer).andExpect(status().isAccepted());
        advance(299);
        limit(confirm(address, CODIGO_DE_CADASTRO, peer), 1);
        advance(1);
        confirm(address, CODIGO_DE_CADASTRO, peer).andExpect(status().isCreated());
        assertThat(pendings.findByEmail(address)).isEmpty();
        assertThat(users.findByEmail(address))
                .hasValueSatisfying(user -> assertThat(user.isEmailVerified()).isTrue());
    }

    @Test
    void confirmacaoAgrupaHeadersInvalidosNoParEPreservaCincoErrosPorPendencia() throws Exception {
        String proxy = "192.0.2.1";
        String client = "203.0.113.50";
        String address = "cinco-confirm@teste.com";
        startSignup(address, "cinco-confirm", proxy).andExpect(status().isAccepted());
        for (int i = 0; i < 40; i++) {
            confirm("sem-pendencia@teste.com", "111111", proxy).andExpect(status().isConflict());
        }
        limit(confirm(address, "111111", proxy, "invalid"), 300);
        limit(confirm(address, "111111", proxy, client, client), 300);
        limit(confirm(address, "111111", proxy, client + ", 203.0.113.51"), 300);
        assertThat(pendings.findByEmail(address).orElseThrow().getAttempts()).isZero();

        for (int attempt = 1; attempt <= 4; attempt++) {
            confirm(address, "111111", proxy, client).andExpect(status().isConflict());
            assertThat(pendings.findByEmail(address).orElseThrow().getAttempts()).isEqualTo((short) attempt);
        }
        confirm(address, "111111", proxy, "203.0.113.51").andExpect(status().isConflict());
        assertThat(pendings.findByEmail(address)).isEmpty();
        confirm(address, CODIGO_DE_CADASTRO, proxy, client).andExpect(status().isConflict());
        assertThat(users.findByEmail(address)).isEmpty();
    }

    private ResultActions startSignup(String address, String username, String peer) throws Exception {
        return mvc.perform(post("/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .with(request -> {
                    request.setRemoteAddr(peer);
                    return request;
                })
                .content("""
                        {"email":"%s","password":"senha-ficticia","displayName":"Pessoa",
                         "username":"%s","roles":["CLIENT"]}
                        """.formatted(address, username)));
    }

    private ResultActions confirm(String address, String code, String peer, String... origins) throws Exception {
        var request = post("/v1/auth/register/confirm").contentType(MediaType.APPLICATION_JSON)
                .with(mock -> {
                    mock.setRemoteAddr(peer);
                    return mock;
                })
                .content("""
                        {"email":"%s","code":"%s"}
                        """.formatted(address, code));
        for (String origin : origins) {
            request.header(AuthClientOrigin.CLIENT_IP_HEADER, origin);
        }
        return mvc.perform(request);
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
