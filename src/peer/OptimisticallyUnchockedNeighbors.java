package peer;

import logging.LogHelper;
import message.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimerTask;

/**
 * This class is used to determine optimistically unchoked neighbor from a list of choked neighbors
 */
public class OptimisticallyUnchockedNeighbors extends TimerTask {

    /**
     * This method runs asynchronously as part of timer task every 'CommonConfiguration.optimisticUnchokingInterval' interval.
     * It determines optimistically unchoked neighbor at random from all the neighbors which are choked.
     */
    @Override
    public void run() {
        peerProcess.updateOtherPeerDetails();
        peerProcess.optimisticUnchokedNeighbors.clear();

        List<RemotePeerDetails> interestedPeerDetailsInArray = new ArrayList();
        for (String peerId : peerProcess.remotePeerDetailsMap.keySet()) {
            RemotePeerDetails remotePeerDetails = peerProcess.remotePeerDetailsMap.get(peerId);
            if (peerId.equals(peerProcess.currentPeerID))
                continue;
            else if (hasPeerInterested(remotePeerDetails)) {
                interestedPeerDetailsInArray.add(remotePeerDetails);
            }
        }

        if(!interestedPeerDetailsInArray.isEmpty()) {
            //randomize the list and get the first element from it.
            Collections.shuffle(interestedPeerDetailsInArray);
            RemotePeerDetails remotePeerDetails = interestedPeerDetailsInArray.get(0);
            peerProcess.optimisticUnchokedNeighbors.put(remotePeerDetails.getId(), remotePeerDetails);
            logAndShowInConsole(peerProcess.currentPeerID + " has the optimistically unchoked neighbor " + remotePeerDetails.getId());

            if(remotePeerDetails.getIsChoked() == 1) {
                //send unchoke message if choked
                peerProcess.remotePeerDetailsMap.get(remotePeerDetails.getId()).setIsChoked(0);
                sendUnChokedMessage(peerProcess.peerToSocketMap.get(remotePeerDetails.getId()), remotePeerDetails.getId());
                sendHaveMessage(peerProcess.peerToSocketMap.get(remotePeerDetails.getId()), remotePeerDetails.getId());
                peerProcess.remotePeerDetailsMap.get(remotePeerDetails.getId()).setPeerState(3);
            }
        }
    }

    /**
     * This method is used to determine if the peer is interested to receive pieces
     * @param remotePeerDetails - peer to check whether it is interested or not
     * @return true - peer interested; false - peer not interested
     */
    private boolean hasPeerInterested(RemotePeerDetails remotePeerDetails) {
        return remotePeerDetails.getIsComplete() == 0 &&
                remotePeerDetails.getIsChoked() == 1 && remotePeerDetails.getIsInterested() == 1;
    }

    /**
     * This method is used to send UNCHOKED message to socket
     * @param socket - socket in which the message to be sent
     * @param remotePeerID - peerID to which the message should be sent
     */
    private void sendUnChokedMessage(Socket socket, String remotePeerID) {
        logAndShowInConsole(peerProcess.currentPeerID + " sending a UNCHOKE message to Peer " + remotePeerID);
        Message message = new Message(Message.MessageConstants.MESSAGE_UNCHOKE);
        byte[] messageInBytes = Message.convertMessageToByteArray(message);
        SendMessageToSocket(socket, messageInBytes);
    }

    /**
     * This method is used to send HAVE message to socket
     * @param socket - socket in which the message to be sent
     * @param peerID - peerID to which the message should be sent
     */
    private void sendHaveMessage(Socket socket, String peerID) {
        logAndShowInConsole(peerProcess.currentPeerID + " sending HAVE message to Peer " + peerID);
        byte[] bitFieldInBytes = peerProcess.bitFieldMessage.getBytes();
        Message message = new Message(Message.MessageConstants.MESSAGE_HAVE, bitFieldInBytes);
        SendMessageToSocket(socket, Message.convertMessageToByteArray(message));
    }

    /**
     * This method is used to write a message to socket
     * @param socket - socket in which the message to be sent
     * @param messageInBytes - message to be sent
     */
    private void SendMessageToSocket(Socket socket, byte[] messageInBytes) {
        try {
            OutputStream out = socket.getOutputStream();
            out.write(messageInBytes);
        } catch (IOException e) {
        }
    }

    /**
     * This method is used to log a message in a log file and show it in console
     * @param message - message to be logged and showed in console
     */
    private static void logAndShowInConsole(String message) {
        LogHelper.logAndPrint(message);
    }
}