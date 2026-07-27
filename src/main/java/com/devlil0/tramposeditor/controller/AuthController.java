package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.dto.AuthResponseDTO;
import com.devlil0.tramposeditor.dto.LoginRequestDTO;
import com.devlil0.tramposeditor.dto.RegisterRequestDTO;
import com.devlil0.tramposeditor.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponseDTO register(@Valid @RequestBody RegisterRequestDTO dto){
        return authService.register(dto);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponseDTO login(@Valid @RequestBody LoginRequestDTO dto){
        return authService.login(dto);
    }
}
