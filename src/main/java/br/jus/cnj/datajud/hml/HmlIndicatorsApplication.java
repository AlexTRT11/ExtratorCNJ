package br.jus.cnj.datajud.hml;

import br.jus.cnj.datajud.indicator.IndicatorCalculator;
import br.jus.cnj.datajud.indicator.IndicatorResult;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Aplicacao simples para extracao de XMLs do banco datajud_hml e calculo
 * dos indicadores I, III e V da Portaria CNJ 238/2024.
 */
public class HmlIndicatorsApplication {

    public static void main(String[] args) throws SQLException {
        String url = System.getenv().getOrDefault("hmlPostgresqlUrl", "jdbc:postgresql://localhost:5432/datajud_hml");
        String user = System.getenv().getOrDefault("hmlPostgresqlUser", "postgres");
        String pwd = System.getenv().getOrDefault("hmlPostgresqlPwd", "postgres");

        HmlXmlExtractor extractor = new HmlXmlExtractor(url, user, pwd);
        LocalDateTime ini = LocalDateTime.of(2024, 8, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2025, 7, 31, 23, 59);
        List<XmlRecord> registros = extractor.fetchXmls(ini, fim);

        IndicatorCalculator calc = new IndicatorCalculator();
        IndicatorResult result = calc.calculate(registros);
        System.out.println(result);
    }
}
