package br.jus.cnj.datajud.elasticToDatajud.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.jus.cnj.datajud.elasticToDatajud.model.Movimentacao;
import br.jus.cnj.datajud.elasticToDatajud.model.ProcessoIndicador;
import br.jus.cnj.datajud.elasticToDatajud.repository.ProcessoIndicadorRepository;
import br.jus.cnj.datajud.elasticToDatajud.util.DataCache;

@Service
public class ConsolidadorIndicadorService {
	@Autowired
	private DataCache dataCache;
	
	@Autowired
	private ProcessoIndicadorRepository processoIndicadorRepository;
	
	public void consolidar(List<Movimentacao> movimentacoes, long idProcesso, int idTribunal) {
		if(movimentacoes.size() > 0) {
			atualizarIndicadores(movimentacoes, idProcesso, idTribunal);
		}
	}
	
	public void atualizarIndicadores(List<Movimentacao> movimentacoes, long idProcesso, int idTribunal) {
        List<ProcessoIndicador> listaAtualizados = dataCache.converterMovimentacoes(movimentacoes);
        List<ProcessoIndicador> listaIndicadores = processoIndicadorRepository.findByIdProcesso(idProcesso, idTribunal);
        for(ProcessoIndicador pNovo : listaAtualizados) {
        	for(ProcessoIndicador pAntigo : listaIndicadores) {
        		if(pNovo.equals(pAntigo)) {
        			pNovo.setId(pAntigo.getId());
            		listaIndicadores.remove(pAntigo);
            		break;
            	}
            }
        }
        listaIndicadores.forEach(pi -> {
            processoIndicadorRepository.delete(pi);
        });
        listaAtualizados.forEach(pi -> {
            processoIndicadorRepository.saveAndFlush(pi);
        }); 
    }
}
