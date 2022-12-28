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

    public Connection host, client;

    public Redirector(Connection host) {
        this.host = host;
        Log.info("Room @ created!", host.getID());
    }

    @Override
    public void disconnected(Connection connection, DcReason reason) {
        host.close(DcReason.closed);
        client.close(DcReason.closed);

        Log.info("Room @ closed!", host.getID());
    }

    @Override
    public void received(Connection connection, Object object) {
        (connection == host ? client : host).sendTCP(object);
    }
}
