package de.take_weiland.mods.commons;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import de.take_weiland.mods.commons.asm.ASMUtils;

@Mod(modid = "testmod_sc_", name = "testmod_sc_", version = "0.1")
//@NetworkMod()
public class testmod_sc_ {

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) throws Exception {
		FMLInterModComms.sendMessage("sevencommons", "setUpdateUrl", "http://www.take-weiland.de/test.txt");

		System.out.println(ASMUtils.getClassInfo(TstLit.class).interfaces());
		System.exit(0);

	}

	private static class TstLit implements SubIface {

	}

	private static interface TestInf {



	}

	private static interface SubIface extends TestInf {



	}

}
