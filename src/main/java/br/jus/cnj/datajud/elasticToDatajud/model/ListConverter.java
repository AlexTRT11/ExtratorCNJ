package br.jus.cnj.datajud.elasticToDatajud.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import javax.persistence.AttributeConverter;

public class ListConverter implements AttributeConverter<List<SituacaoJson>, String> {

    @Override
    public String convertToDatabaseColumn(List<SituacaoJson> situacaoInfo) {

        String situacaoJson = null;
        try {
            situacaoJson = new ObjectMapper().writeValueAsString(situacaoInfo);
        } catch (JsonProcessingException ex) {
            System.out.println(ex.getMessage());
        }

        return situacaoJson;
    }

    @SuppressWarnings("unchecked")
	@Override
    public List<SituacaoJson> convertToEntityAttribute(String situacaoJson) {

        List<SituacaoJson> situacaoInfo = null;
        try {
            situacaoInfo = new ObjectMapper().readValue(situacaoJson, List.class);
        } catch (JsonProcessingException ex) {
            System.out.println(ex.getMessage());
        }

        return situacaoInfo;
    }

}
