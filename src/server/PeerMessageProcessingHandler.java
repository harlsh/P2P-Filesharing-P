package server;

import config.CommonConfiguration;
import logging.LogHelper;
import message.*;
import peer.PeerProcessUtils;
import peer.RemotePeerDetails;
import peer.peerProcess;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Date;
import java.util.Set;

import static peer.peerProcess.messageQueue;

/**
 * This class is used to process messages from message queue.
 */
public class PeerMessageProcessingHandler implements Runnable {


    private static String currentPeerID;

    private RandomAccessFile randomAccessFile;

    public PeerMessageProcessingHandler(String peerID) {
        currentPeerID = peerID;
    }

    private static void logAndShowInConsole(String message) {
        LogHelper.logAndPrint(message);
    }

    private void sendDownloadCompleteMessage(Socket socket, String peerID) {
        logAndShowInConsole(currentPeerID + " sending a DOWNLOAD COMPLETE message to Peer " + peerID);
        Message message = new Message(Message.MessageConstants.MESSAGE_DOWNLOADED);
        byte[] messageInBytes = Message.convertMessageToByteArray(message);
        sendMessageToSocket(socket, messageInBytes);
    }

    private void sendHaveMessage(Socket socket, String peerID) {
        //logAndShowInConsole(peer.peerProcess.currentPeerID + " sending HAVE message to Peer " + peerID);
        byte[] bitFieldInBytes = peerProcess.bitFieldMessage.getBytes();
        Message message = new Message(Message.MessageConstants.MESSAGE_HAVE, bitFieldInBytes);
        sendMessageToSocket(socket, Message.convertMessageToByteArray(message));

    }


    private boolean hasPeerInterested(RemotePeerDetails remotePeerDetails) {
        return remotePeerDetails.getIsComplete() == 0 &&
                remotePeerDetails.getIsChoked() == 0 && remotePeerDetails.getIsInterested() == 1;
    }


    private int getFirstDifferentPieceIndex(String peerID) {
        return peerProcess.bitFieldMessage.getFirstDifferentPieceIndex(peerProcess.remotePeerDetailsMap.get(peerID).getBitFieldMessage());
    }

    private void sendRequestMessage(Socket socket, int pieceIndex, String remotePeerID) {
        logAndShowInConsole(peerProcess.currentPeerID + " sending REQUEST message to Peer " + remotePeerID + " for piece " + pieceIndex);
        int pieceIndexLength = Message.MessageConstants.PIECE_INDEX_LENGTH;
        byte[] pieceInBytes = new byte[pieceIndexLength];
        for (int i = 0; i < pieceIndexLength; i++) {
            pieceInBytes[i] = 0;
        }

        byte[] pieceIndexInBytes = PeerProcessUtils.convertIntToByteArray(pieceIndex);
        System.arraycopy(pieceIndexInBytes, 0, pieceInBytes, 0, pieceIndexInBytes.length);
        Message message = new Message(Message.MessageConstants.MESSAGE_REQUEST, pieceIndexInBytes);
        sendMessageToSocket(socket, Message.convertMessageToByteArray(message));

    }


    private void sendFilePiece(Socket socket, Message message, String remotePeerID) {
        byte[] pieceIndexInBytes = message.getPayload();
        int pieceIndex = PeerProcessUtils.convertByteArrayToInt(pieceIndexInBytes);
        int pieceSize = CommonConfiguration.pieceSize;
        logAndShowInConsole(currentPeerID + " sending a PIECE message for piece " + pieceIndex + " to peer " + remotePeerID);

        byte[] bytesRead = new byte[pieceSize];
        int numberOfBytesRead = 0;
        File file = new File(currentPeerID, CommonConfiguration.fileName);
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek(pieceIndex * pieceSize);
            numberOfBytesRead = randomAccessFile.read(bytesRead, 0, pieceSize);

            byte[] buffer = new byte[numberOfBytesRead + Message.MessageConstants.PIECE_INDEX_LENGTH];
            System.arraycopy(pieceIndexInBytes, 0, buffer, 0, Message.MessageConstants.PIECE_INDEX_LENGTH);
            System.arraycopy(bytesRead, 0, buffer, Message.MessageConstants.PIECE_INDEX_LENGTH, numberOfBytesRead);

            Message messageToBeSent = new Message(Message.MessageConstants.MESSAGE_PIECE, buffer);
            sendMessageToSocket(socket, Message.convertMessageToByteArray(messageToBeSent));
            randomAccessFile.close();

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private boolean isNotPreferredAndUnchokedNeighbour(String remotePeerId) {
        return !peerProcess.preferredNeighboursMap.containsKey(remotePeerId) && !peerProcess.optimisticUnchokedNeighbors.containsKey(remotePeerId);
    }

    private void sendChokedMessage(Socket socket, String remotePeerID) {
        logAndShowInConsole(currentPeerID + " sending a CHOKE message to Peer " + remotePeerID);
        Message message = new Message(Message.MessageConstants.MESSAGE_CHOKE);
        byte[] messageInBytes = Message.convertMessageToByteArray(message);
        sendMessageToSocket(socket, messageInBytes);
    }

    private void sendUnChokedMessage(Socket socket, String remotePeerID) {
        logAndShowInConsole(currentPeerID + " sending a UNCHOKE message to Peer " + remotePeerID);
        Message message = new Message(Message.MessageConstants.MESSAGE_UNCHOKE);
        byte[] messageInBytes = Message.convertMessageToByteArray(message);
        sendMessageToSocket(socket, messageInBytes);
    }

    private void sendNotInterestedMessage(Socket socket, String remotePeerID) {
        logAndShowInConsole(currentPeerID + " sending a NOT INTERESTED message to Peer " + remotePeerID);
        Message message = new Message(Message.MessageConstants.MESSAGE_NOT_INTERESTED);
        byte[] messageInBytes = Message.convertMessageToByteArray(message);
        sendMessageToSocket(socket, messageInBytes);
    }

    private void sendInterestedMessage(Socket socket, String remotePeerID) {
        logAndShowInConsole(currentPeerID + " sending an INTERESTED message to Peer " + remotePeerID);
        Message message = new Message(Message.MessageConstants.MESSAGE_INTERESTED);
        byte[] messageInBytes = Message.convertMessageToByteArray(message);
        sendMessageToSocket(socket, messageInBytes);
    }

    private void sendBitFieldMessage(Socket socket, String remotePeerID) {
        logAndShowInConsole(currentPeerID + " sending a BITFIELD message to Peer " + remotePeerID);
        byte[] bitFieldMessageInByteArray = peerProcess.bitFieldMessage.getBytes();
        Message message = new Message(Message.MessageConstants.MESSAGE_BITFIELD, bitFieldMessageInByteArray);
        byte[] messageInBytes = Message.convertMessageToByteArray(message);
        sendMessageToSocket(socket, messageInBytes);

    }

    private boolean isPeerInterested(Message message, String remotePeerID) {
        boolean peerInterested = false;
        BitFieldMessage bitField = BitFieldMessage.decodeMessage(message.getPayload());
        peerProcess.remotePeerDetailsMap.get(remotePeerID).setBitFieldMessage(bitField);
        int pieceIndex = peerProcess.bitFieldMessage.getInterestingPieceIndex(bitField);
        if (pieceIndex != -1) {
            if (message.getType().equals(Message.MessageConstants.MESSAGE_HAVE))
                logAndShowInConsole(currentPeerID + " received HAVE message from Peer " + remotePeerID + " for piece " + pieceIndex);
            peerInterested = true;
        }

        return peerInterested;
    }

    private void sendMessageToSocket(Socket socket, byte[] messageInBytes) {
        try {
            OutputStream out = socket.getOutputStream();
            out.write(messageInBytes);
        } catch (IOException e) {
        }
    }

    @Override
    public void run() {
        MessageDetails messageDetails;
        Message message;
        String messageType;
        String remotePeerID;

        while (true) {
            //Read message from queue
            messageDetails = messageQueue.poll();
            while (messageDetails == null) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                messageDetails = messageQueue.poll();
            }
            message = messageDetails.getMessage();
            messageType = message.getType();
            remotePeerID = messageDetails.getFromPeerID();
            int peerState = peerProcess.remotePeerDetailsMap.get(remotePeerID).getPeerState();

            if (messageType.equals(Message.MessageConstants.MESSAGE_HAVE) && peerState != 14) {
                //Received a interesting pieces message
                logAndShowInConsole(currentPeerID + " contains interesting pieces from Peer " + remotePeerID);
                if (isPeerInterested(message, remotePeerID)) {
                    sendInterestedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                    peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(9);
                } else {
                    sendNotInterestedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                    peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(13);
                }
            } else {
                if (peerState == 2) {
                    if (messageType.equals(Message.MessageConstants.MESSAGE_BITFIELD)) {
                        //Received bitfield message
                        logAndShowInConsole(currentPeerID + " received a BITFIELD message from Peer " + remotePeerID);
                        sendBitFieldMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                        peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(3);
                    }
                } else if (peerState == 3) {
                    if (messageType.equals(Message.MessageConstants.MESSAGE_INTERESTED)) {
                        //Received interested message
                        logAndShowInConsole(currentPeerID + " receieved an INTERESTED message from Peer " + remotePeerID);
                        peerProcess.remotePeerDetailsMap.get(remotePeerID).setIsInterested(1);
                        peerProcess.remotePeerDetailsMap.get(remotePeerID).setIsHandShaked(1);
                        //check if the neighbor is in unchoked neighbors or optimistically unchoked neighbors list
                        if (isNotPreferredAndUnchokedNeighbour(remotePeerID)) {
                            sendChokedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setIsChoked(1);
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(6);
                        } else {
                            sendUnChokedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setIsChoked(0);
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(4);
                        }
                    } else if (messageType.equals(Message.MessageConstants.MESSAGE_NOT_INTERESTED)) {
                        //Received not interested message
                        logAndShowInConsole(currentPeerID + " receieved an NOT INTERESTED message from Peer " + remotePeerID);
                        peerProcess.remotePeerDetailsMap.get(remotePeerID).setIsInterested(0);
                        peerProcess.remotePeerDetailsMap.get(remotePeerID).setIsHandShaked(1);
                        peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(5);
                    }
                } else if (peerState == 4) {
                    if (messageType.equals(Message.MessageConstants.MESSAGE_REQUEST)) {
                        //Received request message
                        //send file piece to the requestor
                        sendFilePiece(peerProcess.peerToSocketMap.get(remotePeerID), message, remotePeerID);

                        Set<String> remotePeerDetailsKeys = peerProcess.remotePeerDetailsMap.keySet();
                        if (!peerProcess.isFirstPeer && peerProcess.bitFieldMessage.isFileDownloadComplete()) {
                            for (String key : remotePeerDetailsKeys) {
                                if (!key.equals(peerProcess.currentPeerID)) {
                                    Socket socket = peerProcess.peerToSocketMap.get(key);
                                    if (socket != null) {
                                        sendDownloadCompleteMessage(socket, key);
                                    }
                                }
                            }
                        }
                        if (isNotPreferredAndUnchokedNeighbour(remotePeerID)) {
                            //sending choked message if the neighbor is not in unchoked neighbors or optimistically unchoked neighbors list
                            sendChokedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setIsChoked(1);
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(6);
                        }
                    }
                } else if (peerState == 8) {
                    if (messageType.equals(Message.MessageConstants.MESSAGE_BITFIELD)) {
                        //Received bifield message
                        if (isPeerInterested(message, remotePeerID)) {
                            sendInterestedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(9);
                        } else {
                            sendNotInterestedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(13);
                        }
                    }
                } else if (peerState == 9) {
                    if (messageType.equals(Message.MessageConstants.MESSAGE_CHOKE)) {
                        //Received choke message
                        logAndShowInConsole(currentPeerID + " is CHOKED by Peer " + remotePeerID);
                        peerProcess.remotePeerDetailsMap.get(remotePeerID).setIsChoked(1);
                        peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(14);
                    } else if (messageType.equals(Message.MessageConstants.MESSAGE_UNCHOKE)) {
                        //Received unchoke message
                        logAndShowInConsole(currentPeerID + " is UNCHOKED by Peer " + remotePeerID);
                        //get the piece index which is present in remote peer but not in current peer and send a request message
                        int firstDifferentPieceIndex = getFirstDifferentPieceIndex(remotePeerID);
                        if (firstDifferentPieceIndex == -1) {
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(13);
                        } else {
                            sendRequestMessage(peerProcess.peerToSocketMap.get(remotePeerID), firstDifferentPieceIndex, remotePeerID);
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(11);
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setStartTime(new Date());
                        }
                    }
                } else if (peerState == 11) {
                    if (messageType.equals(Message.MessageConstants.MESSAGE_CHOKE)) {
                        //Received choke message
                        logAndShowInConsole(currentPeerID + " is CHOKED by Peer " + remotePeerID);
                        peerProcess.remotePeerDetailsMap.get(remotePeerID).setIsChoked(1);
                        peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(14);
                    } else if (messageType.equals(Message.MessageConstants.MESSAGE_PIECE)) {
                        //Received piece message
                        byte[] payloadInBytes = message.getPayload();
                        //compute data downloading rate of the peer
                        peerProcess.remotePeerDetailsMap.get(remotePeerID).setEndTime(new Date());
                        long totalTime = peerProcess.remotePeerDetailsMap.get(remotePeerID).getEndTime().getTime()
                                - peerProcess.remotePeerDetailsMap.get(remotePeerID).getStartTime().getTime();
                        double dataRate = ((double) (payloadInBytes.length + Message.MessageConstants.MESSAGE_LENGTH + Message.MessageConstants.MESSAGE_TYPE) / (double) totalTime) * 100;
                        peerProcess.remotePeerDetailsMap.get(remotePeerID).setDataRate(dataRate);
                        FilePiece filePiece = FilePieceDelegate.convertByteArrayToFilePiece(payloadInBytes);
                        //update the piece information in current peer bitfield
                        peerProcess.bitFieldMessage.updateBitFieldInformation(remotePeerID, filePiece);
                        int firstDifferentPieceIndex = getFirstDifferentPieceIndex(remotePeerID);
                        if (firstDifferentPieceIndex == -1) {
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(13);
                        } else {
                            sendRequestMessage(peerProcess.peerToSocketMap.get(remotePeerID), firstDifferentPieceIndex, remotePeerID);
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(11);
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setStartTime(new Date());
                        }

                        peerProcess.updateOtherPeerDetails();
                        Set<String> remotePeerDetailsKeys = peerProcess.remotePeerDetailsMap.keySet();
                        for (String key : remotePeerDetailsKeys) {
                            RemotePeerDetails peerDetails = peerProcess.remotePeerDetailsMap.get(key);
                            //send have message to peer if its interested
                            if (!key.equals(peerProcess.currentPeerID) && hasPeerInterested(peerDetails)) {
                                sendHaveMessage(peerProcess.peerToSocketMap.get(key), key);
                                peerProcess.remotePeerDetailsMap.get(key).setPeerState(3);
                            }
                        }

                        payloadInBytes = null;
                        message = null;
                        if (!peerProcess.isFirstPeer && peerProcess.bitFieldMessage.isFileDownloadComplete()) {
                            for (String key : remotePeerDetailsKeys) {
                                RemotePeerDetails peerDetails = peerProcess.remotePeerDetailsMap.get(key);
                                if (!key.equals(peerProcess.currentPeerID)) {
                                    Socket socket = peerProcess.peerToSocketMap.get(key);
                                    if (socket != null) {
                                        sendDownloadCompleteMessage(socket, key);
                                    }
                                }
                            }
                        }
                    }
                } else if (peerState == 14) {
                    if (messageType.equals(Message.MessageConstants.MESSAGE_HAVE)) {
                        //Received contains interesting pieces
                        logAndShowInConsole(currentPeerID + " contains interesting pieces from Peer " + remotePeerID);
                        if (isPeerInterested(message, remotePeerID)) {
                            sendInterestedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(9);
                        } else {
                            sendNotInterestedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                            peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(13);
                        }
                    } else if (messageType.equals(Message.MessageConstants.MESSAGE_UNCHOKE)) {
                        //Received unchoked message
                        logAndShowInConsole(currentPeerID + " is UNCHOKED by Peer " + remotePeerID);
                        peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(14);
                    }
                } else if (peerState == 15) {
                    try {
                        //update neighbor details after it gets file completely
                        peerProcess.remotePeerDetailsMap.get(peerProcess.currentPeerID).updatePeerDetails(remotePeerID, 1);
                        logAndShowInConsole(remotePeerID + " has downloaded the complete file");
                        int previousState = peerProcess.remotePeerDetailsMap.get(remotePeerID).getPreviousPeerState();
                        peerProcess.remotePeerDetailsMap.get(remotePeerID).setPeerState(previousState);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
