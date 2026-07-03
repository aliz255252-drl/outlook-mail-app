package com.example.outlookmail.controller;

import com.example.outlookmail.dto.SendMailRequest;
import com.example.outlookmail.service.GraphService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MailController {

    private final GraphService graphService;

    public MailController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/")
    public String sendForm(Model model) {
        if (!model.containsAttribute("sendMailRequest")) {
            model.addAttribute("sendMailRequest", new SendMailRequest());
        }
        return "send";
    }

    @PostMapping("/send")
    public String send(@ModelAttribute SendMailRequest sendMailRequest,
                       RedirectAttributes redirectAttributes) {
        try {
            graphService.sendMail(sendMailRequest);
            redirectAttributes.addFlashAttribute("success", "Your email was sent successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to send email: " + e.getMessage());
        }
        
        return "redirect:/";
    }
}
