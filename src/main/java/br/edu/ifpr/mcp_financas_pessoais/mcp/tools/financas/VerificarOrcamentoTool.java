package br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.VerificarOrcamentoService;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.SituacaoOrcamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.OrcamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.utils.json.schema.SchemaLoader;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class VerificarOrcamentoTool implements McpToolDefinition {
   private static final String JSON_SCHEMA =
         "/json-schemas/verificar-orcamento.schema.json";

   private final LancamentoRepositorio lancamentoRepositorio;
   private final OrcamentoRepositorio orcamentoRepositorio;

   public VerificarOrcamentoTool(LancamentoRepositorio lancamentoRepositorio,
                                 OrcamentoRepositorio orcamentoRepositorio) {
      this.lancamentoRepositorio = lancamentoRepositorio;
      this.orcamentoRepositorio = orcamentoRepositorio;
   }

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
         throws Exception {
      Map<String, Object> inputSchema = SchemaLoader.readFromFile(JSON_SCHEMA);
      Tool tool = Tool.builder(McpConsts.VOT_TOOL_NAME, inputSchema)
            .title("Verificar orçamento")
            .description("Compara os gastos por categoria com os orçamentos definidos para o mês e ano informados.")
            .build();
      SyncToolSpecification specification = SyncToolSpecification.builder()
            .tool(tool)
            .callHandler(this::run)
            .build();
      mcp.tools(specification);
   }

   @Override
   public CallToolResult run(McpSyncServerExchange exchange,
                             CallToolRequest request) {
      try {
         Map<String, Object> args = request.arguments();
         int mes = readInteger(args.get(McpConsts.VOT_ATTR_MES), "mês");
         int ano = readInteger(args.get(McpConsts.VOT_ATTR_ANO), "ano");
         List<SituacaoOrcamento> situacoes = new VerificarOrcamentoService(
               lancamentoRepositorio, orcamentoRepositorio).verificar(mes, ano);
         return createResult(formatarResposta(situacoes, mes, ano), false);
      }
      catch (IllegalArgumentException ex) {
         return createResult(ex.getMessage(), true);
      }
      catch (Exception ex) {
         return createResult("Não foi possível verificar os orçamentos: " +
               ex.getMessage(), true);
      }
   }

   private static int readInteger(Object value, String nome) {
      if (value == null)
         throw new IllegalArgumentException("O campo " + nome + " é obrigatório.");
      if (!(value instanceof Number))
         throw new IllegalArgumentException("O campo " + nome +
               " deve ser informado como número inteiro.");
      try {
         return new BigDecimal(value.toString()).intValueExact();
      }
      catch (NumberFormatException | ArithmeticException ex) {
         throw new IllegalArgumentException("O campo " + nome +
               " deve ser informado como número inteiro.");
      }
   }

   private static String formatarResposta(List<SituacaoOrcamento> situacoes,
                                          int mes, int ano) {
      StringBuilder response = new StringBuilder("Orçamentos de ")
            .append(String.format("%02d/%04d", mes, ano)).append(":");
      for (SituacaoOrcamento situacao : situacoes) {
         response.append("\n").append(situacao.categoria())
               .append(": gasto:").append(formatarValor(situacao.gasto()))
               .append(", limite:").append(formatarValor(situacao.limite()))
               .append(", restante:").append(formatarValor(situacao.restante()))
               .append(", estourou:").append(situacao.estourou());
      }
      return response.toString();
   }

   private static String formatarValor(BigDecimal valor) {
      return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
   }

   private static CallToolResult createResult(String message, boolean isError) {
      return CallToolResult.builder()
            .content(List.of(TextContent.builder(message).build()))
            .isError(isError)
            .build();
   }
}
