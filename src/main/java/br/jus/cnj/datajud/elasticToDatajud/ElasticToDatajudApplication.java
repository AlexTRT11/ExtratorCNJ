package br.jus.cnj.datajud.elasticToDatajud;

import br.jus.cnj.datajud.elasticToDatajud.service.VerificadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ElasticToDatajudApplication {

    @SuppressWarnings("unused")
	@Autowired
    private VerificadorService verificadorService;
    
    public static void main(String[] args) {
    	System.exit(SpringApplication.exit(SpringApplication.run(ElasticToDatajudApplication.class, args)));
    }
}