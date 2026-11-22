package br.edu.ifpr.mcp_financas_pessoais.mcp.utils;

public final class McpConsts {
   // info sobre o servidor MCP
   public static final String APP_NAME        = "Servidor MCP Java";
   public static final String APP_VERSION     = "1.1.0";
   public static final String APP_DESCRIPTION =
                                 "Descrição: Servidor MCP Java de demonstração";

   // nome e atributo(s) (propriedades nos esquemas JSON) das ferramentas
   // ("tools")
   // AuthPathTool
   public static final String APT_TOOL_NAME = "auth_path";
   // FileSearchTool
   public static final String FST_TOOL_NAME      = "buscar_arquivos";
   public static final String FST_ATTR_PATTERN   = "pattern";
   public static final String FST_ATTR_RECURSIVE = "recursive";
   public static final String FST_ATTR_MAX_RES   = "max_results";
   // MSWordParagraphCounterTool
   public static final String WPC_TOOL_NAME       = "contar_paragrafos_msword";
   public static final String WPC_ATTR_INPUT_FILE = "input_file";
   // MSWordWordCounterTool
   public static final String WWC_TOOL_NAME       = "contar_palavras_msword";
   public static final String WWC_ATTR_INPUT_FILE = "input_file";
   // PdfMetadataTool
   public static final String PDFMT_TOOL_NAME       = "pdf_metadata";
   public static final String PDFMT_ATTR_INPUT_FILE = "input_file";
   // PdfSearchTool
   public static final String PDFST_TOOL_NAME           = "buscar_texto_pdf";
   public static final String PDFST_ATTR_INPUT_FILE     = "input_file";
   public static final String PDFST_ATTR_SEARCH         = "search";
   public static final String PDFST_ATTR_USE_REGEX      = "use_regex";
   public static final String PDFST_ATTR_CASE_SENSITIVE = "case_sensitive";
   // PdfSearchAndReplaceTool
   public static final String PDFSRT_TOOL_NAME        = "buscar_substituir_pdf";
   public static final String PDFSRT_ATTR_INPUT_FILE     = "input_file";
   public static final String PDFSRT_ATTR_OUTPUT_FILE    = "output_file";
   public static final String PDFSRT_ATTR_SEARCH         = "search";
   public static final String PDFSRT_ATTR_REPLACE        = "replace";
   public static final String PDFSRT_ATTR_USE_REGEX      = "use_regex";
   public static final String PDFSRT_ATTR_CASE_SENSITIVE = "case_sensitive";
   // SimplReportToolIT
   public static final String SRT_TOOL_NAME              = "obter_documento";
   public static final String SRT_ATTR_ID                = "id";
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
   public static final String DOT_ATTR_LIMITE_MENSAL = "limite_mensal";
   // VerificarOrcamentoTool
   public static final String VOT_TOOL_NAME = "verificar_orcamento";
   public static final String VOT_ATTR_MES = "mes";
   public static final String VOT_ATTR_ANO = "ano";

   // nome e atributo(s) dos prompts
   // AnalyzePdfPrompt
   public static final String ANALYZE_PDF_PROMPT        = "analisar_pdf";
   public static final String ANALYZE_PDF_ATTR_ARQUIVO  = "arquivo";
   public static final String ANALYZE_PDF_ATTR_TERMO    = "termo";
   public static final String ANALYZE_PDF_ATTR_CONTEXTO = "contexto";
   // GreetingPrompt
   public static final String GREETING_PROMPT    = "saudacao";
   public static final String GREETING_ATTR_NOME = "nome";

   // URIs dos recursos ("resources")
   // AboutResource
   public static final String RESOURCE_ABOUT_NAME = "about";
   public static final String RESOURCE_ABOUT_URI  = "app://" +
                                                    RESOURCE_ABOUT_NAME;
   // RootInfoResource
   public static final String RESOURCE_ROOT_INFO_NAME = "root-info";
   public static final String RESOURCE_ROOT_INFO_URI  = "app://" +
                                                        RESOURCE_ROOT_INFO_NAME;
}
