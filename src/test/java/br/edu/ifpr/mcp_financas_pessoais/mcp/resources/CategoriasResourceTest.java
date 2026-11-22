package br.edu.ifpr.mcp_financas_pessoais.mcp.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_financas_pessoais.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Orcamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryLancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryOrcamentoRepositorio;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;

class CategoriasResourceTest {
   @Test
   @DisplayName("Listar categorias de gasto disponíveis")
   void listaCategorias() {
      InMemoryLancamentoRepositorio lancamentos = new InMemoryLancamentoRepositorio();
      lancamentos.salvar(new Lancamento("id", Lancamento.TipoLancamento.GASTO,
            new BigDecimal("12.50"), "Casa", null,
            LocalDate.of(2026, 11, 5), ""));
      InMemoryOrcamentoRepositorio orcamentos = new InMemoryOrcamentoRepositorio();
      orcamentos.salvar(new Orcamento("Lazer", 11, 2026,
            new BigDecimal("100.00")));

      CategoriasResource resource = new CategoriasResource(lancamentos, orcamentos);
      String content = ((TextResourceContents) resource.read(null,
            ReadResourceRequest.builder(McpConsts.RESOURCE_CATEGORIAS_URI).build())
            .contents().get(0)).text();

      assertEquals("Casa\nLazer", content);
   }
}
