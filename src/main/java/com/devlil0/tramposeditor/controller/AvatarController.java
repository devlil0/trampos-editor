package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.service.AvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * Foto de perfil: quem sobe é o dono, quem lê é qualquer um.
 * <p>
 * A leitura é pública porque a foto aparece na vitrine e no perfil indexado — pedir token
 * pra carregar imagem de página pública quebraria a própria página.
 */
@RestController
@RequiredArgsConstructor
public class AvatarController {

    private final AvatarService avatarService;

    /**
     * Serve a imagem.
     * <p>
     * A URL por username sempre resolve a foto atual, mesmo com um `?v=` antigo.
     * Revalidar permite refletir troca, remoção e conta inativa. A versão é o ETag.
     */
    @GetMapping("/v1/avatars/{username}")
    public ResponseEntity<byte[]> get(@PathVariable String username){
        return avatarService.byUsername(username)
                .map(avatar -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(avatar.getContentType()))
                        .cacheControl(CacheControl.noCache().cachePublic().mustRevalidate())
                        .eTag(avatar.getVersion().toString())
                        .body(avatar.getBytes()))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .cacheControl(CacheControl.noStore()).build());
    }

    @PutMapping(value = "/v1/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> upload(@AuthenticationPrincipal Jwt jwt,
                                      @RequestPart("file") MultipartFile file){
        UUID version = avatarService.replace(UUID.fromString(jwt.getSubject()), file);
        return Map.of("version", version.toString());
    }

    @DeleteMapping("/v1/me/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal Jwt jwt){
        avatarService.remove(UUID.fromString(jwt.getSubject()));
    }
}
