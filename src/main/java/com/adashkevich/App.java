package com.adashkevich;

import com.adashkevich.vkarticle.VkArticle;
import com.adashkevich.vkarticle.converter.Converter;

import java.io.*;
import java.util.Properties;

public class App {

    public static void main(String[] args) {
        loadConverterProperties(args);
        try {
            VkArticle article = VkArticle.loadFromJsonFile(args[0]);
            article.toHtml();
        } catch (IOException e) {
            System.out.println(String.format("Could not load article from %s", args[0]));
        }
    }

    private static void loadConverterProperties(String[] args) {
        Properties prop = new Properties();
        if (args.length > 1) {
            try {
                InputStream is = new FileInputStream(args[1]);
                prop.load(is);
            } catch (IOException e) {
                //TODO log exception
                System.out.println("Error occurred on .properties file reading");
            }
        }
        Converter.prop = prop;
    }
}
