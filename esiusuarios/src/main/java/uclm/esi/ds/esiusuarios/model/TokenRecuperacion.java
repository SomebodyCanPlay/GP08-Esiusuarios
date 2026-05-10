package uclm.esi.ds.esiusuarios.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;

@Entity
@Table(name = "token_recuperacion")
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "token", nullable = false, unique = true, length = 100)
    private String token;

    @Column(name = "expira_en", nullable = false)
    private Long expiraEn;

    public TokenRecuperacion() {}

    public TokenRecuperacion(String email, String token, Long expiraEn) {
        this.email = email;
        this.token = token;
        this.expiraEn = expiraEn;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getExpiraEn() {
        return expiraEn;
    }

    public void setExpiraEn(Long expiraEn) {
        this.expiraEn = expiraEn;
    }

    public boolean haExpirado() {
        return System.currentTimeMillis() > this.expiraEn;
    }
}
