package application;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

/**
 * Controller do Simulador de Posicionamento dos Painéis.
 * Aqui fica só a parte da TELA (ler campos, mostrar resultados, desenhar).
 * Os cálculos ficam em PosicionamentoPaineis.
 */
public class tela_controle_posicionamento {

	// ---- campos de entrada ----
	@FXML private TextField txtComprimento;
	@FXML private TextField txtLargura;
	@FXML private TextField txtMargem;

	// ---- desenho ----
	@FXML private Canvas canvasTelhado;

	// ---- resultados ----
	@FXML private Label lblAreaTotal;
	@FXML private Label lblAreaDisponivel;
	@FXML private Label lblQtdPaineis;
	@FXML private Label lblAreaOcupada;
	@FXML private Label lblPotencia;
	@FXML private Label lblOrientacao;
	@FXML private Label lblStatus;

	private final Dadosapp dados = Dadosapp.getInstancia();

	// cores da identidade visual
	private static final Color AZUL_ESCURO = Color.web("#06245C");
	private static final Color AZUL = Color.web("#0B4FD6");
	private static final Color AZUL_CLARO = Color.web("#2E8BFF");
	private static final Color MARGEM = Color.web("#DDE6F5");

	@FXML
	public void initialize() {
		desenharMensagemInicial();
		lblStatus.setText("");
	}

	// ====================================================================
	// BOTÃO "Simular posicionamento"
	// ====================================================================
	@FXML
	void simular(ActionEvent event) {
		try {
			double comprimento = lerNumero(txtComprimento, "Comprimento do telhado");
			double largura = lerNumero(txtLargura, "Largura do telhado");
			double margem = lerNumero(txtMargem, "Margem de segurança");

			PosicionamentoPaineis.Resultado r = PosicionamentoPaineis.simular(comprimento, largura, margem);

			mostrarResultados(r);
			desenhar(r);

		} catch (IllegalArgumentException e) {
			// todas as validações chegam aqui, com mensagem amigável
			Alert alerta = new Alert(Alert.AlertType.WARNING, e.getMessage());
			alerta.setTitle("Verifique os dados");
			alerta.setHeaderText(null);
			alerta.showAndWait();
		}
	}

	/** Lê um campo de texto e converte para número. Aceita vírgula ou ponto. */
	private double lerNumero(TextField campo, String nomeCampo) {
		String texto = (campo.getText() == null) ? "" : campo.getText().trim();

		if (texto.isEmpty()) {
			throw new IllegalArgumentException("Preencha o campo \"" + nomeCampo + "\".");
		}

		try {
			double valor = Double.parseDouble(texto.replace(",", "."));
			if (Double.isNaN(valor) || Double.isInfinite(valor)) {
				throw new NumberFormatException();
			}
			return valor;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(
					"O campo \"" + nomeCampo + "\" precisa ser um número (exemplo: 10,5).");
		}
	}

	// ====================================================================
	// RESULTADOS
	// ====================================================================
	private void mostrarResultados(PosicionamentoPaineis.Resultado r) {
		lblAreaTotal.setText(String.format("%.2f m²", r.areaTotal));
		lblAreaDisponivel.setText(String.format("%.2f m²", r.areaDisponivel));
		lblQtdPaineis.setText(String.valueOf(r.quantidade));
		lblAreaOcupada.setText(String.format("%.2f m²", r.areaOcupada));

		// potência nominal do painel vem da Dadosapp (não é duplicada aqui)
		double kwp = r.quantidade * Dadosapp.potencia_painel_kw;
		lblPotencia.setText(String.format("%.2f kWp", kwp));

		String disposicao = r.rotacionado ? "Rotacionada 90°" : "Normal";
		lblOrientacao.setText(String.format("%s (normal: %d | rotacionada: %d)",
				disposicao, r.qtdNormal, r.qtdRotacionada));

		mostrarStatus(r.quantidade);
	}

	/** Compara o que cabe no telhado com a quantidade recomendada pelo dimensionamento solar. */
	private void mostrarStatus(int quantidadeNoTelhado) {
		if (dados.consumoTotal() <= 0) {
			lblStatus.setText("Cadastre e calcule o consumo para comparar com a quantidade de painéis recomendada.");
			lblStatus.setStyle("-fx-text-fill: #3A4F7A; -fx-font-size: 18px;");
			return;
		}

		// reaproveita o cálculo que já existe na Dadosapp
		int recomendados = dados.calculoSolar().paineis;

		if (quantidadeNoTelhado >= recomendados) {
			lblStatus.setText(String.format(
					"O telhado comporta os %d painéis recomendados (cabem até %d).",
					recomendados, quantidadeNoTelhado));
			lblStatus.setStyle("-fx-text-fill: #1E8E3E; -fx-font-size: 18px; -fx-font-weight: bold;");
		} else {
			lblStatus.setText(String.format(
					"O telhado comporta apenas %d dos %d painéis recomendados (faltam %d).",
					quantidadeNoTelhado, recomendados, recomendados - quantidadeNoTelhado));
			lblStatus.setStyle("-fx-text-fill: #C62828; -fx-font-size: 18px; -fx-font-weight: bold;");
		}
	}

	// ====================================================================
	// DESENHO NO CANVAS (vista de cima, em escala)
	// ====================================================================
	private void desenhar(PosicionamentoPaineis.Resultado r) {
		GraphicsContext gc = canvasTelhado.getGraphicsContext2D();
		double larguraCanvas = canvasTelhado.getWidth();
		double alturaCanvas = canvasTelhado.getHeight();

		limparCanvas(gc, larguraCanvas, alturaCanvas);

		// ESCALA: pixels por metro. Usa a mesma para tudo, assim as proporções se mantêm.
		double espacoLateral = 70;
		double escala = Math.min(
				(larguraCanvas - 2 * espacoLateral) / r.comprimento,
				(alturaCanvas - 2 * espacoLateral) / r.largura);

		double larguraTelhado = r.comprimento * escala;
		double alturaTelhado = r.largura * escala;
		double x0 = (larguraCanvas - larguraTelhado) / 2;
		double y0 = (alturaCanvas - alturaTelhado) / 2;
		double margem = r.margem * escala;

		// 1) telhado inteiro (a faixa de margem aparece em cinza-azulado)
		gc.setFill(MARGEM);
		gc.fillRect(x0, y0, larguraTelhado, alturaTelhado);
		gc.setStroke(AZUL_ESCURO);
		gc.setLineWidth(3);
		gc.strokeRect(x0, y0, larguraTelhado, alturaTelhado);

		// 2) área útil (branca, com borda tracejada)
		double xUtil = x0 + margem;
		double yUtil = y0 + margem;
		double larguraUtil = larguraTelhado - 2 * margem;
		double alturaUtil = alturaTelhado - 2 * margem;

		gc.setFill(Color.WHITE);
		gc.fillRect(xUtil, yUtil, larguraUtil, alturaUtil);
		gc.setStroke(AZUL);
		gc.setLineWidth(1.5);
		gc.setLineDashes(8, 6);
		gc.strokeRect(xUtil, yUtil, larguraUtil, alturaUtil);
		gc.setLineDashes((double[]) null);

		// 3) painéis, centralizados dentro da área útil
		double pw = r.larguraPainel * escala;
		double ph = r.alturaPainel * escala;
		double esp = r.espacamento * escala;

		double larguraGrade = r.colunas * pw + (r.colunas - 1) * esp;
		double alturaGrade = r.linhas * ph + (r.linhas - 1) * esp;
		double xInicio = xUtil + (larguraUtil - larguraGrade) / 2;
		double yInicio = yUtil + (alturaUtil - alturaGrade) / 2;

		// tamanho do número dentro do painel acompanha o tamanho do painel
		double tamanhoFonte = Math.max(9, Math.min(24, Math.min(pw, ph) * 0.35));
		gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, tamanhoFonte));
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setTextBaseline(VPos.CENTER);

		int numero = 1;
		for (int linha = 0; linha < r.linhas; linha++) {
			for (int coluna = 0; coluna < r.colunas; coluna++) {
				double x = xInicio + coluna * (pw + esp);
				double y = yInicio + linha * (ph + esp);

				gc.setFill(AZUL_ESCURO);
				gc.fillRect(x, y, pw, ph);
				gc.setStroke(AZUL_CLARO);
				gc.setLineWidth(1);
				gc.strokeRect(x, y, pw, ph);

				// identificação do painel
				gc.setFill(Color.WHITE);
				gc.fillText(String.valueOf(numero), x + pw / 2, y + ph / 2);
				numero++;
			}
		}

		// 4) medidas do telhado
		gc.setFill(AZUL_ESCURO);
		gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setTextBaseline(VPos.CENTER);
		gc.fillText(String.format("%.2f m", r.comprimento), larguraCanvas / 2, y0 - 24);

		gc.save();
		gc.translate(x0 - 28, alturaCanvas / 2);
		gc.rotate(-90);
		gc.fillText(String.format("%.2f m", r.largura), 0, 0);
		gc.restore();
	}

	private void limparCanvas(GraphicsContext gc, double largura, double altura) {
		gc.setFill(Color.WHITE);
		gc.fillRect(0, 0, largura, altura);
	}

	private void desenharMensagemInicial() {
		GraphicsContext gc = canvasTelhado.getGraphicsContext2D();
		double largura = canvasTelhado.getWidth();
		double altura = canvasTelhado.getHeight();

		limparCanvas(gc, largura, altura);

		gc.setFill(Color.web("#3A4F7A"));
		gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 22));
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setTextBaseline(VPos.CENTER);
		gc.fillText("Informe as dimensões do telhado e clique em \"Simular posicionamento\".",
				largura / 2, altura / 2);
	}

	// ====================================================================
	// NAVEGAÇÃO
	// ====================================================================
	@FXML
	void voltar(ActionEvent event) {
		try {
			// ajuste o nome se a sua tela de placas tiver outro arquivo
			Parent root = FXMLLoader.load(getClass().getResource("/application/tela_placa_sola.fxml"));
			Scene scene = new Scene(root);
			Stage janela = (Stage) ((Node) event.getSource()).getScene().getWindow();
			janela.setScene(scene);
			janela.setTitle("Energia Solar");
			janela.centerOnScreen();
			janela.show();
		} catch (Exception e) {
			e.printStackTrace();
			new Alert(Alert.AlertType.ERROR, "Não foi possível voltar para a tela anterior.").showAndWait();
		}
	}
}