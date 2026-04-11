package edu.esi.ds.esiusuarios.services;

import org.springframework.stereotype.Service;

import edu.esi.ds.esiusuarios.dao.UserDao;
import edu.esi.ds.esiusuarios.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private EmailService emailService;

    public String login(String name, String password) {
        for (User user : this.userDao.findAll()) {
            if (user.getName().equals(name) && user.getPassword().equals(password)) {
                return "Login successful";
            }
        }
        return null;
    }

    public String checkToken(String token) {
        for (User user : this.userDao.findAll()) {
            if (user.getToken() != null && user.getToken().equals(token)) {
                return user.getName();
            }
        }
        return null;
    }

    public void register(String name, String password) {
        // Validate
        if (name == null || password == null || name.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Username and password must not be empty");
        }
        
        // Check duplicate
        for (User u : this.userDao.findAll()) {
            if (u.getName().equals(name)) {
                throw new IllegalArgumentException("User already exists");
            }
        }

        // Save
        String token = UUID.randomUUID().toString();
        User newUser = new User(name, password, token);
        this.userDao.save(newUser);

        // Notify
        this.emailService.sendWelcomeEmail(name);
    }
}