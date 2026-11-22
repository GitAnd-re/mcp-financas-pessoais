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

public class AnalyzeExpensesPrompt implements McpPromptDefinition {
   private static final String DESCRIPTION =
         "Orienta a análise dos gastos financeiros em um mês ou ano.";

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
         throws Exception {
      Prompt prompt = Prompt.builder(McpConsts.ANALYZE_EXPENSES_PROMPT)
            .description(DESCRIPTION)
            .arguments(List.of(
                  PromptArgument.builder(McpConsts.ANALYZE_EXPENSES_ATTR_ANO)
                        .description("Ano a analisar; deve ser maior que zero.")
                        .required(true)
                        .build(),
                  PromptArgument.builder(McpConsts.ANALYZE_EXPENSES_ATTR_MES)
                        .description("Mês de 1 a 12; se omitido, analisa o ano todo.")
                        .required(false)
                        .build(),
                  PromptArgument.builder(McpConsts.ANALYZE_EXPENSES_ATTR_CONTEXTO)
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

      int ano = readPositiveInteger(args.get(McpConsts.ANALYZE_EXPENSES_ATTR_ANO),
            McpConsts.ANALYZE_EXPENSES_ATTR_ANO);
      Integer mes = readOptionalMonth(
            args.get(McpConsts.ANALYZE_EXPENSES_ATTR_MES));
      String contexto = readOptionalText(
            args.get(McpConsts.ANALYZE_EXPENSES_ATTR_CONTEXTO),
            McpConsts.ANALYZE_EXPENSES_ATTR_CONTEXTO);

      String periodo = mes == null ? "Ano: " + ano
            : String.format("Mês: %02d/%04d", mes, ano);
      String instrucoesResumo = mes == null
            ? "Chame a ferramenta %s com o ano informado e sem mês."
                  .formatted(McpConsts.RFT_TOOL_NAME)
            : "Chame a ferramenta %s com o mês e o ano informados."
                  .formatted(McpConsts.RFT_TOOL_NAME);
      String instrucoesLista = mes == null
            ? "Chame a ferramenta %s com o ano informado e sem mês."
                  .formatted(McpConsts.LLT_TOOL_NAME)
            : "Chame a ferramenta %s com o mês e o ano informados."
                  .formatted(McpConsts.LLT_TOOL_NAME);

      String message = """
            Você é um assistente de finanças pessoais. Analise os gastos do período usando somente os dados retornados pelas ferramentas MCP.

            PERÍODO
            %s

            CONSULTA DOS DADOS
            1. %s
            2. %s
            3. Use os lançamentos para identificar padrões e exemplos concretos. Se uma ferramenta falhar, informe a limitação.

            FOCO ADICIONAL
            %s

            REGRAS
            - Não invente valores, categorias, lançamentos nem tendências.
            - Diferencie os totais retornados pelo servidor das suas interpretações.
            - Analise os gastos por categoria e destaque os maiores valores quando os dados permitirem.
            - Se não houver gastos no período, diga isso claramente.
            - Não apresente a análise como aconselhamento financeiro profissional.

            APRESENTAÇÃO
            Organize a resposta em: resumo do período, gastos por categoria, padrões observados e observações relacionadas ao foco adicional. Seja objetivo e indique quando os dados forem insuficientes.
            """.formatted(periodo, instrucoesResumo, instrucoesLista,
                  contexto.isBlank() ? "Nenhum foco adicional informado." : contexto);

      PromptMessage promptMessage = PromptMessage.builder(Role.USER,
            TextContent.builder(message).build()).build();
      return GetPromptResult.builder(List.of(promptMessage))
            .description(DESCRIPTION)
            .build();
   }

   private static int readPositiveInteger(Object value, String nome) {
      if (!(value instanceof String texto) || texto.isBlank())
         throw new IllegalArgumentException("O argumento '" + nome + "' é obrigatório.");
      try {
         int numero = Integer.parseInt(texto.trim());
         if (numero < 1)
            throw new IllegalArgumentException("O argumento '" + nome + "' deve ser maior que zero.");
         return numero;
      }
      catch (NumberFormatException ex) {
         throw new IllegalArgumentException("O argumento '" + nome + "' deve ser um número inteiro.");
      }
   }

   private static Integer readOptionalMonth(Object value) {
      if (value == null)
         return null;
      if (!(value instanceof String texto))
         throw new IllegalArgumentException("O argumento 'mes' deve ser um número inteiro de 1 a 12.");
      if (texto.isBlank())
         return null;
      try {
         int mes = Integer.parseInt(texto.trim());
         if (mes < 1 || mes > 12)
            throw new IllegalArgumentException("O argumento 'mes' deve estar entre 1 e 12.");
         return mes;
      }
      catch (NumberFormatException ex) {
         throw new IllegalArgumentException("O argumento 'mes' deve ser um número inteiro de 1 a 12.");
      }
   }

   private static String readOptionalText(Object value, String nome) {
      if (value == null)
         return "";
      if (!(value instanceof String texto))
         throw new IllegalArgumentException("O argumento '" + nome + "' deve ser texto.");
      return texto.trim();
   }
}
