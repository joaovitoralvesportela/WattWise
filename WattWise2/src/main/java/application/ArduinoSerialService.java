package application;

import com.fazecast.jSerialComm.SerialPort;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;

import java.nio.charset.StandardCharsets;

public class ArduinoSerialService {

    private static final ArduinoSerialService INSTANCIA = new ArduinoSerialService();

    private SerialPort portaAtiva;
    private Thread threadLeitura;
    private volatile boolean executando = false;

    private final DoubleProperty tensaoVolts = new SimpleDoubleProperty(0.0);
    private final DoubleProperty correnteMa = new SimpleDoubleProperty(0.0);
    private final DoubleProperty potenciaWatts = new SimpleDoubleProperty(0.0);
    private final DoubleProperty energiaGeradaKwh = new SimpleDoubleProperty(0.0);
    private final BooleanProperty conectado = new SimpleBooleanProperty(false);
    private final LongProperty timestampLeitura = new SimpleLongProperty(0);

    private long ultimoTimestampMs = 0;

    private ArduinoSerialService() {}

    public static ArduinoSerialService getInstancia() {
        return INSTANCIA;
    }

    public static SerialPort[] listarPortas() {
        return SerialPort.getCommPorts();
    }

    public boolean conectar(String nomePorta) {
        desconectar();

        SerialPort[] portas = SerialPort.getCommPorts();
        for (SerialPort p : portas) {
            if (nomePorta != null && p.getSystemPortName().equalsIgnoreCase(nomePorta)) {
                portaAtiva = p;
                break;
            }
            String desc = p.getDescriptivePortName().toLowerCase();
            if (nomePorta == null && (desc.contains("arduino") || desc.contains("ch340") || desc.contains("usb") || desc.contains("serial"))) {
                portaAtiva = p;
                break;
            }
        }

        if (portaAtiva == null && portas.length > 0) {
            portaAtiva = portas[0];
        }

        if (portaAtiva == null) {
            return false;
        }

        portaAtiva.setBaudRate(9600);
        // Timeout de 3000ms: espera dados chegarem sem devolver null imediato
        portaAtiva.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 3000, 0);

        if (!portaAtiva.openPort()) {
            System.err.println("Falha ao abrir a porta " + portaAtiva.getSystemPortName());
            return false;
        }

        portaAtiva.setDTR();
        portaAtiva.setRTS();

        System.out.println("Conectado com sucesso na porta: " + portaAtiva.getSystemPortName());
        executando = true;
        ultimoTimestampMs = System.currentTimeMillis();
        conectado.set(true);

        threadLeitura = new Thread(this::loopLeitura, "Thread-Arduino-INA219");
        threadLeitura.setDaemon(true);
        threadLeitura.start();

        return true;
    }

    private void loopLeitura() {
        StringBuilder bufferLinha = new StringBuilder();
        byte[] bufferBytes = new byte[256];

        try {
            // Aguarda 2s para o Arduino reiniciar ao abrir a porta COM
            Thread.sleep(2000);

            while (executando && portaAtiva.isOpen()) {
                int bytesLidos = portaAtiva.readBytes(bufferBytes, bufferBytes.length);

                if (bytesLidos <= 0) {
                    continue; // Nenhum byte novo ainda, apenas aguarda a próxima leitura
                }

                String trecho = new String(bufferBytes, 0, bytesLidos, StandardCharsets.UTF_8);
                bufferLinha.append(trecho);

                // Processa todas as linhas completas terminadas em \n
                int quebra;
                while ((quebra = bufferLinha.indexOf("\n")) != -1) {
                    String linha = bufferLinha.substring(0, quebra).trim();
                    bufferLinha.delete(0, quebra + 1);

                    System.out.println("[RAW COM8]: " + linha);

                    if (!linha.startsWith("{") || !linha.endsWith("}")) {
                        continue;
                    }

                    processarJson(linha);
                }
            }
        } catch (Exception e) {
            if (executando) {
                System.err.println("Erro na leitura serial: " + e.getMessage());
            }
        } finally {
            Platform.runLater(() -> conectado.set(false));
        }
    }

    private void processarJson(String linha) {
        try {
            JsonObject json = JsonParser.parseString(linha).getAsJsonObject();

            if (json.has("erro")) {
                System.err.println("Erro no sensor INA219: " + json.get("erro").getAsString());
                return;
            }

            if (!json.has("v")) {
                return; // Ignora mensagens de status ("INICIANDO_INA219", "INA219_OK")
            }

            double v = json.get("v").getAsDouble();
            double i = json.get("i").getAsDouble();
            double pWatts = json.get("p").getAsDouble() / 1000.0;

            long agora = System.currentTimeMillis();
            double deltaHoras = (agora - ultimoTimestampMs) / 3_600_000.0;
            ultimoTimestampMs = agora;
            double kwhIncremento = (pWatts / 1000.0) * deltaHoras;

            Platform.runLater(() -> {
                tensaoVolts.set(v);
                correnteMa.set(i);
                potenciaWatts.set(pWatts);
                energiaGeradaKwh.set(energiaGeradaKwh.get() + kwhIncremento);
                timestampLeitura.set(System.currentTimeMillis());
            });
        } catch (Exception ignored) {
            // Ignora pacote JSON incompleto no início da conexão
        }
    }

    public void desconectar() {
        executando = false;
        if (portaAtiva != null && portaAtiva.isOpen()) {
            portaAtiva.closePort();
        }
        Platform.runLater(() -> conectado.set(false));
    }

    public DoubleProperty tensaoVoltsProperty() { return tensaoVolts; }
    public DoubleProperty correnteMaProperty() { return correnteMa; }
    public DoubleProperty potenciaWattsProperty() { return potenciaWatts; }
    public DoubleProperty energiaGeradaKwhProperty() { return energiaGeradaKwh; }
    public BooleanProperty conectadoProperty() { return conectado; }
    public LongProperty timestampLeituraProperty() { return timestampLeitura; }
}