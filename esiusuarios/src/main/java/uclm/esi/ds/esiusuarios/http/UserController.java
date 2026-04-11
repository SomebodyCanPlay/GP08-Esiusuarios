package edu.esi.ds.esiusuarios.http;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import org.json.JSONObject;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import edu.esi.ds.esiusuarios.services.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService service;

    @PostMapping("/login")
    public String login(@RequestBody Map<String, String> credentials) {
        JSONObject jsoCredentials = new JSONObject(credentials);
        String name = jsoCredentials.optString("name");
        String password = jsoCredentials.optString("pwd");

        if (name.isEmpty() || password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        String result = this.service.login(name, password);
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return result;
    }

    @PostMapping("/register")
    public String register(@RequestBody Map<String, String> data) {
        JSONObject jso = new JSONObject(data);
        String name = jso.optString("name");
        String password = jso.optString("pwd");

        try {
            this.service.register(name, password);
            return "User registered successfully";
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}