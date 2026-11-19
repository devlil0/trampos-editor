package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.model.ConversationEntity;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IConversationRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.enums.UserRole;
import com.devlil0.tramposeditor.notification.EmailSender;
import com.devlil0.tramposeditor.service.AvatarService;
import com.devlil0.tramposeditor.service.JwtService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "test.auth-limits.enabled=true"})
@ExtendWith(OutputCaptureExtension.class)
@Import({TestcontainersConfiguration.class, ApiEdgeErrorsTest.ConstraintFixture.class})
class ApiEdgeErrorsTest {
    private static final String CONTESTED_USERNAME = "t07-nome-concorrente";
    private static final String BOUNDARY = "t07-upload-ficticio";
    private static final int MIB = 1024 * 1024;
    private static final String INVALID_VALUE = "valor-ficticio-t07";

    @LocalServerPort int port;
    @Autowired IUserRepository users;
    @Autowired JwtService jwtService;
    @Autowired AvatarService avatars;
    @Autowired IConversationRepository conversations;
    @MockitoBean EmailSender sender;

    private final HttpClient http = HttpClient.newHttpClient();
    private UserEntity actor;

    @BeforeEach
    void criarContaDeTeste() {
        actor = users.saveAndFlush(newUser("t07-ator@example.invalid", "t07-ator"));
    }

    @AfterEach
    void limparContasDeTeste() {
        users.findByUsername(CONTESTED_USERNAME).ifPresent(users::delete);
        users.deleteById(actor.getId());
    }

    @Test
    void uuidInvalidoRetorna400ComErroComum() throws Exception {
        var request = authenticated("/v1/me/conversations/" + INVALID_VALUE).GET().build();
        assertCommonError(send(request), 400);
    }

    @Test
    void enumInvalidoNaVitrineRetorna400ComErroComum() throws Exception {
        assertCommonError(send(request("/v1/editors?niche=" + INVALID_VALUE).GET().build()), 400);
    }

    @Test
    void enumInvalidoNoDtoRetorna400ComErroComum() throws Exception {
        assertCommonError(send(jsonPost("/v1/auth/register", """
                {"email":"t07-dto@example.invalid","password":"senha-ficticia","displayName":"Teste",
                 "username":"t07-dto","roles":["valor-ficticio-t07"]}
                """)), 400);
    }

    @Test
    void jsonMalformadoRetorna400SemEcoarConteudo() throws Exception {
        assertCommonError(send(jsonPost("/v1/auth/login", """
                {"email":"t07-json@example.invalid","password":"valor-ficticio-t07"
                """)), 400);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void uploadAcimaDoLimiteRetorna413ComErroComum(boolean limiteTotal) throws Exception {
        byte[] parts = limiteTotal ? multipart(3 * MIB + 1, 3 * MIB + 1) : multipart(5 * MIB + 1);
        var request = authenticated("/v1/me/avatar")
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(parts)).build();
        assertCommonError(send(request), 413);
    }

    @Test
    void servicoDeAvatarUsaOTratamento413ParaSeuTeto() {
        var file = new MockMultipartFile("file", "ficticio.png", "image/png", new byte[5 * MIB + 1]);
        assertThatThrownBy(() -> avatars.replace(actor.getId(), file))
                .isInstanceOf(MaxUploadSizeExceededException.class);
    }

    @Test
    void duplicidadeConcorrenteRetornaUm201EUm409(CapturedOutput output) throws Exception {
        int logStart = output.getAll().length();
        var first = http.sendAsync(authenticated("/__test/t07/duplicate/1")
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        var second = http.sendAsync(authenticated("/__test/t07/duplicate/2")
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());

        var responses = java.util.List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        assertThat(responses).extracting(HttpResponse::statusCode).containsExactlyInAnyOrder(201, 409);
        assertCommonError(responses.stream().filter(response -> response.statusCode() == 409).findFirst().orElseThrow(), 409);
        assertThat(users.findByUsername(CONTESTED_USERNAME)).isPresent();
        assertThat(output.getAll().substring(logStart)).doesNotContain(CONTESTED_USERNAME, "uk_users_username");
    }

    @Test
    void restricaoNaoNulaContinuaFalhaInternaSemDetalhesSql(CapturedOutput output) throws Exception {
        int logStart = output.getAll().length();
        var response = send(authenticated("/__test/t07/not-null/" + actor.getId())
                .POST(HttpRequest.BodyPublishers.noBody()).build());

        assertCommonError(response, 500);
        assertThat(response.body()).doesNotContain("display_name", "NOT NULL");
        assertThat(output.getAll().substring(logStart)).contains("SQLSTATE=23502")
                .doesNotContain("display_name", "update users", actor.getId().toString());
        assertThat(users.findById(actor.getId()).orElseThrow().getDisplayName()).isEqualTo("Conta de teste");
    }

    @Test
    void unicidadeDesconhecidaContinuaFalhaInterna(CapturedOutput output) throws Exception {
        int logStart = output.getAll().length();
        var response = send(authenticated("/__test/t07/unknown-unique/" + actor.getId())
                .POST(HttpRequest.BodyPublishers.noBody()).build());

        assertCommonError(response, 500);
        assertThat(output.getAll().substring(logStart)).contains("SQLSTATE=23505")
                .doesNotContain("pk_users", "insert into users", "t07-chave@example.invalid", actor.getId().toString());
    }

    @Test
    void despachoInternoPreservaErroSemLiberarRequisicaoDireta() throws Exception {
        assertThat(send(request("/v1/auth/__test/t07/error").GET().build()).statusCode()).isEqualTo(500);
        assertThat(send(request("/error").GET().build()).statusCode()).isEqualTo(401);
        assertThat(send(request("/v1/me").GET().build()).statusCode()).isEqualTo(401);

        var invalidBearer = request("/v1/editors?niche=" + INVALID_VALUE)
                .header("Authorization", "Bearer token-ficticio-invalido").GET().build();
        var unauthorized = send(invalidBearer);
        assertThat(unauthorized.statusCode()).isEqualTo(401);
        assertThat(unauthorized.headers().firstValue("WWW-Authenticate").orElse("")).startsWith("Bearer");
    }

    @Test
    void usuarioSemAcessoAConversaContinuaRecebendo403() throws Exception {
        var client = users.saveAndFlush(newUser("t07-cliente@example.invalid", "t07-cliente"));
        var editor = users.saveAndFlush(newUser("t07-editor@example.invalid", "t07-editor"));
        var conversation = conversations.saveAndFlush(ConversationEntity.builder().client(client).editor(editor).build());
        try {
            assertCommonError(send(authenticated("/v1/me/conversations/" + conversation.getId()).GET().build()), 403);
        } finally {
            conversations.deleteById(conversation.getId());
            users.deleteById(editor.getId());
            users.deleteById(client.getId());
        }
    }

    @Test
    void limiteRealMantem429ERetryAfter() throws Exception {
        var resend = jsonPost("/v1/auth/register/resend", """
                {"email":"t07-cooldown@example.invalid"}
                """);
        assertThat(send(resend).statusCode()).isEqualTo(204);
        var limited = send(resend);
        assertCommonError(limited, 429);
        assertThat(Integer.parseInt(limited.headers().firstValue("Retry-After").orElseThrow())).isBetween(1, 60);
        assertThat(limited.headers().firstValue("Cache-Control")).contains("no-store");
    }

    @Test
    void falhaNoEnvioObrigatorioMantem503() throws Exception {
        doThrow(new IllegalStateException("falha simulada")).when(sender).send(any(), any());
        var signup = jsonPost("/v1/auth/register", """
                {"email":"t07-smtp@example.invalid","password":"senha-ficticia","displayName":"Teste",
                 "username":"t07-smtp","roles":["CLIENT"]}
                """);
        assertCommonError(send(signup), 503);
        assertThat(users.findByUsername("t07-smtp")).isEmpty();
    }

    private void assertCommonError(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
        Map<String, Object> payload = JsonPath.parse(response.body()).read("$");
        assertThat(payload).containsOnlyKeys("error");
        assertThat(payload.get("error")).isInstanceOfSatisfying(String.class, message -> assertThat(message).isNotBlank());
        assertThat(response.body()).doesNotContain(INVALID_VALUE, "uk_users_username", "SQLException", "stackTrace");
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(15)).header("Accept", "application/json");
    }

    private HttpRequest.Builder authenticated(String path) {
        return request(path).header("Authorization", "Bearer " + jwtService.generateToken(actor));
    }

    private HttpRequest jsonPost(String path, String body) {
        return request(path).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private byte[] multipart(int... sizes) {
        var body = new ByteArrayOutputStream();
        for (int size : sizes) {
            body.writeBytes(("--" + BOUNDARY + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"ficticio.png\"\r\n"
                    + "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            body.writeBytes(new byte[size]);
            body.writeBytes("\r\n".getBytes(StandardCharsets.US_ASCII));
        }
        body.writeBytes(("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.US_ASCII));
        return body.toByteArray();
    }

    private static UserEntity newUser(String email, String username) {
        return UserEntity.builder().email(email).username(username).displayName("Conta de teste")
                .roles(Set.of(UserRole.CLIENT)).active(true).build();
    }

    /** Exercita as restrições reais sem depender de uma corrida em um serviço de domínio. */
    @TestComponent
    @RestController
    static class ConstraintFixture {
        private final IUserRepository users;
        private final JdbcTemplate jdbc;
        private final CyclicBarrier writers = new CyclicBarrier(2);

        ConstraintFixture(IUserRepository users, JdbcTemplate jdbc) {
            this.users = users;
            this.jdbc = jdbc;
        }

        @PostMapping("/__test/t07/duplicate/{writer}")
        @ResponseStatus(HttpStatus.CREATED)
        void duplicate(@PathVariable int writer) throws Exception {
            writers.await(5, TimeUnit.SECONDS);
            users.saveAndFlush(newUser("t07-escrita-" + writer + "@example.invalid", CONTESTED_USERNAME));
        }

        @GetMapping("/v1/auth/__test/t07/error")
        void sendError(HttpServletResponse response) throws IOException {
            response.sendError(500);
        }

        @PostMapping("/__test/t07/unknown-unique/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void unknownUnique(@PathVariable UUID id) {
            jdbc.update("""
                    insert into users (id, email, username, display_name, created_at)
                    values (?, 't07-chave@example.invalid', 't07-chave', 'Teste', now())
                    """, id);
        }

        @PostMapping("/__test/t07/not-null/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void notNull(@PathVariable UUID id) {
            jdbc.update("update users set display_name = null where id = ?", id);
        }
    }
}
