package message;

import config.CommonConfiguration;

/**
 * This class is used to handle file piece information
 */
public class Piece {
    private final FilePieceDelegate filePieceDelegate = new FilePieceDelegate();

    public Piece() {
        filePieceDelegate.setContent(new byte[CommonConfiguration.pieceSize]);
        filePieceDelegate.setPieceIndex(-1);
        filePieceDelegate.setIsPresent(0);
        filePieceDelegate.setFromPeerID(null);
    }

    public int getIsPresent() {
        return filePieceDelegate.getIsPresent();
    }

    public void setIsPresent(int isPresent) {
        filePieceDelegate.setIsPresent(isPresent);
    }
    
    public void setFromPeerID(String fromPeerID) {
        filePieceDelegate.setFromPeerID(fromPeerID);
    }

    public byte[] getContent() {
        return filePieceDelegate.getContent();
    }

    public void setContent(byte[] content) {
        filePieceDelegate.setContent(content);
    }

    public int getPieceIndex() {
        return filePieceDelegate.getPieceIndex();
    }

    public void setPieceIndex(int pieceIndex) {
        filePieceDelegate.setPieceIndex(pieceIndex);
    }

}
