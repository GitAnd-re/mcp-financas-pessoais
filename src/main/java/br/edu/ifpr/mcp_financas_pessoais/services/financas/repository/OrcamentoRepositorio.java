package br.edu.ifpr.mcp_financas_pessoais.services.financas.repository;

import java.io.IOException;
import java.util.List;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Orcamento;

public interface OrcamentoRepositorio {
   void salvar(Orcamento orcamento) throws IOException;

   List<Orcamento> listar() throws IOException;
}
