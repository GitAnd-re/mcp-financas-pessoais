package br.edu.ifpr.mcp_financas_pessoais.services.financas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Orcamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.SituacaoOrcamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryLancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryOrcamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.OrcamentoRepositorio;

class VerificarOrcamentoServiceTest {
   private InMemoryLancamentoRepositorio lancamentos;
   private InMemoryOrcamentoRepositorio orcamentos;
   private VerificarOrcamentoService service;

   @BeforeEach
   void setUp() {
      lancamentos = new InMemoryLancamentoRepositorio();
      orcamentos = new InMemoryOrcamentoRepositorio();
      service = new VerificarOrcamentoService(lancamentos, orcamentos);
   }

   @Test
   @DisplayName("Rejeitar repositório de lançamentos nulo")
   void rejeitaLancamentoRepositorioNulo() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new VerificarOrcamentoService(null, orcamentos));
      assertEquals("O repositório de lançamentos é obrigatório.", exception.getMessage());
   }

   @Test
   @DisplayName("Rejeitar repositório de orçamentos nulo")
   void rejeitaOrcamentoRepositorioNulo() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new VerificarOrcamentoService(lancamentos, null));
      assertEquals("O repositório de orçamentos é obrigatório.", exception.getMessage());
   }

   @Test
   @DisplayName("Somar gastos do período e comparar com limites")
   void verificaGastosNoPeriodo() throws IOException {
      orcamentos.salvar(new Orcamento("Mercado", new BigDecimal("100.00")));
      orcamentos.salvar(new Orcamento("Lazer", new BigDecimal("50.00")));
      adicionarLancamento("1", Lancamento.TipoLancamento.GASTO, "Mercado",
            "40.00", LocalDate.of(2026, 11, 1));
      adicionarLancamento("2", Lancamento.TipoLancamento.GASTO, "Mercado",
            "75.50", LocalDate.of(2026, 11, 30));
      adicionarLancamento("3", Lancamento.TipoLancamento.RECEITA, "Mercado",
            "200.00", LocalDate.of(2026, 11, 10));
      adicionarLancamento("4", Lancamento.TipoLancamento.GASTO, "Mercado",
            "90.00", LocalDate.of(2026, 10, 10));

      List<SituacaoOrcamento> resultado = service.verificar(11, 2026);
      assertEquals(2, resultado.size());
      SituacaoOrcamento lazer = resultado.get(0);
      assertEquals("Lazer", lazer.categoria());
      assertEquals(BigDecimal.ZERO, lazer.gasto());
      assertEquals(new BigDecimal("50.00"), lazer.limite());
      assertEquals(new BigDecimal("50.00"), lazer.restante());
      assertFalse(lazer.estourou());

      SituacaoOrcamento mercado = resultado.get(1);
      assertEquals(new BigDecimal("115.50"), mercado.gasto());
      assertEquals(new BigDecimal("100.00"), mercado.limite());
      assertEquals(new BigDecimal("-15.50"), mercado.restante());
      assertTrue(mercado.estourou());
   }

   @Test
   @DisplayName("Rejeitar mês fora do intervalo")
   void rejeitaMesInvalido() {
      for (int mes : new int[] { 0, 13 }) {
         IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
               () -> service.verificar(mes, 2026));
         assertEquals("O mês deve estar entre 1 e 12.", exception.getMessage());
      }
      assertTrue(lancamentos.listar().isEmpty());
   }

   @Test
   @DisplayName("Rejeitar ano menor que um")
   void rejeitaAnoInvalido() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.verificar(1, 0));
      assertEquals("O ano deve ser maior que zero.", exception.getMessage());
      assertTrue(lancamentos.listar().isEmpty());
   }

   @Test
   @DisplayName("Informar quando ainda não há orçamentos definidos")
   void rejeitaListaDeOrcamentosVazia() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.verificar(11, 2026));
      assertEquals("Nenhum orçamento foi definido.", exception.getMessage());
   }

   @Test
   @DisplayName("Propagar falha ao ler lançamentos")
   void propagaFalhaAoLerLancamentos() {
      LancamentoRepositorio comFalha = new LancamentoRepositorio() {
         @Override
         public void salvar(Lancamento lancamento) { }

         @Override
         public List<Lancamento> listar() throws IOException {
            throw new IOException("falha ao ler lançamentos");
         }
      };
      orcamentos.salvar(new Orcamento("Mercado", BigDecimal.TEN));
      VerificarOrcamentoService serviceComFalha = new VerificarOrcamentoService(
            comFalha, orcamentos);
      IOException exception = assertThrows(IOException.class,
            () -> serviceComFalha.verificar(11, 2026));
      assertEquals("falha ao ler lançamentos", exception.getMessage());
   }

   @Test
   @DisplayName("Propagar falha ao ler orçamentos")
   void propagaFalhaAoLerOrcamentos() {
      OrcamentoRepositorio comFalha = new OrcamentoRepositorio() {
         @Override
         public void salvar(Orcamento orcamento) { }

         @Override
         public List<Orcamento> listar() throws IOException {
            throw new IOException("falha ao ler orçamentos");
         }
      };
      VerificarOrcamentoService serviceComFalha = new VerificarOrcamentoService(
            lancamentos, comFalha);
      IOException exception = assertThrows(IOException.class,
            () -> serviceComFalha.verificar(11, 2026));
      assertEquals("falha ao ler orçamentos", exception.getMessage());
   }

   private void adicionarLancamento(String id, Lancamento.TipoLancamento tipo,
                                    String categoria, String valor, LocalDate data) {
      lancamentos.salvar(new Lancamento(id, tipo, new BigDecimal(valor),
            categoria, null, data, ""));
   }
}
