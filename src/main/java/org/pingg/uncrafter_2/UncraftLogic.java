package org.pingg.uncrafter_2;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class UncraftLogic {
    private UncraftLogic() {}

    public enum Reason { NONE, NOT_PLAIN, DAMAGED, BLACKLISTED, NO_RECIPE, NEED_MORE, BAD_INGREDIENT, REMAINDER, LOOP }

    public static final class Trace {
        private Reason reason = Reason.NO_RECIPE;
        private int needMin;

        void input(Reason r) {
            reason = r;
        }

        void fail(Reason r) {
            if (r.ordinal() > reason.ordinal()) reason = r;
        }

        void needMore(int count) {
            fail(Reason.NEED_MORE);
            if (needMin == 0 || count < needMin) needMin = count;
        }

        public Reason reason() {
            return reason;
        }

        public int needMin() {
            return needMin;
        }
    }

    public record Result(ItemStack source, int consume, List<ItemStack> grid) {}

    public record Entry(ItemStack input, List<ItemStack> grid) {}

    private static void fail(Trace t, Reason r) {
        if (t != null) t.fail(r);
    }

    private static List<RecipeHolder<?>> sourceRecipes(Level level) {
        Set<String> extra = new LinkedHashSet<>(Config.EXTRA_RECIPE_TYPES.get());
        List<RecipeHolder<?>> out = new ArrayList<>();
        for (RecipeHolder<?> h : level.getRecipeManager().getRecipes()) {
            Recipe<?> r = h.value();
            if (r.isSpecial()) continue;
            RecipeType<?> type = r.getType();
            ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(type);
            if (type == RecipeType.CRAFTING || (typeId != null && extra.contains(typeId.toString()))) out.add(h);
        }
        out.sort((a, b) -> a.id().compareTo(b.id()));
        return out;
    }

    public static Optional<Result> find(Level level, ItemStack stack, int maxTakes, Trace trace) {
        if (stack.isEmpty()) return Optional.empty();
        return find(level, stack, maxTakes, sourceRecipes(level), trace);
    }

    public static List<Entry> entries(Level level) {
        List<RecipeHolder<?>> recipes = sourceRecipes(level);
        Set<Item> items = new LinkedHashSet<>();
        for (RecipeHolder<?> h : recipes) items.add(h.value().getResultItem(level.registryAccess()).getItem());

        List<Entry> out = new ArrayList<>();
        for (Item item : items) {
            ItemStack probe = new ItemStack(item, item.getDefaultMaxStackSize());
            find(level, probe, 1, recipes, null)
                    .ifPresent(r -> out.add(new Entry(new ItemStack(item, r.consume()), r.grid())));
        }
        return out;
    }

    private static Optional<Result> find(Level level, ItemStack stack, int maxTakes, List<RecipeHolder<?>> recipes, Trace t) {
        if (stack.isEmpty()) return Optional.empty();

        if (blockingComponent(stack) != null) {
            if (t != null) t.input(Reason.NOT_PLAIN);
            return Optional.empty();
        }
        if (stack.isDamaged() && !Config.ALLOW_DAMAGED.get()) {
            if (t != null) t.input(Reason.DAMAGED);
            return Optional.empty();
        }

        Item item = stack.getItem();
        if (Config.BLACKLIST.get().contains(BuiltInRegistries.ITEM.getKey(item).toString())) {
            if (t != null) t.input(Reason.BLACKLISTED);
            return Optional.empty();
        }

        Result best = null;
        double bestRatio = Double.MAX_VALUE;

        for (RecipeHolder<?> holder : recipes) {
            Recipe<?> r = holder.value();
            ItemStack out = r.getResultItem(level.registryAccess());
            if (out.getItem() != item) continue;

            if (blockingComponent(out) != null) {
                fail(t, Reason.NO_RECIPE);
                continue;
            }
            int consume = out.getCount();
            if (consume < 1) continue;
            if (consume > stack.getMaxStackSize()) {
                fail(t, Reason.NO_RECIPE);
                continue;
            }
            if (consume > stack.getCount()) {
                if (t != null) t.needMore(consume);
                continue;
            }

            List<ItemStack> grid = buildGrid(r, out, t);
            if (grid == null) continue;
            if (stack.isDamaged()) grid = shrink(grid, 1f - stack.getDamageValue() / (float) stack.getMaxDamage());
            if (inflates(level, recipes, grid, item, consume, maxTakes)) {
                fail(t, Reason.LOOP);
                continue;
            }

            int count = (int) grid.stream().filter(s -> !s.isEmpty()).count();
            double ratio = count / (double) consume;
            if (ratio < bestRatio) {
                bestRatio = ratio;
                best = new Result(stack.copyWithCount(1), consume, grid);
            }
        }
        return Optional.ofNullable(best);
    }

    private static String blockingComponent(ItemStack stack) {
        List<? extends String> allowed = Config.ALLOWED_COMPONENTS.get();
        for (var e : stack.getComponentsPatch().entrySet()) {
            DataComponentType<?> type = e.getKey();
            if (type == DataComponents.DAMAGE || type == DataComponents.REPAIR_COST) continue;
            ResourceLocation key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (key != null && allowed.contains(key.toString())) continue;
            return key == null ? type.toString() : key.toString();
        }
        return null;
    }

    private static List<ItemStack> buildGrid(Recipe<?> r, ItemStack out, Trace t) {
        NonNullList<Ingredient> ings = r.getIngredients();
        boolean shaped = r instanceof ShapedRecipe;
        int width = shaped ? ((ShapedRecipe) r).getWidth() : 3;
        if (shaped && (width > 3 || ((ShapedRecipe) r).getHeight() > 3)) {
            fail(t, Reason.NO_RECIPE);
            return null;
        }
        if (!shaped && ings.size() > 9) {
            fail(t, Reason.NO_RECIPE);
            return null;
        }

        ItemStack[] grid = new ItemStack[9];
        Arrays.fill(grid, ItemStack.EMPTY);
        boolean any = false;

        for (int i = 0; i < ings.size(); i++) {
            Ingredient ing = ings.get(i);
            if (ing.isEmpty()) continue;

            ItemStack choice = pickOption(ing, out, t);
            if (choice == null) return null;

            int slot = shaped ? (i / width) * 3 + (i % width) : i;
            if (slot >= 9) return null;
            grid[slot] = choice.copyWithCount(1);
            any = true;
        }
        if (!any) fail(t, Reason.NO_RECIPE);
        return any ? List.of(grid) : null;
    }

    private static ItemStack pickOption(Ingredient ing, ItemStack out, Trace t) {
        int seen = 0, isResult = 0, hasData = 0, hasRemainder = 0;

        for (ItemStack option : ing.getItems()) {
            if (option.isEmpty()) continue;
            seen++;
            if (option.getItem() == out.getItem()) {
                isResult++;
            } else if (blockingComponent(option) != null) {
                hasData++;
            } else if (!option.getCraftingRemainingItem().isEmpty()) {
                hasRemainder++;
            } else {
                return option;
            }
        }
        if (t != null) {
            boolean onlyRemainder = seen > 0 && hasRemainder > 0 && hasData == 0 && isResult == 0;
            t.fail(onlyRemainder ? Reason.REMAINDER : Reason.BAD_INGREDIENT);
        }
        return null;
    }

    private static List<ItemStack> shrink(List<ItemStack> grid, float fraction) {
        int total = (int) grid.stream().filter(s -> !s.isEmpty()).count();
        int keep = Math.max(1, (int) Math.floor(total * fraction));
        List<ItemStack> out = new ArrayList<>();
        int kept = 0;
        for (ItemStack s : grid) {
            if (!s.isEmpty() && kept < keep) {
                out.add(s);
                kept++;
            } else {
                out.add(ItemStack.EMPTY);
            }
        }
        return out;
    }

    private static boolean inflates(Level level, List<RecipeHolder<?>> recipes, List<ItemStack> grid,
                                    Item input, int consume, int maxTakes) {
        Map<Item, Integer> counts = new HashMap<>();
        for (ItemStack s : grid) if (!s.isEmpty()) counts.merge(s.getItem(), 1, Integer::sum);

        for (RecipeHolder<?> holder : recipes) {
            Recipe<?> r = holder.value();
            ItemStack out = r.getResultItem(level.registryAccess());
            if (out.getItem() != input) continue;

            List<Ingredient> needed = new ArrayList<>();
            for (Ingredient i : r.getIngredients()) if (!i.isEmpty()) needed.add(i);
            if (needed.isEmpty()) continue;

            Map<Item, Integer> budget = new HashMap<>();
            int left = maxTakes;
            for (Map.Entry<Item, Integer> e : counts.entrySet()) {
                int takes = Math.min(left, e.getValue());
                budget.put(e.getKey(), takes);
                left -= takes;
            }
            if (canCraft(needed, budget) && out.getCount() > consume) return true;
        }
        return false;
    }

    private static boolean canCraft(List<Ingredient> needed, Map<Item, Integer> budget) {
        Map<Item, Integer> pool = new HashMap<>(budget);
        for (Ingredient ing : needed) {
            Item match = null;
            for (Map.Entry<Item, Integer> e : pool.entrySet()) {
                if (e.getValue() > 0 && ing.test(new ItemStack(e.getKey()))) {
                    match = e.getKey();
                    break;
                }
            }
            if (match == null) return false;
            pool.merge(match, -1, Integer::sum);
        }
        return true;
    }
}
