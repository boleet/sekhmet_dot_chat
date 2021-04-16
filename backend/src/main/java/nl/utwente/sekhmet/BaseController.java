package nl.utwente.sekhmet;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BaseController {
    @GetMapping("/login")
    private String loginPage() {
        return "login";
    }

//    @GetMapping("/api/**")
//    private String api404() {
//        return "404";
//    }

//    @GetMapping("/{path:^(?!rest/).*}")
//    private String frontendPage() {
//        return "index";
//    }
}
