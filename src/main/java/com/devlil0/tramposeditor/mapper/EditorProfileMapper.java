package com.devlil0.tramposeditor.mapper;

import com.devlil0.tramposeditor.database.model.EditorProfileEntity;
import com.devlil0.tramposeditor.database.model.PortfolioItemEntity;
import com.devlil0.tramposeditor.dto.EditorProfileDTO;
import com.devlil0.tramposeditor.dto.EditorSummaryDTO;
import com.devlil0.tramposeditor.dto.PortfolioItemDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Acessa coleções LAZY (portfólio, nichos) — precisa ser chamado dentro da
 * transação do service, já que open-in-view está desligado.
 */
@Component
public class EditorProfileMapper {

    public EditorProfileDTO toDetail(EditorProfileEntity e){
        List<PortfolioItemDTO> portfolio = e.getPortfolio().stream()
                .map(this::toPortfolioItem)
                .toList();

        return new EditorProfileDTO(
                e.getId(),
                e.getUser().getId(),
                e.getSlug(),
                e.getUser().getDisplayName(),
                e.getVertical(),
                e.getHeadline(),
                e.getBio(),
                Set.copyOf(e.getNiches()),
                Set.copyOf(e.getTools()),
                e.getPriceMinCents(),
                e.getPriceMaxCents(),
                e.getTurnaroundDays(),
                e.isAvailable(),
                e.getRatingAvg(),
                e.getRatingCount(),
                portfolio
        );
    }

    public EditorSummaryDTO toSummary(EditorProfileEntity e){
        String coverVideoUrl = e.getPortfolio().isEmpty()
                ? null
                : e.getPortfolio().getFirst().getVideoUrl();

        return new EditorSummaryDTO(
                e.getId(),
                e.getSlug(),
                e.getUser().getDisplayName(),
                e.getHeadline(),
                Set.copyOf(e.getNiches()),
                Set.copyOf(e.getTools()),
                e.getPriceMinCents(),
                e.getTurnaroundDays(),
                e.isAvailable(),
                e.getRatingAvg(),
                e.getRatingCount(),
                coverVideoUrl
        );
    }

    public PortfolioItemDTO toPortfolioItem(PortfolioItemEntity item){
        return new PortfolioItemDTO(
                item.getId(),
                item.getTitle(),
                item.getProvider(),
                item.getVideoUrl(),
                item.getPosition()
        );
    }
}
