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
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (\n" +
                " id    INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                " nick  TEXT,\n" +
                " login  TEXT,\n" +
                " password  TEXT\n" +
                " );");
        //Для задания проверяем по Николаю необходимость вставки
        ResultSet r = stmt.executeQuery("SELECT COUNT() FROM users WHERE nick = 'Коля';");
        if (r.getInt(1) < 1) {
            stmt.executeUpdate("INSERT INTO users (nick, login, password) VALUES " +
                    "('Коля', 'login0', 'pass0'), " +
                    "('Боря', 'login1', 'pass1'), " +
                    "('Костя', 'login2', 'pass2') " +
                    ";");
        }
    }

    public synchronized String getNickByLoginAndPassword (String inputLogin, String inputPassword) throws IOException {
        String login = inputLogin.replaceAll("-|'", "");
        String password = inputPassword.replaceAll("-|'", "");


        try {
            String query = "SELECT COUNT (*), nick FROM users WHERE " +
                    "login = '" + login + "' AND password = '" + password + "';";
            //System.out.println(query);
            ResultSet rs = stmt.executeQuery(query);

            if (rs.getInt(1) > 0) {
                return rs.getString (2);
            }
        } catch (SQLException e) {
            throw new IOException(e);
        }

        return null;
    }

    public String renameNick(String clientNick, String inputNewNick) {
        String newNick = inputNewNick.replaceAll("[.'-]", "");
        String query = "UPDATE users SET nick = '" + newNick + "' WHERE nick = '" + clientNick + "';";
        try {
            int queryResult = stmt.executeUpdate(query);
            if (queryResult > 0) {
                return newNick;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
