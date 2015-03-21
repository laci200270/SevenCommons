package de.take_weiland.mods.commons;

import de.take_weiland.mods.commons.sync.Sync;
import de.take_weiland.mods.commons.util.Sides;

import java.util.Random;

/**
 * @author diesieben07
 */
public class TestTE extends testmod_sc.SuperTE {

    @Sync
    public String test;

    private int tick;

    @Override
    public void updateEntity() {
        if (tick++ % 10 == 0) {
            if (Sides.logical(this).isServer()) {
                test = String.valueOf(new Random().nextInt(Integer.MAX_VALUE));
            } else {
                System.out.println("client val is " + test);
            }
        }
    }
}
