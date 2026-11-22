package br.edu.ifpr.mcp_financas_pessoais.services.financas.repository;

import java.util.ArrayList;
import java.util.List;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Orcamento;

public final class InMemoryOrcamentoRepositorio implements OrcamentoRepositorio {
   private final List<Orcamento> orcamentos = new ArrayList<>();

   @Override
   public void salvar(Orcamento orcamento) {
      for (int i = 0; i < orcamentos.size(); i++) {
         Orcamento salvo = orcamentos.get(i);
         if (salvo.categoria().equals(orcamento.categoria()) &&
               salvo.mes() == orcamento.mes() && salvo.ano() == orcamento.ano()) {
            orcamentos.set(i, orcamento);
            return;
         }
      }
      orcamentos.add(orcamento);
   }

   @Override
   public List<Orcamento> listar() {
      return List.copyOf(orcamentos);
   }
}
