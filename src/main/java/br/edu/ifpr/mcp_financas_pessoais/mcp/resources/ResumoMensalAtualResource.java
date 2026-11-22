package br.edu.ifpr.mcp_financas_pessoais.mcp.resources;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpResourceDefinition;
import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.ResumoFinanceiroFormatter;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.ResumoFinanceiroService;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.ResumoFinanceiro;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;

public class ResumoMensalAtualResource implements McpResourceDefinition {
   private final LancamentoRepositorio repositorio;
   private final Clock clock;

   public ResumoMensalAtualResource(LancamentoRepositorio repositorio) {
      this(repositorio, Clock.systemDefaultZone());
   }

   ResumoMensalAtualResource(LancamentoRepositorio repositorio, Clock clock) {
      if (repositorio == null)
         throw new IllegalArgumentException("O repositório de lançamentos é obrigatório.");
      if (clock == null)
         throw new IllegalArgumentException("O relógio é obrigatório.");
      this.repositorio = repositorio;
      this.clock = clock;
   }

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
         throws Exception {
      Resource resource = Resource.builder(
            McpConsts.RESOURCE_RESUMO_MENSAL_ATUAL_URI,
            McpConsts.RESOURCE_RESUMO_MENSAL_ATUAL_NAME)
            .title("Resumo financeiro do mês atual")
            .description("Apresenta receitas, gastos, saldo e gastos por categoria do mês atual.")
            .mimeType("text/plain")
            .build();
      mcp.resources(new SyncResourceSpecification(resource, this::read));
   }

   @Override
   public ReadResourceResult read(McpSyncServerExchange exchange,
                                  ReadResourceRequest request) {
      LocalDate hoje = LocalDate.now(clock);
      String content;
      try {
         ResumoFinanceiro resumo = new ResumoFinanceiroService(repositorio)
               .resumir(hoje.getMonthValue(), hoje.getYear());
         content = ResumoFinanceiroFormatter.formatar(resumo);
      }
      catch (IOException ex) {
         content = "Não foi possível gerar o resumo financeiro: " + ex.getMessage();
      }
      TextResourceContents text = TextResourceContents.builder(
            McpConsts.RESOURCE_RESUMO_MENSAL_ATUAL_URI, content)
            .mimeType("text/plain")
            .build();
      return ReadResourceResult.builder(List.of(text)).build();
   }

}
