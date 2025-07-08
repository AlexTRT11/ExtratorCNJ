package br.jus.cnj.datajud.elasticToDatajud.util;

import java.util.Comparator;

import org.json.JSONObject;

public class JSONComparator implements Comparator<JSONObject> {

	@Override
	public int compare(JSONObject o1, JSONObject o2) {
	    String v1 = (String) ((JSONObject) o1.get("dadosBasicos")).get("numero");
	    String v2 = (String) ((JSONObject) o2.get("dadosBasicos")).get("numero");
	    int res = v1.compareTo(v2);
	    if(res == 0) {
	    	v1 = (String) o1.get("grau");
		    v2 = (String) o2.get("grau");
		    res = v1.compareTo(v2);
	    }
	    return res;
	}
}