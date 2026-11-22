package br.edu.ifpr.mcp_financas_pessoais.mcp.resources;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import io.modelcontextprotocol.spec.McpSchema.ListResourcesResult;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import tools.jackson.databind.json.JsonMapper;

class CategoriasResourceIT {
   private static final Logger LOG = LoggerFactory.getLogger(CategoriasResourceIT.class);
   private static final String SERVER_PATH = "server/mcp_financas_pessoais_server.jar";
   private static final DateTimeFormatter DATA_FORMAT =
         DateTimeFormatter.ofPattern("dd/MM/uuuu");

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
   @DisplayName("Listar categorias de gastos e orçamentos pelo servidor MCP")
   void listaCategoriasPeloServidor() {
      LocalDate hoje = LocalDate.now();
      String data = hoje.format(DATA_FORMAT);
      CallToolResult gasto = client.callTool(CallToolRequest.builder(
            McpConsts.RGT_TOOL_NAME)
            .arguments(Map.of(McpConsts.RGT_ATTR_VALOR, new BigDecimal("12.50"),
                  McpConsts.RGT_ATTR_CATEGORIA, "Casa",
                  McpConsts.RGT_ATTR_DATA, data))
            .build());
      CallToolResult receita = client.callTool(CallToolRequest.builder(
            McpConsts.RRT_TOOL_NAME)
            .arguments(Map.of(McpConsts.RRT_ATTR_VALOR, new BigDecimal("50.00"),
                  McpConsts.RRT_ATTR_FONTE, "Salário",
                  McpConsts.RRT_ATTR_DATA, data))
            .build());
      CallToolResult orcamento = client.callTool(CallToolRequest.builder(
            McpConsts.DOT_TOOL_NAME)
            .arguments(Map.of(McpConsts.DOT_ATTR_CATEGORIA, "Lazer",
                  McpConsts.DOT_ATTR_MES, hoje.getMonthValue(),
                  McpConsts.DOT_ATTR_ANO, hoje.getYear(),
                  McpConsts.DOT_ATTR_LIMITE_MENSAL, new BigDecimal("100.00")))
            .build());
      assertFalse(gasto.isError(), texto(gasto));
      assertFalse(receita.isError(), texto(receita));
      assertFalse(orcamento.isError(), texto(orcamento));

      ListResourcesResult resources = client.listResources();
      assertTrue(resources.resources().stream().anyMatch(resource ->
            McpConsts.RESOURCE_CATEGORIAS_URI.equals(resource.uri())));
      ReadResourceResult result = client.readResource(ReadResourceRequest.builder(
            McpConsts.RESOURCE_CATEGORIAS_URI).build());
      String categorias = ((TextResourceContents) result.contents().get(0)).text();
      assertTrue(categorias.contains("Casa"), categorias);
      assertTrue(categorias.contains("Lazer"), categorias);
      assertFalse(categorias.contains("Salário"), categorias);
   }

   private static String texto(CallToolResult result) {
      return ((TextContent) result.content().get(0)).text();
   }
}
