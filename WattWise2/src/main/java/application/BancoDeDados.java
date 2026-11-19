/*
|================================================================|
|  CLASSE PARA O BANCO DE DADOS DO SISTEMA WitteWise             |       
|================================================================|
*/

package application;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class BancoDeDados {
    private Connection connection = null;

    // Método auxiliar para carregar as propriedades do arquivo db.properties
    private Properties carregarPropriedades() {
        Properties props = new Properties();
        // Tenta carregar o arquivo a partir do classpath (ex: pasta src/main/resources)
        try (InputStream input = BancoDeDados.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                throw new RuntimeException("Arquivo db.properties não foi encontrado no classpath!");
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar o arquivo db.properties: " + e.getMessage(), e);
        }
        return props;
    }

    // Método para conectar ao banco usando o db.properties
    public void conectar() {
        Properties props = carregarPropriedades();
        
        String servidor = props.getProperty("db.servidor"); 
        String usuario = props.getProperty("db.usuario");
        String senha = props.getProperty("db.senha");
        String driver = props.getProperty("db.driver");

        try {
            Class.forName(driver);
            this.connection = DriverManager.getConnection(servidor, usuario, senha);
        } catch (Exception e) {
            System.out.println("Erro ao conectar ao banco: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public Connection getConnection() throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            conectar();
        }

        return this.connection;
    }

    // Método para verificar se está conectado
    public boolean estarConectado() {
        try {
            return this.connection != null && !this.connection.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    // Método para desconectar
    public void desconectar() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                this.connection.close();
            }
        } catch (Exception e) {
            System.out.println("Erro ao desconectar: " + e.getMessage());
        }
    }

    // Método para inserir contas (Cadastro)
    public boolean inserirContas(String nome, String email, String senha, String data_nascimento) {
        String query = "INSERT INTO usuarios (nome, email, senha, data_nascimento) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(query)) {
            pstmt.setString(1, nome);
            pstmt.setString(2, email);
            pstmt.setString(3, senha);
            pstmt.setString(4, data_nascimento);
            pstmt.executeUpdate();
            return true; 
        } catch (Exception e) {
            System.out.println("Erro ao cadastrar: " + e.getMessage());
            return false; 
        }
    }

    // Função para validar login
    public boolean validaLogin(String email, String senha) {
        String query = "SELECT * FROM usuarios WHERE email = ? AND senha = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(query)) {
            pstmt.setString(1, email);
            pstmt.setString(2, senha);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); 
            }

        } catch (Exception e) {
            System.out.println("Erro ao validar login: " + e.getMessage());
            return false;
        }
    }
    
    // Método para cadastrar uma máquina nova
    public boolean inserirMaquina(String nome, double consumo_kw, String imagem) {
        String query = "INSERT INTO maquina_nova (nome, consumo_kw, quantidade, imagem) VALUES (?, ?, 1, ?)";
        
        try(PreparedStatement pstmt = getConnection().prepareStatement(query)){
            pstmt.setString(1, nome);
            pstmt.setDouble(2, consumo_kw);
            pstmt.setString(3, imagem);
            pstmt.executeUpdate();
            return true;
        }catch(Exception e) {
            System.out.println("Erro ao cadastrar a máquina: " + e.getMessage());
            return false;
        }
    }
    
    // Método para listar todas as máquinas
    public List<Maquina> listarMaquinas() {
        String query = "SELECT id, nome, consumo_kw, quantidade, imagem FROM maquina_nova ORDER BY nome";
        List<Maquina> lista = new ArrayList<>();
 
        try (PreparedStatement pstmt = getConnection().prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
 
            while (rs.next()) {
                lista.add(new Maquina(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getDouble("consumo_kw"),
                        rs.getInt("quantidade"),
                        rs.getString("imagem")));
            }
            return lista;
 
        } catch (Exception e) {
            System.out.println("Erro ao listar máquinas: " + e.getMessage());
            return null;
        }
    }
    
    // Método para alterar a quantidade de unidades de máquina
    public boolean alterarQuantidadeMaquina(int id, int novaQuantidade) {
        String query = "UPDATE maquina_nova SET quantidade = ? WHERE id = ?";
        
        try(PreparedStatement pstmt = getConnection().prepareStatement(query)){
            pstmt.setInt(1, novaQuantidade);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
            return true;
        }catch(Exception e) {
            System.out.println("Não foi possível alterar as máquinas: " + e.getMessage());
            return false;
        }
    }
    
    // Método para remover uma máquina por completo
    public boolean removerMaquina(int id) {
        String query = "DELETE FROM maquina_nova WHERE id = ?";
        
        try(PreparedStatement pstmt = getConnection().prepareStatement(query)){
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            return true;
        }catch(Exception e) {
            System.out.println("Não foi possível remover a máquina: " + e.getMessage());
            return false;
        }
    }
    
    // Registra horas de uso de uma máquina (tela Calcular Conta)
    public boolean inserirUso(int maquinaId, double horas) {
        String query = "INSERT INTO uso_maquina (maquina_id, horas) VALUES (?, ?)";
        
        try(PreparedStatement pstmt = getConnection().prepareStatement(query)){
            pstmt.setInt(1, maquinaId);
            pstmt.setDouble(2, horas);
            pstmt.executeUpdate();
            return true;
        }catch(Exception e) {
            System.out.println("Erro ao registrar uso: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Soma das horas de uso de cada máquina: id da máquina -> total de horas.
    public Map<Integer, Double> somarHorasPorMaquina(){
        String query = "SELECT maquina_id, SUM(horas) AS total FROM uso_maquina GROUP BY maquina_id";
        Map<Integer, Double> mapa = new HashMap<>();
        
        try (PreparedStatement pstmt = getConnection().prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
    
            while (rs.next()) {
                mapa.put(rs.getInt("maquina_id"), rs.getDouble("total"));
            }
            return mapa;
    
        } catch (Exception e) {
            System.out.println("Erro ao somar horas de uso: " + e.getMessage());
            return null;
        }
    }
}