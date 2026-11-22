package br.edu.ifpr.mcp_financas_pessoais.services.financas.model;

import java.math.BigDecimal;

public record Orcamento(String categoria, BigDecimal limiteMensal) { }
