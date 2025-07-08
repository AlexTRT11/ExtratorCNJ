package br.jus.cnj.datajud.elasticToDatajud.service;

import br.jus.cnj.datajud.elasticToDatajud.model.Parametro;
import br.jus.cnj.datajud.elasticToDatajud.repository.ParametroRepository;
import br.jus.cnj.datajud.elasticToDatajud.repository.ProcessoXmlRepository;
import br.jus.cnj.datajud.elasticToDatajud.service.XmlProcessParser;
import br.jus.cnj.datajud.elasticToDatajud.service.XmlSearchService;
import br.jus.cnj.datajud.elasticToDatajud.model.Tribunal;
import br.jus.cnj.datajud.elasticToDatajud.repository.TribunalRepository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author ricardo.nascimento
 */
@Service
public class VerificadorService {	
    
    @Autowired
    private TribunalRepository tribunalRepository;
    
    @Autowired
    private ParametroRepository parametroRepository;
    
    @Autowired
    private ConsolidadorService consolidadorService;
    
    @Autowired
    private ProcessoXmlRepository processoXmlRepository;

    @Autowired
    private XmlProcessParser xmlProcessParser;
    
    private long total = 0;
    
    @Value("${millisInsercao}")
    private String millis;
    
    @PostConstruct
    public void init() {
    	definirTribunal();
    	executarMigracaoProcessosPorTribunal();
    }
    
    /**
     * Método que define o tribunal a ser migrado caso não exista nenhum registrado
     */
    private void definirTribunal() {
        try {
                if(parametroRepository.count() == 0) {
                        List<JSONObject> lista = processoXmlRepository.getTribunal(0L, new Date().getTime());
	        	for(JSONObject jo : lista) {
	        		String tribunal = jo.getString("siglaTribunal");
	        		Parametro p = new Parametro();
	        		p.setChave(String.format("consumidor-bi:millisInsercao:%s", tribunal.toLowerCase()));
	        		p.setIndicador(millis);
	        		p.setParte(millis);
	        		p.setValor(millis);
	        		p.setSeeu(millis);
	        		p.setDescricao("Armazena o millinserção do último registro do índice processos-"+tribunal+" no DataJud, processado pelo serviço ConsumidorBI");
	        		parametroRepository.save(p);
	        	}
	        	if(parametroRepository.count() == 0) {
	        		System.out.println("O usuário do elastic não tem acesso a view-processos-sigilo-* necessário para importar os dados");
	        	}
        	}
        } catch (Exception e) {
            System.out.println("Exceção: " + e.toString());
        }
    }
    
    /**
     * Método que trasnforma os documentos do elastic em registros de processos e movimentações do data mart
     * O quantitativo de processos convertidos no final pode ser apresentado como um número superior ao total existente no Elastic pois para garantir que todos os processos sejam carregados 
     * buscamos sempre a partir do último milisegundo carregado, o que no mínimo repete um número de processo já utilizado. Entretanto o processo não tem seu conteúdo replicado, pois o mesmo é validado antes da inserção,
     * mas como existem diversos exemplos de processos gravados no mesmo milisegundo, o uso de um milisegundo superior ao último poderia deixar processos para trás. Desta forma escolhemos carregar mais vezes um processo 
     * do que deixar de carregar algum.
     */
    private void executarMigracaoProcessosPorTribunal() {
        try {
        	List<Thread> listaDistribuida = new ArrayList<>();   
        	List<Tribunal> listaTribunais = tribunalRepository.findAll();
            for (Tribunal tribunal : listaTribunais) {
                int count = 0;
                Parametro parametro = getParametro(tribunal.getSigla());
                //Se estiver registrado para carregar o tribunal
                if(parametro != null) {
	                Long millisRef = millis.equals("0")? Long.parseLong(parametro.getValor()) : Long.parseLong(millis);
	                Long millisProximo = millis.equals("0")? Long.parseLong(parametro.getValor()) : Long.parseLong(millis);
	                //Exibe estimativa de tempo
	                exibirTempo(tribunal.getSigla(),millisRef,"processos com movimentações",1500);
	                long limiteTemporal = new Date().getTime();
	                int qtd = 1;
	                float percentual = 0f;
	                int quantidadeNovos = 0;
	                boolean limitarResultados = true;
	                boolean ultimaVez = false;
	                List<JSONObject> result = new ArrayList<>();
	                do {
	                	qtd = 1;
	                	listaDistribuida = new ArrayList<>();
	                	//Criar duas threads, uma delas identifica o último milisegundo e a outra é usada para carga, de forma que enquanto uma está carregando a próxima lista a outra esteja sendo convertida
                                Thread d = new XmlSearchService(processoXmlRepository, xmlProcessParser, tribunal, millisProximo, limiteTemporal, limitarResultados);
	                	Thread e = new DistribuidorProcess(result,consolidadorService,tribunal,millisRef,limitarResultados);
		                d.start();
		                e.start();
		                listaDistribuida.add(d);
		                listaDistribuida.add(e);
		                //Executa processamento distribuído
		                try {
		                    for (Thread t : listaDistribuida) {
		                        t.join();
		                    }
		                } catch (InterruptedException ie) {
		                    System.out.println("Exception " + ie.toString());
		                }
		                try {
                                    XmlSearchService ds = (XmlSearchService) listaDistribuida.get(0);
		                    if (ds.getResult() != null) {
		                    	result = ds.getResult();
		                    	count += result.size();
		                        qtd = result.size();
		                        if(ds.getResult().size() > 0) {
		                        	for(JSONObject ob : result) {
		                        		if(ob.getLong("millisInsercao") > millisProximo)
		                        			millisProximo = ob.getLong("millisInsercao");
		                        	}
			                        if(!(qtd > 0 && millisProximo > millisRef)) {
			                        	ultimaVez = !ultimaVez;
			                        }
		                        }
		                    }
		                    DistribuidorProcess dp = (DistribuidorProcess) listaDistribuida.get(1);
		                    millisRef = dp.getMillis();
		                    //Atualiza o parametro para o último milisegundo já processado
	                    	if (millisRef > Long.parseLong(parametro.getValor())) {
	                            parametro.setValor(millisRef.toString());
	                            parametro.setIndicador(millisRef.toString());
	                            parametroRepository.save(parametro);
	                            //Ajuste nos números para não exibir valores superiores, pois a cada iteração, pelo menos um processo é carregado uma segunda vez, em virtude de ser possível o mesmo millisinsercao ser igual em mais de um processo
	                            percentual = (count/(total*1f)) * 100 > 100? 100 : (count/(total*1f)) * 100;
	                            System.out.println("Executados " + (count>total?total:count) + " novos processos com movimentações - "+percentual+"% de "+total);
	                        }
		                    if(dp.getNovos() > 0) {
		                    	quantidadeNovos += dp.getNovos();
		                    }
		                }catch (Exception ex) {
		                    System.out.println("Exception na Thread " + ex.getMessage());
		                }
	                }while ((qtd > 0 && count % 1000 == 0) || ultimaVez);
	                if (quantidadeNovos > 0) {
	                	//Ajuste nos números para exibir todos os processos migrados apenas, mesmo que possa ser associado a um processo igual(capas distintas)
	                	percentual = (quantidadeNovos/(total*1f)) * 100 > 100? 100 : (quantidadeNovos/(total*1f)) * 100;
	                    System.out.println(tribunal.getSigla() + "- Novos Processos Migrados: " + quantidadeNovos +" - "+percentual+"% de "+total);
	                } else {
	                    System.out.println(tribunal.getSigla() + " encerrado sem novos Processos e movimentações");
	                }
	            }
            }
        } catch (NumberFormatException e) {
            System.out.println("Exceção: " + e.toString());
        }
    }
    
    /**
     * Método que identifica o Tribunal a ser carregado e qual foi o último milisegundo registrado
     * @param index Identificador do Tribunal
     * @return Objeto Parametro do Tribunal identificado
     */
    private Parametro getParametro(String index) {
        Parametro parametro = null;
        String chave = String.format("consumidor-bi:millisInsercao:%s", index.toLowerCase());
        Optional<Parametro> p = parametroRepository.findById(chave);
        if (p.isPresent()) {
            parametro = p.get();
        }
        return parametro;
    }
    
    /**
     * Método que exibe a quantidade de processos no Elastic que devem ser processadas e armazenadas no data mart estimando o tempo de conclusão da tarefa
     * @param tribunal Tribunal no qual será buscado o quantitativo
     * @param millis A partir de qual milisegundo de registro os documentos do elastic serão carregados
     * @param estimativaPorMinuto Estimativa de processos a serem concluídos por minuto utilizando o data mart num SSD de 500MB/s
     * @param mensagem Identificação de qual eatapa estará sendo executada
     */
    private void exibirTempo(String tribunal, long millis, String mensagem, int estimativaPorMinuto) {
    	try {
                total = processoXmlRepository.countProcessos(tribunal, millis, new Date().getTime());
	    	int minutos = (int)total/estimativaPorMinuto;
	    	int horas = minutos/60;
	    	System.out.println("Quantidade de "+ mensagem + " a ser migrados: " + total + " - tempo estimado de migração: " + horas+ " horas e "+(minutos-(horas*60))+ " minutos");
    	}catch(Exception e) {
    		System.out.println("Quantidade de Processos não identificada");
    	}
    }}