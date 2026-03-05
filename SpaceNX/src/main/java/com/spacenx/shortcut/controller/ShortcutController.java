package com.spacenx.shortcut.controller;

import com.spacenx.shortcut.dto.CreateShortcutRequest;
import com.spacenx.shortcut.entity.Shortcut;
import com.spacenx.shortcut.service.ShortcutService;
import com.spacenx.space.entity.Space;
import com.spacenx.space.service.SpaceService;
import com.spacenx.user.entity.User;
import com.spacenx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/spaces/{spaceKey}/shortcuts")
public class ShortcutController {

    private final ShortcutService shortcutService;
    private final SpaceService spaceService;
    private final UserService userService;

    @GetMapping
    public String listShortcuts(@PathVariable String spaceKey,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Space space = spaceService.getSpaceByKey(spaceKey);
        List<Shortcut> shortcuts = shortcutService.getShortcuts(space.getId());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentSpace", space);
        model.addAttribute("shortcuts", shortcuts);
        model.addAttribute("createShortcutRequest", new CreateShortcutRequest());
        model.addAttribute("activeTab", "shortcuts");
        return "shortcut/index";
    }

    @PostMapping
    public String createShortcut(@PathVariable String spaceKey,
                                 @ModelAttribute CreateShortcutRequest request,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            Space space = spaceService.getSpaceByKey(spaceKey);
            shortcutService.createShortcut(
                    space.getId(),
                    request.getName(),
                    request.getUrl(),
                    request.getDescription()
            );
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/spaces/" + spaceKey + "/shortcuts";
    }

    @PostMapping("/{id}/delete")
    public String deleteShortcut(@PathVariable String spaceKey,
                                 @PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            shortcutService.deleteShortcut(id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/spaces/" + spaceKey + "/shortcuts";
    }
}
