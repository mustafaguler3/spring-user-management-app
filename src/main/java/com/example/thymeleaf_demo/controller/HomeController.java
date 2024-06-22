package com.example.thymeleaf_demo.controller;

import com.example.thymeleaf_demo.domain.PasswordResetToken;
import com.example.thymeleaf_demo.domain.User;
import com.example.thymeleaf_demo.dto.LoginDto;
import com.example.thymeleaf_demo.dto.UserDto;
import com.example.thymeleaf_demo.exception.FileStorageException;
import com.example.thymeleaf_demo.exception.ResourceNotFoundException;
import com.example.thymeleaf_demo.repository.PasswordResetTokenRepository;
import com.example.thymeleaf_demo.repository.UserRepository;
import com.example.thymeleaf_demo.service.EmailService;
import com.example.thymeleaf_demo.service.FileStorageService;

import com.example.thymeleaf_demo.service.UserService;
import groovy.util.logging.Log4j2;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;

@Slf4j
@Controller
public class HomeController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final ModelMapper modelMapper;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    public HomeController(UserService userService, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, FileStorageService fileStorageService, EmailService emailService, ModelMapper modelMapper, PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.fileStorageService = fileStorageService;
        this.emailService = emailService;
        this.modelMapper = modelMapper;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @GetMapping(value = {"/", "/home"})
    public String home(Model model) throws Exception {
        model.addAttribute("currentUser",getCurrentUser().getUsername());
        return "home";
    }

    public UserDto getCurrentUser() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof AnonymousAuthenticationToken)){

            String username = authentication.getName();
            UserDto currentUser = userService.findByUsername(username);

            if (currentUser == null){
                throw new ResourceNotFoundException("User not found");
            }

            //model.addAttribute("currentUser",currentUser);
            return currentUser;
        }
        return null;
    }

    @GetMapping("/logout-success")
    public String logoutSuccess(Model model){
        return "logout-success";
    }


    @GetMapping("/users")
    public String userList(Model model,
                           @RequestParam(defaultValue = "0",name = "page") int pageNumber,
                           @RequestParam(defaultValue = "4",name = "size") int pageSize,
                           @RequestParam(value = "keyword",required = false) String keyword
                           ){

        if(keyword != null && !keyword.isEmpty()){
            List<User> searchUser = userService.searchByUsername(keyword);
            model.addAttribute("searchUser",searchUser);

        }
            Page<User> users = userService.findAll(PageRequest.of(pageNumber, pageSize));
            model.addAttribute("userPage", users);

            if(users.isEmpty()){
                throw new ResourceNotFoundException("No users found");
            }
            model.addAttribute("keyword",keyword);

        return "user-list";
    }

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        Resource file = fileStorageService.loadFileAsResource(filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }


    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Integer id){
        Optional<User> user = userService.findById(id);
        if (!user.isPresent()){
            throw new ResourceNotFoundException("User not found");
        }
        userService.deleteUser(user.get());
        return "redirect:/users";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Integer id, Model model){
        Optional<User> user = userService.findById(id);
        model.addAttribute("user",user.get());
        return "edit-form";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable("id") Integer id,
                           @ModelAttribute("user") User user,
                           BindingResult bindingResult,
                           Model model,
                           @RequestParam("profilePicture") MultipartFile profilePicture) throws Exception {

        Optional<User> findedUser = userService.findById(id);

        if (!findedUser.isPresent()){
            throw new ResourceNotFoundException("User not found");
        }

        if (!profilePicture.isEmpty()){
            try {
                String fileName = fileStorageService.storeFile(profilePicture);
                Path filePath = fileStorageService.loadFile(fileName);

                if(!filePath.toString().equals(findedUser.get().getProfilePicture())){
                    fileStorageService.deleteFile(findedUser.get().getProfilePicture());
                }
                findedUser.get().setProfilePicture(fileName);
                model.addAttribute("success","Photo updated successfuly");
            }catch (FileStorageException e){
                e.printStackTrace();
                bindingResult.rejectValue("profilePicture",e.getMessage(),"Failed to upload profile picture");
            }
        }

        findedUser.get().setUsername(user.getUsername());
        findedUser.get().setEmail(user.getEmail());
        findedUser.get().setPassword(user.getPassword());

        UserDto userDto = modelMapper.map(findedUser,UserDto.class);

        userService.saveUser(userDto);

        return "redirect:/edit/"+user.getId();
    }





}





















