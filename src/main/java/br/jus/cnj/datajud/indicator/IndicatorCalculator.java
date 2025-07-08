package br.jus.cnj.datajud.indicator;

import br.jus.cnj.datajud.hml.XmlRecord;
import java.util.List;
import java.util.Locale;

/**
 * Cálculo simplificado dos indicadores da Portaria CNJ 238/2024.
 * Esta implementação faz apenas uma contagem básica de sentenças e decisões
 * homologatórias nos XMLs extraídos.
 */
public class IndicatorCalculator {

    public IndicatorResult calculate(List<XmlRecord> registros) {
        int sentCHNcrim1 = 0;
        int sentCNcrim1 = 0;
        int decHCNcrim2 = 0;
        int decCNcrim2 = 0;
        int sentJudNcrimH1 = 0;
        int sentJudNcrimHJE = 0;
        int sentJudNcrim1 = 0;
        int sentJudNCrimJE = 0;

        for (XmlRecord r : registros) {
            String xml = r.getXml();
            if (xml == null) {
                continue;
            }
            String upper = xml.toUpperCase(Locale.ROOT);
            boolean homologatoria = upper.contains("HOMOLOGA");
            boolean execucao = upper.contains("EXECUCAO") || upper.contains("CUMPRIMENTO");
            if (r.getGrau() != null && r.getGrau() == 1) {
                sentCNcrim1++;
                if (homologatoria) {
                    sentCHNcrim1++;
                }
                if (execucao) {
                    sentJudNcrim1++;
                    if (homologatoria) {
                        sentJudNcrimH1++;
                    }
                }
            } else if (r.getGrau() != null && r.getGrau() == 2) {
                decCNcrim2++;
                if (homologatoria) {
                    decHCNcrim2++;
                }
                if (execucao) {
                    sentJudNCrimJE++;
                    if (homologatoria) {
                        sentJudNcrimHJE++;
                    }
                }
            }
        }

        IndicatorResult result = new IndicatorResult();
        result.setIndicadorI(sentCNcrim1 == 0 ? 0.0 : (double) sentCHNcrim1 / sentCNcrim1);
        result.setIndicadorIII(decCNcrim2 == 0 ? 0.0 : (double) decHCNcrim2 / decCNcrim2);
        int totalSentJud = sentJudNcrim1 + sentJudNCrimJE;
        int totalHomolog = sentJudNcrimH1 + sentJudNcrimHJE;
        result.setIndicadorV(totalSentJud == 0 ? 0.0 : (double) totalHomolog / totalSentJud);
        return result;
    }
}
