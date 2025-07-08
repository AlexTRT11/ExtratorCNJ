package br.jus.cnj.datajud.hml;

import java.sql.Timestamp;

/**
 * Representa um registro de XML proveniente do banco datajud_hml.
 */
public class XmlRecord {
    private String numeroProcesso;
    private Integer cdClasseJudicial;
    private Integer grau;
    private Integer cdOrgaoJulgador;
    private Timestamp dataEnvio;
    private String xml;

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public Integer getCdClasseJudicial() {
        return cdClasseJudicial;
    }

    public void setCdClasseJudicial(Integer cdClasseJudicial) {
        this.cdClasseJudicial = cdClasseJudicial;
    }

    public Integer getGrau() {
        return grau;
    }

    public void setGrau(Integer grau) {
        this.grau = grau;
    }

    public Integer getCdOrgaoJulgador() {
        return cdOrgaoJulgador;
    }

    public void setCdOrgaoJulgador(Integer cdOrgaoJulgador) {
        this.cdOrgaoJulgador = cdOrgaoJulgador;
    }

    public Timestamp getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(Timestamp dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }
}
