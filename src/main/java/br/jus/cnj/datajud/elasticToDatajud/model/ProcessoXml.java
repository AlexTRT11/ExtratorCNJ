package br.jus.cnj.datajud.elasticToDatajud.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "processo_xml")
public class ProcessoXml implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "gen_processo_xml", sequenceName = "sq_processo_xml", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen_processo_xml")
    private Long id;

    @Column(name = "sigla_tribunal")
    private String siglaTribunal;

    @Column(name = "millisinsercao")
    private Long millisInsercao;

    @Column(name = "xml")
    private String xml;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSiglaTribunal() {
        return siglaTribunal;
    }

    public void setSiglaTribunal(String siglaTribunal) {
        this.siglaTribunal = siglaTribunal;
    }

    public Long getMillisInsercao() {
        return millisInsercao;
    }

    public void setMillisInsercao(Long millisInsercao) {
        this.millisInsercao = millisInsercao;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }
}
