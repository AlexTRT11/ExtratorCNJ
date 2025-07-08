package br.jus.cnj.datajud.elasticToDatajud.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

/**
 * Unit tests for {@link XmlProcessParser}.
 */
public class XmlProcessParserTest {

    @Test
    public void testParseSimpleXml() {
        String xml = "<processo>" +
                "<grau>1</grau>" +
                "<siglaTribunal>TJ</siglaTribunal>" +
                "<dadosBasicos><numero>0001</numero></dadosBasicos>" +
                "<movimento><id>5</id></movimento>" +
                "</processo>";

        JSONObject obj = XmlProcessParser.parse(xml);

        assertEquals("0001", obj.getJSONObject("dadosBasicos").getString("numero"));
        assertEquals("1", obj.getString("grau"));
        assertEquals("TJ", obj.getString("siglaTribunal"));
        assertTrue(obj.has("movimento"));
    }
}
