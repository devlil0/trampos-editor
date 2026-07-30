package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.dto.EditorProfileDTO;
import com.devlil0.tramposeditor.dto.EditorSummaryDTO;
import com.devlil0.tramposeditor.dto.PageResponseDTO;
import com.devlil0.tramposeditor.enums.EditorNiche;
import com.devlil0.tramposeditor.enums.EditorStyle;
import com.devlil0.tramposeditor.enums.EditorTool;
import com.devlil0.tramposeditor.service.EditorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vitrine pública — o coração do MVP. Sem autenticação de propósito: são as
 * páginas indexáveis (SEO long-tail) que trazem cliente organicamente.
 */
@RestController
@RequestMapping("v1/editors")
@RequiredArgsConstructor
public class EditorController {

    private final EditorProfileService editorProfileService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponseDTO<EditorSummaryDTO> list(
            @RequestParam(required = false) EditorNiche niche,
            @RequestParam(required = false) EditorTool tool,
            @RequestParam(required = false) EditorStyle style,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Integer maxPriceCents,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){

        return editorProfileService.search(niche, tool, style, available, maxPriceCents, q, page, size);
    }

    @GetMapping("/{slug}")
    @ResponseStatus(HttpStatus.OK)
    public EditorProfileDTO getBySlug(@PathVariable String slug){
        return editorProfileService.getBySlug(slug);
    }
}
