package br.jus.cnj.datajud.elasticToDatajud.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.jus.cnj.datajud.elasticToDatajud.model.ProcessoIndicador;

@Repository
public interface ProcessoIndicadorRepository extends JpaRepository<ProcessoIndicador, Long> {

	@Query("select o from ProcessoIndicador o where o.idProcesso = :idProcesso and o.idTribunal = :idTribunal")
    List<ProcessoIndicador> findByIdProcesso(@Param("idProcesso") Long idProcesso, @Param("idTribunal") Integer idTribunal);
	
	@Query("select o from ProcessoIndicador o where o.idProcesso = :idProcesso and o.idTribunal = :idTribunal and o.idGrau = :idGrau and o.idTipo in (4,5) and o.dataUltimoFim = 0 order by o.dataUltimoInicio desc")
	List<ProcessoIndicador> findByIdProcessoIdTribunalIdGrau(@Param("idProcesso") Long idProcesso, @Param("idTribunal") Integer idTribunal, @Param("idGrau") Integer idGrau);
}
