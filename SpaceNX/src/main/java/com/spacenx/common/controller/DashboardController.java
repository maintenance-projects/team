package com.spacenx.common.controller;

import com.spacenx.space.entity.Space;
import com.spacenx.space.service.SpaceService;
import com.spacenx.user.entity.User;
import com.spacenx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final SpaceService spaceService;
    private final UserService userService;

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        List<Space> spaces = spaceService.getSpacesForUser(currentUser);
        model.addAttribute("spaces", spaces);
        model.addAttribute("currentUser", currentUser);
        return "dashboard/index";
    }
}
