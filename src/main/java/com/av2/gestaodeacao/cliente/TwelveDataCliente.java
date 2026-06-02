package com.av2.gestaodeacao.cliente;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class TwelveDataCliente {

    @Value("${twelvedata.api.key}")
    private String apiKey;

    public Map buscarAcao(String ticker) {

        RestTemplate rt = new RestTemplate();

        String url =
                "https://api.twelvedata.com/quote?symbol="
                        + ticker
                        + "&apikey="
                        + apiKey;

        return rt.getForObject(url, Map.class);
    }
}