package com.shinybunny.cmdplus;

import com.shinybunny.cmdplus.commands.AbilityCommand;
import com.shinybunny.cmdplus.commands.modifyitem.ModifyItemCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.CommandRegistry;

public class CommandsPlus implements ModInitializer {

    @Override
    public void onInitialize() {
        CommandRegistry.INSTANCE.register(false, dispatcher -> {
            ModifyItemCommand.register(dispatcher);
            AbilityCommand.register(dispatcher);
        });
    }
}
