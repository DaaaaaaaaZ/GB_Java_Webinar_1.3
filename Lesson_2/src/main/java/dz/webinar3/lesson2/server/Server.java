package dz.webinar3.lesson2.server;


import dz.webinar3.lesson2.server.db.AuthDB;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static dz.webinar3.lesson2.Commands.*;


public class Server {
    private static final int SERVER_PORT = 7777;

    private static final long AUTH_TIMEOUT = 120000; //Таймаут для аутентификации в миллисекундах

    //private final CopyOnWriteArrayList<Entry> clientDataList = new CopyOnWriteArrayList<>();

    private final ConcurrentHashMap <String, ClientUnit> clients =
            new ConcurrentHashMap<>(); //String - ник, ClientUnit - его модель
    private final ConcurrentHashMap <Socket, Long> waitingForAuth = new ConcurrentHashMap<>();
    private ServerSocket serverSocket;
    private final ConcurrentLinkedQueue <String> broadcastMsgQueue = new ConcurrentLinkedQueue<>();
    private AuthDB authDB;

    public Server () {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            authDB = new AuthDB();
        } catch (IOException e) {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            System.out.println("Сервер не запускается");
            System.exit(-1);
        }
        /*
        clientDataList.add (new Entry("Коля", "login0", "pass0"));
        clientDataList.add (new Entry("Боря", "login1", "pass1"));
        clientDataList.add (new Entry("Костя", "login2", "pass2"));
         */
    }

    public static void main(String[] args) {
        new Server().startServer();
    }

    private void startServer () {
        Socket socket;
        startBroadcastSender ();

        try {
            while (true) {
                socket = serverSocket.accept();
                waitingForAuth.put (socket, System.currentTimeMillis());
                startSocketListener (socket);
            }
        } catch (IOException e) {
            System.out.println("Работа сервера нарушена");
            e.printStackTrace();
        }
    }

    private void startBroadcastSender() {
        Thread broadcastSenderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StringBuilder broadcastMsg = new StringBuilder();

                    while (true) {
                        if (!broadcastMsgQueue.isEmpty()) {
                            broadcastMsg.append(broadcastMsgQueue.poll());
                            System.out.println(broadcastMsg);
                            for (Map.Entry<String, ClientUnit> client : clients.entrySet()) {
                                try {
                                    client.getValue().getOut().writeUTF(broadcastMsg.toString());
                                    client.getValue().getOut().flush();
                                } catch (IOException e) {
                                    System.out.println("Ошибка при отправке broadcast-сообщения пользователю "
                                            + client.getKey());
                                }
                            }
                        }
                        broadcastMsg.setLength(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("BroadcastSender остановлен");
                }
            }
        });
        broadcastSenderThread.setDaemon(true);
        broadcastSenderThread.start();
    }

    private void startSocketListener(Socket socket) {
        Thread socketListenerThread = new Thread(new Runnable() {
            private String inMsg = null;
            private String outMsg = null;
            private String wispNick = null;
            private String clientNick = null;
            private StringBuilder wispMsg = new StringBuilder();
            private String [] inAuthArray = null;
            private String [] inMsgArray = null;
            private boolean isAuthOk = false;
            private String ipAdress = socket.getInetAddress().toString();
            private int port = socket.getPort();
            private long startTimeForAuth = System.currentTimeMillis();
            private DataInputStream in;
            private DataOutputStream out;
            private boolean isStartMessageSend = false;

            @Override
            public void run() {
                try (
                        DataInputStream tempIn = new DataInputStream(socket.getInputStream());
                        DataOutputStream tempOut = new DataOutputStream(socket.getOutputStream())
                    ) {
                    in = tempIn;
                    out = tempOut;

                    while (!isAuthOk) {//Цикл аутентификации
                        try {
                            if (!isStartMessageSend) {
                                out.writeUTF("Для аутентификации введите \n" + CMD_AUTH + " login password");
                                out.flush();
                                isStartMessageSend = true;
                            }

                            try {//Ждем сообщений от клиента и проверяем время таймаута
                                waitForAuthString();
                            } catch (InterruptedException e) {
                                System.out.println(ipAdress + ": " + port + " не успел пройти аутентификацию");
                                throw new SocketException("");
                            }
                            //Читаем сообщение для попытки аутентификации
                            inMsg = in.readUTF();
                            inAuthArray = inMsg.split(" ");
                            if (inAuthArray.length == 1) {
                                if (CMD_TEST.equals(inMsg)) {
                                    out.writeUTF(CMD_OK);
                                } else {
                                    out.writeUTF("Для аутентификации введите \n" + CMD_AUTH
                                            + " login password");
                                }
                                continue;
                            } else {
                                if (CMD_AUTH.equals(inAuthArray[0])) {
                                    try {
                                        verifyAuthString ();
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        out.writeUTF("Для аутентификации введите \n" + CMD_AUTH
                                                + " login password");
                                    }

                                }
                            }
                        } catch (EOFException e) {
                            continue;
                        } catch (UTFDataFormatException e) {
                            out.writeUTF("Ошибка при аутентификации, \nпопробуйте еще раз");
                        }
                    } //Конец цикла аутентификации

                    while (true) {//Рабочий цикл для чтения и отправки сообщений
                        try {
                            inMsg = in.readUTF();
                        } catch (EOFException e) {
                            continue;
                        }
                        if (inMsg.isEmpty() || inMsg.isBlank()) {
                            continue;
                        }

                        inMsgArray = inMsg.split(" ");
                        if (CMD_WISPER.equals (inMsgArray [0])) {
                            try {
                                sendPrivateMessage ();
                            } catch (ArrayIndexOutOfBoundsException e) {
                                continue;
                            }
                        } if (CMD_TEST.equals (inMsgArray [0])) {
                            out.writeUTF(CMD_OK);
                            continue;
                        } if (CMD_AUTH.equals(inMsgArray [0]) ||
                                CMD_OK.equals(inMsgArray[0])) {
                            continue;
                        } if (CMD_NEW_NICK.equals(inMsgArray[0])) {
                            try {
                                String newNick = authDB.renameNick(clientNick, inMsgArray [1]);
                                if (newNick != null) {
                                    broadcastMsgQueue.add (clientNick + " сменил ник на " + newNick);
                                    clientNick = newNick;
                                }
                            } catch (ArrayIndexOutOfBoundsException e) {
                                continue;
                            }
                        } else {
                            broadcastMsgQueue.add (clientNick + ": " + inMsg);
                        }

                    }//Конец цикла входящий сообщений

                } catch (SocketException e) {
                    if (socket != null) {
                        System.out.println("Сокет " + socket.getInetAddress() + ":"
                                + socket.getPort() + " закрыт");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            private void sendPrivateMessage() throws IOException {
                for (Map.Entry<String, ClientUnit> client : clients.entrySet()) {
                    if (client.getKey().equals(inMsgArray [1])) {
                        if (inMsgArray.length > 2) {
                            wispMsg.append(clientNick + ": ");
                            for (int i = 2; i < inMsgArray.length; i++) {
                                wispMsg.append (inMsgArray [i]);
                                if (i != inMsgArray.length - 1) {
                                    wispMsg.append (" ");
                                }
                            }
                            client.getValue().getOut().writeUTF(wispMsg.toString());
                            client.getValue().getOut().flush();
                            wispMsg.setLength(0);
                        }
                    }
                }
            }

            private void verifyAuthString() throws IOException, ArrayIndexOutOfBoundsException {
                String tempNick = "";
                String inputLogin = inAuthArray[1];
                String inputPassword = inAuthArray[2];
                tempNick = authDB.getNickByLoginAndPassword (inputLogin, inputPassword);

                    if (tempNick != null) {
                        if (clients.containsKey(tempNick)) {
                            out.writeUTF("Пользователь " + tempNick +
                                    " уже в чате");
                        } else {
                            clientNick = tempNick;
                            clients.put(clientNick, new ClientUnit(
                                    socket,
                                    in,
                                    out,
                                    clientNick
                            ));
                            isAuthOk = true;
                            broadcastMsgQueue.add(clientNick + " зашёл в чат");
                        }
                    }

                if (!isAuthOk) {
                    out.writeUTF("Для аутентификации введите \n" + CMD_AUTH + " login password");
                }
            }

            //
            private void waitForAuthString() throws IOException, InterruptedException {
                while (in.available() == 0) {
                    if (System.currentTimeMillis() - startTimeForAuth > AUTH_TIMEOUT) {
                        out.writeUTF(CMD_AUTH_TIMEOUT);
                        out.flush();
                        throw new SocketException("");
                    }
                    Thread.sleep(50);
                }
            }

        });
        socketListenerThread.setDaemon(true);

        socketListenerThread.start();
    }
}