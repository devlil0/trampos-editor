package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.dto.DealCreateDTO;
import com.devlil0.tramposeditor.dto.DealDTO;
import com.devlil0.tramposeditor.dto.ReviewCreateDTO;
import com.devlil0.tramposeditor.enums.DealStatus;
import com.devlil0.tramposeditor.service.DealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Trampos fechados e avaliação (ESBOCO §7, item 4).
 * <p>
 * Mora em {@code /v1/me/*} como o resto do que é de dono: histórico é do participante,
 * nunca conteúdo público. A nota resultante, essa sim, aparece na vitrine.
 */
@RestController
@RequestMapping("v1/me/deals")
@RequiredArgsConstructor
public class DealController {

    private final DealService dealService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<DealDTO> listMine(@AuthenticationPrincipal Jwt jwt){
        return dealService.listMine(userId(jwt));
    }

    /** Abre um trampo dentro de uma conversa da vitrine — o caminho sem pedido. */
    @PostMapping("/from-conversation/{conversationId}")
    @ResponseStatus(HttpStatus.CREATED)
    public DealDTO openFromConversation(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable UUID conversationId,
                                        @Valid @RequestBody DealCreateDTO dto){
        return dealService.openFromConversation(userId(jwt), conversationId, dto);
    }

    @PostMapping("/{dealId}/done")
    @ResponseStatus(HttpStatus.OK)
    public DealDTO markDone(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID dealId){
        return dealService.close(userId(jwt), dealId, DealStatus.DONE);
    }

    @PostMapping("/{dealId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public DealDTO cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID dealId){
        return dealService.close(userId(jwt), dealId, DealStatus.CANCELLED);
    }

    @PostMapping("/{dealId}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public DealDTO review(@AuthenticationPrincipal Jwt jwt,
                          @PathVariable UUID dealId,
                          @Valid @RequestBody ReviewCreateDTO dto){
        return dealService.review(userId(jwt), dealId, dto);
    }

    private UUID userId(Jwt jwt){
        return UUID.fromString(jwt.getSubject());
    }
}
