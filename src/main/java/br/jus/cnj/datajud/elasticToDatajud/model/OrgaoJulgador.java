package br.jus.cnj.datajud.elasticToDatajud.model;

import java.text.Collator;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class OrgaoJulgador implements Comparable<OrgaoJulgador>{

    private Integer id;

    private Integer id_tribunal;

    private String nome;

    private String sigla_tribunal;

	private String nome_tribunal;

    private Integer id_segmento_justica;

    private String nome_segmento_justica;

    private Integer id_municipio;

    private String nome_municipio;
    
    @JsonIgnore
    private Double lat;
    
    @JsonIgnore
    private Double lon;

    private String uf;
    
    private String situacao;
    
    private String dsc_telefone_balcao;
    
    private String dsc_link_balcao;
    
    private String dsc_email;
    
    @JsonIgnore
    private String seq_lista_municipio_abrangido;
    
    private String dsc_endereco;
    
    private String cep;
    
    private Integer seq_tipo_unidade_judiciaria;
    
    private Integer seq_classificacao_unid_judiciaria;

    @Override
    public String toString() {
        String id = this.id == null ? "" : this.id.toString();
        String idTribunal = this.id_tribunal == null ? "" : this.id_tribunal.toString();
        String nome = this.nome == null ? "" : this.nome;
        return String.format("%s:%s:%s", id, idTribunal, nome);
    }

    public OrgaoJulgador() {

    }

    public OrgaoJulgador(Integer id, Integer idTribunal, String nome) {
        this.id = id;
        this.id_tribunal = idTribunal;
        this.nome = nome;
    }

	public OrgaoJulgador(Integer id, Integer idTribunal, Integer classificacaoUnidJudiciaria){
        this.id = id;
        this.id_tribunal = idTribunal;
        this.seq_classificacao_unid_judiciaria = classificacaoUnidJudiciaria;
    }
	
	public OrgaoJulgador(Integer id, Integer idTribunal, Integer classificacaoUnidJudiciaria, String nome, String nomeMunicipio, String siglaTribunal, Double latitude, Double longitude, String nomeSegmentoJustica){
        this.id = id;
        this.id_tribunal = idTribunal;
        this.seq_classificacao_unid_judiciaria = classificacaoUnidJudiciaria;
        this.nome = nome;
        this.nome_municipio = nomeMunicipio;
        this.sigla_tribunal = siglaTribunal;
        this.lat = latitude;
        this.lon = longitude;
        this.nome_segmento_justica = nomeSegmentoJustica;
    }
    
    public OrgaoJulgador(Integer id, String nome, Integer idTribunal, String siglaTribunal, String nomeTribunal, Integer idSegmentoJustica, String nomeSegmentoJustica, Integer idMunicipio, String nomeMunicipio, String uf, String situacao) {
        this.id = id;
        this.id_tribunal = idTribunal;
        this.nome = nome;
        this.sigla_tribunal = siglaTribunal;
        this.nome_tribunal = nomeTribunal;
        this.id_segmento_justica = idSegmentoJustica;
        this.nome_segmento_justica = nomeSegmentoJustica;
        this.id_municipio = idMunicipio;
        this.nome_municipio = nomeMunicipio;
        this.uf = uf;
        this.situacao = situacao;
    }
    
    @Override
    public int compareTo(OrgaoJulgador o) {
        Collator cot = Collator.getInstance(new Locale("pt", "BR"));
        if (o != null) {
             return cot.compare(this.getNome(), o.getNome());
            } else {
             return 0;
        }
    }
    
    public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getId_tribunal() {
		return id_tribunal;
	}

	public void setId_tribunal(Integer id_tribunal) {
		this.id_tribunal = id_tribunal;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getSigla_tribunal() {
		return sigla_tribunal;
	}

	public void setSigla_tribunal(String sigla_tribunal) {
		this.sigla_tribunal = sigla_tribunal;
	}

	public String getNome_tribunal() {
		return nome_tribunal;
	}

	public void setNome_tribunal(String nome_tribunal) {
		this.nome_tribunal = nome_tribunal;
	}

	public Integer getId_segmento_justica() {
		return id_segmento_justica;
	}

	public void setId_segmento_justica(Integer id_segmento_justica) {
		this.id_segmento_justica = id_segmento_justica;
	}

	public String getNome_segmento_justica() {
		return nome_segmento_justica;
	}

	public void setNome_segmento_justica(String nome_segmento_justica) {
		this.nome_segmento_justica = nome_segmento_justica;
	}

	public Integer getId_municipio() {
		return id_municipio;
	}

	public void setId_municipio(Integer id_municipio) {
		this.id_municipio = id_municipio;
	}

	public String getNome_municipio() {
		return nome_municipio;
	}

	public void setNome_municipio(String nome_municipio) {
		this.nome_municipio = nome_municipio;
	}

	public Double getLat() {
		return lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}

	public Double getLon() {
		return lon;
	}

	public void setLon(Double lon) {
		this.lon = lon;
	}

	public String getUf() {
		return uf;
	}

	public void setUf(String uf) {
		this.uf = uf;
	}

	public String getSituacao() {
		return situacao;
	}

	public void setSituacao(String situacao) {
		this.situacao = situacao;
	}

	public String getDsc_telefone_balcao() {
		return dsc_telefone_balcao;
	}

	public void setDsc_telefone_balcao(String dsc_telefone_balcao) {
		this.dsc_telefone_balcao = dsc_telefone_balcao;
	}

	public String getDsc_link_balcao() {
		return dsc_link_balcao;
	}

	public void setDsc_link_balcao(String dsc_link_balcao) {
		this.dsc_link_balcao = dsc_link_balcao;
	}

	public String getDsc_email() {
		return dsc_email;
	}

	public void setDsc_email(String dsc_email) {
		this.dsc_email = dsc_email;
	}

	public String getSeq_lista_municipio_abrangido() {
		return seq_lista_municipio_abrangido;
	}

	public void setSeq_lista_municipio_abrangido(String seq_lista_municipio_abrangido) {
		this.seq_lista_municipio_abrangido = seq_lista_municipio_abrangido;
	}

	public String getDsc_endereco() {
		return dsc_endereco;
	}

	public void setDsc_endereco(String dsc_endereco) {
		this.dsc_endereco = dsc_endereco;
	}

	public String getCep() {
		return cep;
	}

	public void setCep(String cep) {
		this.cep = cep;
	}

	public Integer getSeq_tipo_unidade_judiciaria() {
		return seq_tipo_unidade_judiciaria;
	}

	public void setSeq_tipo_unidade_judiciaria(Integer seq_tipo_unidade_judiciaria) {
		this.seq_tipo_unidade_judiciaria = seq_tipo_unidade_judiciaria;
	}

	public Integer getSeq_classificacao_unid_judiciaria() {
		return seq_classificacao_unid_judiciaria;
	}

	public void setSeq_classificacao_unid_judiciaria(Integer seq_classificacao_unid_judiciaria) {
		this.seq_classificacao_unid_judiciaria = seq_classificacao_unid_judiciaria;
	}
}
