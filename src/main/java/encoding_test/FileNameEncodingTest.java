package encoding_test;

import java.nio.file.Files;
import java.nio.file.Path;

public class FileNameEncodingTest {
   public static void main(String[] args) throws Exception {
      Path root = Path.of("/opt/mcp/files");

      System.out.println("  file.encoding: " +
                         System.getProperty("file.encoding"));
      System.out.println("native.encoding: " +
                        System.getProperty("native.encoding"));

      System.out.println("\nArquivos encontrados:");
      try (var paths = Files.walk(root,1)) {
         paths.forEach(path -> { System.out.println(path.getFileName()); });
      }
   }
}
