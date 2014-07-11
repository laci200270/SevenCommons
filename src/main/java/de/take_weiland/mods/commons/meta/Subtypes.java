package de.take_weiland.mods.commons.meta;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * @author diesieben07
 */
public final class Subtypes {

    @SuppressWarnings("unchecked")
    public static <TYPE extends Subtype, ITEM extends Item & HasSubtypes<TYPE>> TYPE getType(ItemStack stack) {
        return getType((ITEM) stack.getItem(), stack.getItemDamage());
    }

    public static <TYPE extends Subtype, ITEM extends Item & HasSubtypes<TYPE>> TYPE getType(ITEM item, ItemStack stack) {
        return getType(item, stack.getItemDamage());
    }

    public static <TYPE extends Subtype, ITEM extends Item & HasSubtypes<TYPE>> TYPE getType(ITEM item, int meta) {
        return item.subtypeProperty().value(meta);
    }

    public static <TYPE extends Subtype, ITEM extends Item & HasSubtypes<TYPE>> String fullName(ITEM item, TYPE type) {
        return item.getUnlocalizedName() + "." + type.subtypeName();
    }

    public static <TYPE extends Subtype, ITEM extends Item & HasSubtypes<TYPE>> String fullName(ITEM item, ItemStack stack) {
        return fullName(item, item.subtypeProperty().value(stack));
    }

    public static <TYPE extends Subtype, ITEM extends Item & HasSubtypes<TYPE>> String fullName(ITEM item, int meta) {
        return fullName(item, item.subtypeProperty().value(meta));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <TYPE extends Subtype, ITEM extends Item & HasSubtypes<TYPE>> void getSubItemsImpl(ITEM item, List list) {
        MetadataProperty<TYPE> property = item.subtypeProperty();

        for (TYPE subtype : property.values()) {
            list.add(new ItemStack(item, 1, property.toMeta(subtype, 0)));
        }
    }

    public static <TYPE extends Subtype, ITEM extends Item & HasSubtypes<TYPE>> List<ItemStack> getSubItems(ITEM item) {
        MetadataProperty<TYPE> property = item.subtypeProperty();
        TYPE[] types = property.values();
        int len = types.length;
        ItemStack[] stacks = new ItemStack[len];

        for (int i = 0; i < len; ++i) {
            stacks[i] = new ItemStack(item, 1, property.toMeta(types[i], 0));
        }

        return Arrays.asList(stacks);
    }

    private Subtypes() { }

}
