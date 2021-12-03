package message;

import logging.LogHelper;

import java.io.UnsupportedEncodingException;

public class HandshakeMessage {

    private byte[] headerInBytes = new byte[Message.MessageConstants.HANDSHAKE_HEADER_LENGTH];
    private byte[] peerIDInBytes = new byte[Message.MessageConstants.HANDSHAKE_PEERID_LENGTH];
    private byte[] zeroBits = new byte[Message.MessageConstants.HANDSHAKE_ZEROBITS_LENGTH];

    private String header;
    private String peerID;

    public HandshakeMessage() {
    }

    public HandshakeMessage(String header, String peerID) {
        try {
            this.header = header;
            this.headerInBytes = header.getBytes(Message.MessageConstants.DEFAULT_CHARSET);
            if (this.headerInBytes.length > Message.MessageConstants.HANDSHAKE_HEADER_LENGTH)
                throw new Exception("Handshake Header is too large");
            this.peerID = peerID;
            this.peerIDInBytes = peerID.getBytes(Message.MessageConstants.DEFAULT_CHARSET);
            if (this.peerIDInBytes.length > Message.MessageConstants.HANDSHAKE_PEERID_LENGTH)
                throw new Exception("Handshake PeerID is too large");
            this.zeroBits = "0000000000".getBytes(Message.MessageConstants.DEFAULT_CHARSET);
        } catch (Exception e) {

        }
    }

    public static byte[] convertHandshakeMessageToBytes(HandshakeMessage handshakeMessage) {
        byte[] handshakeMessageInBytes = new byte[Message.MessageConstants.HANDSHAKE_MESSAGE_LENGTH];
        try {
            if (handshakeMessage.getHeaderInBytes() == null ||
                    (handshakeMessage.getHeaderInBytes().length > Message.MessageConstants.HANDSHAKE_HEADER_LENGTH || handshakeMessage.getHeaderInBytes().length == 0))
                throw new Exception("Handshake Message Header is Invalid");
            else
                System.arraycopy(handshakeMessage.getHeaderInBytes(), 0,
                        handshakeMessageInBytes, 0, handshakeMessage.getHeaderInBytes().length);

            if (handshakeMessage.getZeroBits() == null ||
                    (handshakeMessage.getZeroBits().length > Message.MessageConstants.HANDSHAKE_ZEROBITS_LENGTH || handshakeMessage.getZeroBits().length == 0))
                throw new Exception("Handshake Message Zero Bits are Invalid");
            else//for loop / .fill
                System.arraycopy(handshakeMessage.getZeroBits(), 0,
                        handshakeMessageInBytes, Message.MessageConstants.HANDSHAKE_HEADER_LENGTH, Message.MessageConstants.HANDSHAKE_ZEROBITS_LENGTH - 1);

            if (handshakeMessage.getPeerIDInBytes() == null ||
                    (handshakeMessage.getPeerIDInBytes().length > Message.MessageConstants.HANDSHAKE_PEERID_LENGTH || handshakeMessage.getPeerIDInBytes().length == 0))
                throw new Exception("Handshake Message Peer ID is Invalid");
            else
                System.arraycopy(handshakeMessage.getPeerIDInBytes(), 0, handshakeMessageInBytes,
                        Message.MessageConstants.HANDSHAKE_HEADER_LENGTH + Message.MessageConstants.HANDSHAKE_ZEROBITS_LENGTH,
                        handshakeMessage.getPeerIDInBytes().length);
        } catch (Exception e) {
        }

        return handshakeMessageInBytes;
    }

    public static HandshakeMessage convertBytesToHandshakeMessage(byte[] handShakeMessage) {
        HandshakeMessage message = null;

        try {
            if (handShakeMessage.length != Message.MessageConstants.HANDSHAKE_MESSAGE_LENGTH)
                throw new Exception("While Decoding Handshake message length is invalid");
            message = new HandshakeMessage();
            byte[] messageHeader = new byte[Message.MessageConstants.HANDSHAKE_HEADER_LENGTH];
            byte[] messagePeerID = new byte[Message.MessageConstants.HANDSHAKE_PEERID_LENGTH];

            System.arraycopy(handShakeMessage, 0, messageHeader, 0,
                    Message.MessageConstants.HANDSHAKE_HEADER_LENGTH);
            System.arraycopy(handShakeMessage, Message.MessageConstants.HANDSHAKE_HEADER_LENGTH
                            + Message.MessageConstants.HANDSHAKE_ZEROBITS_LENGTH, messagePeerID, 0,
                    Message.MessageConstants.HANDSHAKE_PEERID_LENGTH);

            message.setHeaderFromBytes(messageHeader);
            message.setPeerIDFromBytes(messagePeerID);

        } catch (Exception e) {

        }
        return message;
    }

    public void setPeerIDFromBytes(byte[] messagePeerID) {
        try {
            peerID = (new String(messagePeerID, Message.MessageConstants.DEFAULT_CHARSET)).trim();
            peerIDInBytes = messagePeerID;
        } catch (UnsupportedEncodingException e) {
            logAndShowInConsole(e.getMessage());
        }
    }

    public void setHeaderFromBytes(byte[] messageHeader) {
        try {
            header = (new String(messageHeader, Message.MessageConstants.DEFAULT_CHARSET)).trim();
            headerInBytes = messageHeader;
        } catch (UnsupportedEncodingException e) {
            logAndShowInConsole(e.getMessage());
        }
    }

    public byte[] getHeaderInBytes() {
        return headerInBytes;
    }

    public byte[] getPeerIDInBytes() {
        return peerIDInBytes;
    }

    public byte[] getZeroBits() {
        return zeroBits;
    }

    public String getHeader() {
        return header;
    }


    public String getPeerID() {
        return peerID;
    }

    private static void logAndShowInConsole(String message) {
        LogHelper.logAndPrint(message);
    }
}
