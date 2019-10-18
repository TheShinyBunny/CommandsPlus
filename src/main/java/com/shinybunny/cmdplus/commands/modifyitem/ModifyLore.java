package com.shinybunny.cmdplus.commands.modifyitem;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.shinybunny.cmdplus.commands.ArgumentKey;
import net.minecraft.command.arguments.TextArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class ModifyLore implements ModifyActionType<ModifyLore.Mode> {

    private static final ArgumentKey<Text> TEXT = ArgumentKey.make("text", TextArgumentType::text, TextArgumentType::getTextArgument);
    private static final ArgumentKey<Integer> INDEX = ArgumentKey.make("index", ()-> IntegerArgumentType.integer(-1), IntegerArgumentType::getInteger);

    private static final DynamicCommandExceptionType INDEX_OUT_OF_BOUND = new DynamicCommandExceptionType((i)->{
        return new TranslatableText("commands.modifyitem.failed.lore.index",i);
    });

    @Override
    public String getName() {
        return "lore";
    }

    @Override
    public Mode[] getModes() {
        return Mode.values();
    }

    protected enum Mode implements ModifyActionType.ActionMode {
        ADD {
            @Override
            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(Command<ServerCommandSource> execute) {
                return argument(TEXT,execute).then(argument(INDEX,execute));
            }

            @Override
            public void modify(ModifyExecuteContext ctx, ListTag lore) throws Exception {
                Text line = ctx.get(TEXT);
                int index = ctx.get(INDEX,-1);

                if (index >= lore.size()) {
                    throw INDEX_OUT_OF_BOUND.create(index);
                }

                StringTag tag = StringTag.of(Text.Serializer.toJson(line));
                if (index == -1) {
                    lore.add(tag);
                } else {
                    lore.add(index,tag);
                }
            }

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                int index = ctx.get(INDEX,-1);
                if (index == -1) {
                    return new TranslatableText("commands.modifyitem.success.lore.add",ctx.get(TEXT),itemCount);
                } else {
                    return new TranslatableText("commands.modifyitem.success.lore.insert",ctx.get(TEXT),index,itemCount);
                }
            }
        },
        SET {
            @Override
            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(Command<ServerCommandSource> execute) {
                return argument(INDEX).then(argument(TEXT,execute));
            }

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return new TranslatableText("commands.modifyitem.success.lore.set",ctx.get(TEXT),ctx.get(INDEX),itemCount);
            }

            @Override
            protected void modify(ModifyExecuteContext ctx, ListTag lore) throws Exception {
                Text line = ctx.get(TEXT);
                int index = ctx.get(INDEX);

                if (index < 0 || index >= lore.size()) {
                    throw INDEX_OUT_OF_BOUND.create(index);
                }

                lore.setTag(index,StringTag.of(Text.Serializer.toJson(line)));
            }
        },
        REMOVE {
            @Override
            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(Command<ServerCommandSource> execute) {
                return argument(INDEX,execute);
            }

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return new TranslatableText("commands.modifyitem.success.lore.remove",ctx.get(INDEX),itemCount);
            }

            @Override
            protected void modify(ModifyExecuteContext ctx, ListTag lore) throws Exception {
                lore.method_10536(ctx.get(INDEX));
            }
        },
        CLEAR {
            @Override
            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(Command<ServerCommandSource> execute) {
                return null;
            }

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return new TranslatableText("commands.modifyitem.success.lore.clear",itemCount);
            }

            @Override
            protected void modify(ModifyExecuteContext ctx, ListTag lore) throws Exception {
                lore.clear();
            }
        };

        protected abstract void modify(ModifyExecuteContext ctx, ListTag lore) throws Exception;

        @Override
        public void modify(ModifyExecuteContext ctx, ItemStack item) throws Exception {
            CompoundTag display = item.getSubTag("display");
            if (display == null) {
                display = new CompoundTag();
            }
            ListTag lore = display.getList("Lore",8);
            modify(ctx,lore);
            display.put("Lore",lore);
            item.putSubTag("display",display);
        }
    }

}
