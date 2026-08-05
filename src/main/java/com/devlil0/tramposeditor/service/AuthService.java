package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.AuthResponseDTO;
import com.devlil0.tramposeditor.dto.LoginRequestDTO;
import com.devlil0.tramposeditor.dto.RegisterRequestDTO;
import com.devlil0.tramposeditor.enums.UserRole;
import com.devlil0.tramposeditor.exception.BusinessException;
import com.devlil0.tramposeditor.exception.UnauthorizedException;
import com.devlil0.tramposeditor.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    /** Mensagem única pra login e senha errados — não entregar quais e-mails existem na base. */
    private static final String INVALID_CREDENTIALS = "E-mail ou senha inválidos";

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final UsernameService usernameService;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO dto){
        String email = normalizeEmail(dto.email());

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Já existe uma conta com este e-mail");
        }

        String displayName = dto.displayName().trim();

        // O username é escolhido no formulário desde a V15 — era gerado, e por isso ninguém
        // nunca decidiu o próprio endereço. A mesma validação serve a checagem que roda
        // enquanto a pessoa digita (GET /v1/usernames/availability), então o que ela leu
        // no campo é o que vale aqui.
        String username = usernameService.normalizar(dto.username());
        String problemaNoUsername = usernameService.validar(username, null);
        if (problemaNoUsername != null) {
            throw new BusinessException(problemaNoUsername);
        }

        UserEntity user = UserEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(dto.password()))
                .displayName(displayName)
                .username(username)
                .usernameChosen(true)
                .roles(sanitizeRoles(dto.roles()))
                .active(true)
                .build();

        try {
            // saveAndFlush: sem o flush o @CreationTimestamp ainda não foi gerado
            // e o createdAt sairia null na resposta do cadastro.
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Entre o `validar` acima e este insert cabe outro cadastro com o mesmo nome
            // ou o mesmo e-mail. É o UNIQUE que decide de verdade quem ficou com ele —
            // sem isto a corrida vira 500 e a pessoa não fica sabendo o que houve.
            throw new BusinessException(
                    "Esse nome de usuário ou e-mail acabou de ser registrado. Tente outro.");
        }

        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO dto){
        UserEntity user = userRepository.findByEmail(normalizeEmail(dto.email()))
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));

        // Conta criada só pelo Google/GitHub não tem senha. Devolve a MESMA mensagem de
        // credencial inválida: dizer "essa conta usa Google" contaria a um estranho como
        // a pessoa entra, e ela mesma descobre isso pelo botão do provedor.
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Conta desativada");
        }

        return buildResponse(user);
    }

    /**
     * A sessão de uma conta já resolvida. Público porque o login social termina fora
     * daqui ({@link SocialLoginService}) e precisa emitir exatamente o mesmo token —
     * entrar pelo Google não pode produzir uma sessão de segunda classe.
     */
    public AuthResponseDTO buildResponse(UserEntity user){
        return new AuthResponseDTO(
                jwtService.generateToken(user),
                TOKEN_TYPE,
                jwtService.getTtlSeconds(),
                userMapper.toDTO(user)
        );
    }

    private String normalizeEmail(String email){
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * ADMIN não é auto-atribuível: quem se cadastra escolhe apenas EDITOR e/ou CLIENT.
     * Sem isso, um POST /v1/auth/register com "roles":["ADMIN"] viraria escalação de privilégio.
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
