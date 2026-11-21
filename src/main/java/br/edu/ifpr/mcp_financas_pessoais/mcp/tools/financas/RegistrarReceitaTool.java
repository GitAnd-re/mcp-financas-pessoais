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
      String msg = "";
      try {
         Map<String, Object> args = request.arguments();

         Object valorArgumento = args.get(McpConsts.RRT_ATTR_VALOR);
         BigDecimal valor = null;
         if (!(valorArgumento instanceof Number)) {
            msg = "O valor deve ser informado como número.";
         }
         else {
            try {
               valor = new BigDecimal(valorArgumento.toString());
            }
            catch (NumberFormatException ex) {
               msg = "O valor informado é inválido.";
            }
         }

         Object fonteArgumento = args.get(McpConsts.RRT_ATTR_FONTE);
         String fonte = null;
         if (!(fonteArgumento instanceof String)) {
            if (fonteArgumento == null && msg.isEmpty())
               msg = "O campo fonte é obrigatório.";
            else if (msg.isEmpty())
               msg = "O campo fonte deve ser texto.";
         }
         else
            fonte = (String) fonteArgumento;

         Object dataArgumento = args.get(McpConsts.RRT_ATTR_DATA);
         String dataTexto = null;
         if (!(dataArgumento instanceof String)) {
            if (dataArgumento == null && msg.isEmpty())
               msg = "O campo data é obrigatório.";
            else if (msg.isEmpty())
               msg = "O campo data deve ser texto.";
         }
         else {
            dataTexto = (String) dataArgumento;
            if (dataTexto.isBlank() && msg.isEmpty())
               msg = "O campo data não pode ser vazio.";
         }

         LocalDate data = null;
         if (dataTexto != null && !dataTexto.isBlank() && msg.isEmpty()) {
            try {
               data = LocalDate.parse(dataTexto, DATE_FORMATTER);
            }
            catch (DateTimeParseException ex) {
               msg = "A data informada é inválida. Use uma data válida no formato DD/MM/AAAA.";
               return createResult(msg, true);
            }
         }

         if (!msg.isEmpty())
            return createResult(msg, true);

         RegistrarReceitaService service = new RegistrarReceitaService(repositorio);
         Lancamento lancamento;
         try {
            lancamento = service.registrar(valor, fonte, data);
         }
         catch (IllegalArgumentException ex) {
            return createResult(ex.getMessage(), true);
         }

         StringBuilder response = new StringBuilder();
         response.append("id:").append(lancamento.id())
                 .append(", tipo:").append(lancamento.tipo().name())
                 .append(", valor:").append(lancamento.valor().toPlainString())
                 .append(", fonte:").append(lancamento.fonte())
                 .append(", data:").append(lancamento.data().format(DATE_FORMATTER));
         return createResult(response.toString(), false);
      }
      catch (Exception ex) {
         return createResult("Não foi possível salvar a receita: " + ex.getMessage(), true);
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
