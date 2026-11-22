package br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryLancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

class RegistrarGastoToolTest {
   private InMemoryLancamentoRepositorio repositorio;
   private RegistrarGastoTool tool;

   @BeforeEach
   void setUp() {
      repositorio = new InMemoryLancamentoRepositorio();
      tool = new RegistrarGastoTool(repositorio);
   }

   @Test
   @DisplayName("Registra gasto sem incluir identificador na resposta")
   void registraGastoComSucesso() {
      CallToolResult result = executar(argumentos(new BigDecimal("25.90"),
            "Alimentação", "20/11/2026", "Almoço"));

      assertFalse(result.isError());
      String text = texto(result);
      assertTrue(text.startsWith("tipo:GASTO, valor:25.90"));
      assertTrue(text.contains(", valor:25.90"));
      assertTrue(text.contains(", categoria:Alimentação"));
      assertTrue(text.contains(", data:20/11/2026"));
      assertTrue(text.contains(", descricao:Almoço"));
      assertEquals(1, repositorio.listar().size());
      assertEquals(Lancamento.TipoLancamento.GASTO,
                   repositorio.listar().get(0).tipo());
   }

   @Test
   @DisplayName("Preserva caracteres especiais na resposta de texto")
   void preservaCaracteresEspeciaisNaResposta() {
      CallToolResult result = executar(argumentos(new BigDecimal("3.50"),
            "Categoria \"especial\"", "20/11/2026", "linha 1\nlinha 2\\fim"));

      assertFalse(result.isError());
      String response = texto(result);
      assertTrue(response.contains(", categoria:Categoria \"especial\""));
      assertTrue(response.contains(", descricao:linha 1\nlinha 2\\fim"));
   }

   @Test
   @DisplayName("Retorna diretamente validação de negócio do service")
   void retornaValidacaoDoServiceSemPrefixoDePersistencia() {
      CallToolResult result = executar(argumentos(BigDecimal.ZERO,
            "Mercado", "20/11/2026", null));

      assertTrue(result.isError());
      assertEquals("O valor deve ser maior que zero.", texto(result));
   }

   @ParameterizedTest(name = "Rejeita valor inválido: {0}")
   @MethodSource("valoresInvalidos")
   void rejeitaValorInvalido(Object valor) {
      Map<String, Object> args = argumentos("Mercado", "20/11/2026");
      if (valor == AUSENTE)
         args.remove("valor");
      else
         args.put("valor", valor);

      assertErro(args, "valor");
      assertTrue(repositorio.listar().isEmpty());
   }

   static Stream<Object> valoresInvalidos() {
      return Stream.of("10.00", null, AUSENTE);
   }

   @ParameterizedTest(name = "Rejeita categoria inválida: {0}")
   @MethodSource("categoriasInvalidas")
   void rejeitaCategoriaInvalida(Object categoria) {
      Map<String, Object> args = argumentos(new BigDecimal("10.00"),
                                             categoria, "20/11/2026", null);
      assertErro(args, "categoria");
   }

   static Stream<Object> categoriasInvalidas() {
      return Stream.of(12, null, AUSENTE);
   }

   @ParameterizedTest(name = "Rejeita data inválida: {0}")
   @MethodSource("datasInvalidas")
   void rejeitaDataInvalida(Object data) {
      Map<String, Object> args = argumentos(new BigDecimal("10.00"), "Mercado", data, null);
      assertErro(args, "data");
   }

   @Test
   @DisplayName("Interrompe na data inválida antes de ler descrição")
   void interrompeNaDataInvalidaAntesDeLerDescricao() {
      Map<String, Object> args = argumentos(new BigDecimal("10.00"),
            "Mercado", "31/02/2026", 123);

      CallToolResult result = executar(args);

      assertTrue(result.isError());
      assertTrue(texto(result).contains("DD/MM/AAAA"));
      assertTrue(repositorio.listar().isEmpty());
   }

   static Stream<Object> datasInvalidas() {
      return Stream.of("", "31/02/2026", "01/13/2026", "20-11-2026",
                       "2026-11-20", 20261120,
                       null, AUSENTE);
   }

   @Test
   @DisplayName("Rejeita descrição que não seja texto")
   void rejeitaDescricaoComTipoInvalido() {
      Map<String, Object> args = argumentos(new BigDecimal("10.00"),
            "Mercado", "20/11/2026", 12);
      assertErro(args, "descrição");
   }

   @Test
   @DisplayName("Retorna erro quando o repositório falha ao salvar")
   void retornaErroQuandoPersistenciaFalha() {
      LancamentoRepositorio comFalha = new LancamentoRepositorio() {
         @Override
         public void salvar(Lancamento lancamento) throws java.io.IOException {
            throw new java.io.IOException("disco indisponível");
         }

         @Override
         public List<Lancamento> listar() {
            return List.of();
         }
      };
      RegistrarGastoTool failingTool = new RegistrarGastoTool(comFalha);
      CallToolResult result = failingTool.run(null, request(argumentos(
            new BigDecimal("10.00"), "Mercado", "20/11/2026", null)));

      assertTrue(result.isError());
      assertTrue(texto(result).contains("disco indisponível"));
   }

   private void assertErro(Map<String, Object> args, String trecho) {
      CallToolResult result = executar(args);
      assertNotNull(result);
      assertTrue(result.isError());
      assertTrue(texto(result).toLowerCase().contains(trecho.toLowerCase()),
                 texto(result));
   }

   private CallToolResult executar(Map<String, Object> args) {
      return tool.run(null, request(args));
   }

   private static CallToolRequest request(Map<String, Object> args) {
      return CallToolRequest.builder(McpConsts.RGT_TOOL_NAME)
                            .arguments(args)
                            .build();
   }

   private static String texto(CallToolResult result) {
      return ((TextContent) result.content().get(0)).text();
   }

   private static Map<String, Object> argumentos(Object valor,
                                                 Object categoria,
                                                 Object data,
                                                 Object descricao) {
      Map<String, Object> args = new HashMap<>();
      if (valor != AUSENTE) args.put("valor", valor);
      if (categoria != AUSENTE) args.put("categoria", categoria);
      if (data != AUSENTE) args.put("data", data);
      if (descricao != AUSENTE && descricao != null) args.put("descricao", descricao);
      return args;
   }

   private static Map<String, Object> argumentos(Object categoria, Object data) {
      Map<String, Object> args = new HashMap<>();
      args.put("valor", new BigDecimal("10.00"));
      if (categoria != AUSENTE) args.put("categoria", categoria);
      if (data != AUSENTE) args.put("data", data);
      return args;
   }

   private static final Object AUSENTE = new Object();
}
