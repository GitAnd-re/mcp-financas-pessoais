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
      if (repositorio == null)
         throw new IllegalArgumentException("O repositório de lançamentos é obrigatório.");
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
      String msg = "";
      try {
         Map<String, Object> args = request.arguments();

         Object valorArgumento = args.get(McpConsts.RGT_ATTR_VALOR);
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

         Object categoriaArgumento = args.get(McpConsts.RGT_ATTR_CATEGORIA);
         String categoria = null;
         if (!(categoriaArgumento instanceof String)) {
            if (categoriaArgumento == null && msg.isEmpty())
               msg = "O campo categoria é obrigatório.";
            else if (msg.isEmpty())
               msg = "O campo categoria deve ser texto.";
         }
         else {
            categoria = (String) categoriaArgumento;
            if (categoria.isBlank() && msg.isEmpty())
               msg = "O campo categoria não pode ser vazio.";
         }

         Object dataArgumento = args.get(McpConsts.RGT_ATTR_DATA);
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

         Object descricaoArgumento = args.get(McpConsts.RGT_ATTR_DESCRICAO);
         String descricao = "";
         if (descricaoArgumento == null) {
            descricao = "";
         }
         else if (descricaoArgumento instanceof String) {
            descricao = (String) descricaoArgumento;
         }
         else if (msg.isEmpty()) {
            msg = "O campo descrição deve ser texto.";
         }

         if (valor != null && valor.signum() <= 0 && msg.isEmpty())
            msg = "O valor deve ser maior que zero.";

         if (!msg.isEmpty()) {
            return createResult(msg, true);
         }

         RegistrarGastoService service = new RegistrarGastoService(repositorio);
         Lancamento lancamento = service.registrar(valor, categoria, data, descricao);

         StringBuilder response = new StringBuilder();
         response.append("id:").append(lancamento.id())
                 .append(", tipo:").append(lancamento.tipo().name())
                 .append(", valor:").append(lancamento.valor().toPlainString())
                 .append(", categoria:").append(lancamento.categoria())
                 .append(", data:").append(lancamento.data().format(DATE_FORMATTER))
                 .append(", descricao:").append(lancamento.descricao());
         return createResult(response.toString(), false);
      }
      catch (Exception ex) {
         return createResult("Não foi possível salvar o gasto: " + ex.getMessage(), true);
      }
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
