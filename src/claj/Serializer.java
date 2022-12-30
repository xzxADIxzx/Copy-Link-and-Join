package claj;

import mindustry.net.ArcNetProvider.PacketSerializer;

import java.nio.ByteBuffer;

public class Serializer extends PacketSerializer {

    public static final byte linkID = -3;

    public ByteBuffer last = ByteBuffer.allocate(8192);

    @Override
    public void write(ByteBuffer buffer, Object object) {
        if (object instanceof String link) {
            buffer.put(linkID);
            writeString(buffer, link);
        } else
            super.write(buffer, object);
    }

    @Override
    public Object read(ByteBuffer buffer) {
        int lastPosition = buffer.position();
        byte id = buffer.get();

        if (id == -2) return super.readFramework(buffer);
        if (id == linkID) return readString(buffer); // link

        last.clear();
        last.put(buffer.position(lastPosition));
        last.limit(buffer.limit() - lastPosition);

        return last.position(0);
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
