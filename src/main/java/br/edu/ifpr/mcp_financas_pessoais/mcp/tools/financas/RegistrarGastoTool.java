package br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.RegistrarGastoService;
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

public class RegistrarGastoTool implements McpToolDefinition {
   private static final String JSON_SCHEMA =
                            "/json-schemas/registrar-gasto.schema.json";
   private static final DateTimeFormatter DATE_FORMATTER =
         DateTimeFormatter.ofPattern("dd/MM/uuuu")
                          .withResolverStyle(ResolverStyle.STRICT);

   private final LancamentoRepositorio repositorio;

   public RegistrarGastoTool(LancamentoRepositorio repositorio) {
      this.repositorio = repositorio;
   }

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
               throws Exception {
      Map<String, Object> inputSchema = SchemaLoader.readFromFile(JSON_SCHEMA);
      Tool tool = Tool.builder(McpConsts.RGT_TOOL_NAME, inputSchema)
                      .title("Registrar gasto")
                      .description("Registra um gasto nas finanças pessoais.")
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
         BigDecimal valor = readValor(args.get(McpConsts.RGT_ATTR_VALOR));
         String categoria = readTextoObrigatorio(
               args.get(McpConsts.RGT_ATTR_CATEGORIA), "categoria");
         LocalDate data = readData(args.get(McpConsts.RGT_ATTR_DATA));
         String descricao = readDescricao(args.get(McpConsts.RGT_ATTR_DESCRICAO));

         Lancamento lancamento = new RegistrarGastoService(repositorio)
               .registrar(valor, categoria, data, descricao);

         StringBuilder response = new StringBuilder();
         response.append("tipo:").append(lancamento.tipo().name())
                 .append(", valor:").append(lancamento.valor().toPlainString())
                 .append(", categoria:").append(lancamento.categoria())
                 .append(", data:").append(lancamento.data().format(DATE_FORMATTER))
                 .append(", descricao:").append(lancamento.descricao());
         return createResult(response.toString(), false);
      }
      catch (IllegalArgumentException ex) {
         return createResult(ex.getMessage(), true);
      }
      catch (Exception ex) {
         return createResult("Não foi possível salvar o gasto: " + ex.getMessage(), true);
      }
   }

   private static BigDecimal readValor(Object value) {
      if (!(value instanceof Number))
         throw new IllegalArgumentException("O valor deve ser informado como número.");
      try {
         return new BigDecimal(value.toString());
      }
      catch (NumberFormatException ex) {
         throw new IllegalArgumentException("O valor informado é inválido.");
      }
   }

   private static String readTextoObrigatorio(Object value, String nome) {
      if (value == null)
         throw new IllegalArgumentException("O campo " + nome + " é obrigatório.");
      if (!(value instanceof String))
         throw new IllegalArgumentException("O campo " + nome + " deve ser texto.");
      return (String) value;
   }

   private static LocalDate readData(Object value) {
      String texto = readTextoObrigatorio(value, "data");
      if (texto.isBlank())
         throw new IllegalArgumentException("O campo data não pode ser vazio.");
      try {
         return LocalDate.parse(texto, DATE_FORMATTER);
      }
      catch (DateTimeParseException ex) {
         throw new IllegalArgumentException(
               "A data informada é inválida. Use uma data válida no formato DD/MM/AAAA.");
      }
   }

   private static String readDescricao(Object value) {
      if (value == null)
         return "";
      if (!(value instanceof String))
         throw new IllegalArgumentException("O campo descrição deve ser texto.");
      return (String) value;
   }

   private static CallToolResult createResult(String message, boolean isError) {
      String resultMessage = message == null || message.isBlank()
                             ? "Ocorreu um erro ao registrar o gasto."
                             : message;
      return CallToolResult.builder()
            .content(List.of(TextContent.builder(resultMessage).build()))
            .isError(isError)
            .build();
   }
}
