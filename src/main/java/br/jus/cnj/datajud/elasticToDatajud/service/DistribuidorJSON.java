package br.jus.cnj.datajud.elasticToDatajud.service;

import br.jus.cnj.datajud.elasticToDatajud.model.Tribunal;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

/**
 *
 * @author ricardo.nascimento
 */
public class DistribuidorJSON extends Thread{
    
    private long ultimo;
    private int qtd = 0;
    private final Tribunal tribunal;
    private final List<List<JSONObject>> lista;
    private final List<List<JSONObject>> listaErro;
    private final ConsolidadorService consolidadorService;
    private final int posicao;

    public DistribuidorJSON(Tribunal tribunal, List<List<JSONObject>> lista, int posicao, ConsolidadorService consolidadorService) {
        this.tribunal = tribunal;
        this.lista = lista;
        this.posicao = posicao;
        this.consolidadorService = consolidadorService;
        listaErro = new ArrayList<>();
    }

    @Override
    public void run() {
        try{
            for(List<JSONObject> p : lista){
                verificarJSON(p, tribunal.getSigla());
            }
        }catch(Exception e){
            System.out.println("Exceção: "+e.toString());
        }
    }
    
    public void verificarJSON(List<JSONObject> saida, String sigla) {
        try {
        	if(!consolidadorService.consolidar(saida)) {
        		listaErro.add(saida);
        	}
        	qtd++;
        	for(JSONObject j : saida) {
	        	if (ultimo < j.getLong("millisInsercao")) {
	                ultimo = j.getLong("millisInsercao");
	            }
        	}
        } catch (Exception e) {
        }
    }
    
    public long getUltimo() {
        return ultimo;
    }
    
    public int getPosicao() {
        return posicao;
    }
    
    public int getQtd() {
        return qtd;
    }
    
    public List<List<JSONObject>> getListaErro(){
    	return listaErro;
    }
}
