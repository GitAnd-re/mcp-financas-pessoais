package br.edu.ifpr.mcp_financas_pessoais.services.financas.repository;

import java.util.ArrayList;
import java.util.List;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;

public final class InMemoryLancamentoRepositorio implements LancamentoRepositorio {
   private final List<Lancamento> lancamentos = new ArrayList<>();

   @Override
   public void salvar(Lancamento lancamento) {
      lancamentos.add(lancamento);
   }

   @Override
   public List<Lancamento> listar() {
      return List.copyOf(lancamentos);
   }
}
