package com.shinybunny.cmdplus.commands.modifyitem;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.shinybunny.cmdplus.ThrowableConsumer;
import com.shinybunny.cmdplus.commands.ArgumentKey;
import net.minecraft.command.arguments.BlockStateArgument;
import net.minecraft.command.arguments.BlockStateArgumentType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class ModifyUsage implements ModifyActionType<ModifyUsage.Mode> {

    private static final ArgumentKey<Type> USAGE_TYPE = ArgumentKey.dummy();
    private static final ArgumentKey<BlockStateArgument> BLOCK = ArgumentKey.make("block", BlockStateArgumentType::blockState, BlockStateArgumentType::getBlockState);

    private static final DynamicCommandExceptionType NOT_BLOCK_ITEM = new DynamicCommandExceptionType(item->{
        return new TranslatableText("commands.modifyitem.failed.placeOn.not_block",item);
    });

    private Type type;

    public ModifyUsage(Type type) {
        this.type = type;
    }

    @Override
    public void addArguments(ModifyExecuteContext ctx) {
        ctx.set(USAGE_TYPE,type);
    }

    @Override
    public String getName() {
        return type.id;
    }

    @Override
    public Mode[] getModes() {
        return Mode.values();
    }

    protected enum Mode implements ModifyActionType.ActionMode {
        ADD {
            @Override
            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(Command<ServerCommandSource> execute) {
                return argument(BLOCK,execute);
            }

            @Override
            public void modify(ModifyExecuteContext ctx, ItemStack item) throws Exception {
                BlockStateArgument block = ctx.get(BLOCK);
                Type type = ctx.get(USAGE_TYPE);
                type.validate(item);
                ListTag list = item.getOrCreateTag().getList(type.tagName,8);
                list.add(StringTag.of(block.getBlockState().toString()));
                item.putSubTag(type.tagName,list);
            }

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return null;
            }
        },
        REMOVE {
            @Override
            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(Command<ServerCommandSource> execute) {
                return argument(BLOCK,execute);
            }

            @Override
            public void modify(ModifyExecuteContext ctx, ItemStack item) throws Exception {
                BlockStateArgument block = ctx.get(BLOCK);
                Type type = ctx.get(USAGE_TYPE);
                type.validate(item);
                ListTag list = item.getOrCreateTag().getList(type.tagName,8);
                int removeIndex = -1;
                for (int i = 0; i < list.size(); i++) {
                    Tag t = list.method_10534(i);
                    if (t.asString().equals(block.getBlockState().toString())) {
                        removeIndex = i;
                        break;
                    }
                }
                if (removeIndex != -1) {
                    list.method_10536(removeIndex);
                    item.putSubTag(type.tagName,list);
                }
            }

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return null;
            }
        },
        CLEAR {
            @Override
            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(Command<ServerCommandSource> execute) {
                return null;
            }

            @Override
            public void modify(ModifyExecuteContext ctx, ItemStack item) throws Exception {
                Type type = ctx.get(USAGE_TYPE);
                type.validate(item);
                item.removeSubTag(type.tagName);
            }

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return null;
            }
        };
    }

    public enum Type {
        CAN_BREAK("destroy","CanDestroy",item->{}),
        CAN_PLACE_ON("placeOn","CanPlaceOn",item->{
            if (!(item.getItem() instanceof BlockItem)) {
                throw NOT_BLOCK_ITEM.create(item.getName());
            }
        });

        private final String id;
        private final String tagName;
        private final ThrowableConsumer<ItemStack, Exception> validate;

        Type(String id, String tagName, ThrowableConsumer<ItemStack,Exception> validate) {
            this.id = id;
            this.tagName = tagName;
            this.validate = validate;
        }

        public void validate(ItemStack stack) throws Exception {
            validate.accept(stack);
        }
    }
}
