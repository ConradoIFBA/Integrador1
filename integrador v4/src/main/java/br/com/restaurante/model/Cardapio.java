package br.com.restaurante.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Cardapio implements Serializable {
	private static final long serialVersionUID = 1L;
	private int idItem;
	private int categoriaId;
	private CategoriaItem categoria;
	private String nome;
	private String descricao;
	private BigDecimal preco;
	private int tempoPreparoMin;
	private boolean disponivel;
	private boolean ativo;
	// Nome do arquivo salvo em disco (ex: "a1b2c3d4.jpg"), não o
	// caminho completo — o caminho é resolvido por UploadImagemUtil e
	// pelo ImagemServlet na hora de servir a imagem. Fica null/vazio
	// para itens que ainda não tiveram foto cadastrada (nesse caso as
	// telas exibem um bloco decorativo no lugar, ver cardapio.jsp).
	private String imagem;

	public Cardapio() {
	}

	public Cardapio(int id, int catId, String nome, String desc, BigDecimal preco, int tempo, boolean disp,
			boolean ativo) {
		this.idItem = id;
		this.categoriaId = catId;
		this.nome = nome;
		this.descricao = desc;
		this.preco = preco;
		this.tempoPreparoMin = tempo;
		this.disponivel = disp;
		this.ativo = ativo;
	}

	public int getIdItem() {
		return idItem;
	}

	public void setIdItem(int v) {
		idItem = v;
	}

	public int getCategoriaId() {
		return categoriaId;
	}

	public void setCategoriaId(int v) {
		categoriaId = v;
	}

	public CategoriaItem getCategoria() {
		return categoria;
	}

	public void setCategoria(CategoriaItem v) {
		categoria = v;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String v) {
		nome = v;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String v) {
		descricao = v;
	}

	public BigDecimal getPreco() {
		return preco;
	}

	public void setPreco(BigDecimal v) {
		preco = v;
	}

	public int getTempoPreparoMin() {
		return tempoPreparoMin;
	}

	public void setTempoPreparoMin(int v) {
		tempoPreparoMin = v;
	}

	public boolean isDisponivel() {
		return disponivel;
	}

	public void setDisponivel(boolean v) {
		disponivel = v;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public void setAtivo(boolean v) {
		ativo = v;
	}

	public String getImagem() {
		return imagem;
	}

	public void setImagem(String v) {
		imagem = v;
	}

	/**
	 * Atalho usado nas JSPs para saber se o item tem foto cadastrada,
	 * sem precisar de um teste de string vazia espalhado em várias
	 * telas (${not empty item.imagem} já funciona também, mas este
	 * método deixa a intenção mais explícita onde for chamado do Java).
	 */
	public boolean isTemImagem() {
		return imagem != null && !imagem.isEmpty();
	}

	public String getNomeCategoria() {
		return categoria != null ? categoria.getNome() : "";
	}

	@Override
	public String toString() {
		return "ItemCardapio[id=" + idItem + ",nome=" + nome + "]";
	}
}
