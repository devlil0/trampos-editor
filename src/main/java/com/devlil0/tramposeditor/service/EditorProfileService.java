package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.EditorProfileEntity;
import com.devlil0.tramposeditor.database.model.PortfolioItemEntity;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IEditorProfileRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.EditorProfileDTO;
import com.devlil0.tramposeditor.dto.EditorProfileUpsertDTO;
import com.devlil0.tramposeditor.dto.EditorSummaryDTO;
import com.devlil0.tramposeditor.dto.PageResponseDTO;
import com.devlil0.tramposeditor.dto.PortfolioItemCreateDTO;
import com.devlil0.tramposeditor.enums.EditorNiche;
import com.devlil0.tramposeditor.enums.EditorTool;
import com.devlil0.tramposeditor.enums.PortfolioProvider;
import com.devlil0.tramposeditor.enums.UserRole;
import com.devlil0.tramposeditor.enums.Vertical;
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.exception.NotFoundException;
import com.devlil0.tramposeditor.mapper.EditorProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EditorProfileService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int SLUG_MAX_LENGTH = 120;

    private final IEditorProfileRepository editorProfileRepository;
    private final IUserRepository userRepository;
    private final EditorProfileMapper mapper;

    // ---------- Vitrine pública ----------

    @Transactional(readOnly = true)
    public PageResponseDTO<EditorSummaryDTO> search(
            EditorNiche niche, EditorTool tool, Boolean available,
            Integer maxPriceCents, String q, int page, int size){

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        String normalizedQ = (q == null || q.isBlank()) ? null : q.trim();

        Page<EditorProfileEntity> result = editorProfileRepository.search(
                Vertical.VIDEO, niche, tool, available, maxPriceCents, normalizedQ, pageable);

        return PageResponseDTO.from(result, mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public EditorProfileDTO getBySlug(String slug){
        EditorProfileEntity profile = editorProfileRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Editor não encontrado: " + slug));

        return mapper.toDetail(profile);
    }

    // ---------- Self-service do editor ----------

    @Transactional(readOnly = true)
    public EditorProfileDTO getMyProfile(UUID userId){
        return mapper.toDetail(loadOwnProfile(userId));
    }

    @Transactional
    public EditorProfileDTO upsertMyProfile(UUID userId, EditorProfileUpsertDTO dto){
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(userId));

        validatePriceRange(dto.priceMinCents(), dto.priceMaxCents());

        // A conta é uma só: criar o perfil É o registro como editor. Quem chegou pra
        // contratar e resolveu editar vira editor aqui, sem cadastro paralelo.
        user.getRoles().add(UserRole.EDITOR);

        EditorProfileEntity profile = editorProfileRepository.findByUserId(userId)
                .orElseGet(() -> EditorProfileEntity.builder()
                        .user(user)
                        // slug gerado uma única vez, no cadastro do perfil — imutável depois (SEO estável).
                        .slug(generateUniqueSlug(user.getDisplayName()))
                        .vertical(Vertical.VIDEO)
                        .build());

        profile.setHeadline(trimToNull(dto.headline()));
        profile.setBio(trimToNull(dto.bio()));
        profile.setNiches(new java.util.HashSet<>(dto.niches()));
        profile.setTools(new java.util.HashSet<>(dto.tools()));
        profile.setPriceMinCents(dto.priceMinCents());
        profile.setPriceMaxCents(dto.priceMaxCents());
        profile.setTurnaroundDays(dto.turnaroundDays());
        profile.setAvailable(dto.available() == null || dto.available());

        editorProfileRepository.save(profile);

        return mapper.toDetail(profile);
    }

    @Transactional
    public EditorProfileDTO addPortfolioItem(UUID userId, PortfolioItemCreateDTO dto){
        EditorProfileEntity profile = loadOwnProfile(userId);

        int nextPosition = profile.getPortfolio().stream()
                .mapToInt(PortfolioItemEntity::getPosition)
                .max()
                .orElse(-1) + 1;

        PortfolioItemEntity item = PortfolioItemEntity.builder()
                .title(dto.title().trim())
                .provider(inferProvider(dto.videoUrl()))
                .videoUrl(dto.videoUrl().trim())
                .position(nextPosition)
                .build();

        profile.addPortfolioItem(item);
        editorProfileRepository.save(profile);

        return mapper.toDetail(profile);
    }

    @Transactional
    public EditorProfileDTO removePortfolioItem(UUID userId, UUID itemId){
        EditorProfileEntity profile = loadOwnProfile(userId);

        boolean removed = profile.getPortfolio().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new NotFoundException("Item de portfólio não encontrado: " + itemId);
        }

        editorProfileRepository.save(profile);

        return mapper.toDetail(profile);
    }

    // ---------- Auxiliares ----------

    private EditorProfileEntity loadOwnProfile(UUID userId){
        return editorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Você ainda não criou um perfil de editor"));
    }

    private void validatePriceRange(Integer min, Integer max){
        if (min != null && max != null && min > max) {
            throw new BusinessException("O preço mínimo não pode ser maior que o máximo");
        }
    }

    private PortfolioProvider inferProvider(String url){
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) {
            return PortfolioProvider.YOUTUBE;
        }
        if (lower.contains("vimeo.com")) {
            return PortfolioProvider.VIMEO;
        }
        throw new BusinessException("Só aceitamos links do YouTube ou Vimeo no portfólio");
    }

    private String trimToNull(String value){
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Gera slug a partir do nome ("João Silva" -> "joao-silva"). Em caso de colisão,
     * anexa um sufixo curto aleatório em vez de um contador — evita uma segunda consulta
     * de sondagem e não vaza quantos homônimos existem.
     */
    private String generateUniqueSlug(String displayName){
        String base = slugify(displayName);
        if (base.isEmpty()) {
            base = "editor";
        }

        String candidate = base;
        while (editorProfileRepository.existsBySlug(candidate)) {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            candidate = truncate(base, SLUG_MAX_LENGTH - suffix.length() - 1) + "-" + suffix;
        }
        return candidate;
    }

    private String slugify(String value){
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")               // remove acentos
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")           // não-alfanumérico vira hífen
                .replaceAll("(^-+)|(-+$)", "");          // apara hífens das pontas
        return truncate(normalized, SLUG_MAX_LENGTH);
    }

    private String truncate(String value, int max){
        return value.length() <= max ? value : value.substring(0, max);
    }
}
