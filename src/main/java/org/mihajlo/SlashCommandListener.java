package org.mihajlo;

import io.github.cdimascio.dotenv.Dotenv;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

import java.awt.*;
import java.sql.*;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mihajlo.SearchChapterAndVerses.searchChapterAndVerses;

public class SlashCommandListener implements SlashCommandCreateListener {

    public static Dotenv dotenv = Dotenv.load();
    public static final String CONN_URL = dotenv.get("CONN_URL");
    public static final String CONN_USER = dotenv.get("CONN_USER");
    public static final String CONN_PASSWORD = dotenv.get("CONN_PASSWORD");

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
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
                                    .thenAccept(interactionOriginalResponseUpdater -> interactionOriginalResponseUpdater.addEmbed(firstPart).update());

                        } else {
                            EmbedBuilder embed = new EmbedBuilder()
                                    .setTitle(knjiga + " " + glava + ":" + pocetniStih + "-" + zavrsniStih) // The searched reference
                                    .setDescription(stihovi) // Verses string
                                    .setFooter("Превод: " + prevod)
                                    .setColor(new Color(117, 25, 25));

                            interaction.respondLater()
                                    .thenAccept(interactionOriginalResponseUpdater -> interactionOriginalResponseUpdater.addEmbed(embed).update());
                        }


                    } else { // If there's only a single verse searched up.
                        EmbedBuilder embed = new EmbedBuilder()
                                .setTitle(knjiga + " " + glava + ":" + pocetniStih) // The searched reference
                                .setDescription(stihovi) // Verses string
                                .setFooter("Превод: " + prevod)
                                .setColor(new Color(117, 25, 25));

                        interaction.respondLater()
                                .thenAccept(interactionOriginalResponseUpdater -> interactionOriginalResponseUpdater.addEmbed(embed).update());

                    }
                }

            });
        }  else if (interaction.getFullCommandName().equals("скраћенице")) {



            try {
                Connection conn = DriverManager.getConnection(CONN_URL, CONN_USER, CONN_PASSWORD);

                String query = "SELECT knjiga_ime, skracenica FROM knjige";

                PreparedStatement prepStmt = conn.prepareStatement(query);
                ResultSet rs = prepStmt.executeQuery();

                StringBuilder skracenice = new StringBuilder("\n**Књиге Старог Завета**\n");
                int rsCount = 0;

                while(rs.next()) {
                    if (rsCount == 39) {
                        skracenice.append("\n**Књиге Новог Завета**\n");
                    }
                    skracenice.append(rs.getString(1)).append(" - ").append('`').append(rs.getString(2)).append("`\n");
                    rsCount++;
                }

                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(new Color(117, 25, 25))
                        .setDescription(skracenice.toString());

                interaction.respondLater()
                        .thenAccept(interactionOriginalResponseUpdater -> interactionOriginalResponseUpdater.addEmbed(embed).update());


                conn.close();

            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
