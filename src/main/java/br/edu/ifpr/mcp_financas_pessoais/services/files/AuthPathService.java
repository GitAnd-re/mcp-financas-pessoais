package br.edu.ifpr.mcp_financas_pessoais.services.files;

import java.io.File;
import java.nio.file.Path;

public class AuthPathService {
   private static final String DEFAULT_DIRECTORY = ".mcp-financas-pessoais";

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
         configuredRoot = Path.of(System.getProperty("user.home"),
                                  DEFAULT_DIRECTORY).toString();

      if (configuredRoot.charAt(configuredRoot.length() - 1) !=
    			                                                 File.separatorChar)
         configuredRoot = configuredRoot + File.separator;

      return configuredRoot;
   }
}
