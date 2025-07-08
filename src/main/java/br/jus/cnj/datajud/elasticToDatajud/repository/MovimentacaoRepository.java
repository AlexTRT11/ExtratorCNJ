package br.jus.cnj.datajud.elasticToDatajud.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.jus.cnj.datajud.elasticToDatajud.model.Movimentacao;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;


@Repository
public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    @Query("select o from Movimentacao o where o.idProcesso = :idProcesso and o.idTribunal = :idTribunal ORDER BY o.dataInicioSituacao ")
    List<Movimentacao> findByIdProcesso(@Param("idProcesso") Long idProcesso, @Param("idTribunal") Integer idTribunal);
    
    @Query("select o from Movimentacao o where o.idProcesso = :idProcesso and o.idTribunal = :idTribunal and o.idGrau = :idGrau")
    List<Movimentacao> findByIdProcessoIdTribunalIdGrau(@Param("idProcesso") Long idProcesso, @Param("idTribunal") Integer idTribunal, @Param("idGrau") Integer idGrau);

    @Modifying
	@Transactional
	@Query("delete from Movimentacao o where o.idProcesso = :idProcesso and o.idTribunal = :idTribunal")
	void deleteByIdProcesso(@Param("idProcesso") Long idProcesso, @Param("idTribunal") Integer idTribunal);
}
