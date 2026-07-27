package com.devlil0.tramposeditor.service;

import com.devlil0.tramposeditor.database.model.UserEntity;
import com.devlil0.tramposeditor.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class JwtService {

    private static final String ISSUER = "tramposeditor";

    private final JwtEncoder jwtEncoder;
    private final Duration ttl;

    public JwtService(JwtEncoder jwtEncoder, @Value("${tramposeditor.security.jwt.ttl}") Duration ttl){
        this.jwtEncoder = jwtEncoder;
        this.ttl = ttl;
    }

    public String generateToken(UserEntity user){
        Instant now = Instant.now();

        List<String> roles = user.getRoles().stream()
                .map(UserRole::name)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName())
                .claim("roles", roles)
                .build();

        // Sem o header explícito o NimbusJwtEncoder assume RS256 e não acha a chave HMAC.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getTtlSeconds(){
        return ttl.toSeconds();
    }
}
