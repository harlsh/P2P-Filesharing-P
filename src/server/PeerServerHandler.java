package server;

import peer.peerProcess;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This method is used to handle the File server thread
 */
public class PeerServerHandler implements Runnable {
    //The File server socket
    private ServerSocket serverSocket;
    //Current PeerID
    private String peerID;
    //Socket of remote peer
    private Socket otherPeerSocket;
    //Remote peer thread
    private Thread otherPeerThread;

    public PeerServerHandler(ServerSocket serverSocket, String peerID) {
        this.serverSocket = serverSocket;
        this.peerID = peerID;
    }

    @Override
    public void run() {
        while(true) {
            try{
                //accept incoming socket connections
                otherPeerSocket = serverSocket.accept();
                //start a thread to handle incoming messages
                otherPeerThread = new Thread(new PeerMessageHandler(otherPeerSocket, 0, peerID));
                peerProcess.serverThreads.add(otherPeerThread);
                otherPeerThread.start();
            }catch (IOException e) {

            }
        }
    }

}
