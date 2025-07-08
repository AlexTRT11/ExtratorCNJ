package br.jus.cnj.datajud.elasticToDatajud.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.jus.cnj.datajud.elasticToDatajud.model.Processo;

@Repository
public interface ProcessoRepository extends JpaRepository<Processo, Long> {

    @Query("select o from Processo o where o.idTribunal = :idTribunal and o.idGrau = :idGrau and o.numero = :numero")
    List<Processo> findByIdTribunalIdGrauNumero(@Param("idTribunal") Integer idTribunal, @Param("idGrau") Integer idGrau, @Param("numero") String numero);
    
    @Query("select o from Processo o where o.idTribunal = :idTribunal and o.id = :idProcesso")
    List<Processo> findByIdTribunalIdProcesso(@Param("idTribunal") Integer idTribunal, @Param("idProcesso") Long idProcesso);
    
    @Query("select o from Processo o where o.idTribunal = :idTribunal and o.siglaGrau = :siglaGrau and o.numero = :numero")
    List<Processo> findByIdTribunalSiglaGrauNumero(@Param("idTribunal") Integer idTribunal, @Param("siglaGrau") String siglaGrau, @Param("numero") String numero);
    
    @Query("select o from Processo o where o.idTribunal = :idTribunal and o.numero = :numero")
    List<Processo> findByIdTribunalNumero(@Param("idTribunal") Integer idTribunal, @Param("numero") String numero);
    
    @Query("select o from Processo o where o.idTribunal = :idTribunal ORDER BY o.id ")
    List<Processo> findByIdTribunal(@Param("idTribunal") Integer idTribunal, Pageable pageable);
    
    @Query("SELECT COUNT(*) FROM Processo o where o.idTribunal = :idTribunal")
    long countProcess(@Param("idTribunal") Integer idTribunal);
}
