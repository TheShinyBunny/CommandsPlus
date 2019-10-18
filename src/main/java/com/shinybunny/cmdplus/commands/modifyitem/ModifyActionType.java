package com.shinybunny.cmdplus.commands.modifyitem;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.shinybunny.cmdplus.ThrowableConsumer;
import com.shinybunny.cmdplus.commands.ArgumentKey;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.*;

import static net.minecraft.server.command.CommandManager.literal;

public interface ModifyActionType<M extends ModifyActionType.ActionMode> {

    default ArgumentBuilder<ServerCommandSource,?> build(ModifyContext ctx) {
        ArgumentBuilder<ServerCommandSource,?> builder = literal(getName());
        for (M m : getModes()) {
            Command<ServerCommandSource> cmd = c->execute(c,m,ctx);
            ArgumentBuilder<ServerCommandSource,?> name = literal(m.toString().toLowerCase(Locale.ROOT));
            ArgumentBuilder<ServerCommandSource,?> args = m.buildArguments(cmd);
            if (args == null) {
                builder.then(name.executes(cmd));
            } else {
                builder.then(name.then(args));
            }
        }
        return builder;
    }

    String getName();

    M[] getModes();

    default void addArguments(ModifyExecuteContext ctx) {

    }

    default int execute(CommandContext<ServerCommandSource> ctx, ActionMode mode, ModifyContext modifyContext) throws CommandSyntaxException {
        ModifyExecuteContext executeCtx = new ModifyExecuteContext(modifyContext.getInventoryType(),modifyContext.getFinderType(),ctx);
        addArguments(executeCtx);
        mode.validate(executeCtx);
        return forEachItem(executeCtx,mode);
    }

    static int forEachItem(ModifyExecuteContext ctx, ActionMode mode) throws CommandSyntaxException {
        int i = 0;
        try {
            List<ModifyItemCommand.InventoryHandle> inventories = ctx.getInventoryType().getInventories(ctx.getCmdCtx());

            for (ModifyItemCommand.InventoryHandle h : inventories) {
                Map<Integer, ItemStack> items = ctx.getFinderType().findItems(h, ctx.getCmdCtx());
                Map<Integer, ItemStack> changes = new HashMap<>();
                for (Map.Entry<Integer, ItemStack> e : items.entrySet()) {
                    if (e.getValue().isEmpty()) continue;
                    System.out.println("modifying item at slot " + e.getKey());
                    ItemStack stack = e.getValue().copy();
                    try {
                        mode.modify(ctx,stack);
                        changes.put(e.getKey(), stack);
                    } catch (RemoveItem re) {
                        changes.put(e.getKey(), ItemStack.EMPTY);
                    } catch (Exception ex) {
                        ctx.sendError(new LiteralText(ex.getMessage()));
                        ex.printStackTrace();
                        continue;
                    }

                    i++;
                }
                changes.forEach(h::setItem);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            ctx.sendError(new LiteralText("internal error"));
            return 0;
        }
        if (i == 0) {
            ctx.sendError(new TranslatableText("commands.modifyitem.failed.no_items"));
        } else {
            ctx.sendFeedback(mode.getSuccessMessage(ctx,i));
        }
        return i;
    }

    interface ActionMode {

        default <T> RequiredArgumentBuilder<ServerCommandSource,T> argument(ArgumentKey<T> key) {
            return CommandManager.argument(key.getName(),key.createType());
        }

        default <T> RequiredArgumentBuilder<ServerCommandSource,T> argument(ArgumentKey<T> key, Command<ServerCommandSource> execute) {
            return CommandManager.argument(key.getName(),key.createType()).executes(execute);
        }

        ArgumentBuilder<ServerCommandSource,?> buildArguments(Command<ServerCommandSource> execute);

        void modify(ModifyExecuteContext ctx, ItemStack item) throws Exception;

        Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount);

        default void validate(ModifyExecuteContext ctx) throws CommandSyntaxException {

        }
    }

    class RemoveItem extends Exception {
    }
}
