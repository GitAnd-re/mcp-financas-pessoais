package br.edu.ifpr.mcp_financas_pessoais.services.financas.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Orcamento;

class JsonOrcamentoRepositorioTest {
   @TempDir
   Path tempDir;

   @Test
   @DisplayName("Arquivo ausente representa lista vazia")
   void arquivoAusente() throws Exception {
      JsonOrcamentoRepositorio repositorio = new JsonOrcamentoRepositorio(
            tempDir.resolve("orcamentos.json"));
      assertTrue(repositorio.listar().isEmpty());
   }

   @Test
   @DisplayName("Salvar e ler orçamento em JSON")
   void salvaELeJson() throws Exception {
      Path arquivo = tempDir.resolve("orcamentos.json");
      JsonOrcamentoRepositorio repositorio = new JsonOrcamentoRepositorio(arquivo);
      Orcamento esperado = new Orcamento("Mercado", 11, 2026,
            new BigDecimal("250.50"));
      repositorio.salvar(esperado);
      assertEquals(List.of(esperado), repositorio.listar());
      assertTrue(Files.readString(arquivo).contains("\"limite_mensal\""));
      assertTrue(Files.readString(arquivo).contains("\"mes\""));
      assertTrue(Files.readString(arquivo).contains("\"ano\""));
   }

   @Test
   @DisplayName("Substituir orçamento salvo para a mesma categoria e período")
   void atualizaOrcamento() throws Exception {
      JsonOrcamentoRepositorio repositorio = new JsonOrcamentoRepositorio(
            tempDir.resolve("orcamentos.json"));
      repositorio.salvar(new Orcamento("Mercado", 11, 2026,
            new BigDecimal("100.00")));
      repositorio.salvar(new Orcamento("Mercado", 12, 2026,
            new BigDecimal("125.00")));
      Orcamento atualizado = new Orcamento("Mercado", 11, 2026,
            new BigDecimal("200.00"));
      repositorio.salvar(atualizado);
      assertEquals(List.of(atualizado, new Orcamento("Mercado", 12, 2026,
            new BigDecimal("125.00"))), repositorio.listar());
   }
}
