package br.com.restaurante.utils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;   // ← import que faltava

import br.com.restaurante.model.ItemPedido;
import br.com.restaurante.model.Pedido;

/**
 * Gera relatório de vendas em PDF via iText 5.
 */
public class RelatorioPDF {

    // ── Fontes ──────────────────────────────────────────────────────
    private static final Font TITULO    = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private static final Font SUBTITULO = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL    = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font NEGRITO   = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
    private static final Font PEQUENO   = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL);
    private static final Font PEQUENO_N = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD);

    private static final BaseColor CINZA_HEADER   = new BaseColor(220, 220, 220);
    private static final BaseColor CINZA_SUBTOTAL = new BaseColor(240, 240, 240);
    private static final BaseColor LARANJA        = new BaseColor(232,  93,  39);

    private final DecimalFormat     moeda = new DecimalFormat("R$ #,##0.00");
    private final DateTimeFormatter dFmt  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── MÉTODO PRINCIPAL ────────────────────────────────────────────

    public byte[] gerar(String nomeRestaurante,
                        String periodoDesc,
                        List<Pedido> pedidos,
                        Map<String, BigDecimal> porCategoria) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        adicionarTitulo(doc, nomeRestaurante, periodoDesc);
        adicionarResumo(doc, pedidos);
        adicionarPorCategoria(doc, porCategoria);
        adicionarDetalhamento(doc, pedidos);

        doc.close();
        return baos.toByteArray();
    }

    // ── 1. TÍTULO ────────────────────────────────────────────────────

    private void adicionarTitulo(Document doc, String restaurante, String periodo)
            throws DocumentException {

        Paragraph titulo = new Paragraph(
            "RELATÓRIO DE VENDAS — " + restaurante.toUpperCase(), TITULO);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(6);
        doc.add(titulo);

        Paragraph per = new Paragraph("Período: " + periodo, NORMAL);
        per.setAlignment(Element.ALIGN_CENTER);
        per.setSpacingAfter(14);
        doc.add(per);

        // Linha separadora laranja — import correto agora
        LineSeparator sep = new LineSeparator();
        sep.setLineColor(LARANJA);
        sep.setLineWidth(1.5f);
        doc.add(new Chunk(sep));
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);
    }

    // ── 2. RESUMO ────────────────────────────────────────────────────

    private void adicionarResumo(Document doc, List<Pedido> pedidos)
            throws DocumentException {

        int qtdPedidos = pedidos.size();

        BigDecimal totalGeral = pedidos.stream()
                .map(Pedido::calcularTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ticketMedio = qtdPedidos > 0
                ? totalGeral.divide(BigDecimal.valueOf(qtdPedidos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long qtdMesa     = pedidos.stream().filter(p -> "mesa".equals(p.getTipo())).count();
        long qtdDelivery = pedidos.stream().filter(p -> "delivery".equals(p.getTipo())).count();

        Paragraph secTitulo = new Paragraph("RESUMO DO PERÍODO", SUBTITULO);
        secTitulo.setSpacingAfter(10);
        doc.add(secTitulo);

        PdfPTable tabela = new PdfPTable(3);
        tabela.setWidthPercentage(100);
        tabela.setWidths(new float[]{34, 33, 33});
        tabela.setSpacingAfter(6);

        celulaResumo(tabela, "Total de Pedidos",  String.valueOf(qtdPedidos));
        celulaResumo(tabela, "Faturamento Total", moeda.format(totalGeral));
        celulaResumo(tabela, "Ticket Médio",      moeda.format(ticketMedio));
        doc.add(tabela);

        Paragraph dist = new Paragraph(
            "Distribuição: " + qtdMesa + " pedido(s) de mesa  ·  "
            + qtdDelivery + " pedido(s) de delivery", PEQUENO);
        dist.setSpacingAfter(20);
        doc.add(dist);
    }

    // ── 3. FATURAMENTO POR CATEGORIA ─────────────────────────────────

    private void adicionarPorCategoria(Document doc,
                                       Map<String, BigDecimal> porCategoria)
            throws DocumentException {

        Paragraph secTitulo = new Paragraph("FATURAMENTO POR CATEGORIA", SUBTITULO);
        secTitulo.setSpacingAfter(10);
        doc.add(secTitulo);

        PdfPTable tabela = new PdfPTable(2);
        tabela.setWidthPercentage(100);
        tabela.setWidths(new float[]{65, 35});
        tabela.setSpacingAfter(20);

        celulaHeader(tabela, "Categoria");
        celulaHeader(tabela, "Total Faturado");

        BigDecimal totalGeral = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : porCategoria.entrySet()) {
            celulaTexto(tabela, entry.getKey());
            celulaValor(tabela, moeda.format(entry.getValue()));
            totalGeral = totalGeral.add(entry.getValue());
        }

        // Linha de total geral
        PdfPCell cLabel = new PdfPCell(new Phrase("TOTAL GERAL", NEGRITO));
        cLabel.setPadding(8);
        cLabel.setBackgroundColor(CINZA_SUBTOTAL);
        cLabel.setBorder(Rectangle.BOX);
        tabela.addCell(cLabel);

        PdfPCell cValor = new PdfPCell(new Phrase(moeda.format(totalGeral), NEGRITO));
        cValor.setPadding(8);
        cValor.setBackgroundColor(CINZA_SUBTOTAL);
        cValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cValor.setBorder(Rectangle.BOX);
        tabela.addCell(cValor);

        doc.add(tabela);
    }

    // ── 4. DETALHAMENTO DOS PEDIDOS ──────────────────────────────────

    private void adicionarDetalhamento(Document doc, List<Pedido> pedidos)
            throws DocumentException {

        if (pedidos.isEmpty()) return;

        doc.newPage();

        Paragraph secTitulo = new Paragraph("DETALHAMENTO DOS PEDIDOS", SUBTITULO);
        secTitulo.setSpacingAfter(10);
        doc.add(secTitulo);

        PdfPTable tabela = new PdfPTable(5);
        tabela.setWidthPercentage(100);
        tabela.setWidths(new float[]{8, 14, 12, 42, 14});

        celulaHeader(tabela, "#");
        celulaHeader(tabela, "Data");
        celulaHeader(tabela, "Tipo");
        celulaHeader(tabela, "Itens");
        celulaHeader(tabela, "Total");

        for (Pedido p : pedidos) {

            // Monta resumo dos itens
            StringBuilder sb = new StringBuilder();
            for (ItemPedido ip : p.getItens()) {
                sb.append(ip.getQuantidade()).append("x ").append(ip.getNomeItem()).append("; ");
            }
            String itensStr = sb.length() > 0
                ? sb.substring(0, Math.min(sb.length() - 2, 100))
                : "-";

            String tipo = "mesa".equals(p.getTipo())
                ? "Mesa " + p.getNumeroMesa()
                : "Delivery";

            celulaTexto(tabela, "#" + p.getIdPedido());
            celulaTexto(tabela, p.getDataAbertura() != null
                ? p.getDataAbertura().format(dFmt) : "-");
            celulaTexto(tabela, tipo);

            PdfPCell cItens = new PdfPCell(new Phrase(itensStr, PEQUENO));
            cItens.setPadding(6);
            cItens.setBorder(Rectangle.BOX);
            tabela.addCell(cItens);

            celulaValor(tabela, moeda.format(p.calcularTotal()));
        }

        doc.add(tabela);
    }

    // ── HELPERS DE CÉLULA ────────────────────────────────────────────

    private void celulaHeader(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, NEGRITO));
        c.setBackgroundColor(CINZA_HEADER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(7);
        c.setBorder(Rectangle.BOX);
        t.addCell(c);
    }

    private void celulaTexto(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "-", PEQUENO));
        c.setPadding(6);
        c.setBorder(Rectangle.BOX);
        t.addCell(c);
    }

    private void celulaValor(PdfPTable t, String valor) {
        PdfPCell c = new PdfPCell(new Phrase(valor, PEQUENO));
        c.setPadding(6);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setBorder(Rectangle.BOX);
        t.addCell(c);
    }

    private void celulaResumo(PdfPTable t, String label, String valor) {
        PdfPCell c = new PdfPCell();
        c.addElement(new Paragraph(label, PEQUENO_N));
        c.addElement(new Paragraph(valor, SUBTITULO));
        c.setPadding(10);
        c.setBorder(Rectangle.BOX);
        c.setBackgroundColor(CINZA_SUBTOTAL);
        t.addCell(c);
    }
}
