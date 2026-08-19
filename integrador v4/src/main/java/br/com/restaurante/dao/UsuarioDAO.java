package br.com.restaurante.dao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import br.com.restaurante.model.Usuario;

/**
 * ================================================================
 * USUARIO DAO - Acesso à tabela "usuario" (schema v3)
 * ================================================================
 *
 * PROPÓSITO:
 * Centraliza o SQL da tabela "usuario" — a base de autenticação de
 * todo o sistema (GERENTE, FUNCIONARIO e USUARIO/cliente). Usado por
 * AuthController (login/cadastro) e indiretamente por todos os
 * demais controllers, que leem o usuário logado da sessão HTTP (não
 * deste DAO diretamente, exceto no momento do login).
 *
 * ⚠️ NENHUMA MUDANÇA DE SCHEMA V2→V3 AFETA ESTE DAO:
 * A tabela usuario e todas as suas colunas mantiveram os mesmos
 * nomes e tamanhos entre v2 e v3 — as mudanças desta versão
 * (item_cardapio→cardapio, identificador_operador ampliado) não
 * tocam a tabela usuario.
 *
 * TABELA: usuario
 * Schema (ver integrador_v3.sql):
 * - id_usuario  (PK, AUTO_INCREMENT)
 * - nome
 * - login        (UNIQUE)
 * - senha         (hash BCrypt, gerado/verificado em AuthController)
 * - perfil         (ENUM: GERENTE, FUNCIONARIO, USUARIO)
 * - funcao          (ENUM: atendente, cozinha — NULLABLE, só relevante
 *                    para perfil FUNCIONARIO; embora, a partir do v3
 *                    dos controllers, o sistema não diferencie mais
 *                    atendente de cozinha operacionalmente — ver
 *                    AuthController.destino() e FilaController/
 *                    PedidoController.temPermissao(), que tratam
 *                    qualquer FUNCIONARIO igual)
 * - ativo            (TINYINT(1), default 1 — soft delete)
 *
 * MÉTODOS DISPONÍVEIS:
 * - buscarPorLogin(login)  → usado no login (AuthController.processarLogin)
 * - buscarPorId(id)         → busca direta por id
 * - inserir(usuario)        → cria um novo usuário (cadastro público sempre
 *                             perfil USUARIO — a restrição de perfil é
 *                             aplicada no controller, não neste DAO)
 * - editar(usuario)         → atualiza nome/senha/funcao
 * - desativar(id)           → soft delete
 *
 * ⚠️ SEGURANÇA — RESPONSABILIDADE DO DAO vs DO CONTROLLER:
 * Este DAO é "burro" de propósito: ele insere/edita exatamente o que
 * recebe, sem aplicar regras de negócio. Quem garante que o cadastro
 * público só cria usuários com perfil="USUARIO", que senhas chegam
 * já com hash BCrypt aplicado, etc., é o AuthController — este DAO
 * confia que o objeto Usuario recebido já está validado e correto.
 *
 * @author Sistema Integrador
 * @version 3.0
 * @see Usuario
 */
public class UsuarioDAO {

    // ---- Conexão injetada via construtor (padrão usado em todo o projeto) ----
    private final Connection conexao;

    /**
     * Construtor — recebe a Connection já aberta (gerenciada pelo
     * controller via try-with-resources).
     */
    public UsuarioDAO(Connection c){
        this.conexao=c;
    }

    /* ================================================================
       BUSCAR USUÁRIO POR LOGIN
       ================================================================

       Usado no fluxo de autenticação (AuthController.processarLogin())
       para localizar o usuário pelo login informado, antes de
       comparar a senha com BCrypt.checkpw(). Também usado em
       processarCadastro() para verificar se um login já está em uso
       antes de permitir um novo cadastro.

       Filtra por ativo=1 — usuários desativados não conseguem mais
       fazer login, mesmo com login/senha corretos.
    */
    public Usuario buscarPorLogin(String login) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM usuario WHERE login=? AND ativo=1")){
            s.setString(1,login);
            try(ResultSet r=s.executeQuery()){
                if(r.next()) return mapear(r);
            }
        }
        return null;
    }

    /* ================================================================
       BUSCAR USUÁRIO POR ID
       ================================================================

       Busca direta pelo id_usuario — útil para recarregar dados
       atualizados do usuário (ex: após uma edição de perfil), já que
       o objeto guardado na sessão HTTP pode ficar desatualizado até
       um novo login.
    */
    public Usuario buscarPorId(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM usuario WHERE id_usuario=? AND ativo=1")){
            s.setInt(1,id);
            try(ResultSet r=s.executeQuery()){
                if(r.next()) return mapear(r);
            }
        }
        return null;
    }

    /* ================================================================
       LISTAR STAFF (GERENTE + FUNCIONARIO)
       ================================================================

       Usado pela nova tela de gerenciamento de funcionários
       (UsuarioController, só GERENTE) — NÃO inclui contas com
       perfil=USUARIO (clientes), que continuam sendo geridas só
       indiretamente (autocadastro público, sem tela administrativa
       dedicada — fora do escopo pedido).

       Ordena por perfil (GERENTE antes de FUNCIONARIO) e depois por
       nome, para a lista ficar organizada por "nível" sem precisar
       de nenhum agrupamento visual complexo na JSP.
    */
    public List<Usuario> listarStaff() throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM usuario WHERE perfil IN ('GERENTE','FUNCIONARIO') AND ativo=1 " +
            "ORDER BY perfil, nome")){
            try(ResultSet r=s.executeQuery()){
                while(r.next()) lista.add(mapear(r));
            }
        }
        return lista;
    }

    /* ================================================================
       INSERIR NOVO USUÁRIO
       ================================================================

       Cria o usuário sempre com ativo=1 (fixo no SQL). Espera que o
       objeto Usuario recebido já venha com:
       - senha JÁ com hash BCrypt aplicado (nunca texto plano —
         responsabilidade de AuthController.processarCadastro())
       - perfil já definido corretamente pelo chamador (o cadastro
         público sempre força "USUARIO" no controller, nunca aqui)

       Usa Statement.RETURN_GENERATED_KEYS para recuperar o
       id_usuario gerado e devolvê-lo no objeto recebido.
    */
    public void inserir(Usuario u) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO usuario(nome,login,senha,perfil,funcao,ativo) VALUES(?,?,?,?,?,1)",
            Statement.RETURN_GENERATED_KEYS)){
            s.setString(1,u.getNome());
            s.setString(2,u.getLogin());
            s.setString(3,u.getSenha());
            s.setString(4,u.getPerfil());
            s.setString(5,u.getFuncao());
            s.executeUpdate();

            // ---- Recupera o id_usuario gerado e devolve no objeto ----
            try(ResultSet r=s.getGeneratedKeys()){
                if(r.next()) u.setIdUsuario(r.getInt(1));
            }
        }
    }

    /* ================================================================
       EDITAR USUÁRIO
       ================================================================

       Atualiza APENAS nome, senha e funcao. NÃO atualiza login nem
       perfil — por design: o login é a chave de identificação única
       do usuário (mudar poderia causar confusão/colisão), e o perfil
       é uma decisão de permissão que não deveria ser alterável por
       uma edição comum de dados pessoais (evita escalonamento de
       privilégio via uma tela de "editar meus dados", por exemplo).

       ⚠️ Se a senha estiver sendo alterada, espera-se que o chamador
       já tenha aplicado o hash BCrypt antes de passar o objeto para
       este método — o DAO não faz criptografia.
    */
    public void editar(Usuario u) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE usuario SET nome=?,senha=?,funcao=? WHERE id_usuario=?")){
            s.setString(1,u.getNome());
            s.setString(2,u.getSenha());
            s.setString(3,u.getFuncao());
            s.setInt(4,u.getIdUsuario());
            s.executeUpdate();
        }
    }

    /* ================================================================
       DESATIVAR USUÁRIO (SOFT DELETE)
       ================================================================

       Marca ativo=0 — nunca DELETE físico, preservando o vínculo
       histórico: pedidos e mesas referenciam usuários indiretamente
       via identificador_operador/operador (String, não FK), então
       tecnicamente um DELETE físico não quebraria integridade
       referencial, mas o soft delete é mantido por consistência com
       o padrão usado em todo o resto do sistema, e porque
       buscarPorLogin()/buscarPorId() já filtram por ativo=1 — um
       usuário desativado simplesmente não consegue mais autenticar.
    */
    public void desativar(int id) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "UPDATE usuario SET ativo=0 WHERE id_usuario=?")){
            s.setInt(1,id);
            s.executeUpdate();
        }
    }

    /* ================================================================
       HELPER: MAPEAR RESULTSET → OBJETO Usuario
       ================================================================

       Conversão direta via construtor com todos os campos, incluindo
       "funcao" (que pode ser NULL no banco para perfis GERENTE/
       USUARIO — r.getString() já retorna null nesse caso
       naturalmente, sem necessidade de tratamento especial como em
       colunas numéricas nullable).
    */
    private Usuario mapear(ResultSet r) throws SQLException {
        return new Usuario(
                r.getInt("id_usuario"),
                r.getString("nome"),
                r.getString("login"),
                r.getString("senha"),
                r.getString("perfil"),
                r.getString("funcao"),
                r.getBoolean("ativo"));
    }
}

/* ================================================================
   RESUMO DO DAO
   ================================================================

   TABELA: usuario (nenhuma mudança entre v2 e v3)

   MÉTODOS:
   1. buscarPorLogin(login) → usado na autenticação (login e cadastro)
   2. buscarPorId(id)        → busca direta por id
   3. inserir(usuario)       → cria (sempre ativo=1)
   4. editar(usuario)        → atualiza só nome/senha/funcao
   5. desativar(id)          → soft delete

   AJUSTES DO SCHEMA V3:
   ✅ Nenhum — esta tabela não foi afetada pelas mudanças v2→v3

   CAMPOS PROPOSITALMENTE NÃO EDITÁVEIS (editar()):
   ✅ login  → chave de identificação única, não deve mudar
   ✅ perfil → decisão de permissão, não deve ser alterável por
      edição comum (evita escalonamento de privilégio)

   SEGURANÇA — DIVISÃO DE RESPONSABILIDADE:
   ✅ Este DAO NÃO aplica hash de senha nem valida regras de negócio
      (ex: forçar perfil=USUARIO no cadastro público) — isso é
      responsabilidade do AuthController, que já entrega ao DAO um
      objeto Usuario pronto para persistir

   DEPENDÊNCIAS:
   - Usuario: model
   - Usado por AuthController (login, cadastro)

   OBSERVAÇÕES GERAIS:
   - Todas as queries usam PreparedStatement (proteção contra SQL
     injection)
   - Conexão é injetada via construtor e gerenciada pelo chamador
   - buscarPorLogin()/buscarPorId() sempre filtram por ativo=1 —
     usuários desativados não autenticam
   ================================================================ */
