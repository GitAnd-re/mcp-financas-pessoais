package br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.SimularParcelamentoService;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.SimulacaoParcelamento;
import br.edu.ifpr.mcp_financas_pessoais.utils.json.schema.SchemaLoader;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class SimularParcelamentoTool implements McpToolDefinition {
   private static final String JSON_SCHEMA =
         "/json-schemas/simular-parcelamento.schema.json";

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
         throws Exception {
      Map<String, Object> inputSchema = SchemaLoader.readFromFile(JSON_SCHEMA);
      Tool tool = Tool.builder(McpConsts.SPT_TOOL_NAME, inputSchema)
            .title("Simular parcelamento")
            .description("Calcula o parcelamento pelo sistema Price e compara com o pagamento à vista. A taxa é um percentual mensal.")
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
         BigDecimal valor = readDecimal(args.get(McpConsts.SPT_ATTR_VALOR), "valor");
         int parcelas = readInteger(args.get(McpConsts.SPT_ATTR_PARCELAS), "parcelas");
         BigDecimal taxa = readDecimal(
               args.get(McpConsts.SPT_ATTR_TAXA_JUROS_MENSAL), "taxa de juros mensal");
         SimulacaoParcelamento simulacao = new SimularParcelamentoService()
               .simular(valor, parcelas, taxa);
         return createResult(formatarResposta(simulacao), false);
      }
      catch (IllegalArgumentException ex) {
         return createResult(ex.getMessage(), true);
      }
      catch (Exception ex) {
         return createResult("Não foi possível simular o parcelamento: " +
               ex.getMessage(), true);
      }
   }

   private static BigDecimal readDecimal(Object value, String nome) {
      if (value == null)
         throw new IllegalArgumentException("O campo " + nome + " é obrigatório.");
      if (!(value instanceof Number))
         throw new IllegalArgumentException("O campo " + nome +
               " deve ser informado como número.");
      try {
         return new BigDecimal(value.toString());
      }
      catch (NumberFormatException ex) {
         throw new IllegalArgumentException("O campo " + nome +
               " informado é inválido.");
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

   private static String formatarResposta(SimulacaoParcelamento simulacao) {
      BigDecimal economiaAVista = simulacao.totalPago()
            .subtract(simulacao.valorAVista());
      return new StringBuilder("Simulação de parcelamento:")
            .append("\nparcelas:").append(simulacao.parcelas())
            .append("\nvalor_parcela:").append(formatarValor(simulacao.valorParcela()))
            .append("\ntotal_pago:").append(formatarValor(simulacao.totalPago()))
            .append("\njuros_pagos:").append(formatarValor(simulacao.jurosPagos()))
            .append("\npagamento_a_vista:").append(formatarValor(simulacao.valorAVista()))
            .append("\neconomia_pagando_a_vista:")
            .append(formatarValor(economiaAVista)).toString();
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
