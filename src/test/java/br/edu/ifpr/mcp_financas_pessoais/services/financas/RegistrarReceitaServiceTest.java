package br.edu.ifpr.mcp_financas_pessoais.services.financas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class RegistrarReceitaServiceTest {
   private InMemoryLancamentoRepositorio repositorio;
   private RegistrarReceitaService service;

   @BeforeEach
   void setUp() {
      repositorio = new InMemoryLancamentoRepositorio();
      service = new RegistrarReceitaService(repositorio);
   }

   @Test
   @DisplayName("Rejeitar repositório nulo")
   void rejeitaRepositorioNulo() {
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new RegistrarReceitaService(null));
      assertEquals("O repositório de lançamentos é obrigatório.", exception.getMessage());
   }

   @Test
   @DisplayName("Registrar receita e normalizar fonte")
   void registraReceita() throws IOException {
      LocalDate data = LocalDate.of(2026, 11, 20);

      Lancamento salva = service.registrar(new BigDecimal("25.90"), "  Salário  ", data);

      assertNotNull(salva.id());
      assertEquals(Lancamento.TipoLancamento.RECEITA, salva.tipo());
      assertEquals(new BigDecimal("25.90"), salva.valor());
      assertEquals("Salário", salva.fonte());
      assertEquals(data, salva.data());
      assertNull(salva.categoria());
      assertEquals("", salva.descricao());
      assertEquals(List.of(salva), repositorio.listar());
   }

   @Test
   @DisplayName("Rejeitar valor ausente, zero ou negativo")
   void rejeitaValorInvalido() {
      for (BigDecimal valor : new BigDecimal[] { null, BigDecimal.ZERO,
                                                 new BigDecimal("-0.01") }) {
         assertThrows(IllegalArgumentException.class,
               () -> service.registrar(valor, "Salário", LocalDate.now()));
      }
      assertEquals(List.of(), repositorio.listar());
   }

   @Test
   @DisplayName("Rejeitar fonte nula ou em branco")
   void rejeitaFonteInvalida() {
      for (String fonte : new String[] { null, "", "   " }) {
         assertThrows(IllegalArgumentException.class,
               () -> service.registrar(BigDecimal.ONE, fonte, LocalDate.now()));
      }
      assertEquals(List.of(), repositorio.listar());
   }

   @Test
   @DisplayName("Rejeitar data nula")
   void rejeitaDataNula() {
      assertThrows(IllegalArgumentException.class,
            () -> service.registrar(BigDecimal.ONE, "Salário", null));
      assertEquals(List.of(), repositorio.listar());
   }

   @Test
   @DisplayName("Propagar falha do repositório")
   void propagaFalhaDoRepositorio() {
      RegistrarReceitaService serviceComFalha = new RegistrarReceitaService(
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
            () -> serviceComFalha.registrar(BigDecimal.ONE, "Salário",
                  LocalDate.of(2026, 11, 20)));
      assertEquals("falha de gravação", exception.getMessage());
   }
}
