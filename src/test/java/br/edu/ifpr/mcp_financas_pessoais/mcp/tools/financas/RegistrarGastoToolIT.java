package br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
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

class RegistrarGastoToolIT {
   private static final Logger LOG = LoggerFactory.getLogger(RegistrarGastoToolIT.class);
   private static final String SERVER_PATH = "server/mcp_financas_pessoais_server.jar";

   @TempDir static Path tempDir;
   private static McpSyncClient client;
   private static Path arquivoDados;

   @BeforeAll
   static void iniciarServidorECliente() throws Exception {
      File serverJar = new File(SERVER_PATH);
      assertTrue(serverJar.isFile(), "JAR do servidor não encontrado: " + serverJar.getAbsolutePath());
      arquivoDados = tempDir.resolve("lancamentos.json");

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
   @DisplayName("Lista e executa registrar_gasto através do servidor stdio")
   void registraGastoEGravaJson() throws Exception {
      ListToolsResult tools = client.listTools();
      assertTrue(tools.tools().stream().anyMatch(tool ->
            McpConsts.RGT_TOOL_NAME.equals(tool.name())));

      CallToolRequest request = CallToolRequest.builder(McpConsts.RGT_TOOL_NAME)
            .arguments(Map.of("valor", 42.75, "categoria", "Transporte",
                              "data", "20/11/2026", "descricao", "Ônibus"))
            .build();
      CallToolResult result = client.callTool(request);

      assertNotNull(result);
      assertFalse(result.isError());
      String response = ((TextContent) result.content().get(0)).text();
      assertTrue(response.startsWith("id:"));
      assertTrue(response.contains(", tipo:GASTO"));
      assertTrue(response.contains(", data:20/11/2026"));
      assertTrue(Files.isRegularFile(arquivoDados));
      String saved = Files.readString(arquivoDados);
      assertTrue(saved.contains("\"GASTO\""));
      assertTrue(saved.contains("Transporte"));
      assertTrue(saved.contains("20/11/2026"));
      assertTrue(saved.contains("Ônibus"));
   }

   @Test
   @DisplayName("Retorna erro MCP para data inexistente")
   void retornaErroParaDataInvalida() {
      CallToolRequest request = CallToolRequest.builder(McpConsts.RGT_TOOL_NAME)
            .arguments(Map.of("valor", 42.75, "categoria", "Transporte",
                              "data", "31/02/2026"))
            .build();
      CallToolResult result = client.callTool(request);

      assertTrue(result.isError());
      assertTrue(((TextContent) result.content().get(0)).text().contains("DD/MM/AAAA"));
   }
}
