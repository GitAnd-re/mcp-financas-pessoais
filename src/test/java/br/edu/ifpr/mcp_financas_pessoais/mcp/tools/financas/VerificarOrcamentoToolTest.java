package br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Orcamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryLancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryOrcamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.OrcamentoRepositorio;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

class VerificarOrcamentoToolTest {
   private InMemoryLancamentoRepositorio lancamentos;
   private InMemoryOrcamentoRepositorio orcamentos;
   private VerificarOrcamentoTool tool;

   @BeforeEach
   void setUp() {
      lancamentos = new InMemoryLancamentoRepositorio();
      orcamentos = new InMemoryOrcamentoRepositorio();
      tool = new VerificarOrcamentoTool(lancamentos, orcamentos);
   }

   @Test
   @DisplayName("Retornar gasto, limite, restante e estado de cada categoria")
   void verificaOrcamento() {
      orcamentos.salvar(new Orcamento("Mercado", new BigDecimal("100.00")));
      lancamentos.salvar(new Lancamento("1", Lancamento.TipoLancamento.GASTO,
            new BigDecimal("125.50"), "Mercado", null,
            java.time.LocalDate.of(2026, 11, 10), ""));

      CallToolResult result = executar(argumentos(11, 2026));
      assertFalse(result.isError());
      assertEquals("Orçamentos de 11/2026:\n" +
            "Mercado: gasto:125.50, limite:100.00, restante:-25.50, estourou:true",
            texto(result));
   }

   @Test
   @DisplayName("Retornar erro para mês fora do intervalo")
   void retornaErroParaMesInvalido() {
      assertErro(argumentos(0, 2026), "O mês deve estar entre 1 e 12.");
      assertErro(argumentos(13, 2026), "O mês deve estar entre 1 e 12.");
   }

   @Test
   @DisplayName("Retornar erro para ano menor que um")
   void retornaErroParaAnoInvalido() {
      assertErro(argumentos(11, 0), "O ano deve ser maior que zero.");
   }

   @Test
   @DisplayName("Rejeitar mês ou ano ausente")
   void rejeitaPeriodoAusente() {
      assertErro(argumentos(AUSENTE, 2026), "mês é obrigatório");
      assertErro(argumentos(11, AUSENTE), "ano é obrigatório");
   }

   @Test
   @DisplayName("Rejeitar mês ou ano com tipo inválido")
   void rejeitaPeriodoComTipoInvalido() {
      assertErro(argumentos("11", 2026), "mês deve ser informado como número inteiro");
      assertErro(argumentos(11, "2026"), "ano deve ser informado como número inteiro");
      assertErro(argumentos(11.5, 2026), "mês deve ser informado como número inteiro");
   }

   @Test
   @DisplayName("Retornar erro quando não houver orçamento")
   void retornaErroQuandoNaoHaOrcamento() {
      assertErro(argumentos(11, 2026), "Nenhum orçamento foi definido.");
   }

   @Test
   @DisplayName("Retornar erro quando o repositório falhar")
   void retornaErroQuandoRepositorioFalha() {
      OrcamentoRepositorio comFalha = new OrcamentoRepositorio() {
         @Override
         public void salvar(Orcamento orcamento) { }

         @Override
         public List<Orcamento> listar() throws IOException {
            throw new IOException("disco indisponível");
         }
      };
      VerificarOrcamentoTool toolComFalha = new VerificarOrcamentoTool(
            lancamentos, comFalha);
      CallToolResult result = toolComFalha.run(null,
            request(argumentos(11, 2026)));
      assertTrue(result.isError());
      assertTrue(texto(result).contains("Não foi possível verificar os orçamentos"));
      assertTrue(texto(result).contains("disco indisponível"));
   }

   private void assertErro(Map<String, Object> args, String mensagem) {
      CallToolResult result = executar(args);
      assertTrue(result.isError());
      assertTrue(texto(result).contains(mensagem), texto(result));
   }

   private CallToolResult executar(Map<String, Object> args) {
      return tool.run(null, request(args));
   }

   private static CallToolRequest request(Map<String, Object> args) {
      return CallToolRequest.builder(McpConsts.VOT_TOOL_NAME).arguments(args).build();
   }

   private static String texto(CallToolResult result) {
      return ((TextContent) result.content().get(0)).text();
   }

   private static Map<String, Object> argumentos(Object mes, Object ano) {
      Map<String, Object> args = new HashMap<>();
      if (mes != AUSENTE)
         args.put(McpConsts.VOT_ATTR_MES, mes);
      if (ano != AUSENTE)
         args.put(McpConsts.VOT_ATTR_ANO, ano);
      return args;
   }

   private static final Object AUSENTE = new Object();
}
