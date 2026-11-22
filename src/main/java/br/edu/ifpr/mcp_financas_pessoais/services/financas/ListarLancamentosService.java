package br.edu.ifpr.mcp_financas_pessoais.services.financas;

import java.io.IOException;
import java.util.List;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;

public class ListarLancamentosService {
   private final LancamentoRepositorio repositorio;

   public ListarLancamentosService(LancamentoRepositorio repositorio) {
      if (repositorio == null)
         throw new IllegalArgumentException("O repositório de lançamentos é obrigatório.");
      this.repositorio = repositorio;
   }

   public List<Lancamento> listar(Integer mes, int ano, String categoria)
         throws IOException {
      if (mes != null && (mes < 1 || mes > 12))
         throw new IllegalArgumentException("O mês deve estar entre 1 e 12.");
      if (ano < 1)
         throw new IllegalArgumentException("O ano deve ser maior que zero.");
      if (categoria != null && categoria.isBlank())
         throw new IllegalArgumentException("A categoria não pode ser vazia.");

      String categoriaNormalizada = categoria == null ? null : categoria.trim();
      return repositorio.listar().stream()
            .filter(lancamento -> mes == null ||
                  lancamento.data().getMonthValue() == mes)
            .filter(lancamento -> lancamento.data().getYear() == ano)
            .filter(lancamento -> categoriaNormalizada == null ||
                  categoriaNormalizada.equals(lancamento.categoria()))
            .toList();
   }
}
