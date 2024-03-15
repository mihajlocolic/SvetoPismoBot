package org.mihajlo;

import io.github.cdimascio.dotenv.Dotenv;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;

import java.awt.*;
import java.sql.*;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends SearchChapterAndVerses {

    public static Dotenv dotenv = Dotenv.load();

    public static final String BOT_TOKEN = dotenv.get("BOT_TOKEN");
    public static final String CONN_URL = dotenv.get("CONN_URL");
    public static final String CONN_USER = dotenv.get("CONN_USER");
    public static final String CONN_PASSWORD = dotenv.get("CONN_PASSWORD");

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



        // Message listener for getting verses through chat.
        api.addMessageCreateListener(event -> {


            String r = "\\(((\\d+\\s)?([А-ШЈЉЊЋЂЏa-шјљњћЂџ]+)\\s)?\\s?(\\d+):(\\d+)(?:-(\\d+))?\\)";
            String messageContent = event.getMessageContent();

            Pattern p = Pattern.compile(r);
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

            if (!referenceString.isEmpty()) {

                String[] str = searchChapterAndVerses(referenceString, event);
                String stihovi = str[0];
                String knjiga = str[1];
                String glava = str[2];
                String pocetniStih = str[3];
                String zavrsniStih = str[4];
                String prevod = str[5];


                if (stihovi != null) {
                    if (zavrsniStih != null) { // If there is the ending verse.
                        if(stihovi.length() > 4096) { // If verses length exceed embed description limit.

                            String regex = "(\\d.+)";

                            Pattern pattern = Pattern.compile(regex);
                            Matcher matcher = pattern.matcher(stihovi);


                            String[] versesArray = new String[Integer.parseInt(zavrsniStih) + 1];
                            int i = Integer.parseInt(pocetniStih);

                            while (matcher.find()) {
                                // Adding verses one by one in the array.
                                versesArray[i] = matcher.group(1);
                                i++;
                            }


                            String versesString = "";
                            int maxLengthTracker = 0;

                            for (String s : versesArray) {
                                if (s != null) {
                                    if (maxLengthTracker + s.length() > 4093) {
                                        break;
                                    }
                                    versesString = versesString.concat(s + '\n');
                                    maxLengthTracker += s.length();
                                }

                            }


                            EmbedBuilder embed = new EmbedBuilder()
                                    .setTitle(knjiga + " " + glava + ":" + pocetniStih + '-' + zavrsniStih) // The searched reference
                                    .addField("", "**Због ограничења Дискорда текст је исечен.**")
                                    .setDescription(versesString) // Verses string
                                    .setFooter("Превод: " + prevod)
                                    .setColor(new Color(117, 25, 25));


                            event.getChannel().sendMessage(embed);

                        } else {
                            EmbedBuilder embed = new EmbedBuilder()
                                    .setTitle(knjiga + " " + glava + ":" + pocetniStih + "-" + zavrsniStih) // The searched reference
                                    .setDescription(stihovi) // Verses string
                                    .setFooter("Превод: " + prevod)
                                    .setColor(new Color(117, 25, 25));

                            event.getChannel().sendMessage(embed);
                        }


                    } else { // If there's only a single verse searched up.
                        EmbedBuilder embed = new EmbedBuilder()
                                .setTitle(knjiga + " " + glava + ":" + pocetniStih) // The searched reference
                                .setDescription(stihovi) // Verses string
                                .setFooter("Превод: " + prevod)
                                .setColor(new Color(117, 25, 25));

                        event.getChannel().sendMessage(embed);

                    }
                }
            }


        });


        // Slash command listener for all the commands below.

        api.addSlashCommandCreateListener(event -> {

            SlashCommandInteraction interaction = event.getSlashCommandInteraction();


            if(interaction.getFullCommandName().equals("помоћ")) {

                EmbedBuilder embed = new EmbedBuilder()
                        .addField("Форматирање", "Да бисте користили бот потребно је да по формату (Јн 1:1-2), унесете текст у поруку (са све заградама) или унесете текст (без заграда) у поље команде.\nЗа листу свих скраћеница користите команду `/скраћенице`.")
                        .setColor(new Color(117, 25, 25));
                event.getInteraction().respondLater()
                        .thenAccept(interactionOriginalResponseUpdater -> interactionOriginalResponseUpdater.addEmbed(embed).update());


            } else if(interaction.getFullCommandName().equals("стих")) {

                Optional<String> tekst = interaction.getArgumentStringValueByName("претрага");
                tekst.ifPresent(s -> {
                    String[] str = searchChapterAndVerses(s, interaction);
                    String stihovi = str[0];
                    String knjiga = str[1];
                    String glava = str[2];
                    String pocetniStih = str[3];
                    String zavrsniStih = str[4];
                    String prevod = str[5];


                    if (stihovi != null) {
                        if (zavrsniStih != null) { // If there is the ending verse.
                            if(stihovi.length() > 4096) { // If verses length exceed embed description limit.

                                String regex = "(\\d.+)";

                                Pattern pattern = Pattern.compile(regex);
                                Matcher matcher = pattern.matcher(stihovi);


                                String[] versesArray = new String[Integer.parseInt(zavrsniStih) + 1];
                                int i = Integer.parseInt(pocetniStih);

                                while (matcher.find()) {
                                    // Adding verses one by one in the array.
                                    versesArray[i] = matcher.group(1);
                                    i++;
                                }


                                String versesString = "";
                                int maxLengthTracker = 0;

                                for (String string : versesArray) {

                                    if (string != null) {
                                        if (maxLengthTracker + string.length() > 4093) {
                                            break;
                                        }

                                        versesString = versesString.concat(string + '\n');
                                        maxLengthTracker += string.length();
                                    }

                                }

                                EmbedBuilder firstPart = new EmbedBuilder()
                                        .setTitle(knjiga + " " + glava + ":" + pocetniStih + '-' + zavrsniStih) // The searched reference
                                        .addField("", "**Због ограничења Дискорда текст је исечен.**")
                                        .setDescription(versesString) // Verses string
                                        .setFooter("Превод: " + prevod)
                                        .setColor(new Color(117, 25, 25));


                                event.getInteraction().respondLater()
                                        .thenAccept(interactionOriginalResponseUpdater -> {
                                            interactionOriginalResponseUpdater.addEmbed(firstPart).update();
                                        });

                            } else {
                                EmbedBuilder embed = new EmbedBuilder()
                                        .setTitle(knjiga + " " + glava + ":" + pocetniStih + "-" + zavrsniStih) // The searched reference
                                        .setDescription(stihovi) // Verses string
                                        .setFooter("Превод: " + prevod)
                                        .setColor(new Color(117, 25, 25));

                                interaction.respondLater()
                                        .thenAccept(interactionOriginalResponseUpdater -> {
                                            interactionOriginalResponseUpdater.addEmbed(embed).update();
                                        });
                            }


                        } else { // If there's only a single verse searched up.
                            EmbedBuilder embed = new EmbedBuilder()
                                    .setTitle(knjiga + " " + glava + ":" + pocetniStih) // The searched reference
                                    .setDescription(stihovi) // Verses string
                                    .setFooter("Превод: " + prevod)
                                    .setColor(new Color(117, 25, 25));

                            interaction.respondLater()
                                    .thenAccept(interactionOriginalResponseUpdater -> {
                                        interactionOriginalResponseUpdater.addEmbed(embed).update();
                                    });

                        }
                    }

                });
            }  else if (interaction.getFullCommandName().equals("скраћенице")) {



                try {
                    Connection conn = DriverManager.getConnection(CONN_URL, CONN_USER, CONN_PASSWORD);

                    String query = "SELECT knjiga_ime, skracenica FROM knjige";

                    PreparedStatement prepStmt = conn.prepareStatement(query);
                    ResultSet rs = prepStmt.executeQuery();

                    String skracenice = "\n**Књиге Старог Завета**\n";
                    int rsCount = 0;

                    while(rs.next()) {
                        if (rsCount == 39) {
                            skracenice += "\n**Књиге Новог Завета**\n";
                        }
                        skracenice += rs.getString(1) + " - " + '`' + rs.getString(2) + '`' + '\n';
                        rsCount++;
                    }

                    EmbedBuilder embed = new EmbedBuilder()
                            .setColor(new Color(117, 25, 25))
                            .setDescription(skracenice);

                    interaction.respondLater()
                            .thenAccept(interactionOriginalResponseUpdater -> {
                                interactionOriginalResponseUpdater.addEmbed(embed).update();
                            });


                    conn.close();

                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }



            }
        });
    }
}