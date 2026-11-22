package br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigDecimal;
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

class DefinirOrcamentoToolIT {
   private static final Logger LOG = LoggerFactory.getLogger(DefinirOrcamentoToolIT.class);
   private static final String SERVER_PATH = "server/mcp_financas_pessoais_server.jar";

   @TempDir static Path tempDir;
   private static McpSyncClient client;
   private static Path arquivoDados;

   @BeforeAll
   static void iniciarServidorECliente() throws Exception {
      File serverJar = new File(SERVER_PATH);
      assertTrue(serverJar.isFile(), "JAR do servidor não encontrado: " +
            serverJar.getAbsolutePath());
      arquivoDados = tempDir.resolve("orcamentos.json");
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
   @DisplayName("Descobrir e executar definir_orcamento no servidor stdio")
   void defineOrcamentoEGravaJson() throws Exception {
      ListToolsResult tools = client.listTools();
      assertTrue(tools.tools().stream().anyMatch(tool ->
            McpConsts.DOT_TOOL_NAME.equals(tool.name())));
      CallToolRequest request = CallToolRequest.builder(McpConsts.DOT_TOOL_NAME)
            .arguments(Map.of(McpConsts.DOT_ATTR_CATEGORIA, "Alimentação",
                  McpConsts.DOT_ATTR_MES, 11,
                  McpConsts.DOT_ATTR_ANO, 2026,
                  McpConsts.DOT_ATTR_LIMITE_MENSAL, new BigDecimal("500.00")))
            .build();
      CallToolResult result = client.callTool(request);

      assertFalse(result.isError());
      String response = ((TextContent) result.content().get(0)).text();
      assertTrue(response.contains("categoria:Alimentação"));
      assertTrue(response.contains("mes:11, ano:2026"), response);
      assertTrue(response.contains("limite_mensal:500"), response);
      assertTrue(Files.isRegularFile(arquivoDados));
      String saved = Files.readString(arquivoDados);
      assertTrue(saved.contains("\"categoria\""));
      assertTrue(saved.contains("\"mes\""));
      assertTrue(saved.contains("\"ano\""));
      assertTrue(saved.contains("\"limite_mensal\""));
      assertTrue(saved.contains("Alimentação"));
      assertTrue(saved.contains("500"));
   }

   @Test
   @DisplayName("Retornar erro MCP quando o limite mensal for zero")
   void retornaErroParaLimiteZero() {
      CallToolRequest request = CallToolRequest.builder(McpConsts.DOT_TOOL_NAME)
            .arguments(Map.of(McpConsts.DOT_ATTR_CATEGORIA, "Alimentação",
                  McpConsts.DOT_ATTR_MES, 11,
                  McpConsts.DOT_ATTR_ANO, 2026,
                  McpConsts.DOT_ATTR_LIMITE_MENSAL, BigDecimal.ZERO))
            .build();
      CallToolResult result = client.callTool(request);
      assertTrue(result.isError());
      String error = ((TextContent) result.content().get(0)).text();
      assertTrue(error.contains("limite_mensal"), error);
   }

   @Test
   @DisplayName("Retornar erro MCP quando a categoria for numérica")
   void retornaErroParaCategoriaNumerica() {
      CallToolRequest request = CallToolRequest.builder(McpConsts.DOT_TOOL_NAME)
            .arguments(Map.of(McpConsts.DOT_ATTR_CATEGORIA, "123",
                  McpConsts.DOT_ATTR_MES, 11,
                  McpConsts.DOT_ATTR_ANO, 2026,
                  McpConsts.DOT_ATTR_LIMITE_MENSAL, new BigDecimal("500.00")))
            .build();
      CallToolResult result = client.callTool(request);
      assertTrue(result.isError());
      assertTrue(((TextContent) result.content().get(0)).text()
            .contains("A categoria não pode ser um número."));
   }
}
