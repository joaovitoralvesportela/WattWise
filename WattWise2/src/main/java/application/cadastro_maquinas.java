package application;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class cadastro_maquinas {
	
	@FXML
    private ImageView imgviewBackGround;
	
	@FXML
	private Button btnSalvar;

	@FXML
	private Button btnVoltarDashboard;

	@FXML
	private TextField textfieldKwMaquina;

	@FXML
	private TextField textfieldNomeMaquina;
	
	 @FXML
	 private Button btnGerenciarMaquina;
	 
	 
	// Precisa colocar o código para enviar as informações salvas para o banco de dados 
	@FXML
	void SalvarInformacoesMaquina(ActionEvent event) {
		String nome = textfieldNomeMaquina.getText().trim();
		double kw = 0;
		
		try {
			kw = Double.parseDouble(textfieldKwMaquina.getText().trim().replace(",", "."));
		}catch(NumberFormatException e) {
			
		}
		
		if(nome.isEmpty() || kw <= 0) {
			Alert alerta = new Alert(Alert.AlertType.ERROR);
			alerta.setHeaderText("Valores inválidos");
			alerta.setContentText("Informe um nome e o consumo em kw maior que zero!");
			alerta.showAndWait();
			return;
		}
		
		final double consumoKw = kw;
		
		btnSalvar.setDisable(true);
		
		Task<Boolean> tarefa = new Task<Boolean>() {
			@Override
			protected Boolean call() {
				BancoDeDados banco = new BancoDeDados();
				banco.conectar();
				try {
					return banco.inserirMaquina(nome, consumoKw, null);
				} finally {
					banco.desconectar();
				}
			}
		};
		
		tarefa.setOnSucceeded(e -> {
			btnSalvar.setDisable(false);
			
			if(tarefa.getValue()) {
				
				Dadosapp.getInstancia().getMaquinas().add(new Maquina(nome, consumoKw));
				
				textfieldNomeMaquina.clear();
				textfieldKwMaquina.clear();
				
				Alert alerta = new Alert(Alert.AlertType.INFORMATION);
				alerta.setTitle("Sucesso!");
				alerta.setHeaderText("Máquina Cadastrada");
				alerta.setContentText("A máquina foi cadastrada");
				alerta.showAndWait();
			} else {
				mostarErroSalvar();
			}
		});
		
		tarefa.setOnFailed(e -> {
			btnSalvar.setDisable(false);
			tarefa.getException().printStackTrace();
			mostarErroSalvar();
		});
		
		Thread thread = new Thread(tarefa);
		thread.setDaemon(true);
		thread.start();
	}
	
	private void mostarErroSalvar() {
		Alert alerta = new Alert(Alert.AlertType.ERROR);
		alerta.setTitle("Erro");
		alerta.setHeaderText("Não foi possível salvar a máquina");
		alerta.setContentText("Verifique a conexão com a internet e se a tabela existe no banco de dados.");
		alerta.showAndWait();
	}
	
	// Funcao para voltar para a tela de menu
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
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
	
	// Função para ir pra ir para tela gerenciar_maquinas
	 @FXML
	 void irparaTelaGerenciarMaquina(ActionEvent event) {
		 try {
			 Parent root = FXMLLoader.load(getClass().getResource("/application/tela_gerenciamento_maquinas.fxml"));
			 Scene scene = new Scene(root);
			 Stage janelairgere = (Stage) ((Node) event.getSource()).getScene().getWindow();
			 janelairgere.setScene(scene);
			 janelairgere.setTitle("Janela Gerenciar Máquinas");
			 janelairgere.centerOnScreen();
			 janelairgere.show();
		 }catch(Exception e) {
			 e.printStackTrace();
			 System.out.println(e.getMessage());
		 }
	 }


}
