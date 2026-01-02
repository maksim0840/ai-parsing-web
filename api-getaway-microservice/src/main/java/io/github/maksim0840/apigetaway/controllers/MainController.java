package io.github.maksim0840.apigetaway.controllers;

import io.github.maksim0840.apigetaway.dto.DescriptionParams;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/parse")
public class MainController {

    @GetMapping({"", "/"})
    public String parseHome(Model model) {
        model.addAttribute("result", "");
        model.addAttribute("status", "");
        return "parse";
    }

    @GetMapping("/result")
    public String showParsingResult(Model model) {
        model.addAttribute("result", "this is a parsing result!!!");
        return "parse";
    }

    @GetMapping("/status")
    public String getParsingStatus(Model model) {
        model.addAttribute("status", "1/10");
        return "parse";
    }

    /*
    Метод для получения параметров парсинга после потверждения формы отправки
     */
    @PostMapping("/startparsing")
    public String sendParsingParams(@ModelAttribute(value = "params") DescriptionParams descriptionParams) {
        String url = descriptionParams.getUrl();
        System.out.println("url: " + url);
        return "parse";
    }

    /*
    Метод для начального заполнения параметра params внутри thymelief
     */
    @ModelAttribute(value = "params")
    public DescriptionParams params() {
        return new DescriptionParams();
    }
}
