package com.charmonium.mixin;

import com.charmonium.Charmonium;
import com.charmonium.CharmoniumClient;
import com.charmonium.command.Command;
import com.charmonium.managers.CommandManager;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatInputSuggestor.class)
public abstract class ChatInputSuggestorMixin {
    @Shadow
    private TextFieldWidget textField;
    @Shadow
    @Nullable
    private ParseResults<CommandSource> parse;
    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow
    private List<OrderedText> messages;

    @Shadow
    public abstract void show(boolean narrateFirstSuggestion);

    @Inject(at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getCursor()I",
            ordinal = 0),
            method = "refresh()V",
            cancellable = true)
    private void onRefresh(CallbackInfo ci) {
        String prefix = CommandManager.PREFIX.getValue();
        String fullInput = textField.getText();
        int cursorPos = textField.getCursor();

        if (fullInput.startsWith(prefix)) {
            ci.cancel(); // Prevent default suggestions
            String withoutPrefix = fullInput.substring(prefix.length()).trim();
            List<String> args = Arrays.asList(withoutPrefix.split("\\s+", -1));

            // Calculate suggestion start position
            int lastSpace = fullInput.lastIndexOf(' ', cursorPos - 1);
            int suggestionStart = lastSpace == -1 ? prefix.length() : lastSpace + 1;

            SuggestionsBuilder builder = new SuggestionsBuilder(fullInput, suggestionStart);

            if (args.isEmpty()) {
                // Suggest all commands when only prefix is present
                Charmonium.getInstance().commandManager.getCommands().keySet()
                        .forEach(cmd -> builder.suggest(cmd));
            } else {
                String currentArg = args.get(args.size() - 1);

                if (args.size() == 1) {
                    // Command name suggestions
                    Charmonium.getInstance().commandManager.getCommands().keySet()
                            .stream()
                            .filter(cmd -> cmd.startsWith(currentArg))
                            .forEach(cmd -> builder.suggest(cmd));
                } else {
                    // Argument suggestions
                    Command cmd = Charmonium.getInstance().commandManager.getCommandBySyntax(args.get(0));
                    if (cmd != null) {
                        String[] suggestions = cmd.getAutocorrect(currentArg);
                        if (suggestions != null) {
                            Arrays.stream(suggestions).forEach(s -> builder.suggest(s));
                        }
                    }
                }
            }

            pendingSuggestions = builder.buildFuture();
            show(false);
        }
    }
}