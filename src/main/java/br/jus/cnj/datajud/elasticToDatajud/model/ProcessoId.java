package br.jus.cnj.datajud.elasticToDatajud.model;

import java.io.Serializable;

public class ProcessoId implements Serializable {
	private static final long serialVersionUID = -8785015906387670409L;
	private Long id;
	private Integer idTribunal;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Integer getIdTribunal() {
		return idTribunal;
	}
	public void setIdTribunal(Integer idTribunal) {
		this.idTribunal = idTribunal;
	}
}
