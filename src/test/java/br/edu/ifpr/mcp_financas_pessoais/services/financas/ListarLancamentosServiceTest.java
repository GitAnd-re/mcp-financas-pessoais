package br.edu.ifpr.mcp_financas_pessoais.services.financas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryLancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;

class ListarLancamentosServiceTest {
   private InMemoryLancamentoRepositorio repositorio;
   private ListarLancamentosService service;

   @BeforeEach
   void setUp() {
      repositorio = new InMemoryLancamentoRepositorio();
      service = new ListarLancamentosService(repositorio);
   }

   @Test
   @DisplayName("Rejeitar repositório nulo")
   void rejeitaRepositorioNulo() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new ListarLancamentosService(null));
      assertEquals("O repositório de lançamentos é obrigatório.", exception.getMessage());
   }

   @Test
   @DisplayName("Listar lançamentos do mês e ano sem filtro de categoria")
   void listaPorMesEAno() throws IOException {
      Lancamento gasto = lancamento("gasto", Lancamento.TipoLancamento.GASTO,
            "Mercado", null, LocalDate.of(2026, 11, 2));
      Lancamento receita = lancamento("receita", Lancamento.TipoLancamento.RECEITA,
            null, "Salário", LocalDate.of(2026, 11, 20));
      Lancamento outroMes = lancamento("outro", Lancamento.TipoLancamento.GASTO,
            "Mercado", null, LocalDate.of(2026, 10, 20));
      repositorio.salvar(gasto);
      repositorio.salvar(receita);
      repositorio.salvar(outroMes);

      assertEquals(List.of(gasto, receita), service.listar(11, 2026, null));
   }

   @Test
   @DisplayName("Listar o ano inteiro quando o mês não for informado")
   void listaAnoInteiroQuandoMesOmitido() throws IOException {
      Lancamento novembro = lancamento("novembro", Lancamento.TipoLancamento.GASTO,
            "Mercado", null, LocalDate.of(2026, 11, 2));
      Lancamento outubro = lancamento("outubro", Lancamento.TipoLancamento.GASTO,
            "Mercado", null, LocalDate.of(2026, 10, 20));
      Lancamento outroAno = lancamento("outro-ano", Lancamento.TipoLancamento.GASTO,
            "Mercado", null, LocalDate.of(2025, 11, 20));
      repositorio.salvar(novembro);
      repositorio.salvar(outubro);
      repositorio.salvar(outroAno);

      assertEquals(List.of(novembro, outubro), service.listar(null, 2026, null));
   }

   @Test
   @DisplayName("Filtrar lançamentos por categoria")
   void filtraPorCategoria() throws IOException {
      Lancamento mercado = lancamento("mercado", Lancamento.TipoLancamento.GASTO,
            "Mercado", null, LocalDate.of(2026, 11, 2));
      Lancamento lazer = lancamento("lazer", Lancamento.TipoLancamento.GASTO,
            "Lazer", null, LocalDate.of(2026, 11, 20));
      repositorio.salvar(mercado);
      repositorio.salvar(lazer);

      assertEquals(List.of(mercado), service.listar(11, 2026, " Mercado "));
   }

   @Test
   @DisplayName("Retornar lista vazia quando não houver lançamentos")
   void retornaListaVazia() throws IOException {
      assertEquals(List.of(), service.listar(11, 2026, null));
   }

   @Test
   @DisplayName("Rejeitar mês fora do intervalo de 1 a 12")
   void rejeitaMesInvalido() {
      for (int mes : new int[] { -1, 0, 13 }) {
         IllegalArgumentException exception = assertThrows(
               IllegalArgumentException.class,
               () -> service.listar(mes, 2026, null));
         assertEquals("O mês deve estar entre 1 e 12.", exception.getMessage());
      }
   }

   @Test
   @DisplayName("Rejeitar ano não positivo")
   void rejeitaAnoInvalido() {
      for (int ano : new int[] { -1, 0 }) {
         assertEquals("O ano deve ser maior que zero.",
               assertThrows(IllegalArgumentException.class,
                     () -> service.listar(11, ano, null)).getMessage());
      }
   }

   @Test
   @DisplayName("Rejeitar categoria em branco")
   void rejeitaCategoriaVazia() {
      for (String categoria : new String[] { "", "   " }) {
         assertEquals("A categoria não pode ser vazia.",
               assertThrows(IllegalArgumentException.class,
                     () -> service.listar(11, 2026, categoria)).getMessage());
      }
   }

   @Test
   @DisplayName("Propagar falha do repositório")
   void propagaFalhaDoRepositorio() {
      ListarLancamentosService serviceComFalha = new ListarLancamentosService(
            new LancamentoRepositorio() {
               @Override
               public void salvar(Lancamento lancamento) { }

               @Override
               public List<Lancamento> listar() throws IOException {
                  throw new IOException("falha de leitura");
               }
            });

      IOException exception = assertThrows(IOException.class,
            () -> serviceComFalha.listar(11, 2026, null));
      assertEquals("falha de leitura", exception.getMessage());
   }

   private static Lancamento lancamento(String id,
                                        Lancamento.TipoLancamento tipo,
                                        String categoria,
                                        String fonte,
                                        LocalDate data) {
      return new Lancamento(id, tipo, new BigDecimal("10.00"), categoria, fonte,
            data, "descrição");
   }
}
