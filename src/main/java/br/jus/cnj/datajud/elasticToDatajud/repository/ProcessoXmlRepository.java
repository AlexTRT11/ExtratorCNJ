package br.jus.cnj.datajud.elasticToDatajud.repository;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Repositório utilizado para acessar registros XML de processos armazenados no banco de dados.
 */
@Component
public class ProcessoXmlRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Recupera uma lista de XMLs do tribunal no intervalo informado.
     *
     * @param tribunal sigla do tribunal
     * @param millisInsercao limite inferior do intervalo
     * @param limite limite superior do intervalo
     * @param limitarResultados define quantidade máxima retornada
     * @return lista de XMLs
     */
    public List<String> getListProcessosXmlByTribunalMillis(String tribunal, Long millisInsercao, Long limite, boolean limitarResultados) {
        int max = limitarResultados ? 1000 : 2000;
        String sql = "SELECT xml FROM processo_xml WHERE sigla_tribunal = ? AND millis_insercao >= ? AND millis_insercao <= ? ORDER BY millis_insercao ASC LIMIT ?";
        return jdbcTemplate.query(sql, new Object[] { tribunal, millisInsercao, limite, max }, (rs, rowNum) -> rs.getString("xml"));
    }

    /**
     * Retorna a quantidade de registros XML dentro do intervalo.
     */
    public long countProcessos(String tribunal, Long millisInsercao, Long limite) {
        String sql = "SELECT COUNT(*) FROM processo_xml WHERE sigla_tribunal = ? AND millis_insercao >= ? AND millis_insercao <= ?";
        Long count = jdbcTemplate.queryForObject(sql, new Object[] { tribunal, millisInsercao, limite }, Long.class);
        return count == null ? 0L : count;
    }

    /**
     * Recupera a lista de tribunais disponíveis na tabela de XML.
     */
    public List<JSONObject> getTribunal(Long millisInsercao, Long limite) {
        String sql = "SELECT DISTINCT sigla_tribunal FROM processo_xml";
        return jdbcTemplate.query(sql, rs -> {
            List<JSONObject> out = new ArrayList<>();
            while (rs.next()) {
                JSONObject o = new JSONObject();
                o.put("siglaTribunal", rs.getString(1));
                out.add(o);
            }
            return out;
        });
    }
}
