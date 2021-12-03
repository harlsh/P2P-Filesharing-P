package peer;

import config.CommonConfiguration;
import logging.LogHelper;
import message.BitFieldMessage;
import message.MessageInfo;
import server.PeerMessageHandler;
import server.PeerMessageProcessingHandler;
import server.PeerServerHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class is used to implement the P2P process to transfer file from peer to peer.
 */
@SuppressWarnings({"deprecation", "unchecked"})
public class peerProcess {
    public Thread serverThread;
    public static boolean isFirstPeer;
    public static String currentPeerID;
    public static int peerIndex;
    public static BitFieldMessage bitFieldMessage;
    public static int currentPeerPort;
    public static int currentPeerHasFile;
    public static boolean isDownloadComplete;
    public static Thread messageProcessor;
    public static volatile ConcurrentLinkedQueue<MessageInfo> messageQueue = new ConcurrentLinkedQueue<>();
    public static Vector<Thread> peerThreads = new Vector();
    public static Vector<Thread> serverThreads = new Vector();
    public static volatile Timer timerPreferredNeighbors;
    public static volatile Timer timerOptimisticUnchokedNeighbors;
    public static volatile ConcurrentHashMap<String, RemotePeerDetails> remotePeerDetailsMap = new ConcurrentHashMap();
    public static volatile ConcurrentHashMap<String, RemotePeerDetails> preferredNeighboursMap = new ConcurrentHashMap();
    public static volatile ConcurrentHashMap<String, Socket> peerToSocketMap = new ConcurrentHashMap();
    public static volatile ConcurrentHashMap<String, RemotePeerDetails> optimisticUnchokedNeighbors = new ConcurrentHashMap();
    public ServerSocket serverSocket;
    /**
     * This method is used to get server thread
     * @return server thread
     */
    public Thread getServerThread() {
        return serverThread;
    }

    /**
     * This method is used to set server thread
     * @param serverThread - server thread
     */
    public void setServerThread(Thread serverThread) {
        this.serverThread = serverThread;
    }

    /**
     * Main method to run p2p file transfer. This method takes processID as input,
     * reads Common.cfg and PeerInfo.cfg and runs peerprocess to transfer files between peers
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    public static void main(String[] args) {
        peerProcess process = new peerProcess();
        currentPeerID = args[0];

        try {
            LogHelper logHelper = new LogHelper();
            logHelper.initializeLogger(currentPeerID);
            logAndPrint(currentPeerID + " is started");

            initializeConfiguration();
            setCurrentPeerDetails();
            initializeBitFieldMessage();

            startMessageProcessingThread(process);
            startFileServerReceiverThreads(process);

            determinePreferredNeighbors(); // ryan
            determineOptimisticallyUnchockedNeighbours(); // ryan

            terminatePeer(process);

        } catch (Exception e) {
        } finally {
            logAndPrint(currentPeerID + " Peer process is exiting..");
            System.exit(0);
        }
    }

    private static void terminatePeer(peerProcess process) {
        while (true) {
            isDownloadComplete = hasDownloadCompleted();
            if (isDownloadComplete) {
                logAndPrint("All peers have completed downloading the file.");
                timerPreferredNeighbors.cancel();
                timerOptimisticUnchokedNeighbors.cancel();

                try {
                    Thread.currentThread();
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }

                if (process.getServerThread().isAlive()) {
                    process.getServerThread().stop();
                }

                if (messageProcessor.isAlive()) {
                    messageProcessor.stop();
                }

                for (Thread thread : peerThreads) {
                    if (thread.isAlive()) {
                        thread.stop();
                    }
                }

                for (Thread thread : serverThreads) {
                    if (thread.isAlive()) {
                        thread.stop();
                    }
                }

                break;

            } else {
                try {
                    Thread.currentThread();
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public static void initializeBitFieldMessage() {
        bitFieldMessage = new BitFieldMessage();
        bitFieldMessage.setPieceDetails(currentPeerID, currentPeerHasFile);
    }

    public static void startFileServerReceiverThreads(peerProcess process) {
        if (isFirstPeer) {
            startFileServerThread(process);
        } else {
            createNewFile();
            startFileReceiverThreads(process);
            startFileServerThread(process);
        }
    }

    public static void startFileReceiverThreads(peerProcess process) {
        Set<String> remotePeerDetailsKeys = remotePeerDetailsMap.keySet();
        for (String peerID : remotePeerDetailsKeys) {
            RemotePeerDetails remotePeerDetails = remotePeerDetailsMap.get(peerID);

            if (process.peerIndex > remotePeerDetails.getIndex()) {
                Thread tempThread = new Thread(new PeerMessageHandler(
                        remotePeerDetails.getHostAddress(), Integer
                        .parseInt(remotePeerDetails.getPort()), 1,
                        currentPeerID));
                peerThreads.add(tempThread);
                tempThread.start();
            }
        }
    }

    public static void startFileServerThread(peerProcess process) {
        try {
            //Start a new file server thread
            process.serverSocket = new ServerSocket(currentPeerPort);
            process.serverThread = new Thread(new PeerServerHandler(process.serverSocket, currentPeerID));
            process.serverThread.start();
        } catch (SocketTimeoutException e) {
            logAndPrint(currentPeerID + " Socket Gets Timed out Error - " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static void setCurrentPeerDetails() {
        final RemotePeerDetails remotePeerDetails = remotePeerDetailsMap.get(currentPeerID);
        currentPeerPort = Integer.parseInt(remotePeerDetails.getPort());
        peerIndex = remotePeerDetails.getIndex();
        if (remotePeerDetails.getHasFile() == 1) {
            isFirstPeer = true;
            currentPeerHasFile = remotePeerDetails.getHasFile();
        }

    }

    public static void initializeConfiguration() throws Exception {
        initializePeerConfiguration();
        addOtherPeerDetails();
        setPreferredNeighbours();

    }

    /**
     * This method creates a timer task to determine preferred neighbors
     */
    public static void determinePreferredNeighbors() {
        timerPreferredNeighbors = new Timer();
        timerPreferredNeighbors.schedule(new PrefNeighbors(),
                CommonConfiguration.unchockingInterval * 1000 * 0,
                CommonConfiguration.unchockingInterval * 1000);
    }

    /**
     * This method creates a timer task to determine optimistically unchoked neighbors
     */
    public static void determineOptimisticallyUnchockedNeighbours() {
        timerOptimisticUnchokedNeighbors = new Timer();
        timerOptimisticUnchokedNeighbors.schedule(new OptimisticallyUnchockedNeighbors(),
                CommonConfiguration.optimisticUnchokingInterval * 1000 * 0,
                CommonConfiguration.optimisticUnchokingInterval * 1000
        );
    }

    /**
     * This method is used to start message processing thread
     * @param process - peerprrocess to start thread into
     */
    public static void startMessageProcessingThread(peerProcess process) {
        messageProcessor = new Thread(new PeerMessageProcessingHandler(currentPeerID));
        messageProcessor.start();
    }

    /**
     * This method is used to create empty file with size 'CommonConfiguration.fileSize' and set zero bits into it
     */
    public static void createNewFile() {
        try {
            File dir = new File(currentPeerID);
            dir.mkdir();

            File newfile = new File(currentPeerID, CommonConfiguration.fileName);
            OutputStream os = new FileOutputStream(newfile, true);
            byte b = 0;

            for (int i = 0; i < CommonConfiguration.fileSize; i++)
                os.write(b);
            os.close();
        } catch (Exception e) {
            logAndPrint(currentPeerID + " ERROR in creating the file : " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * This method is used to set preferred neighbors of a peer
     */
    public static void setPreferredNeighbours() {
        Set<String> remotePeerIDs = remotePeerDetailsMap.keySet();
        for (String peerID : remotePeerIDs) {
            RemotePeerDetails remotePeerDetails = remotePeerDetailsMap.get(peerID);
            if (remotePeerDetails != null && !peerID.equals(currentPeerID)) {
                preferredNeighboursMap.put(peerID, remotePeerDetails);
            }
        }
    }

    /**
     * This method reads PeerInfo.cfg file and adds peers to remotePeerDetailsMap
     */
    public static void addOtherPeerDetails() throws IOException {
        try {
            List<String> lines = Files.readAllLines(Paths.get("PeerInfo.cfg"));
            for (int i = 0; i < lines.size(); i++) {
                String[] properties = lines.get(i).split("\\s+");
                remotePeerDetailsMap.put(properties[0],
                        new RemotePeerDetails(properties[0], properties[1], properties[2], Integer.parseInt(properties[3]), i));
            }
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * This method is used to check if all the peers have downloaded the file
     * @return true - all peers downloaded the file; false - all peers did not download the file
     */
    public static synchronized boolean hasDownloadCompleted() {
        boolean isDownloadCompleted = true;
        try {
            List<String> lines = Files.readAllLines(Paths.get("PeerInfo.cfg"));
            for (int i = 0; i < lines.size(); i++) {
                String[] properties = lines.get(i).split("\\s+");
                if (Integer.parseInt(properties[3]) == 0) {
                    isDownloadCompleted = false;
                    break;
                }
            }
        } catch (IOException e) {
            isDownloadCompleted = false;
        }

        return isDownloadCompleted;
    }

    /**
     * This method reads Common.cfg and initializes the properties in CommonConfiguration class
     * @throws IOException
     */
    public static void initializePeerConfiguration() throws IOException {
        try {
            List<String> lines = Files.readAllLines(Paths.get("Common.cfg"));
            for (String line : lines) {
                String[] properties = line.split("\\s+");
                if (properties[0].equalsIgnoreCase("NumberOfPreferredNeighbors")) {
                    CommonConfiguration.numberOfPreferredNeighbours = Integer.parseInt(properties[1]);
                } else if (properties[0].equalsIgnoreCase("UnchokingInterval")) {
                    CommonConfiguration.unchockingInterval = Integer.parseInt(properties[1]);
                } else if (properties[0].equalsIgnoreCase("OptimisticUnchokingInterval")) {
                    CommonConfiguration.optimisticUnchokingInterval = Integer.parseInt(properties[1]);
                } else if (properties[0].equalsIgnoreCase("FileName")) {
                    CommonConfiguration.fileName = properties[1];
                } else if (properties[0].equalsIgnoreCase("FileSize")) {
                    CommonConfiguration.fileSize = Integer.parseInt(properties[1]);
                } else if (properties[0].equalsIgnoreCase("PieceSize")) {
                    CommonConfiguration.pieceSize = Integer.parseInt(properties[1]);
                }
            }
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * This method is used to log a message in a log file and show it in console
     * @param message - message to be logged and showed in console
     */
    private static void logAndPrint(String message) {
        LogHelper.logAndPrint(message);
    }

    /**
     * This method reads PeerInfo.cfg file and updates peers in remotePeerDetailsMap
     */
    public static void updateOtherPeerDetails() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("PeerInfo.cfg"));
            for (int i = 0; i < lines.size(); i++) {
                String[] properties = lines.get(i).split("\\s+");
                String peerID = properties[0];
                int isCompleted = Integer.parseInt(properties[3]);
                if (isCompleted == 1) {
                    remotePeerDetailsMap.get(peerID).setIsComplete(1);
                    remotePeerDetailsMap.get(peerID).setIsInterested(0);
                    remotePeerDetailsMap.get(peerID).setIsChoked(0);
                }
            }
        } catch (IOException e) {
        }
    }
}
