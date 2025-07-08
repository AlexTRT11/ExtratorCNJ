package br.jus.cnj.datajud.elasticToDatajud.service;

import br.jus.cnj.datajud.elasticToDatajud.model.Tribunal;
import br.jus.cnj.datajud.elasticToDatajud.repository.ProcessoXmlRepository;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

/**
 * Thread responsável por recuperar processos em XML e convertê-los em JSON.
 */
public class XmlSearchService extends Thread {

    private final long millis;
    private final long limiteTemporal;
    private final boolean limitarResultados;
    private final Tribunal tribunal;
    private List<JSONObject> lista;
    private final ProcessoXmlRepository processoXmlRepository;
    private final XmlProcessParser xmlProcessParser;

    public XmlSearchService(ProcessoXmlRepository processoXmlRepository, XmlProcessParser xmlProcessParser,
                            Tribunal tribunal, Long millis, Long limiteTemporal, boolean limitarResultados) {
        this.limitarResultados = limitarResultados;
        this.tribunal = tribunal;
        this.limiteTemporal = limiteTemporal;
        this.millis = millis;
        this.processoXmlRepository = processoXmlRepository;
        this.xmlProcessParser = xmlProcessParser;
    }

    @Override
    public void run() {
        try {
            List<String> xmlList = processoXmlRepository.getListProcessosXmlByTribunalMillis(
                    tribunal.getSigla(), millis, limiteTemporal, limitarResultados);
            List<JSONObject> result = new ArrayList<>();
            for (String xml : xmlList) {
                try {
                    result.add(xmlProcessParser.parse(xml));
                } catch (Exception e) {
                    System.out.println("Erro ao converter XML: " + e.getMessage());
                }
            }
            lista = result;
        } catch (Exception e) {
            System.out.println("Exceção: " + e.toString());
        }
    }

    public List<JSONObject> getResult() {
        return lista;
    }
}
