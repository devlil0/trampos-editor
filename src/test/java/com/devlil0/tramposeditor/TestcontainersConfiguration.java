package com.devlil0.tramposeditor;

import com.devlil0.tramposeditor.service.SignupCodes;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Postgres real nos testes — o mesmo da produção.
 * Com ddl-auto=validate, qualquer divergência entre as migrations e as entidades
 * quebra o contexto aqui em vez de quebrar no deploy.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

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
