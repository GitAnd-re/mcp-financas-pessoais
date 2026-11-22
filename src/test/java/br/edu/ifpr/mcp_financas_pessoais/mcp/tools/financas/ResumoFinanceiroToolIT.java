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
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

class ResumoFinanceiroToolIT {
   private static final Logger LOG = LoggerFactory.getLogger(ResumoFinanceiroToolIT.class);
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
   @DisplayName("Executar resumo financeiro mensal e anual pelo servidor MCP")
   void calculaResumoPeloServidor() {
      CallToolResult gasto1 = client.callTool(CallToolRequest.builder(
            McpConsts.RGT_TOOL_NAME)
            .arguments(Map.of(McpConsts.RGT_ATTR_VALOR, new BigDecimal("30.00"),
                  McpConsts.RGT_ATTR_CATEGORIA, "Curso",
                  McpConsts.RGT_ATTR_DATA, "20/04/2040"))
            .build());
      CallToolResult gasto2 = client.callTool(CallToolRequest.builder(
            McpConsts.RGT_TOOL_NAME)
            .arguments(Map.of(McpConsts.RGT_ATTR_VALOR, new BigDecimal("8.00"),
                  McpConsts.RGT_ATTR_CATEGORIA, "Mercado",
                  McpConsts.RGT_ATTR_DATA, "21/04/2040"))
            .build());
      CallToolResult gasto3 = client.callTool(CallToolRequest.builder(
            McpConsts.RGT_TOOL_NAME)
            .arguments(Map.of(McpConsts.RGT_ATTR_VALOR, new BigDecimal("5.00"),
                  McpConsts.RGT_ATTR_CATEGORIA, "Mercado",
                  McpConsts.RGT_ATTR_DATA, "21/05/2040"))
            .build());
      CallToolResult receita = client.callTool(CallToolRequest.builder(
            McpConsts.RRT_TOOL_NAME)
            .arguments(Map.of(McpConsts.RRT_ATTR_VALOR, new BigDecimal("50.00"),
                  McpConsts.RRT_ATTR_FONTE, "Bolsa",
                  McpConsts.RRT_ATTR_DATA, "22/04/2040"))
            .build());
      assertFalse(gasto1.isError());
      assertFalse(gasto2.isError());
      assertFalse(gasto3.isError());
      assertFalse(receita.isError());

      CallToolResult result = client.callTool(CallToolRequest.builder(
            McpConsts.RFT_TOOL_NAME)
            .arguments(Map.of(McpConsts.RFT_ATTR_MES, 4,
                  McpConsts.RFT_ATTR_ANO, 2040))
            .build());

      assertFalse(result.isError());
      String response = ((TextContent) result.content().get(0)).text();
      assertTrue(response.contains("\n"));
      assertTrue(response.startsWith("Resumo financeiro de 04/2040:"));
      assertTrue(response.contains("total_receitas:50.00"), response);
      assertTrue(response.contains("total_gastos:38.00"), response);
      assertTrue(response.contains("saldo:12.00"), response);
      assertTrue(response.contains("Curso:30.00"), response);
      assertTrue(response.contains("Mercado:8.00"), response);

      CallToolResult anual = client.callTool(CallToolRequest.builder(
            McpConsts.RFT_TOOL_NAME)
            .arguments(Map.of(McpConsts.RFT_ATTR_ANO, 2040))
            .build());

      assertFalse(anual.isError());
      String respostaAnual = ((TextContent) anual.content().get(0)).text();
      assertTrue(respostaAnual.startsWith("Resumo financeiro de 2040:"));
      assertTrue(respostaAnual.contains("total_gastos:43.00"));
      assertTrue(respostaAnual.contains("saldo:7.00"));
      assertTrue(respostaAnual.contains("Mercado:13.00"));
   }

   @Test
   @DisplayName("Retornar erro MCP quando o mês exceder o limite do schema")
   void retornaErroParaMesAcimaDoLimite() {
      CallToolResult result = client.callTool(CallToolRequest.builder(
            McpConsts.RFT_TOOL_NAME)
            .arguments(Map.of(McpConsts.RFT_ATTR_MES, 13,
                  McpConsts.RFT_ATTR_ANO, 2040))
            .build());

      assertTrue(result.isError());
      assertTrue(((TextContent) result.content().get(0)).text()
            .contains("deve ter um valor máximo de 12"));
   }
}
