package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	@Override
	public void start(Stage janelawittewise) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("/application/tela_cadastro_wattwise2.fxml"));
		Scene cena = new Scene(root, 1920, 1080);

		janelawittewise.setScene(cena);
		janelawittewise.setTitle("WitteWise - Energia");
		janelawittewise.setResizable(false);
		janelawittewise.setMaximized(false);
		// Libera a porta USB do Arduino ao fechar a janela pelo "X"
		janelawittewise.setOnCloseRequest(e -> ArduinoSerialService.getInstancia().desconectar());
		
		janelawittewise.show();
		

				
	}

	public static void main(String[] args) {
		launch(args);

	}

}
