package br.jus.cnj.datajud.elasticToDatajud.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.jus.cnj.datajud.elasticToDatajud.model.Processo;
import br.jus.cnj.datajud.elasticToDatajud.repository.ProcessoRepository;
import br.jus.cnj.datajud.elasticToDatajud.util.DataCache;
import br.jus.cnj.datajud.elasticToDatajud.util.EntityUtil;
import br.jus.cnj.datajud.elasticToDatajud.util.Misc;

@Service
public class ConsolidadorProcessoService {
        private static final Integer INVALIDO = -1;
        private static final Integer NAO_INFORMADO = 0;

        Logger log = LoggerFactory.getLogger(ConsolidadorProcessoService.class);

        @Autowired
        private DataCache dataCache;

        @Autowired
        public ProcessoRepository processoRepository;

        @Autowired
        private ConsolidadorMovimentoService consolidadorMovimentoService;

        public Processo consolidar(List<JSONObject> processosJson) {
                if (processosJson == null || processosJson.size() == 0) {
                        throw new RuntimeException("O arquivo JSON com os dados do processo está vazio");
                }
                Processo processo = null;
                if(verificaPossuiMovimento(processosJson)) {
	                //Carrega o processo apenas se ele estiver tramitando ou tenha sido baixado após 01/01/2020
	                if (!verificaProcessoBaixado2020(processosJson) || verificaProcessoExistente(processosJson)) {
	                        processo = criarProcesso(processosJson);
	                        processo = salvarProcesso(processo);
	                }
                }
                return processo;
        }

        private boolean verificaProcessoExistente(List<JSONObject> processosJson) {
        	 JSONObject p = processosJson.get(processosJson.size() - 1);
        	 List<Processo> lista = processoRepository.findByIdTribunalNumero(getIdTribunal(p), getNumero(p));
        	 return lista.isEmpty()?false:true;
        }
        
        private boolean verificaPossuiMovimento(List<JSONObject> processosJson) {
            boolean possuiMovimento = false;
            for (JSONObject proc : processosJson) {
                    if (proc.has("movimento")) {
                            List<JSONObject> movList = null;
                            if (proc.get("movimento") instanceof JSONObject) {
                                    movList = new ArrayList<JSONObject>(0);
                                    movList.add(proc.getJSONObject("movimento"));
                            } else {
                                    JSONArray movArray = proc.getJSONArray("movimento");
                                    movList = Misc.jsonArrayToJsonObjectList(movArray);
                                    Collections.sort(movList, new JsonComparator());
                            }
                            for (JSONObject mov : movList) {
                                    Integer codigoMovimentoNacional = consolidadorMovimentoService.getCodigoMovimentoNacionalSGT(mov);
                                    if (codigoMovimentoNacional != null) {
                                            possuiMovimento = true;
                                            break;
                                    }
                            }
                            if(possuiMovimento) {
                            	break;
                            }
                    }
            }
            return possuiMovimento;
        }
        
        private boolean verificaProcessoBaixado2020(List<JSONObject> processosJson) {
                final Date DATA_MINIMA = Misc.stringToDate("20200101", "yyyyMMdd");
                final Integer CODIGO_BAIXADO_DEFINITIVAMENTE = 22;
                final Integer CODIGO_ARQUIVADO_DEFINITAVAMENTE = 246;
                final Integer CODIGO_REATIVACAO = 849;
                final Integer CODIGO_LIQUIDACAO_INICIADA = 11384;
				final Integer CODIGO_EXECUCAO_INICIADA = 11385;
				final Integer CODIGO_DISTRIBUICAO = 26;
                Boolean processoBaixado = Boolean.FALSE;

                for (JSONObject proc : processosJson) {
                        if (proc.has("movimento")) {
                                List<JSONObject> movList = null;
                                if (proc.get("movimento") instanceof JSONObject) {
                                        movList = new ArrayList<JSONObject>(0);
                                        movList.add(proc.getJSONObject("movimento"));
                                } else {
                                        JSONArray movArray = proc.getJSONArray("movimento");
                                        movList = Misc.jsonArrayToJsonObjectList(movArray);
                                        Collections.sort(movList, new JsonComparator());
                                }
                                for (JSONObject mov : movList) {
                                        Integer codigoMovimentoNacional = consolidadorMovimentoService.getCodigoMovimentoNacionalSGT(mov);
                                        if (codigoMovimentoNacional != null) {
                                                if (codigoMovimentoNacional.equals(CODIGO_BAIXADO_DEFINITIVAMENTE)
                                                        || codigoMovimentoNacional.equals(CODIGO_ARQUIVADO_DEFINITAVAMENTE)) {
                                                        Date dataBaixa = Misc.stringToDateTime(mov.has("dataHora") ? mov.get("dataHora").toString() : null);
                                                        if (dataBaixa != null && Misc.isDataMenorIgual(dataBaixa, DATA_MINIMA)) {
                                                                processoBaixado = Boolean.TRUE;
                                                        }
                                                } else if (codigoMovimentoNacional.equals(CODIGO_REATIVACAO)|| 
								codigoMovimentoNacional.equals(CODIGO_LIQUIDACAO_INICIADA) ||
								codigoMovimentoNacional.equals(CODIGO_EXECUCAO_INICIADA) ||
								codigoMovimentoNacional.equals(CODIGO_DISTRIBUICAO)) {
                                                        processoBaixado = Boolean.FALSE;
                                                }
                                        }
                                }
                        }
                }
                return processoBaixado;
        }

        public Processo criarProcesso(List<JSONObject> processosJson) {
                JSONObject p = processosJson.get(processosJson.size() - 1);

                Processo processo = new Processo();
                processo.setIdTribunal(getIdTribunal(p));
                processo.setSiglaTribunal(getSiglaTribunal(p));
                processo.setIdGrau(getIdGrau(p));
                processo.setSiglaGrau(getSiglaGrau(p));
                processo.setNumero(getNumero(p));
                processo.setDataAjuizamento(geDataAjuizamento(p));
                processo.setIdNivelSigilo(getIdNivelSigilo(p));
                //processo.setNomeNivelSigilo(getNomeNivelSigilo(processo.getIdNivelSigilo()));
                processo.setIdFormato(getIdFormatoProcesso(p));
                processo.setIdSistema(getIdSistema(p));

                processo.setQtdPessoaFisicaPoloAtivo(getQtdPessoa(p, "AT", "FISICA"));
                processo.setQtdPessoaJuridicaPoloAtivo(getQtdPessoa(p, "AT", "JURIDICA"));
                processo.setQtdAutoridadePoloAtivo(getQtdPessoa(p, "AT", "AUTORIDADE"));

                processo.setQtdPessoaFisicaPoloPassivo(getQtdPessoa(p, "PA", "FISICA"));
                processo.setQtdPessoaJuridicaPoloPassivo(getQtdPessoa(p, "PA", "JURIDICA"));
                processo.setQtdAutoridadePoloPassivo(getQtdPessoa(p, "PA", "AUTORIDADE"));

                processo.setQtdPessoaFisicaVitima(getQtdPessoa(p, "VI", "FISICA"));
		
				processo.setAssistenciaJudiciaria(getAssistenciaJudiciaria(p));
				processo.setValorCausa(getValorCausa(p,"valorCausa"));
				processo.setCriminal(Boolean.FALSE); //Será preenchido posteriormente, após o processamento das movimentações
				processo.setIdFaseProcessual(new ArrayList<Integer>(0));
				processo.setIdClasse(new ArrayList<Integer>(0));
				processo.setIdAssunto(new ArrayList<Integer>(0));
				processo.setIdOrgaoJulgador(new ArrayList<Integer>(0));
				processo.setIdOrgaoJulgadorColegiado(new ArrayList<Integer>(0));
				processo.setFlgForaRecorte(false);
				
				processo.setPedidoPrioridade(getCampoBooleano(p,"pedidoPrioridade"));
				processo.setIdTipoPrioridade(getPrioridade(p));
				processo.setCustasIniciais(getValorCausa(p,"custasIniciais"));
				processo.setCustasRecursais(getValorCausa(p,"custasRecursais"));
				processo.setCustasFinais(getValorCausa(p,"custasFinais"));
				processo.setNucleo4(getCampoBooleano(p,"nucleo4"));
				processo.setJuizo100Digital(getCampoBooleano(p,"juizo100Digital"));
				processo.setAnoEleicao(getAnoEleicao(p));
				processo.setIdSituacaoAtual(0);
				processo.setRegistroUnico(getRegistroUnico(p));

                return processo;
        }

        private Processo salvarProcesso(Processo processo) {
        	Processo p = null;
            List<Processo> processoList = processoRepository.findByIdTribunalIdGrauNumero(processo.getIdTribunal(), processo.getIdGrau(), processo.getNumero());
            if (processoList != null && processoList.size() > 0) {
            		processo.setMillisinsercao(processoList.get(0).getMillisinsercao() == null || processo.getMillisinsercao() > processoList.get(0).getMillisinsercao() ? processo.getMillisinsercao() : processoList.get(0).getMillisinsercao());
                    p = atualizarProcesso(processoList.get(0), processo);
            } else {
                    p = processoRepository.saveAndFlush(processo);
            }
            p.setRegistroUnico(processo.getRegistroUnico());
            return p;
        }

        private Processo atualizarProcesso(Processo atual, Processo novo) {
                Processo p = atual;
                if (!atual.equals(novo)) {
                        try {
                                EntityUtil.copyValues(novo, p);
                                processoRepository.saveAndFlush(p);
                        } catch (Exception e) {
                                log.error("Erro ao atualizar  valores do processo: %s" + e.getLocalizedMessage());
                        }
                }
                return p;
        }

        private Integer getIdTribunal(JSONObject p) {
                String sigla = null;
                if (p.has("siglaTribunal") && !p.isNull("siglaTribunal")) {
                        sigla = p.getString("siglaTribunal");
                }
                Integer id = sigla == null ? NAO_INFORMADO : dataCache.getIdTribunal(sigla);
                return id == null ? INVALIDO : id;
        }

        private String getSiglaTribunal(JSONObject p) {
                String sigla = null;
                if (p.has("siglaTribunal") && !p.isNull("siglaTribunal")) {
                        sigla = p.getString("siglaTribunal");
                }
                return sigla;
        }

        private Integer getIdGrau(JSONObject p) {
                String sigla = null;
                if (p.has("grau") && !p.isNull("grau")) {
                        sigla = p.getString("grau");
                }
                Integer id = sigla == null ? NAO_INFORMADO : dataCache.getIdGrau(sigla);
                return id == null ? INVALIDO : id;
        }

        private String getSiglaGrau(JSONObject p) {
                String sigla = null;
                if (p.has("grau") && !p.isNull("grau")) {
                        sigla = p.getString("grau");
                }
                return sigla;
        }

        private String getNumero(JSONObject p) {
                String numero = NAO_INFORMADO.toString();
                if (p.has("dadosBasicos")) {
                        JSONObject dadosBasicos = p.getJSONObject("dadosBasicos");
                        if (dadosBasicos.has("numero") && !dadosBasicos.isNull("numero")) {
                                numero = dadosBasicos.getString("numero");
                        }
                }
                return Misc.completaZeros(numero, 20);
        }

        private Date geDataAjuizamento(JSONObject p) {
                String data = "00010101000000";
                if (p.has("dadosBasicos")) {
                        JSONObject dadosBasicos = p.getJSONObject("dadosBasicos");
                        if (dadosBasicos.has("dataAjuizamento") && !dadosBasicos.isNull("dataAjuizamento")) {
                                if (dadosBasicos.get("dataAjuizamento") instanceof String) {
                                        data = dadosBasicos.getString("dataAjuizamento");
                                } else {
                                        Long milliseconds = dadosBasicos.getLong("dataAjuizamento");
                                        data = Misc.millisecondsToString(milliseconds, "yyyyMMddHHmmss");
                                }
                        }
                }
                return Misc.stringToDate(data, "yyyyMMddHHmmss");
        }

        private Integer getIdNivelSigilo(JSONObject p) {
                Integer idNivelSigilo = NAO_INFORMADO;
                if (p.has("dadosBasicos")) {
                        JSONObject dadosBasicos = p.getJSONObject("dadosBasicos");
                        if (dadosBasicos.has("nivelSigilo") && !dadosBasicos.isNull("nivelSigilo")) {
                                idNivelSigilo = dadosBasicos.getInt("nivelSigilo");
                        }
                }
                if (idNivelSigilo != NAO_INFORMADO) {
                        if (!dataCache.isNivelSigiloValido(idNivelSigilo)) {
                                idNivelSigilo = INVALIDO;
                        }
                }
                return idNivelSigilo;
        }

        private Integer getIdFormatoProcesso(JSONObject p) {
                Integer idFormato = NAO_INFORMADO;
                if (p.has("dadosBasicos")) {
                        JSONObject dadosBasicos = p.getJSONObject("dadosBasicos");
                        if (dadosBasicos.has("procEl") && !dadosBasicos.isNull("procEl")) {
                                idFormato = dadosBasicos.getInt("procEl");
                        }
                }
                if (idFormato != NAO_INFORMADO) {
                        if (!dataCache.isFormatoProcessoValido(idFormato)) {
                                idFormato = INVALIDO;
                        }
                }
                return idFormato;
        }

        private Integer getIdSistema(JSONObject p) {
                Integer idSistema = NAO_INFORMADO;
                if (p.has("dadosBasicos")) {
                        JSONObject dadosBasicos = p.getJSONObject("dadosBasicos");
                        if (dadosBasicos.has("dscSistema") && !dadosBasicos.isNull("dscSistema")) {
                                idSistema = dadosBasicos.getInt("dscSistema");
                        }
                }
                if (idSistema != NAO_INFORMADO) {
                        if (!dataCache.isSistemaValido(idSistema)) {
                                idSistema = INVALIDO;
                        }
                }
                return idSistema;
        }

        private int getQtdPessoa(JSONObject p, String siglaPolo, String tipoPessoa) {
    		int qtd = 0;
    		JSONObject dadosBasicos = p.getJSONObject("dadosBasicos");
    		if(dadosBasicos.has("polo")) {
    			List<JSONObject> polos = Misc.elementToJsonObjectList(dadosBasicos, "polo");
    			for (JSONObject polo : polos) {
    				if(polo.has("polo")) {
    					if(polo.getString("polo").equals(siglaPolo)) {
    						List<JSONObject> partes = Misc.elementToJsonObjectList(polo, "parte");
    						for (JSONObject parte : partes) {
    							if(parte.has("pessoa") && !parte.isNull("pessoa")) {
    								JSONObject pessoa = parte.getJSONObject("pessoa");
    								if(pessoa.has("tipoPessoa") && !pessoa.isNull("tipoPessoa")) {
    									String tipo = pessoa.getString("tipoPessoa");
    									if(tipo.equalsIgnoreCase(tipoPessoa)) {
    										qtd++;
    									}
    								}
    							}
    						}
    					}
    				}
    			}
    		}
    		return qtd;
    	}

    	private Boolean getAssistenciaJudiciaria(JSONObject p) {
    		Boolean assistenciaJudiciaria = Boolean.FALSE;
    		JSONObject dadosBasicos = p.getJSONObject("dadosBasicos");
    		if(dadosBasicos.has("polo")) {
    			List<JSONObject> polos = Misc.elementToJsonObjectList(dadosBasicos, "polo");
    			for (JSONObject polo : polos) {
    				if(polo.has("parte")) {
    					List<JSONObject> partes = Misc.elementToJsonObjectList(polo, "parte");
    					for (JSONObject parte : partes) {
    						if(parte.has("assistenciaJudiciaria") && !parte.isNull("assistenciaJudiciaria")) {
    							assistenciaJudiciaria = parte.getBoolean("assistenciaJudiciaria");
    							if(assistenciaJudiciaria.equals(Boolean.TRUE)) {
    								break;
    							}
    						}
    					}
    				}
    			}
    		}
    		return assistenciaJudiciaria;
    	}

    	private Boolean getCampoBooleano(JSONObject p, String campo) {
    		Boolean valor = false;
    		
    		JSONObject dadosBasicos = p.getJSONObject("dadosBasicos");
    		if(dadosBasicos.has(campo) && !dadosBasicos.isNull(campo)) {
    			valor = Boolean.parseBoolean(dadosBasicos.get(campo).toString());
    		}
    		return valor;
    	}
    	
    	private Integer getAnoEleicao(JSONObject p) {
    		Integer valor = 0;
    		
    		JSONObject dadosBasicos = p.getJSONObject("dadosBasicos");
    		if(dadosBasicos.has("anoEleicao") && !dadosBasicos.isNull("anoEleicao")) {
    			valor = Integer.parseInt(dadosBasicos.get("anoEleicao").toString());
    		}
    		return valor;
    	}
    	
    	private Boolean getRegistroUnico(JSONObject p) {
    		Boolean valor = false;
    		if(p.has("dpj_mtd11Valido") && !p.isNull("dpj_mtd11Valido")) {
    			valor = Boolean.parseBoolean(p.get("dpj_mtd11Valido").toString().toLowerCase());
    		}
    		return valor;
    	}

    	private Double getValorCausa(JSONObject p, String campo) {
    		Double valorCausa = null;
    		final double VALOR_MAXIMO = 9999999999.99;
    		
    		JSONObject dadosBasicos = p.getJSONObject("dadosBasicos");
    		if(dadosBasicos.has(campo) && !dadosBasicos.isNull(campo)) {
    			String valorCausaStr = dadosBasicos.get(campo).toString();
    			if(Misc.isDouble(valorCausaStr)) {
    				valorCausa = Misc.roundDouble(Double.parseDouble(valorCausaStr),2);
    				if(valorCausa.doubleValue() < 0.00 || valorCausa.doubleValue() > VALOR_MAXIMO ) {
    					valorCausa = null;
    				}
    			}
    		}
    		return valorCausa;
    	}
    	
    	private List<String> getPrioridade(JSONObject p) {
    		List<String> assuntoList = new ArrayList<String>(0);
    		
    		JSONObject dadosBasicos = p.getJSONObject("dadosBasicos");
    		List<JSONObject> assuntos = dadosBasicos != null ? (dadosBasicos.has("prioridadeProcesso") ? Misc.elementToJsonObjectList(dadosBasicos, "prioridadeProcesso") : null) : null;
    		if (assuntos == null || assuntos.size() == 0) {
                assuntoList.add("");
            } else {
                for (JSONObject a : assuntos) {
                    if (a.has("tipo")) {
                        String id = a.getString("tipo");
                        if (id != null) {
                        	assuntoList.add(dataCache.getIdPrioridade(id));
                        }                        
                    }
                }
            }
    		return assuntoList;
    	}

        class JsonComparator implements Comparator<JSONObject> {
                @Override
                public int compare(JSONObject o1, JSONObject o2) {
                        Date d1 = Misc.stringToDateTime(o1.has("dataHora") ? o1.get("dataHora").toString() : null);
                        Date d2 = Misc.stringToDateTime(o2.has("dataHora") ? o2.get("dataHora").toString() : null);
                        return d1.compareTo(d2);
                }
        }
}
