package com.devlil0.tramposeditor.controller;

import com.devlil0.tramposeditor.dto.AuthResponseDTO;
import com.devlil0.tramposeditor.dto.EmailVerificationConfirmDTO;
import com.devlil0.tramposeditor.dto.LoginRequestDTO;
import com.devlil0.tramposeditor.dto.PasswordResetConfirmDTO;
import com.devlil0.tramposeditor.dto.PasswordResetRequestDTO;
import com.devlil0.tramposeditor.dto.RegisterRequestDTO;
import com.devlil0.tramposeditor.dto.SignupConfirmDTO;
import com.devlil0.tramposeditor.dto.SignupStartedDTO;
import com.devlil0.tramposeditor.dto.SocialExchangeDTO;
import com.devlil0.tramposeditor.service.AuthService;
import com.devlil0.tramposeditor.service.EmailVerificationService;
import com.devlil0.tramposeditor.service.PasswordResetService;
import com.devlil0.tramposeditor.service.SignupService;
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
    private final SignupService signupService;
    private final PasswordResetService passwordResetService;
    private final SocialLoginService socialLoginService;
    private final EmailVerificationService emailVerificationService;

    /**
     * Passo 1 do cadastro: guarda o que foi preenchido e manda o código (V18).
     * <p>
     * <b>Devolve 202, e não 201, porque nada foi criado.</b> Até 2026-08-06 este endpoint
     * criava a conta e já entregava a sessão — o que fazia de "criar conta" o custo de um
     * POST, e enchia {@code users} de linha que nunca provou posse do endereço. O 201 com
     * {@code AuthResponseDTO} saiu junto: sessão só existe depois do código.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SignupStartedDTO register(@Valid @RequestBody RegisterRequestDTO dto){
        return signupService.start(dto);
    }

    /** Passo 2: o código de volta. É aqui que a conta nasce, e com o e-mail já provado. */
    @PostMapping("/register/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponseDTO confirmRegistration(@Valid @RequestBody SignupConfirmDTO dto){
        return signupService.confirm(dto.email(), dto.code());
    }

    /**
     * Outro código pro mesmo cadastro. Sempre 204, exista o pendente ou não — responder
     * diferente transformaria isto num verificador de quem está no meio de um cadastro.
     */
    @PostMapping("/register/resend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendSignupCode(@Valid @RequestBody PasswordResetRequestDTO dto){
        signupService.resend(dto.email());
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
        return socialLoginService.exchange(dto.code(), dto.origin());
    }

    /**
     * Confirma o e-mail pelo link recebido. Público porque a pessoa pode abrir o link em
     * outro navegador, ou dias depois, sem sessão nenhuma — exigir login aqui transformaria
     * uma confirmação de um clique num "entre primeiro".
     */
    @PostMapping("/email-verification/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmEmail(@Valid @RequestBody EmailVerificationConfirmDTO dto){
        emailVerificationService.confirm(dto.token());
    }
}
