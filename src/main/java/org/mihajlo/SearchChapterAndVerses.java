package org.mihajlo;

import io.github.cdimascio.dotenv.Dotenv;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchChapterAndVerses {

    public static Dotenv dotenv = Dotenv.load();
    public static final String CONN_URL = dotenv.get("CONN_URL");
    public static final String CONN_USER = dotenv.get("CONN_USER");
    public static final String CONN_PASSWORD = dotenv.get("CONN_PASSWORD");

    // Does the actual searching in the database  and returns a String array with verses, chapter, start and end verses and translation.
    // Slash commands method.
    public static String[] searchChapterAndVerses(String reference, SlashCommandInteraction interaction) {



        String[] returnValues = new String[6];

        String verses = ""; // String with verses.
        String bookName = null; // String with name of the book.

        String regex = "^((\\d+\\s)?([А-ШЈЉЊЋЂЏa-шјљњћЂџ]+)\\s)?\\s?(\\d+):(\\d+)(?:-(\\d+))?$";


        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(reference);
        boolean matchFound = matcher.find();

        if(matchFound) {

            String bookSearchString = "";


            String abbreviation = matcher.group(1); // Abbreviation
            if (abbreviation != null) {
                bookSearchString += abbreviation;

                String chapter = matcher.group(4); // Chapter number.
                returnValues[2] = chapter;

                if(chapter != null) {
                    String rawChapterString = null;
                    String queryVersesAndBook = "SELECT glave.stihovi, knjige.knjiga_ime FROM glave JOIN knjige ON glave.knjiga_id = knjige.knjiga_id WHERE glava_broj = ? AND glave.knjiga_id = (SELECT knjiga_id FROM knjige WHERE skracenica = ?)";
                    String queryTranslation = "SELECT prevod_ime FROM knjige JOIN prevodi ON knjige.prevod_id = prevodi.prevod_id WHERE knjiga_id = (SELECT knjiga_id FROM knjige WHERE skracenica = ?)";
                    try {
                        Connection connection = DriverManager.getConnection(CONN_URL, CONN_USER,
                                CONN_PASSWORD);

                        PreparedStatement preparedStatement = connection.prepareStatement(queryVersesAndBook);
                        preparedStatement.setInt(1, Integer.parseInt(chapter));
                        preparedStatement.setString(2, bookSearchString);
                        ResultSet resultSetVerses = preparedStatement.executeQuery();



                        int resultSetCount = 0;

                        if (resultSetVerses.next()) {

                            rawChapterString = resultSetVerses.getString(1); // Raw chapter string.
                            bookName = resultSetVerses.getString(2);

                            returnValues[1] = bookName;
                            resultSetCount++;


                            PreparedStatement preparedStatement2 = connection.prepareStatement(queryTranslation);
                            preparedStatement2.setString(1, bookSearchString);
                            ResultSet resultSetTranslation = preparedStatement2.executeQuery();

                            if (resultSetTranslation.next()) {
                                returnValues[5] = resultSetTranslation.getString(1);
                            }

                        } else {
                            if (resultSetCount == 0) {
                                // If query returned 0 rows.
                                interaction.createImmediateResponder()
                                        .setContent("Грешка: Тражени стихови нису пронађени у бази.")
                                        .setFlags(MessageFlag.EPHEMERAL)
                                        .respond();
                            }
                        }

                    } catch (SQLException e) {
                        System.out.println(e.getMessage());
                    }


                    // FURTHER CHAPTER MANIPULATION BELOW

                    String startingVerse = matcher.group(5); // Start verse.
                    returnValues[3] = startingVerse;

                    if (startingVerse != null) {

                        String endVerse = matcher.group(6); // Optional end verse.
                        returnValues[4] = endVerse;

                        if (endVerse != null) {
                            // If the ending verse is greater than starting verse, if opposite show error.
                            if (Integer.parseInt(endVerse) > Integer.parseInt(startingVerse)) {
                                try {
                                    // If the end verse is actually the last verse in the chapter (if adding one to the index results to out of bounds).
                                    if (rawChapterString.indexOf(String.valueOf(Integer.parseInt(endVerse) + 1) + '.')  == -1) {
                                        // We add an empty verse number after the actual last verse.
                                        rawChapterString += "\n" + (Integer.parseInt(endVerse) + 1) + '.';
                                    }

                                    verses = rawChapterString.substring(rawChapterString.indexOf(startingVerse + '.'), rawChapterString.indexOf(String.valueOf(Integer.parseInt(endVerse) + 1) + '.'));
                                    returnValues[0] = verses;

                                } catch (NullPointerException  | IndexOutOfBoundsException exc) {
                                    System.out.println(exc.getMessage());
                                    interaction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {interactionOriginalResponseUpdater.setContent(exc.getMessage()).setFlags(MessageFlag.EPHEMERAL).update();});
                                }
                            } else {
                                interaction.createImmediateResponder()
                                        .setContent("Грешка: Унос `" + reference + "` је погрешан, молим Вас унесите тачан формат.")
                                        .setFlags(MessageFlag.EPHEMERAL)
                                        .respond();

                            }
                        }
                        else {
                            try {
                                // If the end verse is actually the last verse in the chapter (if adding one to the index results to out of bounds).
                                if (rawChapterString.indexOf(String.valueOf(Integer.parseInt(startingVerse) + 1) + '.') == -1) {
                                    rawChapterString += "\n" + (Integer.parseInt(startingVerse) + 1) + '.';
                                }

                                verses = rawChapterString.substring(rawChapterString.indexOf(String.valueOf(Integer.parseInt(startingVerse)) + '.'), rawChapterString.indexOf(String.valueOf(Integer.parseInt(startingVerse) + 1)));
                                returnValues[0] = verses;

                            } catch (NullPointerException | IndexOutOfBoundsException exc) {
                                System.out.println(exc.getMessage());
                                interaction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {interactionOriginalResponseUpdater.setContent(exc.getMessage()).setFlags(MessageFlag.EPHEMERAL).update();});
                            }
                        }
                    }
                }
            }
        } else {
            // Return an error if the formatting is wrong for all cases.
            interaction.createImmediateResponder()
                    .setContent("Грешка: Унос `" + reference + "` је погрешан, молим Вас унесите тачан формат.")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond();

        }

        return returnValues;
    }


    // For the message listener.
    public static String[] searchChapterAndVerses(String reference, MessageCreateEvent event) {

        String[] returnValues = new String[6];

        String verses = ""; // String with verses.
        String bookName = null; // String with name of the book.

        String regex = "^((\\d+\\s)?([А-ШЈЉЊЋЂЏa-шјљњћЂџ]+)\\s)?\\s?(\\d+):(\\d+)(?:-(\\d+))?$";


        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(reference);
        boolean matchFound = matcher.find();

        if(matchFound) {

            String bookSearchString = "";

            String bookNumber = matcher.group(2);
            if (bookNumber != null) {
                // If there is a number before abbreviation (eg. 1 Мој), add it to the string
                bookSearchString += bookNumber;
            }

            String abbreviation = matcher.group(3); // Abbreviation
            if (abbreviation != null) {
                bookSearchString += abbreviation;

                String chapter = matcher.group(4); // Chapter number.
                returnValues[2] = chapter;

                if(chapter != null) {
                    String rawChapterString = null;
                    String queryVersesAndBook = "SELECT glave.stihovi, knjige.knjiga_ime FROM glave JOIN knjige ON glave.knjiga_id = knjige.knjiga_id WHERE glava_broj = ? AND glave.knjiga_id = (SELECT knjiga_id FROM knjige WHERE skracenica = ?)";
                    String queryTranslation = "SELECT prevod_ime FROM knjige JOIN prevodi ON knjige.prevod_id = prevodi.prevod_id WHERE knjiga_id = (SELECT knjiga_id FROM knjige WHERE skracenica = ?)";
                    try {
                        Connection connection = DriverManager.getConnection(CONN_URL, CONN_USER,
                                CONN_PASSWORD);

                        PreparedStatement preparedStatement = connection.prepareStatement(queryVersesAndBook);
                        preparedStatement.setInt(1, Integer.parseInt(chapter));
                        preparedStatement.setString(2, bookSearchString);
                        ResultSet resultSetVerses = preparedStatement.executeQuery();


                        if (resultSetVerses.next()) {

                            rawChapterString = resultSetVerses.getString(1); // Raw chapter string.
                            bookName = resultSetVerses.getString(2);

                            returnValues[1] = bookName;



                            PreparedStatement preparedStatement2 = connection.prepareStatement(queryTranslation);
                            preparedStatement2.setString(1, bookSearchString);
                            ResultSet resultSetTranslation = preparedStatement2.executeQuery();

                            if (resultSetTranslation.next()) {
                                returnValues[5] = resultSetTranslation.getString(1);
                            }

                        }

                    } catch (SQLException e) {
                        System.out.println(e.getMessage());
                    }


                    // FURTHER CHAPTER MANIPULATION BELOW

                    String startingVerse = matcher.group(5); // Start verse.
                    returnValues[3] = startingVerse;

                    if (startingVerse != null) {

                        String endVerse = matcher.group(6); // Optional end verse.
                        returnValues[4] = endVerse;

                        if (endVerse != null) {
                            if (Integer.parseInt(endVerse) > Integer.parseInt(startingVerse)) {
                                try {
                                    // If the end verse is actually the last verse in the chapter (if adding one to the index results to out of bounds).
                                    if (rawChapterString.indexOf(String.valueOf(Integer.parseInt(endVerse) + 1) + '.')  == -1) {
                                        // We add an empty verse number after the actual last verse.
                                        rawChapterString += "\n" + (Integer.parseInt(endVerse) + 1) + '.';
                                    }

                                    verses = rawChapterString.substring(rawChapterString.indexOf(startingVerse + '.'), rawChapterString.indexOf(String.valueOf(Integer.parseInt(endVerse) + 1) + '.'));
                                    returnValues[0] = verses;

                                } catch (NullPointerException  | IndexOutOfBoundsException exc) {
                                    System.out.println(exc.getMessage());
//                                interaction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {interactionOriginalResponseUpdater.setContent(exc.getMessage()).setFlags(MessageFlag.EPHEMERAL).update();});
                                }
                            }

                        }
                        else {
                            try {
                                // If the end verse is actually the last verse in the chapter (if adding one to the index results to out of bounds).
                                if (rawChapterString.indexOf(String.valueOf(Integer.parseInt(startingVerse) + 1) + '.') == -1) {
                                    rawChapterString += "\n" + (Integer.parseInt(startingVerse) + 1) + '.';
                                }

                                verses = rawChapterString.substring(rawChapterString.indexOf(String.valueOf(Integer.parseInt(startingVerse)) + '.'), rawChapterString.indexOf(String.valueOf(Integer.parseInt(startingVerse) + 1)));
                                returnValues[0] = verses;

                            } catch (NullPointerException | IndexOutOfBoundsException exc) {
                                System.out.println(exc.getMessage());
//                                interaction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {interactionOriginalResponseUpdater.setContent(exc.getMessage()).setFlags(MessageFlag.EPHEMERAL).update();});
                            }
                        }

                    }
                }
            }
        }
        return returnValues;
    }


}
