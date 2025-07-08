package br.jus.cnj.datajud.hml;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsável por extrair os XMLs diretamente do banco {@code datajud_hml}.
 */
public class HmlXmlExtractor {

    private final String url;
    private final String user;
    private final String pwd;

    public HmlXmlExtractor(String url, String user, String pwd) {
        this.url = url;
        this.user = user;
        this.pwd = pwd;
    }

    /**
     * Busca os XMLs dos processos no período informado.
     *
     * @param inicio data inicial
     * @param fim data final
     * @return lista de registros
     */
    public List<XmlRecord> fetchXmls(LocalDateTime inicio, LocalDateTime fim) throws SQLException {
        List<XmlRecord> lista = new ArrayList<>();
        String sql = "SELECT chave.nr_processo, chave.cd_classe_judicial, chave.nm_grau," +
                " chave.cd_orgao_julgador, lote.dh_envio_local," +
                " convert_from(xml.conteudo_xml, 'UTF8') AS xml_text" +
                " FROM datajud_hml.tb_lote_processo lote" +
                " JOIN datajud_hml.tb_chave_processo_cnj chave ON lote.id_chave_processo_cnj = chave.id_chave_processo_cnj" +
                " JOIN datajud_hml.tb_xml_processo xml ON lote.id_xml_processo = xml.id_xml_processo" +
                " WHERE lote.dh_envio_local BETWEEN ? AND ?"; 
        try (Connection con = DriverManager.getConnection(url, user, pwd); 
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(inicio));
            ps.setTimestamp(2, Timestamp.valueOf(fim));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    XmlRecord r = new XmlRecord();
                    r.setNumeroProcesso(rs.getString(1));
                    r.setCdClasseJudicial(rs.getInt(2));
                    r.setGrau(rs.getInt(3));
                    r.setCdOrgaoJulgador(rs.getInt(4));
                    r.setDataEnvio(rs.getTimestamp(5));
                    r.setXml(rs.getString(6));
                    lista.add(r);
                }
            }
        }
        return lista;
    }
}
