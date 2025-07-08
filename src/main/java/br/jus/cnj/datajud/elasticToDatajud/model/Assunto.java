package br.jus.cnj.datajud.elasticToDatajud.model;

public class Assunto {
	private Integer id;
	private Integer idPai;
	private String nome;
	private Boolean criminal;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Integer getIdPai() {
		return idPai;
	}
	public void setIdPai(Integer idPai) {
		this.idPai = idPai;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public Boolean getCriminal() {
		return criminal;
	}
	public void setCriminal(Boolean criminal) {
		this.criminal = criminal;
	}
}
