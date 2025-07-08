package br.jus.cnj.datajud.elasticToDatajud.service;

import org.json.JSONObject;
import org.json.XML;
import org.springframework.stereotype.Component;

/**
 * Utilitário para converter representações em XML de processos para JSON.
 */
@Component
public class XmlProcessParser {

    /**
     * Converte o conteúdo XML de um processo para {@link JSONObject}.
     *
     * @param xml conteúdo XML
     * @return processo representado como {@link JSONObject}
     */
    public JSONObject parse(String xml) {
        return XML.toJSONObject(xml);
    }
}
