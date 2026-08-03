package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.dto.AuthResponseDTO;
import com.devlil0.tramposeditor.dto.LoginRequestDTO;
import com.devlil0.tramposeditor.dto.PasswordResetConfirmDTO;
import com.devlil0.tramposeditor.dto.PasswordResetRequestDTO;
import com.devlil0.tramposeditor.dto.RegisterRequestDTO;
import com.devlil0.tramposeditor.dto.SocialExchangeDTO;
import com.devlil0.tramposeditor.service.AuthService;
import com.devlil0.tramposeditor.service.PasswordResetService;
import com.devlil0.tramposeditor.service.SocialLoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final SocialLoginService socialLoginService;

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

    /**
     * Pedir o link de redefinição. Sempre 204, exista a conta ou não — responder
     * diferente entregaria quais e-mails estão cadastrados.
     */
    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequestDTO dto){
        passwordResetService.request(dto.email());
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmDTO dto){
        passwordResetService.confirm(dto);
    }

    /**
     * Fecha o login social: o frontend troca o código de uso único que veio no
     * redirecionamento por uma sessão. Servidor a servidor — o browser nunca vê o JWT.
     */
    @PostMapping("/social/exchange")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponseDTO exchangeSocialCode(@Valid @RequestBody SocialExchangeDTO dto){
        return socialLoginService.exchange(dto.code());
    }
}
