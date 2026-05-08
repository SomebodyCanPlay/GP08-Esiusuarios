package uclm.esi.ds.esiusuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // Permite que el servidor haga tareas en segundo plano (como enviar emails)
public class EsiusuariosApplication {

	public static void main(String[] args) {
		SpringApplication.run(EsiusuariosApplication.class, args);
	}

}
