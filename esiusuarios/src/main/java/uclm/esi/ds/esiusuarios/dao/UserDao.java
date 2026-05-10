package uclm.esi.ds.esiusuarios.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import uclm.esi.ds.esiusuarios.model.User;
import java.util.Optional;

// DAO para la entidad User
public interface UserDao extends JpaRepository<User, Long> {

    // Buscar por email
    Optional<User> findByEmail(String email);

    // Buscar por token de sesión
    Optional<User> findByTokenSesion(String tokenSesion);

    // Actualiza el token de sesión
    @Modifying
    @Query("UPDATE User u SET u.tokenSesion = :token WHERE u.email = :email")
    void actualizarTokenSesion(@Param("email") String email, @Param("token") String token);

    // Marca la cuenta como inactiva
    @Modifying
    @Query("UPDATE User u SET u.activo = false WHERE u.email = :email")
    void desactivarCuenta(@Param("email") String email);
}
    // @Modifying → le dice a Spring que esta query modifica datos (UPDATE/DELETE)
    // @Query      → aquí escribimos el JPQL (es como SQL pero con nombres de clase Java)
    @Modifying
    @Query("UPDATE User u SET u.tokenSesion = :token WHERE u.email = :email")
    void actualizarTokenSesion(@Param("email") String email, @Param("token") String token);

    // Desactiva la cuenta de un usuario (cancelar cuenta)
    // En vez de borrar al usuario de la BD, ponemos activo = false
    @Modifying
    @Query("UPDATE User u SET u.activo = false WHERE u.email = :email")
    void desactivarCuenta(@Param("email") String email);
}

