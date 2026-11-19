package com.devlil0.tramposeditor.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class EmailTransportTest {
    @Test
    void sucessoSmtpNaoRegistraCodigoOuToken(CapturedOutput output) {
        var mail = spy(new JavaMailSenderImpl());
        doNothing().when(mail).send(any(jakarta.mail.internet.MimeMessage.class));
        var properties = new NotificationProperties("teste@example.com", "https://teste.example.com");
        var service = new NotificationService(new SmtpEmailSender(mail, properties), properties);
        service.signupCode("destino@example.com", "Teste", "938471");
        var user = com.devlil0.tramposeditor.database.model.UserEntity.builder()
                .email("destino@example.com").displayName("Teste").build();
        service.passwordReset(user, "token-ficticio-sigiloso");
        service.verifyEmail(user, "token-ficticio-sigiloso");
        verify(mail, times(3)).send(any(jakarta.mail.internet.MimeMessage.class));
        assertThat(output.getAll()).doesNotContain("938471", "token-ficticio-sigiloso", "destino@example.com");
    }
    private MockEnvironment valid() {
        return new MockEnvironment()
                .withProperty("tramposeditor.notifications.transport", "smtp")
                .withProperty("tramposeditor.notifications.from", "Equipe <teste@example.com>")
                .withProperty("tramposeditor.notifications.base-url", "https://teste.example.com")
                .withProperty("spring.mail.host", "smtp.example.com")
                .withProperty("spring.mail.port", "587")
                .withProperty("spring.mail.username", "usuario-ficticio")
                .withProperty("spring.mail.password", "senha-ficticia");
    }

    @Test
    void producaoNuncaSelecionaAdaptadorDeLog() {
        new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
                .withUserConfiguration(LogEmailSender.class)
                .withPropertyValues("tramposeditor.notifications.transport=log")
                .run(context -> assertThat(context).doesNotHaveBean(EmailSender.class));
    }

    @Test
    void perfilProdMantemTimeoutsNoJavaMailSender() {
        new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
                .withPropertyValues("spring.mail.host=smtp.example.invalid", "spring.mail.test-connection=false")
                .run(context -> {
                    var mail = context.getBean(JavaMailSenderImpl.class);
                    assertThat(mail.getJavaMailProperties())
                            .containsEntry("mail.smtp.connectiontimeout", "5000")
                            .containsEntry("mail.smtp.timeout", "5000")
                            .containsEntry("mail.smtp.writetimeout", "5000")
                            .containsEntry("mail.smtp.starttls.enable", "true");
                });
    }

    @Test
    void configuracaoValidaNaoAbreConexao() {
        assertThatCode(() -> new ProductionEmailConfiguration(valid())).doesNotThrowAnyException();
    }

    @Test
    void configuracaoCriticaInvalidaFalhaSemExibirValores() {
        for (String key : List.of("spring.mail.host", "spring.mail.username", "spring.mail.password",
                "tramposeditor.notifications.from", "tramposeditor.notifications.base-url")) {
            assertThatThrownBy(() -> new ProductionEmailConfiguration(valid().withProperty(key, "")))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("docs/EMAIL.md").hasNoCause();
        }
        for (String url : List.of("http://teste.example.com", "https://localhost",
                "https://teste.example.com/caminho", "https://teste.example.com?token=segredo")) {
            assertThatThrownBy(() -> new ProductionEmailConfiguration(valid()
                    .withProperty("tramposeditor.notifications.base-url", url)))
                    .isInstanceOf(IllegalStateException.class).hasNoCause();
        }
        for (String port : List.of("0", "65536", "invalida")) {
            assertThatThrownBy(() -> new ProductionEmailConfiguration(valid().withProperty("spring.mail.port", port)))
                    .isInstanceOf(IllegalStateException.class);
        }
        assertThatThrownBy(() -> new ProductionEmailConfiguration(valid()
                .withProperty("tramposeditor.notifications.transport", "log")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ProductionEmailConfiguration(valid()
                .withProperty("spring.mail.properties.mail.debug", "true")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ProductionEmailConfiguration(valid()
                .withProperty("tramposeditor.notifications.from", "endereco-invalido")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void falhaSmtpEObrigatoriaNaoVazaConteudo(CapturedOutput output) {
        var mail = spy(new JavaMailSenderImpl());
        doThrow(new MailSendException("token=segredo-do-provedor")).when(mail).send(any(jakarta.mail.internet.MimeMessage.class));
        var properties = new NotificationProperties("teste@example.com", "https://teste.example.com");
        var service = new NotificationService(new SmtpEmailSender(mail, properties), properties);
        assertThatThrownBy(() -> service.signupCode("destino@example.com", "Teste", "938471"))
                .isInstanceOf(RequiredEmailException.class).hasNoCause();
        assertThat(output.getAll()).contains("Falha no envio obrigatório")
                .doesNotContain("938471", "segredo-do-provedor", "destino@example.com");
    }
}