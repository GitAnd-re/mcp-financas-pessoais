package br.edu.ifpr.mcp_financas_pessoais.mcp.tools.financas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

class SimularParcelamentoToolTest {
   private final SimularParcelamentoTool tool = new SimularParcelamentoTool();

   @Test
   @DisplayName("Retornar resumo do parcelamento e comparação à vista")
   void simulaParcelamento() {
      CallToolResult result = executar(argumentos(new BigDecimal("1000.00"), 12,
            new BigDecimal("2.00")));

      assertFalse(result.isError());
      assertEquals("Simulação de parcelamento:\n" +
            "parcelas:12\n" +
            "valor_parcela:94.56\n" +
            "total_pago:1134.72\n" +
            "juros_pagos:134.72\n" +
            "pagamento_a_vista:1000.00\n" +
            "economia_pagando_a_vista:134.72", texto(result));
   }

   @Test
   @DisplayName("Rejeitar valor ausente, nulo ou de tipo inválido")
   void rejeitaValorAusenteOuInvalido() {
      assertErro(argumentos(AUSENTE, 1, BigDecimal.ZERO), "valor é obrigatório");
      assertErro(argumentos(null, 1, BigDecimal.ZERO), "valor é obrigatório");
      assertErro(argumentos("100.00", 1, BigDecimal.ZERO),
            "valor deve ser informado como número");
      assertErro(argumentos(Double.NaN, 1, BigDecimal.ZERO),
            "valor informado é inválido");
   }

   @Test
   @DisplayName("Rejeitar valor zero ou negativo")
   void rejeitaValorNaoPositivo() {
      assertErro(argumentos(BigDecimal.ZERO, 1, BigDecimal.ZERO),
            "O valor deve ser maior que zero.");
      assertErro(argumentos(new BigDecimal("-0.01"), 1, BigDecimal.ZERO),
            "O valor deve ser maior que zero.");
   }

   @Test
   @DisplayName("Rejeitar parcelas ausentes ou que não sejam inteiras")
   void rejeitaParcelasAusentesOuInvalidas() {
      assertErro(argumentos(BigDecimal.ONE, AUSENTE, BigDecimal.ZERO),
            "parcelas é obrigatório");
      assertErro(argumentos(BigDecimal.ONE, null, BigDecimal.ZERO),
            "parcelas é obrigatório");
      assertErro(argumentos(BigDecimal.ONE, "3", BigDecimal.ZERO),
            "parcelas deve ser informado como número inteiro");
      assertErro(argumentos(BigDecimal.ONE, 2.5, BigDecimal.ZERO),
            "parcelas deve ser informado como número inteiro");
   }

   @Test
   @DisplayName("Rejeitar quantidade de parcelas menor que um")
   void rejeitaParcelasNaoPositivas() {
      assertErro(argumentos(BigDecimal.ONE, 0, BigDecimal.ZERO),
            "O número de parcelas deve ser no mínimo 1.");
      assertErro(argumentos(BigDecimal.ONE, -1, BigDecimal.ZERO),
            "O número de parcelas deve ser no mínimo 1.");
   }

   @Test
   @DisplayName("Rejeitar taxa ausente, de tipo inválido ou negativa")
   void rejeitaTaxaInvalida() {
      assertErro(argumentos(BigDecimal.ONE, 1, AUSENTE),
            "taxa de juros mensal é obrigatório");
      assertErro(argumentos(BigDecimal.ONE, 1, null),
            "taxa de juros mensal é obrigatório");
      assertErro(argumentos(BigDecimal.ONE, 1, "2.5"),
            "taxa de juros mensal deve ser informado como número");
      assertErro(argumentos(BigDecimal.ONE, 1, new BigDecimal("-0.01")),
            "A taxa de juros mensal não pode ser negativa.");
   }

   private void assertErro(Map<String, Object> args, String mensagem) {
      CallToolResult result = executar(args);
      assertTrue(result.isError());
      assertTrue(texto(result).contains(mensagem), texto(result));
   }

   private CallToolResult executar(Map<String, Object> args) {
      return tool.run(null, CallToolRequest.builder(McpConsts.SPT_TOOL_NAME)
            .arguments(args).build());
   }

   private static String texto(CallToolResult result) {
      return ((TextContent) result.content().get(0)).text();
   }

   private static Map<String, Object> argumentos(Object valor, Object parcelas,
                                                 Object taxa) {
      Map<String, Object> args = new HashMap<>();
      if (valor != AUSENTE)
         args.put(McpConsts.SPT_ATTR_VALOR, valor);
      if (parcelas != AUSENTE)
         args.put(McpConsts.SPT_ATTR_PARCELAS, parcelas);
      if (taxa != AUSENTE)
         args.put(McpConsts.SPT_ATTR_TAXA_JUROS_MENSAL, taxa);
      return args;
   }

   private static final Object AUSENTE = new Object();
}
