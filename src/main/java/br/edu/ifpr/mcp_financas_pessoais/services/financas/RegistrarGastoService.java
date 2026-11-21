package br.edu.ifpr.mcp_financas_pessoais.services.financas;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;

public class RegistrarGastoService {
   private final LancamentoRepositorio repositorio;

   public RegistrarGastoService(LancamentoRepositorio repositorio) {
      if (repositorio == null)
         throw new IllegalArgumentException("O repositório de lançamentos é obrigatório.");
      this.repositorio = repositorio;
   }

   public Lancamento registrar(BigDecimal valor,
                               String categoria,
                               LocalDate data,
                               String descricao) throws IOException {
      if (valor == null || valor.signum() <= 0)
         throw new IllegalArgumentException("O valor deve ser maior que zero.");
      if (categoria == null || categoria.isBlank())
         throw new IllegalArgumentException("A categoria não pode ser vazia.");
      if (data == null)
         throw new IllegalArgumentException("A data informada é inválida.");

      Lancamento lancamento = new Lancamento(UUID.randomUUID().toString(),
                              Lancamento.TipoLancamento.GASTO, valor,
                              categoria.trim(), null, data,
                              descricao == null ? "" : descricao.trim());
      repositorio.salvar(lancamento);
      return lancamento;
   }
}
