package server;

import peer.peerProcess;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class ServerHandler implements Runnable {
    private ServerSocket serverSocket;
    private String peerID;
    private Socket otherPeerSocket;
    private Thread otherPeerThread;

    public ServerHandler(ServerSocket serverSocket, String peerID) {
        this.serverSocket = serverSocket;
        this.peerID = peerID;
    }

    @Override
    public void run() {
        while (true) {
            try {
                otherPeerSocket = serverSocket.accept();
                otherPeerThread = new Thread(new MessageHandler(otherPeerSocket, 0, peerID));
                peerProcess.serverThreads.add(otherPeerThread);
                otherPeerThread.start();
            } catch (IOException e) {

            }
        }
    }

}
