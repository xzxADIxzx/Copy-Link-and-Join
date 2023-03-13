package claj;

import arc.net.Connection;
import arc.net.DcReason;
import arc.net.NetListener;

/**
 * Contains a host and a client, redirects packets from one to the other.
 * 
 * @author xzxADIxzx
 */
public class Redirector implements NetListener {

    public Connection host, client;

    public Redirector(Connection host) {
        this.host = host;
    }

    @Override
    public void disconnected(Connection connection, DcReason reason) {
        host.close(DcReason.closed);
        if (client != null) client.close(DcReason.closed);
    }

    @Override
    public void received(Connection connection, Object object) {
        var receiver = connection == host ? client : host;
        if (receiver != null) receiver.sendTCP(object);
    }
}
