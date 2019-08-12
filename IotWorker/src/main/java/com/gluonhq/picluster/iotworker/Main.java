package com.gluonhq.picluster.iotworker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Random;

public class Main {

    static final String ID = "foo"; // this needs to be the mac address or so
    static final String SEP = ";";

    static final String SERVER_IP = "127.0.0.1";
    static final int SERVER_PORT = 39265;

    static final String DISPLAY_IP = "";
    static final int DISPLAY_PORT = -1;

    static boolean go = true;

    public static void main(String[] args) {
        System.err.println("Hi, I'm an IoT worker");
        try {
            runJobs();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println("SHOULD NOT REACH HERE, best to reboot!");
    }

    static void runJobs() throws IOException {
        while (go) {
            System.err.println("Create socket...");
            String msg = ID+SEP+"ask"+SEP+"\n";
            String job = sendSingleMessage(msg);


            int idx = job.indexOf(SEP);
            String taskId = job.substring(0, idx);
            String url = job.substring(idx+1);
            System.err.println("Need to process "+url);
            int answer = processURL(taskId, url);

            msg = ID+SEP+"answer"+SEP+taskId+SEP+answer+"\n";
            sendResultToDisplayApp(answer);
            sendSingleMessage(msg);

        }
    }

    static int processURL (String taskId, String url) {
        System.err.println("Processing image at "+url+" with taskId "+taskId);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int answer = (int)(Math.random()*100);
        return answer;
    }

    static String sendSingleMessage (String msg) throws IOException {
        Socket s = new Socket(SERVER_IP, SERVER_PORT);
        System.err.println("Socket created");
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

        System.err.println("Write message: "+msg);
        bw.write(msg);
        bw.flush();
        System.err.println("Message written");
        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
        System.err.println("Got BR");
        String job = br.readLine();
        br.close();
        bw.close();
        s.close();
        return job;
    }

    private static final int PORT = 6666;
    private static final int SIZE_X = 32;
    private static final int SIZE_Y = 32;
    private static final int SIZE = 1024;


    static void sendResultToDisplayApp(int answer) {
        try {
            Socket clientSocket = new Socket(DISPLAY_IP, DISPLAY_PORT);
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            int chunkId_X = new Random().nextInt(SIZE_X);
            int chunkId_Y = new Random().nextInt(SIZE_Y);
            double opacity = 100* answer;
            String msg = String.format("%d,%d,%s", chunkId_X, chunkId_Y,
                    String.format("%.2f", opacity).replace(",", "."));
            out.println(msg);
            String resp = null;
            try {
                resp = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ConnectException c) {
            System.out.println("Error connection: " + c);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}