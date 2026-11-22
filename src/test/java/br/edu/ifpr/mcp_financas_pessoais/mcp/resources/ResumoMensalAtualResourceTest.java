package br.edu.ifpr.mcp_financas_pessoais.mcp.resources;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryLancamentoRepositorio;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;

class ResumoMensalAtualResourceTest {
   @Test
   @DisplayName("Resumir apenas o mês atual do relógio fornecido")
   void resumeMesAtual() {
      InMemoryLancamentoRepositorio lancamentos = new InMemoryLancamentoRepositorio();
      lancamentos.salvar(lancamento("Casa", "12.50", LocalDate.of(2026, 11, 5)));
      lancamentos.salvar(lancamento("Mercado", "40.00", LocalDate.of(2026, 10, 5)));
      ResumoMensalAtualResource resource = new ResumoMensalAtualResource(
            lancamentos, relogioFixo());

      String content = ler(resource);

      assertTrue(content.startsWith("Resumo financeiro de 11/2026:"), content);
      assertTrue(content.contains("total_gastos:12.50"), content);
      assertTrue(content.contains("Casa:12.50"), content);
      assertFalse(content.contains("Mercado"), content);
   }

   @Test
   @DisplayName("Informar ausência de gastos no resumo mensal atual")
   void resumeMesSemLancamentos() {
      ResumoMensalAtualResource resource = new ResumoMensalAtualResource(
            new InMemoryLancamentoRepositorio(), relogioFixo());

      String content = ler(resource);

      assertTrue(content.contains("total_receitas:0.00"), content);
      assertTrue(content.contains("total_gastos:0.00"), content);
      assertTrue(content.contains("Nenhum gasto no período."), content);
   }

   private static String ler(ResumoMensalAtualResource resource) {
      return ((TextResourceContents) resource.read(null,
            ReadResourceRequest.builder(
                  McpConsts.RESOURCE_RESUMO_MENSAL_ATUAL_URI).build())
            .contents().get(0)).text();
   }

   private static Clock relogioFixo() {
      return Clock.fixed(Instant.parse("2026-11-22T15:00:00Z"), ZoneId.of("UTC"));
   }

   private static Lancamento lancamento(String categoria, String valor,
                                        LocalDate data) {
      return new Lancamento("id", Lancamento.TipoLancamento.GASTO,
            new BigDecimal(valor), categoria, null, data, "");
   }
}
