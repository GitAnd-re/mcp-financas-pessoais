package br.edu.ifpr.mcp_financas_pessoais.services.files;

import java.io.File;
import java.nio.file.Path;

public class AuthPathService {
   private static final String DEFAULT_DIRECTORY = ".mcp-financas-pessoais";

   /**
    * Obtém o diretório autorizado para os arquivos JSON financeiros, usando a
    * pasta padrão dentro do diretório pessoal quando não houver configuração.
    * @return diretório terminado pelo separador de arquivos do sistema
    */
   public static String authPath() {
      return resolveAuthPath(System.getProperty("mcp.files.root"),
            System.getenv("MCP_FILES_ROOT"), System.getProperty("user.home"));
   }

   static String resolveAuthPath(String propertyRoot, String environmentRoot,
                                 String userHome) {
      String configuredRoot  = propertyRoot;

      if (configuredRoot == null || configuredRoot.trim().isEmpty())
         configuredRoot = environmentRoot;

      if (configuredRoot == null || configuredRoot.trim().isEmpty())
         configuredRoot = Path.of(userHome, DEFAULT_DIRECTORY).toString();

      if (configuredRoot.charAt(configuredRoot.length() - 1) !=
    			                                                 File.separatorChar)
         configuredRoot = configuredRoot + File.separator;

      return configuredRoot;
   }
}
