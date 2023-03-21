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

    /** Limit for packet count sent within 3 sec that will lead to a disconnect. Note: only for clients. */
    public int spamLimit = 500;

    /** Map containing the connection id and its redirector. */
    public IntMap<Room> rooms = new IntMap<>();

    /** Map containing the connection id and its redirector. */
    public IntMap<Redirector> redirectors = new IntMap<>();

    public Distributor() {
        super(32768, 8192, new Serializer());
        addListener(new Listener());
    }

    public void run(int port) throws IOException {
        Blacklist.refresh(); // refresh github's ips
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

    public Room find(String link) {
        for (Entry<Room> entry : rooms)
            if (entry.value.link.equals(link)) return entry.value;

        return null;
    }

    public Room find(Redirector redirector) {
        for (Entry<Room> entry : rooms)
            if (entry.value.redirectors.contains(redirector)) return entry.value;

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

            var room = rooms.get(connection.getID());
            if (room != null) {
                room.close(); // disconnects all related redirectors
                rooms.remove(connection.getID());
                return;
            }

            var redirector = redirectors.get(connection.getID());
            if (redirector == null) return;

            redirectors.remove(redirector.host.getID());
            if (redirector.client != null) redirectors.remove(redirector.client.getID());

            // called after deletion to prevent double close
            redirector.disconnected(connection, reason);

            room = find(redirector);
            if (room != null) room.redirectors.remove(redirector);
        }

        @Override
        public void received(Connection connection, Object object) {
            var rate = (Ratekeeper) connection.getArbitraryData();
            if (!rate.allow(3000L, spamLimit)) {
                rate.occurences = -spamLimit; // reset to prevent message spam

                var redirector = redirectors.get(connection.getID());
                if (redirector != null && connection == redirector.host) {
                    Log.warn("Connection @ spammed with packets but not disconnected due to being a host.", connection.getID());
                    return; // host can spam packets when killing core and etc.
                }

                Log.warn("Connection @ disconnected due to packet spam.", connection.getID());
                if (redirector != null) {
                    var room = find(redirector);
                    if (room != null) {
                        room.sendMessage("[scarlet]\u26A0[] Connection closed due to packet spam.");
                        room.redirectors.remove(redirector);
                    }
                }

                connection.close(DcReason.closed);
                return;
            }

            if (object instanceof FrameworkMessage) return;
            if (object instanceof String link) {
                if (link.equals("new")) {
                    link = generateLink();

                    connection.sendTCP(link);
                    rooms.put(connection.getID(), new Room(link, connection));

                    Log.info("Connection @ created a room @.", connection.getID(), link);
                } else if (link.startsWith("host")) {
                    var room = find(link.substring(4));
                    if (room == null) {
                        connection.close(DcReason.error);
                        return;
                    }

                    var redirector = new Redirector(connection);
                    room.redirectors.add(redirector);
                    redirectors.put(connection.getID(), redirector);

                    Log.info("Connection @ hosted a redirector in room @.", connection.getID(), room.link);
                } else if (link.startsWith("join")) {
                    var room = find(link.substring(4));
                    if (room == null) {
                        connection.close(DcReason.error);
                        return;
                    }

                    var redirector = room.redirectors.find(r -> r.client == null);
                    if (redirector == null) {
                        connection.close(DcReason.error); // no empty redirectors
                        return;
                    }

                    redirector.client = connection;
                    redirectors.put(connection.getID(), redirector);
                    room.sendMessage("new"); // ask to create a new redirector for the future

                    Log.info("Connection @ joined to room @.", connection.getID(), room.link);
                }

                return;
            }

            var redirector = redirectors.get(connection.getID());
            if (redirector != null) redirector.received(connection, object);
        }
    }
}
