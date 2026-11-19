package application;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class tela_gerenciamento_maquinas {
	@FXML
	private ScrollPane scrollMaquinas;
	@FXML
	private VBox listaMaquinas; // fica DENTRO do ScrollPane
	@FXML
	private Label lblResumo;

	// imagem usada quando a máquina não tem foto (coloque este arquivo no projeto)
	private static final String IMAGEM_PADRAO = "/application/imagens/maquina_padrao.png";

	/**
	 * Uma operação que usa a conexão aberta com o banco e devolve a lista
	 * atualizada.
	 */
	private interface OperacaoBanco {
		List<Maquina> executar(BancoDeDados banco) throws Exception;
	}

	@FXML
	public void initialize() {
		if (listaMaquinas.getChildren().isEmpty()) {
			mostrarMensagem("Carregando máquinas...");
		}
		executar(this::listar, "Não foi possível carregar as máquinas.");
	}

	// ====================================================================
	// OPERAÇÕES NO BANCO (cada uma recebe a conexão já aberta)
	// ====================================================================

	private List<Maquina> listar(BancoDeDados banco) throws Exception {
		List<Maquina> maquinas = banco.listarMaquinas();
		if (maquinas == null) {
			throw new Exception("Falha ao ler as máquinas do banco.");
		}
		return maquinas;
	}

	// [+] adiciona mais uma unidade
	private void aumentar(Maquina m) {
		executar(banco -> {
			exigir(banco.alterarQuantidadeMaquina(m.getId(), m.getQuantidade() + 1));
			return listar(banco);
		}, "Não foi possível adicionar a máquina.");
	}

	// [-] remove uma unidade. Se era a última, pergunta se quer remover a máquina.
	private void diminuir(Maquina m) {
		if (m.getQuantidade() > 1) {
			executar(banco -> {
				exigir(banco.alterarQuantidadeMaquina(m.getId(), m.getQuantidade() - 1));
				return listar(banco);
			}, "Não foi possível remover a máquina.");
			return;
		}

		Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION, "Remover a máquina \"" + m.getNome() + "\"?",
				ButtonType.YES, ButtonType.NO);
		confirmacao.setHeaderText(null);
		Optional<ButtonType> resposta = confirmacao.showAndWait();

		if (resposta.isPresent() && resposta.get() == ButtonType.YES) {
			executar(banco -> {
				exigir(banco.removerMaquina(m.getId()));
				return listar(banco);
			}, "Não foi possível remover a máquina.");
		}
	}

	private void exigir(boolean deuCerto) throws Exception {
		if (!deuCerto) {
			throw new Exception("A operação no banco de dados falhou.");
		}
	}
	
	private void executar(OperacaoBanco operacao, String mensagemErro) {
		listaMaquinas.setDisable(true);

		// guarda a posição da rolagem para não voltar ao topo depois de cada clique
		double posicaoRolagem = scrollMaquinas.getVvalue();

		Task<List<Maquina>> tarefa = new Task<List<Maquina>>() {
			@Override
			protected List<Maquina> call() throws Exception {
				BancoDeDados banco = new BancoDeDados();
				banco.conectar();
				try {
					if (!banco.estarConectado()) {
						throw new Exception("Sem conexão com o banco de dados.");
					}
					return operacao.executar(banco);
				} finally {
					banco.desconectar();
				}
			}
		};

		tarefa.setOnSucceeded(e -> {
			desenharLista(tarefa.getValue());
			listaMaquinas.setDisable(false);
			Platform.runLater(() -> scrollMaquinas.setVvalue(posicaoRolagem));
		});

		tarefa.setOnFailed(e -> {
			tarefa.getException().printStackTrace();
			listaMaquinas.setDisable(false);

			// se ainda não há nada na tela, mostra o aviso na própria lista
			boolean temCartoes = listaMaquinas.getChildren().stream().anyMatch(n -> n instanceof HBox);
			if (!temCartoes) {
				lblResumo.setText("");
				mostrarMensagem(mensagemErro + " Verifique a conexão com o banco de dados.");
			} else {
				mostrarErro(mensagemErro);
			}
		});

		Thread thread = new Thread(tarefa);
		thread.setDaemon(true);   // não impede o programa de fechar
		thread.start();
	}

	// ====================================================================
	// DESENHAR
	// ====================================================================

	private void desenharLista(List<Maquina> maquinas) {
		listaMaquinas.getChildren().clear();

		if (maquinas.isEmpty()) {
			lblResumo.setText("");
			mostrarMensagem("Nenhuma máquina cadastrada ainda.");
			return;
		}

		int unidades = 0;
		double kwTotal = 0;

		for (Maquina m : maquinas) {
			listaMaquinas.getChildren().add(criarCartao(m));
			unidades += m.getQuantidade();
			kwTotal += m.getConsumoTotalKw();
		}

		lblResumo.setText(String.format("%d máquinas  |  %d unidades  |  %.2f kW no total",
				maquinas.size(), unidades, kwTotal));
	}

	/** Monta o cartão de UMA máquina: imagem, nome, consumo, [-] quantidade [+]. */
	private HBox criarCartao(Maquina m) {
		ImageView foto = new ImageView(carregarImagem(m.getCaminhoImagem()));
		foto.setFitWidth(96);
		foto.setFitHeight(96);
		foto.setPreserveRatio(true);

		Label nome = new Label(m.getNome());
		nome.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #06245C;");

		Label consumo = new Label(String.format("%.2f kW por unidade  |  total: %.2f kW",
				m.getConsumoKw(), m.getConsumoTotalKw()));
		consumo.setStyle("-fx-font-size: 20px; -fx-text-fill: #3A4F7A;");

		VBox textos = new VBox(6, nome, consumo);
		textos.setAlignment(Pos.CENTER_LEFT);

		Region espaco = new Region();
		HBox.setHgrow(espaco, Priority.ALWAYS);

		Button menos = criarBotaoRedondo("-");
		menos.setOnAction(e -> diminuir(m));

		Label quantidade = new Label(String.valueOf(m.getQuantidade()));
		quantidade.setMinWidth(48);
		quantidade.setAlignment(Pos.CENTER);
		quantidade.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #06245C;");

		Button mais = criarBotaoRedondo("+");
		mais.setOnAction(e -> aumentar(m));

		HBox cartao = new HBox(24, foto, textos, espaco, menos, quantidade, mais);
		cartao.setAlignment(Pos.CENTER_LEFT);
		cartao.setPadding(new Insets(16, 28, 16, 28));
		cartao.setStyle("-fx-background-color: #F1F5FC; -fx-background-radius: 20;"
				+ " -fx-border-color: #C3D2EE; -fx-border-radius: 20; -fx-border-width: 2;");
		return cartao;
	}

	private Button criarBotaoRedondo(String texto) {
		Button b = new Button(texto);
		b.setPrefSize(52, 52);
		b.setStyle("-fx-background-color: #0B4FD6; -fx-background-radius: 26; -fx-text-fill: white;"
				+ " -fx-font-size: 26px; -fx-font-weight: bold; -fx-cursor: hand;");
		return b;
	}

	private void mostrarMensagem(String texto) {
		Label mensagem = new Label(texto);
		mensagem.setStyle("-fx-font-size: 24px; -fx-text-fill: #3A4F7A;");
		mensagem.setWrapText(true);
		listaMaquinas.getChildren().setAll(mensagem);
	}

	// ====================================================================
	// IMAGEM
	// ====================================================================

	// Aceita imagem dentro do projeto ("/application/imagens/x.png")
	// ou um arquivo do computador ("C:\\fotos\\x.png"). Se nada servir, usa a padrão.
	private Image carregarImagem(String caminho) {
		if (caminho != null && !caminho.isBlank()) {
			if (caminho.startsWith("/")) {
				URL recurso = getClass().getResource(caminho);
				if (recurso != null) {
					return new Image(recurso.toExternalForm(), 96, 96, true, true);
				}
			}
			File arquivo = new File(caminho);
			if (arquivo.exists()) {
				return new Image(arquivo.toURI().toString(), 96, 96, true, true);
			}
		}
		return new Image(getClass().getResource(IMAGEM_PADRAO).toExternalForm(), 96, 96, true, true);
	}

	// ====================================================================
	// NAVEGAÇÃO
	// ====================================================================

	@FXML
	void voltar(ActionEvent event) {
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
			mostrarErro("Não foi possível voltar para o dashboard.");
		}
	}

	private void mostrarErro(String mensagem) {
		new Alert(Alert.AlertType.ERROR, mensagem).showAndWait();
	}
}
