package br.jus.cnj.datajud.elasticToDatajud.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "classe")
public class Classe {

    @Id
    private Integer id;
    
    @Column(name = "id_pai")
    private Integer idPai;
    
    @Column(name = "nome")
    private String nome;
    
    @Column(name = "id_fase_processual")
    private Integer idFase;
    
    @Column(name = "id_tipo_procedimento")
    private Integer idTipoProcedimento;
    
    @Column(name = "id_natureza_procedimento")
    private Integer idNatureza;
    
    @Column(name = "criminal")
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
}
