package pk.bm.pasir_malina_bartlomiej.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
    
@RestController
@CrossOrigin(origins = "http://localhost:5174", allowCredentials = "true")
public class TestController {

    @GetMapping("api/test")
    public String test() {
        return "Hello, World!";
    }

    @GetMapping("/api/info")
    public Map<String, String> getAppInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("appName", "Aplikacja Budżetowa");
        info.put("version", "1.0");
        info.put("message", "Witaj w aplikacji budżetowej stworzonej ze Spring Boot!");
        return info;
    }
}

