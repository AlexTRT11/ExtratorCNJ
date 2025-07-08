package br.jus.cnj.datajud.elasticToDatajud.service;

import br.jus.cnj.datajud.elasticToDatajud.model.Tribunal;
import br.jus.cnj.datajud.elasticToDatajud.repository.ProcessoElasticRepository;

import java.util.List;
import org.json.JSONObject;

/**
 *
 * @author ricardo.nascimento
 */
public class DistribuidorSearch extends Thread{
    
    private final long millis;
    private final long limiteTemporal;
    private final boolean limitarResultados;
    private final Tribunal tribunal;
    private List<JSONObject> lista;
    private final ProcessoElasticRepository processoElasticRepository;

    public DistribuidorSearch(ProcessoElasticRepository processoElasticRepository, Tribunal tribunal, Long millis, Long limiteTemporal, boolean limitarResultados) {
        this.limitarResultados = limitarResultados;
        this.tribunal = tribunal;
        this.limiteTemporal = limiteTemporal;
        this.millis = millis;
        this.processoElasticRepository = processoElasticRepository;
    }

    @Override
    public void run() {
        try{
        	lista = processoElasticRepository.getListProcessosByTribunalMillis(tribunal.getSigla(), millis, limiteTemporal, limitarResultados);
        }catch(Exception e){
            System.out.println("Exceção: "+e.toString());
        }
    }
    
    public List<JSONObject> getResult(){
    	return lista;
    }
}
