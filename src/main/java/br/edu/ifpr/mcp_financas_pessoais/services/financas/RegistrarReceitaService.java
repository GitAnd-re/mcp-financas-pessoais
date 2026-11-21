package br.edu.ifpr.mcp_financas_pessoais.services.financas;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;

public class RegistrarReceitaService {
   private final LancamentoRepositorio repositorio;

   public RegistrarReceitaService(LancamentoRepositorio repositorio) {
      if (repositorio == null)
         throw new IllegalArgumentException("O repositório de lançamentos é obrigatório.");
      this.repositorio = repositorio;
   }

   public Lancamento registrar(BigDecimal valor,
                               String fonte,
                               LocalDate data) throws IOException {
      if (valor == null || valor.signum() <= 0)
         throw new IllegalArgumentException("O valor deve ser maior que zero.");
      if (fonte == null || fonte.isBlank())
         throw new IllegalArgumentException("O campo fonte não pode ser vazio.");
      if (data == null)
         throw new IllegalArgumentException("A data informada é inválida.");

      Lancamento lancamento = new Lancamento(UUID.randomUUID().toString(),
            Lancamento.TipoLancamento.RECEITA, valor, null, fonte.trim(), data, "");
      repositorio.salvar(lancamento);
      return lancamento;
   }
}
