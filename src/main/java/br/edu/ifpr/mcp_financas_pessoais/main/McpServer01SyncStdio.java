package br.edu.ifpr.mcp_financas_pessoais.main;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_financas_pessoais.mcp.prompts.AnalyzePdfPrompt;
import br.edu.ifpr.mcp_financas_pessoais.mcp.prompts.GreetingPrompt;
import br.edu.ifpr.mcp_financas_pessoais.mcp.resources.AboutResource;
import br.edu.ifpr.mcp_financas_pessoais.mcp.resources.CategoriasResource;
import br.edu.ifpr.mcp_financas_pessoais.mcp.resources.RootInfoResource;
import br.edu.ifpr.mcp_financas_pessoais.mcp.resources.ResumoMensalAtualResource;
import br.edu.ifpr.mcp_financas_pessoais.mcp.tools.SimpleReportTool;
import br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas.ListarLancamentosTool;
import br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas.DefinirOrcamentoTool;
import br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas.RegistrarGastoTool;
import br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas.RegistrarReceitaTool;
import br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas.ResumoFinanceiroTool;
import br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas.VerificarOrcamentoTool;
import br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas.SimularParcelamentoTool;
import br.edu.ifpr.mcp_financas_pessoais.mcp.tools.files.AuthPathTool;
import br.edu.ifpr.mcp_financas_pessoais.mcp.tools.files.FileSearchTool;
import br.edu.ifpr.mcp_financas_pessoais.mcp.tools.moffice.word.MSWordParagraphCounterTool;
import br.edu.ifpr.mcp_financas_pessoais.mcp.tools.moffice.word.MSWordWordCounterTool;
import br.edu.ifpr.mcp_financas_pessoais.mcp.tools.pdfs.PdfMetadataTool;
import br.edu.ifpr.mcp_financas_pessoais.mcp.tools.pdfs.PdfSearchAndReplaceTool;
import br.edu.ifpr.mcp_financas_pessoais.mcp.tools.pdfs.PdfSearchTool;
import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpPromptDefinition;
import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpResourceDefinition;
import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_financas_pessoais.services.files.FileSearchService;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.JsonLancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.JsonOrcamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.OrcamentoRepositorio;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import tools.jackson.databind.json.JsonMapper;

public class McpServer01SyncStdio {
   private static final Logger LOG =
                            LoggerFactory.getLogger(McpServer01SyncStdio.class);

   // cria a instância do serviço de busca de arquivos a partir da
   // propriedade "mcp.files.root" OU da variável de ambiente MCP_FILES_ROOT
   // a propriedade pode ser informada na execução do servidor OU a variável
   // de ambiente deve existir antes de executar o servidor
   // objeto será usado para a ferramenta de procura de arquivos e para o
   // recurso de informação do diretório usado pelo servidor
   private FileSearchService fss = FileSearchService.fromConfiguration();

   // Repositório único compartilhado pelas ferramentas financeiras.
   private final LancamentoRepositorio lancamentoRepositorio =
                                 JsonLancamentoRepositorio.fromEnvironment();

   private final OrcamentoRepositorio orcamentoRepositorio =
                                 JsonOrcamentoRepositorio.fromEnvironment();

   private McpSyncServer server;

   private void registerTools(
                          SyncSpecification<SingleSessionSyncSpecification> mcp)
                throws Exception {
      // cria a lista de ferramentas disponíveis no servidor....
      List<McpToolDefinition> tools = List.of(new SimpleReportTool(),
                                              new RegistrarGastoTool(lancamentoRepositorio),
                                              new RegistrarReceitaTool(lancamentoRepositorio),
                                              new ListarLancamentosTool(lancamentoRepositorio),
                                              new ResumoFinanceiroTool(lancamentoRepositorio),
                                              new DefinirOrcamentoTool(orcamentoRepositorio),
                                              new VerificarOrcamentoTool(lancamentoRepositorio,
                                                    orcamentoRepositorio),
                                              new SimularParcelamentoTool(),
                                              new AuthPathTool(),
                                              new FileSearchTool(fss),
                                              new PdfSearchAndReplaceTool(),
                                              new PdfSearchTool(),
                                              new PdfMetadataTool(),
                                              new MSWordWordCounterTool(),
                                              new MSWordParagraphCounterTool()
                                             );
      // ....para cada ferramenta, executa seu registro para posterior execução
      for (McpToolDefinition tool : tools)
         tool.register(mcp);
   }

   private void registerResources(
                          SyncSpecification<SingleSessionSyncSpecification> mcp)
                throws Exception {
      // cria a lista de recursos disponíveis no servidor....
      List<McpResourceDefinition> resources = List.of(new AboutResource(),
                                                      new RootInfoResource(),
                                                      new CategoriasResource(
                                                            lancamentoRepositorio,
                                                            orcamentoRepositorio),
                                                      new ResumoMensalAtualResource(
                                                            lancamentoRepositorio)
                                                     );
      // ....para cada recurso, executa seu registro para posterior recuperação
      // de informações
      for (McpResourceDefinition resource : resources)
         resource.register(mcp);
   }

   private void registerPrompts(
                          SyncSpecification<SingleSessionSyncSpecification> mcp)
                throws Exception {
      // cria a lista de prompts disponíveis no servidor....
      List<McpPromptDefinition> prompts = List.of(new GreetingPrompt(),
                                                  new AnalyzePdfPrompt());
      // ....para cada prompt, executa seu registro para posterior recuperação
      // de informações
      for (McpPromptDefinition prompt : prompts)
         prompt.register(mcp);
   }

   private SyncSpecification<SingleSessionSyncSpecification> configMcpServer() {
      // cria o objeto para comunicação entre servidor e cliente
      JacksonMcpJsonMapper         jsonMapper        =
                                     new JacksonMcpJsonMapper(new JsonMapper());
      // estabelece que a comunicação usará o transporte STDIO
      StdioServerTransportProvider transportProvider =
                                   new StdioServerTransportProvider(jsonMapper);
      // identifica quais "capacidades" o servidor disponibiliza, neste caso,
      // ferramentas e 'logging'
      /* capacidades, o que o servidor disponibiliza, do servidor
      completions  presente se o servidor suporta sugestões para auto completar
                   argumentos
      experimental capacidades não padronizadas que o servidor suporta!!!
      logging      presente se o servidor suporta envio de mensagen ('logging')
                   ao cliente
      prompts      presente se o servidor oferece modelos ('templates') de
                   promptsPresent if the server offers any prompt templates
      resources    presente se o servidor oferece recursos que podem ser lidos.
                   recursos aceitam dois parâmetros:
                   a) (des/h)abilita inscrições para notificações de atualização
                   b) (des/h)abilita notificações de alteração da lista
      tools        presente se o servidor oferece ferramentas
       */
      ServerCapabilities caps = ServerCapabilities.builder()
                                                  .tools(true)
                                                  .resources(false,false)
                                                  .prompts(true)
                                                  .logging()
                                                  .build();
      // identificação do servidor
      Implementation      impl = Implementation.builder(McpConsts.APP_NAME,
    			                                        McpConsts.APP_VERSION)
                                               .build(); 

      // configura o servidor com transporte STDIO
      return McpServer.sync(transportProvider)
                      .serverInfo(impl)
                      .capabilities(caps);
   }

   public McpServer01SyncStdio() throws Exception {
      // configura/inicializa o servidor
      SyncSpecification<SingleSessionSyncSpecification> srv = configMcpServer();
      
      // registra as ferramentas disponíveis neste servidor
      try {
         registerTools(srv);
         registerResources(srv);
         registerPrompts(srv);
         // cria o servidor
         this.server = srv.build();

         LOG.info("MCP Java Server iniciado via STDIO...");
      }
      catch (Exception ex){
         LOG.error("Erro na configuração de uma ou mais ferramentas",ex);
      }
   }

   public static void main(String[] args) throws Exception {
      new McpServer01SyncStdio();
//      McpServer01SyncStdio p = new McpServer01SyncStdio();
//
//      Runtime.getRuntime().addShutdownHook(new Thread() {
//         public void run() {
//            synchronized (p) {
//               p.notify();
//            }
//         }
//      });
//      synchronized (p) {
//         p.wait();
//         server.closeGracefully();
//      }
   }
}
