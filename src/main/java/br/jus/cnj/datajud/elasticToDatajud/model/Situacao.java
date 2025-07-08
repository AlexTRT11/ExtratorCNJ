package br.jus.cnj.datajud.elasticToDatajud.model;

public class Situacao {

    private Integer id;
    private String nome;
    private Integer[] iniciar;
    private Integer[] finalizar;
    private String tipoFinalizacao;
    private Boolean inicializacaoCondicional;
    private Integer idFase;
    private Integer idTipoProcedimento;
    private Integer idNatureza;
    private Boolean criminal;
    private Boolean finalizaFaseAtual;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer[] getIniciar() {
        return iniciar;
    }

    public void setIniciar(Integer[] iniciar) {
        this.iniciar = iniciar;
    }

    public Integer[] getFinalizar() {
        return finalizar;
    }

    public void setFinalizar(Integer[] finalizar) {
        this.finalizar = finalizar;
    }

    public String getTipoFinalizacao() {
        return tipoFinalizacao;
    }

    public void setTipoFinalizacao(String tipoFinalizacao) {
        this.tipoFinalizacao = tipoFinalizacao;
    }

    public Boolean getInicializacaoCondicional() {
        return inicializacaoCondicional;
    }

    public void setInicializacaoCondicional(Boolean inicializacaoCondicional) {
        this.inicializacaoCondicional = inicializacaoCondicional;
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

    @Override
    public String toString() {
        String id = this.id == null ? "" : this.id.toString();
        String nome = this.nome == null ? "" : this.nome;
        return String.format("%s:%s", id, nome);
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
        Situacao other = (Situacao) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    public Boolean getFinalizaFaseAtual() {
        return finalizaFaseAtual;
    }

    public void setFinalizaFaseAtual(Boolean finalizaFaseAtual) {
        this.finalizaFaseAtual = finalizaFaseAtual;
    }
}
