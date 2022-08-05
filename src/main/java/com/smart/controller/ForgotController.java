package com.smart.controller;

import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.service.EmailService;

@Controller
public class ForgotController {

	Random random = new Random(1000);

	@Autowired
	private EmailService emailService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BCryptPasswordEncoder bcrypt;

	// EMAIL ID FORM OPEN HANDLER

	@RequestMapping("/forgot")
	public String openEmailForm() {

		return "forgot_email_form";
	}

	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email, HttpSession session) {

		System.out.println("EMAIL: " + email);

		// GENERATING OTP OF 4 DIGIT

		int otp = random.nextInt(999999);

		System.out.println("OTP: " + otp);

		// WRITE CODE FOR SENDING OTP TO EMAIL

		String subject = "OTP From Smart Contact Manager";
		String message = "" + "<div style='border: 1px solid #e2e2e2; padding:20px'>" + "<h1>"
				+ "Your OTP for changing password is " + "<b>" + otp + "</b>" + "</h1>" + "</div>";
		String to = email;

		boolean flag = this.emailService.sendEmail(subject, message, to);

		if (flag) {

			session.setAttribute("myotp", otp);
			session.setAttribute("email", email);
			return "verify_otp";
		} else {

			session.setAttribute("message", "Check your Email Id!");

			return "forgot_email_form";
		}

	}

	// VERIFY OTP
	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam("otp") int otp, HttpSession session) {

		int myOtp = (int) session.getAttribute("myotp");
		String email = (String) session.getAttribute("email");

		if (myOtp == otp) {
			// PASSWORD CHANGE FORM

			User user = this.userRepository.getUserByUserName(email);

			if (user == null) {
				// SEND ERROR MESSAGE

				session.setAttribute("message", "User does not exist with this email!");

				return "forgot_email_form";

			} else {
				// SEND PASSWORD CHANGE FORM
			}

			return "password_change_form";

		} else {
			session.setAttribute("message", "You have entered wrong OTP");
			return "verify_otp";
		}

	}

	// CHANGE PASSWORD

	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newpassword") String newpassword, HttpSession session) {

		String email = (String) session.getAttribute("email");

		User user = this.userRepository.getUserByUserName(email);

		user.setPassword(this.bcrypt.encode(newpassword));

		this.userRepository.save(user);

		return "redirect:/signin?change=password changed successfully";

	}

}
