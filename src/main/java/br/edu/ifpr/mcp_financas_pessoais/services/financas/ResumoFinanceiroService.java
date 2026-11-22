package br.edu.ifpr.mcp_financas_pessoais.services.financas;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.ResumoFinanceiro;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;

public class ResumoFinanceiroService {
   private static final String CATEGORIA_SEM_NOME = "Sem categoria";

   private final LancamentoRepositorio repositorio;

   public ResumoFinanceiroService(LancamentoRepositorio repositorio) {
      if (repositorio == null)
         throw new IllegalArgumentException("O repositório de lançamentos é obrigatório.");
      this.repositorio = repositorio;
   }

   public ResumoFinanceiro resumir(Integer mes, int ano) throws IOException {
      validarPeriodo(mes, ano);

      BigDecimal totalReceitas = BigDecimal.ZERO;
      BigDecimal totalGastos = BigDecimal.ZERO;
      Map<String, BigDecimal> gastosPorCategoria = new TreeMap<>();

      for (Lancamento lancamento : repositorio.listar()) {
         if (!pertenceAoPeriodo(lancamento, mes, ano))
            continue;

         if (lancamento.tipo() == Lancamento.TipoLancamento.RECEITA) {
            totalReceitas = totalReceitas.add(lancamento.valor());
         }
         else if (lancamento.tipo() == Lancamento.TipoLancamento.GASTO) {
            totalGastos = totalGastos.add(lancamento.valor());
            somarCategoria(gastosPorCategoria, lancamento);
         }
      }

      return new ResumoFinanceiro(mes, ano, totalReceitas, totalGastos,
            totalReceitas.subtract(totalGastos), gastosPorCategoria);
   }

   private static void validarPeriodo(Integer mes, int ano) {
      if (mes != null && (mes < 1 || mes > 12))
         throw new IllegalArgumentException("O mês deve estar entre 1 e 12.");
      if (ano < 1)
         throw new IllegalArgumentException("O ano deve ser maior que zero.");
   }

   private static boolean pertenceAoPeriodo(Lancamento lancamento,
                                             Integer mes, int ano) {
      return (mes == null || lancamento.data().getMonthValue() == mes) &&
            lancamento.data().getYear() == ano;
   }

   private static void somarCategoria(Map<String, BigDecimal> gastosPorCategoria,
                                      Lancamento lancamento) {
      String categoria = lancamento.categoria();
      if (categoria == null || categoria.isBlank())
         categoria = CATEGORIA_SEM_NOME;
      else
         categoria = categoria.trim();

      gastosPorCategoria.merge(categoria, lancamento.valor(), BigDecimal::add);
   }
}
