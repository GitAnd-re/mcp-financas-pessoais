package br.edu.ifpr.mcp_financas_pessoais.mcp.prompts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

class AnalyzeTransactionsPromptTest {
   private final AnalyzeTransactionsPrompt prompt =
         new AnalyzeTransactionsPrompt();

   @Test
   @DisplayName("Rejeitar uma solicitação sem argumentos")
   void rejeitarArgumentosAusentes() {
      assertValidationError(Map.of(), "Nenhum argumento informado.");
   }

   @Test
   @DisplayName("Rejeitar ano ausente ou vazio")
   void rejeitarAnoAusenteOuVazio() {
      assertValidationError(Map.of(McpConsts.ANALYZE_TRANSACTIONS_ATTR_MES,
            "3"), "O argumento 'ano' é obrigatório.");
      assertValidationError(Map.of(McpConsts.ANALYZE_TRANSACTIONS_ATTR_ANO,
            " "), "O argumento 'ano' é obrigatório.");
   }

   @Test
   @DisplayName("Rejeitar ano com tipo, formato ou valor inválido")
   void rejeitarAnoInvalido() {
      assertValidationError(Map.of(McpConsts.ANALYZE_TRANSACTIONS_ATTR_ANO,
            2026), "O argumento 'ano' deve ser um número inteiro.");
      assertValidationError(Map.of(McpConsts.ANALYZE_TRANSACTIONS_ATTR_ANO,
            "dois mil"), "O argumento 'ano' deve ser um número inteiro.");
      assertValidationError(Map.of(McpConsts.ANALYZE_TRANSACTIONS_ATTR_ANO,
            "0"), "O argumento 'ano' deve ser maior que zero.");
   }

   @Test
   @DisplayName("Rejeitar mês com tipo, formato ou faixa inválidos")
   void rejeitarMesInvalido() {
      assertValidationError(Map.of(
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_ANO, "2026",
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_MES, 3),
            "O argumento 'mes' deve ser um número inteiro de 1 a 12.");
      assertValidationError(Map.of(
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_ANO, "2026",
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_MES, "março"),
            "O argumento 'mes' deve ser um número inteiro de 1 a 12.");
      assertValidationError(Map.of(
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_ANO, "2026",
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_MES, "13"),
            "O argumento 'mes' deve estar entre 1 e 12.");
      assertValidationError(Map.of(
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_ANO, "2026",
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_MES, "0"),
            "O argumento 'mes' deve estar entre 1 e 12.");
   }

   @Test
   @DisplayName("Rejeitar categoria que não seja texto")
   void rejeitarCategoriaDeTipoInvalido() {
      assertValidationError(Map.of(
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_ANO, "2026",
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_CATEGORIA, 7),
            "O argumento 'categoria' deve ser texto.");
   }

   @Test
   @DisplayName("Rejeitar contexto que não seja texto")
   void rejeitarContextoDeTipoInvalido() {
      assertValidationError(Map.of(
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_ANO, "2026",
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_CONTEXTO, false),
            "O argumento 'contexto' deve ser texto.");
   }

   @Test
   @DisplayName("Tratar filtros opcionais em branco como omitidos")
   void aceitarFiltrosOpcionaisEmBranco() {
      String text = texto(Map.of(
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_ANO, "2026",
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_MES, " ",
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_CATEGORIA, " ",
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_CONTEXTO, " "));

      assertTrue(text.contains("Ano: 2026"));
      assertTrue(text.contains("Nenhum foco adicional informado."));
      assertFalse(text.contains("Mês: "));
      assertFalse(text.contains("Categoria: "));
   }

   private void assertValidationError(Map<String, Object> arguments,
                                      String expectedMessage) {
      GetPromptRequest request = GetPromptRequest.builder(
            McpConsts.ANALYZE_TRANSACTIONS_PROMPT)
            .arguments(arguments)
            .build();
      IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class, () -> prompt.run(null, request));
      assertEquals(expectedMessage, error.getMessage());
   }

   private String texto(Map<String, Object> arguments) {
      GetPromptRequest request = GetPromptRequest.builder(
            McpConsts.ANALYZE_TRANSACTIONS_PROMPT)
            .arguments(arguments)
            .build();
      GetPromptResult result = prompt.run(null, request);
      return ((TextContent) result.messages().get(0).content()).text();
   }
}
