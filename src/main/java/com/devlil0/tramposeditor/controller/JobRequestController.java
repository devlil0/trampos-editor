package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.dto.JobInterestCreateDTO;
import com.devlil0.tramposeditor.dto.JobRequestFeedDTO;
import com.devlil0.tramposeditor.enums.EditorNiche;
import com.devlil0.tramposeditor.service.JobRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Feed de pedidos pro editor. Exige login de propósito: pedido aberto não é conteúdo
 * público — evita virar lista de e-mails/briefings pra raspagem.
 */
@RestController
@RequestMapping("v1/job-requests")
@RequiredArgsConstructor
public class JobRequestController {

    private final JobRequestService jobRequestService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<JobRequestFeedDTO> feed(@AuthenticationPrincipal Jwt jwt,
                                        @RequestParam(required = false) EditorNiche niche){
        return jobRequestService.feed(userId(jwt), niche);
    }

    @PostMapping("/{jobId}/interests")
    @ResponseStatus(HttpStatus.CREATED)
    public JobRequestFeedDTO expressInterest(@AuthenticationPrincipal Jwt jwt,
                                             @PathVariable UUID jobId,
                                             @Valid @RequestBody JobInterestCreateDTO dto){
        return jobRequestService.expressInterest(userId(jwt), jobId, dto);
    }

    private UUID userId(Jwt jwt){
        return UUID.fromString(jwt.getSubject());
    }
}
