package br.edu.ifpr.mcp_financas_pessoais.services.files;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuthPathServiceTest {
   private static final String DEFAULT_DIRECTORY = ".mcp-financas-pessoais";

   @TempDir
   Path tempDir;

   @Test
   void usaPastaPadraoNoDiretorioPessoalQuandoConfiguracoesAusentes() {
      String path = AuthPathService.resolveAuthPath(null, null,
            tempDir.toString());

      assertEquals(tempDir.resolve(DEFAULT_DIRECTORY).toString() + File.separator,
            path);
   }

   @Test
   void usaPastaPadraoQuandoConfiguracoesEstaoEmBranco() {
      String path = AuthPathService.resolveAuthPath(" ", "\t",
            tempDir.toString());

      assertEquals(tempDir.resolve(DEFAULT_DIRECTORY).toString() + File.separator,
            path);
   }

   @Test
   void priorizaPropriedadeSobreVariavelDeAmbiente() {
      Path propertyPath = tempDir.resolve("configurado-na-propriedade");
      Path environmentPath = tempDir.resolve("configurado-no-ambiente");

      String path = AuthPathService.resolveAuthPath(propertyPath.toString(),
            environmentPath.toString(), tempDir.toString());

      assertEquals(propertyPath.toString() + File.separator, path);
   }

   @Test
   void usaVariavelDeAmbienteQuandoPropriedadeAusente() {
      Path environmentPath = tempDir.resolve("configurado-no-ambiente");

      String path = AuthPathService.resolveAuthPath(null,
            environmentPath.toString(), tempDir.toString());

      assertEquals(environmentPath.toString() + File.separator, path);
   }
}
