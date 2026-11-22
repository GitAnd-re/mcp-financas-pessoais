package br.edu.ifpr.mcp_financas_pessoais.services.financas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.SimulacaoParcelamento;

class SimularParcelamentoServiceTest {
   private final SimularParcelamentoService service = new SimularParcelamentoService();

   @Test
   @DisplayName("Simular parcelas fixas com juros mensais pelo sistema Price")
   void simulaComJuros() {
      SimulacaoParcelamento resultado = service.simular(
            new BigDecimal("1000.00"), 12, new BigDecimal("2.00"));

      assertEquals(12, resultado.parcelas());
      assertEquals(new BigDecimal("94.56"), resultado.valorParcela());
      assertEquals(new BigDecimal("1134.72"), resultado.totalPago());
      assertEquals(new BigDecimal("134.72"), resultado.jurosPagos());
      assertEquals(new BigDecimal("1000.00"), resultado.valorAVista());
   }

   @Test
   @DisplayName("Simular parcelamento sem juros")
   void simulaSemJuros() {
      SimulacaoParcelamento resultado = service.simular(
            new BigDecimal("1200.00"), 12, BigDecimal.ZERO);

      assertEquals(new BigDecimal("100.00"), resultado.valorParcela());
      assertEquals(new BigDecimal("1200.00"), resultado.totalPago());
      assertEquals(new BigDecimal("0.00"), resultado.jurosPagos());
      assertEquals(new BigDecimal("1200.00"), resultado.valorAVista());
   }

   @Test
   @DisplayName("Calcular uma parcela com juros")
   void simulaUmaParcela() {
      SimulacaoParcelamento resultado = service.simular(
            new BigDecimal("100.00"), 1, new BigDecimal("10.00"));

      assertEquals(new BigDecimal("110.00"), resultado.valorParcela());
      assertEquals(new BigDecimal("110.00"), resultado.totalPago());
      assertEquals(new BigDecimal("10.00"), resultado.jurosPagos());
   }

   @Test
   @DisplayName("Aceitar valor mínimo positivo, uma parcela e taxa zero")
   void aceitaLimitesMinimos() {
      SimulacaoParcelamento resultado = service.simular(
            new BigDecimal("0.01"), 1, BigDecimal.ZERO);

      assertEquals(new BigDecimal("0.01"), resultado.valorParcela());
      assertEquals(new BigDecimal("0.01"), resultado.totalPago());
      assertEquals(new BigDecimal("0.00"), resultado.jurosPagos());
   }

   @Test
   @DisplayName("Rejeitar valor ausente, zero ou negativo")
   void rejeitaValorInvalido() {
      for (BigDecimal valor : new BigDecimal[] { null, BigDecimal.ZERO,
            new BigDecimal("-0.01") }) {
         IllegalArgumentException exception = assertThrows(
               IllegalArgumentException.class,
               () -> service.simular(valor, 1, BigDecimal.ZERO));
         assertEquals("O valor deve ser maior que zero.", exception.getMessage());
      }
   }

   @Test
   @DisplayName("Rejeitar quantidade de parcelas menor que um")
   void rejeitaParcelasInvalidas() {
      for (int parcelas : new int[] { 0, -1 }) {
         IllegalArgumentException exception = assertThrows(
               IllegalArgumentException.class,
               () -> service.simular(BigDecimal.ONE, parcelas, BigDecimal.ZERO));
         assertEquals("O número de parcelas deve ser no mínimo 1.",
               exception.getMessage());
      }
   }

   @Test
   @DisplayName("Rejeitar taxa de juros ausente ou negativa")
   void rejeitaTaxaInvalida() {
      for (BigDecimal taxa : new BigDecimal[] { null, new BigDecimal("-0.01") }) {
         IllegalArgumentException exception = assertThrows(
               IllegalArgumentException.class,
               () -> service.simular(BigDecimal.ONE, 1, taxa));
         assertEquals("A taxa de juros mensal não pode ser negativa.",
               exception.getMessage());
      }
   }
}
