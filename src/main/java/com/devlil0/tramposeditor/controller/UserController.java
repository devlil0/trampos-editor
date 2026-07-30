package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.dto.UserResponseDTO;
import com.devlil0.tramposeditor.dto.UserUpdateDTO;
import com.devlil0.tramposeditor.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
}
