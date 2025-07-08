package br.jus.cnj.datajud.elasticToDatajud.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table (name="parametro")
public class Parametro implements Serializable {
	
	private static final long serialVersionUID = -9014604573220641472L;

	@Id
	@Column(name="chave")
	private String chave;
	
	@Column(name="valor")
	private String valor;
	
	@Column(name="parte")
	private String parte;
	
	@Column(name="seeu")
	private String seeu;
	
	@Column(name="indicador")
	private String indicador;

	@Column(name="descricao")
	private String descricao;
	
	public String getChave() {
		return chave;
	}

	public void setChave(String chave) {
		this.chave = chave;
	}

	public String getValor() {
		return valor;
	}

	public void setValor(String valor) {
		this.valor = valor;
	}
	
	public String getParte() {
		return parte;
	}

	public void setParte(String parte) {
		this.parte = parte;
	}
	
	public String getSeeu() {
		return seeu;
	}

	public void setSeeu(String seeu) {
		this.seeu = seeu;
	}
	
	public String getIndicador() {
		return indicador;
	}

	public void setIndicador(String indicador) {
		this.indicador = indicador;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
	
}
