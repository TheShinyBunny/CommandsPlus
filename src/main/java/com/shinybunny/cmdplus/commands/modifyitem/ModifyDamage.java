package com.shinybunny.cmdplus.commands.modifyitem;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.shinybunny.cmdplus.commands.ArgumentKey;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class ModifyDamage implements ModifyActionType<ModifyDamage.Mode> {

    private static final ArgumentKey<Integer> AMOUNT = ArgumentKey.make("amount",()-> IntegerArgumentType.integer(0),IntegerArgumentType::getInteger);

    private static final DynamicCommandExceptionType NOT_DAMAGABLE = new DynamicCommandExceptionType(item->{
        return new TranslatableText("commands.modifyitem.failed.damage.not_damageable",item);
    });

    @Override
    public String getName() {
        return "damage";
    }

    @Override
    public Mode[] getModes() {
        return Mode.values();
    }

    protected enum Mode implements ModifyActionType.ActionMode {
        ADD {
            @Override
            protected int modify(int damage, int amount) {
                return damage + amount;
            }

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return new TranslatableText("commands.modifyitem.success.damage.add",itemCount,ctx.get(AMOUNT));
            }
        },
        RESTORE {

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return new TranslatableText("commands.modifyitem.success.damage.restore",ctx.get(AMOUNT),itemCount);
            }

            @Override
            protected int modify(int damage, int amount) {
                return damage - amount;
            }
        },
        SET {

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return new TranslatableText("commands.modifyitem.success.damage.set",ctx.get(AMOUNT),itemCount);
            }

            @Override
            protected int modify(int damage, int amount) {
                return amount;
            }
        };

        protected abstract int modify(int damage, int amount);

        @Override
        public ArgumentBuilder<ServerCommandSource, ?> buildArguments(Command<ServerCommandSource> execute) {
            return argument(AMOUNT,execute);
        }

        @Override
        public void modify(ModifyExecuteContext ctx, ItemStack item) throws Exception {
            int amount = ctx.get(AMOUNT);
            if (!item.isDamageable()) {
                throw NOT_DAMAGABLE.create(item.getName());
            }
            int damage = item.getDamage();
            int result = modify(damage,amount);
            if (result < 0) {
                result = 0;
            }
            if (result > item.getMaxDamage()) {
                throw new RemoveItem();
            }
            item.setDamage(result);
        }
    }

}
