package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.dto.JobRequestCreateDTO;
import com.devlil0.tramposeditor.dto.JobRequestDTO;
import com.devlil0.tramposeditor.dto.JobRequestFeedDTO;
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
 * Os pedidos do próprio usuário — publicados (como cliente) e candidaturas (como editor).
 * Base separada de /v1/job-requests pela mesma razão do perfil de editor: não misturar
 * leitura de feed com escrita do dono.
 */
@RestController
@RequestMapping("v1/me")
@RequiredArgsConstructor
public class MyJobRequestController {

    private final JobRequestService jobRequestService;

    @PostMapping("/job-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public JobRequestDTO create(@AuthenticationPrincipal Jwt jwt,
                                @Valid @RequestBody JobRequestCreateDTO dto){
        return jobRequestService.create(userId(jwt), dto);
    }

    @GetMapping("/job-requests")
    @ResponseStatus(HttpStatus.OK)
    public List<JobRequestDTO> listMine(@AuthenticationPrincipal Jwt jwt){
        return jobRequestService.listMine(userId(jwt));
    }

    @PostMapping("/job-requests/{jobId}/close")
    @ResponseStatus(HttpStatus.OK)
    public JobRequestDTO close(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID jobId){
        return jobRequestService.close(userId(jwt), jobId);
    }

    @PostMapping("/job-requests/{jobId}/interests/{interestId}/accept")
    @ResponseStatus(HttpStatus.OK)
    public JobRequestDTO acceptInterest(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable UUID jobId,
                                        @PathVariable UUID interestId){
        return jobRequestService.acceptInterest(userId(jwt), jobId, interestId);
    }

    /** Candidaturas que eu (editor) enviei. */
    @GetMapping("/job-interests")
    @ResponseStatus(HttpStatus.OK)
    public List<JobRequestFeedDTO> myInterests(@AuthenticationPrincipal Jwt jwt){
        return jobRequestService.myInterests(userId(jwt));
    }

    private UUID userId(Jwt jwt){
        return UUID.fromString(jwt.getSubject());
    }
}
