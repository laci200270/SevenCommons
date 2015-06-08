package de.take_weiland.mods.commons.net;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedBytes;
import de.take_weiland.mods.commons.util.Scheduler;
import net.minecraft.entity.player.EntityPlayer;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author diesieben07
 */
final class SimplePacketCodec implements PacketCodec<Packet> {

    private final String channel;
    private final Function<? super MCDataInput, ? extends Packet>[] constructors;
    private final ImmutableMap<Class<? extends Packet>, HandlerIDPair> handlers;

    SimplePacketCodec(String channel, Function<? super MCDataInput, ? extends Packet>[] constructors, ImmutableMap<Class<? extends Packet>, HandlerIDPair> handlers) {
        this.channel = channel;
        this.constructors = constructors;
        this.handlers = handlers;
    }

    @Override
    public byte[] encode(Packet packet) {
        MCDataOutputImpl out = new MCDataOutputImpl(packet.expectedSize() + 1);

        /**
         * We don't need to check for contains here, because this codec is only used through
         * PacketToChannelMap, so only valid packets can end up here.
         */
        out.writeByte(handlers.get(packet.getClass()).id);
        packet.writeTo(out);
        // remove this copy in 1.8 by wrapping with a ByteBuf
        // when the vanilla packet properly checks for limits on ByteBufs
        return out.toByteArray();
    }

    @Override
    public Packet decode(byte[] payload) {
        byte id = payload[0];
        Function<? super MCDataInput, ? extends Packet> cstr = constructors[UnsignedBytes.toInt(id)];
        if (cstr == null) {
            throw new ProtocolException("Unknown PacketID " + id);
        }
        return cstr.apply(new MCDataInputImpl(payload, 1, payload.length - 1));
    }

    @Override
    public void handle(Packet packet, EntityPlayer player) {
        BiConsumer<? super Packet, ? super EntityPlayer> handler = handlers.get(packet.getClass()).handler;
        (player.worldObj.isRemote ? Scheduler.client() : Scheduler.server()).execute(() -> handler.accept(packet, player));
    }

    @Override
    public String channel() {
        return channel;
    }

}
