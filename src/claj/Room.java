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

        sendMessage("new"); // there must be at least one empty redirector in the room
        sendMessage("Hello, it's me, [#0096FF]xzxADIxzx#7729[], the creator of CLaJ."); // some contact info
        sendMessage("I just wanted to say that if you have any problems, you can always message me on Discord.");

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
