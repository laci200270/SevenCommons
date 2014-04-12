package de.take_weiland.mods.commons.net;

import cpw.mods.fml.common.network.PacketDispatcher;
import de.take_weiland.mods.commons.internal.ASMHooks;
import de.take_weiland.mods.commons.util.JavaUtils;
import de.take_weiland.mods.commons.util.MiscUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author diesieben07
 */
class Packet250Fake<TYPE extends Enum<TYPE>> extends Packet250CustomPayload implements SimplePacket {

	private static final int MAX_SINGLE_SIZE = 32766; // bug in Packet250CP doesn't allow 32767
	private final PacketBufferImpl<TYPE> buf;
	private final FMLPacketHandlerImpl<TYPE> fmlPh;
	private final int id;

	static {
		// we need to inject our fake packet class into the map so that getPacketId still works (can't override it)
		MiscUtil.getReflector().getClassToIdMap(null).put(Packet250Fake.class, Integer.valueOf(250));
	}

	Packet250Fake(PacketBufferImpl<TYPE> buf, FMLPacketHandlerImpl<TYPE> fmlPh, int id) {
		this.buf = buf;
		this.fmlPh = fmlPh;
		this.id = id;
	}

	@Override
	public void writePacketData(DataOutput out) {
		try {
			writeString(fmlPh.channel, out);
			if ((buf.id & fmlPh.expectsResponseFlag) != 0) {
				ASMHooks.writeVarShort(out, buf.actualLen + fmlPh.idSize + 4);
				fmlPh.writePacketId(out, id);

				int transId = fmlPh.nextTransferId();
				out.writeInt(transId);
				fmlPh.responseHandlers().put(transId, buf.responseHandler);
				System.out.println("On send: " + fmlPh.responseHandlers());
			} else if (buf.id == fmlPh.responseId) {
				ASMHooks.writeVarShort(out, buf.actualLen + fmlPh.idSize + 4);
				fmlPh.writePacketId(out, id);

				out.writeInt(buf.transferId);
			} else {
				ASMHooks.writeVarShort(out, buf.actualLen + fmlPh.idSize);
				fmlPh.writePacketId(out, id);
			}
			out.write(buf.buf, 0, buf.actualLen);
		} catch (IOException e) {
			throw JavaUtils.throwUnchecked(e); // weird bug, can't declare IOException for some reason
		}
	}

	@Override
	public void processPacket(NetHandler netHandler) {
		fmlPh.handle0(buf, id, netHandler.getPlayer());
	}

	@Override
	public int getPacketSize() {
		// channel & first part length
		int result = 2 + fmlPh.channel.length() * 2 + 2;
		if ((buf.id & fmlPh.expectsResponseFlag) != 0 || buf.id == fmlPh.responseId) {
			int len = buf.actualLen + fmlPh.idSize + 4;
			result += ASMHooks.additionalPacketSize(len);
			result += 4;
		} else {
			int len = buf.actualLen + fmlPh.idSize;
			result += ASMHooks.additionalPacketSize(len);
		}
		result += buf.actualLen;
		return result;
	}

	@Override
	public final void readPacketData(DataInput in) {
		// we are never read!
		throw new AssertionError("Something went horribly wrong here!");
	}

	// SimplePacket
	@Override
	public SimplePacket sendTo(PacketTarget target) {
		target.send(this);
		return this;
	}

	@Override
	public SimplePacket sendToServer() {
		PacketDispatcher.sendPacketToServer(this);
		return this;
	}

	@Override
	public SimplePacket sendTo(EntityPlayer player) {
		Packets.sendPacketToPlayer(this, player);
		return this;
	}

	@Override
	public SimplePacket sendTo(Iterable<? extends EntityPlayer> players) {
		Packets.sendPacketToPlayers(this, players);
		return this;
	}

	@Override
	public SimplePacket sendToAll() {
		PacketDispatcher.sendPacketToAllPlayers(this);
		return this;
	}

	@Override
	public SimplePacket sendToAllInDimension(int dimension) {
		PacketDispatcher.sendPacketToAllInDimension(this, dimension);
		return this;
	}

	@Override
	public SimplePacket sendToAllInDimension(World world) {
		PacketDispatcher.sendPacketToAllInDimension(this, world.provider.dimensionId);
		return this;
	}

	@Override
	public SimplePacket sendToAllNear(World world, double x, double y, double z, double radius) {
		PacketDispatcher.sendPacketToAllAround(x, y, z, radius, world.provider.dimensionId, this);
		return this;
	}

	@Override
	public SimplePacket sendToAllNear(int dimension, double x, double y, double z, double radius) {
		PacketDispatcher.sendPacketToAllAround(x, y, z, radius, dimension, this);
		return this;
	}

	@Override
	public SimplePacket sendToAllNear(Entity entity, double radius) {
		PacketDispatcher.sendPacketToAllAround(entity.posX, entity.posY, entity.posZ, radius, entity.worldObj.provider.dimensionId, this);

		return this;
	}

	@Override
	public SimplePacket sendToAllNear(TileEntity te, double radius) {
		PacketDispatcher.sendPacketToAllAround(te.xCoord, te.yCoord, te.zCoord, radius, te.worldObj.provider.dimensionId, this);
		return this;
	}

	@Override
	public SimplePacket sendToAllTracking(Entity entity) {
		Packets.sendPacketToAllTracking(this, entity);
		return this;
	}

	@Override
	public SimplePacket sendToAllTracking(TileEntity te) {
		Packets.sendPacketToAllTracking(this, te);
		return this;
	}

	@Override
	public SimplePacket sendToAllAssociated(Entity e) {
		Packets.sendPacketToAllAssociated(this, e);
		return this;
	}

	@Override
	public SimplePacket sendToViewing(Container c) {
		Packets.sendPacketToViewing(this, c);
		return this;
	}
}