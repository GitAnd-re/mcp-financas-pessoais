package br.edu.ifpr.mcp_financas_pessoais.services.financas.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public record ResumoFinanceiro(Integer mes,
                           int ano,
                           BigDecimal totalReceitas,
                           BigDecimal totalGastos,
                           BigDecimal saldo,
                           Map<String, BigDecimal> gastosPorCategoria) {
   public ResumoFinanceiro {
      gastosPorCategoria = Collections.unmodifiableMap(
            new TreeMap<>(gastosPorCategoria));
   }
}
