package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.dto.ConversationDetailDTO;
import com.devlil0.tramposeditor.dto.ConversationSummaryDTO;
import com.devlil0.tramposeditor.dto.MessageCreateDTO;
import com.devlil0.tramposeditor.dto.MessageDTO;
import com.devlil0.tramposeditor.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * A caixa de entrada. Mora em /v1/me porque tudo aqui é do usuário autenticado —
 * conversa não tem leitura pública, ao contrário da vitrine.
 */
@RestController
@RequestMapping("v1/me/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ConversationSummaryDTO> listMine(@AuthenticationPrincipal Jwt jwt){
        return conversationService.listMine(userId(jwt));
    }

    /** Abrir marca como lida — por isso não é um GET puro do ponto de vista de efeito. */
    @GetMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.OK)
    public ConversationDetailDTO open(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable UUID conversationId){
        return conversationService.open(userId(jwt), conversationId);
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDTO send(@AuthenticationPrincipal Jwt jwt,
                           @PathVariable UUID conversationId,
                           @Valid @RequestBody MessageCreateDTO dto){
        return conversationService.send(userId(jwt), conversationId, dto);
    }

    /**
     * Porta da vitrine: o "Falar com" do perfil. Recebe o slug em vez do id do editor
     * porque é o que a página pública já tem em mãos.
     */
    @PostMapping("/with-editor/{editorSlug}")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationDetailDTO startWithEditor(@AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable String editorSlug,
                                                 @Valid @RequestBody MessageCreateDTO dto){
        return conversationService.startWithEditor(userId(jwt), editorSlug, dto);
    }

    private UUID userId(Jwt jwt){
        return UUID.fromString(jwt.getSubject());
    }
}
