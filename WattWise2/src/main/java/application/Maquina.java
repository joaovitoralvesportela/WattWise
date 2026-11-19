package application;

public class Maquina {

	private final int id;
	private final String nome;
	private final double consumoKw;     // consumo de UMA unidade, em kW
	private final int quantidade;       // quantas unidades dessa máquina existem
	private final String caminhoImagem; // ex.: "/application/imagens/geladeira.png"

	// Construtor completo: usado ao ler do banco de dados
	public Maquina(int id, String nome, double consumoKw, int quantidade, String caminhoImagem) {
		this.id = id;
		this.nome = nome;
		this.consumoKw = consumoKw;
		this.quantidade = quantidade;
		this.caminhoImagem = caminhoImagem;
	}

	// Construtor antigo: mantém funcionando o código que já usa new Maquina(nome, kw)
	public Maquina(String nome, double consumoKw) {
		this(0, nome, consumoKw, 1, null);
	}

	public int getId() { return id; }
	public String getNome() { return nome; }
	public double getConsumoKw() { return consumoKw; }
	public int getQuantidade() { return quantidade; }
	public String getCaminhoImagem() { return caminhoImagem; }

	// consumo somando todas as unidades
	public double getConsumoTotalKw() { return consumoKw * quantidade; }

	// o ComboBox usa o toString para mostrar o item
	@Override
	public String toString() { return nome; }
}