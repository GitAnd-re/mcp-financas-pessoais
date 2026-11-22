package br.edu.ifpr.mcp_financas_pessoais.mcp.utils;

public final class McpConsts {
   // info sobre o servidor MCP
   public static final String APP_NAME        = "Servidor MCP Java";
   public static final String APP_VERSION     = "1.1.0";

   // nome e atributo(s) (propriedades nos esquemas JSON) das ferramentas
   // ("tools")
   // RegistrarGastoTool
   public static final String RGT_TOOL_NAME        = "registrar_gasto";
   public static final String RGT_ATTR_VALOR       = "valor";
   public static final String RGT_ATTR_CATEGORIA   = "categoria";
   public static final String RGT_ATTR_DATA        = "data";
   public static final String RGT_ATTR_DESCRICAO   = "descricao";
   // RegistrarReceitaTool
   public static final String RRT_TOOL_NAME  = "registrar_receita";
   public static final String RRT_ATTR_VALOR = "valor";
   public static final String RRT_ATTR_FONTE = "fonte";
   public static final String RRT_ATTR_DATA  = "data";
   // ListarLancamentosTool
   public static final String LLT_TOOL_NAME        = "listar_lancamentos";
   public static final String LLT_ATTR_MES         = "mes";
   public static final String LLT_ATTR_ANO         = "ano";
   public static final String LLT_ATTR_CATEGORIA   = "categoria";
   // ResumoFinanceiroTool
   public static final String RFT_TOOL_NAME = "resumo_financeiro";
   public static final String RFT_ATTR_MES  = "mes";
   public static final String RFT_ATTR_ANO  = "ano";
   // DefinirOrcamentoTool
   public static final String DOT_TOOL_NAME = "definir_orcamento";
   public static final String DOT_ATTR_CATEGORIA = "categoria";
   public static final String DOT_ATTR_MES = "mes";
   public static final String DOT_ATTR_ANO = "ano";
   public static final String DOT_ATTR_LIMITE_MENSAL = "limite_mensal";
   // VerificarOrcamentoTool
   public static final String VOT_TOOL_NAME = "verificar_orcamento";
   public static final String VOT_ATTR_MES = "mes";
   public static final String VOT_ATTR_ANO = "ano";
   // SimularParcelamentoTool
   public static final String SPT_TOOL_NAME = "simular_parcelamento";
   public static final String SPT_ATTR_VALOR = "valor";
   public static final String SPT_ATTR_PARCELAS = "parcelas";
   public static final String SPT_ATTR_TAXA_JUROS_MENSAL = "taxa_juros_mensal";

   // nome e atributo(s) dos prompts
   // AnalyzeExpensesPrompt
   public static final String ANALYZE_EXPENSES_PROMPT = "analisar_gastos";
   public static final String ANALYZE_EXPENSES_ATTR_MES = "mes";
   public static final String ANALYZE_EXPENSES_ATTR_ANO = "ano";
   public static final String ANALYZE_EXPENSES_ATTR_CONTEXTO = "contexto";
   // AnalyzeTransactionsPrompt
   public static final String ANALYZE_TRANSACTIONS_PROMPT = "analisar_lancamentos";
   public static final String ANALYZE_TRANSACTIONS_ATTR_MES = "mes";
   public static final String ANALYZE_TRANSACTIONS_ATTR_ANO = "ano";
   public static final String ANALYZE_TRANSACTIONS_ATTR_CATEGORIA = "categoria";
   public static final String ANALYZE_TRANSACTIONS_ATTR_CONTEXTO = "contexto";

   // URIs dos recursos ("resources")
   // FinanceiroResources
   public static final String RESOURCE_CATEGORIAS_NAME = "categorias";
   public static final String RESOURCE_CATEGORIAS_URI = "financas://categorias";
   public static final String RESOURCE_RESUMO_MENSAL_ATUAL_NAME =
         "resumo-mensal-atual";
   public static final String RESOURCE_RESUMO_MENSAL_ATUAL_URI =
         "financas://resumo-mensal-atual";
}
