package br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.DefinirOrcamentoService;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Orcamento;
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

public class DefinirOrcamentoTool implements McpToolDefinition {
   private static final String JSON_SCHEMA =
         "/json-schemas/definir-orcamento.schema.json";

   private final OrcamentoRepositorio repositorio;

   public DefinirOrcamentoTool(OrcamentoRepositorio repositorio) {
      this.repositorio = repositorio;
   }

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
         throws Exception {
      Map<String, Object> inputSchema = SchemaLoader.readFromFile(JSON_SCHEMA);
      Tool tool = Tool.builder(McpConsts.DOT_TOOL_NAME, inputSchema)
            .title("Definir orçamento")
            .description("Define ou atualiza o limite mensal de gastos de uma categoria.")
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
         String categoria = readCategoria(args.get(McpConsts.DOT_ATTR_CATEGORIA));
         int mes = readInteger(args.get(McpConsts.DOT_ATTR_MES), "mês");
         int ano = readInteger(args.get(McpConsts.DOT_ATTR_ANO), "ano");
         BigDecimal limite = readLimite(args.get(McpConsts.DOT_ATTR_LIMITE_MENSAL));
         Orcamento orcamento = new DefinirOrcamentoService(repositorio)
               .definir(categoria, mes, ano, limite);
         return createResult(formatarResposta(orcamento), false);
      }
      catch (IllegalArgumentException ex) {
         return createResult(ex.getMessage(), true);
      }
      catch (Exception ex) {
         return createResult("Não foi possível salvar o orçamento: " +
               ex.getMessage(), true);
      }
   }

   private static String readCategoria(Object value) {
      if (value == null)
         throw new IllegalArgumentException("O campo categoria é obrigatório.");
      if (!(value instanceof String))
         throw new IllegalArgumentException("O campo categoria deve ser texto.");
      return (String) value;
   }

   private static BigDecimal readLimite(Object value) {
      if (value == null)
         throw new IllegalArgumentException("O campo limite_mensal é obrigatório.");
      if (!(value instanceof Number))
         throw new IllegalArgumentException("O limite mensal deve ser informado como número.");
      try {
         return new BigDecimal(value.toString());
      }
      catch (NumberFormatException ex) {
         throw new IllegalArgumentException("O limite mensal informado é inválido.");
      }
   }

   private static String formatarResposta(Orcamento orcamento) {
      return new StringBuilder("categoria:").append(orcamento.categoria())
            .append(", mes:").append(orcamento.mes())
            .append(", ano:").append(orcamento.ano())
            .append(", limite_mensal:")
            .append(orcamento.limiteMensal().toPlainString()).toString();
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

   private static CallToolResult createResult(String message, boolean isError) {
      return CallToolResult.builder()
            .content(List.of(TextContent.builder(message).build()))
            .isError(isError)
            .build();
   }
}
