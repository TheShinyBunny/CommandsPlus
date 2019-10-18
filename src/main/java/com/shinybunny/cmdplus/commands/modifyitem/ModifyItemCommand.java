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

    private static final ModifyActionType<?>[] MODIFY_ACTION_TYPES = new ModifyActionType[]{new ModifyEnchantments(),new ModifyDamage(),new ModifyNBT(),new ModifyLore(),new ModifyUsage(ModifyUsage.Type.CAN_BREAK),new ModifyUsage(ModifyUsage.Type.CAN_PLACE_ON)};

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
}
