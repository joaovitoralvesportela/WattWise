package application;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class tela_controle_login {

	@FXML
	private Button btnEntrar;

	@FXML
	private CheckBox checkboxVisualizarSenha;

	@FXML
	private Hyperlink hyperLinkRealizarCadastro;

	@FXML
	private ImageView imgviewBackground;

	@FXML
	private Label lbltituloCadastro;

	@FXML
	private PasswordField passwordfieldSenha;

	@FXML
	private TextField textfiledSenha;
	@FXML
	private TextField textfieldEmail;

	@FXML
	void EntrarSistema(ActionEvent event) {
		String email = textfieldEmail.getText().trim();
		String senha = passwordfieldSenha.getText();

		// Campos vazios
		if (email.isEmpty() || senha.isEmpty()) {
			Alert alerta = new Alert(Alert.AlertType.WARNING);
			alerta.setTitle("AVISO");
			alerta.setHeaderText("Campos obrigatórios!");
			alerta.setContentText("Preencha o email e a senha para entrar.");
			alerta.showAndWait();
			return;
		}
		// Validação de email com regex
		if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
			Alert alerta = new Alert(Alert.AlertType.WARNING);
			alerta.setTitle("Atenção");
			alerta.setHeaderText("Email inválido");
			alerta.setContentText("Informe um email válido (ex: usuario@email.com).");
			alerta.showAndWait();
			return;
		}

		BancoDeDados bd = new BancoDeDados();

		try {
			bd.conectar();
			if (!bd.estarConectado()) {
				Alert alerta = new Alert(Alert.AlertType.ERROR);
				alerta.setTitle("Erro de conexão");
				alerta.setHeaderText("Banco de dados indisponível!");
				alerta.setContentText("Não foi possível conectar. Tente novamente mais tarde.");
				alerta.showAndWait();
				return;
			}
			if (bd.validaLogin(email, senha)) {
				try {
					Parent root = FXMLLoader.load(getClass().getResource("/application/tela_dashboard_wattwise2.fxml"));
					Scene scene = new Scene(root);
					Stage janela = (Stage) ((Node) event.getSource()).getScene().getWindow();
					janela.setScene(scene);
					janela.setTitle("WitteWise - Menu");
					janela.centerOnScreen();
					janela.show();
				} catch (Exception e) {
					Alert erro = new Alert(Alert.AlertType.ERROR);
					erro.setTitle("Erro");
					erro.setHeaderText("Erro ao carregar o menu!");
					erro.setContentText("Não foi possível abrir a tela principal.");
					erro.showAndWait();
					e.printStackTrace();
				}
			} else {
				Alert alerta = new Alert(Alert.AlertType.ERROR);
				alerta.setTitle("Erro");
				alerta.setHeaderText("Usuário ou senha incorretos!");
				alerta.setContentText("Verifique suas informações e tente novamente.");
				alerta.showAndWait();
			}

		} catch (Exception e) {
			Alert erro = new Alert(Alert.AlertType.ERROR);
			erro.setTitle("Erro");
			erro.setHeaderText("Erro inesperado!");
			erro.setContentText("Ocorreu um problema ao tentar fazer login.");
			erro.showAndWait();
			e.printStackTrace();
		} finally {
			bd.desconectar(); // garante que a conexão sempre fecha
		}
	}

	// ir para a tela de cadastro com o hyperlink
	@FXML
	void IrparaTelaCadastroHyperLink(ActionEvent event) {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("/application/tela_cadastro_wattwise2.fxml"));
			Scene scene = new Scene(root);
			Stage janelaCadastro = (Stage) ((Node) event.getSource()).getScene().getWindow();
			janelaCadastro.setScene(scene);
			janelaCadastro.setTitle("tela Cadastro");
			janelaCadastro.centerOnScreen();
			janelaCadastro.show();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}

	}
	// Funcao para visualizar a senha com o checkbox

	@FXML
	public void initialize() {
		textfiledSenha.setVisible(false);
		Image image = new Image(getClass().getResourceAsStream("/application/imagem/wattwise-login.png"));
		imgviewBackground.setImage(image);
	}
	
	// Função para visualizar a senha pelo checkbox
	@FXML
	void VisualizarSenha(ActionEvent event) {
		// passwordfieldSenha
		// textfiledSenha
		// checkboxVisualizarSenha
		if(checkboxVisualizarSenha.isSelected()) {
			textfiledSenha.setText(passwordfieldSenha.getText());
			
			textfiledSenha.setVisible(true);
			textfiledSenha.setManaged(true);
			
			passwordfieldSenha.setVisible(false);
			passwordfieldSenha.setManaged(false);
			
		} else {
			passwordfieldSenha.setText(textfiledSenha.getText());
			
			passwordfieldSenha.setVisible(true);
			passwordfieldSenha.setManaged(true);
			
			textfiledSenha.setVisible(false);
			textfiledSenha.setManaged(false);
		}
	}

}
