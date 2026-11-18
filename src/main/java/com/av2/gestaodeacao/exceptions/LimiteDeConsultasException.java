package com.av2.gestaodeacao.exceptions;

public class LimiteDeConsultasException extends ProvedorIndisponivelException {
    private final long tentarAposSegundos;
    public LimiteDeConsultasException(String provedor, long tentarAposSegundos) {
        super(provedor, "O limite de consultas foi atingido. Aguarde antes de tentar novamente e confira a cota do plano.");
        this.tentarAposSegundos = Math.max(1, Math.min(86_400, tentarAposSegundos));
    }
    public long getTentarAposSegundos() { return tentarAposSegundos; }
}
