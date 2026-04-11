package edu.esi.ds.esiusuarios.dao;

import org.springframework.stereotype.Repository;
import edu.esi.ds.esiusuarios.model.User;
import java.util.ArrayList;
import java.util.List;

@Repository
public class UserDao {

    private List<User> users = new ArrayList<>(List.of(
            new User("Pepe", "pepe123", "PEPE_TOKEN"),
            new User("Ana", "ana123", "ANA_TOKEN")
    ));

    public void save(User user) {
        this.users.add(user);
    }

    public List<User> findAll() {
        return this.users;
    }
}
