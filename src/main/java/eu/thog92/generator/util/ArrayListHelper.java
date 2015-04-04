package eu.thog92.generator.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ArrayListHelper
{

    public static ArrayList<String> loadStringArrayFromFile(String file)
            throws IOException
    {

        ArrayList<String> tmp = new ArrayList<>();
        BufferedReader fileIn = new BufferedReader(new FileReader(file));

        String entry;

        while ((entry = fileIn.readLine()) != null)
        {
            tmp.add(entry);
        }
        fileIn.close();
        return tmp;
    }
}
