package com.av2.gestaodeacao.resources;

import com.av2.gestaodeacao.domains.Corretora;
import com.av2.gestaodeacao.domains.dtos.CorretoraRequestDTO;
import com.av2.gestaodeacao.services.CorretoraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/corretoras")
public class CorretoraResource {

    @Autowired
    private CorretoraService service;

    @PostMapping
    public Corretora salvar(@RequestBody CorretoraRequestDTO dto) {
        return service.salvar(dto);
    }

    @GetMapping
    public List<Corretora> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Corretora buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/cnpj/{cnpj}")
    public Corretora buscarPorCnpj(@PathVariable String cnpj) {
        return service.buscarPorCnpj(cnpj);
    }
}
