package dz.webinar3.lesson2.server.db;


import org.sqlite.SQLiteConnection;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;

public class AuthDB {
    private Connection connection;
    private Statement stmt;

    public AuthDB () {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:/home/dz/dbs/users.db");//:memory:
            stmt = connection.createStatement();
            initDB();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException exc) {
                exc.printStackTrace();
            }
        }
    }

    private void initDB() throws SQLException {
        //Пересоздаем таблицу для задания
        stmt.execute("DROP TABLE IF EXISTS users;");
        stmt.executeUpdate("CREATE TABLE users (\n" +
                " id    INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                " nick  TEXT,\n" +
                " login  TEXT,\n" +
                " password  TEXT\n" +
                " );");
        ResultSet r = stmt.executeQuery("SELECT COUNT() FROM users WHERE nick = 'Коля';");
        stmt.executeUpdate("INSERT INTO users (nick, login, password) VALUES " +
                "('Коля', 'login0', 'pass0'), " +
                "('Боря', 'login1', 'pass1'), " +
                "('Маша', 'login2', 'pass2'), " +
                "('Костя', 'login3', 'pass3') " +
                ";");
    }

    public synchronized String getNickByLoginAndPassword (String inputLogin, String inputPassword) throws IOException {
        try {
            String query = "SELECT COUNT (*), nick FROM users WHERE " +
                    "login = ? AND password = ?;";
            //System.out.println(query);
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, inputLogin);
            pstmt.setString(2, inputPassword);
            ResultSet rs = pstmt.executeQuery();

            if (rs.getInt(1) > 0) {
                return rs.getString (2);
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }

        return null;
    }

    public String renameNick(String clientNick, String inputNewNick) {
        String query = "UPDATE users SET nick = ? WHERE nick = ?;";
        try {
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, inputNewNick);
            pstmt.setString(2, clientNick);
            int queryResult = pstmt.executeUpdate();

            if (queryResult > 0) {
                //Не нашел ничего лучшего, чем получить через базу данных обработанную
                //prepareStatement строку
                pstmt = connection.prepareStatement("SELECT nick FROM users WHERE nick = ?;");
                pstmt.setString(1, inputNewNick);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getString(1);
                } else {
                    throw new SQLException();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
