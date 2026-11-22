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

class SimularParcelamentoToolIT {
   private static final Logger LOG = LoggerFactory.getLogger(SimularParcelamentoToolIT.class);
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
   @DisplayName("Descobrir e executar simular_parcelamento pelo servidor stdio")
   void simulaParcelamentoPeloServidor() {
      ListToolsResult tools = client.listTools();
      assertTrue(tools.tools().stream().anyMatch(tool ->
            McpConsts.SPT_TOOL_NAME.equals(tool.name())));

      CallToolResult result = client.callTool(CallToolRequest.builder(
            McpConsts.SPT_TOOL_NAME)
            .arguments(Map.of(McpConsts.SPT_ATTR_VALOR, new BigDecimal("1000.00"),
                  McpConsts.SPT_ATTR_PARCELAS, 12,
                  McpConsts.SPT_ATTR_TAXA_JUROS_MENSAL, new BigDecimal("2.00")))
            .build());

      assertFalse(result.isError());
      String response = ((TextContent) result.content().get(0)).text();
      assertTrue(response.contains("valor_parcela:94.56"), response);
      assertTrue(response.contains("total_pago:1134.72"), response);
      assertTrue(response.contains("juros_pagos:134.72"), response);
      assertTrue(response.contains("economia_pagando_a_vista:134.72"), response);
   }

   @Test
   @DisplayName("Retornar erro MCP para parcelas abaixo do mínimo")
   void retornaErroParaParcelasInvalidas() {
      CallToolResult result = client.callTool(CallToolRequest.builder(
            McpConsts.SPT_TOOL_NAME)
            .arguments(Map.of(McpConsts.SPT_ATTR_VALOR, BigDecimal.TEN,
                  McpConsts.SPT_ATTR_PARCELAS, 0,
                  McpConsts.SPT_ATTR_TAXA_JUROS_MENSAL, BigDecimal.ZERO))
            .build());

      assertTrue(result.isError());
      assertTrue(((TextContent) result.content().get(0)).text()
            .contains("deve ter um valor mínimo de 1"));
   }

   @Test
   @DisplayName("Retornar erro MCP para taxa de juros negativa")
   void retornaErroParaTaxaNegativa() {
      CallToolResult result = client.callTool(CallToolRequest.builder(
            McpConsts.SPT_TOOL_NAME)
            .arguments(Map.of(McpConsts.SPT_ATTR_VALOR, BigDecimal.TEN,
                  McpConsts.SPT_ATTR_PARCELAS, 1,
                  McpConsts.SPT_ATTR_TAXA_JUROS_MENSAL,
                  new BigDecimal("-0.01")))
            .build());

      assertTrue(result.isError());
      assertTrue(((TextContent) result.content().get(0)).text()
            .contains("deve ter um valor mínimo de 0"));
   }
}
