package application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Guarda as leituras mais recentes (só as da "janela" de tempo). Leituras mais
 * velhas são descartadas, para a memória não crescer sem parar.
 * Não usa JavaFX: é só uma lista com regra de limpeza.
 */
public class HistoricoLeituras {

	private final double janelaSegundos;
	private final List<LeituraSolar> leituras = new ArrayList<>();

	public HistoricoLeituras(double janelaSegundos) {
		this.janelaSegundos = janelaSegundos;
	}

	public void adicionar(LeituraSolar leitura) {
		leituras.add(leitura);

		// descarta o que ficou fora da janela de tempo
		double limite = leitura.getSegundos() - janelaSegundos;
		leituras.removeIf(l -> l.getSegundos() < limite);
	}

	public List<LeituraSolar> getLeituras() {
		return Collections.unmodifiableList(leituras);
	}

	public void limpar() {
		leituras.clear();
	}
}