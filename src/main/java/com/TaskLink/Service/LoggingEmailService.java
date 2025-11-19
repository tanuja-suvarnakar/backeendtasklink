package com.TaskLink.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoggingEmailService implements EmailService {
	@Override
	public void send(String to, String subject, String htmlBody) {
		log.info("DEV MAIL -> to: {} | subject: {}\n{}", to, subject, htmlBody);
	}
}