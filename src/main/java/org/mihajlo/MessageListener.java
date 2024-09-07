package org.mihajlo;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mihajlo.SearchChapterAndVerses.searchChapterAndVerses;
import org.mihajlo.models.DataModel;

//import static org.mihajlo.SvetoPismoBot.getReferenceString;
// The method used to be in SvetoPismoBot class for some reason.

public class MessageListener implements MessageCreateListener {

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        String r = "\\(((\\d+\\s)?([А-ШЈЉЊЋЂЏa-шјљњћЂџ]+)\\s)?\\s?(\\d+):(\\d+)(?:-(\\d+))?\\)";
        String messageContent = event.getMessageContent();

        Pattern p = Pattern.compile(r);
        String referenceString = getReferenceString(p, messageContent);

        if (!referenceString.isEmpty()) {

            String[] str = searchChapterAndVerses(referenceString, event);

            // NOTE: I believe this can be optimized better, perhaps i could use OOP on this.


            DataModel dataModel = new DataModel(str[0], str[1], str[2], str[3], str[4], str[5]);

//            String stihovi = str[0];
//            String knjiga = str[1];
//            String glava = str[2];
//            String pocetniStih = str[3];
//            String zavrsniStih = str[4];
//            String prevod = str[5];

            if (!dataModel.getStihovi().isEmpty()) {
                if (!dataModel.getZavrsniStih().isEmpty()) { // If there is the ending verse.
                    if(dataModel.getStihovi().length() > 4096) { // If verses length exceed embed description limit.

                        String regex = "(\\d.+)";

                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(dataModel.getStihovi());


                        String[] versesArray = new String[Integer.parseInt(dataModel.getZavrsniStih()) + 1];
                        int i = Integer.parseInt(dataModel.getPocetniStih());

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

                        // NOTE: The embed builders are kinda repetitive, i'll need to find a way to optimize this and only work with one object of EmbedBuilder class.

                        EmbedBuilder embed = new EmbedBuilder()
                                .setTitle(dataModel.getKnjiga() + " " + dataModel.getGlava() + ":" + dataModel.getPocetniStih() + '-' + dataModel.getZavrsniStih()) // The searched reference
                                .addField("", "**Због ограничења Дискорда текст је исечен.**")
                                .setDescription(versesString) // Verses string
                                .setFooter("Превод: " + dataModel.getPrevod())
                                .setColor(new Color(117, 25, 25));


                        event.getChannel().sendMessage(embed);

                    } else {
                        EmbedBuilder embed = new EmbedBuilder()
                                .setTitle(dataModel.getKnjiga() + " " + dataModel.getGlava() + ":" + dataModel.getPocetniStih() + "-" + dataModel.getZavrsniStih()) // The searched reference
                                .setDescription(dataModel.getStihovi()) // Verses string
                                .setFooter("Превод: " + dataModel.getPrevod())
                                .setColor(new Color(117, 25, 25));

                        event.getChannel().sendMessage(embed);
                    }


                } else { // If there's only a single verse searched up.
                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle(dataModel.getKnjiga() + " " + dataModel.getGlava() + ":" + dataModel.getPocetniStih()) // The searched reference
                            .setDescription(dataModel.getStihovi()) // Verses string
                            .setFooter("Превод: " + dataModel.getPrevod())
                            .setColor(new Color(117, 25, 25));

                    event.getChannel().sendMessage(embed);

                }
            }
        }
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
