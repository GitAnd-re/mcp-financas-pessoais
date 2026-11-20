package br.edu.ifpr.mcp_financas_pessoais.services.pdfs.processors;

/**
 * Representa o texto extraído de uma página de um documento PDF.
 *
 * @param pageNumber número da página no documento, começando em 1
 * @param text       texto extraído da página
 */
public record PdfPage(int pageNumber, String text) {}
