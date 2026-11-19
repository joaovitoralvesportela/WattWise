package application;

import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

public class RelatorioPDF {

	private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");

	private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(BRASIL);
	private static final NumberFormat NUMERO = NumberFormat.getNumberInstance(BRASIL);

	static {
		// sempre 2 casas decimais nos números
		NUMERO.setMinimumFractionDigits(2);
		NUMERO.setMaximumFractionDigits(2);
	}

	/**
	 * Gera o PDF. Retorna true se salvou e false se o usuário cancelou a escolha do arquivo.
	 * Lança exceção se não houver consumo ou se der erro ao gravar.
	 */
	public static boolean exportar(Window janela) throws Exception {

		Dadosapp dados = Dadosapp.getInstancia();

		double consumo = dados.consumoTotal();

		if (consumo <= 0) {
			throw new IllegalStateException("Adicione informações de consumo antes de gerar um PDF.");
		}

		FileChooser seletor = new FileChooser();
		seletor.setTitle("Salvar relatório WattWise");
		seletor.setInitialFileName("Relatorio_WattWise.pdf");
		seletor.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));

		File arquivo = seletor.showSaveDialog(janela);

		if (arquivo == null) {
			return false; // usuário cancelou
		}

		// garante a extensão pdf
		if (!arquivo.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
			arquivo = new File(arquivo.getAbsolutePath() + ".pdf");
		}

		// resultados calculados a partir dos dados atuais
		double custoMensal = dados.custoTotal();
		double co2 = Dadosapp.co2Kg(consumo);
		double arvores = Dadosapp.arvoresEquivalentes(consumo);
		Dadosapp.ResultadoSolar solar = dados.calculoSolar();

		Document documento = new Document(PageSize.A4);

		try {
			PdfWriter.getInstance(documento, new FileOutputStream(arquivo));
			documento.open();

			Font titulo = new Font(Font.HELVETICA, 22, Font.BOLD);
			Font subtitulo = new Font(Font.HELVETICA, 14, Font.BOLD);

			documento.add(new Paragraph("WattWise - Relatório de consumo e eficiência energética", titulo));
			documento.add(new Paragraph("Data de emissão: "
					+ LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
			documento.add(new Paragraph(" "));

			// 1. Resumo de consumo
			documento.add(new Paragraph("1. Resumo de consumo", subtitulo));
			documento.add(new Paragraph(" "));

			PdfPTable tabela = new PdfPTable(2);
			tabela.setWidthPercentage(100);
			
			// Vai mostrar o indicador e o resutado
			adicionarCabecalho(tabela, "Indicador", "Resultado");
			
			// vai mostrar o consumo mensal
			adicionarLinha(tabela, "Consumo mensal", NUMERO.format(consumo) + " kWh");
			
			// Vai mostrar o custo mensal
			adicionarLinha(tabela, "Custo mensal estimado", MOEDA.format(custoMensal));
			
			// Vai mostrar a emissao estimada de co2
			adicionarLinha(tabela, "Emissão estimada de CO2", NUMERO.format(co2) + " kg");
			
			// vai mostrar a qtd de arvores equivalentes por ano
			adicionarLinha(tabela, "Árvores equivalentes por ano", NUMERO.format(arvores));

			documento.add(tabela);
			documento.add(new Paragraph(" "));

			// 2. Energia solar
			documento.add(new Paragraph("2. Estimativa de energia solar", subtitulo));
			documento.add(new Paragraph(" "));

			PdfPTable tabelaSolar = new PdfPTable(2);
			tabelaSolar.setWidthPercentage(100);

			adicionarCabecalho(tabelaSolar, "Indicador", "Resultado");
			
			// Vai mostrar a quantidade de painel solar necessarios
			adicionarLinha(tabelaSolar, "Painéis necessários", String.valueOf(solar.paineis));
			
			// Vai mostrar a potencia instalada 
			adicionarLinha(tabelaSolar, "Potência instalada", NUMERO.format(solar.potenciaKwp) + " kWp");
			
			// Vai mostrar a geracao mensal estimada
			adicionarLinha(tabelaSolar, "Geração mensal estimada", NUMERO.format(solar.geracaoMensalKwh) + " kWh");
			
			// Vai mostrar o custo estimado
			adicionarLinha(tabelaSolar, "Custo estimado de instalação", MOEDA.format(solar.custoTotal));
			
			// Vai mostrar a economia mensal estimado
			adicionarLinha(tabelaSolar, "Economia mensal estimada", MOEDA.format(solar.economiaMensal));
			
			// Vai mostrar o retorno estimado em anos
			adicionarLinha(tabelaSolar, "Retorno estimado", NUMERO.format(solar.paybackAnos) + " anos");

			documento.add(tabelaSolar);
			documento.add(new Paragraph(" "));
			
			// Mensagem de observacao!
			documento.add(new Paragraph("Observação: os valores são estimativas baseadas "
					+ "nos dados informados e nos parâmetros configurados no WattWise. "
					+ "A geração solar real, a economia e o prazo de retorno podem variar."));
		} finally {
			if (documento.isOpen()) {
				documento.close();
			}
		}

		return true;
	} // <- aqui termina o método exportar()

	private static void adicionarCabecalho(PdfPTable tabela, String coluna1, String coluna2) {
		Font negrito = new Font(Font.HELVETICA, 12, Font.BOLD);
		tabela.addCell(new Phrase(coluna1, negrito));
		tabela.addCell(new Phrase(coluna2, negrito));
	}

	private static void adicionarLinha(PdfPTable tabela, String coluna1, String coluna2) {
		tabela.addCell(new Phrase(coluna1));
		tabela.addCell(new Phrase(coluna2));
	}
}