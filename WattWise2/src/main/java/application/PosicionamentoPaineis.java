package application;

/**
 * Lógica de posicionamento dos painéis no telhado. NÃO usa JavaFX: só matemática,
 * por isso pode ser testada separada da tela.
 *
 * Reaproveita as constantes que ficam na Dadosapp (dimensões e espaçamento do painel).
 *
 * Ideia do cálculo: em cada orientação, os painéis formam uma grade. Em uma linha
 * com espaço E, painéis de tamanho T e espaçamento S cabem:
 *     n = floor((E + S) / (T + S))
 * (o "+ S" existe porque não há espaçamento depois do último painel).
 * A quantidade total é colunas x linhas, e escolhe-se a orientação com mais painéis.
 */
public final class PosicionamentoPaineis {

	// tolerância para erros de arredondamento de números decimais
	private static final double EPS = 1e-9;

	private PosicionamentoPaineis() { }

	/** Tudo o que a tela precisa para mostrar e desenhar o resultado. */
	public static class Resultado {
		// dados do telhado (metros)
		public final double comprimento;
		public final double largura;
		public final double margem;

		// áreas (m²)
		public final double areaTotal;
		public final double areaDisponivel;
		public final double areaOcupada;

		// disposição escolhida
		public final boolean rotacionado;   // true = painéis girados 90°
		public final int colunas;           // painéis ao longo do comprimento
		public final int linhas;            // painéis ao longo da largura
		public final int quantidade;        // colunas x linhas
		public final double larguraPainel;  // medida do painel no sentido do comprimento (m)
		public final double alturaPainel;   // medida do painel no sentido da largura (m)
		public final double espacamento;    // espaço entre painéis (m)

		// comparação entre as duas orientações
		public final int qtdNormal;
		public final int qtdRotacionada;

		Resultado(double comprimento, double largura, double margem,
				double areaTotal, double areaDisponivel, double areaOcupada,
				boolean rotacionado, int colunas, int linhas,
				double larguraPainel, double alturaPainel, double espacamento,
				int qtdNormal, int qtdRotacionada) {
			this.comprimento = comprimento;
			this.largura = largura;
			this.margem = margem;
			this.areaTotal = areaTotal;
			this.areaDisponivel = areaDisponivel;
			this.areaOcupada = areaOcupada;
			this.rotacionado = rotacionado;
			this.colunas = colunas;
			this.linhas = linhas;
			this.quantidade = colunas * linhas;
			this.larguraPainel = larguraPainel;
			this.alturaPainel = alturaPainel;
			this.espacamento = espacamento;
			this.qtdNormal = qtdNormal;
			this.qtdRotacionada = qtdRotacionada;
		}
	}

	/**
	 * Faz a simulação.
	 *
	 * @throws IllegalArgumentException com mensagem amigável se os dados forem inválidos
	 */
	public static Resultado simular(double comprimento, double largura, double margem) {

		validarMedida(comprimento, "O comprimento do telhado");
		validarMedida(largura, "A largura do telhado");

		if (Double.isNaN(margem) || Double.isInfinite(margem)) {
			throw new IllegalArgumentException("A margem de segurança precisa ser um número válido.");
		}
		if (margem < 0) {
			throw new IllegalArgumentException("A margem de segurança não pode ser negativa.");
		}

		// a margem existe nos DOIS lados de cada dimensão
		double comprimentoUtil = comprimento - 2 * margem;
		double larguraUtil = largura - 2 * margem;

		if (comprimentoUtil <= 0 || larguraUtil <= 0) {
			throw new IllegalArgumentException(String.format(
					"A margem de segurança (%.2f m em cada lado) é grande demais para um telhado de %.2f m x %.2f m.",
					margem, comprimento, largura));
		}
		
		// Precisa cirar essas informações na classe Dadosapp
		double lado1 = Dadosapp.PAINEL_COMPRIMENTO_M;   // 2,15 m
		double lado2 = Dadosapp.PAINEL_LARGURA_M;       // 1,20 m
		double esp = Dadosapp.ESPACAMENTO_PAINEIS_M;

		// Orientação NORMAL: o lado maior do painel acompanha o comprimento do telhado
		int colunasNormal = quantosCabem(comprimentoUtil, lado1, esp);
		int linhasNormal = quantosCabem(larguraUtil, lado2, esp);
		int qtdNormal = colunasNormal * linhasNormal;

		// Orientação ROTACIONADA (90°): o lado menor acompanha o comprimento
		int colunasRot = quantosCabem(comprimentoUtil, lado2, esp);
		int linhasRot = quantosCabem(larguraUtil, lado1, esp);
		int qtdRotacionada = colunasRot * linhasRot;

		if (qtdNormal == 0 && qtdRotacionada == 0) {
			throw new IllegalArgumentException(String.format(
					"A área disponível (%.2f m x %.2f m) é pequena demais para comportar um painel de %.2f m x %.2f m.",
					comprimentoUtil, larguraUtil, lado1, lado2));
		}

		// escolhe a disposição com mais painéis (em empate, fica a normal)
		boolean rotacionado = qtdRotacionada > qtdNormal;
		int colunas = rotacionado ? colunasRot : colunasNormal;
		int linhas = rotacionado ? linhasRot : linhasNormal;
		double larguraPainel = rotacionado ? lado2 : lado1;
		double alturaPainel = rotacionado ? lado1 : lado2;

		int quantidade = colunas * linhas;

		return new Resultado(
				comprimento, largura, margem,
				comprimento * largura,
				comprimentoUtil * larguraUtil,
				quantidade * Dadosapp.PAINEL_AREA_M2,
				rotacionado, colunas, linhas,
				larguraPainel, alturaPainel, esp,
				qtdNormal, qtdRotacionada);
	}

	/** Quantos painéis de tamanho 'tamanho' cabem em 'espaco', com 'esp' entre eles. */
	private static int quantosCabem(double espaco, double tamanho, double esp) {
		if (espaco + EPS < tamanho) {
			return 0;
		}
		return (int) Math.floor((espaco + esp) / (tamanho + esp) + EPS);
	}

	private static void validarMedida(double valor, String nome) {
		if (Double.isNaN(valor) || Double.isInfinite(valor)) {
			throw new IllegalArgumentException(nome + " precisa ser um número válido.");
		}
		if (valor < 0) {
			throw new IllegalArgumentException(nome + " não pode ser negativo.");
		}
		if (valor == 0) {
			throw new IllegalArgumentException(nome + " precisa ser maior que zero.");
		}
	}
}