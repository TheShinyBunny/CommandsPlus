package com.shinybunny.cmdplus.commands;

import com.google.common.collect.Iterables;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class EntityInventoryHelper {


    public static ItemStack getItem(Entity entity, int slot) {
        if (slot < 9) {
            return assertEntityType(entity, PlayerEntity.class, (player)->{
               return player.inventory.getInvStack(slot);
            });
        }
        if (slot < 36) {
            return assertEntityType(entity, PlayerEntity.class, (player)->{
                return player.inventory.getInvStack(slot - 9);
            });
        }
        if (slot == 98) {
            return assertEntityType(entity, LivingEntity.class, LivingEntity::getMainHandStack);
        }
        if (slot == 99) {
            return assertEntityType(entity, LivingEntity.class, LivingEntity::getOffHandStack);
        }
        if (slot > 99 && slot < 104) {
            return assertEntityType(entity, LivingEntity.class, living->{
                return Iterables.get(living.getArmorItems(),slot - 100);
            });
        }
        if (slot < 227) {
            return assertEntityType(entity, PlayerEntity.class, (player)->{
                return player.getEnderChestInventory().getInvStack(slot - 200);
            });
        }
        if (slot < 308) {
            return assertEntityType(entity, AbstractTraderEntity.class, villager->{
                return villager.getInventory().getInvStack(slot - 300);
            });
        }
        if (slot == 400) {
            return assertEntityType(entity, HorseBaseEntity.class, horse->{
                return ItemStack.fromTag(horse.toTag(new CompoundTag()).getCompound("SaddleItem"));
            });
        }
        if (slot == 401) {
            return assertEntityType(entity, HorseEntity.class, horse->{
                return ItemStack.fromTag(horse.toTag(new CompoundTag()).getCompound("ArmorItem"));
            });
        }
        if (slot == 499) {
            return assertEntityType(entity, AbstractDonkeyEntity.class, donkey->{
                return donkey.hasChest() ? new ItemStack(Blocks.CHEST) : ItemStack.EMPTY;
            });
        }
        if (slot < 515) {
            return assertEntityType(entity, AbstractDonkeyEntity.class, donkey->{
                return ItemStack.fromTag(donkey.toTag(new CompoundTag()).getList("Items",10).getCompound(slot - 500));
            });
        }
        return null;
    }

    private static <T extends Entity> ItemStack assertEntityType(Entity entity, Class<T> cls, Function<T,ItemStack> func) {
        if (cls.isInstance(entity)) {
            return func.apply((T) entity);
        }
        return null;
    }

    public static Map<Integer, ItemStack> getItems(Entity entity) {
        Map<Integer,ItemStack> items = new HashMap<>();
        if (entity instanceof PlayerEntity) {
            for (int i = 0; i < 36; i++) {
                items.put(i,((PlayerEntity) entity).inventory.main.get(i));
            }
            for (int i = 0; i < 27; i++) {
                items.put(200 + i,((PlayerEntity) entity).getEnderChestInventory().getInvStack(i));
            }
        }
        if (entity instanceof LivingEntity) {
            if (!(entity instanceof PlayerEntity)) {
                items.put(98, ((LivingEntity) entity).getMainHandStack());
                items.put(99, ((LivingEntity) entity).getOffHandStack());
            }
            Iterable<ItemStack> armor = entity.getArmorItems();
            for (int i = 0; i < 4; i++) {
                items.put(i + 100,Iterables.get(armor,i));
            }
        }
        if (entity instanceof AbstractTraderEntity) {
            for (int i = 0; i < 7; i++) {
                items.put(i + 300,((AbstractTraderEntity) entity).getInventory().getInvStack(i));
            }
        }
        if (entity instanceof HorseBaseEntity) {
            items.put(400, getItem(entity,400));
        }
        if (entity instanceof HorseEntity) {
            items.put(401, getItem(entity, 401));
        }
        if (entity instanceof AbstractDonkeyEntity) {
            items.put(499, getItem(entity,499));
            ListTag inv = entity.toTag(new CompoundTag()).getList("Items",10);
            for (int i = 0; i < 15; i++) {
                items.put(i + 500, ItemStack.fromTag(inv.getCompound(i)));
            }
        }
        return items;
    }
}
