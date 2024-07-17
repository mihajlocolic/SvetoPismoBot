package org.mihajlo;

import io.github.cdimascio.dotenv.Dotenv;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOption;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SvetoPismoBot extends SearchChapterAndVerses {

    public static Dotenv dotenv = Dotenv.load();
    public static final String BOT_TOKEN = dotenv.get("BOT_TOKEN");

    private static final Logger logger = LogManager.getLogger(SvetoPismoBot.class);

    public static void main(String[] args) {

        DiscordApi api = new DiscordApiBuilder()
                .setToken(BOT_TOKEN)
                .addIntents(Intent.MESSAGE_CONTENT)
                .login()
                .join();

        logger.trace("Bot is up and running.");

        api.setReconnectDelay(attempt -> attempt * 2);

        Set<SlashCommandBuilder> builders = new HashSet<>();

        builders.add(new SlashCommandBuilder().setName("помоћ").setDescription("Помоћне информације за употребу бота."));
        List<SlashCommandOption> options = new ArrayList<>();
        options.add(SlashCommandOption.createStringOption("претрага", "Претражи стихове у Светом Писму.", true));
        builders.add(new SlashCommandBuilder().setName("стих").setDescription("Претрага стихова у Светом Писму.").setOptions(options));
        builders.add(new SlashCommandBuilder().setName("скраћенице").setDescription("Листа свих скраћеница књига које су доступне."));

        api.bulkOverwriteGlobalApplicationCommands(builders).join();

        // Slash command listener for all the commands above.
        api.addListener(new SlashCommandListener());

        // Message listener for getting verses through chat.
        api.addListener(new MessageListener());

    }
}