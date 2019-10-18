package com.shinybunny.cmdplus.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AbilityCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        ArgumentBuilder<ServerCommandSource,?> builder = argument("target",EntityArgumentType.player());

        for (Ability<?> ability : Abilities.VALUES) {
            builder.then(literal(ability.name).executes(ability::get).then(argument("value",ability.createArgumentType()).executes(ability::set)));
        }

        dispatcher.register(literal("ability")
                .requires(src->src.hasPermissionLevel(2))
                .then(builder));
    }

    private abstract static class Abilities {

        public static final List<Ability<?>> VALUES = new ArrayList<>();

        public static final BoolAbility FLY = new BoolAbility("fly",abilities->abilities.allowFlying,(abilities,b)->abilities.allowFlying = b);
        public static final BoolAbility INVULNERABLE = new BoolAbility("invulnerable",abilities->abilities.invulnerable,(abilities,b)->abilities.invulnerable = b);
        public static final FloatAbility FLY_SPEED = new FloatAbility("flySpeed", PlayerAbilities::getFlySpeed,PlayerAbilities::setFlySpeed);
        public static final FloatAbility WALK_SPEED = new FloatAbility("walkSpeed",PlayerAbilities::getWalkSpeed, PlayerAbilities::setWalkSpeed);


    }

    private abstract static class Ability<T> {
        private final String name;
        private Function<PlayerAbilities, T> getter;
        private final BiConsumer<PlayerAbilities, T> setter;



        private Ability(String name, Function<PlayerAbilities,T> getter, BiConsumer<PlayerAbilities, T> setter) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            Abilities.VALUES.add(this);
        }

        public int get(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
            PlayerEntity player = EntityArgumentType.getPlayer(ctx,"target");
            T t = getter.apply(player.abilities);
            ctx.getSource().sendFeedback(new TranslatableText("commands.ability.get",name,player.getName(),t),true);
            return toInt(t);
        }

        protected abstract int toInt(T t);

        protected abstract ArgumentType<T> createArgumentType();

        protected abstract T getArgument(CommandContext<ServerCommandSource> ctx);

        public int set(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
            PlayerEntity player = EntityArgumentType.getPlayer(ctx,"target");
            T value = getArgument(ctx);
            setter.accept(player.abilities,value);
            player.sendAbilitiesUpdate();
            ctx.getSource().sendFeedback(new TranslatableText("commands.ability.set",name,player.getName(),value),true);
            return 1;
        }
    }

    private static class FloatAbility extends Ability<Float> {
        private FloatAbility(String name, Function<PlayerAbilities, Float> getter, BiConsumer<PlayerAbilities, Float> setter) {
            super(name, getter, setter);
        }

        @Override
        protected int toInt(Float aFloat) {
            return (int)(double)aFloat;
        }

        @Override
        public ArgumentType<Float> createArgumentType() {
            return FloatArgumentType.floatArg();
        }

        @Override
        protected Float getArgument(CommandContext<ServerCommandSource> ctx) {
            return FloatArgumentType.getFloat(ctx,"value");
        }
    }

    private static class BoolAbility extends Ability<Boolean> {
        private BoolAbility(String name, Function<PlayerAbilities, Boolean> getter, BiConsumer<PlayerAbilities, Boolean> setter) {
            super(name, getter, setter);
        }

        @Override
        protected int toInt(Boolean aBoolean) {
            return aBoolean ? 1 : 0;
        }

        @Override
        public ArgumentType<Boolean> createArgumentType() {
            return BoolArgumentType.bool();
        }

        @Override
        protected Boolean getArgument(CommandContext<ServerCommandSource> ctx) {
            return BoolArgumentType.getBool(ctx,"value");
        }
    }
}
