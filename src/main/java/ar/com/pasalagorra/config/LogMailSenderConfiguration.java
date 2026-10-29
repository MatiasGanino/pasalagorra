package ar.com.pasalagorra.config;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import tech.jhipster.config.JHipsterConstants;

/**
 * En dev no hay servidor de mail: los mails se escriben en el log, con el link de activación o de recuperación, en
 * lugar de enviarse.
 */
@Configuration
@Profile(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)
public class LogMailSenderConfiguration {

    @Bean
    public JavaMailSender javaMailSender() {
        return new LogMailSender();
    }

    static class LogMailSender extends JavaMailSenderImpl {

        private static final Logger LOG = LoggerFactory.getLogger(LogMailSender.class);

        @Override
        protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) {
            for (MimeMessage mensaje : mimeMessages) {
                try {
                    LOG.info(
                        "Mail (no enviado, perfil dev) para {} · {}\n{}",
                        Arrays.toString(mensaje.getAllRecipients()),
                        mensaje.getSubject(),
                        mensaje.getContent()
                    );
                } catch (MessagingException | IOException e) {
                    throw new MailSendException("No se pudo leer el mail", e);
                }
            }
        }
    }
}
