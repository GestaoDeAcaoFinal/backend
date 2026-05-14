package com.av2.gestaodeacao.cliente;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class AcaoCliente {
    public Map buscarAcao(String ticker) {
        RestTemplate rt = new RestTemplate();

        String url = "https://brapi.dev/api/quote/" + ticker;

        return rt.getForObject(url, Map.class);
    }
}
