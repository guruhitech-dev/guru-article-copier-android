package it.copiaarticolo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArticleParserTest {
    @Test public void extractsSemanticContentAndRemovesJunk() {
        String html = "<html><head><title>Titolo fallback</title></head><body>" +
                "<nav>Menu da eliminare</nav><article><h1>Il titolo</h1>" +
                "<p>Questo è un paragrafo sufficientemente lungo per essere conservato.</p>" +
                "<h2>Una sezione</h2><ul><li>Primo elemento</li><li>Secondo elemento</li></ul>" +
                "<div class='related'><p>Articolo correlato da eliminare completamente.</p></div>" +
                "</article><footer>Footer</footer></body></html>";

        ArticleParser.Article article = ArticleParser.parse(html, "https://example.com/story");
        assertEquals("Il titolo", article.title());
        assertTrue(article.plainText().contains("Una sezione"));
        assertTrue(article.plainText().contains("• Primo elemento"));
        assertFalse(article.plainText().contains("correlato"));
        assertFalse(article.plainText().contains("Menu"));
    }

    @Test public void fallsBackToDocumentTitle() {
        ArticleParser.Article article = ArticleParser.parse(
                "<html><head><title>Titolo documento</title></head><body><main><p>Un testo abbastanza lungo da essere un articolo vero.</p></main></body></html>",
                "https://example.com");
        assertEquals("Titolo documento", article.title());
    }
}
