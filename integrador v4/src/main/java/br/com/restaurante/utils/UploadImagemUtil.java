package br.com.restaurante.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.Part;

/**
 * ================================================================
 * UPLOADIMAGEMUTIL — SALVAR/VALIDAR/REMOVER FOTOS DO CARDÁPIO
 * ================================================================
 *
 * PROPÓSITO:
 * Centraliza tudo que envolve o arquivo físico da imagem de um item
 * do cardápio: onde ele é guardado no disco, como validar antes de
 * aceitar o upload, como gerar um nome de arquivo seguro, e como
 * apagar a foto antiga quando o gerente troca a imagem de um item.
 *
 * ⚠️ POR QUE AS IMAGENS FICAM FORA DA PASTA DO PROJETO (webapp/):
 * A primeira ideia óbvia seria salvar direto em
 * src/main/webapp/assets/uploads/ — mas isso NÃO funciona bem neste
 * projeto: o Tomcat do Eclipse publica o webapp numa pasta temporária
 * (.metadata/.plugins/.../tmp0/wtpwebapps/integrador/), e qualquer
 * "Clean" no servidor ou republicação do projeto APAGA essa pasta
 * temporária inteira — as fotos enviadas seriam perdidas no primeiro
 * "Clean Tomcat Work Directory" (que, aliás, é exatamente o passo que
 * resolveu o ClassNotFoundException dos filtros outro dia).
 *
 * Por isso as imagens são salvas numa pasta FORA do projeto e fora do
 * deploy do Tomcat: dentro da pasta pessoal do usuário do Windows
 * (System.getProperty("user.home")), em
 * "<user.home>/integrador-uploads/cardapio/". Essa pasta sobrevive a
 * qualquer Clean/republish/redeploy, porque o Tomcat nunca a apaga —
 * ele só apaga o que está dentro da própria pasta de deploy.
 *
 * Quem efetivamente "mostra" essas imagens no navegador é o
 * ImagemServlet (mapeado em /imagens/cardapio/*), que lê os bytes
 * dessa pasta e devolve como resposta HTTP — como a pasta está fora
 * do webapp, o Tomcat não serve os arquivos sozinho, por isso esse
 * servlet dedicado é necessário.
 *
 * VALIDAÇÕES APLICADAS:
 * - Extensão: só .jpg, .jpeg, .png ou .webp (outros tipos são
 *   rejeitados — evita, por exemplo, alguém tentar subir um .jsp ou
 *   .html disfarçado de imagem)
 * - Tamanho: no máximo 3 MB por arquivo
 *
 * NOME DO ARQUIVO SALVO:
 * Nunca usa o nome original enviado pelo navegador (isso seria um
 * risco de segurança — um nome de arquivo controlado pelo usuário
 * poderia conter caracteres especiais ou tentar "escapar" da pasta de
 * destino, um ataque conhecido como path traversal). Em vez disso,
 * gera um UUID aleatório + a extensão original — sempre único, sempre
 * seguro.
 *
 * @author Sistema Integrador
 * @version 1.0
 * @see br.com.restaurante.controller.ImagemServlet
 * @see br.com.restaurante.controller.CardapioController
 */
public class UploadImagemUtil {

    /** Extensões aceitas para upload — qualquer outra é rejeitada. */
    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp");

    /** Tamanho máximo por arquivo: 3 MB (em bytes). */
    private static final long TAMANHO_MAXIMO = 3L * 1024 * 1024;

    /**
     * Resolve (e garante que existe) a pasta onde as imagens do
     * cardápio ficam salvas. Chamado tanto para salvar quanto para
     * servir uma imagem, então a pasta é criada automaticamente na
     * primeira vez que for necessária — não precisa de nenhum passo
     * manual de configuração.
     */
    public static Path getPastaUploads() throws IOException {
        Path pasta = Paths.get(System.getProperty("user.home"), "integrador-uploads", "cardapio");
        if (!Files.exists(pasta)) {
            Files.createDirectories(pasta);
        }
        return pasta;
    }

    /**
     * Valida e salva o arquivo enviado no formulário, apagando a
     * imagem anterior (se houver) para não acumular arquivos órfãos
     * no disco a cada edição.
     *
     * @param parteImagem   o Part do multipart/form-data (campo "imagem")
     * @param imagemAntiga  nome do arquivo atual do item (para apagar
     *                      depois de salvar o novo com sucesso), ou
     *                      null/vazio se o item ainda não tinha foto
     * @return o nome do novo arquivo salvo, PRONTO para ser gravado
     *         na coluna cardapio.imagem — ou null se o campo veio
     *         vazio (usuário não selecionou nenhum arquivo, caso comum
     *         ao editar um item sem trocar a foto)
     * @throws IllegalArgumentException se o arquivo não passar nas
     *         validações (extensão ou tamanho) — a mensagem já vem
     *         pronta para ser exibida ao usuário
     * @throws IOException se houver falha ao gravar o arquivo em disco
     */
    public static String salvar(Part parteImagem, String imagemAntiga) throws IOException {

        // Nenhum arquivo selecionado (campo <input type="file"> vazio)
        // — comportamento normal ao editar um item sem mexer na foto,
        // não é um erro, só significa "não faça nada".
        if (parteImagem == null || parteImagem.getSize() == 0) {
            return null;
        }

        // ---- Validação de tamanho ----
        if (parteImagem.getSize() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException(
                "A imagem é muito grande (máximo 3 MB). Escolha um arquivo menor.");
        }

        // ---- Validação de extensão ----
        String nomeOriginal = parteImagem.getSubmittedFileName();
        String extensao = extrairExtensao(nomeOriginal);
        if (extensao == null || !EXTENSOES_PERMITIDAS.contains(extensao)) {
            throw new IllegalArgumentException(
                "Formato de imagem não permitido. Use JPG, PNG ou WEBP.");
        }

        // ---- Gera um nome de arquivo seguro e único ----
        String nomeArquivo = UUID.randomUUID().toString() + "." + extensao;
        Path destino = getPastaUploads().resolve(nomeArquivo);

        try (InputStream in = parteImagem.getInputStream()) {
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        }

        // ---- Remove a imagem antiga (best-effort) ----
        // Só depois que o novo arquivo já foi salvo com sucesso — se
        // algo desse errado salvando o novo, a imagem antiga
        // continuaria intacta em vez de perdermos as duas.
        if (imagemAntiga != null && !imagemAntiga.isEmpty()) {
            apagar(imagemAntiga);
        }

        return nomeArquivo;
    }

    /**
     * Remove o arquivo de imagem do disco, se existir. Chamado ao
     * substituir uma foto por outra (salvar()) ou quando quiser
     * limpar manualmente. Falhas ao apagar são apenas registradas no
     * console (best-effort) — um arquivo órfão que não pôde ser
     * apagado não deve impedir o restante da operação (ex: salvar o
     * item com a NOVA foto) de dar certo.
     */
    public static void apagar(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isEmpty()) return;
        try {
            Files.deleteIfExists(getPastaUploads().resolve(nomeArquivo));
        } catch (IOException e) {
            System.err.println("⚠️ Não foi possível apagar a imagem antiga '" + nomeArquivo + "': " + e.getMessage());
        }
    }

    /** Extrai a extensão (minúscula, sem o ponto) de um nome de arquivo, ou null se não tiver. */
    private static String extrairExtensao(String nomeArquivo) {
        if (nomeArquivo == null) return null;
        int pontoIdx = nomeArquivo.lastIndexOf('.');
        if (pontoIdx < 0 || pontoIdx == nomeArquivo.length() - 1) return null;
        return nomeArquivo.substring(pontoIdx + 1).toLowerCase();
    }
}
