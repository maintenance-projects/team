package com.spacenx.user.controller;

import com.spacenx.user.entity.User;
import com.spacenx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/settings")
    public String settingsPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        return "user/settings";
    }

    @PostMapping("/settings/profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam String displayName,
                                @RequestParam String email,
                                @RequestParam(required = false) String bio,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            user.setDisplayName(displayName);
            user.setEmail(email);
            user.setBio(bio);
            userService.save(user);
            redirectAttributes.addFlashAttribute("success", "프로필이 업데이트되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/settings/password")
    public String updatePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "현재 비밀번호가 올바르지 않습니다.");
                return "redirect:/settings";
            }
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "새 비밀번호가 일치하지 않습니다.");
                return "redirect:/settings";
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            userService.save(user);
            redirectAttributes.addFlashAttribute("success", "비밀번호가 변경되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }
}
