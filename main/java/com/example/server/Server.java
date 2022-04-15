package com.example.server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.*;
import java.net.*;
import java.util.HashMap;

/**
 * Controls the server for PRISM chat application
 */
public class Server extends Application {



    /**
     * Instance Variables
     */
    private BufferedWriter bw;
    public String finalMessage = "";
    public String address = "";
    public String sendingUser = "";
    public Socket s;
    public HashMap<String,Socket> listOfUsers = new HashMap<String,Socket>();


    /**
     * launches javafx instance
     * @param stage the stage to which the elements will be drawn
     */
    @Override
    public void start(Stage stage) throws IOException {

        /**
         * Set up the GUI elements
         */
        VBox vbox = new VBox();

        TextArea messageIncoming = new TextArea();
        Button exitButton = new Button("Exit");
        GridPane grid = new GridPane();
        grid.add(messageIncoming,0,0);
        grid.add(exitButton,0,1);
        grid.setVgap(10);
        grid.setPadding(new Insets(30,30,30,30));
        vbox.getChildren().add(grid);


        /**
         * Handles the exit button's events
         */
        exitButton.setOnAction(actionEvent -> {
            Platform.exit();
            System.exit(0);
        });



        /**
         * Handles all socket manaagement including connecting clients to the server,
         * spawning new threads for each server, reading and delegating writing messages,
         * delegating disconnected users.
         */
        //Socket
        ServerSocket ss = new ServerSocket(63030);
        Thread thread = new Thread(() -> {
        //System.out.println("New Thread \"thread\"");
        //AtomicInteger count = new AtomicInteger();
            while (true) {


                try {
                    s = ss.accept();
                    BufferedReader br = new BufferedReader((new InputStreamReader(s.getInputStream())));

                    //Get Username
                    sendingUser = br.readLine();
                    listOfUsers.put(sendingUser, s);

                } catch (IOException e) {
                    e.printStackTrace();
                }


                Socket finalS = s;
                System.out.println("Client Connected: " + sendingUser + " - Address: " + s.getRemoteSocketAddress());

                Thread thread1 = new Thread(() -> {
                    //count.getAndIncrement();
                    //System.out.println("New Thread \"thread1\"" + count);
                    while (finalS.isConnected()) {

                        BufferedReader br = null;
                        try {
                            br = new BufferedReader((new InputStreamReader(finalS.getInputStream())));
                            bw = new BufferedWriter(new OutputStreamWriter(finalS.getOutputStream()));

                            String line;
                            int i = 0;
                            while ((i != 3)) {
                                line = br.readLine();
                                if (i == 0) {
                                    address = line;
                                } else if (i == 1) {
                                    sendingUser = line;
                                } else if (i == 2) {
                                    finalMessage = line;
                                }
                                i++;
                            }

                            System.out.println("Sending: " + finalMessage + " - From: " + sendingUser + " - To: " + address);
                            writeMessage(finalS, address, sendingUser);
                            messageIncoming.appendText("Sending: " + finalMessage + " - From: " + sendingUser + " - To: " + address + "\n");
                            address = "";
                            finalMessage = "";
                            sendingUser = "";

                        } catch (IOException e) {
                            removeClient(sendingUser);
//                            removeClient(finalS);
                            try {
                                finalS.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });
                thread1.start();
            }
            });
            thread.start();

        /**
        * Set Up Client GUI Window
        */
        Scene scene = new Scene(vbox, 338.75, 217.25);
        stage.setTitle("Server");
        stage.setScene(scene);
        stage.show();

        //Terminate program when closed via windows methods (eg. windows program bar close button)
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });
    }

    /**
     * Main method that launches the 'main' window
     */
    public static void main(String[] args) {
        launch();
    }

    /**
     * Method that handles the sending of a message to the desired user
     * * @param s is the socket of the user sending the message
     * * @param address is the username of the user to send to
     * * @param sendingUser is the username of the sender
     */
    public void writeMessage(Socket s, String address, String sendingUser) throws IOException {
        System.out.println("Sending to Address: " + address);
        System.out.println("Client Address: " + s.getRemoteSocketAddress());

        /**
         * Slicing out port from user code and sending to desired client
         */
        if (!listOfUsers.containsKey(address)) {
            System.out.println("Could not find user");
            return;
        }

        try {

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(listOfUsers.get(address).getOutputStream()));
            StringBuilder rawUsernameBuilder = new StringBuilder(sendingUser);
            String rawUsername = rawUsernameBuilder.substring(0, sendingUser.length() - 6);
            bw.write(rawUsername + ": " + finalMessage);
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method that is called when a socket connection is closed and the client is
     * * @param sendingUser is the username of the client that disconnected
     * The user is removed from the hash map containing all users
     */
    public void removeClient(String sendingUser) {
        listOfUsers.remove(sendingUser);
    }

}