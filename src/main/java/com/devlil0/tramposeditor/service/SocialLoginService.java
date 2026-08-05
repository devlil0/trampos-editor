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
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

/**
 * Login por provedor (Google, GitHub).
 * <p>
 * A regra que manda é a <b>conta única</b> do {@code FUNDACAO} §11.9: mesmo e-mail é a
 * mesma pessoa. Entrar pelo Google num e-mail que já tem conta com senha <b>vincula</b>
 * o provedor à conta existente; nunca cria uma segunda. Duas contas com o mesmo e-mail
 * seria o começo de "cadê meu perfil?" — o pior primeiro contato possível.
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
     * @param email       e-mail verificado no provedor
     * @param displayName nome como o provedor conhece
     */
    @Transactional
    public String completeLogin(String provider, String subject, String email, String displayName){
        if (email == null || email.isBlank()) {
            // GitHub esconde o e-mail quando a pessoa marca como privado. Sem e-mail não
            // dá pra aplicar a conta única, e criar conta sem ele produziria a duplicata
            // que este serviço inteiro existe pra evitar.
            throw new BusinessException(
                    "Seu %s não expôs um e-mail. Libere o e-mail no provedor ou crie a conta com senha."
                            .formatted(provider));
        }

        Resolved resolvido = resolveUser(provider, subject, normalizeEmail(email), displayName);

        return issueCode(resolvido.user(), resolvido.contaNova());
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

    private Resolved resolveUser(String provider, String subject, String email, String displayName){
        // 1. Já entrou por este provedor antes: é ele, mesmo que o e-mail tenha mudado.
        var jaVinculado = identityRepository.findByProviderAndSubject(provider, subject);
        if (jaVinculado.isPresent()) {
            return new Resolved(jaVinculado.get().getUser(), false);
        }

        // 2. Existe conta com este e-mail: vincula em vez de duplicar.
        var existente = userRepository.findByEmail(email);
        UserEntity user = existente.orElseGet(() -> createUser(email, displayName));

        if (!user.isActive()) {
            throw new UnauthorizedException("Conta desativada");
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
                // Já nasce verificado: o Google e o GitHub só entregam e-mail confirmado, e
                // pedir a mesma prova de novo seria desconfiar de uma melhor que a nossa
                // (V17).
                .emailVerifiedAt(LocalDateTime.now())
                // Todo mundo entra como cliente; virar editor continua sendo publicar o
                // perfil, exatamente como no cadastro por senha (FUNDACAO §11.9).
                .roles(Set.of(UserRole.CLIENT))
                .active(true)
                .build());
    }

    private String issueCode(UserEntity user, boolean contaNova){
        String code = TokenSecrets.generate();

        codeRepository.save(AuthExchangeCodeEntity.builder()
                .user(user)
                .codeHash(TokenSecrets.hash(code))
                .newAccount(contaNova)
                .expiresAt(LocalDateTime.now().plus(VALIDADE_DO_CODIGO))
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
    public AuthResponseDTO exchange(String code, SignupOriginDTO origin){
        AuthExchangeCodeEntity entity = codeRepository.findByCodeHash(TokenSecrets.hash(code))
                .filter(c -> c.isUsable(LocalDateTime.now()))
                .orElseThrow(() -> new UnauthorizedException("Código de login inválido ou expirado"));

        entity.setUsedAt(LocalDateTime.now());
        codeRepository.save(entity);

        UserEntity user = entity.getUser();
        if (entity.isNewAccount()) {
            SignupOrigin.apply(user, origin);
            userRepository.save(user);
        }

        return authService.buildResponse(user);
    }

    private String normalizeEmail(String email){
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
