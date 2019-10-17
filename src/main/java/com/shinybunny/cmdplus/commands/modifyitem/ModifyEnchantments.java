package com.shinybunny.cmdplus.commands.modifyitem;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.shinybunny.cmdplus.commands.ArgumentKey;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.command.arguments.ItemEnchantmentArgumentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.HashMap;
import java.util.Map;

public class ModifyEnchantments implements ModifyActionType<ModifyEnchantments.Mode> {

    private static final ArgumentKey<Enchantment> ENCHANTMENT = ArgumentKey.make("enchantment",ItemEnchantmentArgumentType::itemEnchantment,ItemEnchantmentArgumentType::getEnchantment);
    private static final ArgumentKey<Integer> RANK = ArgumentKey.make("rank",()-> IntegerArgumentType.integer(-1),IntegerArgumentType::getInteger);

    private static final DynamicCommandExceptionType NON_POSITIVE_LEVEL_EXCEPTION = new DynamicCommandExceptionType(i -> {
        return new TranslatableText("commands.modifyitem.failed.enchantment.non_positive_level", i);
    });
    private static final Dynamic2CommandExceptionType EQUAL_LEVELS_EXCEPTION = new Dynamic2CommandExceptionType((item, i) -> {
        return new TranslatableText("commands.modifyitem.failed.enchantment.set.equal_levels", item, i);
    });
    private static final DynamicCommandExceptionType INVALID_REMOVE_ENCHANT_LEVEL = new DynamicCommandExceptionType(i -> {
        return new TranslatableText("commands.modifyitem.failed.enchantment.remove.invalid_level", i);
    });
    private static final Dynamic2CommandExceptionType ITEM_DOESNT_HAVE_ENCHANTMENT = new Dynamic2CommandExceptionType((item, ench) -> {
        return new TranslatableText("commands.modifyitem.failed.enchantment.remove.ench_not_present", item, ench);
    });
    private static final DynamicCommandExceptionType NO_ENCHANTMENTS = new DynamicCommandExceptionType((item) -> {
        return new TranslatableText("commands.modifyitem.failed.enchantment.clear.no_enchants", item);
    });

    @Override
    public String getName() {
        return "enchantment";
    }

    @Override
    public Mode[] getModes() {
        return Mode.values();
    }


    protected enum Mode implements ModifyActionType.ActionMode {
        ADD {

            @Override
            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(Command<ServerCommandSource> execute) {
                return argument(ENCHANTMENT,execute).then(argument(RANK,execute));
            }

            @Override
            public void modify(ModifyExecuteContext ctx, ItemStack item) throws CommandSyntaxException {
                Enchantment enchantment = ctx.get(ENCHANTMENT);
                int rank = ctx.get(RANK);
                Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(item);
                if (enchantments.getOrDefault(enchantment, 0) == rank) {
                    throw EQUAL_LEVELS_EXCEPTION.create(item, rank);
                }
                int prevLevel = enchantments.getOrDefault(enchantment, 0);
                enchantments.put(enchantment, prevLevel + rank);
                EnchantmentHelper.set(enchantments, item);
            }

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return new TranslatableText("commands.modifyitem.success.enchantment.add",ctx.get(RANK), I18n.translate(ctx.get(ENCHANTMENT).getTranslationKey()), itemCount);
            }

            @Override
            public void validate(ModifyExecuteContext ctx) throws CommandSyntaxException {
                int rank = ctx.get(RANK);
                if (rank <= 0) {
                    throw NON_POSITIVE_LEVEL_EXCEPTION.create(RANK);
                }
            }
        },
        SET {
            @Override
            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(Command<ServerCommandSource> execute) {
                return argument(ENCHANTMENT,execute).then(argument(RANK,execute));
            }

            @Override
            public void modify(ModifyExecuteContext ctx, ItemStack item) throws CommandSyntaxException {
                Enchantment enchantment = ctx.get(ENCHANTMENT);
                int rank = ctx.get(RANK);
                Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(item);
                enchantments.put(enchantment, rank);
                EnchantmentHelper.set(enchantments, item);
            }

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return new TranslatableText("commands.modifyitem.success.enchantment.set",ctx.get(ENCHANTMENT).getName(ctx.get(RANK)),itemCount);
            }

            @Override
            public void validate(ModifyExecuteContext ctx) throws CommandSyntaxException {
                int rank = ctx.get(RANK);
                if (rank <= 0) {
                    throw NON_POSITIVE_LEVEL_EXCEPTION.create(RANK);
                }
            }
        },
        REMOVE {
            @Override
            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(Command<ServerCommandSource> execute) {
                return argument(ENCHANTMENT,execute).then(argument(RANK,execute));
            }

            @Override
            public void modify(ModifyExecuteContext ctx, ItemStack item) throws CommandSyntaxException {
                Enchantment enchantment = ctx.get(ENCHANTMENT);
                int rank = ctx.get(RANK);
                Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(item);
                if (!enchantments.containsKey(enchantment)) {
                    throw ITEM_DOESNT_HAVE_ENCHANTMENT.create(item.getName(), I18n.translate(enchantment.getTranslationKey()));
                }
                int resultLevel = enchantments.get(enchantment) - rank;
                if (resultLevel <= 0) {
                    enchantments.remove(enchantment);
                } else {
                    enchantments.put(enchantment, resultLevel);
                }
                EnchantmentHelper.set(enchantments, item);
            }

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return null;
            }

            @Override
            public void validate(ModifyExecuteContext ctx) throws CommandSyntaxException {
                int rank = ctx.get(RANK);
                if (rank <= 0) {
                    throw NON_POSITIVE_LEVEL_EXCEPTION.create(RANK);
                }
            }
        },
        CLEAR {
            @Override
            public ArgumentBuilder<ServerCommandSource, ?> buildArguments(Command<ServerCommandSource> execute) {
                return null;
            }

            @Override
            public void modify(ModifyExecuteContext ctx, ItemStack item) throws CommandSyntaxException {
                if (item.hasEnchantments()) {
                    EnchantmentHelper.set(new HashMap<>(), item);
                } else {
                    throw NO_ENCHANTMENTS.create(item.getName());
                }
            }

            @Override
            public Text getSuccessMessage(ModifyExecuteContext ctx, int itemCount) {
                return new TranslatableText("commands.modifyitem.success.enchantment.clear",itemCount);
            }
        }

    }

}


