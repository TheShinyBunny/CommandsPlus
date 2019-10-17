package com.shinybunny.cmdplus.commands.modifyitem;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.*;
import com.shinybunny.cmdplus.commands.EntityInventoryHelper;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.arguments.*;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.minecraft.server.command.CommandManager.*;

public class ModifyItemCommand {

    private static final ModifyActionType<?>[] MODIFY_ACTION_TYPES = new ModifyActionType[]{new ModifyEnchantments(),new ModifyDamage(),new ModifyNBT()};

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> builder = literal("modifyitem")
                .requires((src)->src.hasPermissionLevel(2));
        for (InventoryType type : InventoryType.values()) {
            builder.then(literal(type.name().toLowerCase(Locale.ROOT)).then(buildForInventory(type)));
        }
        dispatcher.register(builder);
    }

    private static ArgumentBuilder<ServerCommandSource,?> buildForInventory(InventoryType inventoryType) {
        ArgumentBuilder<ServerCommandSource,?> builder = inventoryType.getSelectorArgument();

        for (SlotFinderType type : SlotFinderType.values()) {
            builder.then(literal(type.name().toLowerCase(Locale.ROOT)).then(buildForSlotFinder(inventoryType,type)));
        }
        return builder;
    }

    private static ArgumentBuilder<ServerCommandSource, ?> buildForSlotFinder(InventoryType inventoryType, SlotFinderType finderType) {
        return finderType.buildArguments(inventoryType);
    }


    public enum InventoryType {
        BLOCK {
            @Override
            public RequiredArgumentBuilder<ServerCommandSource, ?> getSelectorArgument() {
                return argument("pos", BlockPosArgumentType.blockPos());
            }

            @Override
            public List<InventoryHandle> getInventories(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
                BlockPos pos = BlockPosArgumentType.getBlockPos(ctx,"pos");
                BlockEntity te = ctx.getSource().getWorld().getBlockEntity(pos);
                if (!(te instanceof Inventory)) {
                    throw NOT_INVENTORY_EXCEPTION.create();
                }
                return Collections.singletonList(InventoryHandle.normal((Inventory) te));
            }
        },
        ENTITY {
            @Override
            public RequiredArgumentBuilder<ServerCommandSource, ?> getSelectorArgument() {
                return argument("selector", EntityArgumentType.entities());
            }

            @Override
            public List<InventoryHandle> getInventories(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
                Collection<? extends Entity> entities = EntityArgumentType.getEntities(ctx,"selector");
                return entities.stream().map(InventoryHandle::entity).collect(Collectors.toList());
            }
        };

        public static final SimpleCommandExceptionType NOT_INVENTORY_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("commands.replaceitem.failed.block"));

        public abstract RequiredArgumentBuilder<ServerCommandSource,?> getSelectorArgument();

        public abstract List<InventoryHandle> getInventories(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException;
    }




    public enum SlotFinderType {
        SLOT {
            @Override
            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(InventoryType inventoryType) {
                ArgumentBuilder<ServerCommandSource,?> builder = argument("slotName",ItemSlotArgumentType.itemSlot());
                return addActions(inventoryType,builder);
            }

            @Override
            public Map<Integer,ItemStack> findItems(InventoryHandle inventory, CommandContext<ServerCommandSource> ctx) {
                int slot = ItemSlotArgumentType.getItemSlot(ctx,"slotName");
                return Collections.singletonMap(slot,inventory.getItem(slot));
            }
        },
        ITEM {
            @Override
            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(InventoryType inventoryType) {
                ArgumentBuilder<ServerCommandSource,?> builder = addActions(inventoryType,argument("count",IntegerArgumentType.integer(-1)));
                return argument("item",ItemPredicateArgumentType.itemPredicate()).then(builder);
            }

            @Override
            public Map<Integer,ItemStack> findItems(InventoryHandle inventory, CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
                Map<Integer,ItemStack> items = inventory.getItems();
                Map<Integer,ItemStack> matching = new HashMap<>();
                Predicate<ItemStack> predicate = ItemPredicateArgumentType.getItemPredicate(ctx,"item");
                int maxCount = IntegerArgumentType.getInteger(ctx,"count");
                for (Map.Entry<Integer,ItemStack> e : items.entrySet()) {
                    if (matching.size() >= maxCount && maxCount != -1) break;
                    ItemStack stack = e.getValue();
                    if (predicate.test(stack)) {
                        matching.put(e.getKey(),stack);
                    }
                }
                return matching;
            }
        };

        public abstract Map<Integer,ItemStack> findItems(InventoryHandle inventory, CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException;

        public abstract ArgumentBuilder<ServerCommandSource,?> buildArguments(InventoryType inventoryType);

        public ArgumentBuilder<ServerCommandSource,?> addActions(InventoryType inventoryType, ArgumentBuilder<ServerCommandSource,?> builder) {
            for (ModifyActionType<?> actionType : MODIFY_ACTION_TYPES) {
                builder.then(actionType.build(new ModifyContext(inventoryType,this)));
            }
            return builder;
        }
    }

    public interface InventoryHandle {


        static InventoryHandle normal(Inventory inv) {
            return new InventoryHandle() {
                @Override
                public void setItem(int slot, ItemStack stack) {
                    inv.setInvStack(slot,stack);
                }

                @Override
                public ItemStack getItem(int slot) {
                    return inv.getInvStack(slot);
                }

                @Override
                public Map<Integer, ItemStack> getItems() {
                    Map<Integer, ItemStack> items = new HashMap<>();
                    for (int i = 0; i < inv.getInvSize(); i++) {
                        items.put(i,inv.getInvStack(i));
                    }
                    return items;
                }
            };
        }

        static InventoryHandle entity(Entity entity) {
            return new InventoryHandle() {
                @Override
                public void setItem(int slot, ItemStack stack) {
                    entity.equip(slot,stack);
                }

                @Override
                public ItemStack getItem(int slot) {
                    return EntityInventoryHelper.getItem(entity,slot);
                }

                @Override
                public Map<Integer,ItemStack> getItems() {
                    return EntityInventoryHelper.getItems(entity);
                }
            };
        }

        void setItem(int slot, ItemStack stack);

        ItemStack getItem(int slot);

        Map<Integer,ItemStack> getItems();
    }

    public static class RemoveItem extends Exception {
    }

    /*private static class ModifyEnchants {

        private static final DynamicCommandExceptionType NON_POSITIVE_LEVEL_EXCEPTION = new DynamicCommandExceptionType(i -> {
            return new TranslatableText("commands.modifyitem.failed.enchantment.non_positive_level", i);
        });
        private static final Dynamic2CommandExceptionType EQUAL_LEVELS_EXCEPTION = new Dynamic2CommandExceptionType((item, i) -> {
            return new TranslatableText("commands.modifyitem.failed.enchantment.set.equal_levels", item, i);
        });
        private static final DynamicCommandExceptionType INVALID_REMOVE_ENCHANT_LEVEL = new DynamicCommandExceptionType(i -> {
            return new TranslatableText("commands.modifyitem.failed.enchantment.remove.invalid_level", i);
        });
        private static final Dynamic2CommandExceptionType ITEM_DOESNT_HAVE_ENCHANTMENT = new Dynamic2CommandExceptionType((item, ench) -> {
            return new TranslatableText("commands.modifyitem.failed.enchantment.remove.ench_not_present", item, ench);
        });
        private static final DynamicCommandExceptionType NO_ENCHANTMENTS = new DynamicCommandExceptionType((item) -> {
            return new TranslatableText("commands.modifyitem.failed.enchantment.clear.no_enchants", item);
        });

        private static ArgumentBuilder<ServerCommandSource, ?> buildEnchantment(InventoryType inventoryType, SlotFinderType finderType) {
            return literal("enchantment")
                    .then(literal("add").then(addEnchantmentAndOptionalRank(1, (ctx, enchantment, level) -> addEnchantment(ctx, inventoryType, finderType, enchantment, level, false))))
                    .then(literal("set").then(addEnchantmentAndOptionalRank(1, (ctx, enchantment, level) -> addEnchantment(ctx, inventoryType, finderType, enchantment, level, true))))
                    .then(literal("remove").then(addEnchantmentAndOptionalRank(-1, (ctx, enchantment, level) -> removeEnchantment(ctx, inventoryType, finderType, enchantment, level))))
                    .then(literal("clear").executes(ctx -> {
                        return forEachItem(ctx,inventoryType, finderType, item -> {
                            if (item.hasEnchantments()) {
                                EnchantmentHelper.set(new HashMap<>(), item);
                            } else {
                                throw NO_ENCHANTMENTS.create(item.getName());
                            }
                        },"commands.modifyitem.success.enchantment.clear");
                    }));
        }

        private static int addEnchantment(CommandContext<ServerCommandSource> ctx, InventoryType inventoryType, SlotFinderType finderType, Enchantment enchantment, int level, boolean replace) throws CommandSyntaxException {
            if (level <= 0) {
                throw NON_POSITIVE_LEVEL_EXCEPTION.create(level);
            }
            String enchName = I18n.translate(enchantment.getTranslationKey());
            return forEachItem(ctx,inventoryType, finderType, item -> {
                Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(item);
                if (replace && enchantments.getOrDefault(enchantment, 0) == level) {
                    throw EQUAL_LEVELS_EXCEPTION.create(item, level);
                }
                if (replace) {
                    enchantments.put(enchantment, level);
                } else {
                    int prevLevel = enchantments.getOrDefault(enchantment, 0);
                    enchantments.put(enchantment, prevLevel + level);
                }
                EnchantmentHelper.set(enchantments, item);
            },replace ? "commands.modifyitem.success.enchantment.set" : "commands.modifyitem.success.enchantment.add", replace ? enchName : level, replace ? level : enchName);
        }

        private static int removeEnchantment(CommandContext<ServerCommandSource> ctx, InventoryType inventoryType, SlotFinderType finderType, Enchantment enchantment, int level) throws CommandSyntaxException {
            if (level < -1 || level == 0) {
                throw INVALID_REMOVE_ENCHANT_LEVEL.create(level);
            }
            return forEachItem(ctx,inventoryType, finderType, item -> {
                Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(item);
                if (!enchantments.containsKey(enchantment)) {
                    throw ITEM_DOESNT_HAVE_ENCHANTMENT.create(item.getName(), I18n.translate(enchantment.getTranslationKey()));
                }
                int resultLevel = enchantments.get(enchantment) - level;
                if (resultLevel <= 0) {
                    enchantments.remove(enchantment);
                } else {
                    enchantments.put(enchantment, resultLevel);
                }
                EnchantmentHelper.set(enchantments, item);
            },"commands.modifyitem.success.enchantment.remove",level,I18n.translate(enchantment.getTranslationKey()));
        }

        private static ArgumentBuilder<ServerCommandSource, ?> addEnchantmentAndOptionalRank(int defaultRank, EnchantmentModifier modifier) {
            return argument("enchantment", ItemEnchantmentArgumentType.itemEnchantment())
                    .executes(ctx -> modifier.run(ctx, ItemEnchantmentArgumentType.getEnchantment(ctx, "enchantment"), defaultRank))
                    .then(argument("rank", IntegerArgumentType.integer())
                            .executes(ctx -> modifier.run(ctx, ItemEnchantmentArgumentType.getEnchantment(ctx, "enchantment"), IntegerArgumentType.getInteger(ctx, "rank"))));
        }

        private interface EnchantmentModifier {
            int run(CommandContext<ServerCommandSource> ctx, Enchantment enchantment, int level) throws CommandSyntaxException;
        }

    }

    private static class ModifyDamage {


        private static final DynamicCommandExceptionType NOT_DAMAGABLE = new DynamicCommandExceptionType(item->{
            return new TranslatableText("commands.modifyitem.failed.damage.not_damageable",item);
        });

        public static ArgumentBuilder<ServerCommandSource, ?> buildDamage(InventoryType inventoryType, SlotFinderType finderType) {
            ArgumentBuilder<ServerCommandSource,?> builder = literal("damage");
            for (Mode m : Mode.values()) {
                builder.then(m.buildArguments(inventoryType, finderType));
            }
            return builder;
        }

        private static int changeDamage(CommandContext<ServerCommandSource> ctx, InventoryType inventoryType, SlotFinderType finderType, Mode mode, int amount) throws CommandSyntaxException {
            return forEachItem(ctx,inventoryType,finderType,item->{
                if (!item.isDamageable()) {
                    throw NOT_DAMAGABLE.create(item.getName());
                }
                int damage = item.getDamage();
                int result = mode.modify(damage,amount);
                if (result < 0) {
                    result = 0;
                }
                if (result > item.getMaxDamage()) {
                    throw new RemoveItem();
                }
                item.setDamage(result);
            },mode.success,amount);
        }

        private enum Mode {
            ADD("commands.modifyitem.success.damage.add") {
                @Override
                public int modify(int damage, int amount) {
                    return damage + amount;
                }
            },
            RESTORE("commands.modifyitem.success.damage.restore") {
                @Override
                public int modify(int damage, int amount) {
                    return damage - amount;
                }
            },
            SET("commands.modifyitem.success.damage.set") {
                @Override
                public int modify(int damage, int amount) {
                    return amount;
                }
            };

            private String success;

            Mode(String success) {
                this.success = success;
            }

            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(InventoryType inventoryType, SlotFinderType finderType) {
                return literal(name().toLowerCase(Locale.ROOT)).then(argument("amount",IntegerArgumentType.integer(0)).executes(ctx->changeDamage(ctx,inventoryType,finderType,this,IntegerArgumentType.getInteger(ctx,"amount"))));
            }


            public abstract int modify(int damage, int amount);
        }

    }

    private static class ModifyNBT {

        public static ArgumentBuilder<ServerCommandSource, ?> buildNBT(InventoryType inventoryType, SlotFinderType finderType) {
            ArgumentBuilder<ServerCommandSource,?> builder = literal("nbt");
            for (Mode m : Mode.values()) {
                builder.then(m.buildArguments(inventoryType, finderType));
            }
            return builder;
        }

        private static int changeNBT(CommandContext<ServerCommandSource> ctx, InventoryType inventoryType, SlotFinderType finderType, Mode mode, CompoundTag nbt) throws CommandSyntaxException {
            return forEachItem(ctx,inventoryType,finderType,item->{
                mode.modify(item,nbt);
            },mode.success,nbt == null ? new Object[0] : nbt);
        }

        private enum Mode {
            MERGE("commands.modifyitem.success.nbt.merge") {
                @Override
                public void modify(ItemStack stack, CompoundTag nbt) {
                    stack.setTag(stack.getOrCreateTag().method_10553().copyFrom(nbt));
                }
            },
            SET("commands.modifyitem.success.nbt.set") {
                @Override
                public void modify(ItemStack stack, CompoundTag nbt) {
                    stack.setTag(nbt);
                }
            },
            CLEAR("commands.modifyitem.success.nbt.clear") {
                @Override
                public void modify(ItemStack stack, CompoundTag nbt) {
                    stack.setTag(null);
                }
            };

            public String success;

            Mode(String success) {
                this.success = success;
            }

            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(InventoryType inventoryType, SlotFinderType finderType) {
                ArgumentBuilder<ServerCommandSource,?> builder = literal(name().toLowerCase(Locale.ROOT));
                if (this == CLEAR) {
                    return builder.executes(ctx->changeNBT(ctx,inventoryType,finderType,this,null));
                } else {
                    return builder.then(argument("nbt",NbtCompoundTagArgumentType.nbtCompound()).executes(ctx->changeNBT(ctx,inventoryType,finderType,this,NbtCompoundTagArgumentType.getCompoundTag(ctx,"nbt"))));
                }
            }


            public abstract void modify(ItemStack stack, CompoundTag nbt);
        }
    }*/
}
