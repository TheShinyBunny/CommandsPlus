package com.shinybunny.cmdplus.commands.modifyitem;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.shinybunny.cmdplus.commands.ArgumentKey;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public class ModifyExecuteContext extends ModifyContext {

    private CommandContext<ServerCommandSource> cmdCtx;
    private Map<ArgumentKey<?>,Object> valueCache = new HashMap<>();

    public ModifyExecuteContext(ModifyItemCommand.InventoryType inventoryType, ModifyItemCommand.SlotFinderType finderType, CommandContext<ServerCommandSource> cmdCtx) {
        super(inventoryType, finderType);
        this.cmdCtx = cmdCtx;
    }

    public CommandContext<ServerCommandSource> getCmdCtx() {
        return cmdCtx;
    }

    public void sendError(Text text) {
        cmdCtx.getSource().sendError(text);
    }

    public void sendFeedback(Text text) {
        cmdCtx.getSource().sendFeedback(text,true);
    }

    public <T> void set(ArgumentKey<T> key, T value) {
        valueCache.put(key,value);
    }

    public <T> T get(ArgumentKey<T> key) {
        Object t = valueCache.get(key);
        if (t == null) {
            t = key.get(cmdCtx);
            valueCache.put(key, t);
        }
        return (T) t;
    }

    public <T> T get(ArgumentKey<T> key, T def) {
        T t = get(key);
        return t == null ? def : t;
    }
}
