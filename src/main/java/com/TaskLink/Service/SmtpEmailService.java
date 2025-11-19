package com.TaskLink.Service;



import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService implements EmailService {
	private final JavaMailSender mailSender;
	@Value("${app.mail.from:tanusuwarnakar123@gmail.com}")
	private String from;

	@Override
	public void send(String to, String subject, String html) {
		try {
			MimeMessage msg = mailSender.createMimeMessage();
			MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
			h.setFrom(from);
			h.setTo(to);
			h.setSubject(subject);
			h.setText(html, true);
			mailSender.send(msg);
			log.info("Email sent -> {} [{}]", to, subject);
		} catch (Exception e) {
			throw new RuntimeException("Email send failed", e);
		}
	}
}
