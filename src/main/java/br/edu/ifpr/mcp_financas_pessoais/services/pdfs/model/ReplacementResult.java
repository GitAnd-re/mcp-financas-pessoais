package br.edu.ifpr.mcp_financas_pessoais.services.pdfs.model;

import java.util.Map;

public record ReplacementResult(int totalOccurrences,
                                Map<Integer, Integer> occurrencesPerPage) {
   // garantimos que o mapa seja imutável para evitar modificações inadvertidas
   public ReplacementResult {
      occurrencesPerPage = Map.copyOf(occurrencesPerPage);
   }
}
