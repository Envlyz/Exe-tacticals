package com.exetacticals.client.aiStuff;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.exetacticals.GCR;
import com.exetacticals.modularSys.ModuleRegister;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = GCR.MODID, value = Dist.CLIENT)
public final class ModelReloadCommand {

    public static final String COMMAND = "exe_tacticals:reload_model";

    public static final String ARG_MODULAR_ID = "modular_id";

    private ModelReloadCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        if (!GCR.IS_DEVELOPMENT) {
            return;
        }
        event.getDispatcher().register(
                Commands.literal(COMMAND)
                        .then(Commands.argument(ARG_MODULAR_ID, StringArgumentType.greedyString())
                                .suggests(ModelReloadCommand::suggestModuleIds)
                                .executes(context ->
                                        ClientModelHotReloader.reloadModel(resolveModuleId(context)) ? 1 : 0))
        );
    }

    private static String resolveModuleId(CommandContext<CommandSourceStack> context) {
        String raw = StringArgumentType.getString(context, ARG_MODULAR_ID);
        return raw.indexOf(':') >= 0 ? raw : GCR.MODID + ":" + raw;
    }

    private static CompletableFuture<Suggestions> suggestModuleIds(CommandContext<CommandSourceStack> context,
                                                                   SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ModuleRegister.all().keySet(), builder);
    }
}
