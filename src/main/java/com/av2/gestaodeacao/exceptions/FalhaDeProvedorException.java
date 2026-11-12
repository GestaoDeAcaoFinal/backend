package com.av2.gestaodeacao.exceptions;

public class FalhaDeProvedorException extends RuntimeException {
    public FalhaDeProvedorException(String provedor) {
        super("O provedor " + provedor + " retornou dados inválidos ou uma resposta de erro");
    }

    public FalhaDeProvedorException(String provedor, Throwable cause) {
        super("O provedor " + provedor + " retornou dados inválidos ou uma resposta de erro", cause);
    }

    public FalhaDeProvedorException(String provedor, int status, Throwable cause) {
        super(status == 401 || status == 403
                ? "O provedor " + provedor + " recusou a autenticação ou o acesso. "
                    + "Verifique a chave da integração e as permissões do plano."
                : "O provedor " + provedor + " retornou dados inválidos ou uma resposta de erro", cause);
    }
}
