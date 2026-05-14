package com.av2.gestaodeacao.services;

import com.av2.gestaodeacao.domains.Acao;
import com.av2.gestaodeacao.domains.OperacaoAcao;
import com.av2.gestaodeacao.domains.enums.TipoOperacao;
import com.av2.gestaodeacao.repositories.AcaoRepository;
import com.av2.gestaodeacao.repositories.OperacaoAcaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperacaoAcaoService {

    @Autowired
    private AcaoRepository repository;

    @Autowired
    private OperacaoAcaoRepository operacaoRepository;

    public OperacaoAcao comprar(Long acaoId, Integer quantidade) {

        Acao acao = repository.findById(acaoId)
                .orElseThrow(() ->
                        new RuntimeException("Ação não encontrada"));

        Double precoUnitario = acao.getCotacaoAtual();

        OperacaoAcao operacaoAcao = new OperacaoAcao();

        operacaoAcao.setAcao(acao);
        operacaoAcao.setQuantidade(quantidade);
        operacaoAcao.setPrecoUnitario(precoUnitario);
        operacaoAcao.setTipoOperacao(TipoOperacao.COMPRA);
        operacaoAcao.setDataOperacao(LocalDateTime.now());

        List<OperacaoAcao> compras = operacaoRepository
                .findByAcaoAndTipoOperacao(
                        acao,
                        TipoOperacao.COMPRA
                );

        int quantidadeAtual = compras.stream()
                .mapToInt(OperacaoAcao::getQuantidade)
                .sum();

        double valorAtual = compras.stream()
                .mapToDouble(op ->
                        op.getQuantidade() * op.getPrecoUnitario())
                .sum();

        double valorNovaCompra =
                quantidade * precoUnitario;

        int novaQuantidadeTotal =
                quantidadeAtual + quantidade;

        double precoMedio =
                (valorAtual + valorNovaCompra)
                        / novaQuantidadeTotal;

        operacaoAcao.setPrecoMedio(precoMedio);

        operacaoAcao.setValorTotal(valorNovaCompra);

        return operacaoRepository.save(operacaoAcao);
    }

    public OperacaoAcao vender(Long acaoId,
                               Integer quantidade) {

        Acao acao = repository.findById(acaoId)
                .orElseThrow(() ->
                        new RuntimeException("Ação não encontrada"));

        List<OperacaoAcao> compras =
                operacaoRepository.findByAcaoAndTipoOperacao(
                        acao,
                        TipoOperacao.COMPRA
                );

        int quantidadeComprada = compras.stream()
                .mapToInt(OperacaoAcao::getQuantidade)
                .sum();

        List<OperacaoAcao> vendas =
                operacaoRepository.findByAcaoAndTipoOperacao(
                        acao,
                        TipoOperacao.VENDA
                );

        int quantidadeVendida = vendas.stream()
                .mapToInt(OperacaoAcao::getQuantidade)
                .sum();

        int quantidadeDisponivel =
                quantidadeComprada - quantidadeVendida;

        if (quantidade > quantidadeDisponivel) {
            throw new RuntimeException("Quantidade insuficiente");
        }

        // AGORA PEGA AUTOMATICAMENTE A COTAÇÃO ATUAL
        double precoVenda = acao.getCotacaoAtual();

        double precoMedio = compras
                .get(compras.size() - 1)
                .getPrecoMedio();

        double lucro =
                (precoVenda - precoMedio) * quantidade;

        OperacaoAcao venda = new OperacaoAcao();

        venda.setAcao(acao);
        venda.setQuantidade(quantidade);
        venda.setPrecoUnitario(precoVenda);
        venda.setPrecoMedio(precoMedio);
        venda.setLucroPrejuizo(lucro);
        venda.setTipoOperacao(TipoOperacao.VENDA);
        venda.setDataOperacao(LocalDateTime.now());

        venda.setValorTotal(precoVenda * quantidade);

        return operacaoRepository.save(venda);
    }

    public List<OperacaoAcao> listarHistorico(Long acaoId) {

        Acao acao = repository.findById(acaoId)
                .orElseThrow(() ->
                        new RuntimeException("Ação não encontrada"));

        return operacaoRepository.findByAcao(acao);
    }

    public List<OperacaoAcao> listarCompras() {

        return operacaoRepository.findByTipoOperacao(
                TipoOperacao.COMPRA
        );
    }

    public List<OperacaoAcao> listarVendas() {

        return operacaoRepository.findByTipoOperacao(
                TipoOperacao.VENDA
        );
    }

    public OperacaoAcao buscarPorId(Long id) {

        return operacaoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Operação não encontrada"
                        ));
    }

    public List<OperacaoAcao> listar() {

        return operacaoRepository.findAll();
    }
}