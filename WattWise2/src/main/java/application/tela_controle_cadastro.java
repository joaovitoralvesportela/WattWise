package application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class tela_controle_cadastro {

    @FXML
    private Button btnCriarConta;

    @FXML
    private Hyperlink hyperlinkFazerLogin;
    
    @FXML
    private ImageView imgviewCadConta;
    
    @FXML
    private Label lbltitulo;

    @FXML
    private PasswordField passwordfieldSenha;

    @FXML
    private TextField textfieldDataNascimento;

    @FXML
    private TextField textfieldEmail;

    @FXML
    private TextField textfieldNome;

    // Salvar informacoes de cadastro e para ir para o dashboard
    @FXML
    void IrparaTelaLoginBotao(ActionEvent event) {
    	String nome   = textfieldNome.getText().trim();
        String email  = textfieldEmail.getText().trim();
        String senha  = passwordfieldSenha.getText();
        String data_nascimento = textfieldDataNascimento.getText();

        //tratamento de erros:

        //Campos vazios
        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || data_nascimento.isEmpty()) {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Atenção");
            alerta.setHeaderText("Campos obrigatórios");
            alerta.setContentText("Todos os campos devem ser preenchidos.");
            alerta.showAndWait();
            return;
        }

        //Nome com tamanho mínimo
        if (nome.length() < 3) {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Atenção");
            alerta.setHeaderText("Nome inválido");
            alerta.setContentText("O nome deve conter pelo menos 3 caracteres.");
            alerta.showAndWait();
            return;
        }

        //Validação de email com regex
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Atenção");
            alerta.setHeaderText("Email inválido");
            alerta.setContentText("Informe um email válido (ex: usuario@email.com).");
            alerta.showAndWait();
            return;
        }

        //Senha com tamanho mínimo
        if (senha.length() < 6) {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Atenção");
            alerta.setHeaderText("Senha fraca");
            alerta.setContentText("A senha deve conter pelo menos 6 caracteres.");
            alerta.showAndWait();
            return;
        }

        // Tratamento de erro para a data de data_nascimento
        if(data_nascimento.isEmpty()){
          Alert alertaNascimento = new Alert(Alert.AlertType.WARNING);
          alertaNascimento.setTitle("AVISO");
          alertaNascimento.setHeaderText("Data de nascimento Obrigátoria!");
          alertaNascimento.setContentText("Informe a data de nascimento dd/mm//yy");
          alertaNascimento.showAndWait();
          return;
        }
        
        // Validar formato e datas inválidas
        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate dataNascimento;

        try{
          formatador.parse(data_nascimento); // valida o formato
          dataNascimento = LocalDate.parse(data_nascimento,formatador); // converte
        }catch(Exception e) {
          Alert alertaData = new Alert(Alert.AlertType.WARNING);
          alertaData.setTitle("Aviso");
          alertaData.setHeaderText("Data Inválida");
          alertaData.setContentText("Informe uma data válida");
          alertaData.showAndWait();
          return;
        }

        // Conxao do banco de dados e inserção de informações
        BancoDeDados bd = new BancoDeDados();
        bd.conectar();
        if (bd.estarConectado()) {
        	
            try {
                bd.inserirContas(nome, email, senha, dataNascimento.toString()); // data_nascimento
                bd.desconectar();

                Alert alerta = new Alert(Alert.AlertType.INFORMATION);
                alerta.setTitle("Cadastro");
                alerta.setHeaderText("Cadastro feito com sucesso!");
                alerta.setContentText("O cadastro foi realizado.");
                alerta.showAndWait();

                // Navegar para a tela de login
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/application/tela_login_wattwise2.fxml"));
                    Scene scene = new Scene(root);
                    Stage janela = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    janela.setScene(scene);
                    janela.setTitle("WitteWise - Tela Login");
                    janela.centerOnScreen();
                    janela.show();
                } catch (Exception e) {
                    Alert erro = new Alert(Alert.AlertType.ERROR);
                    erro.setTitle("Erro");
                    erro.setHeaderText("Erro ao carregar a tela de login");
                    erro.setContentText("Não foi possível abrir a tela de login. Tente novamente.");
                    erro.showAndWait();
                    e.printStackTrace();
                }

            } catch (Exception e) {
                // Erro ao inserir no banco (ex: email já cadastrado)
                Alert erro = new Alert(Alert.AlertType.ERROR);
                erro.setTitle("Erro");
                erro.setHeaderText("Erro ao cadastrar");
                erro.setContentText("Não foi possível realizar o cadastro. Verifique se o email já está em uso.");
                erro.showAndWait();
                e.printStackTrace();
            }
        } else {
            Alert erro = new Alert(Alert.AlertType.ERROR);
            erro.setTitle("Erro de conexão");
            erro.setHeaderText("Banco de dados indisponível");
            erro.setContentText("Não foi possível conectar ao banco de dados. Tente novamente mais tarde.");
            erro.showAndWait();
            return;
        }
    }
 

    // parametro feito pelo hyperlink
    @FXML
    void irparaTelaLogin(ActionEvent event) {
    	try {
    		Parent root = FXMLLoader.load(getClass().getResource("/application/tela_login_wattwise2.fxml"));
    		Scene scene = new Scene(root);
    		Stage janelaLogin = (Stage) ((Node) event.getSource()).getScene().getWindow();
    		janelaLogin.setScene(scene);
    		janelaLogin.setTitle("Tela Login");
    		janelaLogin.centerOnScreen();
    		janelaLogin.show();
    	}catch(Exception e) {
    		e.printStackTrace();
    		System.out.println(e.getMessage());
    	}
    }

}
