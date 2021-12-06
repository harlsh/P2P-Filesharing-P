import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import peer.RemotePeerInfo;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

/*
 * The StartRemotePeers class begins remote peer processes.
 * It reads configuration file PeerInfo.cfg and starts remote peer processes.
 * You must modify this program a little bit if your peer processes are written in C or C++.
 * Please look at the lines below the comment saying IMPORTANT.
 * It uses jsch library to setup ssh connection.
 */
public class StartRemotePeers {

    public Vector<RemotePeerInfo> peerInfoVector;
    public static String path = System.getProperty("user.dir");

    /**
     * @param args
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {

        try {
            StartRemotePeers myStart = new StartRemotePeers();
            myStart.getConfiguration();
            Session session;
            ChannelExec channel;

            System.out.println("path: " + path);

            Scanner scanner = new Scanner(System.in);
            Console console = System.console();
            System.out.print("Enter username: ");
            String username = scanner.next();

            System.out.println();
            System.out.print("Enter password: ");

            String password = new String(console.readPassword());
            for (int i = 0; i < myStart.peerInfoVector.size(); i++) {
                RemotePeerInfo pInfo = myStart.peerInfoVector.elementAt(i);

                System.out.println("Starting remote peer " + pInfo.getId() + " at " + pInfo.getHostAddress());

                session = new JSch().getSession(username,  pInfo.getHostAddress() , 22);
                session.setPassword(password);
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect();

                channel = (ChannelExec) session.openChannel("exec");

                channel.setCommand("cd " + path + "; java peer.peerProcess " + pInfo.getId());
                ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
                channel.setOutputStream(responseStream);
                channel.connect();
                Thread.sleep(1000);
            }
            System.out.println("Started all remote peers");
            System.exit(0);
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public void getConfiguration() {
        peerInfoVector = new Vector();
        try {
            System.out.println("Reading data from PeerInfo.cfg");
            List<String> lines = Files.readAllLines(Paths.get("PeerInfo.cfg"));
            for (int i = 0; i < lines.size(); i++) {
                String[] properties = lines.get(i).split("\\s+");
                peerInfoVector.addElement(new RemotePeerInfo(properties[0], properties[1], properties[2], Integer.parseInt(properties[3]), i));
            }
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }
}
