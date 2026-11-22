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
import br.edu.ifpr.mcp_financas_pessoais.services.financas.RegistrarReceitaService;
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

public class RegistrarReceitaTool implements McpToolDefinition {
   private static final String JSON_SCHEMA =
                            "/json-schemas/registrar-receita.schema.json";
   private static final DateTimeFormatter DATE_FORMATTER =
         DateTimeFormatter.ofPattern("dd/MM/uuuu")
                          .withResolverStyle(ResolverStyle.STRICT);

   private final LancamentoRepositorio repositorio;

   public RegistrarReceitaTool(LancamentoRepositorio repositorio) {
      this.repositorio = repositorio;
   }

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
               throws Exception {
      Map<String, Object> inputSchema = SchemaLoader.readFromFile(JSON_SCHEMA);
      Tool tool = Tool.builder(McpConsts.RRT_TOOL_NAME, inputSchema)
                      .title("Registrar receita")
                      .description("Registra uma receita nas finanças pessoais.")
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
         BigDecimal valor = readValor(args.get(McpConsts.RRT_ATTR_VALOR));
         String fonte = readTextoObrigatorio(
               args.get(McpConsts.RRT_ATTR_FONTE), "fonte");
         LocalDate data = readData(args.get(McpConsts.RRT_ATTR_DATA));

         Lancamento lancamento = new RegistrarReceitaService(repositorio)
               .registrar(valor, fonte, data);

         StringBuilder response = new StringBuilder();
         response.append("id:").append(lancamento.id())
                 .append(", tipo:").append(lancamento.tipo().name())
                 .append(", valor:").append(lancamento.valor().toPlainString())
                 .append(", fonte:").append(lancamento.fonte())
                 .append(", data:").append(lancamento.data().format(DATE_FORMATTER));
         return createResult(response.toString(), false);
      }
      catch (IllegalArgumentException ex) {
         return createResult(ex.getMessage(), true);
      }
      catch (Exception ex) {
         return createResult("Não foi possível salvar a receita: " + ex.getMessage(), true);
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

   private static CallToolResult createResult(String message, boolean isError) {
      String resultMessage = message == null || message.isBlank()
                             ? "Ocorreu um erro ao registrar a receita."
                             : message;
      return CallToolResult.builder()
            .content(List.of(TextContent.builder(resultMessage).build()))
            .isError(isError)
            .build();
   }
}
