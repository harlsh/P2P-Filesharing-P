package server;

import peer.peerProcess;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This method is used to handle the File server thread
 */
public class PeerServerHandler implements Runnable {
    private ServerSocket serverSocket;
    private String peerID;
    private Socket otherPeerSocket;
    private Thread otherPeerThread;

    public PeerServerHandler(ServerSocket serverSocket, String peerID) {
        this.serverSocket = serverSocket;
        this.peerID = peerID;
    }

    @Override
    public void run() {
        while(true) {
            try{
                otherPeerSocket = serverSocket.accept();
                otherPeerThread = new Thread(new PeerMessageHandler(otherPeerSocket, 0, peerID));
                peerProcess.serverThreads.add(otherPeerThread);
                otherPeerThread.start();
            }catch (IOException e) {

            }
        }
    }

}
