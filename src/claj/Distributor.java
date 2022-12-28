package claj;

import arc.net.Connection;
import arc.net.DcReason;
import arc.net.FrameworkMessage;
import arc.net.NetListener;
import arc.net.Server;
import arc.struct.IntMap;
import arc.util.Log;

import java.io.IOException;

/**
 * It is an entry point for clients, distributes their packets to redirectors.
 * 
 * @author xzxADIxzx
 */
public class Distributor extends Server {

    public IntMap<Redirector> redirectors;

    public Distributor() {
        super(32768, 8192, new Serializer());
        addListener(new Listener());
    }

    public void run(int port) throws IOException {
        bind(port, port);
        run();
    }

    public class Listener implements NetListener {

        @Override
        public void connected(Connection connection) {
            Log.info("Connection @ received!", connection.getID());

            if (redirectors.containsKey(connection.getID())) redirectors.put(connection.getID(), new Redirector(connection));
        }

        @Override
        public void disconnected(Connection connection, DcReason reason) {
            Log.info("Connection @ lost: @", connection.getID(), reason);

            var redirector = redirectors.remove(connection.getID());
            if (redirector != null) redirector.disconnected(connection, reason);
        }

        @Override
        public void received(Connection connection, Object object) {
            if (object instanceof FrameworkMessage) return;

            var redirector = redirectors.get(connection.getID());
            if (redirector != null) redirector.received(connection, object);
        }
    }
}
