package com.av2.gestaodeacao.services;


import com.av2.gestaodeacao.cliente.CnpjCliente;
import com.av2.gestaodeacao.cliente.ViaCepCliente;
import com.av2.gestaodeacao.domains.Corretora;
import com.av2.gestaodeacao.domains.dtos.CorretoraRequestDTO;
import com.av2.gestaodeacao.repositories.CorretoraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CorretoraService {

    @Autowired
    private CorretoraRepository repository;

    public Corretora salvar(CorretoraRequestDTO dto) {

        Corretora c = new Corretora();

        c.setCnpj(dto.getCnpj());
        c.setCep(dto.getCep());

        if (repository.findByCnpj(c.getCnpj()).isPresent()) {
            throw new RuntimeException("CNPJ já cadastrado");
        }

        try {
            c.setCep(c.getCep().replace("-", ""));

            Map cepData = viaCepCliente.buscarCep(c.getCep());

            if (cepData.get("erro") != null) {
                throw new RuntimeException("CEP inválido");
            }

            c.setLogradouro((String) cepData.get("logradouro"));
            c.setBairro((String) cepData.get("bairro"));
            c.setCidade((String) cepData.get("localidade"));
            c.setUf((String) cepData.get("uf"));

        } catch (Exception e) {
            throw new RuntimeException("Erro ao consultar API de CEP");
        }

        try {
            Map dadosCnpj = cnpjCliente.buscarCnpj(c.getCnpj());

            if (dadosCnpj == null || dadosCnpj.get("razao_social") == null) {
                throw new RuntimeException("CNPJ inválido");
            }

            c.setRazaoSocial((String) dadosCnpj.get("razao_social"));
            c.setNomeFantasia((String) dadosCnpj.get("nome_fantasia"));
            c.setEmail((String) dadosCnpj.get("email"));
            c.setTelefone((String) dadosCnpj.get("ddd_telefone_1"));
            c.setSituacaoCadastral((String) dadosCnpj.get("descricao_situacao_cadastral"));

        } catch (Exception e) {
            throw new RuntimeException("Erro ao consultar API de CNPJ");
        }

        c.setValidadaNaCvm(true);
        c.setDataCadastro(LocalDateTime.now());

        return repository.save(c);
    }

    public List<Corretora> listar() {
        return repository.findAll();
    }

    public Corretora buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Não encontrada"));
    }

    public Corretora buscarPorCnpj(String cnpj) {
        return repository.findByCnpj(cnpj)
                .orElseThrow(() -> new RuntimeException("Não encontrada"));
    }

    @Autowired
    private ViaCepCliente viaCepCliente;

    @Autowired
    private CnpjCliente cnpjCliente;
}