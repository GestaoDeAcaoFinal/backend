package com.av2.gestaodeacao.resources;

import com.av2.gestaodeacao.domains.Acao;
import com.av2.gestaodeacao.domains.Corretora;
import com.av2.gestaodeacao.domains.dtos.AcaoRequestDTO;
import com.av2.gestaodeacao.services.AcaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/acoes")
public class AcaoResource {

    @Autowired
    private AcaoService service;

    @PostMapping
    public Acao salvar(@RequestBody AcaoRequestDTO dto) {
        return service.salvar(dto);
    }

    @GetMapping
    public List<Acao> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Acao buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/ticker/{ticker}")
    public Acao buscarPorTicker(@PathVariable String ticker) {
        return service.buscarPorTicker(ticker);
    }

    @PutMapping("/{id}/atualizar-cotacao")
    public Acao atualizarCotacao(@PathVariable Long id) {
        return service.atualizarCotacao(id);
    }
}
