package de.take_weiland.mods.commons;

import cpw.mods.fml.common.Mod;

@Mod(modid = "testmod_sc", name = "testmod_sc", version = "0.1")
//@NetworkMod()
public class testmod_sc {


	{
		try {
			System.out.println(Class.class.getDeclaredMethod("<clinit>"));
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}


}
