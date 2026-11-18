package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.AuthExchangeCodeEntity;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.model.UserIdentityEntity;
import com.devlil0.tramposeditor.database.repository.IAuthExchangeCodeRepository;
import com.devlil0.tramposeditor.database.repository.IUserIdentityRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.AuthResponseDTO;
import com.devlil0.tramposeditor.dto.SignupOriginDTO;
import com.devlil0.tramposeditor.enums.UserRole;
import com.devlil0.tramposeditor.exception.SocialLoginFailure;
import com.devlil0.tramposeditor.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

import static com.devlil0.tramposeditor.exception.SocialLoginFailure.Reason.*;

/**
 * Login por provedor (Google, GitHub).
 * <p>
 * O identificador estável do provedor resolve vínculos existentes. Um vínculo novo
 * por e-mail exige prova emitida pelo provedor.
 * <p>
 * A troca por código existe porque o provedor devolve pro backend, mas a sessão mora num
 * cookie httpOnly do frontend. Ver {@link AuthExchangeCodeEntity}.
 */
@Service
@RequiredArgsConstructor
public class SocialLoginService {

    /** Segundos, não horas: a troca acontece no mesmo instante do redirecionamento. */
    private static final Duration VALIDADE_DO_CODIGO = Duration.ofMinutes(2);

    private final IUserRepository userRepository;
    private final IUserIdentityRepository identityRepository;
    private final IAuthExchangeCodeRepository codeRepository;
    private final AuthService authService;
    private final UsernameService usernameService;

    /**
     * Resolve quem entrou e devolve o código de uso único pro redirecionamento.
     *
     * @param provider    {@code google} ou {@code github}
     * @param subject     id da pessoa no provedor — estável, ao contrário do e-mail
     * @param verifiedEmail endereço comprovado pelo adaptador do provedor, ou nulo
     * @param displayName nome como o provedor conhece
     * @param hostedDomain domínio informado pelo ID token do Google, ou nulo
     * @param attempt tentativa recuperada da autorização OAuth validada
     */
    @Transactional
    public String completeLogin(String provider, String subject, String verifiedEmail,
                                String displayName, String hostedDomain, BrowserLoginAttempt attempt){
        if (attempt == null || !attempt.isValidAt(LocalDateTime.now())) {
            throw new SocialLoginFailure(EXPIRED);
        }
        if ((!"google".equals(provider) && !"github".equals(provider))
                || subject == null || subject.isBlank()) {
            throw new SocialLoginFailure(FAILED);
        }

        Resolved resolvido = resolveUser(provider, subject, verifiedEmail, displayName, hostedDomain);

        return issueCode(resolvido.user(), resolvido.contaNova(), attempt);
    }

    /**
     * Uma conta resolvida e se ela <b>acabou de nascer</b>.
     * <p>
     * O booleano viaja até a troca do código porque é lá que a origem da primeira visita
     * chega (o cookie é do frontend, e o frontend só entra em cena na troca). Sem ele, quem
     * já tem conta entrando de novo teria a origem de hoje gravada como se fosse a que o
     * captou meses atrás — ver V16.
     */
    private record Resolved(UserEntity user, boolean contaNova) {
    }

    private Resolved resolveUser(String provider, String subject, String verifiedEmail,
                                 String displayName, String hostedDomain){
        // 1. Já entrou por este provedor antes: é ele, mesmo que o e-mail tenha mudado.
        var jaVinculado = identityRepository.findByProviderAndSubject(provider, subject);
        if (jaVinculado.isPresent()) {
            return new Resolved(jaVinculado.get().getUser(), false);
        }

        if (verifiedEmail == null || verifiedEmail.isBlank()) {
            throw new SocialLoginFailure(EMAIL_UNVERIFIED);
        }
        String email = normalizeEmail(verifiedEmail);
        int at = email.lastIndexOf('@');
        if (at <= 0 || at >= email.length() - 1) {
            throw new SocialLoginFailure(EMAIL_UNVERIFIED);
        }

        var existente = userRepository.findByEmail(email);
        String domain = email.substring(at + 1);
        if (existente.isPresent() && "google".equals(provider)
                && !"gmail.com".equals(domain) && !domain.equalsIgnoreCase(hostedDomain)) {
            throw new SocialLoginFailure(LINK_NOT_ALLOWED);
        }
        UserEntity user = existente.orElseGet(() -> createUser(email, displayName));

        if (!user.isActive()) {
            throw new SocialLoginFailure(ACCOUNT_INACTIVE);
        }

        identityRepository.save(UserIdentityEntity.builder()
                .user(user)
                .provider(provider)
                .subject(subject)
                .build());

        return new Resolved(user, existente.isEmpty());
    }

    /**
     * Conta nova por provedor nasce <b>sem senha</b>: forçar um hash falso mentiria sobre
     * o que a conta é. Quem quiser senha usa o "esqueci a senha" — o fluxo já serve pra
     * definir a primeira.
     */
    private UserEntity createUser(String email, String displayName){
        String nome = displayName == null || displayName.isBlank()
                ? email.split("@")[0]
                : displayName.trim();

        return userRepository.saveAndFlush(UserEntity.builder()
                .email(email)
                .displayName(nome)
                // Aqui não há formulário onde perguntar, então o nome nasce derivado — mas
                // provisório: `usernameChosen = false` manda a pessoa pra tela de escolha
                // antes de ela navegar. Entrar pelo Google não pode dar um endereço que
                // ninguém decidiu, que era o caso até a V15.
                .username(usernameService.gerarPara(nome))
                .usernameChosen(false)
                // O endereço passou pela prova exigida para contas novas.
                .emailVerifiedAt(LocalDateTime.now())
                // Todo mundo entra como cliente; virar editor continua sendo publicar o
                // perfil, exatamente como no cadastro por senha (FUNDACAO §11.9).
                .roles(Set.of(UserRole.CLIENT))
                .active(true)
                .build());
    }

    private String issueCode(UserEntity user, boolean contaNova, BrowserLoginAttempt attempt){
        if (!user.isActive()) {
            throw new SocialLoginFailure(ACCOUNT_INACTIVE);
        }
        String code = TokenSecrets.generate();
        LocalDateTime expiresAt = LocalDateTime.now().plus(VALIDADE_DO_CODIGO);

        codeRepository.save(AuthExchangeCodeEntity.builder()
                .user(user)
                .codeHash(exchangeHash(code, attempt.challenge()))
                .newAccount(contaNova)
                .expiresAt(expiresAt.isBefore(attempt.expiresAt()) ? expiresAt : attempt.expiresAt())
                .build());

        return code;
    }

    /**
     * A troca: o frontend manda o código de servidor pra servidor e recebe o JWT.
     * <p>
     * É aqui que a origem da primeira visita entra no login social — e só aqui dá pra
     * fazer isso. A conta nasce no callback do provedor, mas quem tem o cookie de primeira
     * visita é o frontend, que só aparece nesta chamada.
     */
    @Transactional
    public AuthResponseDTO exchange(String code, String browserProof, SignupOriginDTO origin){
        // A trava fica até o commit. Outra troca do mesmo código espera e então
        // lê o used_at já gravado pela primeira.
        AuthExchangeCodeEntity entity = codeRepository.findLockedByCodeHash(
                        exchangeHash(code, BrowserLoginAttempt.challengeOf(browserProof)))
                .filter(c -> c.isUsable(LocalDateTime.now()))
                .orElseThrow(() -> new UnauthorizedException("Código de login inválido ou expirado"));

        UserEntity user = entity.getUser();
        if (!user.isActive()) {
            throw new UnauthorizedException("Conta desativada");
        }

        entity.setUsedAt(LocalDateTime.now());
        codeRepository.save(entity);

        if (entity.isNewAccount()) {
            SignupOrigin.apply(user, origin);
            userRepository.save(user);
        }

        return authService.buildResponse(user);
    }

    private String exchangeHash(String code, String challenge) {
        return TokenSecrets.hash(code + ":" + challenge);
    }

    private String normalizeEmail(String email){
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
