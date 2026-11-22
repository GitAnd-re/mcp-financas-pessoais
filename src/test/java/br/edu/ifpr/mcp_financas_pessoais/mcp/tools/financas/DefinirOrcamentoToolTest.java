package br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Orcamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryOrcamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.OrcamentoRepositorio;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

class DefinirOrcamentoToolTest {
   private InMemoryOrcamentoRepositorio repositorio;
   private DefinirOrcamentoTool tool;

   @BeforeEach
   void setUp() {
      repositorio = new InMemoryOrcamentoRepositorio();
      tool = new DefinirOrcamentoTool(repositorio);
   }

   @Test
   @DisplayName("Definir orçamento e retornar texto com categoria, período e limite")
   void defineOrcamento() {
      CallToolResult result = executar(argumentos("Alimentação",
            new BigDecimal("500.00")));
      assertFalse(result.isError());
      assertEquals("categoria:Alimentação, mes:11, ano:2026, limite_mensal:500.00",
            texto(result));
      assertEquals(1, repositorio.listar().size());
   }

   @Test
   @DisplayName("Rejeitar categoria ausente ou nula")
   void rejeitaCategoriaAusente() {
      assertErro(argumentos(AUSENTE, new BigDecimal("500.00")), "obrigatório");
      assertErro(argumentos(null, new BigDecimal("500.00")), "obrigatório");
   }

   @Test
   @DisplayName("Rejeitar categoria que não seja texto")
   void rejeitaCategoriaQueNaoSejaTexto() {
      assertErro(argumentos(12, new BigDecimal("500.00")), "deve ser texto");
   }

   @Test
   @DisplayName("Rejeitar categoria vazia ou composta por espaços")
   void rejeitaCategoriaVazia() {
      assertErro(argumentos("", new BigDecimal("500.00")), "não pode ser vazia");
      assertErro(argumentos("   ", new BigDecimal("500.00")), "não pode ser vazia");
   }

   @Test
   @DisplayName("Rejeitar categoria numérica")
   void rejeitaCategoriaNumerica() {
      assertErro(argumentos("123", new BigDecimal("500.00")),
            "A categoria não pode ser um número.");
   }

   @Test
   @DisplayName("Rejeitar limite ausente ou nulo")
   void rejeitaLimiteAusente() {
      assertErro(argumentos("Mercado", AUSENTE), "obrigatório");
      assertErro(argumentos("Mercado", null), "obrigatório");
   }

   @Test
   @DisplayName("Rejeitar limite que não seja número")
   void rejeitaLimiteQueNaoSejaNumero() {
      assertErro(argumentos("Mercado", "100.00"), "como número");
   }

   @Test
   @DisplayName("Rejeitar limite numérico inválido")
   void rejeitaLimiteNumericoInvalido() {
      assertErro(argumentos("Mercado", Double.NaN), "informado é inválido");
   }

   @Test
   @DisplayName("Rejeitar limite zero ou negativo")
   void rejeitaLimiteNaoPositivo() {
      assertErro(argumentos("Mercado", BigDecimal.ZERO), "maior que zero");
      assertErro(argumentos("Mercado", new BigDecimal("-0.01")), "maior que zero");
   }

   @Test
   @DisplayName("Rejeitar mês ausente ou que não seja inteiro")
   void rejeitaMesInvalido() {
      Map<String, Object> ausente = argumentos("Mercado", BigDecimal.ONE);
      ausente.remove(McpConsts.DOT_ATTR_MES);
      assertErro(ausente, "mês é obrigatório");
      assertErro(argumentosComPeriodo("Mercado", BigDecimal.ONE, "11", 2026),
            "mês deve ser informado como número inteiro");
      assertErro(argumentosComPeriodo("Mercado", BigDecimal.ONE, 11.5, 2026),
            "mês deve ser informado como número inteiro");
   }

   @Test
   @DisplayName("Rejeitar ano ausente ou que não seja inteiro")
   void rejeitaAnoInvalido() {
      Map<String, Object> ausente = argumentos("Mercado", BigDecimal.ONE);
      ausente.remove(McpConsts.DOT_ATTR_ANO);
      assertErro(ausente, "ano é obrigatório");
      assertErro(argumentosComPeriodo("Mercado", BigDecimal.ONE, 11, "2026"),
            "ano deve ser informado como número inteiro");
      assertErro(argumentosComPeriodo("Mercado", BigDecimal.ONE, 11, 2026.5),
            "ano deve ser informado como número inteiro");
   }

   @Test
   @DisplayName("Retornar erro se a persistência falhar")
   void retornaErroQuandoPersistenciaFalha() {
      OrcamentoRepositorio comFalha = new OrcamentoRepositorio() {
               @Override
               public void salvar(Orcamento orcamento) throws IOException {
                  throw new IOException("disco indisponível");
               }

               @Override
               public List<Orcamento> listar() {
                  return List.of();
               }
            };
      CallToolResult result = new DefinirOrcamentoTool(comFalha).run(null,
            request(argumentos("Mercado", BigDecimal.ONE)));
      assertTrue(result.isError());
      assertTrue(texto(result).contains("disco indisponível"));
   }

   private void assertErro(Map<String, Object> args, String mensagem) {
      CallToolResult result = executar(args);
      assertTrue(result.isError());
      assertTrue(texto(result).contains(mensagem), texto(result));
      assertTrue(repositorio.listar().isEmpty());
   }

   private CallToolResult executar(Map<String, Object> args) {
      return tool.run(null, request(args));
   }

   private static CallToolRequest request(Map<String, Object> args) {
      return CallToolRequest.builder(McpConsts.DOT_TOOL_NAME).arguments(args).build();
   }

   private static String texto(CallToolResult result) {
      return ((TextContent) result.content().get(0)).text();
   }

   private static Map<String, Object> argumentos(Object categoria, Object limite) {
      return argumentosComPeriodo(categoria, limite, 11, 2026);
   }

   private static Map<String, Object> argumentosComPeriodo(Object categoria,
         Object limite, Object mes, Object ano) {
      Map<String, Object> args = new HashMap<>();
      if (categoria != AUSENTE)
         args.put(McpConsts.DOT_ATTR_CATEGORIA, categoria);
      if (mes != AUSENTE)
         args.put(McpConsts.DOT_ATTR_MES, mes);
      if (ano != AUSENTE)
         args.put(McpConsts.DOT_ATTR_ANO, ano);
      if (limite != AUSENTE)
         args.put(McpConsts.DOT_ATTR_LIMITE_MENSAL, limite);
      return args;
   }

   private static final Object AUSENTE = new Object();
}
