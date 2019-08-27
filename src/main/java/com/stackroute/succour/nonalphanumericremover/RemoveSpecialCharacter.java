package com.stackroute.succour.nonalphanumericremover;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Stream;

public class RemoveSpecialCharacter
{
    private static Set dict;
    private static boolean found;
    private static String suggestion;
    public static String result() throws IOException {
        File file = new File("/home/cgi/Music/text-cleansing/output.txt");

        String uncleanContent = readFileIntoString(file);

        System.out.println(uncleanContent);

        String cleanContent = cleanTextContent(uncleanContent);

        //System.out.println(cleanContent);
        return cleanContent;
    }

    private static String readFileIntoString(File file)
    {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(Paths.get(file.toURI())))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException e)
        {
            System.out.println("Error reading " + file.getAbsolutePath());
        }
        return contentBuilder.toString();
    }

    private static String cleanTextContent(String text) throws IOException {
        if (text.contains("#")) {
            text = Normalizer.normalize(text, Normalizer.Form.NFKD);
            // strips off all non-ASCII characters
            text = text.replaceAll("[^\\x00-\\x7F]", "").replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").replaceAll("\\p{C}", "");
            ;
            String text1[] = text.split(" ");
            for (int i = 0; i < text1.length; i++) {
                text1[i] = text1[i].replaceAll("[^a-zA-Z]", "").toLowerCase();
            }
            StringTokenizer st1 = new StringTokenizer(text);

            for (int i = 1; st1.hasMoreTokens(); i++)
                System.out.println("Token " + i + ": " + st1.nextToken());

            String line;
            InputStream fis = new FileInputStream("/home/cgi/Music/text-cleansing/dictionary.txt");
            BufferedReader dictReader = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
            //contains all words from file
            //hashset is O(1) or O(log n) time for contains()
            dict = new HashSet();
            while ((line = dictReader.readLine()) != null) {
                //build dictionary
                dict.add(line);
            }
            // Done with the file
            dictReader.close();
            dictReader = null;
            fis = null;
            String mistake;
            //Read misspelled words
            InputStream test = new FileInputStream("/home/cgi/Music/text-cleansing/tokens.txt");
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(test, Charset.forName("UTF-8")));
            while ((mistake = inputReader.readLine()) != null) {
                //reset to allow dictionary search
                found = false;
                suggestion = "NO SUGGESTION";
                //prompt for misspelled word
                System.out.print("Wrong Word > ");
                System.out.println(mistake);
                //never need Upper case characters
                mistake = mistake.toLowerCase();
                //no english words have 3 repeated chars... pretty sure
                mistake = removeTripleRepeat(mistake);
                //find suggestion
                if (checkIfDouble(mistake))
                    checkRepeatLetters(mistake, 0, 0);
                else // don't need to check repeat letters if there are none
                    checkVowels(mistake, 0);
                System.out.println(suggestion);
            }
            inputReader.close();
            inputReader = null;

        }
        return text;
    }
        public static boolean checkIfDouble(String word)
        {
            for(int i = 0;i<word.length() - 1;i++)
            {
                //find next occurrence of double consonant
                if(word.charAt(i) == word.charAt(i+1) && word.charAt(i) != 'a' && word.charAt(i) != 'e' &&
                        word.charAt(i) != 'i' && word.charAt(i) != 'o' && word.charAt(i) != 'u')
                {
                    return true;
                }
            }
            return false;
        }
        /**
         * Reduce any occurrence of 3 repeated characters to 2 repeated characters
         */
        public static String removeTripleRepeat(String s)
        {
            for(int i = 0; i<s.length()-2;i++)
            {
                //test 2 characters in front of i for equality
                if(s.charAt(i) == s.charAt(i+1) && s.charAt(i) == s.charAt(i+2))
                {
                    //found triple,  truncate string
                    s = s.substring(0,i)+s.substring(i+1,s.length());
                    i--; //could be another case of triple at this location
                }
            }
            return s;
        }
        /**
         * search dictionary to see if a word can be a suitable suggestion
         * must be in dictionary to be valid suggestion
         * Only processes one suggestion
         */
        public static boolean checkSuggestion(String word)
        {
            if(!found)
            {
                if (dict.contains(word))
                {
                    suggestion = word;
                    found = true;
                    return true;
                }
                else
                    return false; //word not in dictionary
            }
            else // already found one suggestion
            {
                return false;
            }
        }
        //NOTE: no english word contains more than 3 sets of double letters  ie. bookkeeper
        /**
         * checks each permutation of double letters
         * Should only recurse up to 2 times (ends on 3rd call)
         * @param mistake input string to check
         * @param stringIndex ensures that a double isn't analyzed twice
         * @param recurseCount helps identify base case
         */
        public static void checkRepeatLetters(String mistake, int stringIndex, int recurseCount)
        {
            if (recurseCount >2)
                return;
            //find next double letter
            for(int i = stringIndex;i<mistake.length() - 1;i++)
            {
                //find next occurrence of double consonant
                if(mistake.charAt(i) == mistake.charAt(i+1) && mistake.charAt(i) != 'a' && mistake.charAt(i) != 'e' &&
                        mistake.charAt(i) != 'i' && mistake.charAt(i) != 'o' && mistake.charAt(i) != 'u')
                {
                    //check all vowel possibilities for valid suggestion
                    if(checkVowels(mistake,0))
                        return;
                    //recurse to do all cases of this double letter
                    checkRepeatLetters(mistake,i+1,recurseCount+1);
                    if(!found)// can remove some permutations if suggestion has been found already
                    {
                        //single the double if other case failed
                        String singled = mistake.substring(0,i)+mistake.substring(i+1,mistake.length());
                        if(checkVowels(singled,0))
                            return;
                        //recurse on singled case
                        checkRepeatLetters(singled,i+1,recurseCount+1);
                    }
                }
            }
            return; // should never hit this if dictionary is valid
        }
        //Note: most vowels in a word is 6 for english dictionary(usually)
        /**
         * Takes a word and checks every possible vowel combination with dictionary
         * @param stringIndex should start a 0, helps identify which valid values to check
         * possible to generate more than one suggestion? but don't really care; just needs to be valid
         */
        public static boolean checkVowels(String mistake, int stringIndex)
        {
            //circular list of vowels to allow iteration starting at any of first 5 characters
            String[] vowels = {"a","e","i","o","u","a","e","i","o","u"};
            //find next vowel
            for(int i = stringIndex; i<mistake.length();i++)
            {
                //if char is vowel
                if(mistake.charAt(i) == 'a' || mistake.charAt(i) == 'e' || mistake.charAt(i) == 'i'|| mistake.charAt(i) == 'o'|| mistake.charAt(i) == 'u')
                {
                    //find which vowel
                    for(int j = 0;j<5;j++)
                    {
                        if(mistake.substring(i,i+1).equals(vowels[j]))
                        {
                            int localIndex = j;
                            //swap current vowel to every other vowel and check if match in dictionary
                            //will check current vowel as well
                            for(int k=0;k<5;k++)
                            {
                                //replace vowel
                                mistake = mistake.substring(0,i)+vowels[localIndex]+mistake.substring(i+1,mistake.length());
                                //check if found suggestion
                                if (checkSuggestion(mistake))
                                    return true;
                                //recurse to get every case on every other vowel
                                checkVowels(mistake,i+1);
                                localIndex++;
                            }
                        }
                    }
                }
            }
            //no vowels left and no valid suggestions
            return false;
        }


    }

