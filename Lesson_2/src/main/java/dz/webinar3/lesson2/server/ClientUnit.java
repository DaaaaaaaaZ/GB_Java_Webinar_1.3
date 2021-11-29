package dz.webinar3.lesson2.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientUnit {
    private final Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;
    private final String nick;
    private boolean isAuthOK = false;
    private boolean isNeedToDisconnect = false;

    public boolean isAuthOK() {
        return isAuthOK;
    }

    public void setAuthOK(boolean authOK) {
        isAuthOK = authOK;
    }

    public Socket getSocket() {
        return socket;
    }

    public DataOutputStream getOut() {
        return out;
    }

    public DataInputStream getIn() {
        return in;
    }

    public String getLogin() {
        return nick;
    }

    public ClientUnit(Socket socket, DataInputStream in, DataOutputStream out, String nick) {
        this.socket = socket;
        this.out = out;
        this.in = in;
        this.nick = nick;
    }
}
