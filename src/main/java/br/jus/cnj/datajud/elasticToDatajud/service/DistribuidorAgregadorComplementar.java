package br.jus.cnj.datajud.elasticToDatajud.service;

import br.jus.cnj.datajud.elasticToDatajud.model.Movimentacao;
import br.jus.cnj.datajud.elasticToDatajud.model.Processo;
import br.jus.cnj.datajud.elasticToDatajud.model.ProcessoIndicador;
import br.jus.cnj.datajud.elasticToDatajud.repository.MovimentacaoRepository;
import br.jus.cnj.datajud.elasticToDatajud.util.DataCache;
import java.util.List;

import br.jus.cnj.datajud.elasticToDatajud.repository.ProcessoIndicadorRepository;

/**
 *
 * @author ricardo.nascimento
 */
public class DistribuidorAgregadorComplementar extends Thread{
    
    private int qtd = 0;
    private final List<Processo> lista;
    private final DataCache dataCache;
    private final ProcessoIndicadorRepository processoIndicadorRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final int posicao;

    public DistribuidorAgregadorComplementar(List<Processo> lista, int posicao, DataCache dataCache, ProcessoIndicadorRepository processoIndicadorRepository, MovimentacaoRepository movimentacaoRepository) {
        this.lista = lista;
        this.posicao = posicao;
        this.dataCache = dataCache;
        this.processoIndicadorRepository = processoIndicadorRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @Override
    public void run() {
        try{
            for(Processo p : lista){
                executeOperation(p);
            }
        }catch(Exception e){
            System.out.println("Exceção: "+e.toString());
        }
    }
    
    public void executeOperation(Processo p) {
        try {
        	List<Movimentacao> listaMovimentacao = movimentacaoRepository.findByIdProcesso(p.getId(), p.getIdTribunal());
        	//Converte as movimentações do proceso em indicadores
            List<ProcessoIndicador> listaAtualizados = dataCache.converterMovimentacoes(listaMovimentacao);
            //Carrega os indicadores existentes na base de dados para o processo
            List<ProcessoIndicador> listaIndicadores = processoIndicadorRepository.findByIdProcesso(p.getId(), p.getIdTribunal());
            //Para cada indicador já existente, atualizar o identificador para não registrar em duplicidade e remover da lista de indicadores
            for(ProcessoIndicador pNovo : listaAtualizados) {
            	for(ProcessoIndicador pAntigo : listaIndicadores) {
            		if(pNovo.equals(pAntigo)) {
                		pNovo.setId(pAntigo.getId());
                		listaIndicadores.remove(pAntigo);
                		break;
                	}
                }
            }
            //Remove todos os não encontrados com base dos indicadores carregados
            listaIndicadores.forEach(pi -> {
                processoIndicadorRepository.delete(pi);
            });
            //Persiste os novos e os já existentes
            listaAtualizados.forEach(pi -> {
                processoIndicadorRepository.save(pi);
            });                
            qtd++;
        } catch (Exception e) {
        }
    }
    
    public int getPosicao() {
        return posicao;
    }
    
    public int getQtd() {
        return qtd;
    }
}
