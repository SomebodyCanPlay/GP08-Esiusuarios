package uclm.esi.ds.esiusuarios.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import uclm.esi.ds.esiusuarios.model.TokenRecuperacion;
import java.util.Optional;

// DAO para tokens de recuperación
public interface TokenRecuperacionDao extends JpaRepository<TokenRecuperacion, Long> {

    // Buscar token por su valor
    Optional<TokenRecuperacion> findByToken(String token);

    // Borrar tokens por email
    void deleteByEmail(String email);
}
