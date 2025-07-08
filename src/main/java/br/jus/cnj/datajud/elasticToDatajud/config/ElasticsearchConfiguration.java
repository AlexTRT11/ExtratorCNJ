package br.jus.cnj.datajud.elasticToDatajud.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@SuppressWarnings("deprecation")
@Configuration
@EnableElasticsearchRepositories(basePackages = "br.jus.cnj.datajud.elastictodatajud.repository")
public class ElasticsearchConfiguration {

	@Value("${elasticsearchHost}")
    private String host;

    @Value("${elasticsearchPort}")
    private String port;

    @Value("${elasticsearchUser}")
    private String user;

    @Value("${elasticsearchPwd}")
    private String pwd;
    
    @Bean
    public RestHighLevelClient client() {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, pwd));

        RestClientBuilder builder =  RestClient.builder(new HttpHost(host, Integer.parseInt(port),"https"));

        builder.setCompressionEnabled(true);
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            httpClientBuilder.setMaxConnPerRoute(150);
            httpClientBuilder.setMaxConnTotal(300);
            try {
            	httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy()
		        {
		            public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
		            {
		                return true;
		            }
		        }).build());
            }catch(Exception e) {
            	System.out.println(e.getLocalizedMessage());
            }

            return httpClientBuilder;
        });
        return new RestHighLevelClient(builder);
    }
}
