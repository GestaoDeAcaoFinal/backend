package com.av2.gestaodeacao.cliente;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class ViaCepCliente {

    public Map buscarCep(String cep) {
        RestTemplate rt = new RestTemplate();

        String url = "https://viacep.com.br/ws/" + cep + "/json/";

        return rt.getForObject(url, Map.class);
    }
}