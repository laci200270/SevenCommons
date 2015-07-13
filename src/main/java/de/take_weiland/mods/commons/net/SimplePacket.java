package de.take_weiland.mods.commons.net;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import de.take_weiland.mods.commons.internal.SCReflector;
import de.take_weiland.mods.commons.internal.transformers.net.SimplePacketWithResponseTransformer;
import de.take_weiland.mods.commons.util.Entities;
import de.take_weiland.mods.commons.util.Players;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * <p>A packet that can be sent to various targets.</p>
 * <p>When implementing this interface, at least {@link #sendToServer()} and {@link #sendTo(EntityPlayerMP)} must be implemented. If multiple players can be targeted efficiently
 * {@link #sendTo(Iterable, Predicate)} should be overridden as well.</p>
 */
public interface SimplePacket {

    /**
     * <p>Send this packet to the server. This method must only be called from the client thread.</p>
     */
    void sendToServer();

    /**
     * <p>Send this packet to the given player.</p>
     *
     * @param player the player
     */
    void sendTo(EntityPlayerMP player);

    /**
     * <p>Send this packet to the given player. This method must only be called for server-side players.</p>
     * @param player the player
     */
    default void sendTo(EntityPlayer player) {
        sendTo(Players.checkNotClient(player));
    }

    /**
     * <p>Send this packet to the given players. This method must only be called for server-side players.</p>
     * @param players the players
     */
    default void sendTo(EntityPlayer... players) {
        sendTo(Arrays.asList(players), null);
    }

    /**
     * <p>Send this packet to the given players. This method must only be called for server-side players.</p>
     * @param players the players
     */
    default void sendTo(Iterable<? extends EntityPlayer> players) {
        sendTo(players, null);
    }

    /**
     * <p>Send this packet to the players in the collection matching the filter or to all players in the collection if the filter is null. This method must only be called for server-side players.</p>
     * @param players the players
     * @param filter the filter, may be null
     */
    default void sendTo(Iterable<? extends EntityPlayer> players, Predicate<? super EntityPlayerMP> filter) {
        for (EntityPlayer player : players) {
            EntityPlayerMP mp = Players.checkNotClient(player);
            if (filter == null || filter.test(mp)) {
                sendTo(mp);
            }
        }
    }

    /**
     * <p>Send this packet to the players in the given world matching the filter or to all players in the given world if the filter is null. This method must only be called for server-side worlds.</p>
     * @param world the world
     * @param filter the filter, may be null
     */
    default void sendTo(World world, Predicate<? super EntityPlayer> filter) {
        sendTo(Players.allIn(world), filter);
    }

    /**
     * <p>Send this packet to all players matching the filter or to all players if the filter is null.</p>
     * @param filter the filter, may be null
     */
    default void sendTo(Predicate<? super EntityPlayerMP> filter) {
        sendTo(Players.getAll(), filter);
    }

    /**
     * <p>Send this packet to all players.</p>
     */
    default void sendToAll() {
        sendTo(Players.getAll());
    }

    /**
     * <p>Send this packet to all players in the given world. This method must only be called for server-side worlds.</p>
     * @param world the world
     */
    default void sendToAllIn(World world) {
        sendTo(Players.allIn(world));
    }

    /**
     * <p>Send this packet to all players that are within the given radius around the given point. This method must only be called for server-side worlds.</p>
     * @param world the world
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param radius the radius
     */
    default void sendToAllNear(World world, double x, double y, double z, double radius) {
        sendTo(Players.allIn(world), player -> player.getDistanceSq(x, y, z) <= radius);
    }

    /**
     * <p>Send this packet to all players that are within the given radius around the given entity. This method must only be called for server-side entities.</p>
     * <p>Usually {@link #sendToAllTracking(Entity)} or {@link #sendToAllAssociated(Entity)} are more sensible choices.</p>
     * @param entity the entity
     * @param radius the radius
     */
    default void sendToAllNear(Entity entity, double radius) {
        sendToAllNear(entity.worldObj, entity.posX, entity.posY, entity.posZ, radius);
    }

    /**
     * <p>Send this packet to all players that are within the given radius around the given TileEntity. This method must only be called for server-side TileEntities.</p>
     * <p>Usually {@link #sendToAllTracking(TileEntity)} is a more sensible choice.</p>
     * @param te the TileEntity
     * @param radius the radius
     */
    default void sendToAllNear(TileEntity te, double radius) {
        sendToAllNear(te.getWorld(), te.xCoord, te.yCoord, te.zCoord, radius);
    }

    /**
     * <p>Send this packet to all players that are tracking the given entity. This method must only be called for server-side entities.</p>
     * <p><i>Note:</i> Players do not track themselves. See {@link #sendToAllAssociated(Entity)}.</p>
     * @param entity the entity
     */
    default void sendToAllTracking(Entity entity) {
        sendTo(Entities.getTrackingPlayers(entity));
    }

    /**
     * <p>Send this packet to all players that are tracking the given entity. Additionally if the entity is a player also sends the packet to the player itself. This method must only be called for server-side entities.</p>
     * @param entity the entity
     */
    default void sendToAllAssociated(Entity entity) {
        Iterable<EntityPlayerMP> players = Entities.getTrackingPlayers(entity);
        if (entity instanceof EntityPlayerMP) {
            Iterable<EntityPlayerMP> playersFinal = players;
            EntityPlayerMP mp = (EntityPlayerMP) entity;
            players = () -> new OnePlusIterator<>(playersFinal.iterator(), mp);
        }
        sendTo(players);
    }

    /**
     * <p>Send this packet to all players that are tracking the given TileEntity. A player is tracking a TileEntity when the TileEntity is loaded for them ("within view-distance").
     * This method must only be called for server-side TileEntities.</p>
     * @param te the TileEntity
     */
    default void sendToAllTracking(TileEntity te) {
        sendToAllTrackingChunk(te.getWorld(), te.xCoord >> 4, te.zCoord >> 4);
    }

    /**
     * <p>Send this packet to all players that are tracking the given chunk. A player is tracking a chunk when the chunk is loaded for them ("within view-distance").
     * This method must only be called for server-side chunk.</p>
     * @param chunk the Chunk
     */
    default void sendToAllTracking(Chunk chunk) {
        sendToAllTrackingChunk(chunk.worldObj, chunk.xPosition, chunk.zPosition);
    }

    /**
     * <p>Send this packet to all players that are tracking the given chunk. A player is tracking a chunk when the chunk is loaded for them ("within view-distance").
     * This method must only be called for server-side worlds.</p>
     * @param world the world
     * @param chunkX the x coordinate of the chunk
     * @param chunkZ the z coordinate of the chunk
     */
    default void sendToAllTrackingChunk(World world, int chunkX, int chunkZ) {
        sendTo(Players.getTrackingChunk(world, chunkX, chunkZ));
    }

    /**
     * <p>Send this packet to all players that are looking at the given container. This will most likely never be more than one.</p>
     * @param c the container
     */
    default void sendToViewing(Container c) {
        // the filter makes sure it only contains EntityPlayer's
        //noinspection unchecked
        sendTo(Iterables.filter(SCReflector.instance.getCrafters(c), EntityPlayerMP.class));
    }


    /**
     * <p>A version of {@link SimplePacket} with a Response.</p>
     * <p>The methods in this class all return either a {@link CompletableFuture CompletableFuture&lt;R&gt;} or a {@code Map&lt;EntityPlayer, CompletableFuture&lt;R&gt;&gt;}</p>
     */
    interface WithResponse<R> {

        /**
         * <p>Return a SimplePacket version of this packet, where the response is ignored. Use this method whenever a SimplePacket is expected.</p>
         * @return a SimplePacket version
         */
        default SimplePacket discardResponse() {
            /**
             * see {@link SimplePacketWithResponseTransformer}
             */
            return (SimplePacket) this;
        }

        /**
         * <p>Send this packet to the server. This method must only be called from the client thread.</p>
         * @return a {@code CompletableFuture} representing the response
         */
        CompletableFuture<R> sendToServer();

        /**
         * <p>Send this packet to the given player.</p>
         * @param player the player
         * @return a {@code CompletableFuture} representing the response
         */
        CompletableFuture<R> sendTo(EntityPlayerMP player);

        /**
         * <p>Send this packet to the given player. This method must only be called for server-side players.</p>
         * @param player the player
         * @return a {@code CompletableFuture} representing the response
         */
        default CompletableFuture<R> sendTo(EntityPlayer player) {
            return sendTo(Players.checkNotClient(player));
        }

        /**
         * <p>Send this packet to the given players. This method must only be called for server-side players.</p>
         *
         * @param players the players
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendTo(EntityPlayer... players) {
            return sendTo(Arrays.asList(players), null);
        }

        /**
         * <p>Send this packet to the given players. This method must only be called for server-side players.</p>
         *
         * @param players the players
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendTo(Iterable<? extends EntityPlayer> players) {
            return sendTo(players, null);
        }

        /**
         * <p>Send this packet to the players in the collection matching the filter or to all players in the collection if the filter is null. This method must only be called for server-side players.</p>
         * @param players the players
         * @param filter the filter, may be null
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendTo(Iterable<? extends EntityPlayer> players, Predicate<? super EntityPlayerMP> filter) {
            HashMap<EntityPlayerMP, CompletableFuture<R>> map = players instanceof Collection ? Maps.newHashMapWithExpectedSize(((Collection<?>) players).size()) : new HashMap<>();
            Function<EntityPlayerMP, CompletableFuture<R>> sender = this::sendTo;

            for (EntityPlayer player : players) {
                EntityPlayerMP mp = Players.checkNotClient(player);
                if (filter == null || filter.test(mp)) {
                    map.computeIfAbsent(mp, sender);
                }
            }
            return Collections.unmodifiableMap(map);
        }

        /**
         * <p>Send this packet to the players in the given world matching the filter or to all players in the given world if the filter is null. This method must only be called for server-side worlds.</p>
         *
         * @param world  the world
         * @param filter the filter, may be null
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendTo(World world, Predicate<? super EntityPlayerMP> filter) {
            return sendTo(Players.allIn(world), filter);
        }

        /**
         * <p>Send this packet to all players matching the filter or to all players if the filter is null.</p>
         * @param filter the filter, may be null
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendTo(Predicate<? super EntityPlayerMP> filter) {
            return sendTo(Players.getAll(), filter);
        }

        /**
         * <p>Send this packet to all players.</p>
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendToAll() {
            return sendTo(Players.getAll(), null);
        }

        /**
         * <p>Send this packet to all players in the given world. This method must only be called for server-side worlds.</p>
         * @param world the world
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendToAllIn(World world) {
            return sendTo(Players.allIn(world), null);
        }

        /**
         * <p>Send this packet to all players that are within the given radius around the given point. This method must only be called for server-side worlds.</p>
         *
         * @param world  the world
         * @param x      the x coordinate
         * @param y      the y coordinate
         * @param z      the z coordinate
         * @param radius the radius
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendToAllNear(World world, double x, double y, double z, double radius) {
            double rSq = radius * radius;
            return sendTo(Players.allIn(world), p -> p.getDistanceSq(x, y, z) <= rSq);
        }

        /**
         * <p>Send this packet to all players that are within the given radius around the given entity. This method must only be called for server-side entities.</p>
         * <p>Usually {@link #sendToAllTracking(Entity)} or {@link #sendToAllAssociated(Entity)} are more sensible choices.</p>
         * @param entity the entity
         * @param radius the radius
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendToAllNear(Entity entity, double radius) {
            return sendToAllNear(entity.worldObj, entity.posX, entity.posY, entity.posZ, radius);
        }

        /**
         * <p>Send this packet to all players that are within the given radius around the given TileEntity. This method must only be called for server-side TileEntities.</p>
         * <p>Usually {@link #sendToAllTracking(TileEntity)} is a more sensible choice.</p>
         * @param te the TileEntity
         * @param radius the radius
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendToAllNear(TileEntity te, double radius) {
            return sendToAllNear(te.getWorld(), te.xCoord, te.yCoord, te.zCoord, radius);
        }

        /**
         * <p>Send this packet to all players that are tracking the given entity. This method must only be called for server-side entities.</p>
         * <p><i>Note:</i> Players do not track themselves. See {@link #sendToAllAssociated(Entity)}.</p>
         *
         * @param entity the entity
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendToAllTracking(Entity entity) {
            return sendTo(Entities.getTrackingPlayers(entity), null);
        }

        /**
         * <p>Send this packet to all players that are tracking the given entity. Additionally if the entity is a player also sends the packet to the player itself. This method must only be called for server-side entities.</p>
         * @param entity the entity
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendToAllAssociated(Entity entity) {
            Iterable<EntityPlayerMP> players = Entities.getTrackingPlayers(entity);
            if (entity instanceof EntityPlayer) {
                Iterable<EntityPlayerMP> playersFinal = players;
                EntityPlayerMP mp = Players.checkNotClient((EntityPlayer) entity);
                players = () -> new OnePlusIterator<>(playersFinal.iterator(), mp);
            }
            return sendTo(players, null);
        }

        /**
         * <p>Send this packet to all players that are tracking the given TileEntity. A player is tracking a TileEntity when the TileEntity is loaded for them ("within view-distance").
         * This method must only be called for server-side TileEntities.</p>
         * @param te the TileEntity
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendToAllTracking(TileEntity te) {
            return sendToAllTrackingChunk(te.getWorld(), te.xCoord >> 4, te.zCoord >> 4);
        }

        /**
         * <p>Send this packet to all players that are tracking the given chunk. A player is tracking a chunk when the chunk is loaded for them ("within view-distance").
         * This method must only be called for server-side chunk.</p>
         * @param chunk the Chunk
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendToAllTracking(Chunk chunk) {
            return sendToAllTrackingChunk(chunk.worldObj, chunk.xPosition, chunk.zPosition);
        }

        /**
         * <p>Send this packet to all players that are tracking the given chunk. A player is tracking a chunk when the chunk is loaded for them ("within view-distance").
         * This method must only be called for server-side worlds.</p>
         * @param world the world
         * @param chunkX the x coordinate of the chunk
         * @param chunkZ the z coordinate of the chunk
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendToAllTrackingChunk(World world, int chunkX, int chunkZ) {
            return sendTo(Players.getTrackingChunk(world, chunkX, chunkZ), null);
        }

        /**
         * <p>Send this packet to all players that are looking at the given container. This will most likely never be more than one.</p>
         * @param c the container
         * @return a {@code CompletableFuture} for each player representing their response
         */
        default Map<EntityPlayer, CompletableFuture<R>> sendToViewing(Container c) {
            return sendTo(Iterables.filter(SCReflector.instance.getCrafters(c), EntityPlayerMP.class), null);
        }
    }

}
