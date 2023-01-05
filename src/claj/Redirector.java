package claj;

import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;
import arc.util.Log;

/**
 * Represents a room containing a host and a client, redirects packets from one to the other.
 * 
 * @author xzxADIxzx
 */
public class Redirector implements NetListener {

    public String link;
    public Connection host, client;

    public long lastSpammed;

    public Redirector(String link, Connection host) {
        this.link = link;
        this.host = host;

        Log.info("Room @ created!", link);
    }

    @Override
    public void disconnected(Connection connection, DcReason reason) {
        host.close(DcReason.closed);
        if (client != null) client.close(DcReason.closed);

        Log.info("Room @ closed.", link);
    }

    @Override
    public void received(Connection connection, Object object) {
        var receiver = connection == host ? client : host;
        if (receiver != null) receiver.sendTCP(object);
    }
}
