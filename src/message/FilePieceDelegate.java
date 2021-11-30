package message;

import peer.PeerProcessUtils;

public class FilePieceDelegate {
    int isPresent;
    String fromPeerID;
    byte[] content;
    int pieceIndex;

    public FilePieceDelegate() {
    }

    public static FilePiece convertByteArrayToFilePiece(byte[] payloadInBytes) {
        byte[] indexInBytes = new byte[Message.MessageConstants.PIECE_INDEX_LENGTH];
        FilePiece filePiece = new FilePiece();
        System.arraycopy(payloadInBytes, 0, indexInBytes, 0, Message.MessageConstants.PIECE_INDEX_LENGTH);
        filePiece.setPieceIndex(PeerProcessUtils.convertByteArrayToInt(indexInBytes));
        filePiece.setContent(new byte[payloadInBytes.length - Message.MessageConstants.PIECE_INDEX_LENGTH]);
        System.arraycopy(payloadInBytes, Message.MessageConstants.PIECE_INDEX_LENGTH, filePiece.getContent(), 0, payloadInBytes.length - Message.MessageConstants.PIECE_INDEX_LENGTH);
        return filePiece;
    }

    public int getIsPresent() {
        return isPresent;
    }

    public void setIsPresent(int isPresent) {
        this.isPresent = isPresent;
    }

    public String getFromPeerID() {
        return fromPeerID;
    }

    public void setFromPeerID(String fromPeerID) {
        this.fromPeerID = fromPeerID;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public int getPieceIndex() {
        return pieceIndex;
    }

    public void setPieceIndex(int pieceIndex) {
        this.pieceIndex = pieceIndex;
    }
}