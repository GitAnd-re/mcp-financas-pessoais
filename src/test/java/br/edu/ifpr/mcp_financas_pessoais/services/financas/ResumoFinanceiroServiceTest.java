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
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.ResumoFinanceiro;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryLancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;

class ResumoFinanceiroServiceTest {
   private InMemoryLancamentoRepositorio repositorio;
   private ResumoFinanceiroService service;

   @BeforeEach
   void setUp() {
      repositorio = new InMemoryLancamentoRepositorio();
      service = new ResumoFinanceiroService(repositorio);
   }

   @Test
   @DisplayName("Somar receitas, gastos e gastos por categoria no período")
   void calculaResumoDoMes() throws IOException {
      repositorio.salvar(lancamento("r1", Lancamento.TipoLancamento.RECEITA,
            "Salário", "1000.00", LocalDate.of(2026, 11, 1)));
      repositorio.salvar(lancamento("g1", Lancamento.TipoLancamento.GASTO,
            "Mercado", "50.25", LocalDate.of(2026, 11, 2)));
      repositorio.salvar(lancamento("g2", Lancamento.TipoLancamento.GASTO,
            "Mercado", "10.75", LocalDate.of(2026, 11, 30)));
      repositorio.salvar(lancamento("g3", Lancamento.TipoLancamento.GASTO,
            "Lazer", "100.00", LocalDate.of(2026, 11, 15)));
      repositorio.salvar(lancamento("outro-mes", Lancamento.TipoLancamento.GASTO,
            "Mercado", "999.00", LocalDate.of(2026, 10, 31)));
      repositorio.salvar(lancamento("outro-ano", Lancamento.TipoLancamento.RECEITA,
            "Salário", "999.00", LocalDate.of(2025, 11, 30)));

      ResumoFinanceiro resumo = service.resumir(11, 2026);

      assertEquals(new BigDecimal("1000.00"), resumo.totalReceitas());
      assertEquals(new BigDecimal("161.00"), resumo.totalGastos());
      assertEquals(new BigDecimal("839.00"), resumo.saldo());
      assertEquals(new BigDecimal("61.00"), resumo.gastosPorCategoria().get("Mercado"));
      assertEquals(new BigDecimal("100.00"), resumo.gastosPorCategoria().get("Lazer"));
   }

   @Test
   @DisplayName("Retornar totais zerados quando não houver lançamentos")
   void retornaResumoZerado() throws IOException {
      ResumoFinanceiro resumo = service.resumir(1, 2040);

      assertEquals(BigDecimal.ZERO, resumo.totalReceitas());
      assertEquals(BigDecimal.ZERO, resumo.totalGastos());
      assertEquals(BigDecimal.ZERO, resumo.saldo());
      assertEquals(0, resumo.gastosPorCategoria().size());
   }

   @Test
   @DisplayName("Resumir o ano inteiro quando o mês não for informado")
   void resumeAnoInteiroQuandoMesOmitido() throws IOException {
      repositorio.salvar(lancamento("novembro", Lancamento.TipoLancamento.GASTO,
            "Mercado", "10.00", LocalDate.of(2026, 11, 2)));
      repositorio.salvar(lancamento("outubro", Lancamento.TipoLancamento.GASTO,
            "Mercado", "20.00", LocalDate.of(2026, 10, 20)));
      repositorio.salvar(lancamento("outro-ano", Lancamento.TipoLancamento.GASTO,
            "Mercado", "999.00", LocalDate.of(2025, 11, 20)));

      ResumoFinanceiro resumo = service.resumir(null, 2026);

      assertEquals(null, resumo.mes());
      assertEquals(new BigDecimal("30.00"), resumo.totalGastos());
      assertEquals(new BigDecimal("30.00"), resumo.gastosPorCategoria().get("Mercado"));
   }

   @Test
   @DisplayName("Aceitar os meses de limite")
   void aceitaMesesLimite() throws IOException {
      assertEquals(1, service.resumir(1, 2026).mes());
      assertEquals(12, service.resumir(12, 2026).mes());
   }

   @Test
   @DisplayName("Rejeitar mês fora do intervalo de 1 a 12")
   void rejeitaMesInvalido() {
      for (int mes : new int[] { -1, 0, 13 })
         assertEquals("O mês deve estar entre 1 e 12.",
               assertThrows(IllegalArgumentException.class,
                     () -> service.resumir(mes, 2026)).getMessage());
   }

   @Test
   @DisplayName("Rejeitar ano não positivo")
   void rejeitaAnoInvalido() {
      for (int ano : new int[] { -1, 0 })
         assertEquals("O ano deve ser maior que zero.",
               assertThrows(IllegalArgumentException.class,
                     () -> service.resumir(11, ano)).getMessage());
   }

   @Test
   @DisplayName("Rejeitar repositório nulo")
   void rejeitaRepositorioNulo() {
      assertEquals("O repositório de lançamentos é obrigatório.",
            assertThrows(IllegalArgumentException.class,
                  () -> new ResumoFinanceiroService(null)).getMessage());
   }

   @Test
   @DisplayName("Propagar falha de leitura do repositório")
   void propagaFalhaDoRepositorio() {
      ResumoFinanceiroService serviceComFalha = new ResumoFinanceiroService(
            new LancamentoRepositorio() {
               @Override public void salvar(Lancamento lancamento) { }
               @Override public List<Lancamento> listar() throws IOException {
                  throw new IOException("falha de leitura");
               }
            });

      assertEquals("falha de leitura", assertThrows(IOException.class,
            () -> serviceComFalha.resumir(11, 2026)).getMessage());
   }

   private static Lancamento lancamento(String id,
                                        Lancamento.TipoLancamento tipo,
                                        String categoria,
                                        String valor,
                                        LocalDate data) {
      return new Lancamento(id, tipo, new BigDecimal(valor),
            tipo == Lancamento.TipoLancamento.GASTO ? categoria : null,
            tipo == Lancamento.TipoLancamento.RECEITA ? categoria : null,
            data, "descrição");
   }
}
