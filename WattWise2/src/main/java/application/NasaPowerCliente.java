package application;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;


/*
 * 
 * =========================================================================================================|
 * 																											|
 * API PARA FONTE DE DADOS DA NASA 																			|
 * PARA INFORMAÇÕES MAIS PRECISAS E DE TEMPO REAL															|		
 * 																											|
 * PARÂMETROS UTILIZADOS: ALLSKY_SFC_SW_DWN -> Radiação Solar que chega a superficie , em kwh/m2/dia		|
 * 																											|
 * =========================================================================================================|																									|
 */	

public class NasaPowerCliente {
	
	// Informações https site nasa
	private static final String BASE = "https://power.larc.nasa.gov/api/temporal/";
	private static final String PARAMETRO = "ALLSKY_SFC_SW_DWN";

	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	/** Uma leitura de um dia: radiação em kWh/m². */
	public static class LeituraDiaria {
		public final LocalDate data;
		public final double kwhPorM2;

		public LeituraDiaria(LocalDate data, double kwhPorM2) {
			this.data = data;
			this.kwhPorM2 = kwhPorM2;
		}
	}
	
	/*
	 * 
	 * Média de radiação por mês(Chaves por JAn..DEC) e a media do ano(chave ann)
	 *  em kWh/m²/dia. Uma única requisição, ideal para estimar a geração.
	 */
	public static Map<String, Double> mediasMensais(double latitude, double longitude) throws IOException, InterruptedException {
		
		String url = BASE + "climatology/point" + "?parameters=" + PARAMETRO + "&&community=RE" +
		"&longitude=" + numero(longitude) +
		"&latitude=" + numero(latitude) +
		"&format=JSON";
		
		return lerParametro(buscar(url));
	}
	// Media anual de radiação, em kwh/m2/dia. Este é o valor que entrar na conta de geração
	
	public static double mediaAnual(double latitude, double longitude)
			throws IOException, InterruptedException {

		Double anual = mediasMensais(latitude, longitude).get("ANN");
		if (anual == null) {
			throw new IOException("A NASA POWER não devolveu a média anual para este local.");
		}
		return anual;
	}
	
	/*
	 * Radiação nos últimos dias disponiveis. A nasa leva entre 2 a 7 dias para publicar , 
	 * por isso a janela termina 7 dias atrás , Dias sem dados(-999) são ignorados
	 *
	 */
	
	public static List<LeituraDiaria> ultimosDias(double latitude, double longitude, int dias)
			throws IOException, InterruptedException {

		LocalDate fim = LocalDate.now().minusDays(7);
		LocalDate inicio = fim.minusDays(dias - 1L);

		String url = BASE + "daily/point"
				+ "?start=" + inicio.format(DateTimeFormatter.BASIC_ISO_DATE)
				+ "&end=" + fim.format(DateTimeFormatter.BASIC_ISO_DATE)
				+ "&latitude=" + numero(latitude)
				+ "&longitude=" + numero(longitude)
				+ "&community=RE"
				+ "&parameters=" + PARAMETRO
				+ "&format=JSON";

		Map<String, Double> porData = lerParametro(buscar(url));

		List<LeituraDiaria> leituras = new ArrayList<>();
		for (Map.Entry<String, Double> e : porData.entrySet()) {
			LocalDate data = LocalDate.parse(e.getKey(), DateTimeFormatter.BASIC_ISO_DATE);
			leituras.add(new LeituraDiaria(data, e.getValue()));
		}
		return leituras;
	}
	// --------------------------------------------------
	
	// LOCAL.US garante ponto decimal. Com a virgula do pt-br a url ficaria errada
	
	private static String numero(double valor) {
		return String.format(Locale.US, "%.4f", valor);
	}

	private static String buscar(String url) throws IOException, InterruptedException {
		HttpRequest requisicao = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(15))
				.GET()
				.build();

		HttpResponse<String> resposta = HTTP.send(requisicao, HttpResponse.BodyHandlers.ofString());

		if (resposta.statusCode() != 200) {
			throw new IOException("NASA POWER respondeu com status " + resposta.statusCode());
		}
		return resposta.body();
	}
	
	//  Lê properties.parameter.ALLSKY_SFC_SW_DWN e devolve chave -> valor, sem os -999.
	
	private static Map<String, Double> lerParametro(String json) throws IOException {
		try {
			JsonObject parametro = JsonParser.parseString(json).getAsJsonObject()
					.getAsJsonObject("properties")
					.getAsJsonObject("parameter")
					.getAsJsonObject(PARAMETRO);

			Map<String, Double> resultado = new TreeMap<>();
			for (Map.Entry<String, JsonElement> e : parametro.entrySet()) {
				double valor = e.getValue().getAsDouble();
				if (valor > -990) { // -999 = sem dado
					resultado.put(e.getKey(), valor);
				}
			}
			return resultado;
		} catch (RuntimeException ex) {
			throw new IOException("Resposta inesperada da NASA POWER.", ex);
		}
	}
	
}
