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
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Orcamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryLancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryOrcamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.OrcamentoRepositorio;

class ListarCategoriasServiceTest {
   private InMemoryLancamentoRepositorio lancamentos;
   private InMemoryOrcamentoRepositorio orcamentos;
   private ListarCategoriasService service;

   @BeforeEach
   void setUp() {
      lancamentos = new InMemoryLancamentoRepositorio();
      orcamentos = new InMemoryOrcamentoRepositorio();
      service = new ListarCategoriasService(lancamentos, orcamentos);
   }

   @Test
   @DisplayName("Listar categorias de gastos e orçamentos sem duplicatas")
   void listaCategoriasUnicasOrdenadas() throws IOException {
      lancamentos.salvar(lancamento(Lancamento.TipoLancamento.GASTO,
            " Mercado ", "30.00"));
      lancamentos.salvar(lancamento(Lancamento.TipoLancamento.GASTO,
            "Curso", "10.00"));
      lancamentos.salvar(lancamento(Lancamento.TipoLancamento.RECEITA,
            "Salário", "100.00"));
      orcamentos.salvar(new Orcamento("Mercado", 11, 2026,
            new BigDecimal("200.00")));
      orcamentos.salvar(new Orcamento("Moradia", 11, 2026,
            new BigDecimal("500.00")));

      assertEquals(List.of("Curso", "Mercado", "Moradia"), service.listar());
   }

   @Test
   @DisplayName("Retornar lista vazia quando não houver categorias")
   void listaVazia() throws IOException {
      assertEquals(List.of(), service.listar());
   }

   @Test
   @DisplayName("Rejeitar repositórios ausentes")
   void rejeitaRepositorioNulo() {
      assertEquals("O repositório de lançamentos é obrigatório.",
            assertThrows(IllegalArgumentException.class,
                  () -> new ListarCategoriasService(null, orcamentos))
                  .getMessage());
      assertEquals("O repositório de orçamentos é obrigatório.",
            assertThrows(IllegalArgumentException.class,
                  () -> new ListarCategoriasService(lancamentos, null))
                  .getMessage());
   }

   @Test
   @DisplayName("Propagar falhas de leitura dos repositórios")
   void propagaFalhaDeLeitura() {
      LancamentoRepositorio lancamentoComFalha = new LancamentoRepositorio() {
         @Override
         public void salvar(Lancamento lancamento) { }

         @Override
         public List<Lancamento> listar() throws IOException {
            throw new IOException("falha nos lançamentos");
         }
      };
      ListarCategoriasService serviceComFalha = new ListarCategoriasService(
            lancamentoComFalha, orcamentos);
      assertEquals("falha nos lançamentos", assertThrows(IOException.class,
            serviceComFalha::listar).getMessage());

      OrcamentoRepositorio orcamentoComFalha = new OrcamentoRepositorio() {
         @Override
         public void salvar(Orcamento orcamento) { }

         @Override
         public List<Orcamento> listar() throws IOException {
            throw new IOException("falha nos orçamentos");
         }
      };
      serviceComFalha = new ListarCategoriasService(lancamentos,
            orcamentoComFalha);
      assertEquals("falha nos orçamentos", assertThrows(IOException.class,
            serviceComFalha::listar).getMessage());
   }

   private static Lancamento lancamento(Lancamento.TipoLancamento tipo,
                                        String categoria, String valor) {
      return new Lancamento("id", tipo, new BigDecimal(valor), categoria,
            null, LocalDate.of(2026, 11, 1), "");
   }
}
