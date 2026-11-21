package br.edu.ifpr.mcp_financas_pessoais.services.financas.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Lancamento(String id,
                         TipoLancamento tipo,
                         BigDecimal valor,
                         String categoria,
                         String fonte,
                         LocalDate data,
                         String descricao) {
   public enum TipoLancamento { GASTO, RECEITA }
}
