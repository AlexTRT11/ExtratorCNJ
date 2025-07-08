package br.jus.cnj.datajud.elasticToDatajud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.jus.cnj.datajud.elasticToDatajud.model.Classe;

@Repository
public interface ClasseRepository extends JpaRepository<Classe, Long> {

}
