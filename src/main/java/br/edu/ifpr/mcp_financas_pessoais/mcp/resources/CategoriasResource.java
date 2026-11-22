package br.edu.ifpr.mcp_financas_pessoais.mcp.resources;

import java.io.IOException;
import java.util.List;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpResourceDefinition;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.ListarCategoriasService;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.OrcamentoRepositorio;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;

public class CategoriasResource implements McpResourceDefinition {
   private final ListarCategoriasService service;

   public CategoriasResource(LancamentoRepositorio lancamentoRepositorio,
                             OrcamentoRepositorio orcamentoRepositorio) {
      this.service = new ListarCategoriasService(lancamentoRepositorio,
            orcamentoRepositorio);
   }

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
         throws Exception {
      Resource resource = Resource.builder(McpConsts.RESOURCE_CATEGORIAS_URI,
            McpConsts.RESOURCE_CATEGORIAS_NAME)
            .title("Categorias de gastos")
            .description("Lista as categorias de gastos registradas ou orçadas.")
            .mimeType("text/plain")
            .build();
      mcp.resources(new SyncResourceSpecification(resource, this::read));
   }

   @Override
   public ReadResourceResult read(McpSyncServerExchange exchange,
                                  ReadResourceRequest request) {
      String content;
      try {
         List<String> categorias = service.listar();
         content = categorias.isEmpty()
               ? "Nenhuma categoria encontrada."
               : String.join("\n", categorias);
      }
      catch (IOException ex) {
         content = "Não foi possível listar as categorias: " + ex.getMessage();
      }
      TextResourceContents text = TextResourceContents.builder(
            McpConsts.RESOURCE_CATEGORIAS_URI, content)
            .mimeType("text/plain")
            .build();
      return ReadResourceResult.builder(List.of(text)).build();
   }
}
