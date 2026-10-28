package org.pingg.uncrafter_2;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue UNCRAFTER_XP = BUILDER
            .comment("Experience levels one uncraft costs, in every Uncrafter. 0 = free.")
            .defineInRange("uncrafterXpLevels", 0, 0, 100);

    public static final ModConfigSpec.IntValue LOSS_PERCENT = BUILDER
            .comment("Chance in percent that a taken ingredient is lost. 0 = never.")
            .defineInRange("ingredientLossPercent", 0, 0, 100);

    public static final ModConfigSpec.BooleanValue ALLOW_DAMAGED = BUILDER
            .comment("Allow uncrafting damaged tools and armor. The ingredient count shrinks with the remaining durability.")
            .define("allowDamagedItems", true);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST = BUILDER
            .comment("Item ids that can never be uncrafted, for example [\"minecraft:tnt\"].")
            .defineListAllowEmpty("blacklist", List.of(), () -> "minecraft:air",
                    o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_RECIPE_TYPES = BUILDER
            .comment("Extra recipe type ids whose recipes can be uncrafted, for example [\"somemod:assembling\"].",
                    "Crafting-table recipes of every mod already work.")
            .defineListAllowEmpty("extraRecipeTypes", List.of(), () -> "minecraft:crafting",
                    o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALLOWED_COMPONENTS = BUILDER
            .comment("Data component ids that do NOT stop an item from being uncrafted (they are simply lost).",
                    "Damage and repair cost are always allowed. Any other component blocks uncrafting by default,",
                    "because components can hold items or energy. Example: [\"minecraft:custom_name\"].")
            .defineListAllowEmpty("allowedComponents", List.of(), () -> "minecraft:custom_name",
                    o -> o instanceof String s && ResourceLocation.tryParse(s) != null);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
