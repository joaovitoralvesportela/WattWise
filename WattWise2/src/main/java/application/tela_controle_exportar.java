package application;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import org.openpdf.text.DocumentException;

import java.io.IOException;

public class tela_controle_exportar {

	@FXML
	private Button btnVoltarParaTela;

	@FXML
	private Button btnExportarPDF;

	@FXML
	private ImageView imgViewPdf;

	@FXML
	void exportarPDF(ActionEvent event) {

		try {
			
			boolean salvou = RelatorioPDF.exportar(btnExportarPDF.getScene().getWindow());
			if (salvou) {
				new Alert(Alert.AlertType.INFORMATION, "pdf gerado com sucesso!").showAndWait();
			}
		} catch (IllegalStateException e) {
			
			Alert alerta = new Alert(Alert.AlertType.WARNING);
			alerta.setTitle("Dados insuficientes");
			alerta.setHeaderText("Nao foi possivel gerar o relatorio!");
			alerta.setContentText(e.getMessage());
			alerta.showAndWait();
			
			e.printStackTrace();
			System.out.println(e.getMessage());
			
		}catch(Exception e) {
			
			Alert alerta2 = new Alert(Alert.AlertType.ERROR);
			alerta2.setTitle("Erro ao exportar!");
			alerta2.setHeaderText("Falha ao gerar o pdf");
			alerta2.setContentText(e.getMessage());
			alerta2.showAndWait();
			
			e.printStackTrace();
			System.out.println(e.getMessage());
		}

		try {
			
			RelatorioPDF.exportar(btnExportarPDF.getScene().getWindow());

		} catch (IllegalStateException e) {

			Alert alerta = new Alert(Alert.AlertType.WARNING);
			alerta.setTitle("Dados insuficientes");
			alerta.setHeaderText("Não foi possível gerar o relatório");
			alerta.setContentText(e.getMessage());
			alerta.showAndWait();

		} catch (Exception e) {

			Alert alerta = new Alert(Alert.AlertType.ERROR);
			alerta.setTitle("Erro ao exportar");
			alerta.setHeaderText("Falha ao gerar o PDF");
			alerta.setContentText("Verifique se o arquivo pode ser salvo.\n" + e.getMessage());
			alerta.showAndWait();
		}

	}

	@FXML
	void voltar_tela(ActionEvent event) {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("/application/tela_dashboard_wattwise2.fxml"));
			Scene scene = new Scene(root);
			Stage voltarMenu = (Stage) ((Node) event.getSource()).getScene().getWindow();
			voltarMenu.setScene(scene);
			voltarMenu.setTitle("Tela DashBoard");
			voltarMenu.centerOnScreen();
			voltarMenu.show();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}

}
