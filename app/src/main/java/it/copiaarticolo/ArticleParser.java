package it.copiaarticolo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Turns an HTML document into a small semantic model suitable for Reader Mode. */
public final class ArticleParser {
    private static final String JUNK = "script,style,noscript,iframe,svg,canvas,form,button,input," +
            "nav,aside,footer,[role=navigation],[role=complementary],[role=banner]," +
            ".ad,.ads,.advert,.advertisement,.cookie,.cookies,.consent,.sidebar,.share,.social," +
            ".related,.recommended,.recommendations,.comments,.comment,.footer,.nav,.navigation," +
            "[class*=cookie],[id*=cookie],[class*=advert],[id*=advert],[class*=sidebar]," +
            "[class*=related],[class*=recommend],[class*=comment]";

    private ArticleParser() {}

    public static Article parse(String html, String baseUrl) {
        Document document = Jsoup.parse(html, baseUrl);
        document.select(JUNK).remove();
        Element root = chooseContentRoot(document);
        String title = findTitle(document, root);
        List<Block> blocks = new ArrayList<>();

        for (Element element : root.select("h2,h3,p,li")) {
            if (hasSelectedAncestor(element, root)) continue;
            String text = normalizedText(element);
            if (text.isEmpty()) continue;
            String tag = element.tagName();
            if (tag.equals("p") && text.length() < 25 && looksLikeUi(text)) continue;
            BlockType type = tag.equals("h2") ? BlockType.HEADING_2
                    : tag.equals("h3") ? BlockType.HEADING_3
                    : tag.equals("li") ? BlockType.LIST_ITEM : BlockType.PARAGRAPH;
            blocks.add(new Block(type, text));
        }

        if (blocks.isEmpty()) {
            String fallback = normalizedText(root);
            if (!fallback.isEmpty()) blocks.add(new Block(BlockType.PARAGRAPH, fallback));
        }
        return new Article(title, blocks);
    }

    private static Element chooseContentRoot(Document document) {
        Elements explicit = document.select("article,[role=main],main");
        Element best = null;
        int bestScore = -1;
        Elements candidates = explicit.isEmpty() ? document.select("body,section,div") : explicit;
        for (Element candidate : candidates) {
            int paragraphs = candidate.select("p").size();
            int links = candidate.select("a").text().length();
            int score = candidate.text().length() + paragraphs * 120 - links / 2;
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best != null ? best : document.body();
    }

    private static String findTitle(Document document, Element root) {
        Element heading = root.selectFirst("h1");
        if (heading == null) heading = document.selectFirst("h1");
        if (heading != null && !normalizedText(heading).isEmpty()) return normalizedText(heading);
        String ogTitle = document.select("meta[property=og:title]").attr("content").trim();
        return ogTitle.isEmpty() ? document.title().trim() : ogTitle;
    }

    private static boolean hasSelectedAncestor(Element element, Element root) {
        for (Element parent = element.parent(); parent != null && parent != root; parent = parent.parent()) {
            if (parent.is("h2,h3,p,li")) return true;
        }
        return false;
    }

    private static boolean looksLikeUi(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.matches(".*(accedi|login|menu|condividi|iscriviti|newsletter|privacy|cookie).*" );
    }

    private static String normalizedText(Element element) {
        return element.text().replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }

    public enum BlockType { HEADING_2, HEADING_3, PARAGRAPH, LIST_ITEM }

    public record Block(BlockType type, String text) {}

    public record Article(String title, List<Block> blocks) {
        public String plainText() {
            StringBuilder result = new StringBuilder(title == null ? "" : title.trim());
            for (Block block : blocks) {
                if (result.length() > 0) result.append("\n\n");
                if (block.type == BlockType.LIST_ITEM) result.append("• ");
                result.append(block.text);
            }
            return result.toString();
        }
    }
}

