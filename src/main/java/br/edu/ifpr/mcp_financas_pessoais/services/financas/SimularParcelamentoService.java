package br.edu.ifpr.mcp_financas_pessoais.services.financas;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.SimulacaoParcelamento;

public class SimularParcelamentoService {
   private static final BigDecimal CEM = new BigDecimal("100");
   private static final MathContext PRECISAO = MathContext.DECIMAL128;
   private static final int CASAS_DECIMAIS = 2;

   public SimulacaoParcelamento simular(BigDecimal valor, int parcelas,
                                        BigDecimal taxaJurosMensal) {
      validar(valor, parcelas, taxaJurosMensal);

      BigDecimal valorAVista = valor.setScale(CASAS_DECIMAIS, RoundingMode.HALF_UP);
      BigDecimal taxa = taxaJurosMensal.divide(CEM, PRECISAO);
      BigDecimal valorExatoParcela = calcularParcela(valor, parcelas, taxa);
      BigDecimal totalPago = valorExatoParcela.multiply(BigDecimal.valueOf(parcelas),
            PRECISAO).setScale(CASAS_DECIMAIS, RoundingMode.HALF_UP);
      BigDecimal jurosPagos = totalPago.subtract(valorAVista)
            .setScale(CASAS_DECIMAIS, RoundingMode.HALF_UP);

      return new SimulacaoParcelamento(parcelas,
            valorExatoParcela.setScale(CASAS_DECIMAIS, RoundingMode.HALF_UP),
            totalPago, jurosPagos, valorAVista);
   }

   private static BigDecimal calcularParcela(BigDecimal valor, int parcelas,
                                             BigDecimal taxa) {
      if (taxa.signum() == 0)
         return valor.divide(BigDecimal.valueOf(parcelas), PRECISAO);

      BigDecimal fator = BigDecimal.ONE.add(taxa, PRECISAO)
            .pow(parcelas, PRECISAO);
      BigDecimal numerador = valor.multiply(taxa, PRECISAO).multiply(fator, PRECISAO);
      return numerador.divide(fator.subtract(BigDecimal.ONE, PRECISAO), PRECISAO);
   }

   private static void validar(BigDecimal valor, int parcelas,
                               BigDecimal taxaJurosMensal) {
      if (valor == null || valor.signum() <= 0)
         throw new IllegalArgumentException("O valor deve ser maior que zero.");
      if (parcelas < 1)
         throw new IllegalArgumentException(
               "O número de parcelas deve ser no mínimo 1.");
      if (taxaJurosMensal == null || taxaJurosMensal.signum() < 0)
         throw new IllegalArgumentException(
               "A taxa de juros mensal não pode ser negativa.");
   }
}
