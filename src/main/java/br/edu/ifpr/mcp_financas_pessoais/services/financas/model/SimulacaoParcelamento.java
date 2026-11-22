package br.edu.ifpr.mcp_financas_pessoais.services.financas.model;

import java.math.BigDecimal;

public record SimulacaoParcelamento(int parcelas,
                                    BigDecimal valorParcela,
                                    BigDecimal totalPago,
                                    BigDecimal jurosPagos,
                                    BigDecimal valorAVista) { }
