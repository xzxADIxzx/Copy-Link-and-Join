package claj;

import arc.net.FrameworkMessage;
import arc.net.FrameworkMessage.*;
import arc.net.NetSerializer;

import java.nio.ByteBuffer;

public class Serializer implements NetSerializer {

    public static final byte frameworkID = -2, linkID = -3;
    public ByteBuffer last = ByteBuffer.allocate(8192);

    @Override
    public void write(ByteBuffer buffer, Object object) {
        if (object instanceof ByteBuffer raw) {
            buffer.put(raw);
        } else if (object instanceof FrameworkMessage message) {
            buffer.put(frameworkID);
            writeFramework(buffer, message);
        } else if (object instanceof String link) {
            buffer.put(linkID);
            writeString(buffer, link);
        }
    }

    @Override
    public Object read(ByteBuffer buffer) {
        int lastPosition = buffer.position();
        byte id = buffer.get();

        if (id == frameworkID) return readFramework(buffer);
        if (id == linkID) return readString(buffer);

        last.clear();
        last.put(buffer.position(lastPosition));
        last.limit(buffer.limit() - lastPosition);

        return last.position(0);
    }

    public void writeFramework(ByteBuffer buffer, FrameworkMessage message) {
        if (message instanceof Ping p)
            buffer.put((byte) 0).putInt(p.id).put(p.isReply ? (byte) 1 : 0);
        else if (message instanceof DiscoverHost)
            buffer.put((byte) 1);
        else if (message instanceof KeepAlive)
            buffer.put((byte) 2);
        else if (message instanceof RegisterUDP p)
            buffer.put((byte) 3).putInt(p.connectionID);
        else if (message instanceof RegisterTCP p)
            buffer.put((byte) 4).putInt(p.connectionID);
    }

    public FrameworkMessage readFramework(ByteBuffer buffer) {
        byte id = buffer.get();

        if (id == 0)
            return new Ping() {{
                id = buffer.getInt();
                isReply = buffer.get() == 1;
            }};
        else if (id == 1) return FrameworkMessage.discoverHost;
        else if (id == 2) return FrameworkMessage.keepAlive;
        else if (id == 3)
            return new RegisterUDP() {{
                connectionID = buffer.getInt();
            }};
        else if (id == 4)
            return new RegisterTCP() {{
                connectionID = buffer.getInt();
            }};

        throw new RuntimeException("Unknown framework message!");
    }

    public static void writeString(ByteBuffer buffer, String message) {
        buffer.putInt(message.length());
        for (char chara : message.toCharArray())
            buffer.putChar(chara);
    }

    public static String readString(ByteBuffer buffer) {
        int length = buffer.getInt();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++)
            builder.append(buffer.getChar());

        return builder.toString();
    }
}
