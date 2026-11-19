package application;

/** Uma leitura da placa solar em um instante (os dados que o Arduino/INA219 enviou). */
public class LeituraSolar {

	private final double segundos;   // tempo desde o início do gráfico
	private final double tensaoV;    // tensão em volts
	private final double correnteMa; // corrente em miliampères
	private final double potenciaW;  // potência em watts

	public LeituraSolar(double segundos, double tensaoV, double correnteMa, double potenciaW) {
		this.segundos = segundos;
		this.tensaoV = tensaoV;
		this.correnteMa = correnteMa;
		this.potenciaW = potenciaW;
	}

	public double getSegundos() { return segundos; }
	public double getTensaoV() { return tensaoV; }
	public double getCorrenteMa() { return correnteMa; }
	public double getPotenciaW() { return potenciaW; }
}