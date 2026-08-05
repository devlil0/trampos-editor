package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.EditorProfileEntity;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IDealRepository;
import com.devlil0.tramposeditor.database.repository.IEditorProfileRepository;
import com.devlil0.tramposeditor.database.repository.IReviewRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.PublicProfileDTO;
import com.devlil0.tramposeditor.dto.RatingAggregateDTO;
import com.devlil0.tramposeditor.enums.DealStatus;
import com.devlil0.tramposeditor.exception.NotFoundException;
import com.devlil0.tramposeditor.mapper.EditorProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * A página de {@code /{username}} — a identidade da pessoa, seja ela editora ou cliente.
 * <p>
 * Pública e sem autenticação, como a vitrine: é a mesma natureza de ativo. A diferença é
 * que agora <b>todo mundo</b> tem endereço, não só quem publicou perfil de editor.
 */
@Service
@RequiredArgsConstructor
public class PublicProfileService {

    private final IUserRepository userRepository;
    private final IEditorProfileRepository editorProfileRepository;
    private final IDealRepository dealRepository;
    private final IReviewRepository reviewRepository;
    private final EditorProfileMapper editorProfileMapper;

    @Transactional(readOnly = true)
    public PublicProfileDTO getByUsername(String username){
        String normalizado = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);

        UserEntity user = userRepository.findByUsername(normalizado)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new NotFoundException("Perfil não encontrado: " + username));

        Optional<EditorProfileEntity> editor = editorProfileRepository.findByUserId(user.getId());
        RatingAggregateDTO nota = reviewRepository.aggregateFor(user.getId());

        return new PublicProfileDTO(
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarVersion(),
                user.getInstagram(),
                user.getCreatedAt(),
                editor.map(editorProfileMapper::toDetail).orElse(null),
                // DONE, não CANCELLED: trampo cancelado não é currículo de ninguém.
                dealRepository.countClosedForUser(user.getId(), DealStatus.DONE),
                reviewRepository.countByAuthorId(user.getId()),
                nota.average(),
                nota.count());
    }

    /**
     * O username de quem publicou um perfil com este slug — usado pelo redirecionamento
     * das URLs antigas (`/editores/{slug}` → `/{username}`).
     * <p>
     * Existe como consulta própria, e não como "tente o username igual ao slug", porque os
     * dois podem divergir: a pessoa troca o username depois, e o link antigo tem que
     * continuar chegando no lugar certo.
     */
    @Transactional(readOnly = true)
    public Optional<String> usernameDoSlug(String slug){
        return editorProfileRepository.findBySlug(slug)
                .map(profile -> profile.getUser().getUsername());
    }
}
