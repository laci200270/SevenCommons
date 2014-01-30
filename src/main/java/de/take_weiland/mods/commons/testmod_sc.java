package de.take_weiland.mods.commons;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.google.common.base.Function;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import cpw.mods.fml.common.IPlayerTracker;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import de.take_weiland.mods.commons.event.PlayerStartTrackingEvent;
import de.take_weiland.mods.commons.sync.Synced;
import de.take_weiland.mods.commons.sync.TypeSyncer;

@Mod(modid = "testmodsc", name = "testmodsc", version = "0.1")
@NetworkMod()
public class testmod_sc {

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
		NetworkRegistry.instance().registerGuiHandler(this, new IGuiHandler() {
			
			@Override
			public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
				return new TestContainer();
			}
			
			@Override
			public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
				return new TestGuiContainer((Container) getServerGuiElement(ID, player, world, x, y, z));
			}
		});
		
		GameRegistry.registerPlayerTracker(new IPlayerTracker() {
			
			@Override
			public void onPlayerRespawn(EntityPlayer player) { }
			
			@Override
			public void onPlayerLogout(EntityPlayer player) { }
			
			@Override
			public void onPlayerLogin(EntityPlayer player) {
				player.openGui(testmod_sc.this, 0, player.worldObj, 0, 0, 0);
			}
			
			@Override
			public void onPlayerChangedDimension(EntityPlayer player) { }
		});
	}
	
	@ForgeSubscribe
	public void onEntityConstruct(EntityEvent.EntityConstructing event) {
		if (event.entity instanceof EntityPlayer) {
//			event.entity.registerExtendedProperties("foo.bar", new TestExtendedProperties());
		}
	}
	
	@ForgeSubscribe
	public void onEntityTrack(PlayerStartTrackingEvent event) {
//		System.out.println("Start tracking: " + event.tracked);
	}
	
	@ForgeSubscribe
	public void onEntityTick(LivingUpdateEvent event) {
		if (event.entity instanceof EntityPlayer) {
//			if (event.entity.worldObj.isRemote) {
//				System.out.println(((TestExtendedProperties)event.entity.getExtendedProperties("foo.bar")).foobar);
//			} else {
//				((TestExtendedProperties)event.entity.getExtendedProperties("foo.bar")).foobar += ".bar";
//			}
		}
	}
	
	static class TestEntity extends Entity {

		public TestEntity(World par1World) {
			super(par1World);
		}
		
		@Synced
		private int foobar = 2;

		@Override
		protected void entityInit() {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void readEntityFromNBT(NBTTagCompound nbttagcompound) {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void writeEntityToNBT(NBTTagCompound nbttagcompound) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	@Synced
	static class TestContainer extends Container {

		@Synced(useSyncer = 0)
		String synced = "foo";
		
		@Synced(syncGroup = 2)
		int testus = -3;
		
		@Synced.SyncGroupHandler(syncGroup = 2)
		private static Function<TestContainer, Packet> group2handler;
		
		@Synced.DefineSyncer(0)
		private static TypeSyncer<String> customSyncer = new TypeSyncer<String>() {

			@Override
			public boolean equal(String now, String prev) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void write(String instance, DataOutput out)
					throws IOException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public String read(String old, DataInput in) throws IOException {
				// TODO Auto-generated method stub
				return null;
			}
		};
		
		@Override
		public boolean canInteractWith(EntityPlayer entityplayer) {
			ItemStack.class.getClass();
			int a = 5;
			switch (a) {
			case -1:
				break;
			default:
				break;
			case 0:
				break;
			case 1:
				break;
			}
			return true;
		}

		@Override
		public void detectAndSendChanges() {
			super.detectAndSendChanges();
			synced = String.valueOf(Math.random());
			testus = (int) (Math.random() * 400);
		}
		
		
	}
	
	static class TestGuiContainer extends GuiContainer {

		public TestGuiContainer(Container par1Container) {
			super(par1Container);
		}

		@Override
		protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
			drawBackground(0);
		}

		@Override
		public void updateScreen() {
			super.updateScreen();
			System.out.println(((TestContainer)inventorySlots).synced);
			System.out.println(((TestContainer)inventorySlots).testus);
		}
		
	}
	
	static class TestExtendedProperties implements IExtendedEntityProperties {

		@Synced
		private String foobar = "as";
		
		@Override
		public void saveNBTData(NBTTagCompound compound) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void loadNBTData(NBTTagCompound compound) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void init(Entity entity, World world) {
			// TODO Auto-generated method stub
			
		}
		
	}

}
