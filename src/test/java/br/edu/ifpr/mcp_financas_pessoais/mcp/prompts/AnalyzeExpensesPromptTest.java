package br.edu.ifpr.mcp_financas_pessoais.mcp.prompts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

class AnalyzeExpensesPromptTest {
   private final AnalyzeExpensesPrompt prompt = new AnalyzeExpensesPrompt();

   @Test
   @DisplayName("Rejeitar solicitação sem argumentos")
   void rejeitarArgumentosAusentes() {
      GetPromptRequest request = GetPromptRequest.builder(
            McpConsts.ANALYZE_EXPENSES_PROMPT).build();
      IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class, () -> prompt.run(null, request));
      assertEquals("Nenhum argumento informado.", error.getMessage());

      assertValidationError(Map.of(), "Nenhum argumento informado.");
   }

   @Test
   @DisplayName("Rejeitar ano ausente ou vazio")
   void rejeitarAnoAusenteOuVazio() {
      assertValidationError(Map.of(McpConsts.ANALYZE_EXPENSES_ATTR_MES, "3"),
            "O argumento 'ano' é obrigatório.");
      assertValidationError(Map.of(McpConsts.ANALYZE_EXPENSES_ATTR_ANO, " "),
            "O argumento 'ano' é obrigatório.");
   }

   @Test
   @DisplayName("Rejeitar ano com tipo ou formato inválido")
   void rejeitarAnoComTipoOuFormatoInvalido() {
      assertValidationError(Map.of(McpConsts.ANALYZE_EXPENSES_ATTR_ANO, 2026),
            "O argumento 'ano' deve ser um número inteiro.");
      assertValidationError(Map.of(McpConsts.ANALYZE_EXPENSES_ATTR_ANO,
            "dois mil"), "O argumento 'ano' deve ser um número inteiro.");
   }

   @Test
   @DisplayName("Rejeitar ano menor que um")
   void rejeitarAnoNaoPositivo() {
      assertValidationError(Map.of(McpConsts.ANALYZE_EXPENSES_ATTR_ANO, "0"),
            "O argumento 'ano' deve ser maior que zero.");
      assertValidationError(Map.of(McpConsts.ANALYZE_EXPENSES_ATTR_ANO, "-1"),
            "O argumento 'ano' deve ser maior que zero.");
   }

   @Test
   @DisplayName("Rejeitar mês com tipo, formato ou faixa inválidos")
   void rejeitarMesInvalido() {
      assertValidationError(Map.of(McpConsts.ANALYZE_EXPENSES_ATTR_ANO, "2026",
            McpConsts.ANALYZE_EXPENSES_ATTR_MES, 3),
            "O argumento 'mes' deve ser um número inteiro de 1 a 12.");
      assertValidationError(Map.of(McpConsts.ANALYZE_EXPENSES_ATTR_ANO, "2026",
            McpConsts.ANALYZE_EXPENSES_ATTR_MES, "março"),
            "O argumento 'mes' deve ser um número inteiro de 1 a 12.");
      assertValidationError(Map.of(McpConsts.ANALYZE_EXPENSES_ATTR_ANO, "2026",
            McpConsts.ANALYZE_EXPENSES_ATTR_MES, "0"),
            "O argumento 'mes' deve estar entre 1 e 12.");
      assertValidationError(Map.of(McpConsts.ANALYZE_EXPENSES_ATTR_ANO, "2026",
            McpConsts.ANALYZE_EXPENSES_ATTR_MES, "13"),
            "O argumento 'mes' deve estar entre 1 e 12.");
   }

   @ParameterizedTest
   @ValueSource(strings = { "1", "12" })
   @DisplayName("Aceitar meses nos limites válidos")
   void aceitarMesesLimite(String mes) {
      String texto = texto(Map.of(McpConsts.ANALYZE_EXPENSES_ATTR_ANO, "2026",
            McpConsts.ANALYZE_EXPENSES_ATTR_MES, mes));
      assertTrue(texto.contains("Mês: " + ("1".equals(mes) ? "01" : "12") +
            "/2026"));
   }

   @Test
   @DisplayName("Rejeitar contexto que não seja texto")
   void rejeitarContextoDeTipoInvalido() {
      assertValidationError(Map.of(McpConsts.ANALYZE_EXPENSES_ATTR_ANO, "2026",
            McpConsts.ANALYZE_EXPENSES_ATTR_CONTEXTO, false),
            "O argumento 'contexto' deve ser texto.");
   }

   @Test
   @DisplayName("Gerar instruções mensais com contexto")
   void gerarInstrucoesMensais() {
      String texto = texto(Map.of(
            McpConsts.ANALYZE_EXPENSES_ATTR_ANO, "2026",
            McpConsts.ANALYZE_EXPENSES_ATTR_MES, "3",
            McpConsts.ANALYZE_EXPENSES_ATTR_CONTEXTO,
            "Identificar gastos recorrentes"));

      assertTrue(texto.contains("Mês: 03/2026"));
      assertTrue(texto.contains("Identificar gastos recorrentes"));
      assertTrue(texto.contains(McpConsts.RFT_TOOL_NAME));
      assertTrue(texto.contains(McpConsts.LLT_TOOL_NAME));
      assertTrue(texto.contains("Não invente valores"));
      assertFalse(texto.contains("sem mês"));
   }

   @Test
   @DisplayName("Gerar instruções anuais sem mês nem contexto")
   void gerarInstrucoesAnuaisSemMesNemContexto() {
      String texto = texto(Map.of(McpConsts.ANALYZE_EXPENSES_ATTR_ANO, "2025"));

      assertTrue(texto.contains("Ano: 2025"));
      assertTrue(texto.contains("sem mês"));
      assertTrue(texto.contains("Nenhum foco adicional informado."));
      assertFalse(texto.contains("Mês: "));
   }

   @Test
   @DisplayName("Tratar filtros opcionais em branco como omitidos")
   void aceitarArgumentosOpcionaisEmBranco() {
      String texto = texto(Map.of(
            McpConsts.ANALYZE_EXPENSES_ATTR_ANO, "2026",
            McpConsts.ANALYZE_EXPENSES_ATTR_MES, " ",
            McpConsts.ANALYZE_EXPENSES_ATTR_CONTEXTO, " "));

      assertTrue(texto.contains("Ano: 2026"));
      assertTrue(texto.contains("Nenhum foco adicional informado."));
      assertFalse(texto.contains("Mês: "));
   }

   @Test
   @DisplayName("Retornar um prompt com descrição e mensagem de usuário")
   void retornarEstruturaValida() {
      GetPromptResult result = resultado(Map.of(
            McpConsts.ANALYZE_EXPENSES_ATTR_ANO, "2026"));

      assertNotNull(result.description());
      assertEquals(1, result.messages().size());
      PromptMessage message = result.messages().get(0);
      assertEquals("user", message.role().name().toLowerCase());
      assertInstanceOf(TextContent.class, message.content());
   }

   private void assertValidationError(Map<String, Object> arguments,
                                      String expectedMessage) {
      GetPromptRequest request = GetPromptRequest.builder(
            McpConsts.ANALYZE_EXPENSES_PROMPT)
            .arguments(arguments)
            .build();
      IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class, () -> prompt.run(null, request));
      assertEquals(expectedMessage, error.getMessage());
   }

   private String texto(Map<String, Object> arguments) {
      GetPromptResult result = resultado(arguments);
      assertEquals(1, result.messages().size());
      TextContent content = assertInstanceOf(TextContent.class,
            result.messages().get(0).content());
      assertNotNull(content.text());
      assertFalse(content.text().isBlank());
      return content.text();
   }

   private GetPromptResult resultado(Map<String, Object> arguments) {
      GetPromptRequest request = GetPromptRequest.builder(
            McpConsts.ANALYZE_EXPENSES_PROMPT)
            .arguments(arguments)
            .build();
      return prompt.run(null, request);
   }
}
