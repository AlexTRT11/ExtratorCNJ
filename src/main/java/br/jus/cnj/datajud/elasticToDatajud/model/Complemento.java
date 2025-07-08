package br.jus.cnj.datajud.elasticToDatajud.model;

public class Complemento {
	private Integer codigo;
	private String variavel;
	private String valor;
	
	public Integer getCodigo() {
		return codigo;
	}
	public void setCodigo(Integer codigo) {
		this.codigo = codigo;
	}
	public String getVariavel() {
		return variavel;
	}
	public void setVariavel(String variavel) {
		this.variavel = variavel == null ? null : variavel.toLowerCase();
	}
	public String getValor() {
		return valor;
	}
	public void setValor(String valor) {
		this.valor = valor;
	}
	
	@Override
    public String toString() {
		String codigo = this.codigo == null ? "" : this.codigo.toString();
		String variavel = this.variavel == null ? "" : this.variavel;
		String valor = this.valor == null ? "" : this.valor;
        return String.format("%s:%s:%s", codigo, variavel, valor);
    }
	
	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.codigo == null) ? 0 : this.codigo.hashCode());
        result = prime * result + ((this.variavel == null) ? 0 : this.variavel.hashCode());
        result = prime * result + ((this.valor == null) ? 0 : this.valor.hashCode());
        return result;
    }
	
	@Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Complemento other = (Complemento) obj;
        
        if (this.codigo == null) {
        	if (other.codigo != null) return false;
        } else if (!this.codigo.equals(other.codigo)) return false;

        if (this.variavel == null) {
        	if (other.variavel != null) return false;
        } else if (!this.variavel.equals(other.variavel)) return false;

        if (this.valor == null) {
        	if (other.valor != null) return false;
        } else if (!this.valor.equals(other.valor)) return false;

        return true;
    }	
}
