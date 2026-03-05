package com.spacenx.form.controller;

import com.spacenx.form.dto.CreateFormTemplateRequest;
import com.spacenx.form.entity.FormTemplate;
import com.spacenx.form.service.FormService;
import com.spacenx.space.entity.Space;
import com.spacenx.space.service.SpaceService;
import com.spacenx.user.entity.User;
import com.spacenx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/spaces/{spaceKey}/forms")
public class FormController {

    private final FormService formService;
    private final SpaceService spaceService;
    private final UserService userService;

    @GetMapping
    public String listForms(@PathVariable String spaceKey,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Space space = spaceService.getSpaceByKey(spaceKey);
        List<FormTemplate> forms = formService.getFormTemplates(space.getId());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentSpace", space);
        model.addAttribute("forms", forms);
        model.addAttribute("activeTab", "forms");
        return "form/list";
    }

    @GetMapping("/new")
    public String createForm(@PathVariable String spaceKey,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Space space = spaceService.getSpaceByKey(spaceKey);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentSpace", space);
        model.addAttribute("createFormTemplateRequest", new CreateFormTemplateRequest());
        model.addAttribute("activeTab", "forms");
        return "form/create";
    }

    @PostMapping
    public String createFormTemplate(@PathVariable String spaceKey,
                                     @ModelAttribute CreateFormTemplateRequest request,
                                     @RequestParam("file") MultipartFile file,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            Space space = spaceService.getSpaceByKey(spaceKey);
            formService.createFormTemplate(
                    space.getId(),
                    request.getName(),
                    request.getDescription(),
                    file,
                    currentUser
            );
            redirectAttributes.addFlashAttribute("success", "양식 템플릿이 업로드되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/spaces/" + spaceKey + "/forms";
    }

    @GetMapping("/{formId}")
    public String showForm(@PathVariable String spaceKey,
                           @PathVariable Long formId,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Space space = spaceService.getSpaceByKey(spaceKey);
        FormTemplate formTemplate = formService.getFormTemplate(formId);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentSpace", space);
        model.addAttribute("formTemplate", formTemplate);
        model.addAttribute("activeTab", "forms");
        return "form/detail";
    }

    @GetMapping("/{formId}/download")
    public ResponseEntity<Resource> downloadForm(@PathVariable String spaceKey,
                                                  @PathVariable Long formId) {
        FormTemplate template = formService.getFormTemplate(formId);
        Resource resource = formService.getFormTemplateFile(template);

        String encodedFilename = UriUtils.encode(template.getOriginalFilename(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(template.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @PostMapping("/{formId}/delete")
    public String deleteForm(@PathVariable String spaceKey,
                             @PathVariable Long formId,
                             RedirectAttributes redirectAttributes) {
        try {
            formService.deleteFormTemplate(formId);
            redirectAttributes.addFlashAttribute("success", "양식 템플릿이 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/spaces/" + spaceKey + "/forms";
    }
}
