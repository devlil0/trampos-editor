package com.devlil0.tramposeditor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Obtém a prova de e-mail do GitHub em /user/emails, inclusive para perfis
 * que já expõem um endereço público em /user.
 */
@Slf4j
@Component
public class GitHubEmailOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String GITHUB = "github";
    private static final String EMAILS_URI = "https://api.github.com/user/emails";
    private static final String EMAIL_ATTRIBUTE = "email";
    static final String VERIFIED_EMAIL_ATTRIBUTE = "tramposeditor_verified_email";

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;
    private final RestClient restClient;

    public GitHubEmailOAuth2UserService() {
        this(new DefaultOAuth2UserService(), RestClient.create());
    }

    GitHubEmailOAuth2UserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate, RestClient restClient) {
        this.delegate = delegate;
        this.restClient = restClient;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User user = delegate.loadUser(request);

        boolean isGitHub = GITHUB.equals(request.getClientRegistration().getRegistrationId());
        if (!isGitHub) {
            return user;
        }

        String email = buscarEmailPrincipal(request.getAccessToken().getTokenValue());
        // DefaultOAuth2User exige que o mapa contenha a chave usada como nome do
        // principal — por isso copiamos os atributos originais em vez de montar um mapa só
        // com o e-mail.
        Map<String, Object> atributos = new HashMap<>(user.getAttributes());
        atributos.remove(VERIFIED_EMAIL_ATTRIBUTE);
        if (email != null) {
            atributos.put(VERIFIED_EMAIL_ATTRIBUTE, email);
        }

        String nameAttribute = request.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        return new DefaultOAuth2User(user.getAuthorities(), atributos, nameAttribute);
    }

    /**
     * O endereço que o GitHub considera principal, preferindo verificado.
     * <p>
     * A ordenação existe porque o {@code /user/emails} devolve a lista inteira da conta:
     * pegar o primeiro item seria pegar um endereço qualquer, possivelmente não
     * verificado — e e-mail não verificado como chave de conta única é convite a
     * sequestro de conta alheia.
     */
    private String buscarEmailPrincipal(String accessToken) {
        try {
            List<Map<String, Object>> emails = restClient.get()
                    .uri(EMAILS_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (emails == null || emails.isEmpty()) return null;

            return emails.stream()
                    .filter(e -> Boolean.TRUE.equals(e.get("verified")))
                    .filter(e -> e.get(EMAIL_ATTRIBUTE) instanceof String address && !address.isBlank())
                    .max(Comparator.comparing(e -> Boolean.TRUE.equals(e.get("primary"))))
                    .map(e -> (String) e.get(EMAIL_ATTRIBUTE))
                    .orElse(null);
        } catch (RuntimeException e) {
            // Sem prova, o serviço só pode usar um vínculo já existente pelo ID.
            log.warn("Não foi possível consultar e-mails verificados do GitHub");
            return null;
        }
    }
}
