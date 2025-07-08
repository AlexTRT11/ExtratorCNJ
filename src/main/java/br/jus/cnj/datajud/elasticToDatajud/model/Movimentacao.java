package br.jus.cnj.datajud.elasticToDatajud.model;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import br.jus.cnj.datajud.elasticToDatajud.util.Misc;

import java.util.Date;

@Entity
@IdClass(MovimentacaoId.class)
@Table(name = "movimentacao")
@TypeDef(name = "list-array", typeClass = ListArrayType.class)
public class Movimentacao implements Serializable, Comparable<Movimentacao> {

    private static final long serialVersionUID = 1718802861309103664L;

    @Id
    @SequenceGenerator(name = "gen_movimentacao", sequenceName = "sq_movimentacao", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen_movimentacao")
    private Long id;

    @Id
    @Column(name = "id_tribunal")
    private Integer idTribunal;

    @Column(name = "id_movimento")
    private Integer idMovimento;

    @Column(name = "id_processo")
    private Long idProcesso;

    @Column(name = "id_classe")
    private Integer idClasse;

    @Column(name = "id_formato")
    private Integer idFormato;

    @Column(name = "id_grau")
    private Integer idGrau;

    @Type(type = "list-array")
    @Column(name = "id_assunto", columnDefinition = "integer[]")
    private List<Integer> idAssunto;

    @Column(name = "id_orgao_julgador")
    private Integer idOrgaoJulgador;
    
    @Column(name = "id_orgao_julgador_colegiado")
    private Integer idOrgaoJulgadorColegiado;

    @Column(name = "cpf_magistrado")
    private String cpfMagistrado;

    @Column(name = "data")
    private Integer data;

    @Column(name = "horario")
    private String horario;

    @Column(name = "complemento")
    private String complemento;

    @Column(name = "id_situacao")
    private Integer idSituacao;

    @Transient
    private Situacao situacao;

    @Transient
    private String identificadorMovimento;

    @Column(name = "dt_inicio_situacao")
    private Integer dataInicioSituacao;

    @Column(name = "dt_fim_situacao")
    private Integer dataFimSituacao;

    @Column(name = "id_situacao_iniciar")
    private Integer idSituacaoIniciar;

    @Column(name = "id_situacao_finalizar")
    private Integer idSituacaoFinalizar;

    @Column(name = "id_fase_processual")
    private Integer idFase;

    @Column(name = "id_tipo_procedimento")
    private Integer idTipoProcedimento;

    @Column(name = "id_natureza_procedimento")
    private Integer idNatureza;

    @Column(name = "criminal")
    private Boolean criminal;
    
    @Column(name = "flg_cancelado")
    private Boolean cancelado;
    
    @Column(name = "flg_anulado")
    private Boolean anulado;

    public Boolean getCancelado() {
		return cancelado;
	}

	public void setCancelado(Boolean cancelado) {
		this.cancelado = cancelado;
	}

	public Boolean getAnulado() {
		return anulado;
	}

	public void setAnulado(Boolean anulado) {
		this.anulado = anulado;
	}

	@Transient
    private Boolean finalizouSituacoes = Boolean.FALSE;

    @Transient
    private Boolean persistir = Boolean.FALSE;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getIdMovimento() {
        return idMovimento;
    }

    public void setIdMovimento(Integer idMovimento) {
        this.idMovimento = idMovimento;
    }

    public Long getIdProcesso() {
        return idProcesso;
    }

    public void setIdProcesso(Long idProcesso) {
        this.idProcesso = idProcesso;
    }

    public Integer getIdClasse() {
        return idClasse;
    }

    public void setIdClasse(Integer idClasse) {
        this.idClasse = idClasse;
    }

    public Integer getIdFormato() {
        return idFormato;
    }

    public void setIdFormato(Integer idFormato) {
        this.idFormato = idFormato;
    }

    public Integer getIdTribunal() {
        return idTribunal;
    }

    public void setIdTribunal(Integer idTribunal) {
        this.idTribunal = idTribunal;
    }

    public Integer getIdGrau() {
        return idGrau;
    }

    public void setIdGrau(Integer idGrau) {
        this.idGrau = idGrau;
    }

    public List<Integer> getIdAssunto() {
        return idAssunto;
    }

    public void setIdAssunto(List<Integer> idAssunto) {
        this.idAssunto = idAssunto;
    }

    public Integer getIdSituacao() {
        return idSituacao;
    }

    public void setIdSituacao(Integer idSituacao) {
        this.idSituacao = idSituacao;
    }

    public Integer getIdOrgaoJulgador() {
        return idOrgaoJulgador;
    }

    public void setIdOrgaoJulgador(Integer idOrgaoJulgador) {
        this.idOrgaoJulgador = idOrgaoJulgador;
    }
    
    public Integer getIdOrgaoJulgadorColegiado() {
        return idOrgaoJulgadorColegiado;
    }

    public void setIdOrgaoJulgadorColegiado(Integer idOrgaoJulgadorColegiado) {
        this.idOrgaoJulgadorColegiado = idOrgaoJulgadorColegiado;
    }

    public String getCpfMagistrado() {
        return cpfMagistrado;
    }

    public void setCpfMagistrado(String cpfMagistrado) {
        this.cpfMagistrado = cpfMagistrado;
    }

    public Integer getData() {
        return data;
    }

    public void setData(Integer data) {
        this.data = data;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public Integer getDataInicioSituacao() {
        return dataInicioSituacao;
    }

    public void setDataInicioSituacao(Integer dataInicioSituacao) {
        this.dataInicioSituacao = dataInicioSituacao;
    }

    public Integer getDataFimSituacao() {
        return dataFimSituacao;
    }

    public void setDataFimSituacao(Integer dataFimSituacao) {
        this.dataFimSituacao = dataFimSituacao;
    }

    public Integer getIdSituacaoIniciar() {
        return idSituacaoIniciar;
    }

    public void setIdSituacaoIniciar(Integer idSituacaoIniciar) {
        this.idSituacaoIniciar = idSituacaoIniciar;
    }

    public Integer getIdSituacaoFinalizar() {
        return idSituacaoFinalizar;
    }

    public void setIdSituacaoFinalizar(Integer idSituacaoFinalizar) {
        this.idSituacaoFinalizar = idSituacaoFinalizar;
    }

    public Boolean getFinalizouSituacoes() {
        return finalizouSituacoes;
    }

    public void setFinalizouSituacoes(Boolean finalizouSituacoes) {
        this.finalizouSituacoes = finalizouSituacoes;
    }

    public Boolean getPersistir() {
        return persistir;
    }

    public void setPersistir(Boolean persistir) {
        this.persistir = persistir;
    }

    private int compareToDataHorario(Movimentacao o) {
        String first = String.valueOf(this.getData()) + this.getHorario();
        String second = String.valueOf(o.getData()) + o.getHorario();
        return (Misc.stringToLong(first)).compareTo(Misc.stringToLong(second));
    }

    private int compareToIdentificadorMovimento(Movimentacao o) {
    	String first = this.getIdentificadorMovimento() == null || !Misc.isLong(this.getIdentificadorMovimento()) ? "0" : this.getIdentificadorMovimento();
        String second = o.getIdentificadorMovimento() == null || !Misc.isLong(o.getIdentificadorMovimento()) ? "0" : o.getIdentificadorMovimento();
        return Misc.stringToLong(first).compareTo(Misc.stringToLong(second));
    }

    private int compareToIdSituacaoIniciar(Movimentacao o) {
        Integer first = this.getIdSituacaoIniciar() == null || this.getIdSituacaoIniciar() == 38 ? 0 : this.getIdSituacaoIniciar();
        Integer second = o.getIdSituacaoIniciar() == null || o.getIdSituacaoIniciar() == 38  ? 0 : o.getIdSituacaoIniciar();
        return first.compareTo(second);
    }
    
    private int compareToIdSituacao(Movimentacao o) {
        Integer first = this.getIdSituacao() == null ? 0 : this.getIdSituacao();
        Integer second = o.getIdSituacao() == null ? 0 : o.getIdSituacao();
        return first.compareTo(second);
    }

    @Override
    public String toString() {
        String idProcesso = this.idProcesso == null ? "" : this.idProcesso.toString();
        String idMovimento = this.idMovimento == null ? "" : this.idMovimento.toString();
        String idSituacao = this.idSituacao == null ? "" : this.idSituacao.toString();
        String data = this.data == null ? "" : this.data.toString();
        String hora = this.horario == null ? "" : this.horario;
        String identificadorMovimento = this.identificadorMovimento == null ? "" : this.identificadorMovimento;
        String complemento = this.complemento == null ? "" : this.complemento;
        return String.format("{\"idProcesso\":%s, \"idMovimento\":%s, \"idSituacao\":%s, \"data\":%s, \"hora\":%s, \"identificador\":%s, \"complemento\":%s}", idProcesso, idMovimento, idSituacao, data, hora, identificadorMovimento, complemento);
    }

    @Override
    public int compareTo(Movimentacao o) {
        int res = compareToDataHorario(o);
        if (res == 0) {
            res = compareToIdentificadorMovimento(o);
        }
        if (res == 0) {
            res = compareToIdSituacaoIniciar(o);
        }
        if (res == 0) {
            res = compareToIdSituacao(o);
        }
        return res;
    }

    @Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Movimentacao other = (Movimentacao) obj;
		if(partialEquals(obj)) {
			if (complemento == null && other.complemento != null) {
				return false;
			} else if (!complemento.equals(other.complemento)) {
				return false;
			}
		}
		else {
			return false;
		}
		return true;
	}
    
    public boolean partialEquals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Movimentacao other = (Movimentacao) obj;
        if (idTribunal == null) {
            if (other.idTribunal != null) {
                return false;
            }
        } else if (!idTribunal.equals(other.idTribunal)) {
            return false;
        }
        if (idProcesso == null) {
            if (other.idProcesso != null) {
                return false;
            }
        } else if (!idProcesso.equals(other.idProcesso)) {
            return false;
        }
        if (idMovimento == null) {
            if (other.idMovimento != null) {
                return false;
            }
        } else if (!idMovimento.equals(other.idMovimento)) {
            return false;
        }
        if (idFase == null) {
	        if (other.idFase != null) {
	            return false;
	        }
	    } else if (!idFase.equals(other.idFase)) {
	        return false;
	    }
        if (data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!data.equals(other.data)) {
            return false;
        }
        if (dataInicioSituacao == null) {
            if (other.dataInicioSituacao != null) {
                return false;
            }
        } else if (!dataInicioSituacao.equals(other.dataInicioSituacao)) {
            return false;
        }
        if (dataFimSituacao == null) {
            if (other.dataFimSituacao != null) {
                return false;
            }
        } else if (!dataFimSituacao.equals(other.dataFimSituacao)) {
            return false;
        }
        if (horario == null) {
            if (other.horario != null)
                return false;
        } else if (!horario.equals(other.horario))
            return false;
        if (idSituacao == null) {
            if (other.idSituacao != null) {
                return false;
            }
        } else if (!idSituacao.equals(other.idSituacao)) {
            return false;
        }
        if (complemento == null) {
            if (other.complemento != null) {
                return false;
            }
        } else if (!complemento.equals(other.complemento)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((idTribunal == null) ? 0 : idTribunal.hashCode());
        result = prime * result + ((idProcesso == null) ? 0 : idProcesso.hashCode());
        result = prime * result + ((idMovimento == null) ? 0 : idMovimento.hashCode());
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((horario == null) ? 0 : horario.hashCode());
        result = prime * result + ((idSituacao == null) ? 0 : idSituacao.hashCode());
        //result = prime * result + ((idFase == null) ? 0 : idFase.hashCode());
        result = prime * result + ((dataFimSituacao == null) ? 0 : dataFimSituacao.hashCode());
        result = prime * result + ((complemento == null) ? 0 : complemento.hashCode());
        return result;
    }

    public Situacao getSituacao() {
        return situacao;
    }

    public void setSituacao(Situacao situacao) {
        this.situacao = situacao;
    }

    public String getIdentificadorMovimento() {
        return identificadorMovimento;
    }

    public void setIdentificadorMovimento(String identificadorMovimento) {
        this.identificadorMovimento = identificadorMovimento;
    }

    public Integer getIdFase() {
        return idFase;
    }

    public void setIdFase(Integer idFase) {
        this.idFase = idFase;
    }

    public Integer getIdTipoProcedimento() {
        return idTipoProcedimento;
    }

    public void setIdTipoProcedimento(Integer idTipoProcedimento) {
        this.idTipoProcedimento = idTipoProcedimento;
    }

    public Integer getIdNatureza() {
        return idNatureza;
    }

    public void setIdNatureza(Integer idNatureza) {
        this.idNatureza = idNatureza;
    }

    public Boolean getCriminal() {
        return criminal;
    }

    public void setCriminal(Boolean criminal) {
        this.criminal = criminal;
    }

    public Date getDataHora() {
        if (this.data == null) {
            throw new RuntimeException("Data da movimentação não preenchida");
        }
        if (this.data.toString().length() != 8) {
            throw new RuntimeException("Formato da data de movimentação inválido");
        }
        if (this.horario == null) {
            throw new RuntimeException("Hora da movimentação não preenchido");
        }
        if (this.horario.trim().length() != 6) {
            throw new RuntimeException("Formato da hora de movimentação inválido");
        }
        return Misc.stringToDateTime(this.data.toString() + this.horario);
    }

    public Date getDataHoraFimSituacao() {
        Date dataHoraFimSituacao = Misc.stringToDateTime("99990909000000");
        if (this.dataFimSituacao == null) {
            throw new RuntimeException("Data de finalização da situação não preenchida");
        }
        if (this.dataFimSituacao.intValue() > 0) {
            if (this.dataFimSituacao.toString().length() != 8) {
                throw new RuntimeException("Formato da data de finalização da situação inválido");
            } else {
                dataHoraFimSituacao = Misc.stringToDateTime(this.dataFimSituacao.toString() + "000000");
            }
        }
        return dataHoraFimSituacao;
    }
}