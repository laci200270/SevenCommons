package de.take_weiland.mods.commons.internal.updater;

import de.take_weiland.mods.commons.internal.exclude.SCModContainer;
import de.take_weiland.mods.commons.internal.PacketViewUpdates;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class CommandUpdates extends CommandBase {

	private final String command;
	
	public CommandUpdates(String command) {
		this.command = command;
	}
	
	@Override
	public String getCommandName() {
		return command;
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/" + command;
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		if (!(sender instanceof EntityPlayerMP)) {
			throw new CommandException("sevencommons.updates.noplayer");
		} else if (!SCModContainer.updaterEnabled) {
			throw new CommandException("sevencommons.updates.disabled");
		} else {
			EntityPlayer player = (EntityPlayer) sender;
			new PacketViewUpdates(SCModContainer.updateController).sendTo(player);
			SCModContainer.updateController.registerListener((PlayerUpdateInformation) player.getExtendedProperties(PlayerUpdateInformation.IDENTIFIER));
		}
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 4;
	}

    @Override
    public int compareTo(Object iCommand) {
        return 0;
    }

}
