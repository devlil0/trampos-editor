package com.devlil0.tramposeditor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** HS256 exige no mínimo 256 bits de chave. */
    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter,
                                                   ObjectProvider<ClientRegistrationRepository> clientRegistrations,
                                                   ObjectProvider<OAuth2SuccessHandler> oauth2SuccessHandler,
                                                   GitHubEmailOAuth2UserService gitHubEmailUserService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                // O handshake do OAuth precisa guardar o `state` entre o redirecionamento
                // de ida e a volta, então a política não pode ser STATELESS. O resto da
                // API continua sem sessão: nenhuma requisição autenticada por JWT cria uma.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/auth/**").permitAll()
                        // O vaivém com o provedor acontece deslogado, por definição.
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // Vitrine pública: só leitura. A gestão do perfil mora em /v1/me/** e segue autenticada.
                        .requestMatchers(HttpMethod.GET, "/v1/editors", "/v1/editors/**").permitAll()
                        // Perfil por username: mesma natureza da vitrine — é ativo de
                        // aquisição e precisa que o Google entre.
                        .requestMatchers(HttpMethod.GET, "/v1/profiles/**").permitAll()
                        // Foto de perfil: aparece na vitrine e no perfil indexado. Exigir
                        // token pra carregar imagem de página pública quebraria a página.
                        .requestMatchers(HttpMethod.GET, "/v1/avatars/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        // Login social só entra na cadeia quando há provedor configurado. Sem isto, subir
        // sem GOOGLE_CLIENT_ID quebraria a aplicação inteira por falta de
        // ClientRegistrationRepository — e rodar local não pode exigir credencial de
        // ninguém, mesma regra do SMTP.
        ClientRegistrationRepository registrations = clientRegistrations.getIfAvailable();
        if (registrations != null) {
            http.oauth2Login(login -> login
                    // O serviço customizado existe só pelo GitHub, que esconde e-mail
                    // privado do /user — ver GitHubEmailOAuth2UserService.
                    .userInfoEndpoint(userInfo -> userInfo.userService(gitHubEmailUserService))
                    .successHandler(oauth2SuccessHandler.getObject()));
        }

        return http.build();
    }

    @Bean
    public SecretKey jwtSecretKey(@Value("${tramposeditor.security.jwt.secret}") String secret){
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "tramposeditor.security.jwt.secret precisa de no mínimo " + MIN_SECRET_BYTES
                            + " caracteres (recebeu " + keyBytes.length + "). Defina a variável de ambiente JWT_SECRET.");
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey){
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey){
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(){
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        converter.setPrincipalClaimName(JwtClaimNames.SUB);

        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${tramposeditor.cors.allowed-origins}") List<String> allowedOrigins){
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
