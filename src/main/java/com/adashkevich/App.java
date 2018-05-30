package com.adashkevich;

public class App {
    public static void main(String[] args) {
        VkArticle article = new VkArticle(args[0]);
        if (args.length > 1) {
            article.toHtml(args[1]);
        } else {
            article.toHtml();
        }
    }
}
