package application;

import java.util.Random;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/**
 * Gera leituras falsas, como se o Arduino estivesse conectado.
 * Serve para TESTAR a tela e para a apresentação, caso o Arduino falhe.
 *
 * Ele escreve nas mesmas propriedades da ArduinoSerialService, então a tela
 * não precisa saber se os dados são reais ou simulados.
 * Tudo roda na thread da tela (Timeline), por isso é seguro mexer nas propriedades.
 */
public class SimuladorSolar {

	private final ArduinoSerialService servico = ArduinoSerialService.getInstancia();
	private final Random sorteio = new Random();

	private Timeline relogio;
	private int passo = 0;

	public boolean estaRodando() {
		return relogio != null && relogio.getStatus() == Animation.Status.RUNNING;
	}

	public void iniciar() {
		if (estaRodando()) {
			return;
		}
		servico.conectadoProperty().set(true);

		// uma leitura por segundo
		relogio = new Timeline(new KeyFrame(Duration.seconds(1), e -> gerarLeitura()));
		relogio.setCycleCount(Animation.INDEFINITE);
		relogio.play();
	}

	public void parar() {
		if (relogio != null) {
			relogio.stop();
		}
		servico.conectadoProperty().set(false);
	}

	private void gerarLeitura() {
		passo++;

		// "luz" varia suavemente entre 0 e 1, como uma nuvem passando
		double luz = 0.5 + 0.5 * Math.sin(passo / 8.0);

		double tensao = 3.0 + 2.0 * luz + ruido(0.05);       // volts
		double corrente = 40 + 160 * luz + ruido(2.0);       // miliampères
		double potencia = tensao * corrente / 1000.0;        // watts (V x mA / 1000)

		servico.tensaoVoltsProperty().set(tensao);
		servico.correnteMaProperty().set(corrente);
		servico.potenciaWattsProperty().set(potencia);

		// energia acumulada: 1 segundo de potência, em kWh
		double kwhDoSegundo = (potencia / 1000.0) / 3600.0;
		servico.energiaGeradaKwhProperty().set(servico.energiaGeradaKwhProperty().get() + kwhDoSegundo);

		// por último: é a mudança do timestamp que avisa a tela de que há leitura nova
		servico.timestampLeituraProperty().set(System.currentTimeMillis());
	}

	private double ruido(double amplitude) {
		return (sorteio.nextDouble() - 0.5) * 2 * amplitude;
	}
}