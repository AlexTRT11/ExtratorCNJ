package br.jus.cnj.datajud.elasticToDatajud.service;

import br.jus.cnj.datajud.elasticToDatajud.model.Tribunal;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

/**
 *
 * @author ricardo.nascimento
 */
public class DistribuidorProcess extends Thread{
    
    private long millis;
    private int qtdNovos = 0;
    private final boolean limitarResultados;
    private final Tribunal tribunal;
    private List<JSONObject> result;
    private final ConsolidadorService consolidadorService;

    public DistribuidorProcess(List<JSONObject> result, ConsolidadorService consolidadorService, Tribunal tribunal, Long millis, boolean limitarResultados) {
        this.limitarResultados = limitarResultados;
        this.tribunal = tribunal;
        this.result = result;
        this.millis = millis;
        this.consolidadorService = consolidadorService;
    }

    @Override
    public void run() {
        try{
        	millis = atualizarProcessosMigradosMillis(result, tribunal, millis, limitarResultados);
        }catch(Exception e){
            System.out.println("Exceção: "+e.toString());
        }
    }
    
    public Long atualizarProcessosMigradosMillis(List<JSONObject> lista, Tribunal tribunal, Long millis, boolean limitarResultados) {
        Long ultimo = millis;
        try {
            if (!lista.isEmpty()) {            	
                List<DistribuidorJSON> listaDistribuida = new ArrayList<>();
                int threads = limitarResultados? 10 : 20;
                List<List<List<JSONObject>>> listas = new ArrayList<>();
                int pos = -1;
                for(int l = 0; l < threads; l++) {
                	listas.add(new ArrayList<>());
                }
                String numero = "";
                List<JSONObject> list = new ArrayList<JSONObject>();
                for(JSONObject k : lista) {
                	if(!numero.equals(k.getString("grau")+k.getJSONObject("dadosBasicos").getString("numero"))){
                		if(list.size() > 0) {
                			listas.get(pos).add(new ArrayList<>(list));
                		}
                		pos++;
                		if(pos == threads) {
	                		pos = 0;
	                	}
                		numero = k.getString("grau")+k.getJSONObject("dadosBasicos").getString("numero");
                		list = new ArrayList<JSONObject>();
                	}
                	list.add(k);	                	    	
                }
                if(list.size() > 0) {
                    listas.get(pos).add(new ArrayList<>(list));
                }
                for (int i = 0; i < threads; i++) {
                    DistribuidorJSON d = new DistribuidorJSON(tribunal, listas.get(i), i, consolidadorService);
                    d.start();
                    listaDistribuida.add(d);
                }
                try {
                    for (DistribuidorJSON d : listaDistribuida) {
                        d.join();
                    }
                } catch (InterruptedException ie) {
                    System.out.println("Exception " + ie.toString());
                }
                for (DistribuidorJSON d : listaDistribuida) {
                    if (ultimo < d.getUltimo()) {
                        ultimo = d.getUltimo();
                    }
                    qtdNovos += d.getQtd();
                }
            }
        } catch (Exception e) {
            System.out.println("Ocorreu algum problema no " + tribunal.getSigla() + " : " + e.toString());
        }
        return ultimo;
    }
    
    public Long getMillis(){
    	return millis;
    }
    
    public int getNovos() {
    	return qtdNovos;
    }
}
