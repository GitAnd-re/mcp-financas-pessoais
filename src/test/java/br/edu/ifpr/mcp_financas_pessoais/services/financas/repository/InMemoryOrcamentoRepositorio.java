package br.edu.ifpr.mcp_financas_pessoais.services.financas.repository;

import java.util.ArrayList;
import java.util.List;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Orcamento;

public final class InMemoryOrcamentoRepositorio implements OrcamentoRepositorio {
   private final List<Orcamento> orcamentos = new ArrayList<>();

   @Override
   public void salvar(Orcamento orcamento) {
      for (int i = 0; i < orcamentos.size(); i++) {
         if (orcamentos.get(i).categoria().equals(orcamento.categoria())) {
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
