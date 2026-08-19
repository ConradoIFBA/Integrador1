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
 * ================================================================
 * RELATORIOPDF — GERADOR DO RELATÓRIO DE VENDAS EM PDF (iText 5)
 * ================================================================
 *
 * Monta, em memória, um documento PDF completo com o relatório de
 * vendas de um período — usado pelo RelatorioController quando o
 * GERENTE clica em "Gerar PDF" na tela de relatórios. Esta classe
 * não sabe nada sobre banco de dados, sessão HTTP ou permissões —
 * ela só recebe dados já prontos (lista de pedidos + totais por
 * categoria) e devolve os BYTES do PDF pronto; quem busca os dados
 * no banco e quem devolve o PDF ao navegador é responsabilidade do
 * RelatorioController.
 *
 * POR QUE iText 5 (e não uma versão mais nova):
 *   iText 5 usa a API clássica baseada em Document/Paragraph/
 *   PdfPTable, mais simples de usar "na mão" (sem precisar de
 *   templates externos) para relatórios tabulares como este —
 *   adequado ao escopo do projeto.
 *
 * ESTRUTURA DO PDF GERADO (nesta ordem, ver método gerar()):
 *   1. Título do relatório + período + linha separadora laranja
 *   2. Resumo do período (total de pedidos, faturamento, ticket médio)
 *   3. Faturamento por categoria (tabela categoria → valor)
 *   4. Detalhamento de cada pedido (em página nova, uma linha por pedido)
 *
 * SOBRE O IMPORT DE LineSeparator:
 *   Esta classe já teve um bug conhecido em que o import de
 *   `com.itextpdf.text.pdf.draw.LineSeparator` estava ausente,
 *   causando erro de compilação em adicionarTitulo() (a classe
 *   LineSeparator é usada para desenhar a linha horizontal laranja
 *   abaixo do título). O import já está corrigido e presente logo
 *   acima — mantido comentado no código-fonte como lembrete de que
 *   esse é justamente o tipo de import "escondido" que o
 *   Eclipse às vezes não sugere automaticamente por já existir uma
 *   classe LineSeparator em outro pacote do iText, então vale
 *   atenção redobrada se este arquivo for editado no futuro.
 *
 * @author Sistema Integrador
 * @version 3.0
 * @see br.com.restaurante.controller.RelatorioController
 * @see br.com.restaurante.model.Pedido
 */
public class RelatorioPDF {

    // ── Fontes ──────────────────────────────────────────────────────
    // Um "kit" fixo de fontes reutilizado em todo o documento, para
    // manter consistência visual sem redeclarar Font em cada método.
    // Todas em Helvetica (fonte padrão embutida no PDF, não requer
    // arquivo .ttf externo) variando só tamanho/peso conforme o uso:
    private static final Font TITULO    = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);   // título principal
    private static final Font SUBTITULO = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);   // títulos de seção
    private static final Font NORMAL    = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL); // texto corrido
    private static final Font NEGRITO   = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);   // destaques/totais
    private static final Font PEQUENO   = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL); // células de tabela
    private static final Font PEQUENO_N = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD);   // labels pequenos em negrito

    // ── Cores ───────────────────────────────────────────────────────
    // BaseColor é a classe de cor própria do iText (não é
    // java.awt.Color, embora aceite os mesmos componentes RGB).
    private static final BaseColor CINZA_HEADER   = new BaseColor(220, 220, 220); // fundo dos cabeçalhos de tabela
    private static final BaseColor CINZA_SUBTOTAL = new BaseColor(240, 240, 240); // fundo das linhas de total/resumo
    private static final BaseColor LARANJA        = new BaseColor(232,  93,  39); // cor de destaque da marca (linha do título)

    // ── Formatadores ────────────────────────────────────────────────
    // Instância (não static) porque DecimalFormat e
    // DateTimeFormatter NÃO são thread-safe para uso concorrente
    // agressivo — mantê-los como campo de instância evita qualquer
    // dúvida sobre reuso indevido entre threads, já que cada
    // requisição HTTP que gera um relatório cria seu próprio
    // RelatorioPDF novo (ver RelatorioController).
    private final DecimalFormat     moeda = new DecimalFormat("R$ #,##0.00");
    private final DateTimeFormatter dFmt  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── MÉTODO PRINCIPAL ────────────────────────────────────────────

    /**
     * Gera o PDF completo do relatório de vendas e devolve os bytes
     * prontos para serem escritos na resposta HTTP (ou salvos em
     * arquivo, se necessário).
     *
     * @param nomeRestaurante nome exibido no título do relatório
     *        (impresso em maiúsculas)
     * @param periodoDesc     descrição textual do período already
     *        formatada pelo chamador (ex: "01/08/2026 a 08/08/2026")
     * @param pedidos         lista de pedidos do período (já filtrada
     *        pelo RelatorioController, tipicamente só status
     *        'entregue' dentro do intervalo de datas escolhido) —
     *        cada Pedido deve vir com seus ItemPedido carregados,
     *        pois o detalhamento e o cálculo de total dependem disso
     * @param porCategoria    mapa "nome da categoria" → soma
     *        faturada naquela categoria no período, já calculado
     *        pelo chamador (esta classe só formata, não soma nada
     *        vindo do banco)
     * @return array de bytes do PDF pronto, pronto para envio como
     *         resposta HTTP com content-type application/pdf
     * @throws Exception qualquer falha do iText ao montar o
     *         documento (DocumentException) ou de I/O ao escrever
     *         no ByteArrayOutputStream (praticamente nunca ocorre,
     *         já que é um stream em memória, não em disco)
     */
    public byte[] gerar(String nomeRestaurante,
                        String periodoDesc,
                        List<Pedido> pedidos,
                        Map<String, BigDecimal> porCategoria) throws Exception {

        // O PDF inteiro é montado EM MEMÓRIA (ByteArrayOutputStream),
        // nunca gravado em disco — importante porque o Tomcat pode
        // rodar num ambiente sem permissão de escrita em arquivos, e
        // porque não sobra "lixo" de arquivos temporários no servidor
        // a cada relatório gerado.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // PageSize.A4 = formato de página padrão brasileiro (mais
        // comum que Letter para documentos gerados no Brasil).
        // Os 4 números são as margens em pontos, na ordem
        // esquerda/direita/topo/rodapé: 40, 40, 50, 50.
        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);

        // Conecta o "escritor" do PDF ao stream de saída — é este
        // PdfWriter que efetivamente traduz as instruções de alto
        // nível (Paragraph, PdfPTable, etc.) para o formato binário
        // do PDF, gravando no ByteArrayOutputStream conforme o
        // documento é preenchido.
        PdfWriter.getInstance(doc, baos);

        // A partir daqui o documento está "aberto" e pronto para
        // receber elementos — chamar doc.add(...) antes de open()
        // lançaria exceção.
        doc.open();

        // Cada seção do relatório é montada por um método privado
        // dedicado, na ORDEM em que aparecem no PDF final — ver
        // comentário de cada método abaixo para o que cada um faz.
        adicionarTitulo(doc, nomeRestaurante, periodoDesc);
        adicionarResumo(doc, pedidos);
        adicionarPorCategoria(doc, porCategoria);
        adicionarDetalhamento(doc, pedidos);

        // Fecha o documento — é só depois deste close() que o
        // PdfWriter finaliza a escrita (trailer, xref table, etc.)
        // e o conteúdo do ByteArrayOutputStream vira um PDF válido
        // e completo, seguro para ser lido por qualquer visualizador.
        doc.close();
        return baos.toByteArray();
    }

    // ── 1. TÍTULO ────────────────────────────────────────────────────

    /**
     * Monta o cabeçalho do relatório: nome do restaurante em
     * destaque (maiúsculo), a descrição do período logo abaixo, e
     * uma linha horizontal laranja separando o cabeçalho do
     * conteúdo — dá a "cara" visual do documento antes de qualquer
     * dado numérico aparecer.
     */
    private void adicionarTitulo(Document doc, String restaurante, String periodo)
            throws DocumentException {

        Paragraph titulo = new Paragraph(
            "RELATÓRIO DE VENDAS — " + restaurante.toUpperCase(), TITULO);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(6); // espaço abaixo do parágrafo, em pontos
        doc.add(titulo);

        Paragraph per = new Paragraph("Período: " + periodo, NORMAL);
        per.setAlignment(Element.ALIGN_CENTER);
        per.setSpacingAfter(14);
        doc.add(per);

        // Linha separadora laranja — import correto agora.
        // LineSeparator é um elemento "de desenho" do iText (pacote
        // ...pdf.draw), diferente de Paragraph/Phrase — por isso
        // precisa ser envolvido num Chunk (new Chunk(sep)) antes de
        // poder ser adicionado ao Document, já que doc.add() só
        // aceita implementações de Element, e Chunk é o adaptador
        // que faz essa ponte.
        LineSeparator sep = new LineSeparator();
        sep.setLineColor(LARANJA);
        sep.setLineWidth(1.5f);
        doc.add(new Chunk(sep));

        // Duas quebras de linha em branco para dar "respiro" visual
        // antes da próxima seção (RESUMO DO PERÍODO).
        doc.add(Chunk.NEWLINE);
        doc.add(Chunk.NEWLINE);
    }

    // ── 2. RESUMO ────────────────────────────────────────────────────

    /**
     * Calcula e exibe os três números-chave do período — total de
     * pedidos, faturamento total e ticket médio — numa tabela de 3
     * colunas, seguida de uma linha de texto com a distribuição
     * entre pedidos de mesa e delivery.
     *
     * Todo o cálculo é feito aqui EM CIMA da lista de Pedido já
     * recebida (com Stream API), não faz nenhuma nova consulta ao
     * banco — a responsabilidade de já trazer os dados certos
     * (período/status filtrados) é do RelatorioController.
     */
    private void adicionarResumo(Document doc, List<Pedido> pedidos)
            throws DocumentException {

        int qtdPedidos = pedidos.size();

        // Soma o total de todos os pedidos usando o próprio método
        // de domínio Pedido.calcularTotal() (que soma
        // quantidade × preço_unitário de cada item do pedido) —
        // reaproveita a mesma lógica de cálculo usada em outras
        // partes do sistema (ex: tela de detalhe do pedido),
        // evitando duplicar a fórmula do total aqui.
        BigDecimal totalGeral = pedidos.stream()
                .map(Pedido::calcularTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ticket médio = faturamento total ÷ quantidade de pedidos.
        // Guarda explícita contra divisão por zero: se não houve
        // nenhum pedido no período, o ticket médio é
        // apresentado como zero em vez de lançar
        // ArithmeticException (que BigDecimal.divide lançaria ao
        // tentar dividir por BigDecimal.ZERO).
        BigDecimal ticketMedio = qtdPedidos > 0
                ? totalGeral.divide(BigDecimal.valueOf(qtdPedidos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Conta quantos pedidos são de cada tipo — usado só na
        // linha de "Distribuição" abaixo da tabela de resumo.
        long qtdMesa     = pedidos.stream().filter(p -> "mesa".equals(p.getTipo())).count();
        long qtdDelivery = pedidos.stream().filter(p -> "delivery".equals(p.getTipo())).count();

        Paragraph secTitulo = new Paragraph("RESUMO DO PERÍODO", SUBTITULO);
        secTitulo.setSpacingAfter(10);
        doc.add(secTitulo);

        // Tabela de 3 colunas com larguras iguais (34/33/33%) —
        // setWidthPercentage(100) faz a tabela ocupar toda a
        // largura útil da página (descontadas as margens definidas
        // no Document lá no método gerar()).
        PdfPTable tabela = new PdfPTable(3);
        tabela.setWidthPercentage(100);
        tabela.setWidths(new float[]{34, 33, 33});
        tabela.setSpacingAfter(6);

        // celulaResumo() monta cada "card" da tabela (label pequeno
        // em cima, valor grande embaixo, fundo cinza) — ver helper
        // no fim da classe.
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

    /**
     * Renderiza a tabela "Categoria → Total Faturado" a partir do
     * mapa já calculado pelo chamador, encerrando com uma linha de
     * TOTAL GERAL somando todas as categorias — essa soma é
     * recalculada aqui (não recebida pronta) simplesmente somando
     * os values do próprio Map durante a iteração, então se o mapa
     * já vier consistente com o total geral do resumo (seção 2),
     * os dois números devem sempre bater.
     */
    private void adicionarPorCategoria(Document doc,
                                       Map<String, BigDecimal> porCategoria)
            throws DocumentException {

        Paragraph secTitulo = new Paragraph("FATURAMENTO POR CATEGORIA", SUBTITULO);
        secTitulo.setSpacingAfter(10);
        doc.add(secTitulo);

        // Tabela de 2 colunas: nome da categoria (65% da largura) e
        // valor faturado (35%, alinhado à direita como valores
        // monetários costumam ser exibidos).
        PdfPTable tabela = new PdfPTable(2);
        tabela.setWidthPercentage(100);
        tabela.setWidths(new float[]{65, 35});
        tabela.setSpacingAfter(20);

        celulaHeader(tabela, "Categoria");
        celulaHeader(tabela, "Total Faturado");

        BigDecimal totalGeral = BigDecimal.ZERO;

        // A ordem de iteração do Map.entrySet() depende da
        // implementação concreta do Map passado pelo chamador (se
        // for um LinkedHashMap, preserva a ordem de inserção; se
        // for HashMap comum, a ordem não é garantida) — esta classe
        // não impõe nenhuma ordenação própria, então quem monta o
        // mapa no RelatorioController é responsável por decidir a
        // ordem de exibição das categorias, se isso importar.
        for (Map.Entry<String, BigDecimal> entry : porCategoria.entrySet()) {
            celulaTexto(tabela, entry.getKey());
            celulaValor(tabela, moeda.format(entry.getValue()));
            totalGeral = totalGeral.add(entry.getValue());
        }

        // Linha de total geral — montada manualmente (em vez de usar
        // os helpers celulaTexto/celulaValor) porque precisa de
        // fonte em negrito e fundo cinza diferenciado, coisa que os
        // helpers padrão não aplicam.
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

    /**
     * Lista, um a um, todos os pedidos do período em uma tabela de
     * 5 colunas (número, data, tipo, resumo dos itens, total) —
     * sempre em UMA PÁGINA NOVA (doc.newPage()), separando
     * visualmente o "resumo executivo" (seções 1-3) do detalhamento
     * operacional linha-a-linha, que tende a ser bem mais longo.
     *
     * Se não houver nenhum pedido no período, o método simplesmente
     * retorna sem adicionar nada — evita gerar uma página extra em
     * branco (com título de seção mas tabela vazia) só para dizer
     * "não há dados".
     */
    private void adicionarDetalhamento(Document doc, List<Pedido> pedidos)
            throws DocumentException {

        if (pedidos.isEmpty()) return;

        doc.newPage();

        Paragraph secTitulo = new Paragraph("DETALHAMENTO DOS PEDIDOS", SUBTITULO);
        secTitulo.setSpacingAfter(10);
        doc.add(secTitulo);

        // 5 colunas com larguras desproporcionais — a coluna
        // "Itens" (índice 3) recebe a maior fatia (42%) porque
        // costuma conter o texto mais longo (lista de itens do
        // pedido), enquanto "#" (índice 0) recebe a menor (8%) por
        // ser só um número curto.
        PdfPTable tabela = new PdfPTable(5);
        tabela.setWidthPercentage(100);
        tabela.setWidths(new float[]{8, 14, 12, 42, 14});

        celulaHeader(tabela, "#");
        celulaHeader(tabela, "Data");
        celulaHeader(tabela, "Tipo");
        celulaHeader(tabela, "Itens");
        celulaHeader(tabela, "Total");

        for (Pedido p : pedidos) {

            // Monta um resumo textual de todos os itens do pedido no
            // formato "2x Picanha; 1x Suco de laranja; ..." —
            // concatenação manual com StringBuilder em vez de
            // String.join porque cada item já vem com a quantidade
            // embutida no texto, não é uma lista simples de strings.
            StringBuilder sb = new StringBuilder();
            for (ItemPedido ip : p.getItens()) {
                sb.append(ip.getQuantidade()).append("x ").append(ip.getNomeItem()).append("; ");
            }

            // Remove o "; " sobrando no final (2 últimos caracteres)
            // e trunca em no máximo 100 caracteres para pedidos com
            // muitos itens não estourarem a altura da linha da
            // tabela de forma descontrolada. Math.min garante que
            // não tentamos "cortar" além do que a string realmente
            // tem (evita StringIndexOutOfBoundsException em pedidos
            // com resumo curto).
            String itensStr = sb.length() > 0
                ? sb.substring(0, Math.min(sb.length() - 2, 100))
                : "-";

            // Pedidos de mesa mostram o número da mesa; delivery só
            // mostra "Delivery" (não há número de mesa aplicável).
            String tipo = "mesa".equals(p.getTipo())
                ? "Mesa " + p.getNumeroMesa()
                : "Delivery";

            celulaTexto(tabela, "#" + p.getIdPedido());
            celulaTexto(tabela, p.getDataAbertura() != null
                ? p.getDataAbertura().format(dFmt) : "-");
            celulaTexto(tabela, tipo);

            // Célula de itens montada manualmente (em vez de
            // celulaTexto) para poder usar a fonte PEQUENO — mesmo
            // resultado visual de celulaTexto, mas explicitado aqui
            // por clareza de que este texto tende a ser o mais longo
            // da linha.
            PdfPCell cItens = new PdfPCell(new Phrase(itensStr, PEQUENO));
            cItens.setPadding(6);
            cItens.setBorder(Rectangle.BOX);
            tabela.addCell(cItens);

            celulaValor(tabela, moeda.format(p.calcularTotal()));
        }

        doc.add(tabela);
    }

    // ── HELPERS DE CÉLULA ────────────────────────────────────────────
    // Os quatro métodos abaixo existem só para não repetir a mesma
    // sequência de "criar Phrase → configurar padding/borda/
    // alinhamento → adicionar na tabela" toda vez que uma célula
    // precisa ser montada — cada um cobre um "papel visual"
    // diferente dentro das tabelas do relatório.

    /** Célula de CABEÇALHO de tabela: negrito, fundo cinza, centralizada. */
    private void celulaHeader(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, NEGRITO));
        c.setBackgroundColor(CINZA_HEADER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(7);
        c.setBorder(Rectangle.BOX);
        t.addCell(c);
    }

    /**
     * Célula de TEXTO comum (alinhamento padrão à esquerda). Se o
     * texto vier null (defensivo — não deveria acontecer com os
     * dados atuais, mas evita "null" literal aparecendo no PDF caso
     * algum campo do banco esteja vazio), exibe "-" no lugar.
     */
    private void celulaTexto(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "-", PEQUENO));
        c.setPadding(6);
        c.setBorder(Rectangle.BOX);
        t.addCell(c);
    }

    /** Célula de VALOR monetário: alinhada à direita, como é convenção para números. */
    private void celulaValor(PdfPTable t, String valor) {
        PdfPCell c = new PdfPCell(new Phrase(valor, PEQUENO));
        c.setPadding(6);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setBorder(Rectangle.BOX);
        t.addCell(c);
    }

    /**
     * Célula "card" usada na tabela de RESUMO (seção 2): duas linhas
     * dentro da MESMA célula — um label pequeno em negrito em cima
     * (c.addElement aceita múltiplos Paragraphs empilhados
     * verticalmente dentro de uma única PdfPCell) e o valor em
     * destaque (fonte SUBTITULO, maior) embaixo, com fundo cinza
     * para se diferenciar visualmente de uma tabela de dados comum.
     */
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
