package com.devlil0.tramposeditor.database.repository;

import com.devlil0.tramposeditor.database.model.EditorProfileEntity;
import com.devlil0.tramposeditor.enums.EditorNiche;
import com.devlil0.tramposeditor.enums.EditorStyle;
import com.devlil0.tramposeditor.enums.EditorTool;
import com.devlil0.tramposeditor.enums.Vertical;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface IEditorProfileRepository extends JpaRepository<EditorProfileEntity, UUID> {

    Optional<EditorProfileEntity> findByUserId(UUID userId);

    Optional<EditorProfileEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByUserId(UUID userId);

    /**
     * Busca da vitrine. Todo filtro é opcional (padrão ":x is null or ...") pra caber
     * numa query só; `member of` resolve o filtro nas coleções de nicho/ferramenta.
     * O ORDER BY favorece quem está disponível e bem avaliado — o que o cliente quer ver no topo.
     */
    @Query("""
            select distinct e from EditorProfileEntity e
            where e.vertical = :vertical
              and (:niche is null or :niche member of e.niches)
              and (:tool is null or :tool member of e.tools)
              and (:style is null or :style member of e.styles)
              and (:available is null or e.available = :available)
              and (:maxPriceCents is null or (e.priceMinCents is not null and e.priceMinCents <= :maxPriceCents))
              and (:q is null or lower(e.user.displayName) like lower(concat('%', cast(:q as string), '%'))
                              or lower(e.headline) like lower(concat('%', cast(:q as string), '%')))
            order by e.available desc, e.ratingAvg desc, e.ratingCount desc, e.createdAt desc
            """)
    Page<EditorProfileEntity> search(
            @Param("vertical") Vertical vertical,
            @Param("niche") EditorNiche niche,
            @Param("tool") EditorTool tool,
            @Param("style") EditorStyle style,
            @Param("available") Boolean available,
            @Param("maxPriceCents") Integer maxPriceCents,
            @Param("q") String q,
            Pageable pageable
    );
}
