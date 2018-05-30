package com.adashkevich;

public class VkArticle {
    private String url;

    private Converter converter = new Converter();

    public VkArticle(String vkArticleUrl) {
        url = vkArticleUrl;
    }

    public boolean toHtml() {
        return converter.convert(url);
    }

    public boolean toHtml(String resultPath) {
        return converter.convert(url, resultPath);
    }

}
