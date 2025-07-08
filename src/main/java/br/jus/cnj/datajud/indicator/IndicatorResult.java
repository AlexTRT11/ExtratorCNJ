package br.jus.cnj.datajud.indicator;

/**
 * Resultado do cálculo dos indicadores definidos na Portaria CNJ 238/2024.
 */
public class IndicatorResult {
    private double indicadorI;
    private double indicadorIII;
    private double indicadorV;

    public double getIndicadorI() {
        return indicadorI;
    }

    public void setIndicadorI(double indicadorI) {
        this.indicadorI = indicadorI;
    }

    public double getIndicadorIII() {
        return indicadorIII;
    }

    public void setIndicadorIII(double indicadorIII) {
        this.indicadorIII = indicadorIII;
    }

    public double getIndicadorV() {
        return indicadorV;
    }

    public void setIndicadorV(double indicadorV) {
        this.indicadorV = indicadorV;
    }

    @Override
    public String toString() {
        return "IndicatorResult{" +
                "indicadorI=" + indicadorI +
                ", indicadorIII=" + indicadorIII +
                ", indicadorV=" + indicadorV +
                '}';
    }
}
