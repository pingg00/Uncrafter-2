package org.pingg.uncrafter_2;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class UnCrafterMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 25, INPUT_Y = 38;
    public static final int GRID_X = 100, GRID_Y = 18;
    public static final int INV_Y = 94, HOTBAR_Y = 152;

    private static final int INPUT = 0;
    private static final int DISPLAY_START = 1, DISPLAY_END = 10;
    private static final int INV_START = 10, INV_END = 37, HOTBAR_END = 46;

    private final ContainerLevelAccess access;
    private final int maxTakes;
    private final Player owner;

    private final DataSlot takesLeft = DataSlot.standalone();
    private final DataSlot xpCost = DataSlot.standalone();
    private final DataSlot lossPercent = DataSlot.standalone();
    private final DataSlot failReason = DataSlot.standalone();
    private final DataSlot failArg = DataSlot.standalone();

    private final SimpleContainer input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            UnCrafterMenu.this.slotsChanged(this);
        }
    };
    private final SimpleContainer display = new SimpleContainer(9) {
        @Override
        public void setChanged() {}
    };

    private UncraftLogic.Result active;
    private boolean lastTake;

    public UnCrafterMenu(MenuType<?> type, int id, Inventory playerInv, ContainerLevelAccess access, int maxTakes) {
        super(type, id);
        this.access = access;
        this.maxTakes = maxTakes;
        this.owner = playerInv.player;
        addDataSlot(takesLeft);
        addDataSlot(xpCost);
        addDataSlot(lossPercent);
        addDataSlot(failReason);
        addDataSlot(failArg);

        addSlot(new InputSlot(input, 0, INPUT_X, INPUT_Y));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                addSlot(new DisplaySlot(display, c + r * 3, GRID_X + c * 18, GRID_Y + r * 18));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(playerInv, c + r * 9 + 9, 8 + c * 18, INV_Y + r * 18));
        for (int c = 0; c < 9; c++)
            addSlot(new Slot(playerInv, c, 8 + c * 18, HOTBAR_Y));
    }

    public boolean isLocked() {
        return takesLeft.get() > 0;
    }

    public int getTakesLeft() {
        return takesLeft.get();
    }

    public int getMaxTakes() {
        return maxTakes;
    }

    public int getXpCost() {
        return xpCost.get();
    }

    public int getLossPercent() {
        return lossPercent.get();
    }

    public UncraftLogic.Reason getFailReason() {
        UncraftLogic.Reason[] all = UncraftLogic.Reason.values();
        int i = failReason.get();
        return i > 0 && i < all.length ? all[i] : UncraftLogic.Reason.NONE;
    }

    public int getFailArg() {
        return failArg.get();
    }

    public int ingredientCount() {
        int n = 0;
        for (int i = DISPLAY_START; i < DISPLAY_END; i++) if (slots.get(i).hasItem()) n++;
        return n;
    }

    private int xpPrice() {
        return Config.UNCRAFTER_XP.get();
    }

    @Override
    public void broadcastChanges() {
        if (!owner.level().isClientSide) {
            xpCost.set(xpPrice());
            lossPercent.set(Config.LOSS_PERCENT.get());
        }
        super.broadcastChanges();
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == input && !isLocked()) updateResult();
        super.slotsChanged(container);
    }

    private void updateResult() {
        access.execute((level, pos) -> {
            if (level.isClientSide) return;
            ItemStack in = input.getItem(0);
            UncraftLogic.Trace trace = new UncraftLogic.Trace();
            active = UncraftLogic.find(level, in, maxTakes, trace).orElse(null);

            boolean showReason = active == null && !in.isEmpty();
            failReason.set(showReason ? trace.reason().ordinal() : 0);
            failArg.set(showReason ? trace.needMin() : 0);
            for (int i = 0; i < 9; i++) {
                display.setItem(i, active == null ? ItemStack.EMPTY : active.grid().get(i).copy());
            }
        });
    }

    private void clearDisplay() {
        for (int i = 0; i < 9; i++) display.setItem(i, ItemStack.EMPTY);
    }

    private int shown() {
        int n = 0;
        for (int i = 0; i < 9; i++) if (!display.getItem(i).isEmpty()) n++;
        return n;
    }

    private void endSession() {
        takesLeft.set(0);
        active = null;
        clearDisplay();
        updateResult();
    }

    private boolean reserve(int displayIndex) {
        lastTake = false;
        UncraftLogic.Result a = active;
        if (a == null) return false;

        if (!ItemStack.isSameItemSameComponents(display.getItem(displayIndex), a.grid().get(displayIndex))) return false;

        if (isLocked()) {
            takesLeft.set(takesLeft.get() - 1);
            lastTake = takesLeft.get() <= 0 || shown() <= 1;
            return true;
        }

        ItemStack in = input.getItem(0);
        if (in.isEmpty()
                || !ItemStack.isSameItemSameComponents(a.source(), in) || in.getCount() < a.consume()) {
            return false;
        }

        int cost = xpPrice();
        boolean free = cost == 0 || owner.getAbilities().instabuild;
        if (!free && owner.experienceLevel < cost) {
            owner.displayClientMessage(Component.translatable("message.uncrafter_2.no_xp", cost), true);
            return false;
        }

        boolean multi = maxTakes > 1 && shown() > 1;
        // lock first, otherwise changing the input slot rebuilds the preview
        if (multi) takesLeft.set(maxTakes - 1);

        in.shrink(a.consume());
        if (in.isEmpty()) input.setItem(0, ItemStack.EMPTY);
        if (!free) owner.giveExperienceLevels(-cost);

        playEffects();
        award("first_uncraft");
        if (maxTakes > 1) award("upgraded_uncraft");
        lastTake = !multi;
        return true;
    }

    private void finishTake(Slot slot) {
        slot.set(ItemStack.EMPTY);
        if (lastTake) endSession();
        lastTake = false;
    }

    private boolean hasRoom(ItemStack stack) {
        for (int i = INV_START; i < HOTBAR_END; i++) {
            ItemStack s = slots.get(i).getItem();
            if (s.isEmpty() || (ItemStack.isSameItemSameComponents(s, stack) && s.getCount() < s.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    // decide where the ingredient goes before consuming anything
    private boolean take(int slotId, int button, ClickType type, Player player) {
        if (player != owner || !player.isAlive() || player.isSpectator() || !stillValid(player)) return false;

        Slot slot = slots.get(slotId);
        if (!slot.hasItem()) return false;
        int idx = slotId - DISPLAY_START;
        ItemStack ing = slot.getItem().copyWithCount(1);

        Runnable give;
        switch (type) {
            case PICKUP -> {
                ItemStack carried = getCarried();
                boolean fits = carried.isEmpty() || (ItemStack.isSameItemSameComponents(carried, ing)
                        && carried.getCount() < carried.getMaxStackSize());
                if (!fits) return false;
                give = () -> {
                    if (carried.isEmpty()) setCarried(ing); else carried.grow(1);
                };
            }
            case QUICK_MOVE -> {
                if (!hasRoom(ing)) return false;
                give = () -> {
                    ItemStack rest = ing.copy();
                    moveItemStackTo(rest, INV_START, HOTBAR_END, true);
                    if (!rest.isEmpty()) player.drop(rest, false);
                };
            }
            case THROW -> give = () -> player.drop(ing, true);
            case SWAP -> {
                if (!(button >= 0 && button < 9) && button != 40) return false;
                Inventory inv = player.getInventory();
                if (!inv.getItem(button).isEmpty()) return false;
                give = () -> inv.setItem(button, ing);
            }
            default -> {
                return false;
            }
        }

        if (!reserve(idx)) return false;
        if (owner.getRandom().nextInt(100) < Config.LOSS_PERCENT.get()) lose(); else give.run();
        finishTake(slot);
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= DISPLAY_START && slotId < DISPLAY_END) {
            if (!player.level().isClientSide) take(slotId, button, clickType, player);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || player != owner) return false;
        int n = Math.min(isLocked() ? takesLeft.get() : maxTakes, shown());
        boolean any = false;
        for (int k = 0; k < n; k++) {
            int first = -1;
            for (int i = 0; i < 9 && first < 0; i++) if (!display.getItem(i).isEmpty()) first = i;
            if (first < 0 || !take(DISPLAY_START + first, 0, ClickType.QUICK_MOVE, player)) break;
            any = true;
        }
        return any;
    }

    private void award(String name) {
        if (!(owner instanceof ServerPlayer sp)) return;
        AdvancementHolder holder = sp.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(UnCrafterMod.MOD_ID, name));
        if (holder != null) sp.getAdvancements().award(holder, "uncraft");
    }

    private void lose() {
        owner.displayClientMessage(Component.translatable("message.uncrafter_2.lost"), true);
        access.execute((level, pos) -> {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.2f);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.02);
            }
        });
    }

    private void playEffects() {
        access.execute((level, pos) -> {
            if (maxTakes > 1) {
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 0.8f);
                level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.3f, 1.2f);
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5f, 1.2f);
            } else {
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.0f);
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5f, 1.4f);
            }
            if (level instanceof ServerLevel sl) {
                double x = pos.getX() + 0.5, y = pos.getY() + 1.05, z = pos.getZ() + 0.5;
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 10, 0.25, 0.1, 0.25, 0.3);
                sl.sendParticles(ParticleTypes.CRIT, x, y, z, 6, 0.3, 0.1, 0.3, 0.4);
                if (maxTakes > 1) sl.sendParticles(ParticleTypes.END_ROD, x, y, z, 8, 0.25, 0.15, 0.25, 0.05);
            }
        });
    }

    private class InputSlot extends Slot {
        InputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !isLocked();
        }
    }

    private static class DisplaySlot extends Slot {
        DisplaySlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return !(slot instanceof DisplaySlot) && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem() || (index >= DISPLAY_START && index < DISPLAY_END)) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index == INPUT) {
            if (!moveItemStackTo(stack, INV_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (isLocked() || !moveItemStackTo(stack, INPUT, INPUT + 1, false)) {
            // moveItemStackTo merges into the input slot even when mayPlace is false
            if (index < INV_END) {
                if (!moveItemStackTo(stack, INV_END, HOTBAR_END, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, INV_START, INV_END, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) return;
        refund(player);
        access.execute((level, pos) -> clearContainer(player, input));
    }

    private void refund(Player player) {
        int left = takesLeft.get();
        takesLeft.set(0);
        boolean gone = !player.isAlive() || (player instanceof ServerPlayer sp && sp.hasDisconnected());

        for (int i = 0; i < 9 && left > 0; i++) {
            ItemStack s = display.getItem(i);
            if (s.isEmpty()) continue;
            left--;
            display.setItem(i, ItemStack.EMPTY);
            if (player.getRandom().nextInt(100) < Config.LOSS_PERCENT.get()) continue;
            if (gone) player.drop(s, false); else player.getInventory().placeItemBackInInventory(s);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof UnCrafterBlock
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }
}
