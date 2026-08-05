package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.dto.PublicProfileDTO;
import com.devlil0.tramposeditor.service.PublicProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A página pública de uma pessoa, por username.
 * <p>
 * Sem autenticação, como a vitrine: é ativo de aquisição, precisa que o Google entre.
 * Serve tanto editor quanto cliente — quem não publicou perfil de editor vem com
 * {@code editor: null} e só os números de participação.
 */
@RestController
@RequestMapping("v1/profiles")
@RequiredArgsConstructor
public class PublicProfileController {

    private final PublicProfileService publicProfileService;

    @GetMapping("/{username}")
    @ResponseStatus(HttpStatus.OK)
    public PublicProfileDTO byUsername(@PathVariable String username){
        return publicProfileService.getByUsername(username);
    }

    /**
     * Pra onde uma URL antiga de editor deve apontar agora.
     * <p>
     * O frontend chama isto pra montar o 301 de {@code /editores/{slug}} → {@code /{username}}.
     * Devolve 404 quando o slug não existe, pra que o link morto continue sendo 404 em vez
     * de virar redirecionamento pra lugar nenhum.
     */
    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<Map<String, String>> usernameDoSlug(@PathVariable String slug){
        return publicProfileService.usernameDoSlug(slug)
                .map(username -> ResponseEntity.ok(Map.of("username", username)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
