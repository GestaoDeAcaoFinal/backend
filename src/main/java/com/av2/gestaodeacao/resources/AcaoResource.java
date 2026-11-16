package com.av2.gestaodeacao.resources;

import com.av2.gestaodeacao.domains.Acao;
import com.av2.gestaodeacao.domains.dtos.AcaoRequestDTO;
import com.av2.gestaodeacao.services.AcaoService;
import com.av2.gestaodeacao.validation.TickerNormalizer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/acoes")
@Validated
public class AcaoResource {

    @Autowired
    private AcaoService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Acao salvar(@Valid @RequestBody AcaoRequestDTO dto) {
        return service.salvar(dto);
    }

    @GetMapping
    public List<Acao> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Acao buscarPorId(@PathVariable @Positive(message = "Id deve ser positivo") Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/ticker/{ticker}")
    public Acao buscarPorTicker(
            @PathVariable
            @Pattern(
                    regexp = TickerNormalizer.FORMATO_TICKER,
                    message = TickerNormalizer.MENSAGEM_FORMATO
            ) String ticker
    ) {
        return service.buscarPorTicker(ticker);
    }

    @PutMapping("/{id}/atualizar-cotacao")
    @PreAuthorize("hasRole('ADMIN')")
    public Acao atualizarCotacao(@PathVariable @Positive(message = "Id deve ser positivo") Long id) {
        return service.atualizarCotacao(id);
    }
}
