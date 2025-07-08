package br.jus.cnj.datajud.elasticToDatajud.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.jus.cnj.datajud.elasticToDatajud.util.JSONComparator;

@SuppressWarnings("deprecation")
@Component
public class ProcessoElasticRepository {

    @Autowired
    private RestHighLevelClient restClient;
    
    /**
     * Método que Identifica um tribunal a partir do usuário que autentica o acesso a view-processos-sigilo
     * @param millisInsercao Milisegundo a partir do qual os registros são filtrados
     * @param limite Até qual Milisegundo os regisrros são limitados
     * @return Retorna um único documento do Elastic como uma lista de JSONObject, para que seja extraído o Tribunal ao qual o usuário tem acesso
     * @throws IOException
     */
    public List<JSONObject> getTribunal(Long millisInsercao, Long limite) throws IOException {
        List<JSONObject> jsonObjectList = new ArrayList<>(0);
        String index = "view-processos-sigilo-*";
        
        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.rangeQuery("millisInsercao").gte(millisInsercao).lte(limite));

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().must(queryBuilder);
        
        String[] includes = new String[] {"id", "millisInsercao", "grau", "siglaTribunal"};
        
        //Remoção de dados que incorreriam num volume massivo para retorno
        String[] excludes = new String[] {"dadosBasicos*", "movimento*","dadosBasicos.polo.*"};
        
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.fetchSource(includes, excludes);
        searchSourceBuilder.size(1);
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.timeout(new TimeValue(100000));
        searchSourceBuilder.sort(new FieldSortBuilder("millisInsercao").order(SortOrder.ASC));

        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = restClient.search(searchRequest, RequestOptions.DEFAULT);

        if (searchResponse != null) {
            SearchHit[] searchHits = searchResponse.getHits().getHits();
            for (SearchHit searchHit : searchHits) {
                String hitJson = searchHit.getSourceAsString();
                JSONObject jsonObject = new JSONObject(hitJson);
                jsonObjectList.add(jsonObject);
            }
        }
        return jsonObjectList;
    }
    
    /**
     * Método que retorna a lista de Documentos do Elastic a partir de um tribunal, um intervalo de milisegundos
     * @param tribunal Tribunal do qual serão extraídos os documentos no índice do elastic
     * @param millisInsercao Milisegundo a partir do qual os registros são filtrados
     * @param limite Até qual Milisegundo os regisrros são limitados
     * @param limitarResultados Booleano que define o volume de informações a serem retornados
     * @return Retorna a lista de documentos do Elastic como uma lista de JSONObject, para que seja extraído os processos
     * @throws IOException
     */
    public List<JSONObject> getListProcessosByTribunalMillis(String tribunal, Long millisInsercao, Long limite, boolean limitarResultados) throws IOException {
        List<JSONObject> jsonObjectList = new ArrayList<>(0);
        String index = "view-processos-sigilo-*";    

        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termsQuery("siglaTribunal", tribunal))
                .filter(QueryBuilders.rangeQuery("millisInsercao").gte(millisInsercao).lte(limite));

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().must(queryBuilder);
        
        String[] includes = new String[] {"id", "millisInsercao", "grau", "siglaTribunal", "dadosBasicos*", "movimento*"};
        
        //Remoção de dados que incorreriam num volume massivo para retorno
        String[] excludes = new String[] {"dadosBasicos.polo.*"};
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.fetchSource(includes, excludes);
        searchSourceBuilder.size(limitarResultados?1000:2000);
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.timeout(new TimeValue(100000));
        searchSourceBuilder.sort(new FieldSortBuilder("millisInsercao").order(SortOrder.ASC));

        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = restClient.search(searchRequest, RequestOptions.DEFAULT);

        if (searchResponse != null) {
            SearchHit[] searchHits = searchResponse.getHits().getHits();
            for (SearchHit searchHit : searchHits) {
                String hitJson = searchHit.getSourceAsString();
                JSONObject jsonObject = new JSONObject(hitJson);
                jsonObjectList.add(jsonObject);
            }
        }
        Collections.sort(jsonObjectList, new JSONComparator());
        return jsonObjectList;
    }
    
    /**
     * Métoo que calcula a quantidade de documentos do elastic 
     * @param tribunal Tribunal a ser consultado
     * @param millisInsercao Limite inferior do filtro
     * @param limite Limite Superior do filtro
     * @return Quantidade de processos
     * @throws IOException
     */
    public long getQuantidadeProcessosElastic(String tribunal, Long millisInsercao, Long limite) throws IOException {
        long saida = 0;
        String index = "view-processos-sigilo-*";
        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
        		.filter(QueryBuilders.termsQuery("siglaTribunal", tribunal))
                .filter(QueryBuilders.rangeQuery("millisInsercao").gte(millisInsercao).lte(limite));

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().must(queryBuilder);
        
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        
        CountRequest countRequest = new CountRequest(index);
        countRequest.source(searchSourceBuilder);
        
        CountResponse countResponse = restClient.count(countRequest, RequestOptions.DEFAULT);

        if (countResponse != null) {
            saida = countResponse.getCount();
        }
        return saida;
    }
}