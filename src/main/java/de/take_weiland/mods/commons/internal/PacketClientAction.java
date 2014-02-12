package de.take_weiland.mods.commons.internal;

import cpw.mods.fml.relauncher.Side;
import de.take_weiland.mods.commons.network.DataPacket;
import de.take_weiland.mods.commons.network.PacketType;
import net.minecraft.entity.player.EntityPlayer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static de.take_weiland.mods.commons.net.Packets.readEnum;
import static de.take_weiland.mods.commons.net.Packets.writeEnum;

public class PacketClientAction extends DataPacket {

	private Action action;
	
	public PacketClientAction(Action action) {
		this.action = action;
	}

	@Override
	protected void read(EntityPlayer player, Side side, DataInputStream in) throws IOException {
		action = readEnum(in, Action.class);
	}

	@Override
	protected void write(DataOutputStream out) throws IOException {
		writeEnum(out, action);
	}

	@Override
	public boolean isValidForSide(Side side) {
		return side.isClient();
	}

	@Override
	public void execute(EntityPlayer player, Side side) {
		switch (action) {
		case RESTART_FAILURE:
			CommonsModContainer.proxy.displayRestartFailure();
			break;
		}
	}

	@Override
	public PacketType type() {
		return CommonsPackets.CLIENT_ACTION;
	}
	
	public static enum Action {
		
		RESTART_FAILURE
		
	}

}
