package com.Rishabh.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.Rishabh.dao.ContactRepository;
import com.Rishabh.dao.UserRepository;
import com.Rishabh.entities.Contact;
import com.Rishabh.entities.User;
import com.Rishabh.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	//method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("USERNAME " + userName);
		
		// get the user using username(Email)
		User user = userRepository.getUserByUserName(userName);
		System.out.println("USER " + user);
		
		model.addAttribute("user", user);
	}

	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("USERNAME " + userName);
		
		//get the user using username(Email)
		User user = userRepository.getUserByUserName(userName);
		System.out.println("USER " + user);
		model.addAttribute("user", user);
		model.addAttribute("title", "Smart Contact Manager");
		
		return "normal/user_dashboard";
	}
	
	//open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Smart Contact Manager");
		model.addAttribute("contact", new Contact());
		
		return "normal/add_contact_form";
	}
	
	//processing add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, 
			@RequestParam("profileImage") MultipartFile file, 
			Principal principal,
			HttpSession session) {
		
		try {
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			
			
			//processing and uploading file..
			if(file.isEmpty()) {
				//do nothing
				System.out.println("File is empty");
				contact.setImage("contact.png");
			}
			else {
				//Update the image name in contact table and upload the file to folder
				contact.setImage(file.getOriginalFilename());
				
				//save file object for this path
				File saveFile = new ClassPathResource("/static/img").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);	
			}
			
			contact.setUser(user);
			
			user.getContacts().add(contact);
			
			this.userRepository.save(user);
			
			System.out.println("Added to database");
			System.out.println("DATA " + contact);
			
			//message success.....
			session.setAttribute("message", new Message("Your contact is added!! Add new", "success"));
			
		}
		catch(Exception e) {
			System.out.println("ERROR" + e.getMessage());
			e.printStackTrace();
			//message error
			session.setAttribute("message", new Message("Something went wrong!! try again...", "danger"));
			
		}
		
		return "normal/add_contact_form";
	}
	
	
	//show contacts handler
	//per page - 5 contacts
	//current page = 0[page]
	@GetMapping("/show-contacts/{page}")
	public String showContents(@PathVariable("page") Integer page, Model m, Principal principal) {
		m.addAttribute("title", "Show User Contacts");
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		//currentPage - page
		//Contact per page - 5
		Pageable pageable = PageRequest.of(page, 5);
		
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);
		
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	//showing particular contact details
	@GetMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		System.out.println("CID " + cId);
		
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		System.out.println(user.getId());
		System.out.println(contact.getUser().getId());
		
		if(user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
		}
		
		return "normal/contact_detail";
	}
	
	//delete contact handler
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, Model model, HttpSession session, Principal principal) {
		
		Optional<Contact> contactOptional =  Optional.of(this.contactRepository.findById(cId).get());
		Contact contact = contactOptional.get();
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		System.out.println(user.getId());
		System.out.println(contact.getUser().getId());
		
		user.getContacts().remove(contact);
		
		this.userRepository.save(user);
		
		//contact.setUser(null);
		
		//remove image from folders as well
		//this.contactRepository.delete(contact);
		
		/*
		 * if(user.getId() == contact.getUser().getId()) {
		 * this.contactRepository.delete(contact); }
		 */
		
		session.setAttribute("message", new Message("Contact deleted successfully...", "success"));
		
		return "redirect:/user/show-contacts/0";
	}
	
	//open update form handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model m) {
		m.addAttribute("title", "Update Contact");
		
		Contact contact = this.contactRepository.findById(cid).get();
		
		m.addAttribute("contact", contact);
		
		return "normal/update_form";
	}
	
	//update contact handler
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute Contact contact, Principal principal, @RequestParam("profileImage") MultipartFile file, Model m, HttpSession session) {
		
		try {
			
			//old contact details
			
			Contact oldContactDetails = this.contactRepository.findById(contact.getcId()).get();
			
			//image
			//if file is not empty
			//first, we need to delete already uploaded image
			//then, update with the new one
			if(!file.isEmpty()) {
				//file work...
				//rewrite
				
				//delete old photo
				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile, oldContactDetails.getImage());
				file1.delete();
				
				//update new photo
				File saveFile = new ClassPathResource("static/img").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				contact.setImage(file.getOriginalFilename());
				
			}
			else {
				//if no photo is selected while updating, use the last one
				contact.setImage(oldContactDetails.getImage());
			}
			
			
			//saving contact's updated information in db table
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Your contact is updated...", "success"));
			
		}
		catch(Exception e) {
			e.getMessage();
		}
		
		System.out.println("Contact name" + contact.getName());
		System.out.println("Contact ID " + contact.getcId());
		
		
		return "redirect:/user/" + contact.getcId() + "/contact";
	}
	
	//your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title", "Profile Page");
		return "normal/profile";
	}
	
	
	
	
	
	
	
	
}
















