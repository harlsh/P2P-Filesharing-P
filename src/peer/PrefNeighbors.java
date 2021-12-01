package peer;

        import config.CommonConfiguration;
        import logging.LogHelper;
        import message.Message;
        import message.MessageConstants;

        import java.io.IOException;
        import java.io.OutputStream;
        import java.net.Socket;
        import java.util.*;

public class PrefNeighbors extends TimerTask {

    public void run() {
        StringBuilder preferredNeighbors = new StringBuilder();  // preferredNeighbors is a instance of StringBuilder for log
        peerProcess.updateOtherPeerDetails();  //updates remotePeerInfo from PeerInfo.cfg to remotePeerDetailsMap

        List<RemotePeerDetails> interestedPeerDetails = new ArrayList();
        // scan through all peer
        for (String peerId : peerProcess.remotePeerDetailsMap.keySet()) {
            if (!peerId.equals(peerProcess.currentPeerId))
                continue;
            if (peerProcess.remotePeerDetailsMap.get(peerId).getIsComplete() == 1)
                peerProcess.preferredNeighboursMap.remove(peerId);
            else if (peerProcess.remotePeerDetailsMap.get(peerId).getIsComplete() == 0
                    && peerProcess.remotePeerDetailsMap.get(peerId).getIsInterested() == 1
                    // no need to check !peerId.equals(peerProcess.currentPeerID, since we check it in first if
                    ) {
                interestedPeerDetails.add(peerProcess.remotePeerDetailsMap.get(peerId));
            }
        }

        if (interestedPeerDetails.size() > CommonConfiguration.numberOfPreferredNeighbours) {

            peerProcess.preferredNeighboursMap.clear();

            // if peer A has the complete file, it determine preferred neighbors randomly among those
            // that are interested in its data rather than comparing downloading rates.
            if (peerProcess.remotePeerDetailsMap.get(peerProcess.currentPeerID).getIsComplete() == 1) {
                Collections.shuffle(interestedPeerDetails);
            } else {
                // harish's comparator
                Collections.sort(interestedPeerDetails, (RemotePeerDetails a, RemotePeerDetails b) -> a.compareTo(b));
            }
            int countPreferredPeers = 0;

            // update preferredNeighboursMap and preferredNeighbors
            for (RemotePeerDetails peerDetail: interestedPeerDetails){

                String peerId = peerDetail.getId();

                peerProcess.remotePeerDetailsMap.get(peerId).setIsPreferredNeighbor(1);
                peerProcess.preferredNeighboursMap.put(
                        peerId,
                        peerDetail
                        // Question: can we use peerDetail directly?
                        // like: peerDetail
                        // original one: peerProcess.remotePeerDetailsMap.get(peerDetail.getId())
                );
                preferredNeighbors.append(peerId).append(",");

                if (peerProcess.remotePeerDetailsMap.get(peerId).getIsChoked() == 1) {
                    sendUnChokedMessage(peerProcess.peerToSocketMap.get(peerId), peerId);
                    peerProcess.remotePeerDetailsMap.get(peerId).setIsChoked(0);
                    // not sure why sending have
                    sendHaveMessage(peerProcess.peerToSocketMap.get(peerId), peerId);
                    peerProcess.remotePeerDetailsMap.get(peerId).setPeerState(3);
                }

                countPreferredPeers = countPreferredPeers+1;
                if (countPreferredPeers > CommonConfiguration.numberOfPreferredNeighbours - 1)
                    break;
            }
        } else {
            //add all the interested neighbors to list
            remotePeerIDs = peerProcess.remotePeerDetailsMap.keySet();
            for (String key : remotePeerIDs) {
                RemotePeerDetails remotePeerDetails = peerProcess.remotePeerDetailsMap.get(key);
                if (!key.equals(peerProcess.currentPeerID)) {
                    if (remotePeerDetails.getIsComplete() == 0 && remotePeerDetails.getIsInterested() == 1) {
                        if (!peerProcess.preferredNeighboursMap.containsKey(key)) {
                            preferredNeighbors.append(key).append(",");
                            peerProcess.preferredNeighboursMap.put(key, peerProcess.remotePeerDetailsMap.get(key));
                            peerProcess.remotePeerDetailsMap.get(key).setIsPreferredNeighbor(1);
                        }
                        if (remotePeerDetails.getIsChoked() == 1) {
                            sendUnChokedMessage(peerProcess.peerToSocketMap.get(key), key);
                            peerProcess.remotePeerDetailsMap.get(key).setIsChoked(0);
                            sendHaveMessage(peerProcess.peerToSocketMap.get(key), key);
                            peerProcess.remotePeerDetailsMap.get(key).setPeerState(3);
                        }
                    }
                }
            }
        }

        if (preferredNeighbors.length() != 0) {
            preferredNeighbors.deleteCharAt(preferredNeighbors.length() - 1);
            logAndShowInConsole(peerProcess.currentPeerID + " has selected the preferred neighbors - " + preferredNeighbors.toString());
        }
    }

    /**
     * This method is used to send UNCHOKE message to socket
     * @param socket - socket in which the message to be sent
     * @param remotePeerID - peerID to which the message should be sent
     */
    private static void sendUnChokedMessage(Socket socket, String remotePeerID) {
        logAndShowInConsole(peerProcess.currentPeerID + " sending a UNCHOKE message to Peer " + remotePeerID);
        Message message = new Message(MessageConstants.MESSAGE_UNCHOKE);
        SendMessageToSocket(socket, Message.convertMessageToByteArray(message));
    }


    /**
     * This method is used to send HAVE message to socket
     * @param socket - socket in which the message to be sent
     * @param peerID - peerID to which the message should be sent
     */
    private void sendHaveMessage(Socket socket, String peerID) {
        logAndShowInConsole(peer.peerProcess.currentPeerID + " sending HAVE message to Peer " + peerID);
        byte[] bitFieldInBytes = peerProcess.bitFieldMessage.getBytes();
        Message message = new Message(MessageConstants.MESSAGE_HAVE, bitFieldInBytes);
        SendMessageToSocket(socket, Message.convertMessageToByteArray(message));
    }

    /**
     * This method is used to write a message to socket
     * @param socket - socket in which the message to be sent
     * @param messageInBytes - message to be sent
     */
    private static void SendMessageToSocket(Socket socket, byte[] messageInBytes) {
        try {
            OutputStream out = socket.getOutputStream();
            out.write(messageInBytes);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    /**
     * This method is used to log a message in a log file and show it in console
     * @param message - message to be logged and showed in console
     */
    private static void logAndShowInConsole(String message) {
        LogHelper.logAndShowInConsole(message);
    }
}
