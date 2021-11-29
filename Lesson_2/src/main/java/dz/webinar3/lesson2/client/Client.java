package dz.webinar3.lesson2.client;


import dz.webinar3.lesson2.Commands;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Client extends Application {

    private final String SERVER_ADDR = "localhost";

    private final int SERVER_PORT = 7777;
    private Thread inMsgListener;
    private Thread outMsgSender;
    private Thread socketHandler;
    private Boolean isConnectionOn = false;
    private boolean isConnecting = true;

    private Controller controller;

    private long lastTimeMessage;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private long startTimeForTestMessage = System.currentTimeMillis();
    private static long PERIOD_TEST_SERVER = 10000; //Каждые 10 секунд отправляем серверу CMD_TEST

    private final ConcurrentLinkedQueue <String> queueToSend = new ConcurrentLinkedQueue<>();

    private Stage stageFX;

    @Override
    public void start(Stage stage) throws IOException {
        stageFX = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(Client.class.getResource("chat.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Chat");
        stage.setAlwaysOnTop(true);
        stage.setScene(scene);
        controller = fxmlLoader.getController();
        controller.setClient(this);

        startChat();
        stage.show();
    }

    private void setConnectionOn (boolean connectionOn) {
        synchronized (isConnectionOn) {
            isConnectionOn = connectionOn;
        }
    }

    private boolean getConnectionOn () {
        synchronized (isConnectionOn) {
            return isConnectionOn;
        }
    }

    private void startChat() {
        socketHandler = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (!getConnectionOn()) {
                            if (!isServerLive ()) {
                                closeSocketIfOpened();
                                socket = new Socket(SERVER_ADDR, SERVER_PORT);
                                in = new DataInputStream(socket.getInputStream());
                                out = new DataOutputStream(socket.getOutputStream());
                                setConnectionOn(true);
                                isConnecting = true;
                                lastTimeMessage = System.currentTimeMillis();
                            }
                        }
                    } catch (ConnectException e) {
                        if (isConnecting) {
                            controller.showMessage("Подключаемся к серверу ...");
                            isConnecting = false;
                        }
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("Поток подключения к серверу остановлен");
                        break;
                    }
                }
            }

            private boolean isServerLive() {
                if (socket == null || in == null || out == null) {
                    return false;
                }

                if (!isConnectionOn) {
                    return false;
                }

                return true;
            }

            private void closeSocketIfOpened() {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        System.out.println("Не удалось закрыть InputStream у сокета");
                    } finally {
                        in = null;
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        System.out.println("Не удалось закрыть OutputStream у сокета");
                    }
                    finally {
                        out = null;
                    }
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        System.out.println("Не удалось закрыть сокет");
                    } finally {
                        socket = null;
                    }
                }
            }
        });
        socketHandler.setDaemon(true);
        socketHandler.start ();

        outMsgSender = new Thread(new Runnable() {
            @Override
            public void run() {
                String messageOut;
                while (true) {
                    try {
                        if (getConnectionOn()) {
                            if (!queueToSend.isEmpty()) {
                                messageOut = queueToSend.poll();
                                out.writeUTF(messageOut);
                                out.flush();
                            }

                            if (System.currentTimeMillis() - startTimeForTestMessage > PERIOD_TEST_SERVER) {
                                out.writeUTF(Commands.CMD_TEST);
                                out.flush();
                                startTimeForTestMessage = System.currentTimeMillis();
                            }
                        }
                        Thread.sleep(50);
                    } catch (UTFDataFormatException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        System.out.println("Поток, отправляющий сообщения, остановлен");
                    } catch (IOException e) {
                        setConnectionOn(false);
                    } catch (Exception e) {
                        setConnectionOn(false);
                    }
                }
            }
        });
        outMsgSender.setDaemon(true);
        outMsgSender.start ();

        inMsgListener = new Thread(new Runnable() {
            @Override
            public void run() {
                String messageIn;
                while (true) {
                    try {
                        if (getConnectionOn()) {
                            if (in.available() > 0) {
                                messageIn = in.readUTF();
                                System.out.println(messageIn);
                                if (messageIn != null) {
                                    if (Commands.CMD_AUTH_TIMEOUT.equals(messageIn)) {
                                        controller.showMessage("Время на аутентификацию прошло.");
                                        throw new Exception("auth timeout");
                                    } else if (Commands.CMD_OK.equals(messageIn)) {
                                        startTimeForTestMessage = System.currentTimeMillis();
                                    } else if (System.currentTimeMillis() - startTimeForTestMessage > PERIOD_TEST_SERVER * 3) {
                                        setConnectionOn(false);
                                    } else if (messageIn.startsWith(Commands.CMD_HELLO)) {
                                        changeWindowTitle (messageIn);
                                    } else {
                                        controller.showMessage(messageIn);
                                    }
                                }
                            }
                        }
                        Thread.sleep(50);
                    } catch (UTFDataFormatException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        System.out.println("Поток, ожидающий входящие сообщения, остановлен");
                    } catch (IOException e) {
                        setConnectionOn(false);
                    } catch (Exception e) {
                        setConnectionOn(false);
                    }
                }
            }

            private void changeWindowTitle(String messageIn) {
                String title;
                try {
                    title = "Чат: " + messageIn.split(" ")[1];
                    stageFX.setTitle(title);
                } catch (ArrayIndexOutOfBoundsException e) {
                    return;
                }
            }
        });
        inMsgListener.setDaemon(true);
        inMsgListener.start();
    }

    public static void main(String[] args) {
        launch();
    }

    public void sendMessage(String message) {
        queueToSend.add (message);
    }
}