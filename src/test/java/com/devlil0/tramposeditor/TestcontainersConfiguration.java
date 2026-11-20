package com.devlil0.tramposeditor;

import com.devlil0.tramposeditor.service.SignupCodes;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Postgres real nos testes — o mesmo da produção.
 * Com ddl-auto=validate, qualquer divergência entre as migrations e as entidades
 * quebra o contexto aqui em vez de quebrar no deploy.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {
    @Bean
    DynamicPropertyRegistrar limitesDosTestesDeNegocio(Environment environment) {
        return registry -> {
            // Os testes de negócio compartilham contexto, IP e endereços fictícios.
            // A suíte da T05 usa as cotas reais e controla o próprio relógio.
            if (!environment.getProperty("test.auth-limits.enabled", Boolean.class, false)) {
                for (String operation : new String[]{"login", "signup", "password-reset"}) {
                    String prefix = "tramposeditor.auth-limits." + operation;
                    registry.add(prefix + ".per-account", () -> 1000);
                    registry.add(prefix + ".per-origin", () -> 1000);
                    registry.add(prefix + ".cooldown", () -> "0s");
                }
                registry.add("tramposeditor.auth-limits.signup-confirm.per-origin", () -> 1000);
            }
        };
    }


    /**
     * O código que o cadastro em dois passos (V18) espera em teste.
     * <p>
     * Cadastrar passou a exigir devolver um código que só existe no e-mail, e o banco guarda
     * só o hash — sem fixar isto, nenhuma suíte conseguiria criar um usuário, e as dez que
     * dependem de ter conta parariam de rodar.
     */
    public static final String CODIGO_DE_CADASTRO = "000000";

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer(){
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
    }

    /**
     * Substitui o gerador aleatório. Mora aqui, e não em cada suíte, porque toda suíte que
     * cria conta precisa — e porque a alternativa seria repetir a mesma substituição em dez
     * arquivos e esquecer no décimo primeiro.
     */
    @Bean
    @Primary
    public SignupCodes signupCodesFixos(){
        return new SignupCodes() {
            @Override
            public String generate(){
                return CODIGO_DE_CADASTRO;
            }
        };
    }
}
