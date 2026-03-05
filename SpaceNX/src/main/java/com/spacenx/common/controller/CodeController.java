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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/spaces/{spaceKey}/code")
public class CodeController {

    private final SpaceService spaceService;
    private final UserService userService;

    @GetMapping
    public String code(@PathVariable String spaceKey,
                       @AuthenticationPrincipal UserDetails userDetails,
                       Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Space space = spaceService.getSpaceByKey(spaceKey);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentSpace", space);
        model.addAttribute("activeTab", "code");
        return "code/index";
    }
}
