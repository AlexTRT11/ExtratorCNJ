package br.jus.cnj.datajud.elasticToDatajud.util;

import org.json.JSONObject;
import org.json.XML;

/**
 * Utility class to convert XML strings representing a process to
 * {@link JSONObject} instances used by the consolidator services.
 */
public class XmlProcessParser {

    private XmlProcessParser() {
        // utility class
    }

    /**
     * Converts the provided XML string to a {@link JSONObject}. If the
     * resulting object contains a single root element it will be unwrapped so
     * the returned object contains the expected process attributes directly.
     *
     * @param xml XML representation of the process
     * @return JSONObject with the process data
     */
    public static JSONObject parse(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new IllegalArgumentException("XML string is empty");
        }
        JSONObject json = XML.toJSONObject(xml);
        if (json.length() == 1) {
            String key = json.keys().next();
            Object val = json.get(key);
            if (val instanceof JSONObject) {
                json = (JSONObject) val;
            }
        }
        return json;
    }
}
