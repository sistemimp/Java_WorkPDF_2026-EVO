package font;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;

public class Font {
    private static final List<String> REGULAR_FONT_CANDIDATES = List.of(
            "c:/windows/fonts/ARIAL.ttf",
            "c:/windows/fonts/SEGOEUI.ttf",
            "c:/windows/fonts/CALIBRI.ttf");

    private static final List<String> BOLD_FONT_CANDIDATES = List.of(
            "c:/windows/fonts/ARIALBD.ttf",
            "c:/windows/fonts/SEGOEUIB.ttf",
            "c:/windows/fonts/CALIBRIB.ttf");

    // Cache per documento: evita duplicazioni nel PDF senza riusare oggetti tra PDF diversi.
    private static final Map<PdfDocument, PdfFont> cachedRegularByDocument = new WeakHashMap<>();
    private static final Map<PdfDocument, PdfFont> cachedBoldByDocument = new WeakHashMap<>();

    public static synchronized PdfFont fontHELVETICA(PdfDocument document) throws IOException {
        if (document == null) {
            throw new IllegalArgumentException("PdfDocument non puo' essere null.");
        }
        PdfFont font = cachedRegularByDocument.get(document);
        if (font == null) {
            font = loadUnicodeFont(REGULAR_FONT_CANDIDATES,
                    "Impossibile caricare un font TrueType Unicode regolare.");
            cachedRegularByDocument.put(document, font);
        }
        return font;
    }

    public static synchronized PdfFont fontARIALBold(PdfDocument document) throws IOException {
        if (document == null) {
            throw new IllegalArgumentException("PdfDocument non puo' essere null.");
        }
        PdfFont font = cachedBoldByDocument.get(document);
        if (font == null) {
            font = loadUnicodeFont(BOLD_FONT_CANDIDATES,
                    "Impossibile caricare un font TrueType Unicode bold.");
            cachedBoldByDocument.put(document, font);
        }
        return font;
    }

    public static synchronized PdfFont fontARIAL(PdfDocument document) throws IOException {
        return fontHELVETICA(document);
    }

    private static PdfFont loadUnicodeFont(List<String> candidates, String errorMessage) throws IOException {
        IOException lastError = null;
        for (String candidate : candidates) {
            Path path = Path.of(candidate);
            if (!Files.exists(path)) {
                continue;
            }
            try {
                return PdfFontFactory.createFont(candidate, PdfEncodings.IDENTITY_H);
            } catch (IOException ex) {
                lastError = ex;
            }
        }
        if (lastError != null) {
            throw new IOException(errorMessage + " Ultimo errore: " + lastError.getMessage(), lastError);
        }
        throw new IOException(errorMessage + " Nessun file font trovato nei percorsi noti.");
    }
}
