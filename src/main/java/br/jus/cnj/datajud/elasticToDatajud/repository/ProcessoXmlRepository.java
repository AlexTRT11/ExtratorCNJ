package br.jus.cnj.datajud.elasticToDatajud.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.jus.cnj.datajud.elasticToDatajud.model.ProcessoXml;

@Repository
public interface ProcessoXmlRepository extends JpaRepository<ProcessoXml, Long> {

    @Query("select o from ProcessoXml o where o.siglaTribunal = :siglaTribunal and o.millisInsercao >= :inicio and o.millisInsercao <= :fim order by o.millisInsercao")
    List<ProcessoXml> findBySiglaTribunalAndMillisInsercaoBetween(@Param("siglaTribunal") String siglaTribunal,
                                                                  @Param("inicio") Long inicio,
                                                                  @Param("fim") Long fim,
                                                                  Pageable pageable);

    @Query("select count(o) from ProcessoXml o where o.siglaTribunal = :siglaTribunal and o.millisInsercao >= :inicio and o.millisInsercao <= :fim")
    long countBySiglaTribunalAndMillisInsercaoBetween(@Param("siglaTribunal") String siglaTribunal,
                                                      @Param("inicio") Long inicio,
                                                      @Param("fim") Long fim);
}
