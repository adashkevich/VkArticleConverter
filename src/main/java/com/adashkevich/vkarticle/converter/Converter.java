package com.adashkevich.vkarticle.converter;

import com.adashkevich.vkarticle.VkArticle;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.util.*;

public class Converter {
    public static Properties prop;

    public boolean convert(VkArticle article) {
        String tmpDir = System.getProperty("java.io.tmpdir");
        return convert(article, prop.getProperty("output.dir", tmpDir));
    }

    public boolean convert(VkArticle article, String resultPath) {
        try {
            Element articleEl = getArticle(article.getUrl());
            removeExtraElements(articleEl);
            awayLinksFix(articleEl);
            linksSubstitute(articleEl);
            imagesFix(articleEl);
            save(articleEl, article, resultPath);

        } catch (IOException e) {
            //TODO log exception
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void save(Element articleEl, VkArticle article, String path) throws IOException {
        Document template = getTemplate();
        noScriptFix(template);
        setHeaders(template, articleEl, article);
        setContent(template, articleEl);
        String resultFileName = path + article.getName() + ".html";
        try (FileOutputStream out = new FileOutputStream(resultFileName)) {
            out.write(template.html().getBytes("UTF-8"));
        }
        //TODO make logging
        System.out.println(resultFileName);
    }

    private void setHeaders(Document template, Element articleEl, VkArticle article) {
        setTitle(template, articleEl, article);
        setDescription(template, articleEl, article);
        setKeywords(template, articleEl, article);
    }

    private void setTitle(Document template, Element articleEl, VkArticle article) {
        String title = articleEl.select("h1").get(0).text();
        Element titleEl = template.select("*:containsOwn({{TITLE}})").get(0);
        titleEl.empty().text(article.getTitle(title));
    }

    private void setDescription(Document template, Element articleEl, VkArticle article) {
        Element descriptionEl = template.select("meta[name=description]").get(0);
        descriptionEl.attr("content", article.getDescription());
    }

    private void setKeywords(Document template, Element articleEl, VkArticle article) {
        Element keywordEl = template.select("meta[name=keywords]").get(0);
        keywordEl.attr("content", article.getKeywords());
    }


    private void setContent(Document template, Element articleEl) {
        Element contentEl = template.select("*:containsOwn({{CONTENT}})").get(0);
        contentEl.empty().appendChild(articleEl);
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

    //Jsoup bug fix https://github.com/jhy/jsoup/issues/927
    private void noScriptFix(Element html) {
        Elements noScripts = html.select("noscript");
        for (Element noScript : noScripts) {
            String noScriptHtml = Parser.unescapeEntities(noScript.text(), true);
            noScript.html(noScriptHtml);
        }
    }

    private void awayLinksFix(Element articleEl) {
        Elements links = articleEl.select("a");
        for (Element link : links) {
            try {
                String href = URLDecoder.decode(link.attr("href"), "UTF-8");
                if (href.startsWith("/away.php?to=")) {
                    link.attr("href", href.replace("/away.php?to=", ""));
                }
            } catch (UnsupportedEncodingException e) {
                //TODO log exception
                e.printStackTrace();
            }
        }
    }

    private void linksSubstitute(Element articleEl) {
        String linksMapPath = prop.getProperty("links_map.path", "");
        if (!linksMapPath.isEmpty()) {
            try (InputStream is = new FileInputStream(linksMapPath)) {
                Map substituteLinks = new Gson().fromJson(new InputStreamReader(is), Map.class);
                Elements links = articleEl.select("a");
                for (Element link : links) {
                    String href = link.attr("href");
                    if (substituteLinks.containsKey(href)) {
                        link.attr("href", (String)substituteLinks.get(href));
                    }
                }
            } catch (IOException e) {
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
                imgWrapper.attr("data-sizes", "800vw")
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
            if (sizer.isPresent()) {
                return sizer.get();
            }
            return null;
        }

        class Sizer {
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
        InputStream is = getClass().getClassLoader().getResourceAsStream("template.html");
        return Jsoup.parse(is, "UTF-8","");
    }
}
