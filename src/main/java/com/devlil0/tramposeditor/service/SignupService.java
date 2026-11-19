package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.PendingSignupEntity;
import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IPendingSignupRepository;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.AuthResponseDTO;
import com.devlil0.tramposeditor.dto.RegisterRequestDTO;
import com.devlil0.tramposeditor.dto.SignupOriginDTO;
import com.devlil0.tramposeditor.dto.SignupStartedDTO;
import com.devlil0.tramposeditor.enums.UserRole;
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.exception.PostgresIntegrityViolation;
import com.devlil0.tramposeditor.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cadastro em dois passos: pedir e confirmar (V18).
 * <p>
 * <b>O problema:</b> criar conta custava um POST. Um script com uma lista de endereços
 * enchia {@code users} sem provar posse de nenhum deles, e cada linha dessas queima um
 * username, aparece em consulta e conta como usuário numa métrica que deveria significar
 * gente. A verificação de e-mail da V17 não cobria isso porque é posterior — a conta já
 * existe quando o link é mandado, e o custo do bot é criar, não usar.
 * <p>
 * <b>A troca:</b> o cadastro passa a nascer em {@code pending_signups} e só vira conta
 * quando a pessoa devolve o código de seis dígitos que foi pro e-mail. O que o bot precisa
 * pra passar deixou de ser "mandar um POST" e virou "ler uma caixa de entrada que ele
 * controla" — e aí ele já está pagando por endereço, que é o custo que se queria impor.
 * <p>
 * <b>O que isto custa, dito em voz alta:</b> o cadastro passa a depender do e-mail sair de
 * verdade. Para conferir o código em desenvolvimento, use um capturador SMTP local.
 * Em produção, o cadastro depende do transporte SMTP configurado. É uma
 * dependência dura, não um detalhe de configuração.
 * <p>
 * Quem entra por Google ou GitHub não passa por aqui: o provedor já provou o endereço, e
 * exigir código depois disso seria pedir a prova duas vezes.
 */
@Service
@RequiredArgsConstructor
public class SignupService {

    /**
     * Prazo curto de propósito, e é ele que faz o trabalho que os seis dígitos não fazem.
     * Dez minutos é o que uma pessoa leva pra ir na caixa de entrada e voltar; é pouco pra
     * quem quer varrer o espaço de códigos.
     */
    private static final Duration VALIDADE = Duration.ofMinutes(10);

    /**
     * Tentativas por código. Cinco chutes em um milhão de combinações é 1 em 200 mil — e o
     * cadastro morre na quinta, obrigando um código novo, que custa outro e-mail e outro
     * prazo. Contar erro é o que impede transformar "seis dígitos" em "sem segredo".
     */
    private static final short MAX_TENTATIVAS = 5;

    /**
     * Mensagem única pra código errado, vencido e inexistente.
     * <p>
     * Mesma regra do {@code PasswordResetService}: distinguir contaria a quem está chutando
     * se existe um cadastro pendente naquele endereço — que é justamente a informação que
     * uma varredura quer.
     */
    private static final String CODIGO_INVALIDO =
            "Código inválido ou expirado. Peça um novo.";

    private final IPendingSignupRepository pendingRepository;
    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsernameService usernameService;
    private final NotificationService notificationService;
    private final AuthService authService;
    private final SignupCodes signupCodes;
    private final SignupAttempts signupAttempts;
    private final TransactionTemplate transactions;

    private static class SignupRejected extends BusinessException {
        SignupRejected(String message) {
            super(message);
        }
    }

    /**
     * Passo 1: guarda o cadastro e manda o código. <b>Não cria conta.</b>
     */
    public SignupStartedDTO start(RegisterRequestDTO dto){
        // A limpeza libera suas linhas antes de persistir e enviar um código.
        transactions.executeWithoutResult(status -> pendingRepository.deleteExpired(LocalDateTime.now()));
        return transactions.execute(status -> createPending(dto));
    }

    private SignupStartedDTO createPending(RegisterRequestDTO dto) {
        String email = normalizeEmail(dto.email());
        String username = usernameService.normalizar(dto.username());
        usernameService.bloquearParaEscrita(username);
        var previous = pendingRepository.findLockedForStart(email, username, LocalDateTime.now());

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Já existe uma conta com este e-mail");
        }

        String problemaNoUsername = usernameService.validarParaCadastro(username, email);
        if (problemaNoUsername != null) {
            throw new BusinessException(problemaNoUsername);
        }

        // Username preso por outro cadastro pendente. Recusar aqui, e não na confirmação:
        // é agora que a pessoa tem o campo na frente pra corrigir. Deixar passar faria dois
        // cadastros receberem código e o segundo a confirmar levar erro tendo feito tudo
        // certo.
        String previousHash = previous.stream().filter(pending -> pending.getEmail().equals(email))
                .map(PendingSignupEntity::getCodeHash).findFirst().orElse(null);
        pendingRepository.deleteAll(previous);
        pendingRepository.flush();

        String codigo = signupCodes.generateDifferentFrom(previousHash);

        PendingSignupEntity pendente = PendingSignupEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(dto.password()))
                .displayName(dto.displayName().trim())
                .username(username)
                .roles(serializeRoles(sanitizeRoles(dto.roles())))
                .codeHash(TokenSecrets.hash(codigo))
                .attempts((short) 0)
                .expiresAt(LocalDateTime.now().plus(VALIDADE))
                .build();

        aplicarOrigem(pendente, dto.origin());

        try {
            pendingRepository.saveAndFlush(pendente);
        } catch (DataIntegrityViolationException e) {
            var duplicate = PostgresIntegrityViolation.from(e).duplicate();
            if (duplicate != PostgresIntegrityViolation.Duplicate.SIGNUP_EMAIL
                    && duplicate != PostgresIntegrityViolation.Duplicate.SIGNUP_USERNAME) {
                throw e;
            }
            throw new BusinessException(
                    "Esse nome de usuário ou e-mail acabou de ser pedido. Tente de novo.");
        }

        notificationService.signupCode(pendente.getEmail(), pendente.getDisplayName(), codigo);

        return new SignupStartedDTO(pendente.getEmail(), (int) VALIDADE.toSeconds());
    }

    /**
     * Passo 2: confere o código e cria a conta de verdade.
     * <p>
     * A conta nasce com o e-mail já verificado — o código <i>é</i> a prova de posse, e a
     * mesma que o link da V17 produziria. Pedir o link depois disso seria provar duas vezes.
     */
    @Transactional(noRollbackFor = SignupRejected.class)
    public AuthResponseDTO confirm(String emailBruto, String codigo){
        String email = normalizeEmail(emailBruto);

        String username = pendingRepository.findUsernameByEmail(email)
                .orElseThrow(() -> new SignupRejected(CODIGO_INVALIDO));
        usernameService.bloquearParaEscrita(username);
        PendingSignupEntity pendente = pendingRepository.findLockedByEmail(email)
                .filter(pending -> pending.getUsername().equals(username))
                .orElseThrow(() -> new SignupRejected(CODIGO_INVALIDO));

        // As recusas esperadas preservam o contador ou a remoção da pendência.
        // Falhas inesperadas continuam revertendo a transação inteira.
        if (pendente.isExpired(LocalDateTime.now())) {
            pendingRepository.delete(pendente);
            throw new SignupRejected(CODIGO_INVALIDO);
        }

        if (!TokenSecrets.hash(codigo).equals(pendente.getCodeHash())) {
            signupAttempts.registrarErro(pendente.getId(), MAX_TENTATIVAS);
            throw new SignupRejected(CODIGO_INVALIDO);
        }

        // Entre o pedido e a confirmação cabe outro cadastro com o mesmo e-mail ou username.
        if (userRepository.existsByEmail(email)) {
            pendingRepository.delete(pendente);
            throw new SignupRejected("Já existe uma conta com este e-mail");
        }
        if (userRepository.existsByUsername(username)) {
            pendingRepository.delete(pendente);
            throw new SignupRejected("Esse nome de usuário acabou de ser registrado. Tente outro.");
        }

        UserEntity user = UserEntity.builder()
                .email(pendente.getEmail())
                .passwordHash(pendente.getPasswordHash())
                .displayName(pendente.getDisplayName())
                .username(pendente.getUsername())
                .usernameChosen(true)
                .roles(deserializeRoles(pendente.getRoles()))
                .active(true)
                // O código provou o endereço. Este é o ponto da feature inteira.
                .emailVerifiedAt(LocalDateTime.now())
                .build();

        user.recordSignupOrigin(
                pendente.getSignupReferrer(),
                pendente.getSignupUtmSource(),
                pendente.getSignupLanding());

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            var duplicate = PostgresIntegrityViolation.from(e).duplicate();
            if (duplicate != PostgresIntegrityViolation.Duplicate.ACCOUNT_EMAIL
                    && duplicate != PostgresIntegrityViolation.Duplicate.ACCOUNT_USERNAME) {
                throw e;
            }
            throw new BusinessException(
                    "Esse nome de usuário ou e-mail acabou de ser registrado. Tente outro.");
        }

        pendingRepository.delete(pendente);

        return authService.buildResponse(user);
    }

    /**
     * Manda outro código pro mesmo cadastro.
     * <p>
     * Silencioso quando não há cadastro pendente: responder "não existe" transformaria este
     * endereço num verificador de quem está no meio de um cadastro.
     */
    @Transactional
    public void resend(String emailBruto){
        String email = normalizeEmail(emailBruto);

        pendingRepository.findLockedByEmail(email).ifPresent(pendente -> {
            if (pendente.isExpired(LocalDateTime.now())) {
                pendingRepository.delete(pendente);
                return;
            }

            String codigo = signupCodes.generateDifferentFrom(pendente.getCodeHash());
            pendente.setCodeHash(TokenSecrets.hash(codigo));
            // O contador zera junto: o código novo merece as tentativas dele. Sem isto,
            // quem errou quatro vezes recebe um código que já nasce com uma chance só.
            pendente.setAttempts((short) 0);
            pendente.setExpiresAt(LocalDateTime.now().plus(VALIDADE));
            pendingRepository.save(pendente);

            notificationService.signupCode(pendente.getEmail(), pendente.getDisplayName(), codigo);
        });
    }

    private void aplicarOrigem(PendingSignupEntity pendente, SignupOriginDTO origin){
        if (origin == null) return;

        pendente.setSignupReferrer(truncar(origin.referrer(), 255));
        pendente.setSignupUtmSource(truncar(origin.utmSource(), 120));
        pendente.setSignupLanding(truncar(origin.landing(), 255));
    }

    /** Trunca em vez de recusar — medição não pode custar um cadastro (ver {@code SignupOrigin}). */
    private String truncar(String valor, int max){
        if (valor == null) return null;
        String limpo = valor.trim();
        if (limpo.isEmpty()) return null;
        return limpo.length() <= max ? limpo : limpo.substring(0, max);
    }

    private String normalizeEmail(String email){
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String serializeRoles(Set<UserRole> roles){
        return roles.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private Set<UserRole> deserializeRoles(String roles){
        return Arrays.stream(roles.split(","))
                .filter(s -> !s.isBlank())
                .map(UserRole::valueOf)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * ADMIN não é auto-atribuível — mesma regra do {@code AuthService.register}, e ela
     * precisa estar aqui também porque este virou o caminho por onde o cadastro entra.
     */
    private Set<UserRole> sanitizeRoles(Set<UserRole> requested){
        Set<UserRole> roles = requested.stream()
                .filter(role -> role != UserRole.ADMIN)
                .collect(Collectors.toCollection(HashSet::new));

        if (roles.isEmpty()) {
            throw new BusinessException("Escolha ao menos um perfil: EDITOR ou CLIENT");
        }

        return roles;
    }
}
