package br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigDecimal;
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
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

class VerificarOrcamentoToolIT {
   private static final Logger LOG = LoggerFactory.getLogger(VerificarOrcamentoToolIT.class);
   private static final String SERVER_PATH = "server/mcp_financas_pessoais_server.jar";

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
   @DisplayName("Executar verificar_orcamento pelo servidor MCP stdio")
   void verificaOrcamentoPeloServidor() {
      ListToolsResult tools = client.listTools();
      assertTrue(tools.tools().stream().anyMatch(tool ->
            McpConsts.VOT_TOOL_NAME.equals(tool.name())));

      CallToolResult definir = client.callTool(CallToolRequest.builder(
            McpConsts.DOT_TOOL_NAME)
            .arguments(Map.of(McpConsts.DOT_ATTR_CATEGORIA, "Mercado",
                  McpConsts.DOT_ATTR_MES, 11,
                  McpConsts.DOT_ATTR_ANO, 2026,
                  McpConsts.DOT_ATTR_LIMITE_MENSAL, new BigDecimal("100.00")))
            .build());
      assertFalse(definir.isError());
      CallToolResult gasto = client.callTool(CallToolRequest.builder(
            McpConsts.RGT_TOOL_NAME)
            .arguments(Map.of(McpConsts.RGT_ATTR_VALOR, new BigDecimal("125.50"),
                  McpConsts.RGT_ATTR_CATEGORIA, "Mercado",
                  McpConsts.RGT_ATTR_DATA, "10/11/2026"))
            .build());
      assertFalse(gasto.isError());
      CallToolResult result = client.callTool(CallToolRequest.builder(
            McpConsts.VOT_TOOL_NAME)
            .arguments(Map.of(McpConsts.VOT_ATTR_MES, 11,
                  McpConsts.VOT_ATTR_ANO, 2026))
            .build());

      assertFalse(result.isError());
      String response = ((TextContent) result.content().get(0)).text();
      assertTrue(response.startsWith("Orçamentos de 11/2026:"));
      assertTrue(response.contains(
            "Mercado: gasto:125.50, limite:100.00, restante:-25.50, estourou:true"));
   }

   @Test
   @DisplayName("Retornar erro MCP para mês fora do schema")
   void retornaErroParaMesForaDoSchema() {
      CallToolResult result = client.callTool(CallToolRequest.builder(
            McpConsts.VOT_TOOL_NAME)
            .arguments(Map.of(McpConsts.VOT_ATTR_MES, 13,
                  McpConsts.VOT_ATTR_ANO, 2026))
            .build());
      assertTrue(result.isError());
      assertTrue(((TextContent) result.content().get(0)).text()
            .contains("deve ter um valor máximo de 12"));
   }

   @Test
   @DisplayName("Retornar erro quando só houver orçamento de outro período")
   void retornaErroQuandoNaoHaOrcamentoNoPeriodo() {
      CallToolResult definir = client.callTool(CallToolRequest.builder(
            McpConsts.DOT_TOOL_NAME)
            .arguments(Map.of(McpConsts.DOT_ATTR_CATEGORIA, "Viagem",
                  McpConsts.DOT_ATTR_MES, 11,
                  McpConsts.DOT_ATTR_ANO, 2026,
                  McpConsts.DOT_ATTR_LIMITE_MENSAL, new BigDecimal("300.00")))
            .build());
      assertFalse(definir.isError());

      CallToolResult result = client.callTool(CallToolRequest.builder(
            McpConsts.VOT_TOOL_NAME)
            .arguments(Map.of(McpConsts.VOT_ATTR_MES, 12,
                  McpConsts.VOT_ATTR_ANO, 2026))
            .build());

      assertTrue(result.isError());
      assertTrue(((TextContent) result.content().get(0)).text()
            .contains("Nenhum orçamento foi definido para 12/2026."));
   }
}
