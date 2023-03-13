package claj;

import arc.net.Connection;
import arc.net.DcReason;
import arc.struct.Seq;
import arc.util.Log;

/**
 * Represents a room containing a host and redirectors.
 * 
 * @author xzxADIxzx
 */
public class Room {

    public String link;

    public Connection host;
    public Seq<Redirector> redirectors = new Seq<>();

    public Room(String link, Connection host) {
        this.link = link;
        this.host = host;

        Log.info("Room @ created!", link);
    }

    public void close() {
        // rooms only closes if the host left, so there's no point in disconnecting it again
        redirectors.each(r -> r.disconnected(null, DcReason.closed));

        Log.info("Room @ closed.", link);
    }

    public void sendMessage(String message) {
        host.sendTCP(message);
    }
}
