package com.ledgerbank.notifications;

import com.ledgerbank.shared.Money;
import com.ledgerbank.shared.events.TransferHeldEvent;
import java.util.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends customer notifications. Mail failures are logged, not thrown, so a missing
 * or flaky mail server never blocks event processing.
 */
@Service
public class NotificationService {

	private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
	private static final String FROM = "no-reply@ledger-bank.local";

	private final JavaMailSender mailSender;

	public NotificationService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void notifyTransferHeld(TransferHeldEvent event) {
		Money amount = Money.of(event.amount(), Currency.getInstance(event.currency()));
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(FROM);
		message.setTo(event.ownerId() + "@ledger-bank.local");
		message.setSubject("Your transfer is being reviewed");
		message.setText("""
				A transfer of %s %s was flagged for review (%s).
				We will let you know once it has been cleared or declined.
				"""
				.formatted(amount.toBigDecimal().toPlainString(), event.currency(), event.reason()));
		send(message);
	}

	private void send(SimpleMailMessage message) {
		try {
			mailSender.send(message);
			log.info("Sent notification to {}", message.getTo());
		}
		catch (MailException e) {
			log.warn("Failed to send notification email: {}", e.getMessage());
		}
	}
}
