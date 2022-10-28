package factionsplusplus.data.codecs;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.function.Function;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.codec.Codec;
import org.jdbi.v3.core.mapper.ColumnMapper;

public final class UUIDCodec implements Codec<UUID> {
    @Override
    public ColumnMapper<UUID> getColumnMapper() {
        return (r, idx, ctx) -> { 
            ByteBuffer byteByffer = ByteBuffer.wrap(r.getBytes(idx));
            return new UUID(byteByffer.getLong(), byteByffer.getLong());
        };
    }
    @Override
    public Function<UUID, Argument> getArgumentFunction() {
        return uuid -> (idx, stmt, ctx) -> {
            ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
            byteBuffer.putLong(uuid.getMostSignificantBits());
            byteBuffer.putLong(uuid.getLeastSignificantBits());
            stmt.setBytes(idx, byteBuffer.array());
        };
    }
}
