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

class ResumoMensalAtualResourceIT {
   private static final Logger LOG = LoggerFactory.getLogger(
         ResumoMensalAtualResourceIT.class);
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
   @DisplayName("Ler resumo financeiro do mês atual pelo servidor MCP")
   void leResumoMensalAtualPeloServidor() {
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
      assertFalse(gasto.isError(), texto(gasto));
      assertFalse(receita.isError(), texto(receita));

      ListResourcesResult resources = client.listResources();
      assertTrue(resources.resources().stream().anyMatch(resource ->
            McpConsts.RESOURCE_RESUMO_MENSAL_ATUAL_URI.equals(resource.uri())));
      ReadResourceResult result = client.readResource(ReadResourceRequest.builder(
            McpConsts.RESOURCE_RESUMO_MENSAL_ATUAL_URI).build());
      String resumo = ((TextResourceContents) result.contents().get(0)).text();

      assertTrue(resumo.startsWith(String.format("Resumo financeiro de %02d/%04d:",
            hoje.getMonthValue(), hoje.getYear())), resumo);
      assertTrue(resumo.contains("total_receitas:50.00"), resumo);
      assertTrue(resumo.contains("total_gastos:12.50"), resumo);
      assertTrue(resumo.contains("saldo:37.50"), resumo);
      assertTrue(resumo.contains("Casa:12.50"), resumo);
   }

   private static String texto(CallToolResult result) {
      return ((TextContent) result.content().get(0)).text();
   }
}
