package peer;

import config.CommonConfiguration;
import logging.LogHelper;
import message.BitFieldMessage;
import message.MessageInfo;
import server.MessageHandler;
import server.MessageProcessingHandler;
import server.ServerHandler;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static logging.LogHelper.logAndPrint;


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
    public static volatile ConcurrentHashMap<String, RemotePeerInfo> remotePeerDetailsMap = new ConcurrentHashMap();
    public static volatile ConcurrentHashMap<String, RemotePeerInfo> preferredNeighboursMap = new ConcurrentHashMap();
    public static volatile ConcurrentHashMap<String, Socket> peerToSocketMap = new ConcurrentHashMap();
    public static volatile ConcurrentHashMap<String, RemotePeerInfo> optimisticUnchokedNeighbors = new ConcurrentHashMap();
    public ServerSocket serverSocket;

    public Thread getServerThread() {
        return serverThread;
    }


    @SuppressWarnings({"deprecation", "unchecked"})
    public static void main(String[] args) throws Exception {
        peerProcess process = new peerProcess();
        currentPeerID = args[0];

        try {
            LogHelper logHelper = new LogHelper();
            logHelper.initializeLogger(currentPeerID);
            logAndPrint("Started listening...");

            initializeConfiguration();
            setCurrentPeerDetails();
            initializeBitFieldMessage();

            startMessageProcessingThread();
            startFileServerReceiverThreads(process);

            determinePreferredNeighbors();
            determineOptimisticallyUnchockedNeighbours();

            terminatePeer(process);

        } catch (Exception e) {
            logAndPrint(e.toString());
            System.out.println(e);
            throw e;
        } finally {
            logAndPrint("Peer process is exiting..");
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
            RemotePeerInfo remotePeerInfo = remotePeerDetailsMap.get(peerID);

            if (peerIndex > remotePeerInfo.getIndex()) {
                Thread tempThread = new Thread(new MessageHandler(
                        remotePeerInfo.getHostAddress(), Integer
                        .parseInt(remotePeerInfo.getPort()), 1,
                        currentPeerID));
                peerThreads.add(tempThread);
                tempThread.start();
            }
        }
    }

    public static void startFileServerThread(peerProcess process) {
        try {
            process.serverSocket = new ServerSocket(currentPeerPort);
            process.serverThread = new Thread(new ServerHandler(process.serverSocket, currentPeerID));
            process.serverThread.start();
        } catch (SocketTimeoutException e) {
            logAndPrint("Socket Gets Timed out Error - " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static void setCurrentPeerDetails() {
        final RemotePeerInfo remotePeerInfo = remotePeerDetailsMap.get(currentPeerID);
        currentPeerPort = Integer.parseInt(remotePeerInfo.getPort());
        peerIndex = remotePeerInfo.getIndex();
        if (remotePeerInfo.getHasFile() == 1) {
            isFirstPeer = true;
            currentPeerHasFile = remotePeerInfo.getHasFile();
        }

    }

    public static void initializeConfiguration() throws Exception {
        readCommonConfigFile();
        readPeerInfoFile();
        setPreferredNeighbours();
    }


    public static void determinePreferredNeighbors() {
        timerPreferredNeighbors = new Timer();
        timerPreferredNeighbors.schedule(new PreferredNeighbors(),
                0,
                CommonConfiguration.unchockingInterval * 1000);
    }


    public static void determineOptimisticallyUnchockedNeighbours() {
        timerOptimisticUnchokedNeighbors = new Timer();
        timerOptimisticUnchokedNeighbors.schedule(new OptimisticallyUnchokedNeighbors(),
                0,
                CommonConfiguration.optimisticUnchokingInterval * 1000
        );
    }

    public static void startMessageProcessingThread() {
        messageProcessor = new Thread(new MessageProcessingHandler(currentPeerID));
        messageProcessor.start();
    }

    public static void createNewFile() {

        try {
            File dir = new File(currentPeerID);
            dir.mkdir();
            RandomAccessFile f = new RandomAccessFile(CommonConfiguration.fileName, "rw");
            f.setLength(CommonConfiguration.fileSize);
        } catch (Exception e) {

        }

    }

    public static void setPreferredNeighbours() {
        for (String peerID : remotePeerDetailsMap.keySet()) {
            RemotePeerInfo remotePeerInfo = remotePeerDetailsMap.get(peerID);
            if (!peerID.equals(currentPeerID)) {
                preferredNeighboursMap.put(peerID, remotePeerInfo);
            }
        }
        logAndPrint("NeighborsMap: " + remotePeerDetailsMap);
    }

    public static void readPeerInfoFile() throws IOException {
        try {
            List<String> lines = Files.readAllLines(Paths.get("PeerInfo.cfg"));
            for (int i = 0; i < lines.size(); i++) {
                String[] properties = lines.get(i).split("\\s+");
                remotePeerDetailsMap.put(properties[0],
                        new RemotePeerInfo(properties[0], properties[1], properties[2], Integer.parseInt(properties[3]), i));
            }
        } catch (IOException e) {
            throw e;
        }
    }

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

    public static void readCommonConfigFile() throws IOException {
        try {

            System.out.println("Reading data from Common.cfg");
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

    public static class PeerProcessUtils {

        public static byte[] convertIntToByteArray(int value) {
            return ByteBuffer.allocate(4).putInt(value).array();
        }

        public static int convertByteArrayToInt(byte[] dataInBytes) {
            return ByteBuffer.wrap(dataInBytes).getInt();
        }
    }
}
