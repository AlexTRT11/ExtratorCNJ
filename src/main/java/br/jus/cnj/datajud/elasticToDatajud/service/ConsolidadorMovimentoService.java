package br.jus.cnj.datajud.elasticToDatajud.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
//import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.jus.cnj.datajud.elasticToDatajud.model.Assunto;
import br.jus.cnj.datajud.elasticToDatajud.model.Classe;
import br.jus.cnj.datajud.elasticToDatajud.model.Complemento;
import br.jus.cnj.datajud.elasticToDatajud.model.Movimentacao;
import br.jus.cnj.datajud.elasticToDatajud.model.OrgaoJulgador;
import br.jus.cnj.datajud.elasticToDatajud.model.Processo;
import br.jus.cnj.datajud.elasticToDatajud.model.ProcessoIndicador;
import br.jus.cnj.datajud.elasticToDatajud.model.Situacao;
import br.jus.cnj.datajud.elasticToDatajud.repository.MovimentacaoRepository;
import br.jus.cnj.datajud.elasticToDatajud.repository.ProcessoRepository;
import br.jus.cnj.datajud.elasticToDatajud.util.DataCache;
import br.jus.cnj.datajud.elasticToDatajud.util.Misc;
import br.jus.cnj.datajud.elasticToDatajud.repository.ProcessoIndicadorRepository;

@Service
public class ConsolidadorMovimentoService {
    private static final Integer INVALIDO = -1;
    private static final Integer NAO_INFORMADO = 0;
    private static final Integer FASE_CONHECIMENTO = 1;
    private static final Integer FASE_EXECUCAO = 2;
    private static final Integer FASE_INVESTIGATORIA = 3;
    private static final Integer FASE_OUTRA = 4;
    private static final Integer PROCEDIMENTO_CONHECIMENTO = 1;
    private static final Integer PROCEDIMENTO_EXECUCAO_JUDICIAL = 2;
    private static final Integer PROCEDIMENTO_EXECUCAO_FISCAL = 3;
    private static final Integer PROCEDIMENTO_EXECUCAO_EXTRAJUDICIAL = 4;
    private static final Integer PROCEDIMENTO_INVESTIGATORIO = 5;
    private static final Integer NATUREZA_LIQUIDACAO = 24;

    private enum ProcedimentoFaseEnum {
        INVESTIGATORIO,
        CONHECIMENTO_CIVEL,
        CONHECIMENTO_CRIMINAL,
        CONHECIMENTO_FISCAL,
        CONHECIMENTO_EXTRAJUDICIAL,
        EXECUCAO_JUDICIAL_CIVEL,
        LIQUIDACAO_JUDICIAL_CIVEL,
        EXECUCAO_JUDICIAL_CRIMINAL,
        EXECUCAO_FISCAL,
        EXECUCAO_EXTRAJUDICIAL,
        OUTRO
    };

    private enum OrdenacaoEnum {
        CRESCENTE,
        DECRESCENTE
    };

    Logger log = LoggerFactory.getLogger(ConsolidadorMovimentoService.class);

    @Autowired
    private DataCache dataCache;

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private ProcessoIndicadorRepository processoIndicadorRepository;
    
    private boolean processoTRF1paraTRF6 = false;
    private boolean mudancaClasse = false;

    /**
     * Método que controla a consolidação das movimentações
     * @param processo Processo que irá receber as movimentações
     * @param processosJson Lista de JSONObjects contendo os docuemntos do elastic de onde serão extraídas as movimentações
     * @return Lista atualizada de movimentações
     */
    public List<Movimentacao> consolidar(Processo processo, List<JSONObject> processosJson) {
    	List<Movimentacao> movimentacoes = new ArrayList<>();
        if (processo != null) {
        	//tratamento específico para identificar se o processo era de um tribunal que foi dividido em dois (TRF1 e TRF6)
        	processoTRF1paraTRF6 = processo.getIdTribunal() == 5 && processoRepository.findByIdTribunalIdGrauNumero(95, processo.getIdGrau(), processo.getNumero()).size() > 0;
        	//Cria as movimentações a partir do docuemnto do elastic
            movimentacoes = criarMovimentacoes(processo, processosJson);
            //Geração de Movimentos com Situações Vinculadas
            movimentacoes = getMovimentosComSituacaoVinculada(movimentacoes);
            //Trata movimentações de redistribuição
            movimentacoes = normalizarMovimentoRedistribuicao(movimentacoes, processosJson);
            //Trata alterações de classe
            movimentacoes = normalizarMudancaClasses(movimentacoes);
            //Calcula fase processual
            movimentacoes = calcularFasesProcessuais(processo, movimentacoes);
            //Calcula flags especiais
            movimentacoes = atualizarFlagsEspeciais(processo, movimentacoes);
            //Atualiza as situações dos movimentos
            movimentacoes = atualizarSituacaoMovimentos(processo, movimentacoes);
            //Atualiza as situações após redistribuição
            movimentacoes = atualizarSituacoesAposRedistribuicao(movimentacoes);
            //persiste as movimentações
            movimentacoes = salvarMovimentacoes(movimentacoes, processo.getRegistroUnico());
            //Carrega movimentações atualizadas
            movimentacoes = movimentacaoRepository.findByIdProcesso(processo.getId(), processo.getIdTribunal());
            if(movimentacoes != null && movimentacoes.size() > 0) {
            	Collections.sort(movimentacoes);
            	//Atualiza atributos do processo que derivam das movimentações após ordenação
	        	atualizarDataUltimaMovimentacao(processo, processosJson);
				atualizarFasesProcesso(processo, movimentacoes);
				atualizarClassesProcesso(processo, movimentacoes);
				atualizarAssuntosProcesso(processo, movimentacoes);
				atualizarOrgaosJulgadoresProcesso(processo, movimentacoes);
				atualizarOrgaosJulgadoresColegiadoProcesso(processo, movimentacoes);
				atualizarIsCriminal(processo, movimentacoes);
				atualizarUltimoOrgaoJulgador(processo, movimentacoes);
				atualizarUltimaClassePorFase(processo, movimentacoes);
				atualizarPertenceRecorte(processo, movimentacoes);
				atualizarDataSituacao(processo, movimentacoes);
				processoRepository.saveAndFlush(processo);
	        }else {
	        	if(processo.getIdClasse().isEmpty() || processo.getIdAssunto().isEmpty()) {
	        		for (JSONObject proc : processosJson) {
	        			JSONObject dadosBasicos = proc.has("dadosBasicos") ? proc.getJSONObject("dadosBasicos") : null;
	        			Integer classe = dadosBasicos != null ? (dadosBasicos.has("classeProcessual") ? dadosBasicos.getInt("classeProcessual") : null) : null;
	        			classe = getIdClasse(classe);
	        			if(classe != null && !processo.getIdClasse().contains(classe)){
	        				List<Integer> listaClasses = processo.getIdClasse();
	        				listaClasses.add(classe);
	        				processo.setIdClasse(listaClasses);
	        			}
	        			List<JSONObject> assuntos = dadosBasicos != null ? (dadosBasicos.has("assunto") ? Misc.elementToJsonObjectList(dadosBasicos, "assunto") : null) : null;
	        			List<Integer> idAssunto = getIdAssunto(assuntos);
	        			if(idAssunto != null){
	        				for(Integer i : idAssunto) {
	        					if(!processo.getIdAssunto().contains(i)) {
	        						List<Integer> listaAssuntos = processo.getIdAssunto();
	        						listaAssuntos.add(i);
	    	        				processo.setIdAssunto(listaAssuntos);
	        					}
	        				}
	        			}
	        		}
	        		processoRepository.saveAndFlush(processo);
	        	}
	        }
        }
        return movimentacoes;
    }
    
    /**
     * Método que anula cada movimentação após a identificação de um moviemnto de anulação de sentença ou acordão
     * @param processo Processo em questão
     * @param movimentacoes Lista de Movimentações
     * @return Lista atualizada de movimentações
     */
    private List<Movimentacao> atualizarFlagsEspeciais(Processo processo, List<Movimentacao> movimentacoes) {
		final Integer MOVIMENTO_ANULACAO_SENTENCA_ACORDAO = 11373;
		final Integer[] situacoesJulgamento = new Integer[]{18, 27, 28, 29, 62, 72, 90, 129};
		boolean anulacao = false;
		List<Integer> listAnulacao = new ArrayList<>();
		listAnulacao.add(MOVIMENTO_ANULACAO_SENTENCA_ACORDAO);
		
		int count = movimentacoes.size()-1;
		while(count >= 0) {
			if(anulacao) {
				if(Arrays.asList(situacoesJulgamento).contains(movimentacoes.get(count).getIdSituacao())) {
					movimentacoes.get(count).setAnulado(true);
					anulacao = false;
				}
			}
			if(listAnulacao.contains(movimentacoes.get(count).getIdMovimento())) {
				anulacao = true;
            }
			count--;
		}
        return movimentacoes;
    }    
    
    /**
     * Método que extrai os dados do docuemnto do elastic para gerar as moviemntações
     * @param processo Processo em questão
     * @param processosJson Lista de docuemntos do elastic
     * @return Lista atualizada de movimentações
     */
    public List<Movimentacao> criarMovimentacoes(Processo processo, List<JSONObject> processosJson) {
    	final Date DATA_MINIMA = Misc.stringToDate("19000101", "yyyyMMdd");
        Map<String, Movimentacao> movimentacaoMap = new HashMap<String, Movimentacao>(0);
        for (JSONObject proc : processosJson) {
            List<JSONObject> movList = Misc.elementToJsonObjectList(proc, "movimento");
            if (movList != null && movList.size() > 0) {
                JSONObject dadosBasicos = proc.has("dadosBasicos") ? proc.getJSONObject("dadosBasicos") : null;
                Integer classe = dadosBasicos != null ? (dadosBasicos.has("classeProcessual") ? dadosBasicos.getInt("classeProcessual") : null) : null;
                List<JSONObject> assuntos = dadosBasicos != null ? (dadosBasicos.has("assunto") ? Misc.elementToJsonObjectList(dadosBasicos, "assunto") : null) : null;
                for (JSONObject mov : movList) {
                    Date dataMovimentacao = Misc.stringToDateTime(mov.has("dataHora") ? mov.get("dataHora").toString() : null);
                    JSONObject orgaoJulgador = getOrgaoJulgador(mov, dadosBasicos);
                    JSONObject orgaoJulgadorColegiado = getOrgaoJulgadorColegiado(mov, dadosBasicos);
                    //Carrega apenas movimentos com data igual ou superior à data mínima, ou seja apenas movimentos após 01/01/1900 serão considerados
                    if (Misc.isDataMaiorIgual(dataMovimentacao, DATA_MINIMA)) {
                    	String hash = getHashMovimento(mov);
                    	Integer idClasse = getIdClasse(classe);
                        if(mov.has("classeProcessual") && !mov.isNull("classeProcessual")) {
                        	idClasse = getIdClasse(mov.getInt("classeProcessual"));
                        }
                        Integer idOrgaoJulgador = getIdOrgaoJulgador(orgaoJulgador, processo.getIdTribunal());
                        Integer idOrgaoJulgadorColegiado = getIdOrgaoJulgador(orgaoJulgadorColegiado, processo.getIdTribunal());
                        if(idOrgaoJulgador <= 0) {
                        	idOrgaoJulgador = getIdOrgaoJulgador(getOrgaoJulgador(dadosBasicos, dadosBasicos), processo.getIdTribunal());
                        }
                        if (!movimentacaoMap.containsKey(hash)) {
                            List<Integer> idAssunto = getIdAssunto(assuntos);
                            String cpfMagistrado = getCpfMagistrado(mov);
                            Movimentacao movimentacao = criarMovimentacao(processo.getId(), idClasse, idAssunto, idOrgaoJulgador, idOrgaoJulgadorColegiado, cpfMagistrado, mov, processo.getIdFormato(), processo.getIdGrau(), processo.getIdTribunal());
                            movimentacaoMap.put(hash, movimentacao);
                        }else {
                        	if(idClasse != getIdClasse(classe)) {
                        		movimentacaoMap.get(hash).setIdClasse(idClasse);
                        	}
                        	if(idOrgaoJulgador != getIdOrgaoJulgador(orgaoJulgador, processo.getIdTribunal())) {
                        		movimentacaoMap.get(hash).setIdOrgaoJulgador(idOrgaoJulgador);
                        	}
                        	if(idOrgaoJulgadorColegiado != getIdOrgaoJulgador(orgaoJulgadorColegiado, processo.getIdTribunal())) {
                        		movimentacaoMap.get(hash).setIdOrgaoJulgadorColegiado(idOrgaoJulgadorColegiado);
                        	}
                        }
                    }
                }
            }
        }
        List<Movimentacao> movimentacaoList = new ArrayList<Movimentacao>(movimentacaoMap.values());
        Collections.sort(movimentacaoList);
        return movimentacaoList;
    }

    private Movimentacao criarMovimentacao(Long idProcesso, Integer idClasse, List<Integer> idAssunto, Integer idOrgaoJulgador, Integer idOrgaoJulgadorColegiado, String cpfMagistrado, JSONObject mov, Integer idFormato, Integer idGrau, Integer idTribunal) {
        return criarMovimentacao(idProcesso, idClasse, idAssunto, idOrgaoJulgador, idOrgaoJulgadorColegiado, cpfMagistrado, null, mov, idFormato, idGrau, idTribunal);
    }

    private Movimentacao criarMovimentacao(Long idProcesso, Integer idClasse, List<Integer> idAssunto, Integer idOrgaoJulgador, Integer idOrgaoJulgadorColegiado, String cpfMagistrado, Integer idSituacao, JSONObject mov, Integer idFormato, Integer idGrau, Integer idTribunal) {
        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setIdProcesso(idProcesso);
        movimentacao.setIdClasse(idClasse);
        movimentacao.setIdFormato(idFormato);
        movimentacao.setIdGrau(idGrau);
        movimentacao.setIdTribunal(idTribunal);
        movimentacao.setIdAssunto(idAssunto);
        movimentacao.setIdOrgaoJulgador(idOrgaoJulgador);
        movimentacao.setIdOrgaoJulgadorColegiado(idOrgaoJulgadorColegiado);
        movimentacao.setCpfMagistrado(cpfMagistrado);
        movimentacao.setIdSituacao(idSituacao);
        movimentacao.setSituacao(dataCache.getSituacao(idSituacao));
        movimentacao.setIdentificadorMovimento(mov.has("identificadorMovimento") && !mov.isNull("identificadorMovimento") ? mov.getString("identificadorMovimento").toString() : null);
        movimentacao.setData(getIdData(mov.has("dataHora") ? mov.get("dataHora").toString() : null));
        movimentacao.setHorario(getHora(mov.has("dataHora") ? mov.get("dataHora").toString() : null));
        movimentacao.setIdMovimento(getCodigoMovimentoNacional(mov));
        movimentacao.setAnulado(false);
        movimentacao.setCancelado(false);
        String verificarComplemento = getComplementosString(mov);
        //Verifica se o processo foi remetido ao TRF6 pelo TRF1 em virtude da contabilização errada de produtividade na divisão do tribunal em dois
        if(processoTRF1paraTRF6) {
        	if(verificarComplemento != null && verificarComplemento.equals("18:motivo_da_remessa:90")) {
        		verificarComplemento = verificarComplemento.replaceAll("18:motivo_da_remessa:90", "18:motivo_da_remessa:367");
        	}
            //Cancela movimentação de baixado definitivamente no caso de processo migrado do TRF1 para o TRF6 devido a divisão do tribunal
            if(movimentacao.getIdMovimento() == 22 || (movimentacao.getIdMovimento() == 123 && verificarComplemento.equals("18:motivo_da_remessa:90"))) {
            	movimentacao.setCancelado(true);
            }
        }
        movimentacao.setComplemento(verificarComplemento);
        return movimentacao;
    }

    /**
     * Método que gera as moviemntações artificiais para controle do processo
     * @param processo Processo em questão
     * @param movimentacoes Lista de movimentações
     * @return Lista atualizada de movimentações
     */
    private List<Movimentacao> atualizarSituacaoMovimentos(Processo processo, List<Movimentacao> movimentacoes) {
    	final Integer ID_SITUACAO_REMETIDO_PARA_CEJUSC = 118;
		final Integer ID_SITUACAO_REMETIDO_PARA_CAMARA = 130;
		final Integer ID_SITUACAO_REMETIDO_PELO_CEJUSC = 153;
		final Integer ID_SITUACAO_MIGRADO_SISTEMA = 155;
		List<Integer> casos = new ArrayList<>();
		casos.add(ID_SITUACAO_REMETIDO_PARA_CEJUSC);
		casos.add(ID_SITUACAO_REMETIDO_PARA_CAMARA);
		casos.add(ID_SITUACAO_REMETIDO_PELO_CEJUSC);
		
        List<Movimentacao> movimentacaoList = null;
		List<Movimentacao> novaMovimentacaoList = new ArrayList<Movimentacao>(0);
		//boolean reprocessarMovimentacao = false;
        if (movimentacoes == null || movimentacoes.size() == 0) {
            movimentacaoList = movimentacoes;
        } else {
        	if(!processo.getRegistroUnico()) {
        		movimentacaoList = movimentacaoRepository.findByIdProcesso(processo.getId(), processo.getIdTribunal());
	            Collections.sort(movimentacaoList);
	            for(Movimentacao mov : movimentacaoList) {
	            	Situacao s = dataCache.getSituacao(mov.getIdSituacao());
	            	if(mov.getIdSituacaoIniciar() == 0) {
	                    mov.setIdSituacaoFinalizar(NAO_INFORMADO);
	                    mov.setDataFimSituacao(calcularFimVigenciaSituacao(s, mov.getData()));
	                    mov.setPersistir(true);
	                    novaMovimentacaoList.add(mov);
	            	}
	            }
            }
            //Para cada movimentação define a situação de acordo
			for (Movimentacao mov : movimentacoes) {
                Situacao s = dataCache.getSituacao(mov.getIdSituacao());
                if (s != null) {
                    boolean movExistente = false;
                    mov.setSituacao(s);
                    mov.setIdSituacaoIniciar(NAO_INFORMADO);
                    mov.setIdSituacaoFinalizar(NAO_INFORMADO);
                    mov.setDataInicioSituacao(mov.getData());
                    mov.setDataFimSituacao(calcularFimVigenciaSituacao(s, mov.getData()));
                    mov.setPersistir(Boolean.TRUE);
                    for (Movimentacao m : movimentacaoList) {
                    	if(m.equals(mov)) {
                    		if(m.getIdFase().intValue() != mov.getIdFase().intValue() || m.getIdOrgaoJulgador().intValue() != mov.getIdOrgaoJulgador().intValue() || m.getIdOrgaoJulgadorColegiado().intValue() != mov.getIdOrgaoJulgadorColegiado().intValue() || (m.getIdClasse().intValue() != mov.getIdClasse().intValue() && mudancaClasse)) {
                    			if(!movExistente) {
	                    			mov.setId(m.getId());
	                    			novaMovimentacaoList.set(novaMovimentacaoList.indexOf(m), mov);
                    			}
                    		}
                			movExistente = true;
                			break;
                        }
                    }
                    if (!movExistente) {
                        novaMovimentacaoList.add(mov);
                    }
                }
            }
            if (novaMovimentacaoList.size() > 0) {
            	//Reinicia os arrays do processo
				processo.setIdFaseProcessual(new ArrayList<Integer>(0));
				processo.setIdClasse(new ArrayList<Integer>(0));
				processo.setIdAssunto(new ArrayList<Integer>(0));
				processo.setIdOrgaoJulgador(new ArrayList<Integer>(0));
				processo.setIdOrgaoJulgadorColegiado(new ArrayList<Integer>(0));

				//Iguala as listas de novas movimentações e movimentações existentes
				Collections.sort(novaMovimentacaoList);
            	movimentacaoList = new ArrayList<Movimentacao>(novaMovimentacaoList);
            	Integer fase = novaMovimentacaoList.get(0).getIdFase();
            	Integer tipoProcedimento = novaMovimentacaoList.get(0).getIdTipoProcedimento();
            	//Integer orgaoJulgador = novaMovimentacaoList.get(0).getIdOrgaoJulgador();
                for (Movimentacao m : novaMovimentacaoList) {
                    Situacao s = dataCache.getSituacao(m.getIdSituacao());
                    //Se a movimentação não foi cancelada
                    if(!m.getCancelado()) {
                    	//Finaliza toda situação que ela pode
	                    finalizarSituacaoMovimentos(m, s, movimentacaoList);
	                    //Inicia toda situação que pode considerando a exceção do Recebido pelo Tribunal
	                    if(s.getId() != 61 || !estaPendenteTramitando(m, movimentacaoList)) {
	                    	iniciarSituacaoMovimentos(m, s, movimentacaoList);
	                    }
	                    //Trata exceções
	                    if(casos.contains(m.getIdSituacao())) {
	                    	movimentacaoList = executarSituacaoMovimentos(m, movimentacaoList);
	                    }
	                    if(ID_SITUACAO_MIGRADO_SISTEMA == m.getIdSituacao()) {
	                    	movimentacaoList = executarSituacaoMovimentosMigracao(m, movimentacaoList);
	                    }
	                    if(m.getIdSituacao() == 38 && (m.getIdGrau() == 1 || m.getIdGrau() == 3)) { 
	                    	movimentacaoList = executarExcecaoSituacaoMovimentos(m, movimentacaoList);
	                    }
	                    if(m.getIdSituacao() == 40) {
	                    	movimentacaoList = executarRegraRedistribuicao(m,movimentacaoList);
	                    }
	                    if(!fase.equals(m.getIdFase())) {
	                    	movimentacaoList = executarRegraAlteracaoFase(m,movimentacaoList);
	                    }
	                    if(fase.equals(m.getIdFase()) && !tipoProcedimento.equals(m.getIdTipoProcedimento())) {
	                    	movimentacaoList = executarRegraAlteracaoTipoProcedimento(m,movimentacaoList);
	                    	tipoProcedimento = m.getIdTipoProcedimento();
	                    }
                    }
                }
                novaMovimentacaoList = new ArrayList<Movimentacao>();
                for (Movimentacao mov : movimentacaoList) {
                    if (mov.getPersistir()) {
                    	if(!novaMovimentacaoList.contains(mov)) {
                    		novaMovimentacaoList.add(mov);
                    	}
                    }
                }                
                Collections.sort(novaMovimentacaoList);
            }
        }
        return novaMovimentacaoList;
    }
    
    private boolean estaPendenteTramitando(Movimentacao movimentacao, List<Movimentacao> movimentacoes) {
    	String opcoesPendenteTramitando = ",25,88,";
    	Optional<Movimentacao> movPendenteTramitando = movimentacoes.stream().filter(m -> (opcoesPendenteTramitando.contains(","+m.getIdSituacao()+",") && Misc.isDataMenor(m.getDataHora(), movimentacao.getDataHora()) && m.getDataFimSituacao() == 0)).findFirst();
    	if(movPendenteTramitando.isPresent()) {
    		return true;
    	}else {
    		return false;
    	}
    }
    
    /**
     * Verifica se existe alguma movimentação anterior que poderia alterar as situações subsequentes
     * @param movAtuais Lista de Movimentações atuais
     * @param movNovas Lista de Novas movimentações
     * @return se existe moviemntação retroativa ou não
     */
    /*private boolean verificarMovimentacaoRetroativa(List<Movimentacao> movAtuais, List<Movimentacao> movNovas) {
		boolean movRetroativa = false;
		if(movAtuais != null && movAtuais.size() > 0 && movNovas != null && movNovas.size() > 0) {
			Optional<Movimentacao> maxMovAtual = movAtuais.stream().max(Comparator.comparing(Movimentacao::getDataHora));
			if(maxMovAtual.isPresent()) {
				Optional<Movimentacao> minMovNova = movNovas.stream().min(Comparator.comparing(Movimentacao::getDataHora));
				if(Misc.isDataMenor(minMovNova.get().getDataHora(), maxMovAtual.get().getDataHora())) {
					movRetroativa = true;
				}
			}
		}
		return movRetroativa;
	}*/
    
    /**
     * Método que atualiza a situação atual a partir de redistribuição e recebimento em camara ou cejusc
     * @param movimentacoes lista de movimentações
     * @return Lista atualizada de movimentações
     */
    private List<Movimentacao> atualizarSituacoesAposRedistribuicao(List<Movimentacao> movimentacoes) {
		final Integer ID_SITUACAO_REDISTRIBUIDO = 40;
		final Integer ID_SITUACAO_RECEBIDO_DO_CEJUSC = 120;
		final Integer ID_SITUACAO_RECEBIDO_DA_CAMARA = 131;
		List<Integer> casos = new ArrayList<>();
		casos.add(ID_SITUACAO_REDISTRIBUIDO);
		casos.add(ID_SITUACAO_RECEBIDO_DO_CEJUSC);
		casos.add(ID_SITUACAO_RECEBIDO_DA_CAMARA);
		List<String> hashRedistribuidos = new ArrayList<String>(0);
		List<Movimentacao> redistribuidos = movimentacoes.stream().filter(m -> casos.contains(m.getIdSituacao())).collect(Collectors.toList());
		if(redistribuidos != null && redistribuidos.size() > 0) {
			for (Movimentacao movRedistribuido : redistribuidos) {
				String hash = Misc.encodeMD5(movRedistribuido.getIdSituacao().toString() + movRedistribuido.getData().toString() + movRedistribuido.getHorario());
				if(!hashRedistribuidos.contains(hash)) {
					hashRedistribuidos.add(hash);
					Date dataFinal = Misc.getDiaAnterior(Misc.stringToDate(movRedistribuido.getDataInicioSituacao().toString(), "yyyyMMdd"));
					Integer idDataFinal = dataCache.getIdTempo(dataFinal);
					List<Movimentacao> movimentosComSituacaoAberta = movimentacoes.stream()
							.filter(m -> m.getDataFimSituacao().equals(NAO_INFORMADO))
							.collect(Collectors.toList());
					for (Movimentacao movAberta : movimentosComSituacaoAberta) {
						if(Misc.isDataMenor(movAberta.getDataHora(), movRedistribuido.getDataHora())) {
							if(idDataFinal < movAberta.getDataInicioSituacao()) {
								movAberta.setDataFimSituacao(movRedistribuido.getDataInicioSituacao());
							}
							else {
								movAberta.setDataFimSituacao(idDataFinal);
							}
							movAberta.setIdSituacaoFinalizar(movRedistribuido.getIdSituacao());
							movAberta.setPersistir(Boolean.TRUE);
							movRedistribuido.setFinalizouSituacoes(Boolean.TRUE);
							
							Movimentacao novaMov = new Movimentacao();
							novaMov.setIdProcesso(movAberta.getIdProcesso());
							novaMov.setIdMovimento(movAberta.getIdMovimento());
							novaMov.setIdClasse(movAberta.getIdClasse());
							novaMov.setIdFormato(movAberta.getIdFormato());
							novaMov.setIdGrau(movAberta.getIdGrau());
							novaMov.setIdTribunal(movAberta.getIdTribunal());
							novaMov.setIdAssunto(movAberta.getIdAssunto());
							novaMov.setIdOrgaoJulgador(movRedistribuido.getIdOrgaoJulgador());
							novaMov.setIdOrgaoJulgadorColegiado(movRedistribuido.getIdOrgaoJulgadorColegiado());
							novaMov.setCpfMagistrado(movRedistribuido.getCpfMagistrado());
							novaMov.setIdSituacao(movAberta.getIdSituacao());
							novaMov.setSituacao(dataCache.getSituacao(movAberta.getIdSituacao()));
							novaMov.setIdentificadorMovimento(movAberta.getIdentificadorMovimento());
							novaMov.setIdSituacaoIniciar(movRedistribuido.getIdSituacao());
							novaMov.setIdSituacaoFinalizar(NAO_INFORMADO);
							novaMov.setData(movRedistribuido.getData());
							novaMov.setHorario(movRedistribuido.getHorario());
							novaMov.setDataInicioSituacao(movRedistribuido.getData());
							novaMov.setDataFimSituacao(NAO_INFORMADO);
							novaMov.setComplemento(movAberta.getComplemento());
							novaMov.setIdFase(movRedistribuido.getIdFase());
							novaMov.setIdTipoProcedimento(movRedistribuido.getIdTipoProcedimento());
							novaMov.setIdNatureza(movRedistribuido.getIdNatureza());
							novaMov.setCriminal(movRedistribuido.getCriminal());
							novaMov.setPersistir(Boolean.TRUE);
							novaMov.setAnulado(false);
							novaMov.setCancelado(false);
							movimentacoes.add(novaMov);
						}
					}
				}
			}
		}
		Set<Movimentacao> novaMovimentacaoSet = new HashSet<Movimentacao>(0);
        for (Movimentacao mov : movimentacoes) {
            if (mov.getPersistir()) {
                novaMovimentacaoSet.add(mov);
            }
        }
        movimentacoes = new ArrayList<Movimentacao>(novaMovimentacaoSet);
		return movimentacoes;
	}
    
    /**
     * Método que trata situação específica de migraçãod e sistema, como por exemplo um processo passa a tramitar no SEEU
     * @param movimentacao Movimentação Específica
     * @param movimentacoes Lista de Movimentações
     * @return Lista atualizada de movimentações
     */
    private List<Movimentacao> executarSituacaoMovimentosMigracao(Movimentacao movimentacao, List<Movimentacao> movimentacoes) {
    	List<Movimentacao> novaMovimentacaoList = new ArrayList<Movimentacao>(0);
		final Integer ID_SITUACAO_MIGRADO_SISTEMA = 155;
		List<Integer> conversao = new ArrayList<>();
		conversao.add(110);
		List<Integer> casos = new ArrayList<>();
		casos.add(ID_SITUACAO_MIGRADO_SISTEMA);
		List<Movimentacao> migrados = movimentacoes.stream().filter(m -> casos.contains(m.getIdSituacao())).collect(Collectors.toList());
		if(migrados != null && migrados.size() > 0) {
			boolean migrado = false;
			Movimentacao movAnterior = new Movimentacao();
			for (Movimentacao mov : movimentacoes) {
				novaMovimentacaoList.add(mov);
				if(movimentacao.equals(mov)) {
					movAnterior = mov;
					migrado = true;
				} else if(migrado && conversao.contains(mov.getIdSituacao())){
					Integer[] situacoesIniciar = {25,88};
					boolean existeSituacaoAtiva = false;
					Situacao situacao = dataCache.getSituacao(movAnterior.getIdSituacao());
					for(Integer i : situacoesIniciar) {
						if (movimentacoes != null) {
		                    for (Movimentacao movCheck : movimentacoes) {
		                        //Verifica se a situação a ser iniciada já consta na lista de movimentações
		                        if (movCheck.getIdSituacao().equals(i)) {
		                            //Veifica se a situação a ser iniciada que já consta na lista de movimentações encontra-se em aberto 
		                            if (movCheck.getDataFimSituacao().equals(NAO_INFORMADO)) {
		                                existeSituacaoAtiva = true;
		                                break;
		                            }
		                        }
		                    }
		                }
		                if (!existeSituacaoAtiva) {
		                    //Verifica se a situação pode ser aberta ou se há alguma outra situação em aberto que 
		                    //deve manter a nova situação fechada
		                    if (podeAbrirSituacao(i, movAnterior.getDataHora(), movimentacoes)) {
		                        //Inicia a situação caso sua inicialização não esteja condicionada ou, caso contrário,  
		                        //se a movimentação tiver finalizado alguma situação anteriormente
		                        if (!situacao.getInicializacaoCondicional() || movAnterior.getFinalizouSituacoes()) {
			                        Movimentacao novaMov = new Movimentacao();
			                        novaMov.setIdProcesso(movAnterior.getIdProcesso());
			                        novaMov.setIdMovimento(NAO_INFORMADO);
			                        novaMov.setIdClasse(movAnterior.getIdClasse());
			                        novaMov.setIdFormato(movAnterior.getIdFormato());
			                        novaMov.setIdGrau(movAnterior.getIdGrau());
			                        novaMov.setIdTribunal(movAnterior.getIdTribunal());
			                        novaMov.setIdAssunto(movAnterior.getIdAssunto());
			                        novaMov.setIdOrgaoJulgador(mov.getIdOrgaoJulgador());
			                        novaMov.setIdOrgaoJulgadorColegiado(mov.getIdOrgaoJulgadorColegiado());
			                        novaMov.setCpfMagistrado("");
			                        novaMov.setIdSituacao(i);
			                        novaMov.setSituacao(dataCache.getSituacao(i));
			                        novaMov.setIdentificadorMovimento(movAnterior.getIdentificadorMovimento());
			                        novaMov.setIdSituacaoIniciar(movAnterior.getIdSituacao());
			                        novaMov.setIdSituacaoFinalizar(NAO_INFORMADO);
			                        novaMov.setData(movAnterior.getData());
			                        novaMov.setHorario(movAnterior.getHorario());
			                        novaMov.setDataInicioSituacao(movAnterior.getData());
			                        novaMov.setDataFimSituacao(NAO_INFORMADO);
			                        novaMov.setComplemento("");
			                        novaMov.setIdFase(movAnterior.getIdFase());
			                        novaMov.setIdTipoProcedimento(movAnterior.getIdTipoProcedimento());
			                        novaMov.setIdNatureza(movAnterior.getIdNatureza());
			                        novaMov.setCriminal(movAnterior.getCriminal());
			                        novaMov.setAnulado(false);
			                        novaMov.setCancelado(false);
			                        novaMov.setPersistir(Boolean.TRUE);
			                        novaMovimentacaoList.add(novaMov);
		                        }
		                    }
		                }
					}
					migrado = false;
				}
			}
		}else {
			return movimentacoes;
		}
		return novaMovimentacaoList;
	}
    
    /**
     * Valida e trata uma movimentação específica que pode gerar novas situações para tratar envio incorreto pelos tribunais (Remessa para Camara ou Cejusc, ou Remessa pelo Cejusc), que na maioria dos casos não é informado recebimento
     * e o processo não contaria tempo de tramitação no respectivo órgão julgador sem este tratamento específico
     * @param movimentacao Movimentacao Especifica
     * @param movimentacoes Lisat de movimentações
     * @return Lista atualizada de movimentações
     */
    private List<Movimentacao> executarSituacaoMovimentos(Movimentacao movimentacao, List<Movimentacao> movimentacoes) {
    	List<Movimentacao> novaMovimentacaoList = new ArrayList<Movimentacao>(0);
    	final Integer ID_SITUACAO_REMETIDO_PARA_CEJUSC = 118;
		final Integer ID_SITUACAO_REMETIDO_PARA_CAMARA = 130;
		final Integer ID_SITUACAO_REMETIDO_PELO_CEJUSC = 153;
		List<Integer> cejusc = new ArrayList<>();
		cejusc.add(6);
		cejusc.add(7);
		cejusc.add(108);
		cejusc.add(303);
		List<Integer> casos = new ArrayList<>();
		casos.add(ID_SITUACAO_REMETIDO_PARA_CEJUSC);
		casos.add(ID_SITUACAO_REMETIDO_PARA_CAMARA);
		casos.add(ID_SITUACAO_REMETIDO_PELO_CEJUSC);
		final Integer ID_SITUACAO_RECEBIDO_DO_CEJUSC = 120;
		final Integer ID_SITUACAO_RECEBIDO_DA_CAMARA = 131;
		//boolean indoAoCejusc = false;
		//boolean vindoDoCejusc = false;
		List<Integer> situacoesProibidas = new ArrayList<>();
		situacoesProibidas.add(ID_SITUACAO_RECEBIDO_DO_CEJUSC);
		situacoesProibidas.add(ID_SITUACAO_RECEBIDO_DA_CAMARA);
		List<Movimentacao> enviados = movimentacoes.stream().filter(m -> casos.contains(m.getIdSituacao())).collect(Collectors.toList());
		//List<Movimentacao> proibidos = movimentacoes.stream().filter(m -> situacoesProibidas.contains(m.getIdSituacao())).collect(Collectors.toList());
		if(enviados != null && enviados.size() > 0) {
			boolean recebimento = false;
			boolean encontrouMovimento = false;
			boolean executouOcorrencia = false;
			boolean executarAgora = false;
			int pos = 0;
			Movimentacao movAnterior = new Movimentacao();
			for (Movimentacao mov : movimentacoes) {
				novaMovimentacaoList.add(mov);
				pos++;
				if(movimentacao.equals(mov)) {
					encontrouMovimento = true;
				}
				if(encontrouMovimento && !executouOcorrencia) {
					if(casos.contains(mov.getIdSituacao())) {
						recebimento = true;
						if(pos == movimentacoes.size()) {
							movAnterior = mov;
							executarAgora = true;
						}
						/*if(mov.getIdSituacao() == 153) {
							vindoDoCejusc = true;
						}else {
							indoAoCejusc = true;
						}*/
					}
					if(recebimento //&&  proibidos != null && proibidos.size() == 0 
							&& executarAgora) {//&& (mov.getIdOrgaoJulgador().intValue() != movAnterior.getIdOrgaoJulgador().intValue() || vindoDoCejusc)
							//((cejusc.contains(dataCache.getOrgaoJulgadorById(mov.getIdOrgaoJulgador()).getClassificacaoUnidJudiciaria()) && indoAoCejusc) ||
							//(cejusc.contains(dataCache.getOrgaoJulgadorById(movAnterior.getIdOrgaoJulgador()).getClassificacaoUnidJudiciaria()) && vindoDoCejusc))){
						Integer[] situacoesIniciar = {25,88};
						boolean existeSituacaoAtiva = false;
						Situacao situacao = dataCache.getSituacao(movAnterior.getIdSituacao());
						for(Integer i : situacoesIniciar) {
							if (movimentacoes != null) {
			                    for (Movimentacao movCheck : movimentacoes) {
			                        //Verifica se a situação a ser iniciada já consta na lista de movimentações
			                        if (movCheck.getIdSituacao().equals(i)) {
			                            //Veifica se a situação a ser iniciada que já consta na lista de movimentações encontra-se em aberto 
			                            if (movCheck.getDataFimSituacao().equals(NAO_INFORMADO)) {
			                                existeSituacaoAtiva = true;
			                                break;
			                            }
			                        }
			                    }
			                }
			                if (!existeSituacaoAtiva) {
			                    //Verifica se a situação pode ser aberta ou se há alguma outra situação em aberto que 
			                    //deve manter a nova situação fechada
			                    if (podeAbrirSituacao(i, movAnterior.getDataHora(), movimentacoes)) {
			                        //Inicia a situação caso sua inicialização não esteja condicionada ou, caso contrário,  
			                        //se a movimentação tiver finalizado alguma situação anteriormente
			                        if (!situacao.getInicializacaoCondicional() || movAnterior.getFinalizouSituacoes()) {
				                        Movimentacao novaMov = new Movimentacao();
				                        novaMov.setIdProcesso(mov.getIdProcesso());
				                        novaMov.setIdMovimento(NAO_INFORMADO);
				                        novaMov.setIdClasse(mov.getIdClasse());
				                        novaMov.setIdFormato(mov.getIdFormato());
				                        novaMov.setIdGrau(mov.getIdGrau());
				                        novaMov.setIdTribunal(mov.getIdTribunal());
				                        novaMov.setIdAssunto(mov.getIdAssunto());
				                        novaMov.setIdOrgaoJulgador(mov.getIdOrgaoJulgador());
				                        novaMov.setIdOrgaoJulgadorColegiado(mov.getIdOrgaoJulgadorColegiado());
				                        novaMov.setCpfMagistrado("");
				                        novaMov.setIdSituacao(i);
				                        novaMov.setSituacao(dataCache.getSituacao(i));
				                        novaMov.setIdentificadorMovimento(movAnterior.getIdentificadorMovimento());
				                        novaMov.setIdSituacaoIniciar(movAnterior.getIdSituacao());
				                        novaMov.setIdSituacaoFinalizar(NAO_INFORMADO);
				                        novaMov.setData(movAnterior.getData());
				                        novaMov.setHorario(movAnterior.getHorario());
				                        novaMov.setDataInicioSituacao(movAnterior.getData());
				                        novaMov.setDataFimSituacao(NAO_INFORMADO);
				                        novaMov.setComplemento("");
				                        novaMov.setIdFase(mov.getIdFase());
				                        novaMov.setIdTipoProcedimento(mov.getIdTipoProcedimento());
				                        novaMov.setIdNatureza(mov.getIdNatureza());
				                        novaMov.setCriminal(mov.getCriminal());
				                        novaMov.setAnulado(false);
				                        novaMov.setCancelado(false);
				                        novaMov.setPersistir(Boolean.TRUE);
				                        novaMovimentacaoList.add(novaMov);
			                        }
			                    }
			                }
						}
						recebimento = false;
						encontrouMovimento = false;
						executouOcorrencia = true;
						executarAgora = false;
						//indoAoCejusc = false;
						//vindoDoCejusc = false;
					}else {
						executarAgora = true;
					}
				}
				movAnterior = mov;
			}
		}else {
			return movimentacoes;
		}
		return novaMovimentacaoList;
	}
    
    /**
     * Valida e trata uma movimentação específica que pode gerar novas situações para tratar envio incorreto pelos tribunais (Recebido, imediatamente após o Remetido), 
     * para recontar o tempo de tramitação no respectivo órgão julgador sem este tratamento específico
     * @param movimentacao Movimentação especifica
     * @param movimentacoes Lista de movimentações
     * @return Lista atualizada de movimentações
     */
    private List<Movimentacao> executarExcecaoSituacaoMovimentos(Movimentacao movimentacao, List<Movimentacao> movimentacoes) {
    	List<Movimentacao> novaMovimentacaoList = new ArrayList<Movimentacao>(0);
		final Integer ID_SITUACAO_REMETIDO = 41;
		final Integer ID_SITUACAO_REMETIDO_INSTANCIA = 134;
		List<Integer> casos = new ArrayList<>();
		casos.add(ID_SITUACAO_REMETIDO);
		casos.add(ID_SITUACAO_REMETIDO_INSTANCIA);
		final Integer ID_SITUACAO_RECEBIDO = 38;
		List<Integer> situacoesPermitidas = new ArrayList<>();
		situacoesPermitidas.add(ID_SITUACAO_RECEBIDO);
		List<Movimentacao> enviados = movimentacoes.stream().filter(m -> casos.contains(m.getIdSituacao()) && Misc.isDataMenor(m.getDataHora(), movimentacao.getDataHora())).collect(Collectors.toList());
		int movUltimo = 0;
		if(enviados != null && enviados.size() > 0) {
			for (Movimentacao mov : movimentacoes) {
				novaMovimentacaoList.add(mov);
				if(movimentacao.equals(mov) && (movUltimo == 41 || movUltimo == 134)) {
					Date dataFinal = Misc.getDiaAnterior(Misc.stringToDate(mov.getDataInicioSituacao().toString(), "yyyyMMdd"));
	                Integer idDataFinal = dataCache.getIdTempo(dataFinal);
	                Movimentacao movRemessa = novaMovimentacaoList.get(novaMovimentacaoList.size()-2);
	                movRemessa.setDataFimSituacao(idDataFinal < movRemessa.getData() ? movRemessa.getData() : idDataFinal);
	                movRemessa.setIdSituacaoFinalizar(mov.getIdSituacao());
					Integer[] situacoesIniciar = {25,88};
					for(Integer i : situacoesIniciar) {								
	                        Movimentacao novaMov = new Movimentacao();
	                        novaMov.setIdProcesso(mov.getIdProcesso());
	                        novaMov.setIdMovimento(NAO_INFORMADO);
	                        novaMov.setIdClasse(mov.getIdClasse());
	                        novaMov.setIdFormato(mov.getIdFormato());
	                        novaMov.setIdGrau(mov.getIdGrau());
	                        novaMov.setIdTribunal(mov.getIdTribunal());
	                        novaMov.setIdAssunto(mov.getIdAssunto());
	                        novaMov.setIdOrgaoJulgador(mov.getIdOrgaoJulgador());
	                        novaMov.setIdOrgaoJulgadorColegiado(mov.getIdOrgaoJulgadorColegiado());
	                        novaMov.setCpfMagistrado("");
	                        novaMov.setIdSituacao(i);
	                        novaMov.setSituacao(dataCache.getSituacao(i));
	                        novaMov.setIdentificadorMovimento(mov.getIdentificadorMovimento());
	                        novaMov.setIdSituacaoIniciar(mov.getIdSituacao());
	                        novaMov.setIdSituacaoFinalizar(NAO_INFORMADO);
	                        novaMov.setData(mov.getData());
	                        novaMov.setHorario(mov.getHorario());
	                        novaMov.setDataInicioSituacao(mov.getData());
	                        novaMov.setDataFimSituacao(NAO_INFORMADO);
	                        novaMov.setComplemento("");
	                        novaMov.setIdFase(mov.getIdFase());
	                        novaMov.setIdTipoProcedimento(mov.getIdTipoProcedimento());
	                        novaMov.setIdNatureza(mov.getIdNatureza());
	                        novaMov.setCriminal(mov.getCriminal());
	                        novaMov.setAnulado(false);
	                        novaMov.setCancelado(false);
	                        novaMov.setPersistir(Boolean.TRUE);
	                        novaMovimentacaoList.add(novaMov);
					}
				}
				movUltimo = mov.getIdSituacao();
			}
		}else {
			return movimentacoes;
		}
		return novaMovimentacaoList;
	}
    
    private List<Movimentacao> executarRegraRedistribuicao(Movimentacao movimentacao, List<Movimentacao> movimentacoes) {
    	List<Integer> casosSuspensao = new ArrayList<>();
		casosSuspensao.add(45);
		casosSuspensao.add(46);
		casosSuspensao.add(47);
		casosSuspensao.add(48);
		casosSuspensao.add(49);
		casosSuspensao.add(92);
		casosSuspensao.add(93);
		casosSuspensao.add(94);
		casosSuspensao.add(95);
		casosSuspensao.add(96);
		casosSuspensao.add(128);
		casosSuspensao.add(144);
		List<Integer> casosFinalizar = new ArrayList<>();
		casosFinalizar.add(76);
		casosFinalizar.add(5);
		casosFinalizar.add(7);
		casosFinalizar.add(80);
		casosFinalizar.add(12);
		casosFinalizar.add(68);
		casosFinalizar.add(66);
		casosFinalizar.add(67);
		casosFinalizar.add(69);
		casosFinalizar.add(87);
		casosFinalizar.add(43);
		casosFinalizar.add(57);
		casosFinalizar.add(55);
		casosFinalizar.add(59);
		casosFinalizar.add(149);
		casosFinalizar.add(152);
		List<Movimentacao> finalizar = movimentacoes.stream().filter(m -> (casosFinalizar.contains(m.getIdSituacao()) ||casosSuspensao.contains(m.getIdSituacao())) && Misc.isDataMenor(m.getDataHora(), movimentacao.getDataHora()) && movimentacao.getIdOrgaoJulgador() !=  m.getIdOrgaoJulgador() && m.getDataFimSituacao() == 0).collect(Collectors.toList());
		List<Movimentacao> suspender = movimentacoes.stream().filter(m -> casosSuspensao.contains(m.getIdSituacao()) && Misc.isDataMenor(m.getDataHora(), movimentacao.getDataHora()) && movimentacao.getIdOrgaoJulgador() !=  m.getIdOrgaoJulgador() && m.getDataFimSituacao() == 0).collect(Collectors.toList());
		Date dataFinal = Misc.getDiaAnterior(Misc.stringToDate(movimentacao.getDataInicioSituacao().toString(), "yyyyMMdd"));
		Integer idDataFinal = dataCache.getIdTempo(dataFinal);
		if(finalizar != null && finalizar.size() > 0) {
			for (Movimentacao mov : movimentacoes) {
				if(finalizar.contains(mov)) {
					mov.setDataFimSituacao(idDataFinal < mov.getData() ? movimentacao.getData() : idDataFinal);
					mov.setIdSituacaoFinalizar(movimentacao.getIdSituacao());
				}
			}
		}
		List<Movimentacao> novaMovimentacaoList = new ArrayList<Movimentacao>(movimentacoes);
		if(suspender != null && suspender.size() > 0) {
			for(Movimentacao i : suspender) {			
                Movimentacao novaMov = new Movimentacao();
                novaMov.setIdProcesso(movimentacao.getIdProcesso());
                novaMov.setIdMovimento(NAO_INFORMADO);
                novaMov.setIdClasse(movimentacao.getIdClasse());
                novaMov.setIdFormato(movimentacao.getIdFormato());
                novaMov.setIdGrau(movimentacao.getIdGrau());
                novaMov.setIdTribunal(movimentacao.getIdTribunal());
                novaMov.setIdAssunto(movimentacao.getIdAssunto());
                novaMov.setIdOrgaoJulgador(movimentacao.getIdOrgaoJulgador());
                novaMov.setIdOrgaoJulgadorColegiado(movimentacao.getIdOrgaoJulgadorColegiado());
                novaMov.setCpfMagistrado("");
                novaMov.setIdSituacao(i.getIdSituacao());
                novaMov.setSituacao(dataCache.getSituacao(i.getIdSituacao()));
                novaMov.setIdentificadorMovimento(movimentacao.getIdentificadorMovimento());
                novaMov.setIdSituacaoIniciar(movimentacao.getIdSituacao());
                novaMov.setIdSituacaoFinalizar(NAO_INFORMADO);
                novaMov.setData(movimentacao.getData());
                novaMov.setHorario(movimentacao.getHorario());
                novaMov.setDataInicioSituacao(movimentacao.getData());
                novaMov.setDataFimSituacao(NAO_INFORMADO);
                novaMov.setComplemento("");
                novaMov.setIdFase(movimentacao.getIdFase());
                novaMov.setIdTipoProcedimento(movimentacao.getIdTipoProcedimento());
                novaMov.setIdNatureza(movimentacao.getIdNatureza());
                novaMov.setCriminal(movimentacao.getCriminal());
                novaMov.setAnulado(false);
                novaMov.setCancelado(false);
                novaMov.setPersistir(Boolean.TRUE);
                novaMovimentacaoList.add(novaMov);
			}
		}else {
			return movimentacoes;
		}
		return novaMovimentacaoList;
	}
    
    private List<Movimentacao> executarRegraAlteracaoFase(Movimentacao movimentacao, List<Movimentacao> movimentacoes) {//m.getIdOrgaoJulgador().longValue() == movimentacao.getIdOrgaoJulgador().longValue() &&
		List<Movimentacao> redefinir = movimentacoes.stream().filter(m -> m.getIdFase().intValue() != movimentacao.getIdFase().intValue() && Misc.isDataMenorIgual(m.getDataHora(), movimentacao.getDataHora()) && m.getDataFimSituacao() == 0).collect(Collectors.toList());
		Date dataFinal = Misc.getDiaAnterior(Misc.stringToDate(movimentacao.getDataInicioSituacao().toString(), "yyyyMMdd"));
		Integer idDataFinal = dataCache.getIdTempo(dataFinal);
		if(redefinir != null && redefinir.size() > 0) {
			for (Movimentacao mov : movimentacoes) {
				if(redefinir.contains(mov)) {
					mov.setDataFimSituacao(idDataFinal < mov.getData() ? movimentacao.getData() : idDataFinal);
					mov.setIdSituacaoFinalizar(movimentacao.getIdSituacao());
				}
			}
		}
		List<Movimentacao> novaMovimentacaoList = new ArrayList<Movimentacao>(movimentacoes);
		if(redefinir != null && redefinir.size() > 0) {
			for(Movimentacao i : redefinir) {	
				Movimentacao novaMov = new Movimentacao();
                novaMov.setIdProcesso(movimentacao.getIdProcesso());
                novaMov.setIdMovimento(NAO_INFORMADO);
                novaMov.setIdClasse(movimentacao.getIdClasse());
                novaMov.setIdFormato(movimentacao.getIdFormato());
                novaMov.setIdGrau(movimentacao.getIdGrau());
                novaMov.setIdTribunal(movimentacao.getIdTribunal());
                novaMov.setIdAssunto(movimentacao.getIdAssunto());
                novaMov.setIdOrgaoJulgador(movimentacao.getIdOrgaoJulgador());
                novaMov.setIdOrgaoJulgadorColegiado(movimentacao.getIdOrgaoJulgadorColegiado());
                novaMov.setCpfMagistrado("");
                novaMov.setIdSituacao(i.getIdSituacao());
                novaMov.setSituacao(dataCache.getSituacao(i.getIdSituacao()));
                novaMov.setIdentificadorMovimento(movimentacao.getIdentificadorMovimento());
                novaMov.setIdSituacaoIniciar(movimentacao.getIdSituacao());
                novaMov.setIdSituacaoFinalizar(NAO_INFORMADO);
                novaMov.setData(movimentacao.getData());
                novaMov.setHorario(movimentacao.getHorario());
                novaMov.setDataInicioSituacao(movimentacao.getData());
                novaMov.setDataFimSituacao(NAO_INFORMADO);
                novaMov.setComplemento("");
                novaMov.setIdFase(movimentacao.getIdFase());
                novaMov.setIdTipoProcedimento(movimentacao.getIdTipoProcedimento());
                novaMov.setIdNatureza(movimentacao.getIdNatureza());
                novaMov.setCriminal(movimentacao.getCriminal());
                novaMov.setAnulado(false);
                novaMov.setCancelado(false);
                novaMov.setPersistir(Boolean.TRUE);
                novaMovimentacaoList.add(novaMov);
			}
		}else {
			return movimentacoes;
		}
		return novaMovimentacaoList;
	}

    private List<Movimentacao> executarRegraAlteracaoTipoProcedimento(Movimentacao movimentacao, List<Movimentacao> movimentacoes) {//m.getIdOrgaoJulgador().longValue() == movimentacao.getIdOrgaoJulgador().longValue() &&
		List<Movimentacao> redefinir = movimentacoes.stream().filter(m -> m.getIdFase().intValue() == movimentacao.getIdFase().intValue() && m.getIdTipoProcedimento().intValue() != movimentacao.getIdTipoProcedimento().intValue() && Misc.isDataMenorIgual(m.getDataHora(), movimentacao.getDataHora()) && m.getDataFimSituacao() == 0).collect(Collectors.toList());
		Date dataFinal = Misc.getDiaAnterior(Misc.stringToDate(movimentacao.getDataInicioSituacao().toString(), "yyyyMMdd"));
		Integer idDataFinal = dataCache.getIdTempo(dataFinal);
		if(redefinir != null && redefinir.size() > 0) {
			for (Movimentacao mov : movimentacoes) {
				if(redefinir.contains(mov)) {
					mov.setDataFimSituacao(idDataFinal < mov.getData() ? movimentacao.getData() : idDataFinal);
					mov.setIdSituacaoFinalizar(movimentacao.getIdSituacao());
				}
			}
		}
		List<Movimentacao> novaMovimentacaoList = new ArrayList<Movimentacao>(movimentacoes);
		if(redefinir != null && redefinir.size() > 0) {
			for(Movimentacao i : redefinir) {	
				Movimentacao novaMov = new Movimentacao();
                novaMov.setIdProcesso(movimentacao.getIdProcesso());
                novaMov.setIdMovimento(NAO_INFORMADO);
                novaMov.setIdClasse(movimentacao.getIdClasse());
                novaMov.setIdFormato(movimentacao.getIdFormato());
                novaMov.setIdGrau(movimentacao.getIdGrau());
                novaMov.setIdTribunal(movimentacao.getIdTribunal());
                novaMov.setIdAssunto(movimentacao.getIdAssunto());
                novaMov.setIdOrgaoJulgador(movimentacao.getIdOrgaoJulgador());
                novaMov.setIdOrgaoJulgadorColegiado(movimentacao.getIdOrgaoJulgadorColegiado());
                novaMov.setCpfMagistrado("");
                novaMov.setIdSituacao(i.getIdSituacao());
                novaMov.setSituacao(dataCache.getSituacao(i.getIdSituacao()));
                novaMov.setIdentificadorMovimento(movimentacao.getIdentificadorMovimento());
                novaMov.setIdSituacaoIniciar(movimentacao.getIdSituacao());
                novaMov.setIdSituacaoFinalizar(NAO_INFORMADO);
                novaMov.setData(movimentacao.getData());
                novaMov.setHorario(movimentacao.getHorario());
                novaMov.setDataInicioSituacao(movimentacao.getData());
                novaMov.setDataFimSituacao(NAO_INFORMADO);
                novaMov.setComplemento("");
                novaMov.setIdFase(movimentacao.getIdFase());
                novaMov.setIdTipoProcedimento(movimentacao.getIdTipoProcedimento());
                novaMov.setIdNatureza(movimentacao.getIdNatureza());
                novaMov.setCriminal(movimentacao.getCriminal());
                novaMov.setAnulado(false);
                novaMov.setCancelado(false);
                novaMov.setPersistir(Boolean.TRUE);
                novaMovimentacaoList.add(novaMov);
			}
		}else {
			return movimentacoes;
		}
		return novaMovimentacaoList;
	}
    
    /**
     * Calcula as fases processuais associadas as cada movimentação com base na classe, situações específicas e fases divergentes
     * @param processo Processo Atual
     * @param movimentacoes Lista de movimentações
     * @return Lista atualizada de movimentações
     */
    private List<Movimentacao> calcularFasesProcessuais(Processo processo, List<Movimentacao> movimentacoes) {
        movimentacoes = calcularFasesProcessuaisPorClasse(movimentacoes);
        movimentacoes = normalizarSituacoesIniciaisFasesProcessuais(processo, movimentacoes);
        movimentacoes = normalizarFasesDivergentesEntreSituacoesClasses(movimentacoes);
        movimentacoes = normalizarClassesMovimentoDiferenteOriginal(movimentacoes);
        movimentacoes = normalizarFasesPorSituacaoEvoluida(movimentacoes);
        return movimentacoes;
    }
    
    /**
     * Atualiza os movimentações que tiveram alteração de classe e que as classes eram utilizadas para gerar situações
     * @param processo Processo Atual
     * @param movimentacoes Lista de movimentações
     * @return Lista atualizada de movimentações
     */
    private List<Movimentacao> normalizarClassesMovimentoDiferenteOriginal(List<Movimentacao> movList) {
        final Integer MOV_MUDANCA_CLASSE_SGT = 10966;
        final Integer MOV_EVOLUCAO_CLASSE_SGT = 14739;
        final Integer MOV_RETFICACAO_CLASSE_SGT = 14738;
        final Integer COD_COMPLEMENTO_CLASSE_ANTERIOR = 26;
        final Integer COD_COMPLEMENTO_CLASSE_NOVA = 27;
        String codClasseAnteriorSGT = null;
        String codClasseNovaSGT = null;

        //Busca os IDs de movimentos de mudança, evolução e retificação de classe
        Integer idMudancaClasse = dataCache.getIdMovimento(MOV_MUDANCA_CLASSE_SGT);
        Integer idEvolucaoClasse = dataCache.getIdMovimento(MOV_EVOLUCAO_CLASSE_SGT);
        Integer idRetificacaoClasse = dataCache.getIdMovimento(MOV_RETFICACAO_CLASSE_SGT);

        for (int i = 0; i < movList.size(); i++) {
            Movimentacao mov = movList.get(i);
	        if (idMudancaClasse.equals(mov.getIdMovimento()) || idEvolucaoClasse.equals(mov.getIdMovimento()) || idRetificacaoClasse.equals(mov.getIdMovimento())) {
	            List<Complemento> complementoList = getComplementosTexto(mov.getComplemento());
	            if (complementoList != null && complementoList.size() > 0) {
	                for (Complemento c : complementoList) {
	                    if (COD_COMPLEMENTO_CLASSE_ANTERIOR.equals(c.getCodigo())) {
	                        codClasseAnteriorSGT = c.getValor();
	                    } else if (COD_COMPLEMENTO_CLASSE_NOVA.equals(c.getCodigo())) {
	                        codClasseNovaSGT = c.getValor();
	                    }
	                }
	                if(codClasseAnteriorSGT != null && codClasseNovaSGT != null) {
	                	if(isClasseValida(codClasseNovaSGT) && !mov.getIdClasse().equals(Integer.parseInt(codClasseNovaSGT))) {
	                        mov.setCancelado(true);; 
	                    }
	                }
	            }
	        }
        }
        return movList;
    }

    /**
     * Método que calcula a fase processual a partir da situação evoluída garantindo a priorização pela situação e não pela classe
     * @param movimentacoes Lista de Movimentações
     * @return Lista atualizada de movimentações
     */
    private List<Movimentacao> normalizarFasesPorSituacaoEvoluida(List<Movimentacao> movimentacoes) {
    	int fase = movimentacoes.size()>0?movimentacoes.get(0).getIdFase():0;
        for (int i = 0; i < movimentacoes.size(); i++) {
            Movimentacao mov = movimentacoes.get(i);
            Situacao sit = mov.getSituacao();
            //Contabilizar apenas fase de Conhecimento e Execução
            if(!mov.getCancelado() && !mov.getAnulado() && sit.getIdFase() > 0 && sit.getIdFase() < 3) {
            	fase = sit.getIdFase();
            }
            if (!mov.getIdFase().equals(fase)) {
               mov.setIdFase(fase);
            }
        }
        return movimentacoes;
    }
    
    /**
     * Método que calcula a fase processual a partir da classe associada a movimentação
     * @param movimentacoes Lista de Movimentações
     * @return Lista atualizada de movimentações
     */
    private List<Movimentacao> calcularFasesProcessuaisPorClasse(List<Movimentacao> movimentacoes) {
        for (Movimentacao mov : movimentacoes) {
            Classe c = dataCache.getClasseById(mov.getIdClasse());
            if (c != null) {
                mov.setIdFase(c.getIdFase());
                mov.setIdTipoProcedimento(c.getIdTipoProcedimento());
                mov.setIdNatureza(c.getIdNatureza());
                if (c.getCriminal() == null) {
                    mov.setCriminal(isAssuntoCriminal(mov.getIdAssunto()));
                } else {
                    mov.setCriminal(c.getCriminal());
                }
            }
        }
        return movimentacoes;
    }
    
    /**
     * Verifica se um dos assuntos é criminal para o que processo seja considerado criminal
     * @param assuntos Lista de assuntos do processo
     * @return Se é criminal ou não
     */
    private boolean isAssuntoCriminal(List<Integer> assuntos) {
		boolean isCriminal = false;
		if(assuntos != null && assuntos.size() > 0) {
			for (Integer id : assuntos) {
				Assunto a = dataCache.getAssuntoById(id);
				if(a != null && a.getCriminal() != null && a.getCriminal()) {
					isCriminal = true;
					break;
				}
			}
		}
		return isCriminal;
	}
    
    /**
     * Método que associa específicas situações a um fase processual
     * @param processo Processo em questão
     * @param movimentacoes Lista de Movimentações
     * @return Lista atualizada de movimentações
     */
    private List<Movimentacao> normalizarSituacoesIniciaisFasesProcessuais(Processo processo, List<Movimentacao> movimentacoes) {
		final Integer SITUACAO_DENUNCIA_QUEIXA_RECEBIDA = 9;
		final Integer SITUACAO_CLASSE_EVOLUIDA_PARA_ACAO_PENAL = 81;
		final Integer SITUACAO_EXECUCAO_NAO_CRIMINAL_INICIADA = 26;
		final Integer SITUACAO_LIQUIDACAO_INICIADA = 91;
		
		Map<ProcedimentoFaseEnum, List<Movimentacao>> movimentacoesMap = getMovimentacoesPorProcedimentoFase(movimentacoes);
		for (ProcedimentoFaseEnum key : movimentacoesMap.keySet()) {
			List<Movimentacao> movimentacaoList = movimentacoesMap.get(key);
			
			if(key.equals(ProcedimentoFaseEnum.CONHECIMENTO_CRIMINAL)) {
				movimentacoes.addAll(normalizaMovimentacaoInicialFase(processo, movimentacaoList, new ArrayList<Integer>(Arrays.asList(SITUACAO_DENUNCIA_QUEIXA_RECEBIDA, SITUACAO_CLASSE_EVOLUIDA_PARA_ACAO_PENAL))));
			}
			else if(key.equals(ProcedimentoFaseEnum.EXECUCAO_JUDICIAL_CIVEL)) {
				movimentacoes.addAll(normalizaMovimentacaoInicialFase(processo, movimentacaoList, new ArrayList<Integer>(Arrays.asList(SITUACAO_EXECUCAO_NAO_CRIMINAL_INICIADA))));
			}
			else if(key.equals(ProcedimentoFaseEnum.LIQUIDACAO_JUDICIAL_CIVEL)) {
				movimentacoes.addAll(normalizaMovimentacaoInicialFase(processo, movimentacaoList, new ArrayList<Integer>(Arrays.asList(SITUACAO_LIQUIDACAO_INICIADA, SITUACAO_EXECUCAO_NAO_CRIMINAL_INICIADA))));
			}
			else {
				movimentacoes.addAll(normalizaMovimentacaoInicialFase(processo, movimentacaoList, new ArrayList<Integer>(0)));
			}
		}
		Collections.sort(movimentacoes);
		return movimentacoes;
	}

    /**
     * Método que trata exceção de redistribuição por Incompetência
     * @param processo Processo em Questão
     * @param movimentacoesFase Lista de Movimentações
     * @return Movimentação de redistribuição por incompetência
     */
    private Optional<Movimentacao> iniciarSituacaoRedistribuicaoPorIncompetencia(Processo processo, List<Movimentacao> movimentacoesFase) {
        final Integer SITUACAO_DISTRIBUIDO = 24;
        final Integer SITUACAO_REDISTRIBUIDO = 40;
        final Integer SITUACAO_RECEBIDO = 38;
        final Integer SEGUNDO_GRAU = 2;
        final Integer SUPERIOR = 5;

        Optional<Movimentacao> movRedistribuicaoPorIncompetencia = Optional.empty();
        Situacao sitRedistribuicaoPorIncompetencia = null;

        List<Integer> situacoesIniciais = new ArrayList<Integer>(Arrays.asList(SITUACAO_REDISTRIBUIDO));

        if (processo.getIdGrau().equals(SEGUNDO_GRAU) || processo.getIdGrau().equals(SUPERIOR)) {
            situacoesIniciais.addAll(Arrays.asList(SITUACAO_RECEBIDO, SITUACAO_DISTRIBUIDO));
        } else {
            situacoesIniciais.addAll(Arrays.asList(SITUACAO_DISTRIBUIDO));
        }
        Optional<Movimentacao> movInicioFase = movimentacoesFase.stream().filter(m -> situacoesIniciais.contains(m.getIdSituacao())).findFirst();
        if (movInicioFase.isPresent()) {
            Movimentacao mov = movInicioFase.get();
            if (SITUACAO_REDISTRIBUIDO.equals(mov.getIdSituacao())) {
                if (isMovimentacaoRedistribuicaoPorIncompetencia(mov)) {
                    sitRedistribuicaoPorIncompetencia = dataCache.getSituacao(SITUACAO_REDISTRIBUIDO);
                    mov.setIdSituacao(sitRedistribuicaoPorIncompetencia.getId());
                    mov.setSituacao(sitRedistribuicaoPorIncompetencia);
                    movRedistribuicaoPorIncompetencia = Optional.of(mov);
                }
            }
        }
        return movRedistribuicaoPorIncompetencia;
    }

    /**
     * Método que define uma situação inicial para caso que os movimentos informados não o estabeleçam 
     * @param processo Processo em questão
     * @param movimentacoesFase Lista de Movimentações
     * @param idSituacoesIniciais Lista de identificadores de Situaçoes Iniciais
     * @return Lista atualiza de movimentações
     */
    private List<Movimentacao> normalizaMovimentacaoInicialFase(Processo processo, List<Movimentacao> movimentacoesFase, List<Integer> idSituacoesIniciais) {
        final Integer SITUACAO_DISTRIBUIDO = 24;
        final Integer SITUACAO_FASE_PROCESSUAL_INICIADA = 65;
        final Integer SITUACAO_RECEBIDO = 38;
        final Integer SITUACAO_RECEBIDO_TRIBUNAL = 61;
        final Integer SEGUNDO_GRAU = 2;
        final Integer SUPERIOR = 5;

        List<Movimentacao> novasMovimentacoes = new ArrayList<Movimentacao>(0);
        Movimentacao primeiraMovimentacao = movimentacoesFase.get(0);
        
        if (processo.getIdFaseProcessual().contains(primeiraMovimentacao.getIdFase()) && !processo.getRegistroUnico()) {
            return novasMovimentacoes;
        }
    
        Optional<Movimentacao> movInicioFase = iniciarSituacaoRedistribuicaoPorIncompetencia(processo, movimentacoesFase);

        if (!movInicioFase.isPresent()) {
            Integer tipoSituacaoArtificial = SITUACAO_FASE_PROCESSUAL_INICIADA;
            //No tribunal (G2 ou SUP) considera as situações de recebido e distribuído, a que aparecer primeiro
            if (processo.getIdGrau().equals(SEGUNDO_GRAU) || processo.getIdGrau().equals(SUPERIOR)) {
                List<Integer> situacoesIniciaisTribunal = new ArrayList<Integer>(Arrays.asList(SITUACAO_RECEBIDO, SITUACAO_DISTRIBUIDO));
                movInicioFase = movimentacoesFase.stream().filter(m -> situacoesIniciaisTribunal.contains(m.getIdSituacao())).findFirst();
                if (movInicioFase.isPresent() && movInicioFase.get().getIdSituacao().equals(SITUACAO_RECEBIDO)) {
                    tipoSituacaoArtificial = SITUACAO_RECEBIDO_TRIBUNAL;
                    movInicioFase = Optional.empty();
                }
            } else {
                movInicioFase = movimentacoesFase.stream().filter(m -> idSituacoesIniciais.contains(m.getIdSituacao())).findFirst();
                if (!movInicioFase.isPresent()) {
                    movInicioFase = movimentacoesFase.stream().filter(m -> m.getIdSituacao().equals(SITUACAO_DISTRIBUIDO)).findFirst();
                }
            }
            if (!movInicioFase.isPresent()) {
                Situacao situacao = dataCache.getSituacao(tipoSituacaoArtificial);
                Movimentacao novaMov = new Movimentacao();
                novaMov.setIdProcesso(processo.getId());
                novaMov.setIdMovimento(NAO_INFORMADO);
                novaMov.setIdClasse(primeiraMovimentacao.getIdClasse());
                novaMov.setIdFormato(primeiraMovimentacao.getIdFormato());
                novaMov.setIdGrau(primeiraMovimentacao.getIdGrau());
                novaMov.setIdTribunal(primeiraMovimentacao.getIdTribunal());
                novaMov.setIdAssunto(primeiraMovimentacao.getIdAssunto());
                novaMov.setIdOrgaoJulgador(primeiraMovimentacao.getIdOrgaoJulgador());
                novaMov.setIdOrgaoJulgadorColegiado(primeiraMovimentacao.getIdOrgaoJulgadorColegiado());
                novaMov.setCpfMagistrado(primeiraMovimentacao.getCpfMagistrado());
                novaMov.setIdSituacao(situacao.getId());
                novaMov.setSituacao(situacao);
                novaMov.setIdSituacaoIniciar(NAO_INFORMADO);
                novaMov.setIdSituacaoFinalizar(NAO_INFORMADO);
                novaMov.setData(primeiraMovimentacao.getData());
                novaMov.setHorario(primeiraMovimentacao.getHorario());
                novaMov.setDataInicioSituacao(primeiraMovimentacao.getData());
                novaMov.setDataFimSituacao(primeiraMovimentacao.getData());
                novaMov.setComplemento("");
                novaMov.setIdFase(primeiraMovimentacao.getIdFase());
                novaMov.setIdTipoProcedimento(primeiraMovimentacao.getIdTipoProcedimento());
                novaMov.setIdNatureza(primeiraMovimentacao.getIdNatureza());
                novaMov.setCriminal(primeiraMovimentacao.getCriminal());
                novaMov.setIdentificadorMovimento(NAO_INFORMADO.toString());
                novaMov.setAnulado(false);
                novaMov.setCancelado(false);
                novaMov.setPersistir(Boolean.TRUE);
                novasMovimentacoes.add(novaMov);
            }
        }
        return novasMovimentacoes;
    }

    /**
     * Identifica as fases do processo
     * @param movimentacoes Lista de movimentações
     * @return Todas as fases que o processo passou
     */
    private List<Integer> getFasesMovimentacao(List<Movimentacao> movimentacoes) {
        List<Integer> fases = new ArrayList<Integer>(0);
        Optional<Movimentacao> movInvestigatoria = movimentacoes.stream().filter(m -> m.getIdFase().equals(FASE_INVESTIGATORIA)).findFirst();
        Optional<Movimentacao> movConhecimento = movimentacoes.stream().filter(m -> m.getIdFase().equals(FASE_CONHECIMENTO)).findFirst();
        Optional<Movimentacao> movExecucao = movimentacoes.stream().filter(m -> m.getIdFase().equals(FASE_EXECUCAO)).findFirst();
        Optional<Movimentacao> movOutros = movimentacoes.stream().filter(m -> m.getIdFase().equals(FASE_OUTRA)).findFirst();

        if (movInvestigatoria.isPresent()) {
            fases.add(FASE_INVESTIGATORIA);
        }
        if (movConhecimento.isPresent()) {
            fases.add(FASE_CONHECIMENTO);
        }
        if (movExecucao.isPresent()) {
            fases.add(FASE_EXECUCAO);
        }
        if (movOutros.isPresent()) {
            fases.add(FASE_OUTRA);
        }
        return fases;
    }
    
    /**
     * Identifica as classes do processo
     * @param movimentacoes Lista de movimentações
     * @return Todas as classe que o processo teve
     */
    private List<Integer> getClassesMovimentacao(List<Movimentacao> movimentacoes) {
		List<Integer> classesSGT = new ArrayList<Integer>(0);
		Set<Integer> classes = movimentacoes.stream().map(Movimentacao::getIdClasse).collect(Collectors.toSet());
		
		if(classes.size() > 0) {
			for (Integer idClasse : classes) {
				Classe classe = dataCache.getClasseById(idClasse);
				if(classe != null && classe.getId().intValue() > 0) {
					classesSGT.add(classe.getId());
				}
			}
		}
		return classesSGT;
	}

    /**
     * Identifica os órgãos julgadores do processo
     * @param movimentacoes Lista de movimentações
     * @return Todos os órgãos julgadores que o processo passou
     */
	private List<Integer> getOrgaosJulgadoresMovimentacao(List<Movimentacao> movimentacoes) {
		List<Integer> orgaosJulgadoresSGT = new ArrayList<Integer>(0);
		Set<Integer> orgaosJulgadores = movimentacoes.stream().map(Movimentacao::getIdOrgaoJulgador).collect(Collectors.toSet());
		
		if(orgaosJulgadores.size() > 0) {
			for (Integer idOrgaoJulgador : orgaosJulgadores) {
				OrgaoJulgador oj = dataCache.getOrgaoJulgadorById(idOrgaoJulgador);
				if(oj != null && oj.getId().intValue() > 0) {
					orgaosJulgadoresSGT.add(oj.getId());
				}
			}
		}
		return orgaosJulgadoresSGT;
	}
	
	private List<Integer> getOrgaosJulgadoresColegiadoMovimentacao(List<Movimentacao> movimentacoes) {
		List<Integer> orgaosJulgadoresSGT = new ArrayList<Integer>(0);
		Set<Integer> orgaosJulgadores = movimentacoes.stream().map(Movimentacao::getIdOrgaoJulgadorColegiado).collect(Collectors.toSet());
		
		if(orgaosJulgadores.size() > 0) {
			for (Integer idOrgaoJulgador : orgaosJulgadores) {
				OrgaoJulgador oj = dataCache.getOrgaoJulgadorById(idOrgaoJulgador);
				if(oj != null && oj.getId().intValue() > 0) {
					orgaosJulgadoresSGT.add(oj.getId());
				}
			}
		}
		return orgaosJulgadoresSGT;
	}

	/**
     * Identifica os assuntos do processo
     * @param movimentacoes Lista de movimentações
     * @return Todos os assuntos que o processo teve
     */
	private List<Integer> getAssuntosMovimentacao(List<Movimentacao> movimentacoes) {
		if(movimentacoes.size() > 0) {
			Set<Integer> assuntosSGT = new HashSet<Integer>(0);
			Movimentacao mov = movimentacoes.stream().skip(movimentacoes.size() - 1).findFirst().get();
			List<Integer> assuntos = mov.getIdAssunto();
			if(assuntos.size() > 0) {
				for (Integer idAssunto : assuntos) {
					Integer idAssuntoSGT = dataCache.getIdAssunto(idAssunto);
					if(idAssuntoSGT != null && idAssuntoSGT.intValue() > 0) {
						assuntosSGT.add(idAssuntoSGT);
					}
				}
			}
			return new ArrayList<Integer>(assuntosSGT);
		}else {
			return new ArrayList<Integer>();
		}
	}

	/**
	 * Gera lista de movimentações por fase de uma listagem de processos
	 * @param movimentacoes Lista de Movimentações
	 * @return Map contendo fase e reespectivas movimentações
	 */
    private Map<ProcedimentoFaseEnum, List<Movimentacao>> getMovimentacoesPorProcedimentoFase(List<Movimentacao> movimentacoes) {
        Map<ProcedimentoFaseEnum, List<Movimentacao>> movimentacoesMap = new HashMap<ProcedimentoFaseEnum, List<Movimentacao>>(0);

        List<Movimentacao> movsInvestigatoria = movimentacoes.stream().filter(m -> m.getIdFase().equals(FASE_INVESTIGATORIA) && m.getIdTipoProcedimento().equals(PROCEDIMENTO_INVESTIGATORIO)).collect(Collectors.toList());
        List<Movimentacao> movsConhecimentoCivel = movimentacoes.stream().filter(m -> m.getIdFase().equals(FASE_CONHECIMENTO) && m.getIdTipoProcedimento().equals(PROCEDIMENTO_CONHECIMENTO) && m.getCriminal().equals(Boolean.FALSE)).collect(Collectors.toList());
        List<Movimentacao> movsConhecimentoCriminal = movimentacoes.stream().filter(m -> m.getIdFase().equals(FASE_CONHECIMENTO) && m.getIdTipoProcedimento().equals(PROCEDIMENTO_CONHECIMENTO) && m.getCriminal().equals(Boolean.TRUE)).collect(Collectors.toList());
        List<Movimentacao> movsConhecimentoFiscal = movimentacoes.stream().filter(m -> m.getIdFase().equals(FASE_CONHECIMENTO) && m.getIdTipoProcedimento().equals(PROCEDIMENTO_EXECUCAO_FISCAL)).collect(Collectors.toList());
        List<Movimentacao> movsConhecimentoExtraJudicial = movimentacoes.stream().filter(m -> m.getIdFase().equals(FASE_CONHECIMENTO) && m.getIdTipoProcedimento().equals(PROCEDIMENTO_EXECUCAO_EXTRAJUDICIAL)).collect(Collectors.toList());
        List<Movimentacao> movsExecucaoJudicialCivel = movimentacoes.stream().filter(m -> m.getIdFase().equals(FASE_EXECUCAO) && m.getIdTipoProcedimento().equals(PROCEDIMENTO_EXECUCAO_JUDICIAL) && m.getCriminal().equals(Boolean.FALSE) && !m.getIdNatureza().equals(NATUREZA_LIQUIDACAO)).collect(Collectors.toList());
        List<Movimentacao> movsLiquidacaoJudicialCivel = movimentacoes.stream().filter(m -> m.getIdFase().equals(FASE_EXECUCAO) && m.getIdTipoProcedimento().equals(PROCEDIMENTO_EXECUCAO_JUDICIAL) && m.getCriminal().equals(Boolean.FALSE) && m.getIdNatureza().equals(NATUREZA_LIQUIDACAO)).collect(Collectors.toList());
        List<Movimentacao> movsExecucaoJudicialCriminal = movimentacoes.stream().filter(m -> m.getIdFase().equals(FASE_EXECUCAO) && m.getIdTipoProcedimento().equals(PROCEDIMENTO_EXECUCAO_JUDICIAL) && m.getCriminal().equals(Boolean.TRUE)).collect(Collectors.toList());
        List<Movimentacao> movsExecucaoFiscal = movimentacoes.stream().filter(m -> m.getIdFase().equals(FASE_EXECUCAO) && m.getIdTipoProcedimento().equals(PROCEDIMENTO_EXECUCAO_FISCAL)).collect(Collectors.toList());
        List<Movimentacao> movsExecucaoExtraJudicial = movimentacoes.stream().filter(m -> m.getIdFase().equals(FASE_EXECUCAO) && m.getIdTipoProcedimento().equals(PROCEDIMENTO_EXECUCAO_EXTRAJUDICIAL)).collect(Collectors.toList());
        List<Movimentacao> movsOutra = movimentacoes.stream().filter(m -> m.getIdFase().equals(FASE_OUTRA)).collect(Collectors.toList());

        if (movsInvestigatoria != null && movsInvestigatoria.size() > 0) {
            movimentacoesMap.put(ProcedimentoFaseEnum.INVESTIGATORIO, movsInvestigatoria);
        }
        if (movsConhecimentoCivel != null && movsConhecimentoCivel.size() > 0) {
            movimentacoesMap.put(ProcedimentoFaseEnum.CONHECIMENTO_CIVEL, movsConhecimentoCivel);
        }
        if (movsConhecimentoCriminal != null && movsConhecimentoCriminal.size() > 0) {
            movimentacoesMap.put(ProcedimentoFaseEnum.CONHECIMENTO_CRIMINAL, movsConhecimentoCriminal);
        }
        if (movsConhecimentoFiscal != null && movsConhecimentoFiscal.size() > 0) {
            movimentacoesMap.put(ProcedimentoFaseEnum.CONHECIMENTO_FISCAL, movsConhecimentoFiscal);
        }
        if (movsConhecimentoExtraJudicial != null && movsConhecimentoExtraJudicial.size() > 0) {
            movimentacoesMap.put(ProcedimentoFaseEnum.CONHECIMENTO_EXTRAJUDICIAL, movsConhecimentoExtraJudicial);
        }
        if (movsExecucaoJudicialCivel != null && movsExecucaoJudicialCivel.size() > 0) {
            movimentacoesMap.put(ProcedimentoFaseEnum.EXECUCAO_JUDICIAL_CIVEL, movsExecucaoJudicialCivel);
        }
        if (movsLiquidacaoJudicialCivel != null && movsLiquidacaoJudicialCivel.size() > 0) {
            movimentacoesMap.put(ProcedimentoFaseEnum.LIQUIDACAO_JUDICIAL_CIVEL, movsLiquidacaoJudicialCivel);
        }
        if (movsExecucaoJudicialCriminal != null && movsExecucaoJudicialCriminal.size() > 0) {
            movimentacoesMap.put(ProcedimentoFaseEnum.EXECUCAO_JUDICIAL_CRIMINAL, movsExecucaoJudicialCriminal);
        }
        if (movsExecucaoFiscal != null && movsExecucaoFiscal.size() > 0) {
            movimentacoesMap.put(ProcedimentoFaseEnum.EXECUCAO_FISCAL, movsExecucaoFiscal);
        }
        if (movsExecucaoExtraJudicial != null && movsExecucaoExtraJudicial.size() > 0) {
            movimentacoesMap.put(ProcedimentoFaseEnum.EXECUCAO_EXTRAJUDICIAL, movsExecucaoExtraJudicial);
        }
        if (movsOutra != null && movsOutra.size() > 0) {
            movimentacoesMap.put(ProcedimentoFaseEnum.OUTRO, movsOutra);
        }

        return movimentacoesMap;
    }

    /**
     * Compara cada fase apontada pela classe e da situação da movimentação, e se forem diferentes assume a da classe
     * @param movimentacoes Lista de Movimentações
     * @return Lista atualizada de movimentações
     */
    private List<Movimentacao> normalizarFasesDivergentesEntreSituacoesClasses(List<Movimentacao> movimentacoes) {
        List<Situacao> situacoesFase = dataCache.getSituacoes().stream().filter(s -> s.getIdFase() > 0).collect(Collectors.toList());
        for (int i = 0; i < movimentacoes.size(); i++) {
            Movimentacao mov = movimentacoes.get(i);
            if (situacoesFase.contains(mov.getSituacao())) {
                Situacao sit = mov.getSituacao();
                Classe cla = dataCache.getClasseById(mov.getIdClasse());
                if (!sit.getIdFase().equals(cla.getIdFase())) {
                    List<Movimentacao> movList = movimentacoes.stream().skip(i).collect(Collectors.toList());
                    for (Movimentacao m : movList) {
                        if (m.getIdClasse().equals(cla.getId())) {
                            m.setIdFase(sit.getIdFase());
                        }
                    }
                }
            }
        }
        return movimentacoes;
    }
    
    /**
     * Método que define data de fim de toda situação que Inativa como a data de início e define a data de fim como Sem finalização (0) quando a situação é Ativa
     * @param situacao Situação a ser validada
     * @param dataInicioVigencia Data da situação
     * @return
     */
    private Integer calcularFimVigenciaSituacao(Situacao situacao, Integer dataInicioVigencia) {
        Integer dataFimVigencia = null;
        if (situacao.getTipoFinalizacao().equals("I")) {
            dataFimVigencia = dataInicioVigencia;
        }
        return dataFimVigencia == null ? NAO_INFORMADO : dataFimVigencia;
    }

    /**
     * Método que gera movimentações artificiais para controle da tramitação processual a partir da situação em questão, se a nova situação ainda não existir
     * @param movimentacao Movimentação em questão
     * @param situacao Situação em questão
     * @param movimentacaoList Lista de Movimentações
     */
    private void iniciarSituacaoMovimentos(Movimentacao movimentacao, Situacao situacao, List<Movimentacao> movimentacaoList) {
        boolean existeSituacaoAtiva = false;
        Integer[] situacoesIniciar = situacao.getIniciar();
        if (situacoesIniciar != null && situacoesIniciar.length > 0 && !situacoesIniciar[0].equals(NAO_INFORMADO)) {
            for (Integer ini : situacoesIniciar) {
                if (movimentacaoList != null) {
                    for (Movimentacao mov : movimentacaoList) {
                        //Verifica se a situação a ser iniciada já consta na lista de movimentações
                        if (mov.getIdSituacao().equals(ini)) {
                            //Veifica se a situação a ser iniciada que já consta na lista de movimentações encontra-se em aberto 
                            if (mov.getDataFimSituacao().equals(NAO_INFORMADO)) {
                                existeSituacaoAtiva = true;
                                break;
                            }
                        }
                    }
                }
                if (!existeSituacaoAtiva) {
                    //Verifica se a situação pode ser aberta ou se há alguma outra situação em aberto que 
                    //deve manter a nova situação fechada
                    if (podeAbrirSituacao(ini, movimentacao.getDataHora(), movimentacaoList)) {
                        //Inicia a situação caso sua inicialização não esteja condicionada ou, caso contrário,  
                        //se a movimentação tiver finalizado alguma situação anteriormente
                        if (!situacao.getInicializacaoCondicional() || movimentacao.getFinalizouSituacoes()) {
                            Movimentacao novaMov = new Movimentacao();
                            novaMov.setIdProcesso(movimentacao.getIdProcesso());
                            novaMov.setIdMovimento(NAO_INFORMADO);
                            novaMov.setIdClasse(movimentacao.getIdClasse());
                            novaMov.setIdFormato(movimentacao.getIdFormato());
                            novaMov.setIdGrau(movimentacao.getIdGrau());
                            novaMov.setIdTribunal(movimentacao.getIdTribunal());
                            novaMov.setIdAssunto(movimentacao.getIdAssunto());
                            novaMov.setIdOrgaoJulgador(movimentacao.getIdOrgaoJulgador());
                            novaMov.setIdOrgaoJulgadorColegiado(movimentacao.getIdOrgaoJulgadorColegiado());
                            novaMov.setCpfMagistrado("");
                            novaMov.setIdSituacao(ini);
                            novaMov.setSituacao(dataCache.getSituacao(ini));
                            novaMov.setIdentificadorMovimento(movimentacao.getIdentificadorMovimento());
                            novaMov.setIdSituacaoIniciar(situacao.getId());
                            novaMov.setIdSituacaoFinalizar(NAO_INFORMADO);
                            novaMov.setData(movimentacao.getData());
                            novaMov.setHorario(movimentacao.getHorario());
                            novaMov.setDataInicioSituacao(movimentacao.getData());
                            novaMov.setDataFimSituacao(NAO_INFORMADO);
                            novaMov.setComplemento("");
                            novaMov.setIdFase(movimentacao.getIdFase());
                            novaMov.setIdTipoProcedimento(movimentacao.getIdTipoProcedimento());
                            novaMov.setIdNatureza(movimentacao.getIdNatureza());
                            novaMov.setCriminal(movimentacao.getCriminal());
                            novaMov.setAnulado(false);
                            novaMov.setCancelado(false);
                            novaMov.setPersistir(Boolean.TRUE);
                            movimentacaoList.add(novaMov);
                        }
                    }
                }
                existeSituacaoAtiva = false;
            }
        }
    }

    /**
     * Método que Verifica se a situação pode ser aberta ou se há alguma outra situação em aberto que deve manter a nova situação fechada
     * @param idSituacao Identificador da Situação
     * @param dataHoraInicioSituacao Data e Hora do início da situação
     * @param movimentacoes Lista de Movimentações
     * @return Se pode ou não abrir a nova situação
     */
    private boolean podeAbrirSituacao(Integer idSituacao, Date dataHoraInicioSituacao, List<Movimentacao> movimentacoes) {
        for (Movimentacao mov : movimentacoes) {
            Situacao s = dataCache.getSituacao(mov.getIdSituacao());
            if (s.getFinalizar() != null && s.getFinalizar().length > 0 && !s.getFinalizar()[0].equals(NAO_INFORMADO)) {
                for (Integer idSituacaoFinalizar : s.getFinalizar()) {
                    if (idSituacaoFinalizar.equals(idSituacao) && Misc.isDataMenorIgual(mov.getDataHora(), dataHoraInicioSituacao)) {
                        if (mov.getDataFimSituacao().equals(0) || Misc.isDataMaior(mov.getDataHoraFimSituacao(), dataHoraInicioSituacao)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Método que finaliza situações associadas a movimentações para controle da tramitação processual a partir da situação em questão
     * @param movimentacao Movimentação em questão
     * @param situacao Situação em questão
     * @param movimentacaoList Lista de Movimentações
     */
    private void finalizarSituacaoMovimentos(Movimentacao movimentacao, Situacao situacao, List<Movimentacao> movimentacaoList) {
		Integer[] situacoesFinalizar = situacao.getFinalizar();
        Collections.sort(movimentacaoList);
        int pos = movimentacaoList.indexOf(movimentacao);
        if (situacoesFinalizar != null && situacoesFinalizar.length > 0 && !situacoesFinalizar[0].equals(NAO_INFORMADO)) {
            if (movimentacaoList != null) {
                Date dataFinal = Misc.getDiaAnterior(Misc.stringToDate(movimentacao.getDataInicioSituacao().toString(), "yyyyMMdd"));
                Integer idDataFinal = dataCache.getIdTempo(dataFinal);
                for (Integer f : situacoesFinalizar) {
                	int i = -1;
                    for (Movimentacao mov : movimentacaoList) {
                        i++;
                        //Continua apenas se: 
                        //1) a situação puder finalizar situações na mesma fase ou 
                        //2) se a fase da situação corrente for diferente da fase da situação a ser finalizada
                        //3) a movimentação for de distribuição por dependência
                        if (situacao.getFinalizaFaseAtual()
                            || !mov.getIdFase().equals(movimentacao.getIdFase())
                            || isMovimentacaoDistribuicaoDependencia(movimentacao)) {
                            if (mov.compareTo(movimentacao) < 1 && pos >= i) {
                                //Processa apenas se a situação na lista não for a mesma situação atual
                                if (!mov.equals(movimentacao)) {
                                    if (mov.getIdSituacao().equals(f)) {
                                        if (mov.getDataFimSituacao().equals(NAO_INFORMADO)) {
                                            if (idDataFinal < mov.getDataInicioSituacao()) {
                                                mov.setDataFimSituacao(mov.getDataInicioSituacao());
                                            } else {
                                                mov.setDataFimSituacao(idDataFinal);
                                            }
                                            mov.setIdSituacaoFinalizar(situacao.getId());
                                            mov.setPersistir(Boolean.TRUE);
                                            movimentacao.setFinalizouSituacoes(Boolean.TRUE);
                                        }
                                    }
                                }else {
                                	break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Persiste as movimentações no data mart
     * @param movimentacoes Lista de movimentações
     * @return Lista Atualizada de movimentações
     */
    private List<Movimentacao> salvarMovimentacoes(List<Movimentacao> movimentacoes, boolean registroUnico) {
        if (movimentacoes != null && movimentacoes.size() > 0) {
        	List<Movimentacao> movimentosExistentes = movimentacaoRepository.findByIdProcesso(movimentacoes.get(0).getIdProcesso(), movimentacoes.get(0).getIdTribunal());
        	Collections.sort(movimentosExistentes);
        	for(Movimentacao pNovo : movimentacoes) {
	        	for(Movimentacao pAntigo : movimentosExistentes) {
	        		if(pNovo.equals(pAntigo)) {
	            		pNovo.setId(pAntigo.getId());
	            		movimentosExistentes.remove(pAntigo);
	            		break;
	            	}
	            }
	        }
        	movimentosExistentes.forEach(pi -> {
        		if(pi.getIdSituacaoIniciar() != 0 || registroUnico) {
        			try {
        				movimentacaoRepository.delete(pi);
            		}catch(Exception e) {
            			System.out.println("Exclusão Movimentação "+e.toString());
            		}
        		}
            });        	
        	movimentacoes.forEach(pi -> {
        		try {
        			movimentacaoRepository.saveAndFlush(pi);
        		}catch(Exception e) {
        			System.out.println("Inserção Movimentação "+e.toString());
        		}
	        }); 
        }
        return movimentacoes;
    }

    private List<Movimentacao> getMovimentosComSituacaoVinculada(List<Movimentacao> movimentacoes) {
        List<Movimentacao> movList = new ArrayList<Movimentacao>(0);

        for (Movimentacao mov : movimentacoes) {
            Integer idMovSGT = dataCache.getIdMovimento(mov.getIdMovimento());
            if (idMovSGT != null && idMovSGT > 0) {
                Integer idSituacao = dataCache.getIdSituacao(idMovSGT, mov.getComplemento());
                if (idSituacao != null) {
                	idSituacao = idSituacao == 24 && (mov.getIdGrau() == 2 || mov.getIdGrau() == 5)? 61 : idSituacao;
                    mov.setIdSituacao(idSituacao);
                    mov.setSituacao(dataCache.getSituacao(idSituacao));
                    movList.add(mov);
                }
            }
        }
        return movList;
    }
    
    protected Integer getCodigoMovimentoNacionalSGT(JSONObject mov) {
        Integer codigo = null;
        if (mov.has("movimentoNacional") && !mov.isNull("movimentoNacional")) {
            JSONObject movimentoNacional = mov.getJSONObject("movimentoNacional");
            if (movimentoNacional.has("codigoNacional")) {
                codigo = movimentoNacional.getInt("codigoNacional");
            }
        } else if (mov.has("movimentoLocal") && !mov.isNull("movimentoLocal")) {
            JSONObject movimentoLocal = mov.getJSONObject("movimentoLocal");
            if (movimentoLocal.has("codigoPaiNacional")) {
                codigo = movimentoLocal.getInt("codigoPaiNacional");
            }
        }
        return codigo;
    }

    private Integer getCodigoMovimentoNacional(JSONObject mov) {
        Integer id = NAO_INFORMADO;
        Integer codigo = getCodigoMovimentoNacionalSGT(mov);
        if (codigo != null) {
            id = dataCache.getIdMovimento(codigo);
        }
        return id == null ? INVALIDO : id;
    }

    private String getComplementosString(JSONObject mov) {
        List<Complemento> complementos = getComplementos(mov);
        return getComplementosString(complementos);
    }

    private String getComplementosString(List<Complemento> complementos) {
        StringBuilder complementosStr = new StringBuilder();
        for (Complemento c : complementos) {
            if (complementosStr.length() > 0) {
                complementosStr.append(";");
            }
            complementosStr.append(c.toString());
        }
        return complementosStr.toString();
    }

    private List<Complemento> getComplementos(JSONObject mov) {
        Set<Complemento> complementos = new HashSet<Complemento>(0);
        JSONObject movimentoNacional = mov.has("movimentoNacional") && !mov.isNull("movimentoNacional") ? mov.getJSONObject("movimentoNacional") : null;
        complementos.addAll(getComplementosNacionais(mov.has("complementoNacional") ? mov.getJSONArray("complementoNacional") : null));
        complementos.addAll(getComplementosJson(movimentoNacional != null ? (movimentoNacional.has("complemento") ? movimentoNacional.getJSONArray("complemento") : null) : null));
        return complementos.stream().collect(Collectors.toList());
    }

    private List<Complemento> getComplementosNacionais(JSONArray complementos) {
        List<Complemento> complementoList = new ArrayList<Complemento>(0);
        if (complementos != null) {
            for (int i = 0; i < complementos.length(); i++) {
                JSONObject c = complementos.getJSONObject(i);
                if (c.has("codComplemento") && c.has("descricaoComplemento") && c.has("codComplementoTabelado")) {
                    Integer valor = c.getInt("codComplementoTabelado");
                    if (valor > 0) {
                        Complemento complemento = new Complemento();
                        complemento.setCodigo(c.getInt("codComplemento"));
                        complemento.setVariavel(c.getString("descricaoComplemento"));
                        complemento.setValor(String.valueOf(valor));
                        complementoList.add(complemento);
                    }
                }
            }
        }
        return complementoList;
    }

    private List<Complemento> getComplementosJson(JSONArray complementos) {
        List<Complemento> complementoList = new ArrayList<Complemento>(0);
        if (complementos != null) {
            for (int i = 0; i < complementos.length(); i++) {
                if (complementos.get(i) != null && !complementos.get(i).toString().trim().equalsIgnoreCase("null")) {
                    String c = complementos.getString(i);
                    List<Complemento> cList = getComplementosTexto(c);
                    if (cList != null && cList.size() > 0) {
                        complementoList.addAll(cList);
                    }
                }
            }
        }
        return complementoList;
    }

    private List<Complemento> getComplementosTexto(String complemento) {
        List<Complemento> complementoList = new ArrayList<Complemento>(0);

        if (complemento != null && complemento.trim().length() > 0) {
            String[] complementoArray = complemento.split(";");
            for (String complementoTexto : complementoArray) {
                String[] att = complementoTexto.split(":");
                if (att.length == 3) {
                    Complemento c = new Complemento();
                    String codigo = att[0].replaceAll("[^\\d]", "");
                    if (Misc.isInteger(codigo)) {
                        c.setCodigo(Misc.stringToInteger(codigo));
                        c.setVariavel(att[1]);
                        c.setValor(att[2]);
                        complementoList.add(c);
                    }
                } else {
                    Matcher m = Pattern.compile("([0-9]+)\\:([\\w\\h]+)\\:([0-9]+)").matcher(complementoTexto);
                    while (m.find()) {
                        String[] att2 = m.group().split(":");
                        if (Misc.isInteger(att2[0])) {
                            Complemento c = new Complemento();
                            c.setCodigo(Misc.stringToInteger(att2[0]));
                            c.setVariavel(att2[1]);
                            c.setValor(att2[2]);
                            complementoList.add(c);
                        }
                    }
                }
            }
        }
        return complementoList;
    }
    
    private String getHashMovimento(JSONObject mov) {
        String codigo = String.valueOf(getCodigoMovimentoNacional(mov));
        String dataRaw = mov.get("dataHora").toString().replace("-", "").replace(":","");
        String data = dataRaw.length() > 8 ? dataRaw.substring(0,8) : dataRaw;
        String hora = dataRaw.length() >= 14 ? dataRaw.substring(8,14) : "000000";
        String complemento = getComplementosString(mov);
        return Misc.encodeMD5(codigo + data + hora + complemento);
    }

    /*private String getHashMovimentacaoSemComplmento(Movimentacao mov) {
		String idTribunal = String.valueOf(mov.getIdTribunal());
		String idGrau = String.valueOf(mov.getIdGrau());
		String idProcesso = String.valueOf(mov.getIdProcesso());
		String idMovimento = String.valueOf(mov.getIdMovimento());
		String idSituacao = String.valueOf(mov.getIdSituacao());
		String data = String.valueOf(mov.getData());
		String hora = mov.getHorario();
		return Misc.encodeMD5(idTribunal + idGrau + idProcesso + idMovimento + idSituacao + data + hora);
	}*/
    
    private Integer getIdClasse(String codClasseSGT) {
        Integer idClasse = INVALIDO;
        if (codClasseSGT != null && codClasseSGT.trim().length() > 0) {
            if (Misc.isInteger(codClasseSGT.trim())) {
                idClasse = getIdClasse(Misc.stringToInteger(codClasseSGT.trim()));
            }
        }
        return idClasse;
    }

    private Integer getIdClasse(Integer classe) {
        Integer id = NAO_INFORMADO;
        if (classe != null) {
            id = dataCache.getIdClasse(classe);
        }
        return id == null ? INVALIDO : id;
    }

    private List<Integer> getIdAssunto(List<JSONObject> assuntos) {
        List<Integer> assuntoList = new ArrayList<Integer>(0);
        if (assuntos == null || assuntos.size() == 0) {
            assuntoList.add(NAO_INFORMADO);
        } else {
            for (JSONObject a : assuntos) {
                if (a.has("codigoNacional")) {
                    Integer id = null;
                    if (Misc.isInteger(a.get("codigoNacional").toString())) {
                        id = dataCache.getIdAssunto(a.getInt("codigoNacional"));
                    }
                    assuntoList.add(id == null ? INVALIDO : id);
                }else {
                	if (a.has("assuntoLocal")) {
                		JSONObject assuntoLocal = a.getJSONObject("assuntoLocal");
                		if (assuntoLocal.has("codigoPaiNacional")) {
	                        Integer id = null;
	                        if (Misc.isInteger(assuntoLocal.get("codigoPaiNacional").toString())) {
	                            id = dataCache.getIdAssunto(assuntoLocal.getInt("codigoPaiNacional"));
	                        }
	                        assuntoList.add(id == null ? INVALIDO : id);
                		}
                	}else {
            			if (a.has("codigoPaiNacional")) {
                            Integer id = null;
                            if (Misc.isInteger(a.get("codigoPaiNacional").toString())) {
                                id = dataCache.getIdAssunto(a.getInt("codigoPaiNacional"));
                            }
                            assuntoList.add(id == null ? INVALIDO : id);
                        }
            		}
                }
            }
        }
        return assuntoList;
    }

    private JSONObject getOrgaoJulgador(JSONObject movimentoJson, JSONObject dadosBasicos) {
        JSONObject orgaoJulgadorJson = null;
        if (movimentoJson.has("orgaoJulgador") && !movimentoJson.isNull("orgaoJulgador")) {
            orgaoJulgadorJson = movimentoJson.getJSONObject("orgaoJulgador");
        } else {
            if (dadosBasicos.has("orgaoJulgador") && !dadosBasicos.isNull("orgaoJulgador")) {
                orgaoJulgadorJson = dadosBasicos.getJSONObject("orgaoJulgador");
            }
        }
        return orgaoJulgadorJson;
    }
    
    private JSONObject getOrgaoJulgadorColegiado(JSONObject movimentoJson, JSONObject dadosBasicos) {
        JSONObject orgaoJulgadorJson = null;
        if (movimentoJson.has("orgaoJulgadorColegiado") && !movimentoJson.isNull("orgaoJulgadorColegiado")) {
            orgaoJulgadorJson = movimentoJson.getJSONObject("orgaoJulgadorColegiado");
        }
        return orgaoJulgadorJson;
    }

    private Integer getIdOrgaoJulgador(JSONObject orgaoJulgador, Integer idTribunal) {
        Integer id = NAO_INFORMADO;
        if (orgaoJulgador != null) {
            if (orgaoJulgador.has("codigoOrgao")) {
                Object codigoObj = orgaoJulgador.get("codigoOrgao");
                if (codigoObj != null) {
                    if (!Misc.isInteger(codigoObj.toString())) {
                        id = INVALIDO;
                    } else {
                        Integer codigo = orgaoJulgador.getInt("codigoOrgao");
                        OrgaoJulgador oj = dataCache.getOrgaoJulgador(codigo, idTribunal);
                        if (oj == null) {
                            id = null;
                        } else {
                            if (!Objects.equals(oj.getId_tribunal(), idTribunal)) {
                                id = null;
                            } else {
                                id = oj.getId();
                            }
                        }
                    }
                }
            }
        }
        return id == null ? INVALIDO : id;
    }

    private String getCpfMagistrado(JSONObject mov) {
        String valor = "";
        JSONArray magistradoProlator = mov.has("magistradoProlator") ? mov.getJSONArray("magistradoProlator") : null;
        Object reponsavelMovimento = mov.has("responsavelMovimento") ? mov.get("responsavelMovimento") : null;
        Object tipoResponsavelMovimento = mov.has("tipoResponsavelMovimento") ? mov.get("tipoResponsavelMovimento") : null;

        if (magistradoProlator != null && magistradoProlator.length() > 0) {
            valor = magistradoProlator.getString(0);//dataCache.getCpfMagistrado(magistradoProlator.getString(0));
        } else {
            if (reponsavelMovimento != null && tipoResponsavelMovimento != null && tipoResponsavelMovimento.equals(1)) {
                valor = reponsavelMovimento.toString();//dataCache.getCpfMagistrado(reponsavelMovimento.toString());
            }
        }
        return valor == null ? null : (Misc.validarCpf(valor) ? valor : null);
    }

    private Integer getIdData(String dataHora) {
        if (dataHora == null || dataHora.trim().length() == 0) {
            return NAO_INFORMADO;
        }
        if (dataHora.length() != 14) {
            throw new RuntimeException("Data com tamanho inválido: " + dataHora);
        }
        String data = dataHora.substring(0, 8);
        //Apenas para testar se a data é válida
        Misc.stringToDate(data, "yyyyMMdd");
        return dataCache.getIdTempo(data);
    }

    private String getHora(String dataHora) {
        if (dataHora == null) {
            throw new RuntimeException("Data da movimentação não informada");
        }
        if (dataHora.length() != 14) {
            throw new RuntimeException("Data com tamanho inválido: " + dataHora);
        }
        //Apenas para testar se a data é válida
        Misc.stringToDate(dataHora, "yyyyMMddHHmmss");
        String hora = dataHora.substring(8, 14);
        return hora;
    }

    private void atualizarFasesProcesso(Processo processo, List<Movimentacao> movimentacoes) {
        List<Integer> fases = getFasesMovimentacao(movimentacoes);
        List<Integer> fasesProcesso = processo.getIdFaseProcessual();
        if (fasesProcesso.size() == 0 || fasesProcesso.get(0).equals(NAO_INFORMADO)) {
            fasesProcesso.clear();
            fasesProcesso.addAll(fases);
        } else {
            List<Integer> fasesNovas = fasesProcesso.stream().filter(f -> !fases.contains(f)).collect(Collectors.toList());
            fasesProcesso.addAll(fasesNovas);
        }
        Collections.sort(fasesProcesso);
        processo.setIdFaseProcessual(fasesProcesso);

    }

    private void atualizarUltimaClassePorFase(Processo processo, List<Movimentacao> movimentacoes) {
    	List<Integer> fasesProcesso = processo.getIdFaseProcessual();
    	if (fasesProcesso.contains(FASE_EXECUCAO)) {
    		int id = 0;
    		int count = movimentacoes.size()-1;
    		while(count >= 0) {
    			if(movimentacoes.get(count).getIdFase().equals(FASE_EXECUCAO)) {
    				id = movimentacoes.get(count).getIdClasse();
    				if(id > 0) {
    					break;
    				}
    			}
    			count--;
    		}
    		processo.setIdClasseUltimaFase2(id);
        }
    	if (fasesProcesso.contains(FASE_CONHECIMENTO)) {
    		int id = 0;
    		int count = movimentacoes.size()-1;
    		while(count >= 0) {
    			if(movimentacoes.get(count).getIdFase().equals(FASE_CONHECIMENTO)) {
    				id = movimentacoes.get(count).getIdClasse();
    				if(id > 0) {
    					break;
    				}
    			}
    			count--;
    		}
    		processo.setIdClasseUltimaFase1(id);
        }
	}
    
    private void atualizarClassesProcesso(Processo processo, List<Movimentacao> movimentacoes) {
		List<Integer> classes = getClassesMovimentacao(movimentacoes);
		List<Integer> classesProcesso = processo.getIdClasse();
		if(classesProcesso == null || classesProcesso.size() == 0 || classesProcesso.get(0).equals(NAO_INFORMADO)) {
			classesProcesso = new ArrayList<Integer>(0);
			classesProcesso.addAll(classes);
		}
		else {
			List<Integer> classesNovas = classesProcesso.stream().filter(f -> !classes.contains(f)).collect(Collectors.toList());
			classesProcesso.addAll(classesNovas);
		}
		processo.setIdClasse(classesProcesso);
	}


	private void atualizarAssuntosProcesso(Processo processo, List<Movimentacao> movimentacoes) {
		List<Integer> assuntos = getAssuntosMovimentacao(movimentacoes);
		processo.setIdAssunto(assuntos);
	}

	private void atualizarOrgaosJulgadoresProcesso(Processo processo, List<Movimentacao> movimentacoes) {
		List<Integer> orgaosJulgadores = getOrgaosJulgadoresMovimentacao(movimentacoes);
		List<Integer> orgaosJulgadoresProcesso = processo.getIdOrgaoJulgador();
		if(orgaosJulgadoresProcesso == null || orgaosJulgadoresProcesso.size() == 0 || orgaosJulgadoresProcesso.get(0).equals(NAO_INFORMADO)) {
			orgaosJulgadoresProcesso = new ArrayList<Integer>(0);
			orgaosJulgadoresProcesso.addAll(orgaosJulgadores);
		}
		else {
			List<Integer> orgaosJulgadoresNovos = orgaosJulgadoresProcesso.stream().filter(f -> !orgaosJulgadores.contains(f)).collect(Collectors.toList());
			orgaosJulgadoresProcesso.addAll(orgaosJulgadoresNovos);
		}
		processo.setIdOrgaoJulgador(orgaosJulgadoresProcesso);
	}
	
	private void atualizarOrgaosJulgadoresColegiadoProcesso(Processo processo, List<Movimentacao> movimentacoes) {
		List<Integer> orgaosJulgadores = getOrgaosJulgadoresColegiadoMovimentacao(movimentacoes);
		List<Integer> orgaosJulgadoresProcesso = processo.getIdOrgaoJulgadorColegiado();
		if(orgaosJulgadoresProcesso == null || orgaosJulgadoresProcesso.size() == 0 || orgaosJulgadoresProcesso.get(0).equals(NAO_INFORMADO)) {
			orgaosJulgadoresProcesso = new ArrayList<Integer>(0);
			orgaosJulgadoresProcesso.addAll(orgaosJulgadores);
		}
		else {
			List<Integer> orgaosJulgadoresNovos = orgaosJulgadoresProcesso.stream().filter(f -> !orgaosJulgadores.contains(f)).collect(Collectors.toList());
			orgaosJulgadoresProcesso.addAll(orgaosJulgadoresNovos);
		}
		processo.setIdOrgaoJulgadorColegiado(orgaosJulgadoresProcesso);
	}
	
	private void atualizarUltimoOrgaoJulgador(Processo processo, List<Movimentacao> movimentacoes) {
		int id = 0;
		int count = movimentacoes.size()-1;
		while(id == 0) {
			if(count > -1 && movimentacoes.get(count) != null) {
				id = movimentacoes.get(count).getIdOrgaoJulgador();
			}
		}
		processo.setIdOrgaoJulgadorUltimo(id);
	}

	private void atualizarIsCriminal(Processo processo, List<Movimentacao> movimentacoes) {
		Optional<Movimentacao> mov = movimentacoes.stream().filter(m -> m.getCriminal().equals(Boolean.TRUE)).findFirst();
		processo.setCriminal(mov.isPresent());
	}

	private void atualizarPertenceRecorte(Processo processo, List<Movimentacao> movimentacoes) {
		String opcoes = ",2,10,23,24,26,37,41,65,";
    	Optional<Movimentacao> mov = movimentacoes.stream().filter(m -> (opcoes.contains(","+m.getIdSituacao()+",") && m.getDataInicioSituacao() >= 20200101)).findFirst();
    	Optional<Movimentacao> mov2 = movimentacoes.stream().filter(m -> (m.getIdSituacao().equals(88) && (m.getDataFimSituacao().equals(0) || m.getDataFimSituacao() >= 20200101))).findFirst();
    	if(!mov.isPresent() && !mov2.isPresent() && !dataCache.getProcessoDentroEscopo(processo.getIdTribunal(), processo.getNumero(), processo.getId())) {
    		processo.setFlgForaRecorte(true);
    	}else {
    		processo.setFlgForaRecorte(false);
    	}
    }
	
	private void atualizarDataSituacao(Processo processo, List<Movimentacao> movimentacoes) {
    	String opcoesBaixa = ",2,10,23,41,";
    	String opcoesNovo = ",0,9,24,26,61,65,81,91,";
    	String opcoesSituacoes = ",2,4,10,23,25,41,45,46,47,48,49,92,93,94,95,96,128,144,";
    	Optional<Movimentacao> movNovo = movimentacoes.stream().filter(m -> (opcoesNovo.contains(","+m.getIdSituacaoIniciar()+",") && m.getIdSituacao().equals(88))).findFirst();
    	Optional<Movimentacao> movBaixa = movimentacoes.stream().filter(m -> opcoesBaixa.contains(","+m.getIdSituacao()+",")).findFirst();
    	if(movNovo.isPresent()) {
    		processo.setDataCasoNovo(movNovo.get().getDataHora());
    	}
    	if(movBaixa.isPresent()) {
    		processo.setDataBaixa(movBaixa.get().getDataHora());
    	}
    	Collections.reverse(movimentacoes);
    	Optional<Movimentacao> movUltimaSituacao = movimentacoes.stream().filter(m -> (opcoesSituacoes.contains(","+m.getIdSituacao()+",") && m.getDataFimSituacao() == 0)).findFirst();
    	if(movUltimaSituacao.isPresent()) {
    		processo.setIdSituacaoAtual(movUltimaSituacao.get().getIdSituacao());
    		processo.setDataSituacaoAtual(movUltimaSituacao.get().getDataHora());
    	}
    	Collections.reverse(movimentacoes);
    }
	
	private void atualizarDataUltimaMovimentacao(Processo processo, List<JSONObject> processosJson) {
        JSONObject p = processosJson.get(processosJson.size() - 1);
        Date dataAtual = processo.getDataUltimaMovimentacao();
        Date novaData = getDataUltimaMovimentacao(p);
        if (dataAtual == null || Misc.isDataMaior(novaData, dataAtual)) {
            processo.setDataUltimaMovimentacao(novaData);
        }
    }

    private Date getDataUltimaMovimentacao(JSONObject p) {
        Date data = Misc.stringToDate("00010101000000", "yyyyMMddHHmmss");
        List<JSONObject> movList = Misc.elementToJsonObjectList(p, "movimento");
        if (movList != null && movList.size() > 0) {
            for (JSONObject mov : movList) {
                if (!movimentoExterno(mov)) {
                    if (mov.has("dataHora") && !mov.isNull("dataHora")) {
                    	Date dataHora = Misc.stringToDateTime(mov.get("dataHora").toString());
                        if (dataHora != null && Misc.isDataMenor(data, dataHora)) {
                            data = dataHora;
                        }
                    }
                }
            }
        }
        return data;
    }

    private boolean movimentoExterno(JSONObject mov) {
        final int JUNTADA = 67;
        final int PETICAO = 85;
        final int DOCUMENTO = 581;
        boolean externo = false;

        Integer codigo = getCodigoMovimentoNacionalSGT(mov);
        if(codigo != null && (codigo.equals(JUNTADA)
            || codigo.equals(PETICAO)
            || codigo.equals(DOCUMENTO))) {
            externo = true;
        }
        return externo;
    }

    private List<Movimentacao> normalizarMudancaClasses(List<Movimentacao> movList) {
        final Integer MOV_MUDANCA_CLASSE_SGT = 10966;
        final Integer MOV_EVOLUCAO_CLASSE_SGT = 14739;
        final Integer MOV_RETFICACAO_CLASSE_SGT = 14738;
        final Integer COD_COMPLEMENTO_CLASSE_ANTERIOR = 26;
        final Integer COD_COMPLEMENTO_CLASSE_NOVA = 27;
        String codClasseAnteriorSGT = null;
        String codClasseNovaSGT = null;

        //Busca os IDs de movimentos de mudança, evolução e retificação de classe
        Integer idMudancaClasse = dataCache.getIdMovimento(MOV_MUDANCA_CLASSE_SGT);
        Integer idEvolucaoClasse = dataCache.getIdMovimento(MOV_EVOLUCAO_CLASSE_SGT);
        Integer idRetificacaoClasse = dataCache.getIdMovimento(MOV_RETFICACAO_CLASSE_SGT);

        //Verifica se conseguiu capturar os IDs de mudança, evolução e retificação de classe
        if (idMudancaClasse != null && idMudancaClasse > 0 && idEvolucaoClasse != null && idEvolucaoClasse > 0 && idRetificacaoClasse != null && idRetificacaoClasse > 0) {
            for (int i = 0; i < movList.size(); i++) {
                Movimentacao mov = movList.get(i);
                if (idMudancaClasse.equals(mov.getIdMovimento()) || idEvolucaoClasse.equals(mov.getIdMovimento()) || idRetificacaoClasse.equals(mov.getIdMovimento())) {
                    List<Complemento> complementoList = getComplementosTexto(mov.getComplemento());
                    if (complementoList != null && complementoList.size() > 0) {
                        for (Complemento c : complementoList) {
                            if (COD_COMPLEMENTO_CLASSE_ANTERIOR.equals(c.getCodigo())) {
                                codClasseAnteriorSGT = c.getValor();
                            } else if (COD_COMPLEMENTO_CLASSE_NOVA.equals(c.getCodigo())) {
                                codClasseNovaSGT = c.getValor();
                            }
                        }
                        if (idRetificacaoClasse.equals(mov.getIdMovimento())) {
                            retificarClasse(codClasseAnteriorSGT, codClasseNovaSGT, i, movList);
                            mudancaClasse = true;
                        } else {
                            if (isClasseValida(codClasseAnteriorSGT) && isClasseValida(codClasseNovaSGT)) {
                                evoluirClasses(codClasseAnteriorSGT, codClasseNovaSGT, i, OrdenacaoEnum.CRESCENTE, movList);
                                evoluirClasses(codClasseAnteriorSGT, codClasseNovaSGT, i, OrdenacaoEnum.DECRESCENTE, movList);
                                mudancaClasse = true;
                            }
                        }
                    }
                }
            }
        }
        return movList;
    }

    private Integer getIdOrgaoJulgadorCapa(List<JSONObject> processosJson) {
        Integer idOrgaoJulgadorCapa = NAO_INFORMADO;
        if (processosJson != null && processosJson.size() > 0) {
            JSONObject procJson = processosJson.get(processosJson.size() - 1);
            JSONObject dadosBasicos = procJson.has("dadosBasicos") ? procJson.getJSONObject("dadosBasicos") : null;
            if (dadosBasicos != null) {
                JSONObject ojJson = dadosBasicos.has("orgaoJulgador") && !dadosBasicos.isNull("orgaoJulgador") ? dadosBasicos.getJSONObject("orgaoJulgador") : null;
                if (ojJson != null) {
                    if (ojJson.has("codigoOrgao")) {
                        Object codigoObj = ojJson.get("codigoOrgao");
                        if (codigoObj != null) {
                            if (Misc.isInteger(codigoObj.toString())) {
                                String siglaTribunal = dadosBasicos.has("siglaTribunal") ? dadosBasicos.getString("siglaTribunal") : null;
                                if (siglaTribunal != null) {
                                    Integer idTribunal = dataCache.getIdTribunal(siglaTribunal);
                                    Integer idOrgaoJulgadorSGT = Misc.stringToInteger(codigoObj.toString());
                                    OrgaoJulgador oj = dataCache.getOrgaoJulgador(idOrgaoJulgadorSGT, idTribunal);
                                    if (oj != null) {
                                        idOrgaoJulgadorCapa = oj.getId();
                                    } else {
                                        idOrgaoJulgadorCapa = INVALIDO;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return idOrgaoJulgadorCapa;
    }

    private List<Movimentacao> normalizarMovimentoRedistribuicao(List<Movimentacao> movList, List<JSONObject> processosJson) {
        final Integer MOV_REDISTRIBUIDO = 19;

        for (int i = 0; i < movList.size(); i++) {
            Movimentacao mov = movList.get(i);
            //Verifica se o movimento é de redistribuição
            if (mov.getIdMovimento().equals(MOV_REDISTRIBUIDO)) {
                Movimentacao movPosterior = null;

                //Armazena o movimento posterior se houver elemento posterior na lista
                if (movList.size() > i + 1) {
                    movPosterior = movList.get(i + 1);
                }

                if (movPosterior != null) {
                    //Se o oj do movimento posterior for diferente do oj do moviment atual, então altera o oj do movimento atual
                    if (!mov.getIdOrgaoJulgador().equals(movPosterior.getIdOrgaoJulgador())) {
                        mov.setIdOrgaoJulgador(movPosterior.getIdOrgaoJulgador());
                    }
                } //Se não houver movimento posterior, compara com o órgao julgador da capa do processo
                else {
                    Integer idOrgaoJulgadorCapa = getIdOrgaoJulgadorCapa(processosJson);
                    if (idOrgaoJulgadorCapa != null && idOrgaoJulgadorCapa.intValue() > 0) {
                        //Se o oj da capa for diferente do oj do movimento atual, então altera o oj do movimento atual
                        if (!mov.getIdOrgaoJulgador().equals(idOrgaoJulgadorCapa)) {
                            mov.setIdOrgaoJulgador(idOrgaoJulgadorCapa);
                        }
                    }
                }
            }
        }
        return movList;
    }

    private boolean isClasseValida(String codClasseSGT) {
        if (!Misc.isInteger(codClasseSGT)) {
            return false;
        }
        Integer idClasse = dataCache.getIdClasse(Misc.stringToInteger(codClasseSGT));
        return idClasse != null && idClasse > 0;
    }

    private void evoluirClasses(String codClasseAnteriorSGT, String codClasseNovaSGT, int posicao, OrdenacaoEnum ordem, List<Movimentacao> movimentacoes) {
        final Integer MOV_MUDANCA_CLASSE = 85;
        final Integer MOV_EVOLUCAO_CLASSE = 722;
        final Integer MOV_RETIFICACAO_CLASSE = 775;

        if (codClasseAnteriorSGT == null || codClasseAnteriorSGT.trim().length() == 0) {
            return;
        }
        if (codClasseNovaSGT == null || codClasseNovaSGT.trim().length() == 0) {
            return;
        }

        if (!Misc.isInteger(codClasseAnteriorSGT.trim()) || !Misc.isInteger(codClasseNovaSGT.trim())) {
            return;
        }

        Integer idClasseAnterior = getIdClasse(Misc.stringToInteger(codClasseAnteriorSGT.trim()));
        Integer idClasseNova = getIdClasse(Misc.stringToInteger(codClasseNovaSGT.trim()));

        if (!idClasseAnterior.equals(INVALIDO) && !idClasseNova.equals(INVALIDO) && !idClasseAnterior.equals(NAO_INFORMADO) && !idClasseNova.equals(NAO_INFORMADO)) {
            List<Movimentacao> movimentacaoList = null;
            if (ordem.equals(OrdenacaoEnum.CRESCENTE)) {
                movimentacaoList = movimentacoes.stream().skip(posicao).collect(Collectors.toList());
                for (Movimentacao mov : movimentacaoList) {
                    mov.setIdClasse(idClasseNova);
                    //Sai do loop se encontrar movimento de baixa
                    if (isMovimentacaoBaixa(mov)) {
                        break;
                    }
                }
            } else if (ordem.equals(OrdenacaoEnum.DECRESCENTE)) {
                movimentacaoList = movimentacoes.stream().limit(posicao).collect(Collectors.toList());
                //Verifica se existe movimento de mudança de classe anterior ao atual. Se existir, sai sem alterar as classes dos movimentos anteriores
                Optional<Movimentacao> movMudancaClassePrecedente = movimentacaoList.stream().filter(m -> m.getIdMovimento().equals(MOV_MUDANCA_CLASSE) || m.getIdMovimento().equals(MOV_EVOLUCAO_CLASSE)).findFirst();
                if (!movMudancaClassePrecedente.isPresent()) {
                    for (int i = movimentacaoList.size() - 1; i >= 0; i--) {
                        Movimentacao mov = movimentacaoList.get(i);
                        if (mov.getIdMovimento().equals(MOV_RETIFICACAO_CLASSE)) {
                            break;
                        }
                        mov.setIdClasse(idClasseAnterior);
                    }
                }
            }
        }
    }

    private void retificarClasse(String codClasseAnteriorSGT, String codClasseNovaSGT, int posicao, List<Movimentacao> movimentacoes) {
        Integer idClasseAnterior = getIdClasse(codClasseAnteriorSGT);
        Integer idClasseNova = getIdClasse(codClasseNovaSGT);

        if (!idClasseAnterior.equals(INVALIDO) && !idClasseNova.equals(INVALIDO) && !idClasseAnterior.equals(NAO_INFORMADO) && !idClasseNova.equals(NAO_INFORMADO)) {
            Movimentacao movRetificacao = movimentacoes.get(posicao);
            movRetificacao.setIdClasse(idClasseNova);
            List<Movimentacao> movimentacaoList = movimentacoes.stream().limit(posicao).collect(Collectors.toList());
            for (Movimentacao mov : movimentacaoList) {
                if (mov.getIdClasse().equals(idClasseAnterior)) {
                    mov.setIdClasse(idClasseNova);
                }
            }
        }
    }

    private boolean isMovimentacaoBaixa(Movimentacao mov) {
    	final Integer MOV_ARQUIVAMENTO_DEFINITIVO = 246;
        final Integer MOV_BAIXA_DEFINITIVA = 22;
        final Integer MOV_CANCELAMENTO_DISTRIBUICAO_DISTRIBUIDOR = 488;
        final Integer MOV_CANCELAMENTO_DISTRIBUICAO_ESCRIVAO = 12186;
        final Integer MOV_REMESSA_DISTRIBUIDOR = 982;
        final Integer MOV_REMESSA_ESCRIVAO = 123;
        final Integer COD_MOTIVO_REMESSA = 18;
        final String MOTIVO_REMESSA_JULGAMENTO = "194";
        final String MOTIVO_REMESSA_RECURSO = "38";
        final String MOTIVO_REMESSA_COMPETENCIA = "90";

        boolean isBaixa = Boolean.FALSE;

        if (mov.getIdMovimento().equals(MOV_ARQUIVAMENTO_DEFINITIVO)
            || mov.getIdMovimento().equals(MOV_BAIXA_DEFINITIVA)
            || mov.getIdMovimento().equals(MOV_CANCELAMENTO_DISTRIBUICAO_DISTRIBUIDOR)
            || mov.getIdMovimento().equals(MOV_CANCELAMENTO_DISTRIBUICAO_ESCRIVAO)) {
            isBaixa = true;
        } else {
            if (mov.getIdMovimento().equals(MOV_REMESSA_DISTRIBUIDOR) || mov.getIdMovimento().equals(MOV_REMESSA_ESCRIVAO)) {
                List<Complemento> complementos = getComplementosTexto(mov.getComplemento());
                if (complementos != null && complementos.size() > 0) {
                    for (Complemento c : complementos) {
                        if (c.getCodigo().equals(COD_MOTIVO_REMESSA)) {
                            if (c.getValor().equals(MOTIVO_REMESSA_RECURSO)
                                || c.getValor().equals(MOTIVO_REMESSA_JULGAMENTO)
                                || c.getValor().equals(MOTIVO_REMESSA_COMPETENCIA)) {
                                isBaixa = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return isBaixa;
    }

    private boolean isMovimentacaoDistribuicaoDependencia(Movimentacao mov) {
    	final Integer MOV_DISTRIBUIDO = 26;
        final Integer COD_TIPO_DISTRIBUICAO = 2;
        final String TIPO_DISTRIBUICAO_DEPENDENCIA = "4";
        final Integer G2 = 2;
        final Integer SUP = 5;

        boolean isDitribuicaoDependente = Boolean.FALSE;

        if (mov.getIdGrau().equals(G2) || mov.getIdGrau().equals(SUP)) {
            if (mov.getIdMovimento().equals(MOV_DISTRIBUIDO)) {
                List<Complemento> complementos = getComplementosTexto(mov.getComplemento());
                if (complementos != null && complementos.size() > 0) {
                    for (Complemento c : complementos) {
                        if (c.getCodigo().equals(COD_TIPO_DISTRIBUICAO)) {
                            if (c.getValor().equals(TIPO_DISTRIBUICAO_DEPENDENCIA)) {
                                isDitribuicaoDependente = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return isDitribuicaoDependente;
    }

    private boolean isMovimentacaoRedistribuicaoPorIncompetencia(Movimentacao mov) {
    	final Integer MOV_REDISTRIBUIDO = 36;
        final Integer COD_MOTIVO_REDISTRIBUICAO = 17;
        final String MOTIVO_REDISTRIBUICAO_INCOMPETENCIA = "83";

        boolean isRedistribuicaoPorIncompetencia = Boolean.FALSE;

        if (mov.getIdMovimento().equals(MOV_REDISTRIBUIDO)) {
            List<Complemento> complementos = getComplementosTexto(mov.getComplemento());
            if (complementos != null && complementos.size() > 0) {
                for (Complemento c : complementos) {
                    if (c.getCodigo().equals(COD_MOTIVO_REDISTRIBUICAO)) {
                        if (c.getValor().equals(MOTIVO_REDISTRIBUICAO_INCOMPETENCIA)) {
                            isRedistribuicaoPorIncompetencia = true;
                            break;
                        }
                    }
                }
            }
        }
        return isRedistribuicaoPorIncompetencia;
    }
    
    public int getDataFinal(JSONObject p) {
    	int data = 0;
        List<JSONObject> movList = Misc.elementToJsonObjectList(p, "movimento");
        if (movList != null && movList.size() > 0) {
            for (JSONObject mov : movList) {
                if (!movimentoExterno(mov)) {
                    if (mov.has("dataHora") && !mov.isNull("dataHora")) {
                        int dataHora = getIdData(mov.get("dataHora").toString());
                        if (data < dataHora) {
                            data = dataHora;
                        }
                    }
                }
            }
        }
    	return data;
    }
    
    public int getDataLimite(Long idProcesso, int idTribunal, String siglaGrau) {
    	List<ProcessoIndicador> saida = processoIndicadorRepository.findByIdProcessoIdTribunalIdGrau(idProcesso, idTribunal, dataCache.getIdGrau(siglaGrau));
    	return saida.size() == 0 ? 0 : saida.get(0).getDataUltimoInicio();
    }
    
    public int getDataInicial(JSONObject p) {
    	int data = 99999999;
        List<JSONObject> movList = Misc.elementToJsonObjectList(p, "movimento");
        if (movList != null && movList.size() > 0) {
            for (JSONObject mov : movList) {
                if (!movimentoExterno(mov)) {
                    if (mov.has("dataHora") && !mov.isNull("dataHora")) {
                        int dataHora = getIdData(mov.get("dataHora").toString());
                        if (data > dataHora) {
                            data = dataHora;
                        }
                    }
                }
            }
        }
    	return data;
    }
}
