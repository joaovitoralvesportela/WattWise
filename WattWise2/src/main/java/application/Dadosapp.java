package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.Task;


public class Dadosapp {

	// valor da tarifa da conta de energia
	public static final double TARIFA_KWH = 0.76;

	// ********** SUSTENTABILIDADE *********************************************
	public static final double fator_co2_kg_por_kwh = 0.0385;

	public static final double kg_co2_por_arvore_ano = 7.0;

	public static double co2Kg(double kwh) {
		return kwh * fator_co2_kg_por_kwh;
	}

	public static double arvoresEquivalentes(double kwh) {
		return co2Kg(kwh) / kg_co2_por_arvore_ano;
	}
	/*
	 * // Precisa cirar essas informações na classe Dadosapp
		double lado1 = Dadosapp.PAINEL_COMPRIMENTO_M;   // 2,15 m
		double lado2 = Dadosapp.PAINEL_LARGURA_M;       // 1,20 m
		double esp = Dadosapp.ESPACAMENTO_PAINEIS_M;
	 * 
	 */
	

	// ********** ENERGIA SOLAR ************************************************
	
	public static final double PAINEL_COMPRIMENTO_M = 2.15;
			
	public static final double PAINEL_LARGURA_M = 1.20;
	
	public static final double ESPACAMENTO_PAINEIS_M = 0.02;
	
	public static final double PAINEL_AREA_M2 = 2.58;
			
	// Horas de sol pleno por dia: valor PADRÃO, usado quando a NASA não responde
	public static final double hsp_dia = 5.0;

	// Local usado na consulta à NASA POWER (exemplo: Fortaleza). Troque pelo seu local.
	public static final double LATITUDE_PADRAO = -3.7319;
	public static final double LONGITUDE_PADRAO = -38.5267;

	// Eficiencia do sistema considerando as perdas de mais ou menos 20%
	public static final double eficiencia_solar = 0.80;

	// Potencia de cada painel kw -> (550w)
	public static final double potencia_painel_kw = 0.55;

	// Custo instalado por kWp (R$ 2,45 por Wp = R$ 2450 por kWp)
	public static final double custo_por_wp = 2450.0;

	// Quantidade de dias do mes
	private static final double dias_no_mes = 30.0;

	// Radiação anual vinda da NASA (kWh/m²/dia). null = ainda não consultada.
	private Double radiacaoAnual;

	public Double getRadiacaoAnual() {
		return radiacaoAnual;
	}

	public void setRadiacaoAnual(Double radiacaoAnual) {
		this.radiacaoAnual = radiacaoAnual;
	}

	public static class ResultadoSolar {
		public final int paineis;
		public final double potenciaKwp;
		public final double geracaoMensalKwh;
		public final double custoTotal;
		public final double economiaMensal;
		public final double paybackAnos;

		public ResultadoSolar(int paineis, double potenciaKwp, double geracaoMensalKwh,
				double custoTotal, double economiaMensal, double paybackAnos) {
			this.paineis = paineis;
			this.potenciaKwp = potenciaKwp;
			this.geracaoMensalKwh = geracaoMensalKwh;
			this.custoTotal = custoTotal;
			this.economiaMensal = economiaMensal;
			this.paybackAnos = paybackAnos;
		}
	}

	// Usa a radiação da NASA se já foi consultada; senão, o valor padrão
	public ResultadoSolar calculoSolar() {
		double hsp = (radiacaoAnual != null) ? radiacaoAnual : hsp_dia;
		return calculoSolar(hsp);
	}

	// Calcula placas, custo, economia e retorno.
	// hspDia = radiação diária em kWh/m²/dia (equivale às horas de sol pleno)
	public ResultadoSolar calculoSolar(double hspDia) {
		// Consumo total é tratado como consumo do mes
		double consumoMensal = consumoTotal();

		// Geração de 1 kWp por mes
		double geracaoPorKwp = hspDia * dias_no_mes * eficiencia_solar;

		// Potencia necessaria e qtd de paineis
		double kwpNecessario = consumoMensal / geracaoPorKwp;
		int paineis = (int) Math.ceil(kwpNecessario / potencia_painel_kw);

		// Potencia realmente instalada e o que ela gera
		double kwpInstalado = paineis * potencia_painel_kw;
		double geracaoMensal = kwpInstalado * geracaoPorKwp;

		// Dinheiro
		double custo = kwpInstalado * custo_por_wp;
		double economiaMensal = Math.min(geracaoMensal, consumoMensal) * TARIFA_KWH;
		double paybackAnos = economiaMensal > 0 ? custo / (economiaMensal * 12) : 0;

		return new ResultadoSolar(paineis, kwpInstalado, geracaoMensal, custo, economiaMensal, paybackAnos);
	}

	// **************************************************************************

	private static final Dadosapp INSTANCIA = new Dadosapp();

	private final ObservableList<Maquina> maquinas = FXCollections.observableArrayList();
	private final ObservableList<Uso> usos = FXCollections.observableArrayList();

	private Dadosapp() {}

	public static Dadosapp getInstancia() {
		return INSTANCIA;
	}

	public ObservableList<Maquina> getMaquinas() {
		return maquinas;
	}

	public ObservableList<Uso> getUsos() {
		return usos;
	}

	public double consumoTotal() {
		return usos.stream().mapToDouble(Uso::getKwh).sum();
	}

	public double custoTotal() {
		return consumoTotal() * TARIFA_KWH;
	}
	
	
	// O claude mandou por enquanto apagar essa classe , pois ela vai ir para outras telas
	// O motivo é que ela pode interferir quando for eviar as informações para o banco de dados
	public boolean registrarLeituraArduino() {
	    double kwAtual = ArduinoSerialService.getInstancia().potenciaWattsProperty().get() / 1000.0;
	    if (kwAtual > 0) {
	        this.usos.add(new Uso("Painel Solar (INA219)", kwAtual, 1.0));
	        return true;
	    }
	    return false;
	}
	
	/*
	 * Será adicionada blocos de código para atualizar as informações em tempo real
	 * 
	 */
	
	
	// Consumo somado por máquina. Máquinas sem uso aparecem com zero
	public Map<String, Double> consumoPorMaquina() {
		Map<String, Double> mapa = new LinkedHashMap<>();
		for (Maquina m : maquinas)
			mapa.put(m.getNome(), 0.0);
		for (Uso u : usos)
			mapa.merge(u.getNomeMaquina(), u.getKwh(), Double::sum);
		return mapa;
	}
	/*
	 * 
	 * Será feito uma atualização nessa classe no dia 19/11/2026
	 * Para que as infomações cadastradas sejam atualizadas em tempo real
	 * 
	 * Tambem será adicionada a função para o gráfico da placa solar mostrar a produção em tempo real com base nos segudnos
	 */
	
}