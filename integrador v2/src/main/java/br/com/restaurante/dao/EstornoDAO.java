package br.com.restaurante.dao;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import br.com.restaurante.model.Estorno;
public class EstornoDAO {
    private final Connection conexao;
    public EstornoDAO(Connection c){this.conexao=c;}
    public void registrar(Estorno e) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "INSERT INTO estorno(pedido_id,valor_estornado,tipo_estorno,forma_pagamento,motivo,identificador_operador) VALUES(?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS)){
            s.setInt(1,e.getPedidoId());s.setBigDecimal(2,e.getValorEstornado());
            s.setString(3,e.getTipoEstorno());s.setString(4,e.getFormaPagamento());
            s.setString(5,e.getMotivo());s.setString(6,e.getIdentificadorOperador());s.executeUpdate();
            try(ResultSet r=s.getGeneratedKeys()){if(r.next())e.setIdEstorno(r.getInt(1));}
        }
    }
    public List<Estorno> listarPorPedido(int pedidoId) throws SQLException {
        List<Estorno> l=new ArrayList<>();
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT * FROM estorno WHERE pedido_id=? ORDER BY data_hora ASC")){
            s.setInt(1,pedidoId);try(ResultSet r=s.executeQuery()){while(r.next())l.add(mapear(r));}
        } return l;
    }
    public BigDecimal somarEstornosPorPedido(int pedidoId) throws SQLException {
        try(PreparedStatement s=conexao.prepareStatement(
            "SELECT COALESCE(SUM(valor_estornado),0) AS total FROM estorno WHERE pedido_id=?")){
            s.setInt(1,pedidoId);try(ResultSet r=s.executeQuery()){if(r.next())return r.getBigDecimal("total");}
        } return BigDecimal.ZERO;
    }
    private Estorno mapear(ResultSet r) throws SQLException {
        return new Estorno(r.getInt("id_estorno"),r.getInt("pedido_id"),r.getBigDecimal("valor_estornado"),
            r.getString("tipo_estorno"),r.getString("forma_pagamento"),r.getString("motivo"),
            r.getTimestamp("data_hora").toLocalDateTime(),r.getString("identificador_operador"));
    }
}
