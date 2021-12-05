package server;

import message.HandshakeMessage;
import message.Message;
import message.MessageInfo;
import peer.peerProcess;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static logging.LogHelper.logAndPrint;
import static peer.peerProcess.messageQueue;

public class MessageHandler implements Runnable {
    private final ThreadLocal<HandshakeMessage> handshakeMessage = new ThreadLocal<>();
    String ownPeerId;
    String remotePeerId;
    private int connType;
    private InputStream socketInputStream;
    private OutputStream socketOutputStream;
    private Socket peerSocket;

    public MessageHandler(String address, int port, int connectionType, String serverPeerID) {
        try {
            connType = connectionType;
            ownPeerId = serverPeerID;
            peerSocket = new Socket(address, port);
            socketInputStream = peerSocket.getInputStream();
            socketOutputStream = peerSocket.getOutputStream();


        } catch (IOException e) {
        }
    }

    public MessageHandler(Socket socket, int connectionType, String serverPeerID) {
        try {
            peerSocket = socket;
            connType = connectionType;
            ownPeerId = serverPeerID;
            socketInputStream = peerSocket.getInputStream();
            socketOutputStream = peerSocket.getOutputStream();
        } catch (IOException e) {

        }
    }


    @Override
    public void run() {
        byte[] handShakeMessageInBytes = new byte[32];
        byte[] dataBufferWithoutPayload = new byte[Message.MessageConstants.MESSAGE_LENGTH + Message.MessageConstants.MESSAGE_TYPE];
        byte[] messageLengthInBytes;
        byte[] messageTypeInBytes;
        MessageInfo messageInfo = new MessageInfo();
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
                    handshakeMessage.set(HandshakeMessage.convertBytesToHandshakeMessage(handShakeMessageInBytes));
                    if (handshakeMessage.get().getHeader().equals(Message.MessageConstants.HANDSHAKE_HEADER)) {
                        remotePeerId = handshakeMessage.get().getPeerID();
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
            } else {
                while (true) {
                    socketInputStream.read(handShakeMessageInBytes);
                    handshakeMessage.set(HandshakeMessage.convertBytesToHandshakeMessage(handShakeMessageInBytes));
                    if (handshakeMessage.get().getHeader().equals(Message.MessageConstants.HANDSHAKE_HEADER)) {
                        remotePeerId = handshakeMessage.get().getPeerID();
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
                    messageInfo.setMessage(message);
                    messageInfo.setFromPeerID(remotePeerId);
                    messageQueue.add(messageInfo);
                } else if (messageType.equals(Message.MessageConstants.MESSAGE_DOWNLOADED)) {
                    messageInfo.setMessage(message);
                    messageInfo.setFromPeerID(remotePeerId);
                    int peerState = peerProcess.remotePeerDetailsMap.get(remotePeerId).getPeerState();
                    peerProcess.remotePeerDetailsMap.get(remotePeerId).setPreviousPeerState(peerState);
                    peerProcess.remotePeerDetailsMap.get(remotePeerId).setPeerState(15);
                    messageQueue.add(messageInfo);
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
                    messageInfo.setMessage(dataMsgWithPayload);
                    messageInfo.setFromPeerID(remotePeerId);
                    messageQueue.add(messageInfo);
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
