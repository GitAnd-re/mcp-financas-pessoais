package br.edu.ifpr.mcp_financas_pessoais.mcp.prompts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_financas_pessoais.main.AppConsts;
import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ListPromptsResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

class AnalyzeExpensesPromptIT {
   private static final Logger LOG = LoggerFactory.getLogger(
         AnalyzeExpensesPromptIT.class);
   private static final String SERVER_PATH =
         "server/mcp_financas_pessoais_server.jar";

   @TempDir static Path tempDir;
   private static McpSyncClient client;

   @BeforeAll
   static void iniciarServidorECliente() throws Exception {
      File serverJar = new File(SERVER_PATH);
      assertTrue(serverJar.isFile(), "JAR do servidor não encontrado: " +
            serverJar.getAbsolutePath());

      ServerParameters parameters = ServerParameters.builder("java")
            .args("-jar", serverJar.getAbsolutePath())
            .addEnvVar(AppConsts.AUTH_DIR_VAR, tempDir.toString())
            .build();
      StdioClientTransport transport = new StdioClientTransport(parameters,
            new JacksonMcpJsonMapper(new JsonMapper()));
      transport.setStdErrorHandler(line -> LOG.info("[SERVER STDERR] " + line));
      client = McpClient.sync(transport).build();
      client.initialize();
   }

   @AfterAll
   static void encerrarCliente() throws Exception {
      if (client != null)
         client.closeGracefully();
   }

   @Test
   @DisplayName("O prompt de análise de gastos é anunciado pelo servidor")
   void anunciaPromptEArgumentos() {
      ListPromptsResult result = client.listPrompts();
      assertNotNull(result);

      Prompt prompt = result.prompts().stream()
            .filter(item -> McpConsts.ANALYZE_EXPENSES_PROMPT.equals(item.name()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Prompt não registrado."));

      assertEquals(3, prompt.arguments().size());
      assertTrue(prompt.arguments().stream().anyMatch(argument ->
            McpConsts.ANALYZE_EXPENSES_ATTR_ANO.equals(argument.name()) &&
                  Boolean.TRUE.equals(argument.required())));
      assertTrue(prompt.arguments().stream().anyMatch(argument ->
            McpConsts.ANALYZE_EXPENSES_ATTR_MES.equals(argument.name()) &&
                  !Boolean.TRUE.equals(argument.required())));
      assertTrue(prompt.arguments().stream().anyMatch(argument ->
            McpConsts.ANALYZE_EXPENSES_ATTR_CONTEXTO.equals(argument.name()) &&
                  !Boolean.TRUE.equals(argument.required())));
   }

   @Test
   @DisplayName("Gerar instruções de análise para um mês")
   void geraInstrucoesMensais() {
      GetPromptResult result = client.getPrompt(GetPromptRequest.builder(
            McpConsts.ANALYZE_EXPENSES_PROMPT)
            .arguments(Map.of(McpConsts.ANALYZE_EXPENSES_ATTR_ANO, "2026",
                  McpConsts.ANALYZE_EXPENSES_ATTR_MES, "3",
                  McpConsts.ANALYZE_EXPENSES_ATTR_CONTEXTO,
                  "Identificar gastos recorrentes"))
            .build());

      String text = textoDoPrompt(result);
      assertTrue(text.contains("Mês: 03/2026"));
      assertTrue(text.contains("Identificar gastos recorrentes"));
      assertTrue(text.contains(McpConsts.RFT_TOOL_NAME));
      assertTrue(text.contains(McpConsts.LLT_TOOL_NAME));
      assertTrue(text.contains("Não invente valores"));
   }

   @Test
   @DisplayName("Gerar instruções anuais sem mês nem contexto")
   void geraInstrucoesAnuaisSemMesNemContexto() {
      GetPromptResult result = client.getPrompt(GetPromptRequest.builder(
            McpConsts.ANALYZE_EXPENSES_PROMPT)
            .arguments(Map.of(McpConsts.ANALYZE_EXPENSES_ATTR_ANO, "2025"))
            .build());

      String text = textoDoPrompt(result);
      assertTrue(text.contains("Ano: 2025"));
      assertTrue(text.contains("sem mês"));
      assertTrue(text.contains("Nenhum foco adicional informado."));
      assertFalse(text.contains("Mês: "));
   }

   private static String textoDoPrompt(GetPromptResult result) {
      assertNotNull(result);
      assertEquals(1, result.messages().size());
      PromptMessage message = result.messages().get(0);
      TextContent content = assertInstanceOf(TextContent.class,
            message.content());
      assertNotNull(content.text());
      assertFalse(content.text().isBlank());
      return content.text();
   }
}
