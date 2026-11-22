package br.edu.ifpr.mcp_financas_pessoais.services.financas;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Orcamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.SituacaoOrcamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.OrcamentoRepositorio;

public class VerificarOrcamentoService {
   private final LancamentoRepositorio lancamentoRepositorio;
   private final OrcamentoRepositorio orcamentoRepositorio;

   public VerificarOrcamentoService(LancamentoRepositorio lancamentoRepositorio,
                                    OrcamentoRepositorio orcamentoRepositorio) {
      if (lancamentoRepositorio == null)
         throw new IllegalArgumentException("O repositório de lançamentos é obrigatório.");
      if (orcamentoRepositorio == null)
         throw new IllegalArgumentException("O repositório de orçamentos é obrigatório.");
      this.lancamentoRepositorio = lancamentoRepositorio;
      this.orcamentoRepositorio = orcamentoRepositorio;
   }

   public List<SituacaoOrcamento> verificar(int mes, int ano) throws IOException {
      validarPeriodo(mes, ano);
      List<Orcamento> todosOrcamentos = orcamentoRepositorio.listar();
      if (todosOrcamentos.isEmpty())
         throw new IllegalArgumentException("Nenhum orçamento foi definido.");
      List<Orcamento> orcamentos = todosOrcamentos.stream()
            .filter(orcamento -> orcamento.mes() == mes && orcamento.ano() == ano)
            .toList();
      if (orcamentos.isEmpty())
         throw new IllegalArgumentException("Nenhum orçamento foi definido para " +
               String.format("%02d/%04d", mes, ano) + ".");

      Map<String, BigDecimal> gastosPorCategoria = somarGastos(mes, ano);
      List<SituacaoOrcamento> situacoes = new ArrayList<>();
      for (Orcamento orcamento : orcamentos) {
         BigDecimal gasto = gastosPorCategoria.getOrDefault(orcamento.categoria(),
               BigDecimal.ZERO);
         BigDecimal restante = orcamento.limiteMensal().subtract(gasto);
         situacoes.add(new SituacaoOrcamento(orcamento.categoria(), gasto,
               orcamento.limiteMensal(), restante, gasto.compareTo(
                     orcamento.limiteMensal()) > 0));
      }
      situacoes.sort((a, b) -> a.categoria().compareTo(b.categoria()));
      return List.copyOf(situacoes);
   }

   private Map<String, BigDecimal> somarGastos(int mes, int ano) throws IOException {
      Map<String, BigDecimal> gastos = new TreeMap<>();
      for (Lancamento lancamento : lancamentoRepositorio.listar()) {
         if (lancamento.tipo() != Lancamento.TipoLancamento.GASTO ||
               lancamento.data().getMonthValue() != mes ||
               lancamento.data().getYear() != ano ||
               lancamento.categoria() == null)
            continue;
         gastos.merge(lancamento.categoria().trim(), lancamento.valor(), BigDecimal::add);
      }
      return gastos;
   }

   private static void validarPeriodo(int mes, int ano) {
      if (mes < 1 || mes > 12)
         throw new IllegalArgumentException("O mês deve estar entre 1 e 12.");
      if (ano < 1)
         throw new IllegalArgumentException("O ano deve ser maior que zero.");
   }
}
