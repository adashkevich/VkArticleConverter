package com.adashkevich.vkarticle;

import com.adashkevich.vkarticle.converter.Converter;
import com.google.gson.Gson;
import java.io.*;

public class VkArticle {
    private String url;
    private String title;
    private String description;
    private String keywords;
    private Converter converter = new Converter();

    public String getUrl() {
        return url;
    }

    public String getTitle(String defaultTitle) {
        return title != null ? title : defaultTitle;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public String getKeywords() {
        return keywords != null ? keywords : "";
    }

    public String getName() {
        return url.split("@")[1];
    }

    public boolean toHtml() {
        return converter.convert(this);
    }

    public boolean toHtml(String resultPath) {
        return converter.convert(this, resultPath);
    }

    public static VkArticle loadFromJsonFile(String jsonFilePath) throws IOException {
        try (InputStream is = new FileInputStream(jsonFilePath)) {
            return new Gson().fromJson(new InputStreamReader(is), VkArticle.class);
        }
    }
}
