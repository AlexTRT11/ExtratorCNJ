package br.jus.cnj.datajud.elasticToDatajud.model;

import java.text.Collator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.vladmihalcea.hibernate.type.array.ListArrayType;

@Entity
@Table(name = "orgao_julgador")
@TypeDef(name = "list-array", typeClass = ListArrayType.class)
public class Serventia implements Comparable<Serventia>{
	
	@Id
    private Integer id;

    @Column(name = "id_tribunal")
    private Integer idTribunal;

    @Column(name = "nome")
    private String nome;

    @Column(name = "sigla_tribunal")
    private String siglaTribunal;

    @Column(name = "nome_tribunal")
    private String nomeTribunal;

    @Column(name = "id_segmento_justica")
    private Integer idSegmentoJustica;

    @Column(name = "nome_segmento_justica")
    private String nomeSegmentoJustica;

    @Column(name = "id_municipio")
    private Integer idMunicipio;

    @Column(name = "nome_municipio")
    private String nomeMunicipio;

    @Column(name = "uf")
    private String uf;
    
    @Column(name = "situacao")
    private String situacao;
    
    @Column(name = "seq_tipo_unidade_judiciaria")
    private Integer idTipoUnidadeJudiciaria;
    
    @Column(name = "seq_classificacao_unid_judiciaria")
    private Integer idClassificacaoUnidadeJudiciaria;
    
    @Column(name = "lat")
    private Float latitude;
    
    @Column(name = "lon")
    private Float longitude;
    
    @Column(name="dsc_endereco")
    private String endereco;
    
    @Column(name="dsc_telefone")
    private String telefone;
    
    @Column(name="dsc_email")
    private String email;
    
    @Column(name="cep")
    private String cep;
    
    @Column(name="cod_unidade_judicial")
    private String codigo_unidade;
    
    @Column(name="flg_juizo_digital")
    private Boolean juizo_digital;

    @Column(name="dat_adesao_juizo_digital")
	private Date dtAdesaoJuizoDigital;
    
    @Column(name="dat_termino_juizo_digital")
	private Date dtTerminoJuizoDigital;
    
    @Column(name="dat_instalacao")
	private Date dtInstalacao;
    
    @Type(type = "list-array")
	@Column( name = "seq_lista_municipio_abrangido", columnDefinition = "integer[]" )
	private List<Integer> idMunicipiosAbrangidos; 
    
    @Type(type = "list-array")
	@Column( name = "seq_lista_competencia_juizo", columnDefinition = "integer[]" )
	private List<Integer> idCodigosCompetencias; 
    
    @Column(name="dsc_telefone_balcao")
    private String telefoneBalcao;
    
    @Column(name="dsc_link_balcao")
    private String linkBalcao;

	@Column(name="flg_balcao_virtual")
	private Boolean balcaoVirtual;
	
	@Column(name = "seq_entrancia")
    private Integer entrancia;
	
	public Serventia() {}
	
	public Serventia(Integer id, String nome, Integer idTribunal, String siglaTribunal, String nomeTribunal, Integer idSegmentoJustica, String nomeSegmentoJustica, Integer idMunicipio, String nomeMunicipio, String uf, String situacao,
			Integer idTipoUnidadeJudiciaria, Integer idClassificacaoUnidadeJudiciaria, String endereco, String telefone, String email, String cep, String codigo_unidade, Boolean juizo_digital, Date dtAdesaoJuizoDigital, Date dtTerminoJuizoDigital,
			Date dtInstalacao, List<Integer> idMunicipiosAbrangidos, List<Integer> idCodigosCompetencias, String telefoneBalcao, String linkBalcao, Boolean balcaoVirtual, Float latitude, Float longitude, Integer entrancia) {
        setId(id);
        setIdTribunal(idTribunal);
        setNome(nome);
        setSiglaTribunal(siglaTribunal);
        setNomeTribunal(nomeTribunal);
        setIdSegmentoJustica(idSegmentoJustica);
        setNomeSegmentoJustica(nomeSegmentoJustica);
        setIdMunicipio(idMunicipio);
        setNomeMunicipio(nomeMunicipio);
        setUf(uf);
        setSituacao(situacao);
        setIdTipoUnidadeJudiciaria(idTipoUnidadeJudiciaria);
        setIdClassificacaoUnidadeJudiciaria(idClassificacaoUnidadeJudiciaria);
        setEndereco(endereco);
        setTelefone(telefone);
        setEmail(email);
        setCep(cep);
        setCodigo_unidade(codigo_unidade);
        setJuizo_digital(juizo_digital);
        setDtAdesaoJuizoDigital(dtAdesaoJuizoDigital);
        setDtTerminoJuizoDigital(dtTerminoJuizoDigital);
        setDtInstalacao(dtInstalacao);
        setIdMunicipiosAbrangidos(idMunicipiosAbrangidos);
        setIdCodigosCompetencias(idCodigosCompetencias);
        setTelefoneBalcao(telefoneBalcao);
        setLinkBalcao(linkBalcao);
        setBalcaoVirtual(balcaoVirtual);
        setLatitude(latitude);
        setLongitude(longitude);
        setEntrancia(entrancia);
    }
    
	public Integer getEntrancia() {
		return entrancia;
	}

	public void setEntrancia(Integer entrancia) {
		this.entrancia = entrancia;
	}

	@Override
    public String toString() {
        String id = this.id == null ? "" : this.id.toString();
        String idTribunal = this.idTribunal == null ? "" : this.idTribunal.toString();
        String nome = this.nome == null ? "" : this.nome;
        return String.format("%s:%s:%s", id, idTribunal, nome);
    }
    
    @Override
    public int compareTo(Serventia o) {
        Collator cot = Collator.getInstance(new Locale("pt", "BR"));
        if (o != null) {
             return cot.compare(this.getId(), o.getId());
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

    public Integer getIdTribunal() {
        return idTribunal;
    }

    public void setIdTribunal(Integer idTribunal) {
        this.idTribunal = idTribunal;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSiglaTribunal() {
        return siglaTribunal;
    }

    public void setSiglaTribunal(String siglaTribunal) {
        this.siglaTribunal = siglaTribunal;
    }

    public String getNomeTribunal() {
        return nomeTribunal;
    }

    public void setNomeTribunal(String nomeTribunal) {
        this.nomeTribunal = nomeTribunal;
    }

    public Integer getIdSegmentoJustica() {
        return idSegmentoJustica;
    }

    public void setIdSegmentoJustica(Integer idSegmentoJustica) {
        this.idSegmentoJustica = idSegmentoJustica;
    }

    public String getNomeSegmentoJustica() {
        return nomeSegmentoJustica;
    }

    public void setNomeSegmentoJustica(String nomeSegmentoJustica) {
        this.nomeSegmentoJustica = nomeSegmentoJustica;
    }

    public Integer getIdMunicipio() {
        return idMunicipio;
    }

    public void setIdMunicipio(Integer idMunicipio) {
        this.idMunicipio = idMunicipio;
    }

    public String getNomeMunicipio() {
        return nomeMunicipio;
    }

    public void setNomeMunicipio(String nomeMunicipio) {
        this.nomeMunicipio = nomeMunicipio;
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
	
	public Integer getIdTipoUnidadeJudiciaria() {
		return idTipoUnidadeJudiciaria;
	}

	public void setIdTipoUnidadeJudiciaria(Integer idTipoUnidadeJudiciaria) {
		this.idTipoUnidadeJudiciaria = idTipoUnidadeJudiciaria == 0 ? 4 : idTipoUnidadeJudiciaria;
	}

	public Integer getIdClassificacaoUnidadeJudiciaria() {
		return idClassificacaoUnidadeJudiciaria;
	}

	public void setIdClassificacaoUnidadeJudiciaria(Integer idClassificacaoUnidadeJudiciaria) {
		this.idClassificacaoUnidadeJudiciaria = idClassificacaoUnidadeJudiciaria == 0 ? 400 : idClassificacaoUnidadeJudiciaria;
	}

	public String getEndereco() {
		return endereco;
	}

	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getCep() {
		return cep;
	}

	public void setCep(String cep) {
		this.cep = cep;
	}

	public String getCodigo_unidade() {
		return codigo_unidade;
	}

	public void setCodigo_unidade(String codigo_unidade) {
		this.codigo_unidade = codigo_unidade;
	}

	public Boolean getJuizo_digital() {
		return juizo_digital;
	}

	public void setJuizo_digital(Boolean juizo_digital) {
		this.juizo_digital = juizo_digital;
	}

	public Date getDtAdesaoJuizoDigital() {
		return dtAdesaoJuizoDigital;
	}

	public void setDtAdesaoJuizoDigital(Date dtAdesaoJuizoDigital) {
		this.dtAdesaoJuizoDigital = dtAdesaoJuizoDigital;
	}

	public Date getDtTerminoJuizoDigital() {
		return dtTerminoJuizoDigital;
	}

	public void setDtTerminoJuizoDigital(Date dtTerminoJuizoDigital) {
		this.dtTerminoJuizoDigital = dtTerminoJuizoDigital;
	}

	public Date getDtInstalacao() {
		return dtInstalacao;
	}

	public void setDtInstalacao(Date dtInstalacao) {
		this.dtInstalacao = dtInstalacao;
	}

	public List<Integer> getIdMunicipiosAbrangidos() {
		return idMunicipiosAbrangidos;
	}

	public void setIdMunicipiosAbrangidos(List<Integer> idMunicipiosAbrangidos) {
		this.idMunicipiosAbrangidos = idMunicipiosAbrangidos;
	}

	public List<Integer> getIdCodigosCompetencias() {
		return idCodigosCompetencias;
	}

	public void setIdCodigosCompetencias(List<Integer> idCodigosCompetencias) {
		this.idCodigosCompetencias = idCodigosCompetencias;
	}

	public String getTelefoneBalcao() {
		return telefoneBalcao;
	}

	public void setTelefoneBalcao(String telefoneBalcao) {
		this.telefoneBalcao = telefoneBalcao;
	}

	public String getLinkBalcao() {
		return linkBalcao;
	}

	public void setLinkBalcao(String linkBalcao) {
		this.linkBalcao = linkBalcao;
	}

	public Boolean getBalcaoVirtual() {
		return balcaoVirtual;
	}

	public void setBalcaoVirtual(Boolean balcaoVirtual) {
		this.balcaoVirtual = balcaoVirtual;
	}
	
	public Float getLatitude() {
		return latitude;
	}

	public void setLatitude(Float latitude) {
		this.latitude = latitude;
	}
	
	public Float getLongitude() {
		return longitude;
	}

	public void setLongitude(Float longitude) {
		this.longitude = longitude;
	}
}