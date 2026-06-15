package uclm.esi.ds.esiusuarios.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import uclm.esi.ds.esiusuarios.model.TokenConfirmacion;

public interface TokenConfirmacionRepository extends JpaRepository<TokenConfirmacion, Long> {
    Optional<TokenConfirmacion> findByToken(String token);
}
