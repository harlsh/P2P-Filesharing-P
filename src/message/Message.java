package message;

import logging.LogHelper;
import peer.peerProcess;

import java.io.UnsupportedEncodingException;

public class Message {
    //Type of message
    private String type;
    //Length of the message
    private String length;
    //The length of data in the message
    private int dataLength = MessageConstants.MESSAGE_TYPE;
    //Type of message in bytes
    private byte[] typeInBytes = null;
    //Length of the message in bytes
    private byte[] lengthInBytes = null;
    //The content of the message
    private byte[] payload = null;

    public Message() {
    }

    public Message(String messageType) {
        try {
            if (messageType == MessageConstants.MESSAGE_INTERESTED || messageType == MessageConstants.MESSAGE_NOT_INTERESTED ||
                    messageType == MessageConstants.MESSAGE_CHOKE || messageType == MessageConstants.MESSAGE_UNCHOKE
                    || messageType == MessageConstants.MESSAGE_DOWNLOADED) {
                setMessageLength(1);
                setMessageType(messageType);
                this.payload = null;
            } else {
                logAndPrint("Error Occurred while initialzing Message constructor");
                throw new Exception("Message Constructor - Wrong constructor selected");
            }
        } catch (Exception e) {
            logAndPrint(e.getMessage());
        }
    }

    public Message(String messageType, byte[] payload) {
        try {
            if (payload != null) {
                setMessageLength(payload.length + 1);
                if (lengthInBytes.length > MessageConstants.MESSAGE_LENGTH) {
                    logAndPrint("Error Occurred while initialzing Message constructor");
                    throw new Exception("Message Constructor - Message Length is too large");
                }
                setPayload(payload);
            } else {
                if (messageType == MessageConstants.MESSAGE_INTERESTED || messageType == MessageConstants.MESSAGE_NOT_INTERESTED
                        || messageType == MessageConstants.MESSAGE_CHOKE || messageType == MessageConstants.MESSAGE_UNCHOKE
                        || messageType == MessageConstants.MESSAGE_DOWNLOADED) {
                    setMessageLength(1);
                    this.payload = null;
                } else {
                    logAndPrint("Error Occurred while initialzing Message constructor");
                    throw new Exception("Message Constructor - Message Payload should not be null");
                }
            }
            setMessageType(messageType);
            if (typeInBytes.length > MessageConstants.MESSAGE_TYPE) {
                logAndPrint("Error Occurred while initialzing Message constructor");
                throw new Exception("Message Constructor - Message Type length is too large");
            }
        } catch (Exception e) {
            logAndPrint("Error Occurred while initialzing Message constructor - " + e.getMessage());
        }
    }

    private static void logAndPrint(String message) {
        LogHelper.logAndPrint(message);
    }

    public static Message convertByteArrayToMessage(byte[] message) {

        Message msg = new Message();
        byte[] msgLength = new byte[MessageConstants.MESSAGE_LENGTH];
        byte[] msgType = new byte[MessageConstants.MESSAGE_TYPE];
        byte[] payLoad = null;
        int len;

        try {
            if (message == null)
                throw new Exception("Invalid data.");
            else if (message.length < MessageConstants.MESSAGE_LENGTH + MessageConstants.MESSAGE_TYPE)
                throw new Exception("Byte array length is too small...");


            System.arraycopy(message, 0, msgLength, 0, MessageConstants.MESSAGE_LENGTH);
            System.arraycopy(message, MessageConstants.MESSAGE_LENGTH, msgType, 0, MessageConstants.MESSAGE_TYPE);

            msg.setMessageLength(msgLength);
            msg.setMessageType(msgType);

            len = peerProcess.PeerProcessUtils.convertByteArrayToInt(msgLength);

            if (len > 1) {
                payLoad = new byte[len - 1];
                System.arraycopy(message, MessageConstants.MESSAGE_LENGTH + MessageConstants.MESSAGE_TYPE, payLoad, 0, message.length - MessageConstants.MESSAGE_LENGTH - MessageConstants.MESSAGE_TYPE);
                msg.setPayload(payLoad);
            }
        } catch (Exception e) {
            LogHelper.logAndPrint(e.toString());
            msg = null;
        }
        return msg;
    }

    public void setMessageLength(int messageLength) {
        dataLength = messageLength;
        length = ((Integer) messageLength).toString();
        lengthInBytes = peerProcess.PeerProcessUtils.convertIntToByteArray(messageLength);
    }

    public void setMessageType(String messageType) {
        type = messageType.trim();
        try {
            typeInBytes = messageType.getBytes(MessageConstants.DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            logAndPrint(e.getMessage());
            e.printStackTrace();
        }
    }

    public int getMessageLengthAsInteger() {
        return this.dataLength;
    }

    public static byte[] convertMessageToByteArray(Message message) {
        byte[] messageInByteArray = null;
        try {
            int messageType = Integer.parseInt(message.getType());
            if (message.getLengthInBytes().length > MessageConstants.MESSAGE_LENGTH)
                throw new Exception("Message Length is Invalid.");
            else if (messageType < 0 || messageType > 8)
                throw new Exception("Message Type is Invalid.");
            else if (message.getTypeInBytes() == null)
                throw new Exception("Message Type is Invalid.");
            else if (message.getLengthInBytes() == null)
                throw new Exception("Message Length is Invalid.");

            if (message.getPayload() != null) {
                messageInByteArray = new byte[MessageConstants.MESSAGE_LENGTH + MessageConstants.MESSAGE_TYPE + message.getPayload().length];
                System.arraycopy(message.getLengthInBytes(), 0, messageInByteArray, 0, message.getLengthInBytes().length);
                System.arraycopy(message.getTypeInBytes(), 0, messageInByteArray, MessageConstants.MESSAGE_LENGTH, MessageConstants.MESSAGE_TYPE);
                System.arraycopy(message.getPayload(), 0, messageInByteArray,
                        MessageConstants.MESSAGE_LENGTH + MessageConstants.MESSAGE_TYPE, message.getPayload().length);
            } else {
                messageInByteArray = new byte[MessageConstants.MESSAGE_LENGTH + MessageConstants.MESSAGE_TYPE];
                System.arraycopy(message.getLengthInBytes(), 0, messageInByteArray, 0, message.getLengthInBytes().length);
                System.arraycopy(message.getTypeInBytes(), 0, messageInByteArray, MessageConstants.MESSAGE_LENGTH, MessageConstants.MESSAGE_TYPE);
            }
        } catch (Exception e) {
        }

        return messageInByteArray;
    }

    public void setMessageLength(byte[] len) {

        Integer l = peerProcess.PeerProcessUtils.convertByteArrayToInt(len);
        this.length = l.toString();
        this.lengthInBytes = len;
        this.dataLength = l;
    }

    public String getType() {
        return type;
    }

    public byte[] getTypeInBytes() {
        return typeInBytes;
    }

    public byte[] getLengthInBytes() {
        return lengthInBytes;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public void setMessageType(byte[] type) {
        try {
            this.type = new String(type, MessageConstants.DEFAULT_CHARSET);
            this.typeInBytes = type;
        } catch (UnsupportedEncodingException e) {
            logAndPrint(e.toString());
        }
    }

    public static class MessageConstants {

        public static final String DEFAULT_CHARSET = "UTF8";

        public static final int HANDSHAKE_MESSAGE_LENGTH = 32;

        public static final int HANDSHAKE_HEADER_LENGTH = 18;

        public static final int HANDSHAKE_ZEROBITS_LENGTH = 10;

        public static final int HANDSHAKE_PEERID_LENGTH = 4;

        public static final int MESSAGE_LENGTH = 4;

        public static final int MESSAGE_TYPE = 1;

        public static final String MESSAGE_CHOKE = "0";

        public static final String MESSAGE_UNCHOKE = "1";

        public static final String MESSAGE_INTERESTED = "2";

        public static final String MESSAGE_NOT_INTERESTED = "3";

        public static final String MESSAGE_HAVE = "4";

        public static final String MESSAGE_BITFIELD = "5";

        public static final String MESSAGE_REQUEST = "6";

        public static final String MESSAGE_PIECE = "7";

        public static final String MESSAGE_DOWNLOADED = "8";

        public static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";

        public static final int PIECE_INDEX_LENGTH = 4;

        public static final int ACTIVE_CONNECTION = 1;

    }
}
