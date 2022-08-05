package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;

	// METHOD FOR ADDING COMMON DATA TO RESPONSE

	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {

		String userName = principal.getName();
		System.out.println("Username" + userName);

		// GET THE USER USING USERNAME(EMAIL)

		User user = userRepository.getUserByUserName(userName);

		System.out.println("User" + user);

		model.addAttribute("user", user);

	}

	// DASHBOARD HOME

	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {

		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}

	// OPEN ADD FORM HANDLER

	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {

		model.addAttribute("title", "Add Contact");

		model.addAttribute("contact", new Contact());

		return "normal/add_contact_form";
	}

	// PROCESSING ADD CONTACT FORM

	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {

		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);

			// PROCESSING AND UPLOADING FILE

			if (file.isEmpty()) {
				// IF THE FILE IS EMPTY THEN TRY OUR MESSAGE

				contact.setImage("contact.png");

			} else {
				// UPLOAD THE FILE TO FOLDER AND UPDATE THE NAME TO CONTACT

				contact.setImage(file.getOriginalFilename());

				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			}

			user.getContacts().add(contact);

			contact.setUser(user);

			this.userRepository.save(user); // Saving contact details to database

			System.out.println("DATA" + contact);

			System.out.println("Added to database");

			// SUCCESS MESSAGE

			session.setAttribute("message", new Message("Your contact is added!!", "success"));

		} catch (Exception e) {
			System.out.println("ERROR" + e.getMessage());
			e.printStackTrace();

			// ERROR MESSAGE

			session.setAttribute("message", new Message("Something went wrong!!", "danger"));

		}

		return "normal/add_contact_form";

	}

	// SHOW CONTACTS HANDLER
	// PER PAGE = 5[n]
	// CURRENT PAGE = 0 [PAGE]
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m, Principal principal) {

		m.addAttribute("title", "Show User Contacts");

		String userName = principal.getName();

		User user = this.userRepository.getUserByUserName(userName);

		Pageable pageable = PageRequest.of(page, 5);

		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);

		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show_contacts";
	}

	// SHOWING PARTICULAR CONTACT DETAILS

	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {

		System.out.println("cId " + cId);

		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		// CHECKS SO THAT THE USER CAN ACCESS THEIR CONTACT ONLY

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}

		return "normal/contact_detail";
	}

	// DELETE CONTACT HANDLER

	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cId, Model model, HttpSession session,
			Principal principal) {

		Contact contact = this.contactRepository.findById(cId).get();

//		contact.setUser(null);

//		this.contactRepository.delete(contact);

		User user = this.userRepository.getUserByUserName(principal.getName());

		user.getContacts().remove(contact);

		this.userRepository.save(user);

		session.setAttribute("message", new Message("Contact deleted successfully...", "success"));

		return "redirect:/user/show-contacts/0";
	}

	// OPEN UPDATE FORM HANDLER
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model m) {

		m.addAttribute("title", "Update Contact");

		Contact contact = this.contactRepository.findById(cid).get();

		m.addAttribute("contact", contact);

		return "normal/update_form";
	}

	// UPDATE CONTACT HANDLER
	@RequestMapping(value = "/process-update", method = RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Model m, HttpSession session, Principal principal) {

		try {

			// OLD CONTACT DETAIL

			Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();

			if (!file.isEmpty()) {

				// DELETE OLD PHOTO

				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile, oldContactDetail.getImage());
				file1.delete();

				// UPDATE NEW PHOTO

				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				contact.setImage(file.getOriginalFilename());

			} else {
				contact.setImage(oldContactDetail.getImage());
			}

			User user = this.userRepository.getUserByUserName(principal.getName());

			contact.setUser(user);

			this.contactRepository.save(contact);

			session.setAttribute("message", new Message("Your contact is updated...", "success"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("CONTACT NAME: " + contact.getName());
		System.out.println("CONTACT ID: " + contact.getcId());

		return "redirect:/user/" + contact.getcId() + "/contact";
	}

	// YOUR PROFILE HANDLER

	@GetMapping("/profile")
	public String yourProfile(Model model) {

		model.addAttribute("title", "Profile Page");

		return "normal/profile";
	}

	// OPEN SETTINGS HANDLER
	@GetMapping("/settings")
	public String openSettings() {
		return "normal/settings";
	}

	// CHANGE PASSWORD HANDLER
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword, Principal principal, HttpSession session) {

		
		System.out.println("OLD PASSWORD: " +oldPassword);
		System.out.println("NEW PASSWORD: " +newPassword);
		
		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);
		System.out.println(currentUser.getPassword());
		
		if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword())) {
			// CHANGE THE PASSWORD
			
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			
			session.setAttribute("message", new Message("Your Password is Successfully Changed!", "success"));

			
		} else {
			// ERROR
			
			session.setAttribute("message", new Message("Please Enter Correct Old Password!", "danger"));
			return "redirect:/user/settings";
			
		}
		
		
		
		return "redirect:/user/index";
	}

}
