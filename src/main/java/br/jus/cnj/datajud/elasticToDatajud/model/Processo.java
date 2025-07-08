package br.jus.cnj.datajud.elasticToDatajud.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import br.jus.cnj.datajud.elasticToDatajud.util.Misc;

@Entity
@IdClass(ProcessoId.class)
public class Processo implements Serializable  {
	private static final long serialVersionUID = 9051428444497337372L;
	
	@Id
	@SequenceGenerator(name = "gen_processo", sequenceName = "sq_processo", allocationSize = 1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "gen_processo")
	private Long id;
	
	@Id
	@Column(name="id_tribunal")
	private Integer idTribunal;

	@Column(name="id_sistema")
	private Integer idSistema;
	
	@Column(name="id_formato")
	private Integer idFormato;
	
	@Column(name="id_grau")
	private Integer idGrau;
	
	@Column(name = "sigla_tribunal")
    private String siglaTribunal;
    
    @Column(name = "sigla_grau")
    private String siglaGrau;
        
    @Column(name = "nome_nivel_sigilo")
    private String nomeNivelSigilo;
	
	@Column(name="id_nivel_sigilo")
	private Integer idNivelSigilo;
	
	@Column(name="numero")
	private String numero;
	
	@Column(name="data_ajuizamento")
	private Date dataAjuizamento;

	@Column(name="data_ultima_movimentacao")
	private Date dataUltimaMovimentacao;

	@Column(name="qtd_pf_polo_ativo")
	private Integer qtdPessoaFisicaPoloAtivo;
	
	@Column(name="qtd_pj_polo_ativo")
	private Integer qtdPessoaJuridicaPoloAtivo;
	
	@Column(name="qtd_aut_polo_ativo")
	private Integer qtdAutoridadePoloAtivo;
	
	@Column(name="qtd_pf_polo_passivo")
	private Integer qtdPessoaFisicaPoloPassivo;
	
	@Column(name="qtd_pj_polo_passivo")
	private Integer qtdPessoaJuridicaPoloPassivo;
	
	@Column(name="qtd_aut_polo_passivo")
	private Integer qtdAutoridadePoloPassivo;
	
	@Column(name="qtd_pf_vitima")
	private Integer qtdPessoaFisicaVitima;
	
	@Type(type = "list-array")
	@Column( name = "id_fase_processual", columnDefinition = "integer[]" )
	private List<Integer> idFaseProcessual; 
	
	@Type(type = "list-array")
	@Column( name = "id_classe", columnDefinition = "integer[]" )
	private List<Integer> idClasse; 
	
	@Type(type = "list-array")
	@Column( name = "id_assunto", columnDefinition = "integer[]" )
	private List<Integer> idAssunto; 

	@Type(type = "list-array")
	@Column( name = "id_orgao_julgador", columnDefinition = "integer[]" )
	private List<Integer> idOrgaoJulgador; 
	
	@Type(type = "list-array")
	@Column( name = "id_orgao_julgador_colegiado", columnDefinition = "integer[]" )
	private List<Integer> idOrgaoJulgadorColegiado; 

	@Column(name="assistencia_judiciaria")
	private Boolean assistenciaJudiciaria;

	@Column(name="criminal")
	private Boolean criminal;

	@Column(name="valor_causa")
	private Double valorCausa; 

	@Column(name="data_cn")
	private Date dataCasoNovo;
	
	@Column(name="data_baixa")
	private Date dataBaixa;
	
	@Column(name="id_situacao_atual")
	private Integer idSituacaoAtual;
	
	@Column(name="data_situacao_atual")
	private Date dataSituacaoAtual;

	@Type(type = "list-array")
	@Column( name = "id_tipo_prioridade", columnDefinition = "string[]" )
	private List<String> idTipoPrioridade;
	
	@Column(name="flg_pedido_prioridade")
	private Boolean pedidoPrioridade;
	
	@Column(name="custas_iniciais")
	private Double custasIniciais;
	
	@Column(name="custas_recursais")
	private Double custasRecursais;
	
	@Column(name="custas_finais")
	private Double custasFinais;
	
	@Column(name="flg_nucleo_4")
	private Boolean nucleo4;
	
	@Column(name="flg_juizo_100_digital")
	private Boolean juizo100Digital;
	
	@Column(name="ano_eleicao")
	private Integer anoEleicao;
	
	@Column(name="millisinsercao")
	private Long millisinsercao;

	public Long getMillisinsercao() {
		return millisinsercao;
	}
	public void setMillisinsercao(Long millisinsercao) {
		this.millisinsercao = millisinsercao;
	}
	
	@Transient
    private Boolean registroUnico;
	
	public Boolean getRegistroUnico() {
		return registroUnico;
	}
	public void setRegistroUnico(Boolean registroUnico) {
		this.registroUnico = registroUnico;
	}

	public Date getDataCasoNovo() {
		return dataCasoNovo;
	}
	public void setDataCasoNovo(Date dataCasoNovo) {
		this.dataCasoNovo = dataCasoNovo;
	}
	public Date getDataBaixa() {
		return dataBaixa;
	}
	public void setDataBaixa(Date dataBaixa) {
		this.dataBaixa = dataBaixa;
	}
	public Integer getIdSituacaoAtual() {
		return idSituacaoAtual;
	}
	public void setIdSituacaoAtual(Integer idSituacaoAtual) {
		this.idSituacaoAtual = idSituacaoAtual;
	}
	public Date getDataSituacaoAtual() {
		return dataSituacaoAtual;
	}
	public void setDataSituacaoAtual(Date dataSituacaoAtual) {
		this.dataSituacaoAtual = dataSituacaoAtual;
	}
	public List<String> getIdTipoPrioridade() {
		return idTipoPrioridade;
	}
	public void setIdTipoPrioridade(List<String> idTipoPrioridade) {
		this.idTipoPrioridade = idTipoPrioridade;
	}
	public Boolean getPedidoPrioridade() {
		return pedidoPrioridade;
	}
	public void setPedidoPrioridade(Boolean pedidoPrioridade) {
		this.pedidoPrioridade = pedidoPrioridade;
	}
	public Double getCustasIniciais() {
		return custasIniciais;
	}
	public void setCustasIniciais(Double custasIniciais) {
		this.custasIniciais = custasIniciais;
	}
	public Double getCustasRecursais() {
		return custasRecursais;
	}
	public void setCustasRecursais(Double custasRecursais) {
		this.custasRecursais = custasRecursais;
	}
	public Double getCustasFinais() {
		return custasFinais;
	}
	public void setCustasFinais(Double custasFinais) {
		this.custasFinais = custasFinais;
	}
	public Boolean getNucleo4() {
		return nucleo4;
	}
	public void setNucleo4(Boolean nucleo4) {
		this.nucleo4 = nucleo4;
	}
	public Boolean getJuizo100Digital() {
		return juizo100Digital;
	}
	public void setJuizo100Digital(Boolean juizo100Digital) {
		this.juizo100Digital = juizo100Digital;
	}
	public Integer getAnoEleicao() {
		return anoEleicao;
	}
	public void setAnoEleicao(Integer anoEleicao) {
		this.anoEleicao = anoEleicao;
	}
	
	public Integer getIdOrgaoJulgadorUltimo() {
		return idOrgaoJulgadorUltimo;
	}
	public void setIdOrgaoJulgadorUltimo(Integer idOrgaoJulgadorUltimo) {
		this.idOrgaoJulgadorUltimo = idOrgaoJulgadorUltimo;
	}
	public Integer getIdClasseUltimaFase1() {
		return idClasseUltimaFase1;
	}
	public void setIdClasseUltimaFase1(Integer idClasseUltimaFase1) {
		this.idClasseUltimaFase1 = idClasseUltimaFase1;
	}
	public Integer getIdClasseUltimaFase2() {
		return idClasseUltimaFase2;
	}
	public void setIdClasseUltimaFase2(Integer idClasseUltimaFase2) {
		this.idClasseUltimaFase2 = idClasseUltimaFase2;
	}
	public Boolean getFlgForaRecorte() {
		return flgForaRecorte;
	}
	public void setFlgForaRecorte(Boolean flgForaRecorte) {
		this.flgForaRecorte = flgForaRecorte;
	}

	@Column(name="id_orgao_julgador_ultimo")
	private Integer idOrgaoJulgadorUltimo;
	
	@Column(name="id_classe_ultima_fase_1")
	private Integer idClasseUltimaFase1;
	
	@Column(name="id_classe_ultima_fase_2")
	private Integer idClasseUltimaFase2;
	
	@Column(name="flg_fora_recorte")
	private Boolean flgForaRecorte;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Integer getIdSistema() {
		return idSistema;
	}
	public void setIdSistema(Integer idSistema) {
		this.idSistema = idSistema;
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
	public Integer getIdNivelSigilo() {
		return idNivelSigilo;
	}
	public void setIdNivelSigilo(Integer idNivelSigilo) {
		this.idNivelSigilo = idNivelSigilo;
	}
	public String getNumero() {
		return numero;
	}
	public void setNumero(String numero) {
		this.numero = numero;
	}
	public Date getDataAjuizamento() {
		return dataAjuizamento;
	}
	public void setDataAjuizamento(Date dataAjuizamento) {
		this.dataAjuizamento = dataAjuizamento;
	}
	public Date getDataUltimaMovimentacao() {
		return dataUltimaMovimentacao;
	}
	public void setDataUltimaMovimentacao(Date dataUltimaMovimentacao) {
		this.dataUltimaMovimentacao = dataUltimaMovimentacao;
	}
	public Integer getQtdPessoaFisicaPoloAtivo() {
		return qtdPessoaFisicaPoloAtivo;
	}
	public void setQtdPessoaFisicaPoloAtivo(Integer qtdPessoaFisicaPoloAtivo) {
		this.qtdPessoaFisicaPoloAtivo = qtdPessoaFisicaPoloAtivo;
	}
	public Integer getQtdPessoaJuridicaPoloAtivo() {
		return qtdPessoaJuridicaPoloAtivo;
	}
	public void setQtdPessoaJuridicaPoloAtivo(Integer qtdPessoaJuridicaPoloAtivo) {
		this.qtdPessoaJuridicaPoloAtivo = qtdPessoaJuridicaPoloAtivo;
	}
	public Integer getQtdAutoridadePoloAtivo() {
		return qtdAutoridadePoloAtivo;
	}
	public void setQtdAutoridadePoloAtivo(Integer qtdAutoridadePoloAtivo) {
		this.qtdAutoridadePoloAtivo = qtdAutoridadePoloAtivo;
	}
	public Integer getQtdPessoaFisicaPoloPassivo() {
		return qtdPessoaFisicaPoloPassivo;
	}
	public void setQtdPessoaFisicaPoloPassivo(Integer qtdPessoaFisicaPoloPassivo) {
		this.qtdPessoaFisicaPoloPassivo = qtdPessoaFisicaPoloPassivo;
	}
	public Integer getQtdPessoaJuridicaPoloPassivo() {
		return qtdPessoaJuridicaPoloPassivo;
	}
	public void setQtdPessoaJuridicaPoloPassivo(Integer qtdPessoaJuridicaPoloPassivo) {
		this.qtdPessoaJuridicaPoloPassivo = qtdPessoaJuridicaPoloPassivo;
	}
	public Integer getQtdAutoridadePoloPassivo() {
		return qtdAutoridadePoloPassivo;
	}
	public void setQtdAutoridadePoloPassivo(Integer qtdAutoridadePoloPassivo) {
		this.qtdAutoridadePoloPassivo = qtdAutoridadePoloPassivo;
	}
	public Integer getQtdPessoaFisicaVitima() {
		return qtdPessoaFisicaVitima;
	}
	public void setQtdPessoaFisicaVitima(Integer qtdPessoaFisicaVitima) {
		this.qtdPessoaFisicaVitima = qtdPessoaFisicaVitima;
	}
	public List<Integer> getIdFaseProcessual() {
		return idFaseProcessual;
	}
	public void setIdFaseProcessual(List<Integer> idFaseProcessual) {
		this.idFaseProcessual = idFaseProcessual;
	}

	public List<Integer> getIdClasse() {
		return idClasse;
	}
	public void setIdClasse(List<Integer> idClasse) {
		this.idClasse = idClasse;
	}

	public List<Integer> getIdAssunto() {
		return idAssunto;
	}
	public void setIdAssunto(List<Integer> idAssunto) {
		this.idAssunto = idAssunto;
	}

	public List<Integer> getIdOrgaoJulgador() {
		return idOrgaoJulgador;
	}
	public void setIdOrgaoJulgador(List<Integer> idOrgaoJulgador) {
		this.idOrgaoJulgador = idOrgaoJulgador;
	}

	public List<Integer> getIdOrgaoJulgadorColegiado() {
		return idOrgaoJulgadorColegiado;
	}
	public void setIdOrgaoJulgadorColegiado(List<Integer> idOrgaoJulgadorColegiado) {
		this.idOrgaoJulgadorColegiado = idOrgaoJulgadorColegiado;
	}
	
	public Boolean getAssistenciaJudiciaria() {
		return assistenciaJudiciaria;
	}
	public void setAssistenciaJudiciaria(Boolean assistenciaJudiciaria) {
		this.assistenciaJudiciaria = assistenciaJudiciaria;
	}

	public Boolean getCriminal() {
		return criminal;
	}
	public void setCriminal(Boolean criminal) {
		this.criminal = criminal;
	}

	public Double getValorCausa() {
		return valorCausa;
	}
	public void setValorCausa(Double valorCausa) {
		this.valorCausa = valorCausa;
	}

	@Override
    public String toString() {
		String idTribunal = this.idTribunal == null ? "" : this.idTribunal.toString();
		String idGrau = this.idGrau == null ? "" : this.idGrau.toString();
		String numero = this.numero == null ? "" : this.numero;
        return String.format("%s:%s:%s", idTribunal, idGrau, numero);
    }
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((idGrau == null) ? 0 : idGrau.hashCode());
		result = prime * result + ((idTribunal == null) ? 0 : idTribunal.hashCode());
		result = prime * result + ((numero == null) ? 0 : numero.hashCode());
		result = prime * result + ((qtdPessoaFisicaPoloAtivo == null) ? 0 : qtdPessoaFisicaPoloAtivo.hashCode());
		result = prime * result + ((qtdPessoaFisicaPoloPassivo == null) ? 0 : qtdPessoaFisicaPoloPassivo.hashCode());
		result = prime * result + ((qtdPessoaFisicaVitima == null) ? 0 : qtdPessoaFisicaVitima.hashCode());
		result = prime * result + ((qtdPessoaJuridicaPoloAtivo == null) ? 0 : qtdPessoaJuridicaPoloAtivo.hashCode());
		result = prime * result	+ ((qtdPessoaJuridicaPoloPassivo == null) ? 0 : qtdPessoaJuridicaPoloPassivo.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Processo other = (Processo) obj;
		if (dataAjuizamento == null) {
			if (other.dataAjuizamento != null)
				return false;
		} else if (!Misc.dateToString(dataAjuizamento,"yyyyMMddHHmmss").equals(Misc.dateToString(other.dataAjuizamento,"yyyyMMddHHmmss")))
			return false;
		if (idFormato == null) {
			if (other.idFormato != null)
				return false;
		} else if (!idFormato.equals(other.idFormato))
			return false;
		if (idGrau == null) {
			if (other.idGrau != null)
				return false;
		} else if (!idGrau.equals(other.idGrau))
			return false;
		if (idNivelSigilo == null) {
			if (other.idNivelSigilo != null)
				return false;
		} else if (!idNivelSigilo.equals(other.idNivelSigilo))
			return false;
		if (idSistema == null) {
			if (other.idSistema != null)
				return false;
		} else if (!idSistema.equals(other.idSistema))
			return false;
		if (idTribunal == null) {
			if (other.idTribunal != null)
				return false;
		} else if (!idTribunal.equals(other.idTribunal))
			return false;
		if (numero == null) {
			if (other.numero != null)
				return false;
		} else if (!numero.equals(other.numero))
			return false;
		if (qtdAutoridadePoloAtivo == null) {
			if (other.qtdAutoridadePoloAtivo != null)
				return false;
		} else if (!qtdAutoridadePoloAtivo.equals(other.qtdAutoridadePoloAtivo))
			return false;
		if (qtdAutoridadePoloPassivo == null) {
			if (other.qtdAutoridadePoloPassivo != null)
				return false;
		} else if (!qtdAutoridadePoloPassivo.equals(other.qtdAutoridadePoloPassivo))
			return false;
		if (qtdPessoaFisicaPoloAtivo == null) {
			if (other.qtdPessoaFisicaPoloAtivo != null)
				return false;
		} else if (!qtdPessoaFisicaPoloAtivo.equals(other.qtdPessoaFisicaPoloAtivo))
			return false;
		if (qtdPessoaFisicaPoloPassivo == null) {
			if (other.qtdPessoaFisicaPoloPassivo != null)
				return false;
		} else if (!qtdPessoaFisicaPoloPassivo.equals(other.qtdPessoaFisicaPoloPassivo))
			return false;
		if (qtdPessoaFisicaVitima == null) {
			if (other.qtdPessoaFisicaVitima != null)
				return false;
		} else if (!qtdPessoaFisicaVitima.equals(other.qtdPessoaFisicaVitima))
			return false;
		if (qtdPessoaJuridicaPoloAtivo == null) {
			if (other.qtdPessoaJuridicaPoloAtivo != null)
				return false;
		} else if (!qtdPessoaJuridicaPoloAtivo.equals(other.qtdPessoaJuridicaPoloAtivo))
			return false;
		if (qtdPessoaJuridicaPoloPassivo == null) {
			if (other.qtdPessoaJuridicaPoloPassivo != null)
				return false;
		} else if (!qtdPessoaJuridicaPoloPassivo.equals(other.qtdPessoaJuridicaPoloPassivo))
			return false;
		return true;
	}
	
	public String getSiglaGrau() {
        return siglaGrau;
    }

    public void setSiglaGrau(String siglaGrau) {
        this.siglaGrau = siglaGrau;
    }

    public String getSiglaTribunal() {
        return siglaTribunal;
    }

    public void setSiglaTribunal(String siglaTribunal) {
        this.siglaTribunal = siglaTribunal;
    }

    public String getNomeNivelSigilo() {
        return nomeNivelSigilo;
    }

    public void setNomeNivelSigilo(String nomeNivelSigilo) {
        this.nomeNivelSigilo = nomeNivelSigilo;
    }
}