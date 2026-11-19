package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.TestcontainersConfiguration;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IUserAvatarRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.service.JwtService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AvatarTest {
    @Autowired private MockMvc mvc;
    @Autowired private IUserRepository users;
    @Autowired private IUserAvatarRepository avatars;
    @Autowired private JwtService jwt;
    @LocalServerPort private int port;

    private final List<UUID> created = new ArrayList<>();
    private UserEntity owner;
    private String token;

    @BeforeEach
    void setUp() {
        owner = newUser();
        token = jwt.generateToken(owner);
    }

    @AfterEach
    void cleanUp() {
        for (UUID id : created) {
            avatars.deleteById(id);
            users.deleteById(id);
        }
        created.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"jpg", "png"})
    void uploadNormalizaRecorteEPersiste(String format) throws Exception {
        BufferedImage image = new BufferedImage(60, 20, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, 60, 20);
        graphics.setColor(Color.RED);
        graphics.fillRect(20, 0, 20, 20);
        graphics.dispose();

        String version = upload(token, encode(image, format))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String expectedVersion = JsonPath.read(version, "$.version");
        var stored = avatars.findById(owner.getId()).orElseThrow();
        assertThat(stored.getVersion().toString()).isEqualTo(expectedVersion);
        assertThat(users.findById(owner.getId()).orElseThrow().getAvatarVersion()).isEqualTo(stored.getVersion());

        byte[] bytes = mvc.perform(get(url()).queryParam("v", expectedVersion))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string(HttpHeaders.ETAG, "\"" + expectedVersion + "\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, must-revalidate, public"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(bytes).isEqualTo(stored.getBytes());
        BufferedImage normalized = ImageIO.read(new ByteArrayInputStream(bytes));
        assertThat(normalized.getWidth()).isEqualTo(256);
        assertThat(normalized.getHeight()).isEqualTo(256);
        Color pixel = new Color(normalized.getRGB(64, 64));
        assertThat(pixel.getRed()).isGreaterThan(240);
        assertThat(pixel.getBlue()).isLessThan(15);
    }

    @Test
    void pngTransparenteGanhaFundoBranco() throws Exception {
        upload(token, encode(new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB), "png"))
                .andExpect(status().isOk());
        BufferedImage normalized = ImageIO.read(new ByteArrayInputStream(avatars.findById(owner.getId()).orElseThrow().getBytes()));
        assertThat(normalized.getColorModel().hasAlpha()).isFalse();
        assertThat(new Color(normalized.getRGB(128, 128))).isEqualTo(Color.WHITE);
    }

    @Test
    void jpegDescartaExifDoArquivoOriginal() throws Exception {
        byte[] jpeg = encode(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "jpg");
        // EXIF de orientação 6 em um JPEG pequeno. Nenhuma imagem grande é alocada.
        ByteBuffer exif = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN);
        exif.put("Exif\0\0".getBytes(StandardCharsets.US_ASCII));
        exif.put((byte) 'I').put((byte) 'I').putShort((short) 42).putInt(8);
        exif.putShort((short) 1).putShort((short) 0x0112).putShort((short) 3);
        exif.putInt(1).putShort((short) 6).putShort((short) 0).putInt(0);
        var source = new ByteArrayOutputStream();
        source.write(jpeg, 0, 2);
        source.write(new byte[]{(byte) 0xff, (byte) 0xe1, 0, 34});
        source.write(exif.array());
        source.write(jpeg, 2, jpeg.length - 2);
        upload(token, source.toByteArray()).andExpect(status().isOk());
        byte[] normalized = avatars.findById(owner.getId()).orElseThrow().getBytes();
        assertThat(new String(normalized, StandardCharsets.ISO_8859_1)).doesNotContain("Exif");
        assertThat(ImageIO.read(new ByteArrayInputStream(normalized)).getWidth()).isEqualTo(256);
    }

    @Test
    void trocaRevalidaUrlAntigaERemocaoInvalidaEtag() throws Exception {
        upload(token, solid(Color.RED)).andExpect(status().isOk());
        String oldEtag = mvc.perform(get(url())).andReturn().getResponse().getHeader(HttpHeaders.ETAG);
        mvc.perform(get(url()).header(HttpHeaders.IF_NONE_MATCH, oldEtag)).andExpect(status().isNotModified());
        upload(token, solid(Color.BLUE)).andExpect(status().isOk());
        mvc.perform(get(url()).queryParam("v", oldEtag.replace("\"", ""))
                        .header(HttpHeaders.IF_NONE_MATCH, oldEtag))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getHeader(HttpHeaders.ETAG)).isNotEqualTo(oldEtag));
        mvc.perform(delete("/v1/me/avatar").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(avatars.findById(owner.getId())).isEmpty();
        assertThat(users.findById(owner.getId()).orElseThrow().getAvatarVersion()).isNull();
        mvc.perform(get(url()).header(HttpHeaders.IF_NONE_MATCH, oldEtag))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
    }

    @Test
    void arquivosInvalidosOuNaoDeclaradosNaoSubstituemFoto() throws Exception {
        upload(token, solid(Color.RED)).andExpect(status().isOk());
        UUID original = avatars.findById(owner.getId()).orElseThrow().getVersion();
        for (byte[] bytes : List.of(
                new byte[0], "arquivo inválido".getBytes(StandardCharsets.UTF_8),
                encode(new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB), "gif"))) {
            upload(token, bytes).andExpect(status().isConflict()).andExpect(jsonPath("$.error").isString());
        }
        assertThat(ImageIO.getImageReadersByFormatName("webp").hasNext()).isFalse();
        assertThat(avatars.findById(owner.getId()).orElseThrow().getVersion()).isEqualTo(original);
    }

    @Test
    void serviceRecusaExcessoDeBytes() throws Exception {
        // MockMvc ignora o parser multipart do servidor. Este caso exercita a barreira do service.
        upload(token, new byte[5 * 1024 * 1024 + 1]).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("A imagem precisa ter no máximo 5 MB."));
        assertThat(avatars.findById(owner.getId())).isEmpty();
    }

    @Test
    void cabecalhosComDimensoesExageradasSaoRecusadosAntesDoDecode() throws Exception {
        for (int[] size : List.of(new int[]{100_000, 1}, new int[]{5000, 4000})) {
            byte[] png = encode(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png");
            ByteBuffer.wrap(png).putInt(16, size[0]).putInt(20, size[1]);
            CRC32 crc = new CRC32();
            crc.update(png, 12, 17);
            ByteBuffer.wrap(png).putInt(29, (int) crc.getValue());
            upload(token, png).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value(
                            "A imagem precisa ter até 8192 pixels por lado e no máximo 16 milhões de pixels."));
        }
        assertThat(avatars.findById(owner.getId())).isEmpty();
    }

    @Test
    void outroUsuarioSoPodeAlterarOProprioAvatar() throws Exception {
        upload(token, solid(Color.RED)).andExpect(status().isOk());
        var version = avatars.findById(owner.getId()).orElseThrow().getVersion();
        UserEntity other = newUser();
        String otherToken = jwt.generateToken(other);
        // IDs e usernames extras não substituem o principal JWT.
        mvc.perform(multipart("/v1/me/avatar").file(file(solid(Color.BLUE)))
                        .param("userId", owner.getId().toString()).param("username", owner.getUsername())
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isOk());
        assertThat(avatars.findById(owner.getId()).orElseThrow().getVersion()).isEqualTo(version);
        mvc.perform(delete("/v1/me/avatar").param("userId", owner.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isNoContent());
        assertThat(avatars.findById(owner.getId())).isPresent();
        assertThat(avatars.findById(other.getId())).isEmpty();
        upload(null, solid(Color.BLUE)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/v1/me/avatar")).andExpect(status().isUnauthorized());
    }

    @Test
    void contaInativaNaoServeFotoNemAlteraAvatar() throws Exception {
        upload(token, solid(Color.RED)).andExpect(status().isOk());
        String etag = mvc.perform(get(url())).andReturn().getResponse().getHeader(HttpHeaders.ETAG);
        owner.setActive(false);
        users.save(owner);
        mvc.perform(get(url()).header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
        upload(token, solid(Color.BLUE)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/v1/me/avatar").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertThat(avatars.findById(owner.getId())).isPresent();
    }

    @Test
    void limiteMultipartRealDoSpring() throws Exception {
        String boundary = "t23-boundary";
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"avatar.jpg\""
                + "\r\nContent-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        body.write(new byte[5 * 1024 * 1024 + 1]);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII));
        try (HttpClient client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v1/me/avatar"))
                    .timeout(Duration.ofSeconds(20))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=" + boundary)
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("T23 multipart real: status=" + response.statusCode()
                    + ", content-type=" + response.headers().firstValue("content-type").orElse("(ausente)")
                    + ", body=" + response.body());
            // T07 deve fixar o JSON comum. Este teste mantém explícito o limite real do Spring.
            assertThat(response.statusCode()).isEqualTo(413);
            assertThat(response.body()).isEmpty();
            assertThat(response.headers().firstValue("content-type")).isEmpty();
            assertThat(avatars.findById(owner.getId())).isEmpty();
        }
    }

    private UserEntity newUser() {
        String username = "avatar-" + UUID.randomUUID().toString().substring(0, 8);
        UserEntity user = users.save(UserEntity.builder().username(username)
                .email(username + "@example.test").displayName("Avatar Test").build());
        created.add(user.getId());
        return user;
    }

    private String url() {
        return "/v1/avatars/" + owner.getUsername();
    }

    private ResultActions upload(String bearer, byte[] bytes) throws Exception {
        var request = multipart("/v1/me/avatar").file(file(bytes))
                .with(req -> { req.setMethod("PUT"); return req; });
        if (bearer != null) request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
        return mvc.perform(request);
    }

    private MockMultipartFile file(byte[] bytes) {
        return new MockMultipartFile("file", "avatar.jpg", "image/jpeg", bytes);
    }

    private byte[] solid(Color color) throws Exception {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, 20, 20);
        graphics.dispose();
        return encode(image, "jpg");
    }

    private byte[] encode(BufferedImage image, String format) throws Exception {
        var out = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, out)).isTrue();
        return out.toByteArray();
    }
}
