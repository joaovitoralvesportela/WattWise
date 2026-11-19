package application;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class tela_controle_sustentabilidade {

    @FXML
    private Label LabelQtdArvores;

    @FXML
    private Button btnVoltarTelaDashBoard;

    @FXML
    private ImageView imgviewCalcArvores;

    @FXML
    private Label lblCarbonoEmitido;

    @FXML
    private Label lblConSumoTotalSustentabilidade;
    
    
    //public static final double fator_co2_kg_por_kwh = 0.0385;
    
    //public static final double kg_co2_por_arvore_ano = 7.0;
    
    //private tela_controle_sustentabilidade(){}
    
    private final Dadosapp dados = Dadosapp.getInstancia(); 
    
    // Transformar kwh em KG de carbono
    //public static double co2Kg(double kwh) {
    	//return kwh * fator_co2_kg_por_kwh;}
    
    // Quantas árvores precisam trabalhar durante 1 ANO para absover esse Co2
    //public static double arvoresEquivalentes(double kwh) {
    	//return co2Kg(kwh) / kg_co2_por_arvore_ano;}
    
    // Agora precisa da função para apresentar as informações nas labels
    
    // lblConSumoTotalSustentabilidade -> KHW
    // lblCarbonoEmitido -> CO2
    // LabelQtdArvores -> qtd arvores
    
    // Aqui vai ficar a função para apresentar as informações
    public void initialize() {
    	// Exemplo: lblConsumoTotal.setText(String.format("%.2f kWh", dados.consumoTotal()));
    	
    	
    	double kwh = dados.consumoTotal();
    	
    	// responsavel por mostrar a quantidad de consumo em kwh;
    	lblConSumoTotalSustentabilidade.setText(String.format("%.2f Kwh", kwh));
    	
    	// responsavel por mostrar a quantidade de carbono emitido em kg
    	lblCarbonoEmitido.setText(String.format("%.2f kg/khw", Dadosapp.co2Kg(kwh)));
    	
    	// responsavel por mostrar a quantidade de árvores necessários para capturar o carbono
    	LabelQtdArvores.setText(String.format("%.2f árvores", Dadosapp.arvoresEquivalentes(kwh)));
    }
    
    // Função para voltar para a tela de dashboard
    @FXML
    void VoltarParaTelaDash(ActionEvent event) {
    	try {
    		Parent root = FXMLLoader.load(getClass().getResource("/application/tela_dashboard_wattwise2.fxml"));
    		Scene scene = new Scene(root);
    		Stage janelaVoltarDash = (Stage) ((Node) event.getSource()).getScene().getWindow();
    		janelaVoltarDash.setScene(scene);
    		janelaVoltarDash.setTitle("Calculo Carbono");
    		janelaVoltarDash.centerOnScreen();
    		janelaVoltarDash.show();
    	}catch(Exception e) {
    		e.printStackTrace();
    		System.out.println(e.getMessage());
    	}
    }

}
