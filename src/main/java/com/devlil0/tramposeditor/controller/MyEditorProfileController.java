package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.dto.EditorProfileDTO;
import com.devlil0.tramposeditor.dto.EditorProfileUpsertDTO;
import com.devlil0.tramposeditor.dto.PortfolioItemCreateDTO;
import com.devlil0.tramposeditor.service.EditorProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Gestão do próprio perfil de editor. Base separada de /v1/editors pra que a
 * vitrine possa ser 100% pública sem abrir estas rotas por engano no SecurityConfig.
 */
@RestController
@RequestMapping("v1/me/editor-profile")
@RequiredArgsConstructor
public class MyEditorProfileController {

    private final EditorProfileService editorProfileService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public EditorProfileDTO myProfile(@AuthenticationPrincipal Jwt jwt){
        return editorProfileService.getMyProfile(userId(jwt));
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public EditorProfileDTO upsert(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody EditorProfileUpsertDTO dto){
        return editorProfileService.upsertMyProfile(userId(jwt), dto);
    }

    @PostMapping("/portfolio")
    @ResponseStatus(HttpStatus.CREATED)
    public EditorProfileDTO addPortfolioItem(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PortfolioItemCreateDTO dto){
        return editorProfileService.addPortfolioItem(userId(jwt), dto);
    }

    @DeleteMapping("/portfolio/{itemId}")
    @ResponseStatus(HttpStatus.OK)
    public EditorProfileDTO removePortfolioItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID itemId){
        return editorProfileService.removePortfolioItem(userId(jwt), itemId);
    }

    private UUID userId(Jwt jwt){
        return UUID.fromString(jwt.getSubject());
    }
}
