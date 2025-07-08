package br.jus.cnj.datajud.elasticToDatajud.service;

import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.jus.cnj.datajud.elasticToDatajud.model.Movimentacao;
import br.jus.cnj.datajud.elasticToDatajud.model.Processo;

@Service
public class ConsolidadorService {

    Logger log = LoggerFactory.getLogger(ConsolidadorService.class);

    @Autowired
    public ConsolidadorProcessoService consolidadorProcessoService;

    @Autowired
    private ConsolidadorMovimentoService consolidadorMovimentoService;
    
    @Autowired
    private ConsolidadorIndicadorService consolidadorIndicadorService;

    /**
     * Método que controla a conversão do Documento do Elastic em Processo e Movimentações
     * @param processosJson Lista de Documentos do Elastic
     */
    public boolean consolidar(List<JSONObject> processosJson) {
        if (processosJson == null || processosJson.size() == 0) {
            throw new RuntimeException("O arquivo JSON com os dados do processo está vazio");
        }
        try {
	        Processo processo = consolidadorProcessoService.consolidar(processosJson);
	        if(processo != null) {
	        	consolidadorMovimentoService.consolidar(processo, processosJson);
	        	List<Movimentacao> movimentacoes = consolidadorMovimentoService.consolidar(processo, processosJson);
		        if( movimentacoes.size() > 0) {
		        	consolidadorIndicadorService.consolidar(movimentacoes, processo.getId(), processo.getIdTribunal());
		        }
	        }
	        return true;
        }catch(Exception e) {
        	return false;
        }
    }
}
