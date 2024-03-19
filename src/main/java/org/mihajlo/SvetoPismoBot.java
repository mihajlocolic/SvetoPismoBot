package org.mihajlo;

import io.github.cdimascio.dotenv.Dotenv;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOption;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SvetoPismoBot extends SearchChapterAndVerses {

    public static Dotenv dotenv = Dotenv.load();
    public static final String BOT_TOKEN = dotenv.get("BOT_TOKEN");

    public static void main(String[] args) {

        DiscordApi api = new DiscordApiBuilder()
                .setToken(BOT_TOKEN)
                .addIntents(Intent.MESSAGE_CONTENT)
                .login()
                .join();

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

    public static String getReferenceString(Pattern p, String messageContent) {
        Matcher m = p.matcher(messageContent);

        String referenceString = "";

        if(m.find()) {

            String abbreviation = m.group(1); // Abbreviation is group 1 (2 and 3 together are group 1)
            if (abbreviation != null) {
                referenceString += abbreviation;

                String chapter = m.group(4); // Chapter number.
                if (chapter != null) {
                    referenceString += " " + chapter;
                }

                String startingVerse = m.group(5);

                if (startingVerse != null) {
                    referenceString += ':' + startingVerse;
                }

                String endingVerse = m.group(6);
                if (endingVerse != null) {
                    referenceString += '-' + endingVerse;
                }


            }
        }
        return referenceString;
    }
}