package uclm.esi.ds.esiusuarios;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

// ¿Por qué necesitamos esto?
// Cuando añadimos Spring Security al build.gradle, automáticamente bloquea TODAS
// las rutas y pide usuario/contraseña. Eso rompería nuestros endpoints.
// Esta clase le dice a Spring Security: "déjalo pasar todo, nosotros gestionamos la seguridad".

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Desactivar CSRF (Cross-Site Request Forgery)
            // CSRF es una protección para formularios web clásicos.
            // En una API REST que usa tokens propios, no necesitamos CSRF.
            .csrf(csrf -> csrf.disable())

            // Permitir todas las peticiones sin pedir login a Spring Security
            // (nosotros ya gestionamos la autenticación con nuestro UserService)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
