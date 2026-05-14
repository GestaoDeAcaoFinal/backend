package com.av2.gestaodeacao.resources;

import com.av2.gestaodeacao.domains.OperacaoAcao;
import com.av2.gestaodeacao.services.OperacaoAcaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/operacoes")
public class OperacaoAcaoResource {

    @Autowired
    private OperacaoAcaoService service;

    @GetMapping
    public List<OperacaoAcao> listar() {

        return service.listar();
    }

    @PostMapping("/comprar/{acaoId}")
    public OperacaoAcao comprar(
            @PathVariable Long acaoId,
            @RequestParam Integer quantidade
    ) {

        return service.comprar(
                acaoId,
                quantidade
        );
    }

    @PostMapping("/vender/{acaoId}")
    public OperacaoAcao vender(
            @PathVariable Long acaoId,
            @RequestParam Integer quantidade
    ) {

        return service.vender(
                acaoId,
                quantidade
        );
    }

    @GetMapping("/{acaoId}")
    public List<OperacaoAcao> listar(
            @PathVariable Long acaoId
    ) {

        return service.listarHistorico(acaoId);
    }

    @GetMapping("/compras")
    public List<OperacaoAcao> listarCompras() {

        return service.listarCompras();
    }

    @GetMapping("/vendas")
    public List<OperacaoAcao> listarVendas() {

        return service.listarVendas();
    }

    @GetMapping("/buscar/{id}")
    public OperacaoAcao buscarPorId(
            @PathVariable Long id
    ) {

        return service.buscarPorId(id);
    }
}