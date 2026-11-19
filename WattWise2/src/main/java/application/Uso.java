package application;

public class Uso {
	private final String nomeMaquina;
	
	private final double kw;
	
	private final double horas;
	
	public Uso(String nomeMaquina, double kw, double horas) {
		this.nomeMaquina = nomeMaquina;
		this.kw = kw;
		this.horas = horas;
	}
	public String getNomeMaquina() {
		return nomeMaquina;
	}
	// Parte onded calcula a quantidade de kwh consumidos por kw e horas
	// kw x horas = kwh
	public double getKwh() {
		return kw * horas;
	}
}
