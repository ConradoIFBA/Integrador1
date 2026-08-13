package br.com.restaurante.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import br.com.restaurante.utils.UploadImagemUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ================================================================
 * IMAGEM SERVLET — SERVE AS FOTOS DOS ITENS DO CARDÁPIO
 * ================================================================
 *
 * PROPÓSITO:
 * As imagens dos itens do cardápio são salvas FORA da pasta do
 * projeto (ver UploadImagemUtil para o motivo — sobreviver a
 * Clean/republish do Tomcat), então o Tomcat não consegue servi-las
 * sozinho como faz com o conteúdo de webapp/assets/. Este servlet
 * existe só para isso: ler os bytes do arquivo naquela pasta externa
 * e devolver como resposta HTTP, com o Content-Type correto.
 *
 * ROTA MAPEADA: /imagens/cardapio/{nomeDoArquivo}
 * Exemplo de uso numa JSP:
 *   <img src="${pageContext.request.contextPath}/imagens/cardapio/${item.imagem}">
 *
 * Esta rota é INTENCIONALMENTE pública (sem passar pelo AuthFilter,
 * que só protege /app/*) — faz sentido, já que fotos de pratos não
 * são informação sensível, e o cardápio em si já é visível para
 * qualquer perfil logado.
 *
 * SEGURANÇA — por que não dá para simplesmente concatenar o
 * pathInfo num caminho de arquivo sem cuidado:
 * O nome do arquivo pedido vem da URL, ou seja, é controlado por
 * quem faz a requisição. Sem validação, alguém poderia tentar pedir
 * algo como "/imagens/cardapio/../../db.properties" (path traversal)
 * para ler arquivos fora da pasta de uploads. Por isso o nome é
 * normalizado e CONFERIDO contra a pasta de uploads antes de ler
 * qualquer coisa — se o caminho resolvido não estiver DENTRO da
 * pasta esperada, a requisição é rejeitada com 404 em vez de vazar
 * qualquer arquivo do sistema.
 *
 * @author Sistema Integrador
 * @version 1.0
 * @see UploadImagemUtil
 */
@WebServlet("/imagens/cardapio/*")
public class ImagemServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String nomeArquivo = req.getPathInfo(); // ex: "/a1b2c3d4.jpg"
        if (nomeArquivo == null || nomeArquivo.length() <= 1) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        nomeArquivo = nomeArquivo.substring(1); // remove a "/" inicial

        try {
            Path pastaUploads = UploadImagemUtil.getPastaUploads().normalize();
            Path arquivo = pastaUploads.resolve(nomeArquivo).normalize();

            // ---- Proteção contra path traversal ----
            // Se o caminho resolvido "escapou" da pasta de uploads
            // (por causa de algo como "../" no nome pedido), rejeita
            // sem nem tentar ler o arquivo.
            if (!arquivo.startsWith(pastaUploads) || !Files.exists(arquivo) || !Files.isRegularFile(arquivo)) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // ---- Content-Type baseado na extensão ----
            String contentType = Files.probeContentType(arquivo);
            res.setContentType(contentType != null ? contentType : "application/octet-stream");

            // Cache de 1 dia no navegador — as imagens são imutáveis
            // (cada upload gera um nome de arquivo novo via UUID), então
            // não há risco de o navegador mostrar uma versão desatualizada.
            res.setHeader("Cache-Control", "public, max-age=86400");

            try (OutputStream out = res.getOutputStream()) {
                Files.copy(arquivo, out);
            }

        } catch (IOException e) {
            System.err.println("❌ Erro ao servir imagem '" + nomeArquivo + "': " + e.getMessage());
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
