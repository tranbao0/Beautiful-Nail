package com.beautifulnail.scheduler.controller;

import com.beautifulnail.scheduler.model.User;
import com.beautifulnail.scheduler.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("userId") != null) return "redirect:/";
        return "auth/login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam String email,
                              @RequestParam String password,
                              HttpSession session,
                              Model model) {
        Optional<User> found = userService.authenticate(email, password);
        if (found.isEmpty()) {
            model.addAttribute("error", "Invalid email or password.");
            model.addAttribute("email", email);
            return "auth/login";
        }
        User user = found.get();
        applySession(session, user);
        return "receptionist".equals(user.getRole()) ? "redirect:/receptionist" : "redirect:/appointments";
    }

    // ── Register ─────────────────────────────────────────────────────────────

    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (session.getAttribute("userId") != null) return "redirect:/";
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerSubmit(@RequestParam String firstName,
                                 @RequestParam(required = false) String mInit,
                                 @RequestParam String lastName,
                                 @RequestParam String email,
                                 @RequestParam(required = false) String phone,
                                 @RequestParam String password,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("email", email);
            return "auth/register";
        }
        if (userService.emailExists(email)) {
            model.addAttribute("error", "An account with that email already exists.");
            model.addAttribute("firstName", firstName);
            model.addAttribute("lastName", lastName);
            model.addAttribute("email", email);
            return "auth/register";
        }
        User user = userService.register(firstName, mInit, lastName, email, phone, password);
        applySession(session, user);
        return "redirect:/appointments";
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void applySession(HttpSession session, User user) {
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userRole", user.getRole());
        session.setAttribute("userName", user.getFirstName() + " " + user.getLastName());
        session.setAttribute("userEmail", user.getEmail());
        session.setAttribute("userPhone", user.getPhone());
    }
}
