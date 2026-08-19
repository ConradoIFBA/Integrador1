package br.com.restaurante.filter;

import java.io.IOException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;

/**
 * ================================================================
 * CHARSETFILTER — FORÇA UTF-8 EM TODA REQUISIÇÃO/RESPOSTA
 * ================================================================
 *
 * Filtro "de infraestrutura": não tem nenhuma regra de negócio, só
 * garante que TODA requisição que chega e TODA resposta que sai da
 * aplicação usem UTF-8 como codificação de caracteres. Sem isso,
 * acentos e caracteres especiais (ç, ã, é, ª, º, "—", emojis usados
 * nos logs, etc.) podem chegar corrompidos — problema clássico em
 * projetos Java EE que mexem com texto em português.
 *
 * MAPEAMENTO: @WebFilter("/*")
 *   Roda para TODA E QUALQUER requisição da aplicação — inclusive
 *   as públicas (/auth/login, /auth/cadastro) que não passam pelo
 *   AuthFilter. Faz sentido ser assim: encoding é uma preocupação
 *   transversal, não depende de o usuário estar logado ou não.
 *
 * POR QUE REQUEST *E* RESPONSE PRECISAM SER TRATADOS SEPARADAMENTE:
 *   - request.setCharacterEncoding(...) afeta como o SERVLET
 *     CONTAINER decodifica os parâmetros vindos do cliente (campos
 *     de formulário, query string). Se não for chamado ANTES de
 *     qualquer request.getParameter(...), o container já terá
 *     decodificado os bytes com o encoding padrão (geralmente
 *     ISO-8859-1 em muitos ambientes), e aí já era — não tem como
 *     "corrigir" depois. Por isso este filtro roda bem no início da
 *     cadeia, antes de qualquer Controller ler parâmetros.
 *   - response.setCharacterEncoding(...) afeta como o CONTAINER
 *     codifica o HTML/texto que a aplicação devolve ao navegador.
 *
 * O IF NO REQUEST (`if (req.getCharacterEncoding()==null)`):
 *   Só define o encoding se ainda não tiver sido definido por
 *   ninguém. Isso evita sobrescrever um encoding que porventura já
 *   tenha sido setado mais cedo por outro filtro/componente — na
 *   prática, como este é o único lugar que mexe nisso no projeto,
 *   o efeito é sempre "definir UTF-8", mas o guard deixa o filtro
 *   mais defensivo/reutilizável.
 *
 * PARÂMETRO DE INICIALIZAÇÃO ("encoding"):
 *   O filtro aceita um <init-param> chamado "encoding" (configurável
 *   via web.xml) para permitir trocar a codificação sem recompilar
 *   o código — mas se ninguém configurar nada, cai no padrão
 *   "UTF-8" (mais que suficiente para este projeto).
 *
 * ⚠️ REGISTRO DUPLICADO (ponto de atenção):
 *   Esta classe é anotada com @WebFilter("/*"), o que já basta para
 *   o Tomcat registrá-la automaticamente. PORÉM o web.xml do
 *   projeto TAMBÉM declara um <filter>/<filter-mapping> chamado
 *   "CharsetFilter" apontando pra esta mesma classe. Como o nome
 *   usado pela anotação (implícito, = FQCN da classe) não bate com
 *   o filter-name do web.xml, o container acaba enxergando DOIS
 *   filtros distintos — e este código roda duas vezes por
 *   requisição (visível nos logs do Tomcat, onde
 *   "CharsetFilter.doFilter" aparece duplicado na stack trace).
 *   Não quebra nada porque a lógica é idempotente (setar UTF-8 duas
 *   vezes tem o mesmo efeito de setar uma vez), mas é redundante.
 *   Para eliminar, basta remover o bloco <filter>/<filter-mapping>
 *   do web.xml e deixar só a anotação @WebFilter cuidando do
 *   registro.
 *
 * @author Sistema Integrador
 * @version 3.0
 * @see br.com.restaurante.filter.AuthFilter
 */
@WebFilter("/*")
public class CharsetFilter implements Filter {

    // Codificação efetivamente usada — carregada em init() a partir
    // do <init-param>, com fallback para "UTF-8".
    private String encoding;

    /**
     * Chamado UMA VEZ pelo container quando o filtro é instanciado
     * (no startup da aplicação, não a cada requisição).
     * Lê o parâmetro "encoding" configurado no web.xml, se existir;
     * caso contrário assume UTF-8.
     */
    @Override
    public void init(FilterConfig c) throws ServletException {
        encoding = c.getInitParameter("encoding");
        if (encoding == null) encoding = "UTF-8";
    }

    /**
     * Executado para CADA requisição que casa com o mapeamento
     * ("/*" → literalmente todas). Faz duas coisas e repassa a
     * requisição adiante na cadeia de filtros (chain.doFilter):
     *
     *   1. Garante o encoding de LEITURA da requisição (parâmetros
     *      de formulário/query string), só se ainda não tiver sido
     *      definido.
     *   2. Garante o encoding de ESCRITA da resposta (o HTML/texto
     *      que o servidor devolve).
     *
     * Não faz nenhuma validação de negócio nem decide se a
     * requisição pode ou não continuar — é estritamente um filtro
     * de "preparação" que sempre deixa a cadeia prosseguir.
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // Só define se ainda não tiver sido definido — evita
        // sobrescrever um encoding já configurado antes na cadeia.
        if (req.getCharacterEncoding() == null) {
            req.setCharacterEncoding(encoding);
        }

        // A resposta sempre recebe o encoding aqui, incondicional-
        // mente — não há motivo para "preservar" um valor anterior
        // neste caso, já que é este filtro quem define o padrão do
        // sistema.
        res.setCharacterEncoding(encoding);

        // Repassa a requisição/resposta para o próximo elo da
        // cadeia (próximo filtro ou, no fim, o Servlet/Controller
        // de destino). Sem esta chamada, a requisição "trava" aqui
        // e nunca chega ao destino.
        chain.doFilter(req, res);
    }

    /**
     * Chamado UMA VEZ quando o filtro é destruído (undeploy /
     * shutdown do container). Não há recursos para liberar aqui
     * (nenhuma conexão, arquivo ou thread aberta por este filtro),
     * então o método fica vazio propositalmente.
     */
    @Override
    public void destroy() {}
}
