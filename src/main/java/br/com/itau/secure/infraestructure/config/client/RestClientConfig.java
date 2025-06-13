package br.com.itau.secure.infraestructure.config.client;

import br.com.itau.secure.api.client.RestClientFactory;
import br.com.itau.secure.api.client.RiskClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class RestClientConfig {

    @Bean
    public RiskClient riskClient(RestClientFactory factory) {
        RestClient restClient = factory.riskRestClient();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory.builderFor(adapter).build();

        return proxyFactory.createClient(RiskClient.class);
    }
}
