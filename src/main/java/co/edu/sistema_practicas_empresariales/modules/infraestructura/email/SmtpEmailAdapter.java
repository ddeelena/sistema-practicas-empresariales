package co.edu.sistema_practicas_empresariales.modules.infraestructura.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import jakarta.mail.internet.MimeMessage;

@Component
public class SmtpEmailAdapter implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailAdapter.class);

    private final JavaMailSender mailSender;

    public SmtpEmailAdapter(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void enviarCorreo(String destinatario, String asunto, String cuerpoHtml) {
        logger.info("Enviando correo a: {}, Asunto: {}", destinatario, asunto);
        if (mailSender == null) {
            logger.warn("JavaMailSender no está configurado. El correo se imprimirá únicamente en los registros.");
            logger.info("Cuerpo del correo:\n{}", cuerpoHtml);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);
            
            mailSender.send(message);
            logger.info("Correo enviado exitosamente a {}", destinatario);
        } catch (Exception e) {
            logger.error("Error al enviar el correo a {}: {}", destinatario, e.getMessage(), e);
        }
    }
}
