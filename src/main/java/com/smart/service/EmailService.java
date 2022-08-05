package com.smart.service;

import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Service
public class EmailService {

	public boolean sendEmail(String subject, String message, String to) {

		boolean f = false;

		String from = "vivekkishore853@gmail.com";

		// VARIABLE FOR GMAIL
		String host = "smtp.gmail.com";

		// GET THE SYSTEM PROPERTIES
		Properties properties = System.getProperties();
		System.out.println("PROPERTIES: " + properties);

		// SETTING IMPORTANT INFORMATION TO PROPERTY OBJECT
		properties.put("mail.smtp.host", host);
		properties.put("mail.smtp.port", "465");
		properties.put("mail.smtp.ssl.enable", "true");
		properties.put("mail.smtp.auth", "true");

		// STEP 1: TO GET THE SESSION OBJECT

		Session session = Session.getInstance(properties, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("vivekkishore853@gmail.com", "wefxnkyemmlnhpdh");
			}

		});

		session.setDebug(true);

		// STEP 2: COMPOSE THE MESSAGE [TEXT,MULTIMEDIA]

		MimeMessage m = new MimeMessage(session);

		try {

			// FROM EMAIL
			m.setFrom(from);

			// ADDING RECIPIENT TO MESSAGE
			m.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

			// ADDING SUBJECT TO MESSAGE
			m.setSubject(subject);

			// ADDING TEXT TO MESSAGE
			m.setText(message);
			m.setContent(message,"text/html");
			

			// STEP 3: SEND THE MESSAGE USING TRANSPORT CLASS
			Transport.send(m);

			System.out.println("Sent success.........");

			f = true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return f;

	}

}
