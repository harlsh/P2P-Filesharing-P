package server;

import logging.LogHelper;
import message.HandshakeMessage;
import message.Message;
import message.MessageDetails;
import peer.peerProcess;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static peer.peerProcess.messageQueue;

/**
 * This class is used to write/read messages from socket
 */
public class PeerMessageHandler implements Runnable {
    private int connType;
    String ownPeerId;
    String remotePeerId;
    private InputStream socketInputStream;
    private OutputStream socketOutputStream;
    private HandshakeMessage handshakeMessage;
    private Socket peerSocket;

    public PeerMessageHandler(String address, int port, int connectionType, String serverPeerID) {
        try {
            connType = connectionType;
            ownPeerId = serverPeerID;
            peerSocket = new Socket(address, port);
            socketInputStream = peerSocket.getInputStream();
            socketOutputStream = peerSocket.getOutputStream();


        } catch (IOException e) {
        }
    }

    public PeerMessageHandler(Socket socket, int connectionType, String serverPeerID) {
        try {
            peerSocket = socket;
            connType = connectionType;
            ownPeerId = serverPeerID;
            socketInputStream = peerSocket.getInputStream();
            socketOutputStream = peerSocket.getOutputStream();
        } catch (IOException e) {

        }
    }

    private static void logAndPrint(String message) {
        LogHelper.logAndPrint(message);
    }

    @Override
    public void run() {
        byte[] handShakeMessageInBytes = new byte[32];
        byte[] dataBufferWithoutPayload = new byte[Message.MessageConstants.MESSAGE_LENGTH + Message.MessageConstants.MESSAGE_TYPE];
        byte[] messageLengthInBytes;
        byte[] messageTypeInBytes;
        MessageDetails messageDetails = new MessageDetails();
        try {
            if (connType == Message.MessageConstants.ACTIVE_CONNECTION) {

                if (handShakeMessageSent()) {
                    logAndPrint(ownPeerId + " HANDSHAKE has been sent");
                } else {
                    logAndPrint(ownPeerId + " HANDSHAKE sending failed");
                    System.exit(0);
                }

                while (true) {
                    socketInputStream.read(handShakeMessageInBytes);
                    handshakeMessage = HandshakeMessage.convertBytesToHandshakeMessage(handShakeMessageInBytes);
                    if (handshakeMessage.getHeader().equals(Message.MessageConstants.HANDSHAKE_HEADER)) {
                        remotePeerId = handshakeMessage.getPeerID();
                        logAndPrint(ownPeerId + " makes a connection to Peer " + remotePeerId);
                        logAndPrint(ownPeerId + " Received a HANDSHAKE message from Peer " + remotePeerId);
                        peerProcess.peerToSocketMap.put(remotePeerId, this.peerSocket);
                        break;
                    }
                }

                Message d = new Message(Message.MessageConstants.MESSAGE_BITFIELD, peerProcess.bitFieldMessage.getBytes());
                byte[] b = Message.convertMessageToByteArray(d);
                socketOutputStream.write(b);
                peerProcess.remotePeerDetailsMap.get(remotePeerId).setPeerState(8);
            }

            else {
                while (true) {
                    socketInputStream.read(handShakeMessageInBytes);
                    handshakeMessage = HandshakeMessage.convertBytesToHandshakeMessage(handShakeMessageInBytes);
                    if (handshakeMessage.getHeader().equals(Message.MessageConstants.HANDSHAKE_HEADER)) {
                        remotePeerId = handshakeMessage.getPeerID();
                        logAndPrint(ownPeerId + " is connected from Peer " + remotePeerId);
                        logAndPrint(ownPeerId + " Received a HANDSHAKE message from Peer " + remotePeerId);

                        peerProcess.peerToSocketMap.put(remotePeerId, this.peerSocket);
                        break;
                    } else {
                        continue;
                    }
                }
                if (handShakeMessageSent()) {
                    logAndPrint(ownPeerId + " HANDSHAKE message has been sent successfully.");

                } else {
                    logAndPrint(ownPeerId + " HANDSHAKE message sending failed.");
                    System.exit(0);
                }

                peerProcess.remotePeerDetailsMap.get(remotePeerId).setPeerState(2);
            }

            while (true) {
                int headerBytes = socketInputStream.read(dataBufferWithoutPayload);
                if (headerBytes == -1)
                    break;
                messageLengthInBytes = new byte[Message.MessageConstants.MESSAGE_LENGTH];
                messageTypeInBytes = new byte[Message.MessageConstants.MESSAGE_TYPE];
                System.arraycopy(dataBufferWithoutPayload, 0, messageLengthInBytes, 0, Message.MessageConstants.MESSAGE_LENGTH);
                System.arraycopy(dataBufferWithoutPayload, Message.MessageConstants.MESSAGE_LENGTH, messageTypeInBytes, 0, Message.MessageConstants.MESSAGE_TYPE);
                Message message = new Message();
                message.setMessageLength(messageLengthInBytes);
                message.setMessageType(messageTypeInBytes);
                String messageType = message.getType();
                if (messageType.equals(Message.MessageConstants.MESSAGE_INTERESTED) || messageType.equals(Message.MessageConstants.MESSAGE_NOT_INTERESTED) ||
                        messageType.equals(Message.MessageConstants.MESSAGE_CHOKE) || messageType.equals(Message.MessageConstants.MESSAGE_UNCHOKE)) {
                    messageDetails.setMessage(message);
                    messageDetails.setFromPeerID(remotePeerId);
                    messageQueue.add(messageDetails);
                } else if (messageType.equals(Message.MessageConstants.MESSAGE_DOWNLOADED)) {
                    messageDetails.setMessage(message);
                    messageDetails.setFromPeerID(remotePeerId);
                    int peerState = peerProcess.remotePeerDetailsMap.get(remotePeerId).getPeerState();
                    peerProcess.remotePeerDetailsMap.get(remotePeerId).setPreviousPeerState(peerState);
                    peerProcess.remotePeerDetailsMap.get(remotePeerId).setPeerState(15);
                    messageQueue.add(messageDetails);
                } else {
                    int bytesAlreadyRead = 0;
                    int bytesRead;
                    byte[] dataBuffPayload = new byte[message.getMessageLengthAsInteger() - 1];
                    while (bytesAlreadyRead < message.getMessageLengthAsInteger() - 1) {
                        bytesRead = socketInputStream.read(dataBuffPayload, bytesAlreadyRead, message.getMessageLengthAsInteger() - 1 - bytesAlreadyRead);
                        if (bytesRead == -1)
                            return;
                        bytesAlreadyRead += bytesRead;
                    }

                    byte[] dataBuffWithPayload = new byte[message.getMessageLengthAsInteger() + Message.MessageConstants.MESSAGE_LENGTH];
                    System.arraycopy(dataBufferWithoutPayload, 0, dataBuffWithPayload, 0, Message.MessageConstants.MESSAGE_LENGTH + Message.MessageConstants.MESSAGE_TYPE);
                    System.arraycopy(dataBuffPayload, 0, dataBuffWithPayload, Message.MessageConstants.MESSAGE_LENGTH + Message.MessageConstants.MESSAGE_TYPE, dataBuffPayload.length);

                    Message dataMsgWithPayload = Message.convertByteArrayToMessage(dataBuffWithPayload);
                    messageDetails.setMessage(dataMsgWithPayload);
                    messageDetails.setFromPeerID(remotePeerId);
                    messageQueue.add(messageDetails);
                }
            }

        } catch (Exception e) {
        }
    }

    public boolean handShakeMessageSent() {
        boolean messageSent = false;
        try {
            HandshakeMessage handshakeMessage = new HandshakeMessage(Message.MessageConstants.HANDSHAKE_HEADER, this.ownPeerId);
            socketOutputStream.write(HandshakeMessage.convertHandshakeMessageToBytes(handshakeMessage));
            messageSent = true;
        } catch (IOException e) {
        }
        return messageSent;
    }
}
