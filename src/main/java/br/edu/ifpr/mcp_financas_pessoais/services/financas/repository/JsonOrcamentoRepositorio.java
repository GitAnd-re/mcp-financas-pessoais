package br.edu.ifpr.mcp_financas_pessoais.services.financas.repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import br.edu.ifpr.mcp_financas_pessoais.services.files.AuthPathService;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Orcamento;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class JsonOrcamentoRepositorio implements OrcamentoRepositorio {
   public static final String NOME_ARQUIVO_PADRAO = "orcamentos.json";

   private static final ObjectMapper MAPPER = new JsonMapper();
   private final Path arquivo;

   public JsonOrcamentoRepositorio(Path arquivo) {
      if (arquivo == null)
         throw new IllegalArgumentException("O caminho do arquivo JSON é obrigatório.");
      this.arquivo = arquivo;
   }

   public static JsonOrcamentoRepositorio fromEnvironment() {
      Path path = Path.of(AuthPathService.authPath()).resolve(NOME_ARQUIVO_PADRAO);
      return new JsonOrcamentoRepositorio(path);
   }

   @Override
   public synchronized void salvar(Orcamento orcamento) throws IOException {
      if (orcamento == null)
         throw new IllegalArgumentException("O orçamento é obrigatório.");

      List<Orcamento> orcamentos = lerDados();
      boolean atualizado = false;
      for (int i = 0; i < orcamentos.size(); i++) {
         if (orcamentos.get(i).categoria().equals(orcamento.categoria())) {
            orcamentos.set(i, orcamento);
            atualizado = true;
            break;
         }
      }
      if (!atualizado)
         orcamentos.add(orcamento);

      Path parent = arquivo.toAbsolutePath().getParent();
      if (parent != null)
         Files.createDirectories(parent);
      try (var writer = Files.newBufferedWriter(arquivo)) {
         List<DadosOrcamento> dados = orcamentos.stream()
               .map(DadosOrcamento::fromModel).toList();
         MAPPER.writerWithDefaultPrettyPrinter().writeValue(writer, dados);
      }
   }

   @Override
   public synchronized List<Orcamento> listar() throws IOException {
      return lerDados();
   }

   private List<Orcamento> lerDados() throws IOException {
      if (Files.notExists(arquivo))
         return new ArrayList<>();
      try (var reader = Files.newBufferedReader(arquivo)) {
         List<DadosOrcamento> dados = MAPPER.readerForListOf(DadosOrcamento.class)
               .readValue(reader);
         if (dados == null)
            return new ArrayList<>();
         List<Orcamento> orcamentos = new ArrayList<>();
         for (DadosOrcamento dado : dados)
            orcamentos.add(dado.toModel());
         return orcamentos;
      }
   }

   private record DadosOrcamento(String categoria, BigDecimal limite_mensal) {
      static DadosOrcamento fromModel(Orcamento orcamento) {
         return new DadosOrcamento(orcamento.categoria(), orcamento.limiteMensal());
      }

      Orcamento toModel() {
         if (categoria == null || limite_mensal == null)
            throw new IllegalArgumentException(
                  "O arquivo JSON contém um campo obrigatório ausente.");
         return new Orcamento(categoria, limite_mensal);
      }
   }
}
