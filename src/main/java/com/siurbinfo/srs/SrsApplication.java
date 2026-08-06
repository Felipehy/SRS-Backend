package com.siurbinfo.srs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal da aplicacao Spring Boot.
 * Ponto de entrada que inicializa o contexto e sobe o servidor embutido.
 */
@SpringBootApplication
public class SrsApplication {

	// Inicia a aplicacao Spring Boot
	public static void main(String[] args) {
		SpringApplication.run(SrsApplication.class, args);
	}

}
