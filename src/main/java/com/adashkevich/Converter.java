package com.adashkevich;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Converter {
    //Pattern vkArticlePattern = Pattern.compile("(https://)?vk.com/@([a-z0-9-]+)");

    public Converter() {

    }

    public boolean convert(String vkArticleUrl) {
        return convert(vkArticleUrl, System.getProperty("java.io.tmpdir"));
    }

    public boolean convert(String vkArticleUrl, String resultPath) {
        try {
            String name = getArticleName(vkArticleUrl);
            Element articleEl = getArticle(vkArticleUrl);
            removeExtraElements(articleEl);
            awayLinksFix(articleEl);
            imagesFix(articleEl);
            save(articleEl, name, resultPath);

        } catch (IOException e) {
            //TODO log exception
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void save(Element articleEl, String name, String path) throws IOException {
        Document template = getTemplate();
        setTitle(template, articleEl);
        setContent(template, articleEl);
        String resultFileName = path + name + ".html";
        try (FileOutputStream out = new FileOutputStream(resultFileName)) {
            out.write(template.html().getBytes("UTF-8"));
        }
        //TODO make logging
        System.out.println(resultFileName);
    }

    private void setTitle(Document template, Element articleEl) {
        String title = articleEl.select("h1").get(0).text();
        Element titleEl = template.select("*:containsOwn({{TITLE}})").get(0);
        titleEl.empty().text(title);
    }

    private void setContent(Document template, Element articleEl) {
        Element contentEl = template.select("*:containsOwn({{CONTENT}})").get(0);
        contentEl.empty().appendChild(articleEl);
    }

    private String getArticleName(String vkArticleUrl) {
        return vkArticleUrl.split("@")[1];
    }

    private Element getArticle(String vkArticleUrl) throws IOException {
        Document doc = Jsoup.connect(vkArticleUrl).get();
        return doc.select(".article").get(0);
    }

    private void removeExtraElements(Element articleEl) {
        articleEl.removeClass("article_mobile");
        articleEl.select(".article__info_line").get(0).remove();
        articleEl.select(".article_bottom_extra_info").get(0).remove();
    }

    private void awayLinksFix(Element articleEl) {
        Elements links = articleEl.select("a");
        for (Element link : links) {
            try {
                String href = link.attr("href");
                if (href.startsWith("/away.php?to=")) {
                    href.replace("/away.php?to=", "");
                    href = URLEncoder.encode(href, "UTF-8");
                    link.attr("href", href);
                }
            } catch (UnsupportedEncodingException e) {
                //TODO log exception
                e.printStackTrace();
            }
        }
    }

    private void imagesFix(Element articleEl) {
        Elements figures = articleEl.select("figure");
        for (Element figure : figures) {
            try {
                Element imgWrapper = figure.select(".article_object_sizer_wrap").get(0);
                SizerSet sizer = new SizerSet(imgWrapper.attr("data-sizes"));
                String srcSet = sizer.toString();
                imgWrapper.attr("data-sizes", "100vw")
                        .attr("data-srcset", srcSet)
                        .addClass("progressive").addClass("replace")
                        .attr("data-href", sizer.biggestImg());
                Element img = imgWrapper.select("img").get(0);
                img.addClass("preview");
                figure.select(".article_figure_content").get(0).removeAttr("style");
                figure.select(".article_figure_sizer").get(0).remove();
            } catch (Exception e) {
                //TODO log exception
                e.printStackTrace();
            }
        }
    }

    class SizerSet {
        private List<Sizer> sizers;
        private final Type intermediateType = new TypeToken<List<Map<Character, List<String>>>>() {}.getType();

        public SizerSet(String sizerSetJson) {
            List<Map<Character, List<String>>> intermediate = new Gson().fromJson(sizerSetJson, intermediateType);
            sizers = new ArrayList<>();
            intermediate.get(0).forEach((k, v) -> sizers.add(new Sizer(k, v)));
        }

        public String biggestImg() {
            Sizer sizer;
            if ((sizer = get('w')) != null || (sizer = get('z')) != null ||
                    (sizer = get('x')) != null || (sizer = get('y')) != null) {
                return sizer.getUrl();
            }
            return "";
        }

        @Override
        public String toString() {
            StringBuilder srcSet = new StringBuilder();
            Sizer sizer;
            if ((sizer = get('w')) != null) {
                srcSet.append(String.format("%s %sw, ", sizer.getUrl(), sizer.getWidth()));
            }
            if ((sizer = get('z')) != null) {
                srcSet.append(String.format("%s %sw, ", sizer.getUrl(), sizer.getWidth()));
            }
            if ((sizer = get('x')) != null) {
                srcSet.append(String.format("%s %sw, ", sizer.getUrl(), sizer.getWidth()));
            }
            if ((sizer = get('y')) != null) {
                srcSet.append(String.format("%s %sw, ", sizer.getUrl(), sizer.getWidth()));
            }
            return srcSet.toString();
        }

        public Sizer get(char size) {
            Optional<Sizer> sizer = sizers.stream().filter(s -> s.getSize() == size).findAny();
            if(sizer.isPresent()) {
                return sizer.get();
            }
            return null;
        }

        class Sizer  {
            private char size;
            private String url;
            private int width;

            Sizer(Character key, List<String> value) {
                size = key;
                url = value.get(0);
                width = Integer.parseInt(value.get(1));
            }

            public Character getSize() {
                return size;
            }

            public String getUrl() {
                return url;
            }

            public int getWidth() {
                return width;
            }
        }
    }

    private Document getTemplate() throws IOException {
        URL url = getClass().getClassLoader().getResource("template.html");
        return Jsoup.parse(new File(URLDecoder.decode(url.getFile(), "UTF-8")), "UTF-8");
    }
}
