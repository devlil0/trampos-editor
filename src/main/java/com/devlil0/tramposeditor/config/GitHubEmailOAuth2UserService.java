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
 * Preenche o e-mail que o GitHub esconde.
 * <p>
 * O {@code /user} do GitHub só traz {@code email} quando a pessoa marcou o endereço como
 * público no perfil — o padrão é privado, e aí vem {@code null}. O escopo
 * {@code user:email} não muda isso: ele libera um segundo endpoint, {@code /user/emails},
 * que o Spring não chama sozinho porque a especificação OAuth2 prevê um user-info só.
 * <p>
 * Sem esse complemento, quem tem e-mail privado chega aqui sem e-mail e o login morre com
 * "seu github não expôs um e-mail" — e a conta única do {@code FUNDACAO} §11.9, que casa
 * as identidades justamente pelo e-mail, não teria por onde casar.
 * <p>
 * Google não passa por aqui: o OIDC já entrega o e-mail no id_token.
 */
@Slf4j
@Component
public class GitHubEmailOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String GITHUB = "github";
    private static final String EMAILS_URI = "https://api.github.com/user/emails";
    private static final String EMAIL_ATTRIBUTE = "email";

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final RestClient restClient = RestClient.create();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User user = delegate.loadUser(request);

        boolean isGitHub = GITHUB.equals(request.getClientRegistration().getRegistrationId());
        if (!isGitHub || user.getAttribute(EMAIL_ATTRIBUTE) != null) {
            return user;
        }

        String email = buscarEmailPrincipal(request.getAccessToken().getTokenValue());
        if (email == null) {
            // Devolve como veio: quem decide a mensagem de erro é o SocialLoginService,
            // que já sabe dizer o que a pessoa precisa fazer.
            return user;
        }

        // DefaultOAuth2User exige que o mapa contenha a chave usada como nome do
        // principal — por isso copiamos os atributos originais em vez de montar um mapa só
        // com o e-mail.
        Map<String, Object> atributos = new HashMap<>(user.getAttributes());
        atributos.put(EMAIL_ATTRIBUTE, email);

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
                    .max(Comparator.comparing(e -> Boolean.TRUE.equals(e.get("primary"))))
                    .map(e -> (String) e.get(EMAIL_ATTRIBUTE))
                    .orElse(null);
        } catch (RuntimeException e) {
            // Falha aqui não pode derrubar o login: cai no fluxo de "não expôs e-mail",
            // que já tem mensagem pra pessoa.
            log.warn("Não consegui ler /user/emails do GitHub: {}", e.getMessage());
            return null;
        }
    }
}
