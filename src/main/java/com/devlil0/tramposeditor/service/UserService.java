package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.UserResponseDTO;
import com.devlil0.tramposeditor.dto.UserUpdateDTO;
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.exception.NotFoundException;
import com.devlil0.tramposeditor.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

    /**
     * Username do Instagram: 1–30 caracteres, letras, números, ponto e underline.
     * É a regra da própria plataforma — vale replicar pra não gravar link quebrado.
     */
    private static final Pattern INSTAGRAM_USERNAME = Pattern.compile("^[A-Za-z0-9._]{1,30}$");

    /** Tudo que vem antes do username quando a pessoa cola a URL em vez de digitar o @. */
    private static final Pattern INSTAGRAM_URL_PREFIX =
            Pattern.compile("^(https?://)?(www\\.)?instagram\\.com/", Pattern.CASE_INSENSITIVE);

    private final IUserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id){
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new NotFoundException(id));

        return userMapper.toDTO(user);
    }

    /**
     * Atualiza os dados que o dono da conta controla. Hoje só o Instagram — nome e
     * e-mail ficam de fora porque o slug público do editor é derivado do nome e é
     * imutável (SEO estável), então trocar um sem o outro deixaria os dois em desacordo.
     */
    @Transactional
    public UserResponseDTO updateMe(UUID id, UserUpdateDTO dto){
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new NotFoundException(id));

        user.setInstagram(normalizeInstagram(dto.instagram()));
        userRepository.save(user);

        return userMapper.toDTO(user);
    }

    /**
     * Reduz o que a pessoa mandou ao username puro: aceita "@fulano", "fulano",
     * "instagram.com/fulano" e a URL completa com barra no fim. Campo vazio apaga
     * o que estava lá — é como se tira o Instagram do perfil.
     */
    private String normalizeInstagram(String raw){
        if (raw == null) {
            return null;
        }

        String value = INSTAGRAM_URL_PREFIX.matcher(raw.trim()).replaceFirst("");
        // Barra final da URL colada e query string ("?igsh=...") que o app do Instagram anexa.
        value = value.split("[/?]", 2)[0];
        if (value.startsWith("@")) {
            value = value.substring(1);
        }

        if (value.isEmpty()) {
            return null;
        }

        if (!INSTAGRAM_USERNAME.matcher(value).matches()) {
            throw new BusinessException(
                    "Instagram inválido. Use só o nome de usuário, como @seuperfil");
        }

        return value;
    }
}
