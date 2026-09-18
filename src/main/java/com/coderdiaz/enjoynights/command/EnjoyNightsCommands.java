package com.coderdiaz.enjoynights.command;

import com.coderdiaz.enjoynights.util.NightAndMoonHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class EnjoyNightsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("enjoynights")
                        .then(Commands.literal("info")
                                .executes(ctx -> showInfo(ctx.getSource())))
                        .then(Commands.literal("bloodmoon")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.literal("start")
                                        .executes(ctx -> startBloodMoon(ctx.getSource())))
                                .then(Commands.literal("stop")
                                        .executes(ctx -> stopBloodMoon(ctx.getSource()))))
        );
    }

    private static int showInfo(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        long day = NightAndMoonHelper.getDay(level);
        int phase = NightAndMoonHelper.getMoonPhase(level);
        boolean isNight = NightAndMoonHelper.isNight(level);
        boolean isBloodMoon = NightAndMoonHelper.isBloodMoon(level);
        long daysUntilBloodMoon = NightAndMoonHelper.getDaysUntilNextBloodMoon(level);

        String[] phaseNames = new String[]{
                "Luna Llena (Full Moon)",
                "Gibosa Menguante",
                "Cuarto Menguante",
                "Creciente Menguante",
                "Luna Nueva (New Moon)",
                "Creciente Iluminada",
                "Cuarto Creciente",
                "Gibosa Creciente"
        };

        source.sendSuccess(() -> Component.literal("=== Enjoy Nights! Info ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("Día actual: " + day).withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Estado: " + (isNight ? "Noche" : "Día")).withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("Fase Lunar: " + phaseNames[phase]).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        source.sendSuccess(() -> Component.literal("Luna de Sangre activa: " + (isBloodMoon ? "SÍ" : "NO")).withStyle(isBloodMoon ? ChatFormatting.RED : ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Días hasta próxima Luna de Sangre: " + daysUntilBloodMoon).withStyle(ChatFormatting.WHITE), false);

        return 1;
    }

    private static int startBloodMoon(CommandSourceStack source) {
        NightAndMoonHelper.setForceBloodMoon(true);
        source.sendSuccess(() -> Component.literal("¡Luna de Sangre forzada activada!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
        return 1;
    }

    private static int stopBloodMoon(CommandSourceStack source) {
        NightAndMoonHelper.setForceBloodMoon(false);
        source.sendSuccess(() -> Component.literal("Luna de Sangre forzada desactivada.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}
