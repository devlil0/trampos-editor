package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.dto.UserResponseDTO;
import com.devlil0.tramposeditor.dto.UserUpdateDTO;
import com.devlil0.tramposeditor.dto.UsernameUpdateDTO;
import com.devlil0.tramposeditor.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserResponseDTO me(@AuthenticationPrincipal Jwt jwt){
        return userService.findById(UUID.fromString(jwt.getSubject()));
    }

    /** Edita os dados da própria conta. Hoje: o Instagram, que vale pros dois papéis. */
    @PatchMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserResponseDTO updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserUpdateDTO dto){
        return userService.updateMe(UUID.fromString(jwt.getSubject()), dto);
    }

    /**
     * Escolhe ou troca o username. Endpoint próprio, e não mais um campo no PATCH acima,
     * porque muda o <b>endereço público</b> da pessoa e tem regra própria — unicidade,
     * nome reservado e cooldown. Junto no mesmo submit, um "já está em uso" faria parecer
     * que o Instagram também não salvou.
     */
    @PutMapping("/me/username")
    @ResponseStatus(HttpStatus.OK)
    public UserResponseDTO trocarUsername(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody UsernameUpdateDTO dto){
        return userService.trocarUsername(UUID.fromString(jwt.getSubject()), dto);
    }
}
