package br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.ResumoFinanceiroService;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.ResumoFinanceiro;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.utils.json.schema.SchemaLoader;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class ResumoFinanceiroTool implements McpToolDefinition {
   private static final String JSON_SCHEMA =
         "/json-schemas/resumo-financeiro.schema.json";

   private final LancamentoRepositorio repositorio;

   public ResumoFinanceiroTool(LancamentoRepositorio repositorio) {
      this.repositorio = repositorio;
   }

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
         throws Exception {
      Map<String, Object> inputSchema = SchemaLoader.readFromFile(JSON_SCHEMA);
      Tool tool = Tool.builder(McpConsts.RFT_TOOL_NAME, inputSchema)
            .title("Resumo financeiro")
            .description("Resume receitas, gastos, saldo e gastos por categoria de um mês ou ano.")
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
         Integer mes = readOptionalInteger(args.get(McpConsts.RFT_ATTR_MES), "mês");
         int ano = readRequiredInteger(args.get(McpConsts.RFT_ATTR_ANO), "ano");
         ResumoFinanceiro resumo = new ResumoFinanceiroService(repositorio)
               .resumir(mes, ano);
         return createResult(formatarResposta(resumo), false);
      }
      catch (IllegalArgumentException ex) {
         return createResult(ex.getMessage(), true);
      }
      catch (Exception ex) {
         return createResult("Não foi possível gerar o resumo financeiro: " +
               ex.getMessage(), true);
      }
   }

   private static Integer readOptionalInteger(Object value, String nome) {
      return value == null ? null : readRequiredInteger(value, nome);
   }

   private static int readRequiredInteger(Object value, String nome) {
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

   private static String formatarResposta(ResumoFinanceiro resumo) {
      String periodo = resumo.mes() == null
            ? String.valueOf(resumo.ano())
            : String.format("%02d/%04d", resumo.mes(), resumo.ano());
      StringBuilder response = new StringBuilder("Resumo financeiro de ")
            .append(periodo)
            .append(":\ntotal_receitas:")
            .append(formatarValor(resumo.totalReceitas()))
            .append("\ntotal_gastos:")
            .append(formatarValor(resumo.totalGastos()))
            .append("\nsaldo:")
            .append(formatarValor(resumo.saldo()))
            .append("\n\ngastos_por_categoria:");

      if (resumo.gastosPorCategoria().isEmpty())
         response.append("\nNenhum gasto no período.");
      else
         resumo.gastosPorCategoria().forEach((categoria, valor) ->
               response.append("\n").append(categoria).append(":")
                     .append(formatarValor(valor)));

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
