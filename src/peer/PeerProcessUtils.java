package peer;

import java.nio.ByteBuffer;

/**
 * Utils class that contains common utility methods
 */
public class PeerProcessUtils {

    public static byte[] convertIntToByteArray(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static int convertByteArrayToInt(byte[] dataInBytes) {
        return ByteBuffer.wrap(dataInBytes).getInt();
    }
}
