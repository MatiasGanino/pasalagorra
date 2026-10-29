package ar.com.pasalagorra.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * RF-01: en dev los mails se escriben en el log y no fallan aunque no haya servidor de mail.
 */
class LogMailSenderConfigurationTest {

    @Test
    void enDevElMailSeEscribeEnElLogSinServidor() throws Exception {
        JavaMailSender sender = new LogMailSenderConfiguration().javaMailSender();
        MimeMessage mail = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mail, false, StandardCharsets.UTF_8.name());
        helper.setTo("fan@example.com");
        helper.setFrom("pasalagorra@localhost");
        helper.setSubject("Activación");
        helper.setText("<a href=\"http://localhost:4200/account/activate?key=123\">activar</a>", true);

        assertThatCode(() -> sender.send(mail)).doesNotThrowAnyException();
    }
}
