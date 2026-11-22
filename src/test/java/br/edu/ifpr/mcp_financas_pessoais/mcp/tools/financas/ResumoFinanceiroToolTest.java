package br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryLancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

class ResumoFinanceiroToolTest {
   private InMemoryLancamentoRepositorio repositorio;
   private ResumoFinanceiroTool tool;

   @BeforeEach
   void setUp() {
      repositorio = new InMemoryLancamentoRepositorio();
      tool = new ResumoFinanceiroTool(repositorio);
   }

   @Test
   @DisplayName("Retornar resumo textual com totais e categorias")
   void calculaResumoComSucesso() {
      repositorio.salvar(lancamento("g1", Lancamento.TipoLancamento.GASTO,
            "Mercado", "10.25", LocalDate.of(2026, 11, 1)));
      repositorio.salvar(lancamento("g2", Lancamento.TipoLancamento.GASTO,
            "Mercado", "5.75", LocalDate.of(2026, 11, 2)));
      repositorio.salvar(lancamento("r1", Lancamento.TipoLancamento.RECEITA,
            "Salário", "100.00", LocalDate.of(2026, 11, 3)));

      CallToolResult result = executar(Map.of("mes", 11, "ano", 2026));

      assertFalse(result.isError());
      String texto = texto(result);
      assertTrue(texto.contains("\n"));
      assertTrue(texto.startsWith("Resumo financeiro de 11/2026:"));
      assertTrue(texto.contains("total_receitas:100.00"));
      assertTrue(texto.contains("total_gastos:16.00"));
      assertTrue(texto.contains("saldo:84.00"));
      assertTrue(texto.contains("Mercado:16.00"));
   }

   @Test
   @DisplayName("Resumir o ano quando mês não for informado")
   void resumeAnoQuandoMesOmitido() {
      repositorio.salvar(lancamento("outubro", Lancamento.TipoLancamento.GASTO,
            "Mercado", "10.00", LocalDate.of(2026, 10, 20)));
      repositorio.salvar(lancamento("novembro", Lancamento.TipoLancamento.GASTO,
            "Lazer", "20.00", LocalDate.of(2026, 11, 20)));
      repositorio.salvar(lancamento("outro-ano", Lancamento.TipoLancamento.GASTO,
            "Mercado", "999.00", LocalDate.of(2025, 11, 20)));

      CallToolResult result = executar(Map.of("ano", 2026));

      assertFalse(result.isError());
      assertTrue(texto(result).startsWith("Resumo financeiro de 2026:"));
      assertTrue(texto(result).contains("total_gastos:30.00"));
      assertTrue(texto(result).contains("Mercado:10.00"));
      assertTrue(texto(result).contains("Lazer:20.00"));
   }

   @Test
   @DisplayName("Indicar quando não houver gastos no período")
   void resumoSemGastos() {
      CallToolResult result = executar(Map.of("mes", 1, "ano", 2040));

      assertFalse(result.isError());
      assertTrue(texto(result).contains("Nenhum gasto no período."));
   }

   @Test
   @DisplayName("Retornar erro para mês inválido")
   void rejeitaMesInvalido() {
      CallToolResult result = executar(Map.of("mes", 13, "ano", 2026));
      assertTrue(result.isError());
      assertTrue(texto(result).equals("O mês deve estar entre 1 e 12."));
   }

   @Test
   @DisplayName("Rejeitar campos ausentes, não inteiros ou fracionários")
   void rejeitaArgumentosInvalidos() {
      CallToolResult anoAusente = executar(Map.of("mes", 11));
      CallToolResult mesTexto = executar(Map.of("mes", "11", "ano", 2026));
      CallToolResult anoFracionario = executar(Map.of("mes", 11, "ano", 2026.5));

      assertFalse(executar(Map.of("ano", 2026)).isError());
      assertTrue(anoAusente.isError());
      assertTrue(texto(anoAusente).contains("ano"));
      assertTrue(mesTexto.isError());
      assertTrue(texto(mesTexto).contains("mês"));
      assertTrue(anoFracionario.isError());
      assertTrue(texto(anoFracionario).contains("ano"));
   }

   @Test
   @DisplayName("Retornar erro quando o repositório falha")
   void retornaErroDeLeitura() {
      LancamentoRepositorio comFalha = new LancamentoRepositorio() {
         @Override public void salvar(Lancamento lancamento) { }
         @Override public List<Lancamento> listar() throws IOException {
            throw new IOException("disco indisponível");
         }
      };
      CallToolResult result = new ResumoFinanceiroTool(comFalha).run(null,
            request(Map.of("mes", 11, "ano", 2026)));

      assertTrue(result.isError());
      assertTrue(texto(result).contains(
            "Não foi possível gerar o resumo financeiro: disco indisponível"));
   }

   private CallToolResult executar(Map<String, Object> args) {
      return tool.run(null, request(args));
   }

   private static CallToolRequest request(Map<String, Object> args) {
      return CallToolRequest.builder(McpConsts.RFT_TOOL_NAME).arguments(args).build();
   }

   private static Lancamento lancamento(String id, Lancamento.TipoLancamento tipo,
                                        String categoria, String valor, LocalDate data) {
      return new Lancamento(id, tipo, new BigDecimal(valor),
            tipo == Lancamento.TipoLancamento.GASTO ? categoria : null,
            tipo == Lancamento.TipoLancamento.RECEITA ? categoria : null,
            data, "descrição");
   }

   private static String texto(CallToolResult result) {
      return ((TextContent) result.content().get(0)).text();
   }
}
