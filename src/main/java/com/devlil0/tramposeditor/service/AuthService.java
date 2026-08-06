package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.database.repository.IUserRepository;
import com.devlil0.tramposeditor.dto.AuthResponseDTO;
import com.devlil0.tramposeditor.dto.LoginRequestDTO;
import com.devlil0.tramposeditor.exception.UnauthorizedException;
import com.devlil0.tramposeditor.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Autenticação de quem já existe.
 * <p>
 * O cadastro saiu daqui em 2026-08-06 (V18) e virou o {@link SignupService}. Não foi só
 * mudança de endereço: {@code register} criava a conta e devolvia a sessão no mesmo passo,
 * o que fazia de "criar conta" o custo de um POST. Agora o cadastro nasce pendente e só
 * vira conta quando o código de seis dígitos volta do e-mail.
 * <p>
 * O que sobrou aqui é o que sempre foi desta classe: provar identidade e emitir sessão. O
 * {@code sanitizeRoles} foi junto pro serviço que agora decide quem entra.
 */
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

}
