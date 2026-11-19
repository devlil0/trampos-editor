package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.model.PasswordResetTokenEntity;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.model.UserIdentityEntity;
import com.devlil0.tramposeditor.database.repository.IPasswordResetTokenRepository;
import com.devlil0.tramposeditor.database.repository.IUserIdentityRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.PasswordChangeDTO;
import com.devlil0.tramposeditor.dto.PasswordResetConfirmDTO;
import com.devlil0.tramposeditor.enums.UserRole;
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.exception.UnauthorizedException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "test.auth-limits.enabled=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountSettingsTest {
    private static final String CURRENT = "senha-ficticia-atual";
    private static final String NEW = "senha-ficticia-nova";
    private static final String RESET = "senha-ficticia-reset";
    private static final String PASSWORD = "/v1/users/me/password";
    private static final String DISPLAY_NAME = "/v1/users/me/display-name";

    @Autowired MockMvc mvc;
    @Autowired IUserRepository users;
    @Autowired IUserIdentityRepository identities;
    @Autowired IPasswordResetTokenRepository resetTokens;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired PasswordChangeService change;
    @Autowired PasswordResetService reset;
    @Autowired TransactionTemplate transactions;
    @Autowired EntityManager entityManager;

    private final List<UUID> testUsers = new ArrayList<>();

    @AfterEach
    void clean() {
        users.deleteAllById(testUsers);
    }

    @Test
    void somenteTitularEditaNomeEPerfilPublicoRefleteAEdicao() throws Exception {
        var owner = account(CURRENT, true);
        var other = account(CURRENT, true);
        String token = bearer(owner);
        var name = """
                {"displayName":"  Novo nome  "}
                """;
        mvc.perform(put(DISPLAY_NAME).contentType(MediaType.APPLICATION_JSON).content(name))
                .andExpect(status().isUnauthorized());
        mvc.perform(put(DISPLAY_NAME).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Novo nome"))
                .andExpect(jsonPath("$.instagram").value("instagram.anterior"))
                .andExpect(jsonPath("$.username").value(owner.getUsername()));
        mvc.perform(get("/v1/profiles/" + owner.getUsername()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Novo nome"));
        assertThat(reload(other).getDisplayName()).isEqualTo("Nome anterior");

        mvc.perform(patch("/v1/users/me").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"instagram\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Novo nome"));
        assertThat(reload(owner).getInstagram()).isNull();

        String username = owner.getUsername() + "-novo";
        mvc.perform(put("/v1/users/me/username").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"" + username + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.displayName").value("Novo nome"));
    }

    @Test
    void nomeInvalidoRecebe400SemAlterarConta() throws Exception {
        var user = account(CURRENT, true);
        String token = bearer(user);
        for (String body : List.of("{}", "{\"displayName\":\"   \"}", "{\"displayName\":\"" + "a".repeat(81) + "\"}")) {
            mvc.perform(put(DISPLAY_NAME).header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isArray());
        }
        assertThat(reload(user).getDisplayName()).isEqualTo(user.getDisplayName());
    }

    @Test
    void inativoNaoPodeEditarNomeOuSenha() throws Exception {
        var user = account(CURRENT, false);
        String token = bearer(user);
        mvc.perform(put(DISPLAY_NAME).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"Novo\"}"))
                .andExpect(status().isUnauthorized());
        password(token, CURRENT, NEW).andExpect(status().isUnauthorized());
        assertUnchanged(user);
        assertThat(reload(user).getDisplayName()).isEqualTo(user.getDisplayName());
    }

    @Test
    void falhasPreservamHashSessaoEReset() throws Exception {
        var user = account(CURRENT, true);
        String pendingReset = resetLink(user);
        String token = bearer(user);
        mvc.perform(post(PASSWORD).contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody(CURRENT, NEW)))
                .andExpect(status().isUnauthorized());
        password(token, "senha-ficticia-incorreta", NEW).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Senha atual incorreta."));
        for (String invalid : List.of("curta", " ".repeat(8), "a".repeat(73), "é".repeat(37), "😃".repeat(4))) {
            password(token, CURRENT, invalid).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").isArray());
        }
        assertUnchanged(user);
        assertThat(resetTokens.findByTokenHash(TokenSecrets.hash(pendingReset))).isPresent();
        me(token).andExpect(status().isOk());
    }

    @Test
    void trocaRevogaDoisJwtsRemoveResetEAceitaNovaSenha() throws Exception {
        var user = account(CURRENT, true);
        String pendingReset = resetLink(user);
        String first = bearer(user);
        String second = bearer(user);
        assertThat(first).isNotEqualTo(second);
        me(first).andExpect(status().isOk());
        me(second).andExpect(status().isOk());

        String newPasswordAtLimit = "é".repeat(36);
        password(first, CURRENT, newPasswordAtLimit).andExpect(status().isNoContent()).andExpect(content().string(""));
        me(first).andExpect(status().isUnauthorized());
        me(second).andExpect(status().isUnauthorized());
        login(user, CURRENT).andExpect(status().isUnauthorized());
        login(user, newPasswordAtLimit).andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
        assertThat(reload(user).getSessionVersion()).isEqualTo(user.getSessionVersion() + 1);
        assertThat(resetTokens.findByTokenHash(TokenSecrets.hash(pendingReset))).isEmpty();
        assertThatThrownBy(() -> reset.confirm(new PasswordResetConfirmDTO(pendingReset, RESET)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void contaSocialUsaRecuperacaoParaPrimeiraSenhaEPreservaVinculo() throws Exception {
        var user = account(null, true);
        var identity = identities.saveAndFlush(UserIdentityEntity.builder().user(user).provider("github")
                .subject(UUID.randomUUID().toString()).build());
        password(bearer(user), CURRENT, NEW).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Esta conta ainda não tem senha. Use a recuperação de senha para definir a primeira."));
        assertUnchanged(user);

        reset.confirm(new PasswordResetConfirmDTO(resetLink(user), CURRENT));
        password(bearer(reload(user)), CURRENT, NEW).andExpect(status().isNoContent());
        login(user, NEW).andExpect(status().isOk());
        assertThat(identities.findByProviderAndSubject("github", identity.getSubject())).isPresent();
    }

    @Test
    void limiteDaTrocaTemCotaPropriaESinalizaRetryAfter() throws Exception {
        var user = account(CURRENT, true);
        String token = bearer(user);
        for (int i = 0; i < 5; i++) {
            password(token, "senha-ficticia-incorreta", NEW).andExpect(status().isForbidden());
        }
        password(token, CURRENT, NEW).andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        login(user, CURRENT).andExpect(status().isOk());
        assertUnchanged(user);
    }

    @Test
    void rollbackReverteSenhaVersaoEInvalidacaoDoReset() {
        var user = account(CURRENT, true);
        String pendingReset = resetLink(user);
        var failure = new IllegalStateException("falha simulada ao concluir a transação");

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            changeFor(user);
            entityManager.flush();
            throw failure;
        })).isSameAs(failure);

        assertUnchanged(user);
        assertThat(resetTokens.findByTokenHash(TokenSecrets.hash(pendingReset))).isPresent();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void trocaEResetSerializamPelaConta(boolean changeFirst) throws Exception {
        var user = account(CURRENT, true);
        String token = resetLink(user);
        Runnable changePassword = () -> changeFor(user);
        Runnable resetPassword = () -> reset.confirm(new PasswordResetConfirmDTO(token, RESET));

        if (changeFirst) {
            compete(changePassword, resetPassword, BusinessException.class);
        } else {
            compete(resetPassword, changePassword, UnauthorizedException.class);
        }

        var updated = reload(user);
        assertThat(updated.getSessionVersion()).isEqualTo(user.getSessionVersion() + 1);
        assertThat(encoder.matches(changeFirst ? NEW : RESET, updated.getPasswordHash())).isTrue();
        if (changeFirst) {
            assertThat(resetTokens.findByTokenHash(TokenSecrets.hash(token))).isEmpty();
        } else {
            assertThat(resetTokens.findByTokenHash(TokenSecrets.hash(token)).orElseThrow().getUsedAt()).isNotNull();
        }
    }

    private void compete(Runnable winner, Runnable loser, Class<? extends RuntimeException> expected) throws Exception {
        var applied = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var competing = new CountDownLatch(1);

        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> transactions.executeWithoutResult(status -> {
                winner.run();
                applied.countDown();
                await(release);
            }));
            try {
                await(applied);
                var second = workers.submit(() -> {
                    competing.countDown();
                    assertThatThrownBy(loser::run).isInstanceOf(expected);
                });
                await(competing);
                assertThatThrownBy(() -> second.get(500, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                release.countDown();
                first.get(10, TimeUnit.SECONDS);
                second.get(10, TimeUnit.SECONDS);
            } finally {
                release.countDown();
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private void changeFor(UserEntity user) {
        change.change(user.getId(), user.getSessionVersion(), new PasswordChangeDTO(CURRENT, NEW), "192.0.2.20");
    }

    private UserEntity account(String password, boolean active) {
        String name = "t08-" + UUID.randomUUID().toString().substring(0, 12);
        var user = users.saveAndFlush(UserEntity.builder().email(name + "@example.invalid").username(name)
                .displayName("Nome anterior").instagram("instagram.anterior").active(active)
                .emailVerifiedAt(LocalDateTime.now()).passwordHash(password == null ? null : encoder.encode(password))
                .roles(Set.of(UserRole.CLIENT)).build());
        testUsers.add(user.getId());
        return user;
    }

    private UserEntity reload(UserEntity user) {
        return users.findById(user.getId()).orElseThrow();
    }

    private void assertUnchanged(UserEntity user) {
        var after = reload(user);
        assertThat(after.getPasswordHash()).isEqualTo(user.getPasswordHash());
        assertThat(after.getSessionVersion()).isEqualTo(user.getSessionVersion());
    }

    private String resetLink(UserEntity user) {
        String token = UUID.randomUUID().toString();
        resetTokens.saveAndFlush(PasswordResetTokenEntity.builder().user(user)
                .tokenHash(TokenSecrets.hash(token)).expiresAt(LocalDateTime.now().plusMinutes(10)).build());
        return token;
    }

    private String bearer(UserEntity user) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().subject(user.getId().toString()).id(UUID.randomUUID().toString())
                .issuedAt(now).expiresAt(now.plusSeconds(180))
                .claim(JwtService.SESSION_VERSION_CLAIM, user.getSessionVersion())
                .claim("roles", List.of("CLIENT")).build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private ResultActions me(String token) throws Exception {
        return mvc.perform(get("/v1/users/me").header("Authorization", token));
    }

    private ResultActions password(String token, String current, String next) throws Exception {
        return mvc.perform(post(PASSWORD).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(passwordBody(current, next)));
    }

    private String passwordBody(String current, String next) {
        return """
                {"currentPassword":"%s","newPassword":"%s"}
                """.formatted(current, next);
    }

    private ResultActions login(UserEntity user, String password) throws Exception {
        return mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(user.getEmail(), password)));
    }
}
