package br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
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
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

class RegistrarReceitaToolTest {
   private InMemoryLancamentoRepositorio repositorio;
   private RegistrarReceitaTool tool;

   @BeforeEach
   void setUp() {
      repositorio = new InMemoryLancamentoRepositorio();
      tool = new RegistrarReceitaTool(repositorio);
   }

   @Test
   @DisplayName("Registrar receita e retornar identificador")
   void registraReceitaComSucesso() {
      CallToolResult result = executar(argumentos(new BigDecimal("25.90"),
            "Salário", "20/11/2026"));

      assertFalse(result.isError());
      String text = texto(result);
      assertTrue(text.startsWith("id:"));
      assertTrue(text.contains(", tipo:RECEITA"));
      assertTrue(text.contains(", valor:25.90"));
      assertTrue(text.contains(", fonte:Salário"));
      assertTrue(text.contains(", data:20/11/2026"));
      assertEquals(1, repositorio.listar().size());
      Lancamento salvo = repositorio.listar().get(0);
      assertEquals(Lancamento.TipoLancamento.RECEITA, salvo.tipo());
      assertEquals("Salário", salvo.fonte());
      assertNull(salvo.categoria());
   }

   @Test
   @DisplayName("Aceitar o menor valor monetário positivo comum")
   void aceitaUmCentavo() {
      CallToolResult result = executar(argumentos(new BigDecimal("0.01"),
            "Reembolso", "29/02/2024"));

      assertFalse(result.isError());
      assertEquals(new BigDecimal("0.01"), repositorio.listar().get(0).valor());
   }

   @Test
   @DisplayName("Retornar diretamente as mensagens de validação do serviço")
   void retornaMensagensDeValidacaoDoService() {
      CallToolResult valorInvalido = executar(argumentos(BigDecimal.ZERO,
            "Salário", "20/11/2026"));
      assertTrue(valorInvalido.isError());
      assertEquals("O valor deve ser maior que zero.", texto(valorInvalido));
   }

   @ParameterizedTest(name = "Rejeita valor inválido: {0}")
   @MethodSource("valoresInvalidos")
   void rejeitaValorInvalido(Object valor) {
      Map<String, Object> args = argumentos(new BigDecimal("10.00"),
            "Salário", "20/11/2026");
      if (valor == AUSENTE)
         args.remove(McpConsts.RRT_ATTR_VALOR);
      else
         args.put(McpConsts.RRT_ATTR_VALOR, valor);

      assertErro(args, "valor");
      assertTrue(repositorio.listar().isEmpty());
   }

   static Stream<Object> valoresInvalidos() {
      return Stream.of("10.00", null, AUSENTE);
   }

   @ParameterizedTest(name = "Rejeita fonte inválida: {0}")
   @MethodSource("fontesInvalidas")
   void rejeitaFonteInvalida(Object fonte) {
      Map<String, Object> args = argumentos(new BigDecimal("10.00"),
            "Salário", "20/11/2026");
      if (fonte == AUSENTE)
         args.remove(McpConsts.RRT_ATTR_FONTE);
      else
         args.put(McpConsts.RRT_ATTR_FONTE, fonte);

      assertErro(args, "fonte");
   }

   static Stream<Object> fontesInvalidas() {
      return Stream.of(12, null, AUSENTE);
   }

   @ParameterizedTest(name = "Rejeita data inválida: {0}")
   @MethodSource("datasInvalidas")
   void rejeitaDataInvalida(Object data) {
      Map<String, Object> args = argumentos(new BigDecimal("10.00"),
            "Salário", "20/11/2026");
      if (data == AUSENTE)
         args.remove(McpConsts.RRT_ATTR_DATA);
      else
         args.put(McpConsts.RRT_ATTR_DATA, data);

      assertErro(args, "data");
      assertTrue(repositorio.listar().isEmpty());
   }

   static Stream<Object> datasInvalidas() {
      return Stream.of("", "31/02/2026", "01/13/2026", "20-11-2026",
            "2026-11-20", 20261120, null, AUSENTE);
   }

   @Test
   @DisplayName("Retornar erro quando o repositório falha ao salvar")
   void retornaErroQuandoPersistenciaFalha() {
      LancamentoRepositorio comFalha = new LancamentoRepositorio() {
         @Override
         public void salvar(Lancamento lancamento) throws IOException {
            throw new IOException("disco indisponível");
         }

         @Override
         public List<Lancamento> listar() {
            return List.of();
         }
      };
      RegistrarReceitaTool failingTool = new RegistrarReceitaTool(comFalha);
      CallToolResult result = failingTool.run(null, request(argumentos(
            new BigDecimal("10.00"), "Salário", "20/11/2026")));

      assertTrue(result.isError());
      assertTrue(texto(result).contains("disco indisponível"));
   }

   private void assertErro(Map<String, Object> args, String trecho) {
      CallToolResult result = executar(args);
      assertNotNull(result);
      assertTrue(result.isError());
      assertTrue(texto(result).toLowerCase().contains(trecho.toLowerCase()), texto(result));
   }

   private CallToolResult executar(Map<String, Object> args) {
      return tool.run(null, request(args));
   }

   private static CallToolRequest request(Map<String, Object> args) {
      return CallToolRequest.builder(McpConsts.RRT_TOOL_NAME)
                            .arguments(args)
                            .build();
   }

   private static String texto(CallToolResult result) {
      return ((TextContent) result.content().get(0)).text();
   }

   private static Map<String, Object> argumentos(Object valor,
                                                 Object fonte,
                                                 Object data) {
      Map<String, Object> args = new HashMap<>();
      if (valor != AUSENTE) args.put(McpConsts.RRT_ATTR_VALOR, valor);
      if (fonte != AUSENTE) args.put(McpConsts.RRT_ATTR_FONTE, fonte);
      if (data != AUSENTE) args.put(McpConsts.RRT_ATTR_DATA, data);
      return args;
   }

   private static final Object AUSENTE = new Object();
}
