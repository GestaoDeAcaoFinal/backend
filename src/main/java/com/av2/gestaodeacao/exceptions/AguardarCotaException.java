package com.av2.gestaodeacao.exceptions;

/** Orçamento local esgotado; nenhuma chamada ao provedor foi iniciada. */
public class AguardarCotaException extends ProvedorIndisponivelException {
    private final long tentarAposSegundos;
    public AguardarCotaException(long segundos) {
        super("Twelve Data", "Aguarde a liberação da próxima cota de consultas.");
        tentarAposSegundos = Math.max(1, Math.min(86_400, segundos));
    }
    public long getTentarAposSegundos() { return tentarAposSegundos; }
}
