package br.edu.ifpr.mcp_financas_pessoais.services.financas.model;

import java.math.BigDecimal;

public record Orcamento(String categoria,
                        int mes,
                        int ano,
                        BigDecimal limiteMensal) {
   public Orcamento(String categoria, BigDecimal limiteMensal) {
      this(categoria, 0, 0, limiteMensal);
   }
}
