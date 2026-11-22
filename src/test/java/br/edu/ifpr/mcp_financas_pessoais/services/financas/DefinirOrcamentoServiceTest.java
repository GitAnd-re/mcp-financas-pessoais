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
      Orcamento salvo = service.definir("  Alimentação  ", 11, 2026,
            new BigDecimal("500.00"));
      assertEquals(new Orcamento("Alimentação", 11, 2026,
            new BigDecimal("500.00")), salvo);
      assertEquals(List.of(salvo), repositorio.listar());
   }

   @Test
   @DisplayName("Aceitar limite positivo de um centavo")
   void aceitaUmCentavo() throws IOException {
      Orcamento salvo = service.definir("Outros", 2, 2024,
            new BigDecimal("0.01"));
      assertEquals(new BigDecimal("0.01"), salvo.limiteMensal());
   }

   @Test
   @DisplayName("Rejeitar categoria ausente ou vazia")
   void rejeitaCategoriaInvalida() {
      for (String categoria : new String[] { null, "", "   " }) {
         IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
               () -> service.definir(categoria, 11, 2026, BigDecimal.ONE));
         assertEquals("A categoria não pode ser vazia.", exception.getMessage());
      }
      assertEquals(List.of(), repositorio.listar());
   }

   @Test
   @DisplayName("Rejeitar categoria que contém apenas um número")
   void rejeitaCategoriaNumerica() {
      for (String categoria : new String[] { "123", "10.50", "-2", "1e3" }) {
         IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
               () -> service.definir(categoria, 11, 2026, BigDecimal.ONE));
         assertEquals("A categoria não pode ser um número.", exception.getMessage());
      }
      assertEquals(List.of(), repositorio.listar());
   }

   @Test
   @DisplayName("Rejeitar mês fora do intervalo de 1 a 12")
   void rejeitaMesInvalido() {
      for (int mes : new int[] { 0, 13 }) {
         IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
               () -> service.definir("Mercado", mes, 2026, BigDecimal.ONE));
         assertEquals("O mês deve estar entre 1 e 12.", exception.getMessage());
      }
      assertEquals(List.of(), repositorio.listar());
   }

   @Test
   @DisplayName("Rejeitar ano menor que um")
   void rejeitaAnoInvalido() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> service.definir("Mercado", 11, 0, BigDecimal.ONE));
      assertEquals("O ano deve ser maior que zero.", exception.getMessage());
      assertEquals(List.of(), repositorio.listar());
   }

   @Test
   @DisplayName("Rejeitar limite ausente, zero ou negativo")
   void rejeitaLimiteInvalido() {
      for (BigDecimal limite : new BigDecimal[] { null, BigDecimal.ZERO,
            new BigDecimal("-0.01") }) {
         IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
               () -> service.definir("Mercado", 11, 2026, limite));
         assertEquals("O limite mensal deve ser maior que zero.", exception.getMessage());
      }
      assertEquals(List.of(), repositorio.listar());
   }

   @Test
   @DisplayName("Atualizar o limite da mesma categoria")
   void atualizaCategoriaExistente() throws IOException {
      service.definir("Mercado", 11, 2026, new BigDecimal("100.00"));
      Orcamento atualizado = service.definir("Mercado", 11, 2026,
            new BigDecimal("150.00"));
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
            () -> serviceComFalha.definir("Mercado", 11, 2026, BigDecimal.ONE));
      assertEquals("falha de gravação", exception.getMessage());
   }
}
