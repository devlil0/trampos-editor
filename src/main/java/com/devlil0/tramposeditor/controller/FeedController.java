package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.dto.EditorSummaryDTO;
import com.devlil0.tramposeditor.dto.PageResponseDTO;
import com.devlil0.tramposeditor.enums.EditorNiche;
import com.devlil0.tramposeditor.service.EditorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * O feed da oferta — editores disponíveis, pro cliente logado navegar.
 * <p>
 * Existe separado da vitrine (`/v1/editors`) de propósito, e a diferença não é técnica:
 * <ul>
 *   <li><b>vitrine</b> é pública e indexável — é o funil de aquisição, precisa que o
 *       Google entre; mostra todo mundo, disponível ou não, e aceita a bateria inteira
 *       de filtros;</li>
 *   <li><b>feed</b> exige login e só mostra quem está <b>disponível</b> agora, com um
 *       recorte só: categoria. É pra quem já está dentro e quer ver o que tem hoje.</li>
 * </ul>
 * O par disso é `/v1/job-requests`, o feed da demanda. Os dois lados do mercado, mesma
 * forma: uma categoria, uma página por vez, só pra quem tem conta.
 * <p>
 * Nada aqui entra no `permitAll` do {@code SecurityConfig} — o `anyRequest().authenticated()`
 * é o que faz o feed ser da área logada.
 */
@RestController
@RequestMapping("v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final EditorProfileService editorProfileService;

    @GetMapping("/editors")
    @ResponseStatus(HttpStatus.OK)
    public PageResponseDTO<EditorSummaryDTO> editors(
            @RequestParam(required = false) EditorNiche niche,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){

        // `available = true` fixo: o feed é "quem pega trampo agora". Quem se marcou
        // ocupado sai daqui e continua na vitrine — marcar-se ocupado não pode custar
        // a existência do perfil (ESBOCO §6, "disponibilidade honesta").
        return editorProfileService.search(niche, null, null, true, null, null, page, size);
    }
}
