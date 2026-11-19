package application;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class tela_controle_placa {
	
	 @FXML
	 private Button btnPosicionamentoPlaca;
	
	@FXML
	private Label lblQtdPlacas; // Quantidade de placas solares
	@FXML
	private Label lblPotenciaKwp; // Potência instalada (kWp)
	@FXML
	private Label lblCustoSistema; // Custo estimado do sistema (R$)
	@FXML
	private Label lblEconomiaMensal; // Economia mensal (R$)
	@FXML
	private Label lblRetornoAnos; // Retorno do investimento (anos)

	// NOVOS (adicione no Scene Builder)
	@FXML
	private Label lblRadiacao; // Radiação solar diária (kWh/m²/dia)
	@FXML
	private Label lblGeracaoMensal; // Geração estimada (kWh/mês)
	@FXML
	private Label lblFonteDados; // Mostra os dados que vem do arduino!!!

	private final Dadosapp dados = Dadosapp.getInstancia();

	@FXML
	public void initialize() {
		ArduinoSerialService arduino = ArduinoSerialService.getInstancia();
	    boolean ok = arduino.conectadoProperty().get() || arduino.conectar(null);

	    if (ok) {
	        lblFonteDados.setText("Arduino conectado! Aguardando leitura...");
	    } else {
	        lblFonteDados.setText("Arduino não detectado");
	    }

	    // ATENÇÃO: precisa ser timestampLeituraProperty() para atualizar mesmo com 0.0 W!
	    arduino.timestampLeituraProperty().addListener((obs, oldVal, newVal) -> {
	        double v = arduino.tensaoVoltsProperty().get();
	        double ma = arduino.correnteMaProperty().get();
	        double w = arduino.potenciaWattsProperty().get();
	        lblFonteDados.setText(String.format("Painel Real: %.2f V | %.0f mA | %.2f W", v, ma, w));
	    });

	    if (dados.getRadiacaoAnual() != null) {
	        atualizarTela(dados.getRadiacaoAnual(), "Fonte: NASA POWER");
	        return;
	    }

	    atualizarTela(Dadosapp.hsp_dia, "Consultando a NASA POWER...");
		// A internet roda em outra thread. Se rodasse aqui, a tela travaria.
		Task<Double> consulta = new Task<>() {
			@Override
			protected Double call() throws Exception {
				return NasaPowerCliente.mediaAnual(Dadosapp.LATITUDE_PADRAO, Dadosapp.LONGITUDE_PADRAO);
			}
		};

		consulta.setOnSucceeded(e -> {
			double radiacao = consulta.getValue();
			dados.setRadiacaoAnual(radiacao);
			atualizarTela(radiacao, "Fonte: NASA POWER");
		});

		consulta.setOnFailed(e -> {
			consulta.getException().printStackTrace();
			atualizarTela(Dadosapp.hsp_dia, "Sem conexão: usando valor padrão de sol");
		});

		Thread thread = new Thread(consulta);
		thread.setDaemon(true);
		thread.start();
	}

	private void atualizarTela(double radiacao, String fonte) {
		Dadosapp.ResultadoSolar r = dados.calculoSolar(radiacao);

		lblRadiacao.setText(String.format("%.2f kWh/m²/dia", radiacao));
		lblGeracaoMensal.setText(String.format("%.0f kWh/mês", r.geracaoMensalKwh));
		lblQtdPlacas.setText(String.valueOf(r.paineis));
		lblPotenciaKwp.setText(String.format("%.2f kWp", r.potenciaKwp));
		lblCustoSistema.setText(String.format("R$ %.2f", r.custoTotal));
		lblEconomiaMensal.setText(String.format("R$ %.2f", r.economiaMensal));
		lblRetornoAnos.setText(r.paybackAnos > 0 ? String.format("%.1f anos", r.paybackAnos) : "-");
		//lblFonteDados.setText(fonte);
	}

	@FXML
	void VoltarParaTelaDash(ActionEvent event) {
	    Dadosapp.getInstancia().registrarLeituraArduino(); // Salva a leitura no gráfico antes de voltar
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
	    }
	}
	
	// Função para ir para a tela de posicionamento de placas solares!
	@FXML
    void irparatelaPosicionamentoPlacas(ActionEvent event) {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("/application/tela_posicionamento (1).fxml"));
			Scene scene = new Scene(root);
			Stage janelapo = (Stage) ((Node) event.getSource()).getScene().getWindow();
			janelapo.setScene(scene);
			janelapo.setTitle("Tela Posicionamento");
			janelapo.centerOnScreen();
			janelapo.show();
		}catch(Exception e) {
			System.out.println("Não foi possivel ir para a tela" + e.getMessage());
			e.printStackTrace();
		}
    }
}