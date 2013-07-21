package de.take_weiland.mods.commons.event;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraftforge.event.Cancelable;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * fired when the breeding AI spawns the baby and experience
 * @author diesieben07
 *
 */
@Cancelable
public class LivingBreedEvent extends LivingEvent {

	public final EntityAnimal animal;
	public final EntityAnimal mate;
	public final EntityAgeable child;
	public int xp;
	
	public LivingBreedEvent(EntityAnimal animal, EntityAnimal mate, EntityAgeable child) {
		super(animal);
		this.animal = animal;
		this.mate = mate;
		this.child = child;
	}
}
