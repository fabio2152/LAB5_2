package com.lab5.marketing.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lab5.marketing.service.CampaignService;

@Controller
public class CampaignController {

    private static final Logger log = LoggerFactory.getLogger(CampaignController.class);

    private final CampaignService service;

    public CampaignController(CampaignService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("campaigns", service.recent());
        addStatus(model);
        return "index";
    }

    @PostMapping("/campaigns")
    public String create(@RequestParam String companyName,
                         @RequestParam(required = false) String productDescription,
                         @RequestParam(required = false) String targetAudience,
                         @RequestParam MultipartFile photo,
                         RedirectAttributes redirect) {
        try {
            var campaign = service.create(companyName, productDescription, targetAudience, photo);
            return "redirect:/campaigns/" + campaign.getId();
        } catch (RuntimeException e) {
            log.error("Error creando campaña", e);
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/campaigns/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("campaign", service.get(id));
        addStatus(model);
        return "campaign";
    }

    @PostMapping("/campaigns/{id}/select")
    public String select(@PathVariable long id, @RequestParam long scriptId, RedirectAttributes redirect) {
        try {
            service.selectScript(id, scriptId);
            redirect.addFlashAttribute("success", "Se generaron las 3 fotos y el video.");
        } catch (RuntimeException e) {
            log.error("Error generando contenido", e);
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/campaigns/" + id;
    }

    @PostMapping("/campaigns/{id}/publish")
    public String publish(@PathVariable long id, RedirectAttributes redirect) {
        try {
            service.publish(id);
        } catch (RuntimeException e) {
            log.error("Error publicando", e);
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/campaigns/" + id + "#publicacion";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge(RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", "La foto supera el tamaño máximo de 15 MB.");
        return "redirect:/";
    }

    private void addStatus(Model model) {
        model.addAttribute("demoMode", service.isDemoMode());
        model.addAttribute("instagramConfigured", service.isInstagramConfigured());
        model.addAttribute("s3Enabled", service.isS3Enabled());
    }
}
