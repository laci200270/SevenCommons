package de.take_weiland.mods.commons.internal.net;

import com.google.common.collect.ImmutableMap;
import cpw.mods.fml.common.LoaderState;
import de.take_weiland.mods.commons.internal.SevenCommons;
import de.take_weiland.mods.commons.net.Packet;
import de.take_weiland.mods.commons.net.PacketCodec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author diesieben07
 */
public final class PacketToChannelMap {

    private static Map<Class<? extends Packet>, PacketCodec<Packet>> channels = new ConcurrentHashMap<>();

    public static PacketCodec<Packet> getChannel(Packet packet) {
        PacketCodec<Packet> codec = channels.get(packet.getClass());
        if (codec == null) {
            throw new IllegalStateException(String.format("Cannot send unregistered Packet %s", packet.getClass().getName()));
        }
        return codec;
    }

    static synchronized void put(Class<? extends Packet> packetClass, PacketCodec<Packet> codec) {
        PacketCodec<Packet> oldCodec = channels.putIfAbsent(packetClass, codec);
        if (oldCodec != null) {
            throw new IllegalStateException(String.format("Packet %s already in use with channel %s", packetClass.getName(), oldCodec.channel()));
        }
    }

    public static synchronized void putAll(Iterable<Class<? extends Packet>> packets, PacketCodec<Packet> codec) {
        for (Class<? extends Packet> packet : packets) {
            PacketCodec<Packet> oldCodec = channels.putIfAbsent(packet, codec);
            if (oldCodec != null) {
                throw new IllegalStateException(String.format("Packet %s already in use with channel %s", packet.getName(), oldCodec.channel()));
            }
        }
    }

    private static synchronized void freeze() {
        channels = ImmutableMap.copyOf(channels);
    }

    static {
        SevenCommons.registerStateCallback(LoaderState.ModState.POSTINITIALIZED, PacketToChannelMap::freeze);
    }

    private PacketToChannelMap() {
    }

}