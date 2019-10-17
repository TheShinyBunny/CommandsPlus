package com.shinybunny.cmdplus.commands.modifyitem;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.shinybunny.cmdplus.commands.ArgumentKey;
import net.minecraft.command.arguments.NbtCompoundTagArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class ModifyNBT implements ModifyActionType<ModifyNBT.Mode> {

    private static final ArgumentKey<CompoundTag> NBT = ArgumentKey.make("nbt", NbtCompoundTagArgumentType::nbtCompound, NbtCompoundTagArgumentType::getCompoundTag);

    @Override
    public String getName() {
        return "nbt";
    }

    @Override
    public Mode[] getModes() {
        return Mode.values();
    }

    protected enum Mode implements ModifyActionType.ActionMode {
        MERGE {
            @Override
            public void modify(ModifyExecuteContext ctx, ItemStack item) throws Exception {
                CompoundTag nbt = ctx.get(NBT);
                item.setTag(item.getOrCreateTag().method_10553().copyFrom(nbt));
            }

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return new TranslatableText("commands.modifyitem.success.nbt.merge",ctx.get(NBT).toText(),itemCount);
            }
        },
        SET {
            @Override
            public void modify(ModifyExecuteContext ctx, ItemStack item) throws Exception {
                CompoundTag nbt = ctx.get(NBT);
                item.setTag(nbt);
            }

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return new TranslatableText("commands.modifyitem.success.nbt.set",itemCount,ctx.get(NBT).toText());
            }
        },
        CLEAR {
            @Override
            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(Command<ServerCommandSource> execute) {
                return null;
            }

            @Override
            public void modify(ModifyExecuteContext ctx, ItemStack item) throws Exception {
                item.setTag(null);
            }

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return new TranslatableText("commands.modifyitem.success.nbt.clear",itemCount);
            }
        };

        @Override
        public ArgumentBuilder<ServerCommandSource, ?> buildArguments(Command<ServerCommandSource> execute) {
            return argument(NBT,execute);
        }
    }

}
