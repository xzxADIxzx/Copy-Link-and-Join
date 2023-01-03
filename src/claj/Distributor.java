package claj;

import arc.math.Mathf;
import arc.net.Connection;
import arc.net.DcReason;
import arc.net.FrameworkMessage;
import arc.net.NetListener;
import arc.net.Server;
import arc.struct.IntMap;
import arc.struct.IntMap.Entry;
import arc.util.Log;
import arc.util.Ratekeeper;

import java.io.IOException;

/**
 * It is an entry point for clients, distributes their packets to redirectors.
 * 
 * @author xzxADIxzx
 */
public class Distributor extends Server {

    /** List of all characters that are allowed in a link. */
    public static final char[] symbols = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwYyXxZz".toCharArray();

    /** Limit for packet count sent within 3 sec that will lead to a blacklist. */
    public static final int spamLimit = 250;

    public IntMap<Redirector> redirectors = new IntMap<>();

    public Distributor() {
        super(32768, 8192, new Serializer());
        addListener(new Listener());
    }

    public void run(int port) throws IOException {
        Blacklist.load(); // refresh github's ips
        Log.info("Distributor hosted on port @.", port);

        bind(port, port);
        run();
    }

    // region room management

    public String generateLink() {
        StringBuilder builder = new StringBuilder("CLaJ");
        for (int i = 0; i < 42; i++)
            builder.append(symbols[Mathf.random(symbols.length - 1)]);

        return builder.toString();
    }

    public Redirector find(String link) {
        for (Entry<Redirector> entry : redirectors)
            if (entry.value.link.equals(link) && entry.value.client == null) return entry.value;

        return null;
    }

    // endregion

    public class Listener implements NetListener {

        @Override
        public void connected(Connection connection) {
            if (Blacklist.contains(connection.getRemoteAddressTCP().getAddress().getHostAddress())) {
                connection.close(DcReason.closed);
                return;
            }

            Log.info("Connection @ received!", connection.getID());
            connection.setArbitraryData(new Ratekeeper());
        }

        @Override
        public void disconnected(Connection connection, DcReason reason) {
            Log.info("Connection @ lost: @.", connection.getID(), reason);

            var redirector = redirectors.get(connection.getID());
            if (redirector == null) return;

            redirectors.remove(redirector.host.getID());
            if (redirector.client != null) redirectors.remove(redirector.client.getID());

            // called after deletion to prevent double close message
            redirector.disconnected(connection, reason);
        }

        @Override
        public void received(Connection connection, Object object) {
            var rate = (Ratekeeper) connection.getArbitraryData();
            if (!rate.allow(3000L, spamLimit)) {
                String ip = connection.getRemoteAddressTCP().getAddress().getHostAddress();
                Log.warn("Blacklisting @ as potential DOS attack - packet spam.", ip);
                Blacklist.add(ip);

                connection.close(DcReason.closed);
                return;
            }

            if (object instanceof FrameworkMessage) return;
            if (object instanceof String link) {
                if (link.equals("new")) {
                    link = generateLink();

                    connection.sendTCP(link);
                    redirectors.put(connection.getID(), new Redirector(link, connection));
                } else {
                    var redirector = find(link);
                    if (redirector == null) {
                        connection.close(DcReason.error);
                        return;
                    }

                    redirector.client = connection;
                    redirectors.put(connection.getID(), redirector);
                    Log.info("Connection @ joined to room @.", connection.getID(), redirector.link);
                }

                return;
            }

            var redirector = redirectors.get(connection.getID());
            if (redirector != null) redirector.received(connection, object);
        }
    }
}
