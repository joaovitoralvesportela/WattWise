package application;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/**
 * Tela de monitoramento da placa solar em tempo real.
 *
 * Lê as propriedades da ArduinoSerialService (tensão, corrente, potência, energia)
 * e desenha no gráfico: tempo em segundos embaixo, valor medido à esquerda.
 */
public class tela_controle_monitoramento {

	// ---- gráfico ----
	@FXML private LineChart<Number, Number> graficoEnergia;
	@FXML private NumberAxis eixoTempo;
	@FXML private NumberAxis eixoValor;

	// ---- controles ----
	@FXML private ComboBox<String> cbPorta;
	@FXML private ComboBox<String> cbMetrica;
	@FXML private Button btnConectar;
	@FXML private Button btnSimular;
	@FXML private Button btnLimpar;

	// ---- cartões de valores ----
	@FXML private Label lblTensao;
	@FXML private Label lblCorrente;
	@FXML private Label lblPotencia;
	@FXML private Label lblEnergia;

	// ---- cartão de conexão ----
	@FXML private Circle circStatus;
	@FXML private Label lblStatus;
	@FXML private Label lblUltimaLeitura;

	// quanto tempo cabe na tela (segundos) e o passo em que o eixo "anda"
	private static final double JANELA_SEGUNDOS = 60;
	private static final double PASSO_EIXO = 10;
	private static final String AUTOMATICO = "Automático";

	/** O que o gráfico pode mostrar no eixo da esquerda. */
	private enum Metrica {
		POTENCIA("Potência (W)") {
			@Override double valor(LeituraSolar l) { return l.getPotenciaW(); }
		},
		TENSAO("Tensão (V)") {
			@Override double valor(LeituraSolar l) { return l.getTensaoV(); }
		},
		CORRENTE("Corrente (mA)") {
			@Override double valor(LeituraSolar l) { return l.getCorrenteMa(); }
		};

		final String rotulo;

		Metrica(String rotulo) {
			this.rotulo = rotulo;
		}

		abstract double valor(LeituraSolar leitura);
	}

	private final ArduinoSerialService servico = ArduinoSerialService.getInstancia();
	private final HistoricoLeituras historico = new HistoricoLeituras(JANELA_SEGUNDOS + PASSO_EIXO);
	private final SimuladorSolar simulador = new SimuladorSolar();
	private final XYChart.Series<Number, Number> serie = new XYChart.Series<>();

	private Metrica metrica = Metrica.POTENCIA;
	private long inicioMs = -1;   // instante da primeira leitura = 0 segundo do gráfico

	// Ouvintes guardados em campos para poderem ser REMOVIDOS ao sair da tela.
	// (se não removê-los, cada vez que a tela abre eles se acumulam)
	private final ChangeListener<Number> ouvinteLeitura = (obs, antigo, novo) -> novaLeitura();

	// a ArduinoSerialService muda "conectado" também fora da thread da tela, por isso runLater
	private final ChangeListener<Boolean> ouvinteConexao = (obs, antigo, novo) -> Platform.runLater(this::atualizarStatus);

	// ====================================================================
	// INÍCIO
	// ====================================================================
	@FXML
	public void initialize() {
		graficoEnergia.getData().add(serie);
		if (serie.getNode() != null) {
			serie.getNode().setStyle("-fx-stroke: #0B4FD6; -fx-stroke-width: 3px;");
		}

		// opções do eixo da esquerda
		for (Metrica m : Metrica.values()) {
			cbMetrica.getItems().add(m.rotulo);
		}
		cbMetrica.getSelectionModel().select(0);
		cbMetrica.getSelectionModel().selectedIndexProperty().addListener((obs, antigo, novo) -> {
			if (novo.intValue() >= 0) {
				mudarMetrica(Metrica.values()[novo.intValue()]);
			}
		});

		// lista as portas e atualiza a lista toda vez que o usuário abre o seletor
		carregarPortas();
		cbPorta.setOnShowing(e -> carregarPortas());

		ajustarEixoTempo(0);
		atualizarStatus();

		servico.timestampLeituraProperty().addListener(ouvinteLeitura);
		servico.conectadoProperty().addListener(ouvinteConexao);
	}

	// ====================================================================
	// LEITURA NOVA: atualiza os cartões e o gráfico
	// ====================================================================
	private void novaLeitura() {
		long agora = servico.timestampLeituraProperty().get();
		if (inicioMs < 0) {
			inicioMs = agora;
		}
		double segundos = (agora - inicioMs) / 1000.0;

		LeituraSolar leitura = new LeituraSolar(
				segundos,
				servico.tensaoVoltsProperty().get(),
				servico.correnteMaProperty().get(),
				servico.potenciaWattsProperty().get());
		historico.adicionar(leitura);

		// cartões
		lblTensao.setText(String.format("%.2f V", leitura.getTensaoV()));
		lblCorrente.setText(String.format("%.1f mA", leitura.getCorrenteMa()));
		lblPotencia.setText(String.format("%.3f W", leitura.getPotenciaW()));
		// a placa do teste gera pouco: em Wh o número fica mais legível que em kWh
		lblEnergia.setText(String.format("%.4f Wh", servico.energiaGeradaKwhProperty().get() * 1000));
		lblUltimaLeitura.setText("Última leitura: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

		// gráfico
		serie.getData().add(new XYChart.Data<Number, Number>(segundos, metrica.valor(leitura)));
		double limiteEsquerdo = ajustarEixoTempo(segundos);
		serie.getData().removeIf(d -> d.getXValue().doubleValue() < limiteEsquerdo);
	}

	/**
	 * Faz o eixo do tempo "andar" de 10 em 10 segundos, sempre com números redondos
	 * (0, 10, 20...). Devolve o limite esquerdo atual.
	 */
	private double ajustarEixoTempo(double segundos) {
		double esquerdo = Math.max(0, Math.floor((segundos - JANELA_SEGUNDOS) / PASSO_EIXO) * PASSO_EIXO);
		eixoTempo.setLowerBound(esquerdo);
		eixoTempo.setUpperBound(esquerdo + JANELA_SEGUNDOS + PASSO_EIXO);
		return esquerdo;
	}

	/** Troca o que o gráfico mostra (W, V ou mA) e redesenha com o histórico que já existe. */
	private void mudarMetrica(Metrica nova) {
		metrica = nova;
		eixoValor.setLabel(nova.rotulo);

		List<XYChart.Data<Number, Number>> pontos = new ArrayList<>();
		for (LeituraSolar l : historico.getLeituras()) {
			if (l.getSegundos() >= eixoTempo.getLowerBound()) {
				pontos.add(new XYChart.Data<Number, Number>(l.getSegundos(), nova.valor(l)));
			}
		}
		serie.getData().setAll(pontos);
	}

	// ====================================================================
	// BOTÕES
	// ====================================================================

	@FXML
	void alternarConexao(ActionEvent event) {
		if (simulador.estaRodando()) {
			aviso("Pare a simulação antes de conectar o Arduino.");
			return;
		}

		// já conectado: o botão serve para desconectar
		if (servico.conectadoProperty().get()) {
			servico.desconectar();
			return;
		}

		String escolha = cbPorta.getValue();
		final String nomePorta = (escolha == null || AUTOMATICO.equals(escolha)) ? null : escolha;

		btnConectar.setDisable(true);

		// abrir a porta serial pode demorar: roda em segundo plano
		Task<Boolean> tarefa = new Task<Boolean>() {
			@Override
			protected Boolean call() {
				return servico.conectar(nomePorta);
			}
		};

		tarefa.setOnSucceeded(e -> {
			atualizarStatus();
			if (!tarefa.getValue()) {
				aviso("Não foi possível conectar. Verifique o cabo USB, a porta escolhida e se outro "
						+ "programa (como o Monitor Serial do Arduino) não está usando a porta.");
			}
		});

		tarefa.setOnFailed(e -> {
			tarefa.getException().printStackTrace();
			atualizarStatus();
			aviso("Erro ao conectar com o Arduino.");
		});

		Thread thread = new Thread(tarefa);
		thread.setDaemon(true);
		thread.start();
	}

	@FXML
	void alternarSimulacao(ActionEvent event) {
		if (simulador.estaRodando()) {
			simulador.parar();
		} else {
			if (servico.conectadoProperty().get()) {
				aviso("Desconecte o Arduino antes de iniciar a simulação.");
				return;
			}
			simulador.iniciar();
		}
		atualizarStatus();
	}

	@FXML
	void limparGrafico(ActionEvent event) {
		historico.limpar();
		serie.getData().clear();
		inicioMs = -1;
		ajustarEixoTempo(0);
	}

	// ====================================================================
	// STATUS E PORTAS
	// ====================================================================

	/** Atualiza o cartão de conexão e os textos dos botões. */
	private void atualizarStatus() {
		boolean simulando = simulador.estaRodando();
		boolean conectado = servico.conectadoProperty().get();

		if (simulando) {
			lblStatus.setText("Simulação ativa");
			circStatus.setFill(Color.web("#F59E0B"));
		} else if (conectado) {
			lblStatus.setText("Conectado");
			circStatus.setFill(Color.web("#1E8E3E"));
		} else {
			lblStatus.setText("Desconectado");
			circStatus.setFill(Color.web("#9AA7BF"));
		}

		btnConectar.setText(conectado && !simulando ? "Desconectar" : "Conectar");
		btnConectar.setDisable(simulando);
		btnSimular.setText(simulando ? "Parar simulação" : "Simular");
	}

	private void carregarPortas() {
		List<String> nomes = new ArrayList<>();
		nomes.add(AUTOMATICO);
		for (SerialPort porta : ArduinoSerialService.listarPortas()) {
			nomes.add(porta.getSystemPortName());
		}

		// só mexe na lista se mudou (evita atrapalhar o seletor aberto)
		if (!nomes.equals(cbPorta.getItems())) {
			String selecionada = cbPorta.getValue();
			cbPorta.getItems().setAll(nomes);
			cbPorta.setValue(nomes.contains(selecionada) ? selecionada : AUTOMATICO);
		} else if (cbPorta.getValue() == null) {
			cbPorta.setValue(AUTOMATICO);
		}
	}

	// ====================================================================
	// SAIR DA TELA
	// ====================================================================

	/** Remove os ouvintes e para a simulação. O Arduino real continua conectado. */
	private void encerrar() {
		servico.timestampLeituraProperty().removeListener(ouvinteLeitura);
		servico.conectadoProperty().removeListener(ouvinteConexao);
		if (simulador.estaRodando()) {
			simulador.parar();
		}
	}

	@FXML
	void voltar(ActionEvent event) {
		encerrar();
		try {
			Parent root = FXMLLoader.load(getClass().getResource("/application/tela_dashboard_wattwise2.fxml"));
			Scene scene = new Scene(root);
			Stage janela = (Stage) ((Node) event.getSource()).getScene().getWindow();
			janela.setScene(scene);
			janela.setTitle("Dashboard");
			janela.centerOnScreen();
			janela.show();
		} catch (Exception e) {
			e.printStackTrace();
			aviso("Não foi possível voltar para o dashboard.");
		}
	}

	private void aviso(String mensagem) {
		Alert alerta = new Alert(Alert.AlertType.WARNING, mensagem);
		alerta.setHeaderText(null);
		alerta.showAndWait();
	}
}