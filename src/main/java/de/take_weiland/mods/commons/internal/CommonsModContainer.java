package de.take_weiland.mods.commons.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import cpw.mods.fml.client.FMLFileResourcePack;
import cpw.mods.fml.client.FMLFolderResourcePack;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import de.take_weiland.mods.commons.config.ConfigInjector;
import de.take_weiland.mods.commons.config.GetProperty;
import de.take_weiland.mods.commons.internal.updater.CommandUpdates;
import de.take_weiland.mods.commons.internal.updater.UpdateController;
import de.take_weiland.mods.commons.internal.updater.UpdateControllerLocal;
import de.take_weiland.mods.commons.net.Network;
import de.take_weiland.mods.commons.net.PacketFactory;
import de.take_weiland.mods.commons.network.PacketTransport;
import de.take_weiland.mods.commons.network.PacketTransports;
import net.minecraftforge.common.Configuration;

import java.io.File;

public final class CommonsModContainer extends DummyModContainer {

	public static SevenCommonsProxy proxy;
	public static CommonsModContainer instance;
	public static UpdateController updateController;
	public static PacketTransport packetTransport;
    public static PacketFactory<SCPacket.Type> packetFactory;
	
	@GetProperty(comment = "Set to false to disable the auto-updating feature of SevenCommons")
	public static boolean updaterEnabled = true;
	
	@GetProperty(comment = "The name of the command used to access the update feature on a server")
	public static String updateCommand = "modupdates";
	
	public CommonsModContainer() {
		super(new ModMetadata());
		ModMetadata meta = getMetadata();
		meta.name = "SevenCommons";
		meta.modId = "sevencommons";
		meta.authorList = ImmutableList.of("diesieben07");
		meta.version = SevenCommons.VERSION;
		
		meta.description = "Provides various Utilities for other mods.";
		
		meta.autogenerated = false;
		
		instance = this;
	}

	@Override
	public boolean registerBus(EventBus bus, LoadController controller) {
		bus.register(this);
		return true;
	}
	
	@Subscribe
	public void preInit(FMLPreInitializationEvent event) {
		try { // my version of @SidedProxy
			if (event.getSide().isServer()) {
				proxy = (SevenCommonsProxy) Class.forName("de.take_weiland.mods.commons.internal.ServerProxy").newInstance();
			} else {
				proxy = (SevenCommonsProxy) Class.forName("de.take_weiland.mods.commons.internal.client.ClientProxy").newInstance();
			}
		} catch (Throwable t) {
			// nope
			t.printStackTrace();
		}
		
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		ConfigInjector.inject(config, getClass());

		packetFactory = Network.simplePacketHandler("SevenCommons", SCPacket.Type.class);
		packetTransport = PacketTransports.withPacket250("SevenCommons_OLD", CommonsPackets.class);
		
		proxy.preInit(event);
		
		GameRegistry.registerPlayerTracker(new SCEventHandler());
	}	

	@Subscribe
	public void postInit(FMLPostInitializationEvent event) {
		if (updaterEnabled) {
			updateController = new UpdateControllerLocal();
			updateController.searchForUpdates();
		}
	}
	
	@Subscribe
	public void serverStarting(FMLServerStartingEvent event) {
		if (event.getSide().isServer()) {
			event.registerServerCommand(new CommandUpdates(updateCommand));
		}
	}
	
	@Override
	public File getSource() {
		return SevenCommons.source;
	}

	@Override
	public Class<?> getCustomResourcePackClass() {
		return getSource().isDirectory() ? FMLFolderResourcePack.class : FMLFileResourcePack.class;
	}

}
