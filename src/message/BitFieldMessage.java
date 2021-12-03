package message;

import config.CommonConfiguration;
import peer.peerProcess;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.stream.Collectors;

import static logging.LogHelper.logAndPrint;

public class BitFieldMessage {

    private Piece[] pieces;
    private int numberOfPieces;

    public BitFieldMessage() {
        Double fileSize = Double.parseDouble(String.valueOf(CommonConfiguration.fileSize));
        Double pieceSize = Double.parseDouble(String.valueOf(CommonConfiguration.pieceSize));
        numberOfPieces = (int) Math.ceil((double) fileSize / (double) pieceSize);
        pieces = new Piece[numberOfPieces];
        Arrays.fill(pieces, new Piece());
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


    public int getNumberOfPieces() {
        return numberOfPieces;
    }

    public Piece[] getPieces() {
        return pieces;
    }

    public void setPieceDetails(String peerId, int hasFile) {

        Arrays.asList(pieces).stream().forEach(x -> {
            x.setIsPresent(hasFile);
            x.setFromPeerID(peerId);
        });
    }

    public byte[] getBytes() {
        int s = numberOfPieces >> 3;
        if (numberOfPieces % 8 != 0)
            s = s + 1;
        byte[] iP = new byte[s];
        int tempInt = 0;
        int count = 0;
        int Cnt;
        for (Cnt = 1; Cnt <= numberOfPieces; Cnt++) {
            int tempP = pieces[Cnt - 1].getIsPresent();
            tempInt = tempInt << 1;
            if (tempP == 1) {
                tempInt = tempInt + 1;
            } else
                tempInt = tempInt + 0;

            if (Cnt % 8 == 0 && Cnt != 0) {
                iP[count] = (byte) tempInt;
                count++;
                tempInt = 0;
            }

        }
        if ((Cnt - 1) % 8 != 0) {
            int tempShift = ((numberOfPieces) - (numberOfPieces / 8) * 8);
            tempInt = tempInt << (8 - tempShift);
            iP[count] = (byte) tempInt;
        }
        return iP;
    }

    public int getNumberOfPiecesPresent() {
        return Arrays.asList(pieces).stream().filter(x -> x.getIsPresent() == 1).collect(Collectors.toList()).size();
    }

    public boolean isFileDownloadComplete() {
        return Arrays.asList(pieces).stream().filter(x -> x.getIsPresent() == 0).count() > 0;
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

    public void updateBitFieldInformation(String peerID, Piece piece) {
        int pieceIndex = piece.getPieceIndex();
        try {
            if (isPieceAlreadyPresent(pieceIndex)) {
                logAndPrint(peerID + " Piece already received");
            } else {
                String fileName = CommonConfiguration.fileName;

                File file = new File(peerProcess.currentPeerID, fileName);
                int offSet = pieceIndex * CommonConfiguration.pieceSize;
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                byte[] pieceToWrite = piece.getContent();
                randomAccessFile.seek(offSet);
                randomAccessFile.write(pieceToWrite);

                pieces[pieceIndex].setIsPresent(1);
                pieces[pieceIndex].setFromPeerID(peerID);
                randomAccessFile.close();
                logAndPrint(peerProcess.currentPeerID + " has downloaded the PIECE " + pieceIndex
                        + " from Peer " + peerID + ". Now the number of pieces it has is "
                        + peerProcess.bitFieldMessage.getNumberOfPiecesPresent());

                if (peerProcess.bitFieldMessage.isFileDownloadComplete()) {
                    //update file download details
                    peerProcess.remotePeerDetailsMap.get(peerID).setIsInterested(0);
                    peerProcess.remotePeerDetailsMap.get(peerID).setIsComplete(1);
                    peerProcess.remotePeerDetailsMap.get(peerID).setIsChoked(0);
                    peerProcess.remotePeerDetailsMap.get(peerID).updatePeerDetails(peerProcess.currentPeerID, 1);
                    logAndPrint(peerProcess.currentPeerID + " has DOWNLOADED the complete file.");
                }
            }
        } catch (IOException e) {
            logAndPrint(peerProcess.currentPeerID + " EROR in updating bitfield " + e.getMessage());

            e.printStackTrace();
        }
    }

    private boolean isPieceAlreadyPresent(int pieceIndex) {
        return peerProcess.bitFieldMessage.getPieces()[pieceIndex].getIsPresent() == 1;
    }

}