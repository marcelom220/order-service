package br.com.itau.secure.api.client;

import br.com.itau.secure.api.model.FraudCheckInput;
import br.com.itau.secure.api.model.FraudCheckResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/risks")
public interface RiskClient {

    @PostExchange
    FraudCheckResult checkFraud(@RequestBody FraudCheckInput input);

}
