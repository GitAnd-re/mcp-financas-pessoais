package br.edu.ifpr.mcp_financas_pessoais.services.financas.repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_financas_pessoais.services.files.AuthPathService;
import br.edu.ifpr.mcp_financas_pessoais.services.financas.model.Lancamento;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class JsonLancamentoRepositorio implements LancamentoRepositorio {
   public static final String NOME_ARQUIVO_PADRAO = "lancamentos.json";

   private static final ObjectMapper MAPPER = new JsonMapper();
   private static final DateTimeFormatter DATE_FORMATTER =
         DateTimeFormatter.ofPattern("dd/MM/uuuu")
                          .withResolverStyle(ResolverStyle.STRICT);
   private static final TypeReference<List<Map<String, Object>>> JSON_LIST_TYPE =
                                    new TypeReference<List<Map<String, Object>>>() {};

   private final Path arquivo;

   public JsonLancamentoRepositorio(Path arquivo) {
      if (arquivo == null)
         throw new IllegalArgumentException("O caminho do arquivo JSON é obrigatório.");
      this.arquivo = arquivo;
   }

   public static JsonLancamentoRepositorio fromEnvironment() {
      Path path = Path.of(AuthPathService.authPath()).resolve(NOME_ARQUIVO_PADRAO);
      return new JsonLancamentoRepositorio(path);
   }

   @Override
   public synchronized void salvar(Lancamento lancamento) throws IOException {
      if (lancamento == null)
         throw new IllegalArgumentException("O lançamento é obrigatório.");

      List<Map<String, Object>> dados = lerDados();
      dados.add(toJson(lancamento));

      Path parent = arquivo.toAbsolutePath().getParent();
      if (parent != null)
         Files.createDirectories(parent);

      try (var writer = Files.newBufferedWriter(arquivo)) {
         MAPPER.writerWithDefaultPrettyPrinter().writeValue(writer, dados);
      }
   }

   @Override
   public synchronized List<Lancamento> listar() throws IOException {
      return lerDados().stream().map(JsonLancamentoRepositorio::fromJson).toList();
   }

   private List<Map<String, Object>> lerDados() throws IOException {
      if (Files.notExists(arquivo))
         return new ArrayList<>();

      try (var reader = Files.newBufferedReader(arquivo)) {
         List<Map<String, Object>> dados = MAPPER.readValue(reader, JSON_LIST_TYPE);
         return dados == null ? new ArrayList<>() : new ArrayList<>(dados);
      }
   }

   private static Map<String, Object> toJson(Lancamento lancamento) {
      Map<String, Object> json = new LinkedHashMap<>();
      json.put("id", lancamento.id());
      json.put("tipo", lancamento.tipo().name());
      json.put("valor", lancamento.valor());
      json.put("categoria", lancamento.categoria());
      json.put("fonte", lancamento.fonte());
      json.put("data", lancamento.data().format(DATE_FORMATTER));
      json.put("descricao", lancamento.descricao());
      return json;
   }

   private static Lancamento fromJson(Map<String, Object> json) {
      Object value = json.get("valor");
      return new Lancamento(
         stringValue(json.get("id")),
         Lancamento.TipoLancamento.valueOf(stringValue(json.get("tipo"))),
         new BigDecimal(value.toString()),
         nullableString(json.get("categoria")),
         nullableString(json.get("fonte")),
         LocalDate.parse(stringValue(json.get("data")), DATE_FORMATTER),
         nullableString(json.get("descricao"))
      );
   }

   private static String stringValue(Object value) {
      if (value == null)
         throw new IllegalArgumentException("O arquivo JSON contém um campo obrigatório ausente.");
      return value.toString();
   }

   private static String nullableString(Object value) {
      return value == null ? null : value.toString();
   }
}
