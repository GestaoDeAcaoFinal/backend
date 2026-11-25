package com.av2.gestaodeacao.services.cvm;

import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CadastroCvmParserTest {

    private final CadastroCvmParser parser = new CadastroCvmParser();

    @ParameterizedTest
    @MethodSource("arquivosAusentes")
    void rejeitaArquivoNuloOuVazioComMensagemDoProvedor(byte[] arquivo) {
        assertThatThrownBy(() -> parser.parse(arquivo))
                .isExactlyInstanceOf(FalhaDeProvedorException.class)
                .hasMessage("O provedor CVM retornou dados inválidos ou uma resposta de erro");
    }

    private static Stream<byte[]> arquivosAusentes() {
        return Stream.of(null, new byte[0]);
    }

    @Test
    void encontraCsvEmSubpastaDepoisDeIgnorarDiretorioEOutroArquivo() {
        Map<String, byte[]> entradas = new LinkedHashMap<>();
        entradas.put("cad_intermed.csv/", new byte[0]);
        entradas.put("outro.csv", "CNPJ;SIT\n123;CANCELADA\n".getBytes(StandardCharsets.US_ASCII));
        entradas.put("dados\\cad_intermed.csv", ("CNPJ;SIT\n"
                + "12.345.678/0001-90;EM FUNCIONAMENTO NORMAL\n").getBytes(StandardCharsets.US_ASCII));

        assertThat(parser.parse(zipComEntradas(entradas))).containsExactly("12345678000190");
    }

    @Test
    void aceitaBomUtf8AntesDoCabecalhoInterpretadoEmWindows1252() {
        byte[] csv = ("\uFEFFCNPJ;SIT\n"
                + "12.345.678/0001-90;EM FUNCIONAMENTO NORMAL\n").getBytes(StandardCharsets.UTF_8);

        assertThat(parser.parse(zipComEntrada("cad_intermed.csv", csv)))
                .containsExactly("12345678000190");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SIT;DENOM_SOCIAL\nCANCELADA;Teste\n", "OUTRA;DENOM_SOCIAL\nx;Teste\n"})
    void rejeitaCabecalhoSemCnpjMesmoQuandoSituacaoEstaPresente(String csv) {
        assertThatThrownBy(() -> parser.parse(zipComCsv(csv)))
                .isExactlyInstanceOf(FalhaDeProvedorException.class)
                .hasMessage("O provedor CVM retornou dados inválidos ou uma resposta de erro");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12.345.678/0001-900"})
    void rejeitaCnpjDeParticipanteAtivoComQuantidadeInvalidaDeDigitos(String cnpj) {
        String csv = "CNPJ;SIT\n" + cnpj + ";EM FUNCIONAMENTO NORMAL\n";

        assertThatThrownBy(() -> parser.parse(zipComCsv(csv)))
                .isExactlyInstanceOf(FalhaDeProvedorException.class)
                .hasMessage("O provedor CVM retornou dados inválidos ou uma resposta de erro")
                .hasMessageNotContaining(cnpj);
    }

    @Test
    void converteCsvComAspasSemFechamentoEmFalhaSeguraDoProvedor() {
        assertThatThrownBy(() -> parser.parse(zipComCsv("CNPJ;SIT\n\"conteudo-interno")))
                .isExactlyInstanceOf(FalhaDeProvedorException.class)
                .hasMessage("O provedor CVM retornou dados inválidos ou uma resposta de erro")
                .hasMessageNotContaining("conteudo-interno")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void cadastroSomenteComCabecalhoRetornaConjuntoVazioImutavel() {
        Set<String> ativos = parser.parse(zipComCsv("CNPJ;SIT\n"));

        assertThat(ativos).isEmpty();
        assertThatThrownBy(() -> ativos.add("12345678000190"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void normalizaSituacaoEDeduplicaCnpjsSemIncluirCadastroInativoInvalido() {
        Set<String> ativos = parser.parse(zipComCsv("""
                CNPJ;SIT
                 12.345.678/0001-90 ; em funcionamento normal
                12345678000190;EM FUNCIONAMENTO NORMAL
                invalido;CANCELADA
                """));

        assertThat(ativos).containsExactly("12345678000190");
    }

    @Test
    void indexaSomenteParticipantesAtivosComCnpjNormalizadoEConjuntoImutavel() {
        byte[] zip = zipComCsv("""
                CNPJ;SIT;DENOM_SOCIAL
                12.345.678/0001-90;EM FUNCIONAMENTO NORMAL;Ativa Çorretora
                98.765.432/0001-10;CANCELADA;Inativa
                ;EM FUNCIONAMENTO NORMAL;Sem CNPJ
                """);

        Set<String> ativos = parser.parse(zip);

        assertThat(ativos).containsExactly("12345678000190");
        assertThat(ativos).doesNotContain("98765432000110", "00000000000000");
        assertThatThrownBy(() -> ativos.add("11111111000111"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejeitaZipAusenteCorrompidoOuSemColunasObrigatorias() {
        assertThatThrownBy(() -> parser.parse(new byte[]{1, 2, 3}))
                .isInstanceOf(FalhaDeProvedorException.class);
        assertThatThrownBy(() -> parser.parse(zipComCsv("CNPJ;DENOM_SOCIAL\n123;Teste\n")))
                .isInstanceOf(FalhaDeProvedorException.class);
        assertThatThrownBy(() -> parser.parse(zipComEntrada("outro.csv", "CNPJ;SIT\n".getBytes())))
                .isInstanceOf(FalhaDeProvedorException.class);
    }

    @Test
    void rejeitaConteudoQueNaoPodeSerDecodificadoEmWindows1252() {
        byte[] csv = "CNPJ;SIT\n".getBytes(Charset.forName("windows-1252"));
        byte[] invalido = new byte[csv.length + 1];
        System.arraycopy(csv, 0, invalido, 0, csv.length);
        invalido[invalido.length - 1] = (byte) 0x81;

        assertThatThrownBy(() -> parser.parse(zipComEntrada("cad_intermed.csv", invalido)))
                .isInstanceOf(FalhaDeProvedorException.class);
    }

    private byte[] zipComCsv(String csv) {
        return zipComEntrada("cad_intermed.csv", csv.stripIndent().getBytes(Charset.forName("windows-1252")));
    }

    private byte[] zipComEntrada(String nome, byte[] conteudo) {
        return zipComEntradas(Map.of(nome, conteudo));
    }

    private byte[] zipComEntradas(Map<String, byte[]> entradas) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                for (var entrada : entradas.entrySet()) {
                    zip.putNextEntry(new ZipEntry(entrada.getKey()));
                    zip.write(entrada.getValue());
                    zip.closeEntry();
                }
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
