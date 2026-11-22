package br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.ListarLancamentosService;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
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

public class ListarLancamentosTool implements McpToolDefinition {
   private static final String JSON_SCHEMA =
         "/json-schemas/listar-lancamentos.schema.json";
   private static final DateTimeFormatter DATE_FORMATTER =
         DateTimeFormatter.ofPattern("dd/MM/uuuu");

   private final LancamentoRepositorio repositorio;

   public ListarLancamentosTool(LancamentoRepositorio repositorio) {
      this.repositorio = repositorio;
   }

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
         throws Exception {
      Map<String, Object> inputSchema = SchemaLoader.readFromFile(JSON_SCHEMA);
      Tool tool = Tool.builder(McpConsts.LLT_TOOL_NAME, inputSchema)
            .title("Listar lançamentos")
            .description("Lista os lançamentos de um mês e ano, opcionalmente filtrados por categoria.")
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
         Integer mes = args.containsKey(McpConsts.LLT_ATTR_MES)
               ? readInteger(args.get(McpConsts.LLT_ATTR_MES), "mês")
               : null;
         int ano = readInteger(args.get(McpConsts.LLT_ATTR_ANO), "ano");
         Object categoriaArgumento = args.get(McpConsts.LLT_ATTR_CATEGORIA);
         if (categoriaArgumento != null && !(categoriaArgumento instanceof String))
            return createResult("O campo categoria deve ser texto.", true);

         String categoria = (String) categoriaArgumento;
         List<Lancamento> lancamentos = new ListarLancamentosService(repositorio)
               .listar(mes, ano, categoria);
         return createResult(formatarResposta(lancamentos, mes, ano), false);
      }
      catch (IllegalArgumentException ex) {
         return createResult(ex.getMessage(), true);
      }
      catch (Exception ex) {
         return createResult("Não foi possível listar os lançamentos: " +
               ex.getMessage(), true);
      }
   }

   private static int readInteger(Object value, String nome) {
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

   private static String formatarResposta(List<Lancamento> lancamentos,
                                          Integer mes, int ano) {
      String periodo = mes == null ? String.valueOf(ano)
            : String.format("%02d/%04d", mes, ano);
      if (lancamentos.isEmpty())
         return "Nenhum lançamento encontrado para " + periodo + ".";

      StringBuilder response = new StringBuilder("Lançamentos de ")
            .append(periodo).append(":");
      for (Lancamento lancamento : lancamentos) {
         response.append("\n").append(formatarLancamento(lancamento));
      }
      return response.toString();
   }

   private static String formatarLancamento(Lancamento lancamento) {
      return new StringBuilder("tipo:").append(lancamento.tipo().name())
            .append(", valor:").append(lancamento.valor().toPlainString())
            .append(", categoria:").append(lancamento.categoria())
            .append(", fonte:").append(lancamento.fonte())
            .append(", data:").append(lancamento.data().format(DATE_FORMATTER))
            .append(", descricao:").append(lancamento.descricao())
            .toString();
   }

   private static CallToolResult createResult(String message, boolean isError) {
      return CallToolResult.builder()
            .content(List.of(TextContent.builder(message).build()))
            .isError(isError)
            .build();
   }
}
