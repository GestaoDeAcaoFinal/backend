package com.av2.gestaodeacao.services;

import com.av2.gestaodeacao.cliente.AcaoCliente;
import com.av2.gestaodeacao.domains.Acao;
import com.av2.gestaodeacao.domains.Corretora;
import com.av2.gestaodeacao.domains.dtos.AcaoRequestDTO;
import com.av2.gestaodeacao.repositories.AcaoRepository;
import com.av2.gestaodeacao.repositories.CorretoraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AcaoService {

    @Autowired
    private AcaoRepository repository;

    @Autowired
    private CorretoraRepository corretoraRepository;

    public Acao salvar(AcaoRequestDTO dto) {

        Acao a = new Acao();

        a.setTicker(dto.getTicker().toUpperCase());

        if (repository.findByTicker(a.getTicker()).isPresent()) {
            throw new RuntimeException("Ticker já cadastrado");
        }

        Corretora corretora = corretoraRepository
                .findById(dto.getCorretoraId())
                .orElseThrow(() ->
                        new RuntimeException("Corretora não encontrada"));

        a.setCorretoraRelacionada(corretora);

        try {
            Map response = cliente.buscarAcao(a.getTicker());

            List results = (List) response.get("results");

            if (results == null || results.isEmpty()) {
                throw new RuntimeException("Ticker inválido");
            }

            Map dados = (Map) results.get(0);

            a.setNomeEmpresa((String) dados.get("longName"));

            Object preco = dados.get("regularMarketPrice");
            if (preco instanceof Integer) {
                a.setCotacaoAtual(((Integer) preco).doubleValue());
            } else {
                a.setCotacaoAtual((Double) preco);
            }

            a.setMoeda((String) dados.get("currency"));

            if (a.getTicker().endsWith("34")) {
                a.setMercado("EUA");
            } else {
                a.setMercado("BR");
            }

            a.setDataHoraCotacao(LocalDateTime.now());

        } catch (Exception e) {
            throw new RuntimeException("Erro ao consultar API de ações");
        }

        return repository.save(a);
    }

    public List<Acao> listar() {
        return repository.findAll();
    }

    public Acao buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Não encontrada"));
    }

    public Acao buscarPorTicker(String ticker) {
        return repository.findByTicker(ticker)
                .orElseThrow(() -> new RuntimeException("Não encontrada"));
    }
    public Acao atualizarCotacao(Long id) {

        Acao a = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ação não encontrada"));

        try {
            Map response = cliente.buscarAcao(a.getTicker());

            List results = (List) response.get("results");

            if (results == null || results.isEmpty()) {
                throw new RuntimeException("Erro ao atualizar cotação");
            }

            Map dados = (Map) results.get(0);

            Object preco = dados.get("regularMarketPrice");
            if (preco instanceof Integer) {
                a.setCotacaoAtual(((Integer) preco).doubleValue());
            } else {
                a.setCotacaoAtual((Double) preco);
            }

            a.setDataHoraCotacao(LocalDateTime.now());

        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar cotação");
        }

        return repository.save(a);
    }
    @Autowired
    private AcaoCliente cliente;
}
