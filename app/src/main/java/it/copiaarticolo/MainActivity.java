package it.copiaarticolo;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MainActivity extends Activity {
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>]+", Pattern.CASE_INSENSITIVE);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText urlInput;
    private TextView articleText;
    private ProgressBar progress;
    private Button copyButton;
    private Button openButton;
    private String currentUrl;
    private String currentPlainText;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        urlInput = findViewById(R.id.url_input);
        articleText = findViewById(R.id.article_text);
        progress = findViewById(R.id.progress);
        copyButton = findViewById(R.id.copy_button);
        openButton = findViewById(R.id.open_button);

        findViewById(R.id.read_button).setOnClickListener(v -> loadTypedUrl());
        urlInput.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO) { loadTypedUrl(); return true; }
            return false;
        });
        copyButton.setOnClickListener(v -> copyEverything());
        openButton.setOnClickListener(v -> openOriginal());
        handleIntent(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (!Intent.ACTION_SEND.equals(intent.getAction()) || !"text/plain".equals(intent.getType())) return;
        String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
        String url = extractUrl(shared);
        if (url == null) showError("Il contenuto condiviso non contiene un URL web valido.");
        else { urlInput.setText(url); download(url); }
    }

    private void loadTypedUrl() {
        String url = extractUrl(urlInput.getText().toString().trim());
        if (url == null) { showError("Inserisci un URL che inizi con https://"); return; }
        urlInput.setText(url);
        download(url);
    }

    static String extractUrl(String value) {
        if (value == null) return null;
        Matcher matcher = URL_PATTERN.matcher(value);
        if (!matcher.find()) return null;
        String raw = matcher.group().replaceAll("[),.;!?]+$", "");
        try {
            URI uri = URI.create(raw);
            if (uri.getHost() == null || !"https".equalsIgnoreCase(uri.getScheme())) return null;
            return uri.toString();
        } catch (IllegalArgumentException ignored) { return null; }
    }

    private void download(String url) {
        setLoading(true);
        articleText.setText("Download e pulizia dell’articolo…");
        executor.execute(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(15_000);
                connection.setReadTimeout(25_000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) CopiaArticolo/1.0");
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml");
                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) throw new IOException("Risposta HTTP " + status);
                String type = connection.getContentType() == null ? "" : connection.getContentType();
                if (!type.isEmpty() && !type.toLowerCase().contains("html"))
                    throw new IOException("Il link non indica una pagina HTML");
                String body;
                try (InputStream stream = connection.getInputStream()) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
                    body = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
                } finally {
                    connection.disconnect();
                }
                ArticleParser.Article article = ArticleParser.parse(body, url);
                runOnUiThread(() -> showArticle(url, article));
            } catch (Exception error) {
                runOnUiThread(() -> { setLoading(false); showError("Impossibile leggere la pagina: " + error.getMessage()); });
            }
        });
    }

    private void showArticle(String url, ArticleParser.Article article) {
        currentUrl = url;
        currentPlainText = article.plainText();
        SpannableStringBuilder styled = new SpannableStringBuilder();
        appendStyled(styled, article.title(), 1.5f, Typeface.BOLD, false);
        for (ArticleParser.Block block : article.blocks()) {
            boolean list = block.type() == ArticleParser.BlockType.LIST_ITEM;
            float size = block.type() == ArticleParser.BlockType.HEADING_2 ? 1.25f
                    : block.type() == ArticleParser.BlockType.HEADING_3 ? 1.12f : 1f;
            int style = block.type() == ArticleParser.BlockType.PARAGRAPH || list ? Typeface.NORMAL : Typeface.BOLD;
            appendStyled(styled, block.text(), size, style, list);
        }
        articleText.setText(styled);
        copyButton.setEnabled(!currentPlainText.isEmpty());
        openButton.setEnabled(true);
        setLoading(false);
    }

    private static void appendStyled(SpannableStringBuilder out, String text, float size, int style, boolean bullet) {
        if (text == null || text.trim().isEmpty()) return;
        if (out.length() > 0) out.append("\n\n");
        int start = out.length();
        if (bullet) out.append("• ");
        out.append(text.trim());
        out.setSpan(new RelativeSizeSpan(size), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        out.setSpan(new StyleSpan(style), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void copyEverything() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Articolo", currentPlainText));
        Toast.makeText(this, "Articolo copiato", Toast.LENGTH_SHORT).show();
    }

    private void openOriginal() {
        if (currentUrl != null) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)));
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        urlInput.setEnabled(!loading);
    }

    private void showError(String message) {
        articleText.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
