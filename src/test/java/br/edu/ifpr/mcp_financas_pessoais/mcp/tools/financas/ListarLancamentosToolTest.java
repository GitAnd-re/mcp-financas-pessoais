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

class ListarLancamentosToolTest {
   private InMemoryLancamentoRepositorio repositorio;
   private ListarLancamentosTool tool;

   @BeforeEach
   void setUp() {
      repositorio = new InMemoryLancamentoRepositorio();
      tool = new ListarLancamentosTool(repositorio);
   }

   @Test
   @DisplayName("Retornar lançamentos no formato de texto")
   void listaLancamentosComSucesso() {
      repositorio.salvar(lancamento("id-gasto", Lancamento.TipoLancamento.GASTO,
            "Mercado", null, LocalDate.of(2026, 11, 20)));
      repositorio.salvar(lancamento("id-receita", Lancamento.TipoLancamento.RECEITA,
            null, "Salário", LocalDate.of(2026, 11, 21)));

      CallToolResult result = executar(Map.of("mes", 11, "ano", 2026));

      assertFalse(result.isError());
      String text = texto(result);
      assertTrue(text.startsWith("Lançamentos de 11/2026:"));
      assertTrue(text.contains("tipo:GASTO, valor:10.00"));
      assertTrue(text.contains("categoria:Mercado, fonte:null"));
      assertTrue(text.contains("tipo:RECEITA, valor:20.50"));
      assertTrue(text.contains("categoria:null, fonte:Salário"));
      assertTrue(text.contains("data:21/11/2026"));
      assertFalse(text.contains("id:"));
   }

   @Test
   @DisplayName("Listar o ano inteiro quando o mês não for informado")
   void listaAnoInteiroQuandoMesOmitido() {
      repositorio.salvar(lancamento("novembro", Lancamento.TipoLancamento.GASTO,
            "Mercado", null, LocalDate.of(2026, 11, 20)));
      repositorio.salvar(lancamento("outubro", Lancamento.TipoLancamento.GASTO,
            "Mercado", null, LocalDate.of(2026, 10, 21)));
      repositorio.salvar(lancamento("outro-ano", Lancamento.TipoLancamento.GASTO,
            "Mercado", null, LocalDate.of(2025, 10, 21)));

      CallToolResult result = executar(Map.of("ano", 2026));

      assertFalse(result.isError());
      String text = texto(result);
      assertTrue(text.startsWith("Lançamentos de 2026:"));
      assertTrue(text.contains("data:20/11/2026"));
      assertTrue(text.contains("data:21/10/2026"));
      assertFalse(text.contains("data:20/10/2025"));
   }

   @Test
   @DisplayName("Aplicar filtro opcional de categoria")
   void filtraPorCategoria() {
      repositorio.salvar(lancamento("mercado", Lancamento.TipoLancamento.GASTO,
            "Mercado", null, LocalDate.of(2026, 11, 20)));
      repositorio.salvar(lancamento("lazer", Lancamento.TipoLancamento.GASTO,
            "Lazer", null, LocalDate.of(2026, 11, 21)));

      CallToolResult result = executar(Map.of("mes", 11, "ano", 2026,
            "categoria", "Mercado"));

      assertFalse(result.isError());
      assertTrue(texto(result).contains("categoria:Mercado"));
      assertFalse(texto(result).contains("categoria:Lazer"));
   }

   @Test
   @DisplayName("Informar quando não houver lançamentos")
   void listaSemResultados() {
      CallToolResult result = executar(Map.of("ano", 2026));

      assertFalse(result.isError());
      assertTrue(texto(result).equals("Nenhum lançamento encontrado para 2026."));
   }

   @Test
   @DisplayName("Retornar erro para mês inválido")
   void retornaErroParaMesInvalido() {
      CallToolResult result = executar(Map.of("mes", 13, "ano", 2026));

      assertTrue(result.isError());
      assertTrue(texto(result).equals("O mês deve estar entre 1 e 12."));
   }

   @Test
   @DisplayName("Rejeitar mês informado com tipo inválido")
   void rejeitaMesComTipoInvalido() {
      CallToolResult textoComoMes = executar(Map.of("mes", "11", "ano", 2026));
      CallToolResult fracionario = executar(Map.of("mes", 11.5, "ano", 2026));

      assertTrue(textoComoMes.isError());
      assertTrue(texto(textoComoMes).contains("mês"));
      assertTrue(fracionario.isError());
      assertTrue(texto(fracionario).contains("mês"));
   }

   @Test
   @DisplayName("Rejeitar ano ausente ou não inteiro")
   void rejeitaAnoAusenteOuNaoInteiro() {
      CallToolResult ausente = executar(Map.of("mes", 11));
      CallToolResult textoComoAno = executar(Map.of("mes", 11, "ano", "2026"));

      assertTrue(ausente.isError());
      assertTrue(texto(ausente).contains("ano"));
      assertTrue(textoComoAno.isError());
      assertTrue(texto(textoComoAno).contains("ano"));
   }

   @Test
   @DisplayName("Rejeitar categoria que não seja texto")
   void rejeitaCategoriaComTipoInvalido() {
      CallToolResult result = executar(Map.of("mes", 11, "ano", 2026,
            "categoria", 12));

      assertTrue(result.isError());
      assertTrue(texto(result).equals("O campo categoria deve ser texto."));
   }

   @Test
   @DisplayName("Retornar erro quando o repositório falha ao listar")
   void retornaErroQuandoRepositorioFalha() {
      LancamentoRepositorio comFalha = new LancamentoRepositorio() {
         @Override
         public void salvar(Lancamento lancamento) { }

         @Override
         public List<Lancamento> listar() throws IOException {
            throw new IOException("disco indisponível");
         }
      };
      CallToolResult result = new ListarLancamentosTool(comFalha).run(null,
            request(Map.of("mes", 11, "ano", 2026)));

      assertTrue(result.isError());
      assertTrue(texto(result).contains("Não foi possível listar os lançamentos: disco indisponível"));
   }

   private CallToolResult executar(Map<String, Object> args) {
      return tool.run(null, request(args));
   }

   private static CallToolRequest request(Map<String, Object> args) {
      return CallToolRequest.builder(McpConsts.LLT_TOOL_NAME)
            .arguments(args)
            .build();
   }

   private static Lancamento lancamento(String id,
                                        Lancamento.TipoLancamento tipo,
                                        String categoria,
                                        String fonte,
                                        LocalDate data) {
      return new Lancamento(id, tipo, new BigDecimal(tipo == Lancamento.TipoLancamento.GASTO
            ? "10.00" : "20.50"), categoria, fonte, data, "descrição");
   }

   private static String texto(CallToolResult result) {
      return ((TextContent) result.content().get(0)).text();
   }
}
