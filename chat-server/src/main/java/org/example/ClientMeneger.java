package org.example;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientMeneger implements Runnable {

    private final Socket socket;
    public final static ArrayList<ClientMeneger> clients = new ArrayList<>();
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    private String name;

    public ClientMeneger(Socket socket) {
        this.socket = socket;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            name = bufferedReader.readLine();
            clients.add(this);
            System.out.println(name + " подключился к беседе");
            broadcastMessage("Пользователь " + name + " подключился к чату");
        }
        catch(IOException e) {
            closeEverything(socket, bufferedWriter, bufferedReader);
        }
    }

    @Override
    public void run() {
        String messageFromClient;
        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();
                // проверка на наличие спец символа '@'
                if (personalMessege(messageFromClient)) {
                    String[] destinationAndMessage = parseForPersonalMessage(messageFromClient);
                    // проверка на отсутствие сообщения
                    if (destinationAndMessage[2].equals("")) {
                        for (ClientMeneger client:clients) {
                            if (client.name.equals(destinationAndMessage[1])) {
                                client.bufferedWriter.write(destinationAndMessage[0] + destinationAndMessage[2]);
                                client.bufferedWriter.newLine();
                                client.bufferedWriter.flush();
                            }
                        }
                    } else {
                        bufferedWriter.write("Вы отправили пустое сообщение");
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    }
                } else broadcastMessage(messageFromClient);

            } catch (IOException e) {
                closeEverything(socket, bufferedWriter, bufferedReader);
                break;
            }
        }
    }

    private void broadcastMessage(String message) {

        for (ClientMeneger client : clients) {
            try {
                if (!client.name.equals(name)) {
                    client.bufferedWriter.write(message);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                }

            } catch (IOException e) {
                closeEverything(socket, bufferedWriter, bufferedReader);
            }
        }
    }

    /**
     * Раздел полученного сообщения на части
     * @param message
     * @return Возвращает массив строк destinationAndMessage, состоящий из 3 элементов: 1-й - имя отправителя,
     * 2-й - адресат (уже без символа '@'), 3-й - сообщение
     */
    private String[] parseForPersonalMessage(String message) {
            String[] destinationAndMessage = new String[3];
            String[] partsOfMessage = message.split(" ");
            destinationAndMessage[0] = partsOfMessage[0] + " ";
            destinationAndMessage[1] = partsOfMessage[1].substring(1);

            String newMessage = "";
            for (int i = 2; i < partsOfMessage.length - 1; i++) {
                newMessage += partsOfMessage[i] + " ";
            }
            newMessage += partsOfMessage[partsOfMessage.length - 1];
            destinationAndMessage[2] = newMessage;

            return destinationAndMessage;
    }

    /**
     * Проверка на наличие спец символа '@'
     * @param message От Client мы получаем конструкцию bufferedWriter.write(name + ": " + message).
     *                Поэтому ищем символ '@' во втором слове
     * @return
     */
    private boolean personalMessege(String message) {
        String[] parts = message.split(" ");
        if (parts[1].charAt(0) == '@') return true;
        return false;
    }

    private void closeEverything(Socket socket, BufferedWriter bufferedWriter, BufferedReader bufferedReader) {
        removeClient();
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeClient() {
        clients.remove(this);
        System.out.println(name + " покинул чат");
        broadcastMessage("Пользователь " + name + " покинул чат");
    }
}
