package de.take_weiland.mods.commons.net;

import cpw.mods.fml.relauncher.Side;
import de.take_weiland.mods.commons.internal.PacketWithFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public abstract class ModPacket<TYPE extends Enum<TYPE> & SimplePacketType<TYPE>> implements SimplePacket {

	protected abstract boolean validOn(Side side);
	
	protected abstract void write(WritableDataBuf buffer);
	
	protected abstract void handle(DataBuf buffer, EntityPlayer player, Side side);
	
	protected int expectedSize() {
		return 32;
	}
	
	private SimplePacket delegate;
	private SimplePacket make() {
		SimplePacket delegate;
		if ((delegate = this.delegate) == null) {
			delegate = this.delegate = make0();
		}
		return delegate;
	}
	
	private SimplePacket make0() {
		@SuppressWarnings("unchecked")
		PacketWithFactory<TYPE> pwf = (PacketWithFactory<TYPE>) this;
		
		PacketBuilder builder = pwf._sc_getFactory().builder(pwf._sc_getType(), expectedSize());
		write(builder);
		return builder.toPacket();
	}

	@SuppressWarnings("unchecked")
	public final TYPE type() {
		return ((PacketWithFactory<TYPE>) this)._sc_getType();
	}

	@Override
	public final void sendTo(PacketTarget target) {
		make().sendTo(target);
	}

	@Override
	public final void sendToServer() {
		make().sendToServer();
	}

	@Override
	public final void sendTo(EntityPlayer player) {
		make().sendTo(player);
	}

	@Override
	public final void sendTo(Iterable<? extends EntityPlayer> players) {
		make().sendTo(players);
	}

	@Override
	public final void sendToAll() {
		make().sendToAll();
	}

	@Override
	public final void sendToAllInDimension(int dimension) {
		make().sendToAllInDimension(dimension);
	}

	@Override
	public final void sendToAllInDimension(World world) {
		make().sendToAllInDimension(world);
	}

	@Override
	public final void sendToAllNear(World world, double x, double y, double z, double radius) {
		make().sendToAllNear(world, x, y, z, radius);
	}

	@Override
	public final void sendToAllNear(int dimension, double x, double y, double z, double radius) {
		make().sendToAllNear(dimension, x, y, z, radius);
	}

	@Override
	public final void sendToAllNear(Entity entity, double radius) {
		make().sendToAllNear(entity, radius);
	}

	@Override
	public final void sendToAllNear(TileEntity te, double radius) {
		make().sendToAllNear(te, radius);
	}

	@Override
	public final void sendToAllTracking(Entity entity) {
		make().sendToAllTracking(entity);
	}

	@Override
	public final void sendToAllTracking(TileEntity te) {
		make().sendToAllTracking(te);
	}

	@Override
	public final void sendToAllAssociated(Entity e) {
		make().sendToAllAssociated(e);
	}

	@Override
	public final void sendToViewing(Container c) {
		make().sendToViewing(c);
	}
}
