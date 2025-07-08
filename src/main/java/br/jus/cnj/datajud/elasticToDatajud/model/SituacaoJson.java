package br.jus.cnj.datajud.elasticToDatajud.model;

import java.io.Serializable;
import java.util.List;

public class SituacaoJson implements Serializable{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer seq;
    private Integer idSituacao;
    private Integer idSituacaoIniciar;
    private Integer idSituacaoFinalizar;
    private Long idMovimentacao;
    private Integer idMovimento;
    private Integer idClasse;
    private List<Integer> idAssunto;
    private Integer idOrgaoJulgador;
    private Integer idOrgaoJulgadorColegiado;
    private String cpfMagistrado;
    private Integer dataInicio;
    private Integer dataFim;

    
    public SituacaoJson(Integer seq, Integer idSituacao, Integer idSituacaoIniciar, Integer idSituacaoFinalizar, Long idMovimentacao, Integer idMovimento, 
        Integer idClasse, Integer idOrgaoJulgador, Integer idOrgaoJulgadorColegiado,String cpfMagistrado, Integer dataInicio, Integer dataFim, List<Integer> idAssunto) {
        this.seq = seq;
        this.idSituacao = idSituacao;
        this.idSituacaoIniciar = idSituacaoIniciar;
        this.idSituacaoFinalizar = idSituacaoFinalizar;
        this.idMovimentacao = idMovimentacao;
        this.idMovimento = idMovimento;
        this.idClasse = idClasse;
        this.idOrgaoJulgador = idOrgaoJulgador;
        this.idOrgaoJulgadorColegiado = idOrgaoJulgadorColegiado;
        this.cpfMagistrado = cpfMagistrado;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.idAssunto = idAssunto;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SituacaoJson other = (SituacaoJson) obj;
        if (getIdSituacao() == null) {
            if (other.getIdSituacao() != null) {
                return false;
            }
        } else if (!idSituacao.equals(other.idSituacao)) {
            return false;
        }
        if (getIdSituacaoIniciar() == null) {
            if (other.getIdSituacaoIniciar() != null) {
                return false;
            }
        } else if (!idSituacaoIniciar.equals(other.idSituacaoIniciar)) {
            return false;
        }
        if (getIdSituacaoFinalizar() == null) {
            if (other.getIdSituacaoFinalizar() != null) {
                return false;
            }
        } else if (!idSituacaoFinalizar.equals(other.idSituacaoFinalizar)) {
            return false;
        }
        if (getIdMovimentacao()== null) {
            if (other.getIdMovimentacao() != null) {
                return false;
            }
        } else if (!idMovimentacao.equals(other.idMovimentacao)) {
            return false;
        }
        if (getIdMovimento()== null) {
            if (other.getIdMovimento() != null) {
                return false;
            }
        } else if (!idMovimento.equals(other.idMovimento)) {
            return false;
        }
        if (getCpfMagistrado()== null) {
            if (other.getCpfMagistrado() != null) {
                return false;
            }
        } else if (!cpfMagistrado.equals(other.cpfMagistrado)) {
            return false;
        }
        if (getIdClasse()== null) {
            if (other.getIdClasse()!= null) {
                return false;
            }
        } else if (!idClasse.equals(other.idClasse)) {
            return false;
        }
        if (getIdOrgaoJulgador()== null) {
            if (other.getIdOrgaoJulgador()!= null) {
                return false;
            }
        } else if (!idOrgaoJulgador.equals(other.idOrgaoJulgador)) {
            return false;
        }
        if (getIdOrgaoJulgadorColegiado()== null) {
            if (other.getIdOrgaoJulgadorColegiado()!= null) {
                return false;
            }
        } else if (!idOrgaoJulgadorColegiado.equals(other.idOrgaoJulgadorColegiado)) {
            return false;
        }
        if (getDataInicio()== null) {
            if (other.getDataInicio()!= null) {
                return false;
            }
        } else if (!dataInicio.equals(other.dataInicio)) {
            return false;
        }
        if (getDataFim()== null) {
            if (other.getDataFim()!= null) {
                return false;
            }
        } else if (!dataFim.equals(other.dataFim)) {
            return false;
        }
        if (getIdAssunto()== null) {
            if (other.getIdAssunto()!= null) {
                return false;
            }
        } else if (!idAssunto.equals(other.idAssunto)) {
            return false;
        }
        return true;
    }
    
    /**
     * @return the seq
     */
    public Integer getSeq() {
        return seq;
    }

    /**
     * @param seq the seq to set
     */
    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    /**
     * @return the idSituacao
     */
    public Integer getIdSituacao() {
        return idSituacao;
    }

    /**
     * @param idSituacao the idSituacao to set
     */
    public void setIdSituacao(Integer idSituacao) {
        this.idSituacao = idSituacao;
    }

    /**
     * @return the idSituacaoIniciar
     */
    public Integer getIdSituacaoIniciar() {
        return idSituacaoIniciar;
    }

    /**
     * @param idSituacaoIniciar the idSituacaoIniciar to set
     */
    public void setIdSituacaoIniciar(Integer idSituacaoIniciar) {
        this.idSituacaoIniciar = idSituacaoIniciar;
    }

    /**
     * @return the idSituacaoFinalizar
     */
    public Integer getIdSituacaoFinalizar() {
        return idSituacaoFinalizar;
    }

    /**
     * @param idSituacaoFinalizar the idSituacaoFinalizar to set
     */
    public void setIdSituacaoFinalizar(Integer idSituacaoFinalizar) {
        this.idSituacaoFinalizar = idSituacaoFinalizar;
    }

    /**
     * @return the idMovimentacao
     */
    public Long getIdMovimentacao() {
        return idMovimentacao;
    }

    /**
     * @param idMovimentacao the idMovimentacao to set
     */
    public void setIdMovimentacao(Long idMovimentacao) {
        this.idMovimentacao = idMovimentacao;
    }

    /**
     * @return the idMovimento
     */
    public Integer getIdMovimento() {
        return idMovimento;
    }

    /**
     * @param idMovimento the idMovimento to set
     */
    public void setIdMovimento(Integer idMovimento) {
        this.idMovimento = idMovimento;
    }

    /**
     * @return the idClasse
     */
    public Integer getIdClasse() {
        return idClasse;
    }

    /**
     * @param idClasse the idClasse to set
     */
    public void setIdClasse(Integer idClasse) {
        this.idClasse = idClasse;
    }

    /**
     * @return the idAssunto
     */
    public List<Integer> getIdAssunto() {
        return idAssunto;
    }

    /**
     * @param idAssunto the idAssunto to set
     */
    public void setIdAssunto(List<Integer> idAssunto) {
        this.idAssunto = idAssunto;
    }

    /**
     * @return the idOrgaoJulgador
     */
    public Integer getIdOrgaoJulgador() {
        return idOrgaoJulgador;
    }

    /**
     * @param idOrgaoJulgador the idOrgaoJulgador to set
     */
    public void setIdOrgaoJulgador(Integer idOrgaoJulgador) {
        this.idOrgaoJulgador = idOrgaoJulgador;
    }

    public void setIdOrgaoJulgadorColegiado(Integer idOrgaoJulgadorColegiado) {
        this.idOrgaoJulgadorColegiado = idOrgaoJulgadorColegiado;
    }
    
    public Integer getIdOrgaoJulgadorColegiado() {
        return idOrgaoJulgadorColegiado;
    }  
    
    public String getCpfMagistrado() {
        return cpfMagistrado;
    }

    public void setCpfMagistrado(String cpfMagistrado) {
        this.cpfMagistrado = cpfMagistrado;
    }

    /**
     * @return the dataInicio
     */
    public Integer getDataInicio() {
        return dataInicio;
    }

    /**
     * @param dataInicio the dataInicio to set
     */
    public void setDataInicio(Integer dataInicio) {
        this.dataInicio = dataInicio;
    }

    /**
     * @return the dataFim
     */
    public Integer getDataFim() {
        return dataFim;
    }

    /**
     * @param dataFim the dataFim to set
     */
    public void setDataFim(Integer dataFim) {
        this.dataFim = dataFim;
    }
    
    public String toString() {
    	return "[Seq: "+this.seq+", IdSituacao: "+ this.idSituacao +", IdSituacaoIniciar: "+ this.idSituacaoIniciar +
    			", IdMovimentacao: "+this.idMovimentacao+", IdMovimento: "+ this.idMovimento +", IdClasse: "+ this.idClasse + ", IdOrgaoJulgador"+ this.idOrgaoJulgador+
    			", CpfMagistrado: "+this.cpfMagistrado+", DataInicio: "+ this.dataInicio +", DataFim: "+ this.dataFim + ", IdAssunto"+ this.idAssunto+"]";
    }
}
