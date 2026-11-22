package br.edu.ifpr.mcp_financas_pessoais.mcp.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.ResumoFinanceiro;

public final class ResumoFinanceiroFormatter {
   private ResumoFinanceiroFormatter() { }

   public static String formatar(ResumoFinanceiro resumo) {
      String periodo = resumo.mes() == null
            ? String.valueOf(resumo.ano())
            : String.format("%02d/%04d", resumo.mes(), resumo.ano());
      StringBuilder response = new StringBuilder("Resumo financeiro de ")
            .append(periodo)
            .append(":\ntotal_receitas:")
            .append(formatarValor(resumo.totalReceitas()))
            .append("\ntotal_gastos:")
            .append(formatarValor(resumo.totalGastos()))
            .append("\nsaldo:")
            .append(formatarValor(resumo.saldo()))
            .append("\n\ngastos_por_categoria:");

      if (resumo.gastosPorCategoria().isEmpty())
         response.append("\nNenhum gasto no período.");
      else
         resumo.gastosPorCategoria().forEach((categoria, valor) ->
               response.append("\n").append(categoria).append(":")
                     .append(formatarValor(valor)));

      return response.toString();
   }

   private static String formatarValor(BigDecimal valor) {
      return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
   }
}
