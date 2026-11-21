package br.edu.ifpr.mcp_financas_pessoais.services.financas.repository;

import java.io.IOException;
import java.util.List;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;

public interface LancamentoRepositorio {
   void salvar(Lancamento lancamento) throws IOException;

   List<Lancamento> listar() throws IOException;
}
