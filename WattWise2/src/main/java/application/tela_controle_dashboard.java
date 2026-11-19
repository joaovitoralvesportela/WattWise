package application;

import java.net.URL;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class tela_controle_dashboard {
	
	/*
	 * Fazer a atualização das informações em tempo real assim que as informação são alteradas pelo usuário
	 * 
	 */
	@FXML
    private Button btnMonitorar;
	
    @FXML
    private Button btnCadastrarMaquinas;

    @FXML
    private Button btnCalcullarConta;

    @FXML
    private Button btnSairSistema;

    @FXML
    private BarChart<String, Number> graficoConsumo;
    
    @FXML
    private Button btnCalcularCarbono;
    
    @FXML
    private ImageView imgviewDashboard;

    @FXML
    private Label lblConsumoTotal;

    @FXML
    private Label lblCustoEstimado;

    @FXML
    private Label lblMaquinasCadastradas;
    
    @FXML
    private Button btnCalcPlacasSolar;
    
    @FXML
    private Button btnRelatorioPDF;
    
    
 // ainda vou criar a classe dados
    private final Dadosapp dados = Dadosapp.getInstancia();
    
   

    @FXML
    public void initialize() {
        graficoConsumo.setAnimated(false);
        graficoConsumo.setLegendVisible(false);

        // 1. Atualiza imediatamente com o que já estiver na memória
        atualizarDashboard();

        // 2. Busca as máquinas do Banco de Dados sem travar a tela
        carregarDadosDoBanco();
    }

    private void carregarDadosDoBanco() {
        javafx.concurrent.Task<java.util.List<Maquina>> task = new javafx.concurrent.Task<>() {
            @Override
            protected java.util.List<Maquina> call() {
                BancoDeDados banco = new BancoDeDados();
                banco.conectar();
                try {
                    // Ajuste para o nome exato do método de consulta da sua classe BancoDeDados
                    return banco.listarMaquinas();
                } finally {
                    banco.desconectar();
                }
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue() != null) {
                dados.getMaquinas().setAll(task.getValue());
                atualizarDashboard();
            }
        });

        task.setOnFailed(e -> task.getException().printStackTrace());

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public void atualizarDashboard() {
        lblConsumoTotal.setText(String.format("%.2f kWh", dados.consumoTotal()));
        lblMaquinasCadastradas.setText(String.valueOf(dados.getMaquinas().size()));
        lblCustoEstimado.setText(String.format("R$ %.2f", dados.custoTotal()));

        graficoConsumo.getData().clear();

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Consumo em (kWh)");

        java.util.Map<String, Double> mapa = dados.consumoPorMaquina();
        boolean semUsos = mapa.values().stream().allMatch(v -> v == 0.0);

        if (semUsos) {
            for (Maquina m : dados.getMaquinas()) {
                serie.getData().add(new XYChart.Data<>(m.getNome(), m.getConsumoTotalKw()));
            }
        } else {
            mapa.forEach((nome, kwh) -> serie.getData().add(new XYChart.Data<>(nome, kwh)));
        }

        graficoConsumo.getData().add(serie);
    }
    
    // função para ir Cadastrar Maquinas, falta colocar o endereço fxml
    @FXML
    void IrparaTelaCadastrarMaquina(ActionEvent event) {
    	try {
    		Parent root = FXMLLoader.load(getClass().getResource("/application/cad_maquinas_wattwise.fxml"));
    		Scene scene = new Scene(root);
    		Stage janelaCadMaquina = (Stage) ((Node) event.getSource()).getScene().getWindow();
    		janelaCadMaquina.setScene(scene);
    		janelaCadMaquina.setTitle("Cadastro de máquina");
    		janelaCadMaquina.centerOnScreen();
    		janelaCadMaquina.show();
    	}catch(Exception e) {
    		e.printStackTrace();
    		System.out.println(e.getMessage());
    	}
    }
    // Função para Calcular a conta de energia, falta colocar o endereço fxml
    @FXML
    void IrparaTelaCalcularConta(ActionEvent event) {
    	try {
    		Parent root = FXMLLoader.load(getClass().getResource("/application/cal_valores_wattwise2.fxml"));
    		Scene scene = new Scene(root);
    		Stage janelaCalcular = (Stage) ((Node) event.getSource()).getScene().getWindow();
    		janelaCalcular.setScene(scene);
    		janelaCalcular.setTitle("Cadastro de máquina");
    		janelaCalcular.centerOnScreen();
    		janelaCalcular.show();
    	}catch(Exception e) {
    		e.printStackTrace();
    		System.out.println(e.getMessage());
    	}
    }
    @FXML
    void irparaTelaCalcularCarbono(ActionEvent event) {
    	try {
    		Parent root = FXMLLoader.load(getClass().getResource("/application/tela_calculo_arvore.fxml"));
    		Scene scene = new Scene(root);
    		Stage janelaCalCo2 = (Stage) ((Node) event.getSource()).getScene().getWindow();
    		janelaCalCo2.setScene(scene);
    		janelaCalCo2.setTitle("Calculo Co2");
    		janelaCalCo2.centerOnScreen();
    		janelaCalCo2.show();
    	}catch(Exception e) {
    		e.printStackTrace();
    		System.out.println(e.getMessage());
    	}
    	
    }
    @FXML
    void irparaTelaCalPlacaSolar(ActionEvent event) {
    	try {
            URL fxml = getClass().getResource("/application/tela_placa_sola.fxml");
            if (fxml == null) {
                new Alert(Alert.AlertType.ERROR, "Arquivo da tela não encontrado.").showAndWait();
                return;
            }
            Parent root = FXMLLoader.load(fxml);
            Stage janela = (Stage) ((Node) event.getSource()).getScene().getWindow();
            janela.setScene(new Scene(root));
            janela.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
            Throwable causa = (e.getCause() != null) ? e.getCause() : e;
            new Alert(Alert.AlertType.ERROR, "Erro ao abrir a tela:\n" + causa).showAndWait();
        }
    }
    @FXML
    void gerarPdf(ActionEvent event) {
    	try {
    		Parent root = FXMLLoader.load(getClass().getResource("/application/tela_relatorio.fxml"));
    		Scene scene = new Scene(root);
    		Stage janelaPDF = (Stage) ((Node) event.getSource()).getScene().getWindow();
    		janelaPDF.setScene(scene);
    		janelaPDF.setTitle("Gerar Relatorio - pdf");
    		janelaPDF.centerOnScreen();
    		janelaPDF.show();
    	}catch(Exception e) {
    		e.printStackTrace();
    		System.out.println(e.getMessage());
    	}
    	
    }
    //Função para sair do dashBoard
    @FXML
    void SairSistema(ActionEvent event) {
    	Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Deseja sair do WattWise?", ButtonType.YES, ButtonType.NO);
        a.setTitle("WattWise");
        a.setHeaderText(null);
        a.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> Platform.exit());
    }
    
    @FXML
    void IrparaTelaMonitorar(ActionEvent event) {
    	try {
    		Parent root = FXMLLoader.load(getClass().getResource("/application/tela_monitoramento.fxml"));
    		Scene scene = new Scene(root);
    		Stage janelamoni = (Stage) ((Node) event.getSource()).getScene().getWindow();
    		janelamoni.setScene(scene);
    		janelamoni.setTitle("Monitoramento de placas");
    		janelamoni.centerOnScreen();
    		janelamoni.show();
    	}catch(Exception e) {
    		System.out.println("Não foi possivel conectar a placa" + e.getMessage());
    		e.printStackTrace();
    	}
    }

}
