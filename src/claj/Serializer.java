package claj;

import mindustry.net.ArcNetProvider.PacketSerializer;

import java.nio.ByteBuffer;

public class Serializer extends PacketSerializer {

    public ByteBuffer last = ByteBuffer.allocate(8192);

    @Override
    public Object read(ByteBuffer buffer) {
        int lastPosition = buffer.position();
        byte id = buffer.get();

        if (id == -2) return super.readFramework(buffer);

        last.clear();
        last.put(buffer.position(lastPosition));
        last.limit(buffer.limit() - lastPosition);

        return last.position(0);
    }
}
