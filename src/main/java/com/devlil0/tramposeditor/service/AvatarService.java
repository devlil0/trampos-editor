package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.UserAvatarEntity;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IUserAvatarRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Foto de perfil: recebe, normaliza e guarda.
 * <p>
 * <b>A imagem nunca é armazenada como veio.</b> Ela é decodificada, recortada em quadrado,
 * reduzida e reencodada em JPEG. Isso não é só economia de espaço — é o que garante que:
 * <ul>
 *   <li>o arquivo é mesmo uma imagem, e não um script com extensão trocada: {@code ImageIO}
 *       devolve {@code null} pra qualquer coisa que não decodifique;</li>
 *   <li>nada embutido sobrevive — reencodar descarta qualquer payload escondido em campo
 *       de metadado;</li>
 *   <li>o EXIF some junto, e com ele a <b>geolocalização</b> que câmera de celular grava
 *       por padrão. Publicar a foto sem isso seria vazar a casa de quem subiu.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AvatarService {

    /** Lado do quadrado final. 256 basta pro maior uso (perfil) e pesa ~20KB. */
    private static final int SIDE = 256;

    /** Teto do arquivo recebido, antes de qualquer processamento. */
    private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024;
    private static final int MAX_DIMENSION = 8192;
    private static final long MAX_PIXELS = 16_000_000L;

    private static final String OUTPUT_TYPE = "image/jpeg";

    private final IUserRepository userRepository;
    private final IUserAvatarRepository avatarRepository;

    @Transactional
    public UUID replace(UUID userId, MultipartFile file){
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Escolha uma imagem.");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new BusinessException("A imagem precisa ter no máximo 5 MB.");
        }

        byte[] normalizada = normalizar(file);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Conta não encontrada"));

        UUID version = UUID.randomUUID();

        UserAvatarEntity avatar = avatarRepository.findById(userId)
                .orElseGet(() -> UserAvatarEntity.builder().user(user).build());
        avatar.setContentType(OUTPUT_TYPE);
        avatar.setBytes(normalizada);
        avatar.setVersion(version);
        avatar.setUpdatedAt(LocalDateTime.now());
        avatarRepository.save(avatar);

        // O ponteiro na conta é o que permite listar perfis sabendo se há foto sem tocar
        // no bytea — e é ele que entra na URL, pra o cache do browser saber que mudou.
        user.setAvatarVersion(version);
        userRepository.save(user);

        return version;
    }

    @Transactional
    public void remove(UUID userId){
        avatarRepository.deleteById(userId);
        userRepository.findById(userId).ifPresent(user -> {
            user.setAvatarVersion(null);
            userRepository.save(user);
        });
    }

    @Transactional(readOnly = true)
    public Optional<UserAvatarEntity> byUsername(String username){
        return avatarRepository.findByUsername(username);
    }

    /**
     * Recorta o centro em quadrado e reduz pra {@link #SIDE}.
     * <p>
     * Recorte central e não deformação: avatar é redondo na interface, e uma foto
     * espremida pra caber num quadrado fica com o rosto achatado.
     * <p>
     * Desde 2026-08-06 a interface manda a imagem <b>já quadrada</b>: quem enquadra é a
     * pessoa, no browser (ver {@code avatar-cropper.tsx}). Pra essas, o recorte abaixo é
     * operação neutra — largura e altura já são iguais. Ele fica porque continua sendo o
     * comportamento correto pro que chega de qualquer outro lugar (cliente antigo em cache,
     * chamada direta na API), e porque normalização de entrada não é responsabilidade que
     * se delegue ao cliente: o que o cliente escolhe é o enquadramento, não a validação.
     */
    private byte[] normalizar(MultipartFile file){
        BufferedImage original;
        try (var source = file.getInputStream();
             var input = new MemoryCacheImageInputStream(source)) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new BusinessException("Esse arquivo não é uma imagem válida. Use JPG ou PNG.");
            }
            var reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("png")) {
                    throw new BusinessException("Formato não suportado. Use JPG ou PNG.");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION
                        || (long) width * height > MAX_PIXELS) {
                    throw new BusinessException(
                            "A imagem precisa ter até 8192 pixels por lado e no máximo 16 milhões de pixels.");
                }
                original = reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new BusinessException("Não consegui ler essa imagem. Tente outra.");
        }
        if (original == null) {
            throw new BusinessException("Esse arquivo não é uma imagem válida. Use JPG ou PNG.");
        }

        int lado = Math.min(original.getWidth(), original.getHeight());
        int x = (original.getWidth() - lado) / 2;
        int y = (original.getHeight() - lado) / 2;
        BufferedImage quadrada = original.getSubimage(x, y, lado, lado);

        // TYPE_INT_RGB e não ARGB: JPEG não tem canal alfa, e PNG transparente convertido
        // sem fundo sai com as áreas transparentes pretas.
        BufferedImage destino = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, SIDE, SIDE);
            g.drawImage(quadrada, 0, 0, SIDE, SIDE, null);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(destino, "jpg", out)) {
                throw new IllegalStateException("Codificador JPEG indisponível");
            }
        } catch (IOException e) {
            throw new BusinessException("Não consegui processar essa imagem. Tente outra.");
        }
        return out.toByteArray();
    }
}
