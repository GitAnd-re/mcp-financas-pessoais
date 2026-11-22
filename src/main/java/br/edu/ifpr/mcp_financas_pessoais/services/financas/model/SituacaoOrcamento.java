package br.edu.ifpr.mcp_financas_pessoais.services.financas.model;

import java.math.BigDecimal;

public record SituacaoOrcamento(String categoria,
                                BigDecimal gasto,
                                BigDecimal limite,
                                BigDecimal restante,
                                boolean estourou) { }
