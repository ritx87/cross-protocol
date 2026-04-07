package com.example.client1saml;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class Client1Controller {
    @GetMapping("/")
    public String index(@AuthenticationPrincipal Saml2AuthenticatedPrincipal principal, Model model) {
        if (principal != null) {
            model.addAttribute("userName", principal.getName());
            model.addAttribute("attributes", principal.getAttributes());
        }
        return "index";
    }
}
