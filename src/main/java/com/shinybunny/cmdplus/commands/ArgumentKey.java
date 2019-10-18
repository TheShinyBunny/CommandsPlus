package com.shinybunny.cmdplus.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;

import java.util.function.Supplier;

public class ArgumentKey<T> {

    private final String name;
    private final Supplier<? extends ArgumentType<T>> argumentTypeSupplier;
    private final ValueGetter<T> valueGetter;

    public ArgumentKey(String name, Supplier<? extends ArgumentType<T>> argumentTypeSupplier, ValueGetter<T> valueGetter) {
        this.name = name;
        this.argumentTypeSupplier = argumentTypeSupplier;
        this.valueGetter = valueGetter;
    }

    public static <T> ArgumentKey<T> make(String name, Supplier<? extends ArgumentType<T>> argumentTypeSupplier, ValueGetter<T> valueGetter) {
        return new ArgumentKey<>(name,argumentTypeSupplier,valueGetter);
    }

    public static <T> ArgumentKey<T> dummy() {
        return new ArgumentKey<>("dummy", null,((ctx, name) -> null));
    }

    public String getName() {
        return name;
    }

    public Supplier<? extends ArgumentType<T>> getArgumentTypeSupplier() {
        return argumentTypeSupplier;
    }

    public ValueGetter<T> getValueGetter() {
        return valueGetter;
    }

    public ArgumentType<T> createType() {
        return argumentTypeSupplier.get();
    }

    public T get(CommandContext<ServerCommandSource> ctx) {
        try {
            return valueGetter.get(ctx, name);
        } catch (Exception e) {
            return null;
        }
    }

    @FunctionalInterface
    public interface ValueGetter<T> {

        T get(CommandContext<ServerCommandSource> ctx, String name) throws CommandSyntaxException;

    }
}
