package br.edu.ifpr.mcp_financas_pessoais.services.financas.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import tools.jackson.databind.exc.MismatchedInputException;

class JsonLancamentoRepositorioTest {
   @TempDir
   Path tempDir;

   @Test
   @DisplayName("Rejeitar caminho nulo")
   void rejeitaCaminhoNulo() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new JsonLancamentoRepositorio(null));
      assertEquals("O caminho do arquivo JSON é obrigatório.", exception.getMessage());
   }

   @Test
   @DisplayName("Listar vazio quando o arquivo ainda não existe")
   void listaVazioSemArquivo() throws IOException {
      JsonLancamentoRepositorio repositorio = repositorio("dados/lancamentos.json");

      assertTrue(repositorio.listar().isEmpty());
   }

   @Test
   @DisplayName("Salvar criando diretórios e recuperar lançamento")
   void salvaERecuperaLancamento() throws IOException {
      Path arquivo = tempDir.resolve("dados/financeiro/lancamentos.json");
      JsonLancamentoRepositorio repositorio = new JsonLancamentoRepositorio(arquivo);
      Lancamento lancamento = lancamento("gasto-1", new BigDecimal("25.90"),
            "Alimentação", null, LocalDate.of(2026, 11, 20), "Almoço");

      repositorio.salvar(lancamento);

      assertTrue(Files.isRegularFile(arquivo));
      Lancamento recuperado = repositorio.listar().get(0);
      assertEquals(lancamento.id(), recuperado.id());
      assertEquals(lancamento.tipo(), recuperado.tipo());
      assertEquals(0, lancamento.valor().compareTo(recuperado.valor()));
      assertEquals(lancamento.categoria(), recuperado.categoria());
      assertEquals(lancamento.data(), recuperado.data());
      assertEquals(lancamento.descricao(), recuperado.descricao());
      String json = Files.readString(arquivo);
      assertTrue(json.contains("\"GASTO\""));
      assertTrue(json.contains("25.90"));
      assertTrue(json.contains("20/11/2026"));
   }

   @Test
   @DisplayName("Acrescentar lançamentos existentes")
   void acrescentaLancamentos() throws IOException {
      JsonLancamentoRepositorio repositorio = repositorio("lancamentos.json");
      Lancamento primeiro = lancamento("gasto-1", new BigDecimal("10.00"),
            "Mercado", null, LocalDate.of(2026, 11, 19), "Compras");
      Lancamento segundo = lancamento("gasto-2", new BigDecimal("5.50"),
            "Transporte", null, LocalDate.of(2026, 11, 20), "Ônibus");

      repositorio.salvar(primeiro);
      repositorio.salvar(segundo);

      List<Lancamento> recuperados = repositorio.listar();
      assertEquals(2, recuperados.size());
      assertEquals(primeiro.id(), recuperados.get(0).id());
      assertEquals(0, primeiro.valor().compareTo(recuperados.get(0).valor()));
      assertEquals(segundo.id(), recuperados.get(1).id());
      assertEquals(0, segundo.valor().compareTo(recuperados.get(1).valor()));
   }

   @Test
   @DisplayName("Permitir campos opcionais nulos ao ler")
   void recuperaCamposOpcionaisNulos() throws IOException {
      JsonLancamentoRepositorio repositorio = repositorio("lancamentos.json");
      Lancamento lancamento = lancamento("receita-1", new BigDecimal("100.00"),
            null, "Salário", LocalDate.of(2026, 11, 20), null);
      repositorio.salvar(lancamento);

      Lancamento recuperado = repositorio.listar().get(0);

      assertNull(recuperado.categoria());
      assertEquals("Salário", recuperado.fonte());
      assertNull(recuperado.descricao());
   }

   @Test
   @DisplayName("Rejeitar lançamento nulo")
   void rejeitaLancamentoNulo() {
      JsonLancamentoRepositorio repositorio = repositorio("lancamentos.json");

      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> repositorio.salvar(null));
      assertEquals("O lançamento é obrigatório.", exception.getMessage());
   }

   @Test
   @DisplayName("Propagar erro quando arquivo JSON estiver malformado")
   void rejeitaJsonMalformado() throws IOException {
      Path arquivo = tempDir.resolve("lancamentos.json");
      Files.writeString(arquivo, "{ json inválido");
      JsonLancamentoRepositorio repositorio = new JsonLancamentoRepositorio(arquivo);

      assertThrows(MismatchedInputException.class, repositorio::listar);
   }

   @Test
   @DisplayName("Rejeitar lançamento com campo obrigatório ausente")
   void rejeitaCampoObrigatorioAusente() throws IOException {
      Path arquivo = tempDir.resolve("lancamentos.json");
      Files.writeString(arquivo, "[{\"id\":null,\"tipo\":\"GASTO\",\"valor\":1,\"data\":\"20/11/2026\"}]");
      JsonLancamentoRepositorio repositorio = new JsonLancamentoRepositorio(arquivo);

      assertThrows(IllegalArgumentException.class, repositorio::listar);
   }

   private JsonLancamentoRepositorio repositorio(String arquivo) {
      return new JsonLancamentoRepositorio(tempDir.resolve(arquivo));
   }

   private static Lancamento lancamento(String id, BigDecimal valor,
         String categoria, String fonte, LocalDate data, String descricao) {
      Lancamento.TipoLancamento tipo = fonte == null
            ? Lancamento.TipoLancamento.GASTO
            : Lancamento.TipoLancamento.RECEITA;
      return new Lancamento(id, tipo, valor, categoria, fonte, data, descricao);
   }
}
