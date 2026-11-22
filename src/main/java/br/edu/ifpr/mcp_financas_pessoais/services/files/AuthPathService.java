package br.edu.ifpr.mcp_financas_pessoais.services.files;

import java.io.File;

public class AuthPathService {
   /**
    * Obtém o diretório configurado para os arquivos JSON financeiros.
    * @return
    */
   public static String authPath() {
      String propertyRoot    = System.getProperty("mcp.files.root");
      String environmentRoot = System.getenv("MCP_FILES_ROOT");
      String configuredRoot  = propertyRoot;

      if (configuredRoot == null || configuredRoot.trim().isEmpty())
         configuredRoot = environmentRoot;

      if (configuredRoot == null || configuredRoot.trim().isEmpty())
         throw new IllegalStateException("Configure a variável de ambiente " +
                                         "'MCP_FILES_ROOT' ou a propriedade " +
                                         "'mcp.files.root'.");

      if (configuredRoot.charAt(configuredRoot.length() - 1) !=
    			                                                 File.separatorChar)
         configuredRoot = configuredRoot + File.separator;

      return configuredRoot;
   }
}
