package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.UserResponseDTO;
import com.devlil0.tramposeditor.dto.UserUpdateDTO;
import com.devlil0.tramposeditor.dto.UsernameUpdateDTO;
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.exception.NotFoundException;
import com.devlil0.tramposeditor.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final UsernameService usernameService;

    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id){
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new NotFoundException(id));

        return userMapper.toDTO(user);
    }

    /**
     * Atualiza os dados que o dono da conta controla. Hoje só o Instagram — o username tem
     * endpoint próprio ({@link #trocarUsername}) porque carrega regra que o Instagram não
     * tem: unicidade, nomes reservados e cooldown.
     */
    @Transactional
    public UserResponseDTO updateMe(UUID id, UserUpdateDTO dto){
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new NotFoundException(id));

        user.setInstagram(normalizeInstagram(dto.instagram()));
        userRepository.save(user);

        return userMapper.toDTO(user);
    }

    /**
     * Escolhe ou troca o username — o endereço da pessoa em {@code /{username}}.
     * <p>
     * Serve os dois momentos com a mesma regra, e a diferença entre eles é o que decide o
     * cooldown: a <b>primeira escolha</b> (conta social que ainda não escolheu, ou conta que
     * nunca trocou) não consome nada; a partir daí vale
     * {@link UsernameService#COOLDOWN_DIAS}.
     * <p>
     * O endereço antigo não deixa rastro: some da base, volta a ficar livre pra qualquer um
     * e {@code /{antigo}} passa a dar 404. É a decisão de 2026-08-05 — o mesmo que o
     * Instagram faz — e é justamente o que o cooldown existe pra segurar.
     */
    @Transactional
    public UserResponseDTO trocarUsername(UUID id, UsernameUpdateDTO dto){
        UserEntity user = userRepository.findById(id).orElseThrow(() -> new NotFoundException(id));

        String novo = usernameService.normalizar(dto.username());

        // Reenviar o nome que já é seu não é troca: devolve o estado atual sem gastar
        // cooldown. Sem isto, um duplo submit custaria 14 dias à pessoa.
        if (novo != null && novo.equals(user.getUsername()) && user.isUsernameChosen()) {
            return userMapper.toDTO(user);
        }

        boolean eTroca = user.isUsernameChosen();
        if (eTroca) {
            long dias = usernameService.diasAteProximaTroca(user.getUsernameChangedAt());
            if (dias > 0) {
                throw new BusinessException(dias == 1
                        ? "Você trocou seu nome de usuário há pouco. Poderá trocar de novo amanhã."
                        : "Você trocou seu nome de usuário há pouco. Poderá trocar de novo em "
                          + dias + " dias.");
            }
        }

        String problema = usernameService.validar(novo, id);
        if (problema != null) {
            throw new BusinessException(problema);
        }

        user.setUsername(novo);
        user.setUsernameChosen(true);
        if (eTroca) {
            user.setUsernameChangedAt(LocalDateTime.now());
        }

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Mesma corrida do cadastro: entre validar e gravar, outra pessoa pode ter
            // levado o nome. Quem decide é o UNIQUE.
            throw new BusinessException("Esse nome de usuário acabou de ser tomado. Tente outro.");
        }

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
