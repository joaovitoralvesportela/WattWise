package application;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class calcular_valor {

    @FXML
    private ComboBox<Maquina> ComboBox;

    @FXML
    private Button btnCalcular;

    @FXML
    private Button btnVoltarDashboard1;

    @FXML
    private ImageView imgViewBackGroundCal;

    @FXML
    private Label lblResultado;

    @FXML
    private TextField textfieldConsumoKw;

    @FXML
    private TextField textfieldTempoUso;
    
    private final Dadosapp dados = Dadosapp.getInstancia();
    
    
    @FXML
    public void initialize() {
    	
    	// Isso vai listar as máquinas cadastradas
    	ComboBox.setItems(dados.getMaquinas());
    	
    	// Ao escolher a máquina isso preenche o kw dela (O usuário pode alterar)
    	ComboBox.valueProperty().addListener((obs, antiga, nova) -> {
            if (nova != null) textfieldConsumoKw.setText(String.valueOf(nova.getConsumoKw()));
        });
    }
    
    @FXML
    void CalcularConta(ActionEvent event) {
    	Maquina maquina = ComboBox.getValue();
    	if(maquina == null) {
    		Alert alerta = new Alert(Alert.AlertType.WARNING);
			alerta.setTitle("Atenção");
			alerta.setHeaderText("Valores precisam ser informados");
			alerta.setContentText("Selecione uma máquina!");
			alerta.showAndWait();
			return;
    	}
    	double horas, kw;
    	try {
    		horas = Double.parseDouble(textfieldTempoUso.getText().trim().replace(",", "."));
    		kw = Double.parseDouble(textfieldConsumoKw.getText().trim().replace(",", "."));
    	}catch(Exception e) {
    		Alert alerta = new Alert(Alert.AlertType.ERROR);
			alerta.setTitle("Atenção");
			alerta.setHeaderText("Valores precisam ser informados");
			alerta.setContentText("Selecione um valor válido!");
			alerta.showAndWait();
    		e.printStackTrace();
    		System.out.println(e.getMessage());
    		return;
    	}
    	if(horas <= 0 || kw <= 0) {
    		Alert alerta = new Alert(Alert.AlertType.ERROR);
			alerta.setTitle("Atenção");
			alerta.setHeaderText("Valores incosistentes");
			alerta.setContentText("Os valores precisam ser maiores que zero!");
			alerta.showAndWait();
    		return;
    	}
    	Uso uso = new Uso(maquina.getNome(), kw, horas);
    	dados.getUsos().add(uso); 
    	// Isso vai fazer com que o gráfico no dashboard atualize
    	
    	double custo = uso.getKwh() * Dadosapp.TARIFA_KWH;
    	lblResultado.setText(String.format("Consumo:%.2f kwh|custo: R$ %.2f", uso.getKwh(), custo));
    }

    @FXML
    void voltarParaDashBoard(ActionEvent event) {
    	try {
    		Parent root = FXMLLoader.load(getClass().getResource("/application/tela_dashboard_wattwise2.fxml"));
    		Scene scene = new Scene(root);
    		Stage janelaVoltar = (Stage) ((Node) event.getSource()).getScene().getWindow();
    		janelaVoltar.setScene(scene);
    		janelaVoltar.setTitle("DashBoard");
    		janelaVoltar.centerOnScreen();
    		janelaVoltar.show();
    	}catch(Exception e) {
    		e.printStackTrace();
    		System.out.println(e.getMessage());
    	}
    }

}
