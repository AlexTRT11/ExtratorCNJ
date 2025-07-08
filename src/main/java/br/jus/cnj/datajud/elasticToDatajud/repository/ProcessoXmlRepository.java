package br.jus.cnj.datajud.elasticToDatajud.service;

import br.jus.cnj.datajud.elasticToDatajud.model.Parametro;
import br.jus.cnj.datajud.elasticToDatajud.repository.ParametroRepository;
import br.jus.cnj.datajud.elasticToDatajud.repository.ProcessoXmlRepository;
import br.jus.cnj.datajud.elasticToDatajud.service.XmlProcessParser;
import br.jus.cnj.datajud.elasticToDatajud.service.XmlSearchService;
import br.jus.cnj.datajud.elasticToDatajud.model.Tribunal;
import br.jus.cnj.datajud.elasticToDatajud.repository.TribunalRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class VerificadorService {

    @Autowired
    private ParametroRepository parametroRepository;

    @Autowired
    private TribunalRepository tribunalRepository;

    @Autowired
    private ConsolidadorService consolidadorService;

    @Autowired
    private ProcessoXmlRepository processoXmlRepository;

    @Autowired
    private XmlProcessParser xmlProcessParser;

    private long total = 0;

    public void init() {
        // Initialization logic...
    }

    /*
     * Define tribunals based on available XML records.
     */
    private void definirTribunal() {
        try {
            if (parametroRepository.count() == 0) {
                List<JSONObject> lista = processoXmlRepository.getTribunal(0L, new Date().getTime());
                for (JSONObject jo : lista) {
                    String tribunal = jo.getString("siglaTribunal");
                    Parametro p = new Parametro();
                    // ... fill Parametro fields ...
                    parametroRepository.save(p);
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao definir tribunais: " + e.getMessage());
        }
    }

    private void executarMigracaoProcessosPorTribunal() {
        // Example structure, adapt as needed for your actual migration logic
        Tribunal tribunal = /* get tribunal */;
        Long millisProximo = /* ... */;
        Long limiteTemporal = /* ... */;
        boolean limitarResultados = /* ... */;

        int qtd = 1;
        List<Object> listaDistribuida = new ArrayList<>();
        try {
            Thread d = new XmlSearchService(processoXmlRepository, xmlProcessParser, tribunal, millisProximo, limiteTemporal, limitarResultados);
            Thread e = new DistribuidorProcess(/* result */, consolidadorService, tribunal, /* millisRef */, limitarResultados);
            d.start();
            e.start();
            d.join();
            e.join();

            XmlSearchService ds = (XmlSearchService) listaDistribuida.get(0);
            if (ds.getResult() != null) {
                List<JSONObject> result = ds.getResult();
                int count = result.size();
                // ... continue migration logic ...
            }
        } catch (InterruptedException ie) {
            System.out.println("Exception " + ie.toString());
        }
    }

    private Parametro getParametro(String index) {
        // Implement as needed
        return null;
    }

    /*
     * Display estimated time for migration.
     */
    private void exibirTempo(String tribunal, long millis, String mensagem, int estimativaPorMinuto) {
        try {
            total = processoXmlRepository.countProcessos(tribunal, millis, new Date().getTime());
            int minutos = (int) total / estimativaPorMinuto;
            int horas = minutos / 60;
            System.out.println("Quantidade de " + mensagem + " a ser migrados: " + total +
                " - tempo estimado de migração: " + horas + " horas e " + (minutos - (horas * 60)) + " minutos");
        } catch (Exception e) {
            System.out.println("Quantidade de Processos não identificada");
        }
    }
}