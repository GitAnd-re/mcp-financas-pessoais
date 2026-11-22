package br.edu.ifpr.mcp_financas_pessoais.services.financas;

import java.io.IOException;
import java.math.BigDecimal;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Orcamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.OrcamentoRepositorio;

public class DefinirOrcamentoService {
   private final OrcamentoRepositorio repositorio;

   public DefinirOrcamentoService(OrcamentoRepositorio repositorio) {
      if (repositorio == null)
         throw new IllegalArgumentException("O repositório de orçamentos é obrigatório.");
      this.repositorio = repositorio;
   }

   public Orcamento definir(String categoria, int mes, int ano,
                            BigDecimal limiteMensal)
         throws IOException {
      if (categoria == null || categoria.isBlank())
         throw new IllegalArgumentException("A categoria não pode ser vazia.");
      String categoriaNormalizada = categoria.trim();
      if (ehNumero(categoriaNormalizada))
         throw new IllegalArgumentException("A categoria não pode ser um número.");
      if (mes < 1 || mes > 12)
         throw new IllegalArgumentException("O mês deve estar entre 1 e 12.");
      if (ano < 1)
         throw new IllegalArgumentException("O ano deve ser maior que zero.");
      if (limiteMensal == null || limiteMensal.signum() <= 0)
         throw new IllegalArgumentException("O limite mensal deve ser maior que zero.");

      Orcamento orcamento = new Orcamento(categoriaNormalizada, mes, ano,
            limiteMensal);
      repositorio.salvar(orcamento);
      return orcamento;
   }

   private static boolean ehNumero(String valor) {
      try {
         new BigDecimal(valor);
         return true;
      }
      catch (NumberFormatException ex) {
         return false;
      }
   }
}
