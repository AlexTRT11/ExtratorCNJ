package br.jus.cnj.datajud.elasticToDatajud.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

@Entity
@Table(name = "processo_indicador")
@TypeDefs({
        @TypeDef(name = "json", typeClass = JsonStringType.class),
        @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
public class ProcessoIndicador implements Serializable {
    
    private static final long serialVersionUID = 9051428444497337372L;

    @Id
    @SequenceGenerator(name = "gen_processo_indicador", sequenceName = "sq_processo_indicador", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen_processo_indicador")
    private Long id;
    
    @Column(name = "id_processo")
    private Long idProcesso;
    
    @Column(name = "id_tribunal")
    private Integer idTribunal;
    
    @Column(name = "id_grau")
    private Integer idGrau;
    
    @Column(name = "id_fase")
    private Integer idFase;
    
    @Column(name = "id_tipo_procedimento")
    private Integer idTipoProcedimento;
    
    @Column(name = "id_tipo")
    private Integer idTipo;
        
    @Column(name = "dt_primeiro_inicio")
    private Integer dataPrimeiroInicio;
    
    @Column(name = "dt_ultimo_inicio")
    private Integer dataUltimoInicio;
    
    @Column(name = "dt_ultimo_fim")
    private Integer dataUltimoFim;
    
    @Column(name = "recurso")
    private Integer recurso;
    
    @Transient
    private String situacoesJSON;

    @Type(type = "jsonb")
    @Column(columnDefinition = "json")
    private List<SituacaoJson> situacoes;

    public void serializeSituacoes() throws JsonProcessingException {
        this.setSituacoesJSON(new ObjectMapper().writeValueAsString(getSituacoesJSON()));
    }
    
    @SuppressWarnings("unchecked")
	public void deserializeSituacoes() throws IOException {
        this.setSituacoes((List<SituacaoJson>) new ObjectMapper().readValue(getSituacoesJSON(), List.class));
    }
    
    public ProcessoIndicador(){
        
    }
    
    public ProcessoIndicador(Long idProcesso, Integer idTribunal, Integer idGrau, Integer idFase, Integer idTipoProcedimento, Integer idTipo, 
        Integer dataPrimeiroInicio, Integer dataUltimoInicio, Integer dataUltimoFim, SituacaoJson situacao, int recurso) {
        this.idProcesso = idProcesso;
        this.idTribunal = idTribunal;
        this.idGrau = idGrau;
    	if(idGrau == 2 || idGrau == 5) {
        	this.recurso = recurso+1;
        }else {
        	this.recurso = null;
        }
        this.idFase = idFase;
        this.idTipoProcedimento = idTipoProcedimento;
        this.idTipo = idTipo;
        this.dataPrimeiroInicio = dataPrimeiroInicio;
        this.dataUltimoInicio = dataUltimoInicio;
        this.dataUltimoFim = dataUltimoFim;
        this.situacoes = new ArrayList<>();
        this.situacoes.add(situacao);
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
        ProcessoIndicador other = (ProcessoIndicador) obj;
        if (getIdTribunal() == null) {
            if (other.getIdTribunal() != null) {
                return false;
            }
        } else if (!idTribunal.equals(other.idTribunal)) {
            return false;
        }
        if (getIdProcesso() == null) {
            if (other.getIdProcesso() != null) {
                return false;
            }
        } else if (!idProcesso.equals(other.idProcesso)) {
            return false;
        }
        if (getIdFase() == null) {
            if (other.getIdFase() != null) {
                return false;
            }
        } else if (!idFase.equals(other.idFase)) {
            return false;
        }
        if (getIdTipo() == null) {
            if (other.getIdTipo() != null) {
                return false;
            }
        } else if (!idTipo.equals(other.idTipo)) {
            return false;
        }
        if (getIdGrau() == null) {
            if (other.getIdGrau() != null) {
                return false;
            }
        } else if (!idGrau.equals(other.idGrau)) {
            return false;
        }
        if (getIdTipoProcedimento() == null) {
            if (other.getIdTipoProcedimento() != null) {
                return false;
            }
        } else if (!idTipoProcedimento.equals(other.idTipoProcedimento)) {
            return false;
        }
        if (getRecurso() == null) {
            if (other.getRecurso() != null) {
                return false;
            }
        } else if (!recurso.equals(other.recurso)) {
            return false;
        }
        return true;
    }

    /**
     * @return the serialVersionUID
     */
    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the idProcesso
     */
    public Long getIdProcesso() {
        return idProcesso;
    }

    /**
     * @param idProcesso the idProcesso to set
     */
    public void setIdProcesso(Long idProcesso) {
        this.idProcesso = idProcesso;
    }

    /**
     * @return the idTribunal
     */
    public Integer getIdTribunal() {
        return idTribunal;
    }

    /**
     * @param idTribunal the idTribunal to set
     */
    public void setIdTribunal(Integer idTribunal) {
        this.idTribunal = idTribunal;
    }

    /**
     * @return the idGrau
     */
    public Integer getIdGrau() {
        return idGrau;
    }

    /**
     * @param idGrau the idGrau to set
     */
    public void setIdGrau(Integer idGrau) {
        this.idGrau = idGrau;
    }

    /**
     * @return the idFase
     */
    public Integer getIdFase() {
        return idFase;
    }

    /**
     * @param idFase the idFase to set
     */
    public void setIdFase(Integer idFase) {
        this.idFase = idFase;
    }

    /**
     * @return the idTipoProcedimento
     */
    public Integer getIdTipoProcedimento() {
        return idTipoProcedimento;
    }

    /**
     * @param idTipoProcedimento the idTipoProcedimento to set
     */
    public void setIdTipoProcedimento(Integer idTipoProcedimento) {
        this.idTipoProcedimento = idTipoProcedimento;
    }

    /**
     * @return the idTipo
     */
    public Integer getIdTipo() {
        return idTipo;
    }

    /**
     * @param idTipo the idTipo to set
     */
    public void setIdTipo(Integer idTipo) {
        this.idTipo = idTipo;
    }

    /**
     * @return the dataPrimeiroInicio
     */
    public Integer getDataPrimeiroInicio() {
        return dataPrimeiroInicio;
    }

    /**
     * @param dataPrimeiroInicio the dataPrimeiroInicio to set
     */
    public void setDataPrimeiroInicio(Integer dataPrimeiroInicio) {
        this.dataPrimeiroInicio = dataPrimeiroInicio;
    }

    /**
     * @return the dataUltimoInicio
     */
    public Integer getDataUltimoInicio() {
        return dataUltimoInicio;
    }

    /**
     * @param dataUltimoInicio the dataUltimoInicio to set
     */
    public void setDataUltimoInicio(Integer dataUltimoInicio) {
        this.dataUltimoInicio = dataUltimoInicio;
    }

    /**
     * @return the dataUltimoFim
     */
    public Integer getDataUltimoFim() {
        return dataUltimoFim;
    }

    /**
     * @param dataUltimoFim the dataUltimoFim to set
     */
    public void setDataUltimoFim(Integer dataUltimoFim) {
        this.dataUltimoFim = dataUltimoFim;
    }

    /**
     * @return the situacoes
     */
    public String getSituacoesJSON() {
        try {
            serializeSituacoes();
        } catch (JsonProcessingException ex) {
            System.out.println(ex.getMessage());
        }
        return situacoesJSON;
    }

    /**
     * @param situacoes the situacoes to set
     */
    public void setSituacoesJSON(String situacoesJSON) {
        this.situacoesJSON = situacoesJSON;
    }

    /**
     * @return the situacoes
     */
    public List<SituacaoJson> getSituacoes() {
        return situacoes;
    }

    /**
     * @param situacoes the situacoes to set
     */
    public void setSituacoes(List<SituacaoJson> situacoes) {
        this.situacoes = situacoes;
    }

    public void updateDados(SituacaoJson situacao){
    	if(!this.situacoes.contains(situacao)){
    		if(situacao.getDataInicio() > dataUltimoInicio){
                dataUltimoInicio = situacao.getDataInicio();
            }
            if(situacao.getDataFim() > dataUltimoFim || situacao.getDataFim() == 0){
                dataUltimoFim = situacao.getDataFim();
            }
        	situacao.setSeq(situacoes.size()+1);
            this.situacoes.add(situacao);
        }
    }    
    
    public Integer getRecurso() {
        return recurso;
    }

    /**
     * @param idFase the idFase to set
     */
    public void setRecurso(Integer recurso) {
        this.recurso = recurso;
    }
}
