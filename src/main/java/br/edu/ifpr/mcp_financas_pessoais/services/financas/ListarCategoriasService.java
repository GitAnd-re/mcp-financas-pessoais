package br.edu.ifpr.mcp_financas_pessoais.services.financas;

import java.io.IOException;
import java.util.List;
import java.util.TreeSet;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Orcamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.OrcamentoRepositorio;

public class ListarCategoriasService {
   private final LancamentoRepositorio lancamentoRepositorio;
   private final OrcamentoRepositorio orcamentoRepositorio;

   public ListarCategoriasService(LancamentoRepositorio lancamentoRepositorio,
                                 OrcamentoRepositorio orcamentoRepositorio) {
      if (lancamentoRepositorio == null)
         throw new IllegalArgumentException("O repositório de lançamentos é obrigatório.");
      if (orcamentoRepositorio == null)
         throw new IllegalArgumentException("O repositório de orçamentos é obrigatório.");
      this.lancamentoRepositorio = lancamentoRepositorio;
      this.orcamentoRepositorio = orcamentoRepositorio;
   }

   public List<String> listar() throws IOException {
      TreeSet<String> categorias = new TreeSet<>();
      for (Lancamento lancamento : lancamentoRepositorio.listar()) {
         if (lancamento.tipo() == Lancamento.TipoLancamento.GASTO)
            adicionarCategoria(categorias, lancamento.categoria());
      }
      for (Orcamento orcamento : orcamentoRepositorio.listar())
         adicionarCategoria(categorias, orcamento.categoria());
      return List.copyOf(categorias);
   }

   private static void adicionarCategoria(TreeSet<String> categorias,
                                          String categoria) {
      if (categoria != null && !categoria.isBlank())
         categorias.add(categoria.trim());
   }
}
