package com.io.github.oniroo.items;

import com.io.github.oniroo.WarOfSurvivalMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModItems {
    // REPAIR TAGS
    public static final TagKey<Item> REPAIRS_SHADOW = TagKey.of(Registries.ITEM.getKey(), Identifier.of(WarOfSurvivalMod.MOD_ID, "repairs_shadow"));

    // TOOL MATERIALS
    public static final ToolMaterial SHADOW_TOOL_MATERIAL = new ToolMaterial(
        BlockTags.INCORRECT_FOR_WOODEN_TOOL,
        455,
        5.0F,
        1.5F,
        22,
        REPAIRS_SHADOW
    );

    // ITEMS
    public static final Item SWORD_OF_SHADOW = register(
        "sword_of_shadow",
        Item::new,
        new Item.Settings().sword(SHADOW_TOOL_MATERIAL, 1f, -1f)
    );

    // ITEM GROUPS
    public static final RegistryKey<ItemGroup> CUSTOM_ITEM_GROUP_KEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(), Identifier.of(WarOfSurvivalMod.MOD_ID, "item_group"));
    public static final ItemGroup CUSTOM_ITEM_GROUP = FabricItemGroup.builder()
        .icon(() -> new ItemStack(ModItems.SWORD_OF_SHADOW))
        .displayName(Text.translatable("itemgroup.war_of_survival"))
        .build();

    /*---------------------------------*/

    // methods
    public static Item register(String name, Function<Item.Settings, Item> itemFactory, Item.Settings settings) {
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(WarOfSurvivalMod.MOD_ID, name));

        Item item = itemFactory.apply(settings.registryKey(itemKey));

        Registry.register(Registries.ITEM, itemKey, item);

        return item;
    }

    // initialize
    public static void initialize() {
        Registry.register(Registries.ITEM_GROUP, CUSTOM_ITEM_GROUP_KEY, CUSTOM_ITEM_GROUP);
        ItemGroupEvents.modifyEntriesEvent(CUSTOM_ITEM_GROUP_KEY).register(itemGroup ->
            itemGroup.add(ModItems.SWORD_OF_SHADOW)
        );
    }
}
