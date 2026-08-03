package com.devlil0.tramposeditor.config;

import com.devlil0.tramposeditor.notification.NotificationProperties;
import com.devlil0.tramposeditor.service.SocialLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Fim do login social: resolve a conta e devolve a pessoa pro frontend com um código de
 * uso único.
 * <p>
 * O código vai na query string em vez do JWT de propósito — JWT em URL fica em log de
 * servidor, histórico do browser e cabeçalho Referer. O código vale dois minutos, serve
 * uma vez, e só o servidor do Next consegue trocá-lo por sessão.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final SocialLoginService socialLoginService;
    private final NotificationProperties properties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String provider = token.getAuthorizedClientRegistrationId();
        OAuth2User user = token.getPrincipal();

        try {
            String code = socialLoginService.completeLogin(
                    provider,
                    subjectOf(provider, user),
                    user.getAttribute("email"),
                    displayNameOf(user));

            redirect(response, "/entrar/callback?code=" + encode(code));
        } catch (RuntimeException e) {
            // Falhar aqui não pode despejar stack trace numa tela: manda o motivo curto
            // pro login, que sabe mostrar erro.
            log.warn("Login por {} não completou: {}", provider, e.getMessage());
            redirect(response, "/entrar?erro=" + encode(e.getMessage()));
        }
    }

    /**
     * O identificador estável da pessoa no provedor. Google usa {@code sub} (OIDC);
     * GitHub devolve um {@code id} numérico.
     */
    private String subjectOf(String provider, OAuth2User user){
        Object subject = "github".equals(provider) ? user.getAttribute("id") : user.getAttribute("sub");
        return String.valueOf(subject);
    }

    /** GitHub pode ter `name` vazio; aí o login (`login`) é o melhor que existe. */
    private String displayNameOf(OAuth2User user){
        String name = user.getAttribute("name");
        return name != null && !name.isBlank() ? name : user.getAttribute("login");
    }

    private void redirect(HttpServletResponse response, String path) throws IOException {
        response.sendRedirect(properties.link(path));
    }

    private String encode(String value){
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
