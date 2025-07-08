package br.jus.cnj.datajud.elasticToDatajud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.jus.cnj.datajud.elasticToDatajud.model.Parametro;

@Repository
public interface ParametroRepository extends JpaRepository <Parametro, String> {
	
}
