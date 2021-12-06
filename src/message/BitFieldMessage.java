package message;

import config.CommonConfiguration;
import peer.peerProcess;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import static logging.LogHelper.logAndPrint;

public class BitFieldMessage {

    private Piece[] pieces;
    private int numberOfPieces;

    public BitFieldMessage() {
        Double fileSize = Double.parseDouble(String.valueOf(CommonConfiguration.fileSize));
        Double pieceSize = Double.parseDouble(String.valueOf(CommonConfiguration.pieceSize));
        numberOfPieces = (int) Math.ceil(fileSize / pieceSize);
        pieces = new Piece[numberOfPieces];

        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = new Piece();
        }

    }

    public Piece[] getPieces() {
        return pieces;
    }


    public int getNumberOfPieces() {
        return numberOfPieces;
    }

    public void setPieceDetails(String peerId, int hasFile) {

        Arrays.asList(pieces).stream().forEach(x -> {
            x.setIsPresent(hasFile);
            x.setFromPeerID(peerId);
        });
    }

    public byte[] getBytes() {
        int s = numberOfPieces >> 3;
        if ((numberOfPieces % 8) != 0) // n%m == n & (m-1)
            s = s + 1;
        byte[] iP = new byte[s];
        int tempInt = 0;
        int count = 0;
        int cnt;
        for (cnt = 1; cnt <= numberOfPieces; cnt++) {
            int tempP = pieces[cnt - 1].getIsPresent();
            tempInt = tempInt << 1;
            if (tempP == 1) {
                tempInt = tempInt + 1;
            } else
                tempInt = tempInt + 0;

            if ((cnt & 7) == 0 && cnt != 0) {
                iP[count] = (byte) tempInt;
                count++;
                tempInt = 0;
            }

        }
        if (((cnt - 1) & 7) != 0) {
            int tempShift = ((numberOfPieces) - (numberOfPieces / 8) * 8);
            tempInt = tempInt << (8 - tempShift);
            iP[count] = (byte) tempInt;
        }
        return iP;
    }

    public static BitFieldMessage decodeMessage(byte[] bitField) {
        BitFieldMessage bitFieldMessage = new BitFieldMessage();
        for (int i = 0; i < bitField.length; i++) {
            int count = 7;
            while (count >= 0) {
                int test = 1 << count;
                if (i * 8 + (8 - count - 1) < bitFieldMessage.getNumberOfPieces()) {
                    if ((bitField[i] & (test)) != 0)
                        bitFieldMessage.getPieces()[i * 8 + (8 - count - 1)].setIsPresent(1);
                    else
                        bitFieldMessage.getPieces()[i * 8 + (8 - count - 1)].setIsPresent(0);
                }
                count--;
            }
        }

        return bitFieldMessage;
    }

    public int getNumberOfPiecesPresent() {
        int count = 0;
        for (Piece filePiece : pieces) {
            if (filePiece.getIsPresent() == 1) {
                count++;
            }
        }

        return count;
    }

    public boolean isFileDownloadComplete() {
        boolean isFileDownloaded = true;
        for (Piece filePiece : pieces) {
            if (filePiece.getIsPresent() == 0) {
                isFileDownloaded = false;
                break;
            }
        }

        return isFileDownloaded;
    }

    public synchronized int getInterestingPieceIndex(BitFieldMessage bitFieldMessage) {
        int numberOfPieces = bitFieldMessage.getNumberOfPieces();
        int interestingPiece = -1;

        for (int i = 0; i < numberOfPieces; i++) {
            if (bitFieldMessage.getPieces()[i].getIsPresent() == 1
                    && this.getPieces()[i].getIsPresent() == 0) {
                interestingPiece = i;
                break;
            }
        }

        return interestingPiece;
    }

    public synchronized int getFirstDifferentPieceIndex(BitFieldMessage bitFieldMessage) {
        int firstPieces = numberOfPieces;
        int secondPieces = bitFieldMessage.getNumberOfPieces();
        int pieceIndex = -1;

        for (int i = 0; i < Math.min(firstPieces, secondPieces); i++) {
            if (pieces[i].getIsPresent() == 0 && bitFieldMessage.getPieces()[i].getIsPresent() == 1) {
                pieceIndex = i;
                break;
            }
        }

        return pieceIndex;
    }

    public void updateBitFieldInformation(String peerID, Piece filePiece) {
        int pieceIndex = filePiece.getPieceIndex();
        try {
            if (isPieceAlreadyPresent(pieceIndex)) {
                logAndPrint("Piece already received");
            } else {
                String fileName = CommonConfiguration.fileName;

                File file = new File(peerProcess.currentPeerID, fileName);
                int offSet = pieceIndex * CommonConfiguration.pieceSize;
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                byte[] pieceToWrite = filePiece.getContent();
                randomAccessFile.seek(offSet);
                randomAccessFile.write(pieceToWrite);

                pieces[pieceIndex].setIsPresent(1);
                pieces[pieceIndex].setFromPeerID(peerID);
                randomAccessFile.close();
                logAndPrint("has downloaded the PIECE " + pieceIndex
                        + " from Peer " + peerID + ". Number of pieces now is "
                        + peerProcess.bitFieldMessage.getNumberOfPiecesPresent());

                if (peerProcess.bitFieldMessage.isFileDownloadComplete()) {
                    //update file download details
                    peerProcess.remotePeerDetailsMap.get(peerID).setIsInterested(0);
                    peerProcess.remotePeerDetailsMap.get(peerID).setIsComplete(1);
                    peerProcess.remotePeerDetailsMap.get(peerID).setIsChoked(0);
                    peerProcess.remotePeerDetailsMap.get(peerID).updatePeerDetails(peerProcess.currentPeerID, 1);
                    logAndPrint("has DOWNLOADED the complete file.");
                    logAndPrint("Waiting for other peers to finish");
                }
            }
        } catch (IOException e) {
            logAndPrint(peerProcess.currentPeerID + " ERROR in updating bitfield " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isPieceAlreadyPresent(int pieceIndex) {
        return peerProcess.bitFieldMessage.getPieces()[pieceIndex].getIsPresent() == 1;
    }

}