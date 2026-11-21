package br.edu.ifpr.mcp_financas_pessoais.services.financas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.InMemoryLancamentoRepositorio;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.repository.LancamentoRepositorio;

class RegistrarGastoServiceTest {
   private InMemoryLancamentoRepositorio repositorio;
   private RegistrarGastoService service;

   @BeforeEach
   void setUp() {
      repositorio = new InMemoryLancamentoRepositorio();
      service = new RegistrarGastoService(repositorio);
   }

   @Test
   @DisplayName("Rejeitar repositório nulo")
   void rejeitaRepositorioNulo() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new RegistrarGastoService(null));
      assertEquals("O repositório de lançamentos é obrigatório.", exception.getMessage());
   }

   @Test
   @DisplayName("Registrar gasto e normalizar categoria e descrição")
   void registraGasto() throws IOException {
      LocalDate data = LocalDate.of(2026, 11, 20);

      Lancamento salvo = service.registrar(new BigDecimal("25.90"),
            "  Alimentação  ", data, "  Almoço  ");

      assertNotNull(salvo.id());
      assertEquals(Lancamento.TipoLancamento.GASTO, salvo.tipo());
      assertEquals(new BigDecimal("25.90"), salvo.valor());
      assertEquals("Alimentação", salvo.categoria());
      assertEquals(data, salvo.data());
      assertEquals("Almoço", salvo.descricao());
      assertEquals(List.of(salvo), repositorio.listar());
   }

   @Test
   @DisplayName("Aceitar descrição nula como texto vazio")
   void aceitaDescricaoNula() throws IOException {
      Lancamento salvo = service.registrar(new BigDecimal("0.01"), "Outros",
            LocalDate.of(2024, 2, 29), null);

      assertEquals("", salvo.descricao());
   }

   @Test
   @DisplayName("Rejeitar valor ausente, zero ou negativo")
   void rejeitaValorInvalido() {
      for (BigDecimal valor : new BigDecimal[] { null, BigDecimal.ZERO,
                                                 new BigDecimal("-0.01") }) {
         assertThrows(IllegalArgumentException.class,
               () -> service.registrar(valor, "Mercado", LocalDate.now(), ""));
      }
      assertEquals(List.of(), repositorio.listar());
   }

   @Test
   @DisplayName("Rejeitar categoria nula ou em branco")
   void rejeitaCategoriaInvalida() {
      for (String categoria : new String[] { null, "", "   " }) {
         assertThrows(IllegalArgumentException.class,
               () -> service.registrar(BigDecimal.ONE, categoria, LocalDate.now(), ""));
      }
      assertEquals(List.of(), repositorio.listar());
   }

   @Test
   @DisplayName("Rejeitar data nula")
   void rejeitaDataNula() {
      assertThrows(IllegalArgumentException.class,
            () -> service.registrar(BigDecimal.ONE, "Mercado", null, ""));
      assertEquals(List.of(), repositorio.listar());
   }

   @Test
   @DisplayName("Propagar falha do repositório")
   void propagaFalhaDoRepositorio() {
      RegistrarGastoService serviceComFalha = new RegistrarGastoService(
            new LancamentoRepositorio() {
               @Override
               public void salvar(Lancamento lancamento) throws IOException {
                  throw new IOException("falha de gravação");
               }

               @Override
               public List<Lancamento> listar() {
                  return List.of();
               }
            });

      IOException exception = assertThrows(IOException.class,
            () -> serviceComFalha.registrar(BigDecimal.ONE, "Mercado",
                  LocalDate.of(2026, 11, 20), ""));
      assertEquals("falha de gravação", exception.getMessage());
   }
}
