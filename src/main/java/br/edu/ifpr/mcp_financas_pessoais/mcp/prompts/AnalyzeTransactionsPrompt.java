package br.edu.ifpr.mcp_financas_pessoais.mcp.prompts;

import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpPromptDefinition;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

public class AnalyzeTransactionsPrompt implements McpPromptDefinition {
   private static final String DESCRIPTION =
         "Orienta a análise dos lançamentos financeiros de um mês ou ano.";

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
         throws Exception {
      Prompt prompt = Prompt.builder(McpConsts.ANALYZE_TRANSACTIONS_PROMPT)
            .description(DESCRIPTION)
            .arguments(List.of(
                  PromptArgument.builder(McpConsts.ANALYZE_TRANSACTIONS_ATTR_ANO)
                        .description("Ano a consultar; deve ser maior que zero.")
                        .required(true)
                        .build(),
                  PromptArgument.builder(McpConsts.ANALYZE_TRANSACTIONS_ATTR_MES)
                        .description("Mês de 1 a 12; se omitido, consulta o ano todo.")
                        .required(false)
                        .build(),
                  PromptArgument.builder(
                        McpConsts.ANALYZE_TRANSACTIONS_ATTR_CATEGORIA)
                        .description("Categoria usada para filtrar os lançamentos.")
                        .required(false)
                        .build(),
                  PromptArgument.builder(
                        McpConsts.ANALYZE_TRANSACTIONS_ATTR_CONTEXTO)
                        .description("Pergunta ou foco adicional da análise.")
                        .required(false)
                        .build()))
            .build();

      mcp.prompts(new SyncPromptSpecification(prompt, this::run));
   }

   @Override
   public GetPromptResult run(McpSyncServerExchange exchange,
                              GetPromptRequest request) {
      Map<String, Object> args = request.arguments();
      if (args == null || args.isEmpty())
         throw new IllegalArgumentException("Nenhum argumento informado.");

      int ano = readPositiveInteger(
            args.get(McpConsts.ANALYZE_TRANSACTIONS_ATTR_ANO),
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_ANO);
      Integer mes = readOptionalMonth(
            args.get(McpConsts.ANALYZE_TRANSACTIONS_ATTR_MES));
      String categoria = readOptionalText(
            args.get(McpConsts.ANALYZE_TRANSACTIONS_ATTR_CATEGORIA),
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_CATEGORIA);
      String contexto = readOptionalText(
            args.get(McpConsts.ANALYZE_TRANSACTIONS_ATTR_CONTEXTO),
            McpConsts.ANALYZE_TRANSACTIONS_ATTR_CONTEXTO);

      String periodo = mes == null ? "Ano: " + ano
            : String.format("Mês: %02d/%04d", mes, ano);
      String filtroCategoria = categoria.isBlank() ? ""
            : "\nCategoria: " + categoria;
      StringBuilder argumentosConsulta = new StringBuilder("ano: ")
            .append(ano);
      if (mes != null)
         argumentosConsulta.append(", mes: ").append(mes);
      if (!categoria.isBlank())
         argumentosConsulta.append(", categoria: ").append(categoria);
      String instrucaoConsulta = "Consulte a ferramenta %s para obter os "
            .formatted(McpConsts.LLT_TOOL_NAME) +
            (mes == null ? "lançamentos do ano informado."
                  : "lançamentos do mês e ano informados.") +
            (categoria.isBlank() ? ""
                  : " Aplique também o filtro da categoria informada.") +
            " Use estes argumentos: " + argumentosConsulta + ".";

      String message = """
            Você é um assistente de finanças pessoais. Analise os lançamentos individuais usando somente os dados retornados pelas ferramentas MCP.

            PERÍODO
            %s%s

            CONSULTA DOS DADOS
            %s Passe à ferramenta somente os filtros informados. Se a consulta retornar erro, explique a limitação e não invente resultados.

            FOCO ADICIONAL
            %s

            REGRAS DE ANÁLISE
            - Considere cada lançamento retornado e diferencie gastos de receitas.
            - Use as descrições e categorias disponíveis para identificar padrões ou itens que mereçam atenção.
            - Não invente transações, datas, categorias ou valores, nem atribua recorrência sem evidência nos registros.
            - Não some valores de memória quando a precisão do total for importante; indique quando os dados retornados não forem suficientes para concluir.
            - Se nenhum lançamento for encontrado, informe isso claramente.

            APRESENTAÇÃO
            Organize a resposta em: visão geral dos lançamentos, padrões encontrados, itens relevantes e observações relacionadas ao foco informado. Seja objetivo e separe fatos de interpretações.
            """.formatted(periodo, filtroCategoria, instrucaoConsulta,
                  contexto.isBlank() ? "Nenhum foco adicional informado." : contexto);

      PromptMessage promptMessage = PromptMessage.builder(Role.USER,
            TextContent.builder(message).build()).build();
      return GetPromptResult.builder(List.of(promptMessage))
            .description(DESCRIPTION)
            .build();
   }

   private static int readPositiveInteger(Object value, String nome) {
      if (!(value instanceof String texto) || texto.isBlank())
         throw new IllegalArgumentException(
               "O argumento '" + nome + "' é obrigatório.");
      try {
         int numero = Integer.parseInt(texto.trim());
         if (numero < 1)
            throw new IllegalArgumentException(
                  "O argumento '" + nome + "' deve ser maior que zero.");
         return numero;
      }
      catch (NumberFormatException ex) {
         throw new IllegalArgumentException(
               "O argumento '" + nome + "' deve ser um número inteiro.");
      }
   }

   private static Integer readOptionalMonth(Object value) {
      if (value == null)
         return null;
      if (!(value instanceof String texto))
         throw new IllegalArgumentException(
               "O argumento 'mes' deve ser um número inteiro de 1 a 12.");
      if (texto.isBlank())
         return null;
      try {
         int mes = Integer.parseInt(texto.trim());
         if (mes < 1 || mes > 12)
            throw new IllegalArgumentException(
                  "O argumento 'mes' deve estar entre 1 e 12.");
         return mes;
      }
      catch (NumberFormatException ex) {
         throw new IllegalArgumentException(
               "O argumento 'mes' deve ser um número inteiro de 1 a 12.");
      }
   }

   private static String readOptionalText(Object value, String nome) {
      if (value == null)
         return "";
      if (!(value instanceof String texto))
         throw new IllegalArgumentException(
               "O argumento '" + nome + "' deve ser texto.");
      return texto.trim();
   }
}
