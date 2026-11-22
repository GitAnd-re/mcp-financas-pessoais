package br.edu.ifpr.mcp_financas_pessoais.services.financas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Orcamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryOrcamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.OrcamentoRepositorio;

class DefinirOrcamentoServiceTest {
   private InMemoryOrcamentoRepositorio repositorio;
   private DefinirOrcamentoService service;

   @BeforeEach
   void setUp() {
      repositorio = new InMemoryOrcamentoRepositorio();
      service = new DefinirOrcamentoService(repositorio);
   }

   @Test
   @DisplayName("Rejeitar repositório nulo")
   void rejeitaRepositorioNulo() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new DefinirOrcamentoService(null));
      assertEquals("O repositório de orçamentos é obrigatório.", exception.getMessage());
   }

   @Test
   @DisplayName("Definir orçamento e normalizar a categoria")
   void defineOrcamento() throws IOException {
      Orcamento salvo = service.definir("  Alimentação  ", new BigDecimal("500.00"));
      assertEquals(new Orcamento("Alimentação", new BigDecimal("500.00")), salvo);
      assertEquals(List.of(salvo), repositorio.listar());
   }

   @Test
   @DisplayName("Aceitar limite positivo de um centavo")
   void aceitaUmCentavo() throws IOException {
      Orcamento salvo = service.definir("Outros", new BigDecimal("0.01"));
      assertEquals(new BigDecimal("0.01"), salvo.limiteMensal());
   }

   @Test
   @DisplayName("Rejeitar categoria ausente ou vazia")
   void rejeitaCategoriaInvalida() {
      for (String categoria : new String[] { null, "", "   " }) {
         IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
               () -> service.definir(categoria, BigDecimal.ONE));
         assertEquals("A categoria não pode ser vazia.", exception.getMessage());
      }
      assertEquals(List.of(), repositorio.listar());
   }

   @Test
   @DisplayName("Rejeitar categoria que contém apenas um número")
   void rejeitaCategoriaNumerica() {
      for (String categoria : new String[] { "123", "10.50", "-2", "1e3" }) {
         IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
               () -> service.definir(categoria, BigDecimal.ONE));
         assertEquals("A categoria não pode ser um número.", exception.getMessage());
      }
      assertEquals(List.of(), repositorio.listar());
   }

   @Test
   @DisplayName("Rejeitar limite ausente, zero ou negativo")
   void rejeitaLimiteInvalido() {
      for (BigDecimal limite : new BigDecimal[] { null, BigDecimal.ZERO,
            new BigDecimal("-0.01") }) {
         IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
               () -> service.definir("Mercado", limite));
         assertEquals("O limite mensal deve ser maior que zero.", exception.getMessage());
      }
      assertEquals(List.of(), repositorio.listar());
   }

   @Test
   @DisplayName("Atualizar o limite da mesma categoria")
   void atualizaCategoriaExistente() throws IOException {
      service.definir("Mercado", new BigDecimal("100.00"));
      Orcamento atualizado = service.definir("Mercado", new BigDecimal("150.00"));
      assertEquals(new BigDecimal("150.00"), atualizado.limiteMensal());
      assertEquals(List.of(atualizado), repositorio.listar());
   }

   @Test
   @DisplayName("Propagar falha do repositório")
   void propagaFalhaDoRepositorio() {
      OrcamentoRepositorio comFalha = new OrcamentoRepositorio() {
         @Override
         public void salvar(Orcamento orcamento) throws IOException {
            throw new IOException("falha de gravação");
         }

         @Override
         public List<Orcamento> listar() {
            return List.of();
         }
      };
      DefinirOrcamentoService serviceComFalha = new DefinirOrcamentoService(comFalha);
      IOException exception = assertThrows(IOException.class,
            () -> serviceComFalha.definir("Mercado", BigDecimal.ONE));
      assertEquals("falha de gravação", exception.getMessage());
   }
}
