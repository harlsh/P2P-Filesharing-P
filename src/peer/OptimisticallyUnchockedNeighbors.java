package peer;

import logging.LogHelper;
import message.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.TimerTask;
import java.util.Vector;

public class OptimisticallyUnchockedNeighbors extends TimerTask {

    @Override
    public void run() {
        peerProcess.updateOtherPeerDetails();
        peerProcess.optimisticUnchokedNeighbors.clear();

        List<RemotePeerDetails> interestedPeerDetailsInArray = new ArrayList();
        for (RemotePeerDetails peerDetailObject : peerProcess.remotePeerDetailsMap.values()){
            if (peerDetailObject.getId().equals(peerProcess.currentPeerID))
                continue;
            else if (  peerDetailObject.getIsComplete()==0
                    && peerDetailObject.getIsChoked()==1
                    && peerDetailObject.getInterested()==1){
                interestedPeerDetailsInArray.add(peerDetailObject);
            }
        }

        if(interestedPeerDetailsInArray.size()>0) {
            Collections.shuffle(interestedPeerDetailsInArray);
            RemotePeerDetails selectedPeerDeatilObject = interestedPeerDetailsInArray.firstElement();
            String selectedPeerId = selectedPeerDeatilObject.getId();
            peerProcess.optimisticUnchokedNeighbors.put(selectedPeerId, selectedPeerDeatilObject);
            logAndShowInConsole(peerProcess.currentPeerID + " has the optimistically unchoked neighbor " + selectedPeerId);

            if(selectedPeerDeatilObject.getIsChoked() == 1) {
                //send unchoke message if choked
                peerProcess.remotePeerDetailsMap.get(selectedPeerDeatilObject.getId()).setIsChoked(0);
                sendUnChokedMessage(peerProcess.peerToSocketMap.get(selectedPeerDeatilObject.getId()), selectedPeerDeatilObject.getId());
                sendHaveMessage(peerProcess.peerToSocketMap.get(selectedPeerDeatilObject.getId()), selectedPeerDeatilObject.getId());
                peerProcess.remotePeerDetailsMap.get(selectedPeerDeatilObject.getId()).setPeerState(3);
            }
        }
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
