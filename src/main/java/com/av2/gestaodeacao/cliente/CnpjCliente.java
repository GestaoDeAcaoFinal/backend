package com.av2.gestaodeacao.cliente;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class CnpjCliente {
    public Map buscarCnpj(String cnpj) {
        RestTemplate rt = new RestTemplate();

        String url = "https://brasilapi.com.br/api/cnpj/v1/" + cnpj;

        return rt.getForObject(url, Map.class);
    }
}
