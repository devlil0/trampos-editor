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

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO dto){
        String email = normalizeEmail(dto.email());

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Já existe uma conta com este e-mail");
        }

        UserEntity user = UserEntity.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(dto.password()))
                .displayName(dto.displayName().trim())
                .roles(sanitizeRoles(dto.roles()))
                .active(true)
                .build();

        // saveAndFlush: sem o flush o @CreationTimestamp ainda não foi gerado
        // e o createdAt sairia null na resposta do cadastro.
        userRepository.saveAndFlush(user);

        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO dto){
        UserEntity user = userRepository.findByEmail(normalizeEmail(dto.email()))
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Conta desativada");
        }

        return buildResponse(user);
    }

    private AuthResponseDTO buildResponse(UserEntity user){
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
