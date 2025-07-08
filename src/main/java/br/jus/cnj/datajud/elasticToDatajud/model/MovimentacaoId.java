package br.jus.cnj.datajud.elasticToDatajud.model;

import java.io.Serializable;

public class MovimentacaoId implements Serializable {
	private static final long serialVersionUID = 1316792222956854210L;
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
