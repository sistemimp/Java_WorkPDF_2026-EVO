package mediaprint.normalizza;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import com.itextpdf.kernel.pdf.PdfAConformance;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfOutputIntent;
import com.itextpdf.pdfa.PdfADocument;

/**
 * Utility per la conversione PDF/A e l'aggiunta dei metadati richiesti dalla
 * stampante Riso GL9730.
 */
public final class RisoOptimizer {

        private static final String OUTPUT_INTENT_NAME = "sRGB IEC61966-2.1";

        private RisoOptimizer() {
        }

        public static void optimize(String inputPath, String outputPath, Options options, Consumer<String> logger)
                        throws IOException {
                optimize(inputPath, outputPath, options, logger, null);
        }

        public static void optimize(String inputPath, String outputPath, Options options, Consumer<String> logger,
                        BooleanSupplier cancellationRequested)
                        throws IOException {
                if (options == null || !options.enabled) {
                        return;
                }
                Objects.requireNonNull(inputPath, "inputPath");
                Objects.requireNonNull(outputPath, "outputPath");
                BooleanSupplier isCancelled = cancellationRequested != null ? cancellationRequested : () -> false;
                throwIfCancelled(isCancelled);

                log(logger, String.format(Locale.ITALIAN,
                                "Avvio ottimizzazione Riso GL9730 (RecordID: %s)",
                                options.recordId.isEmpty() ? "-" : options.recordId));

                Path outPath = Paths.get(outputPath);
                Path parent = outPath.getParent();
                if (parent != null && !Files.exists(parent)) {
                        Files.createDirectories(parent);
                }

                PdfOutputIntent intent = createSrgbOutputIntent();
                try (PdfReader reader = new PdfReader(inputPath);
                                PdfWriter writer = new PdfWriter(outputPath)) {
                        try (PdfDocument src = new PdfDocument(reader);
                                        PdfADocument dst = new PdfADocument(writer, PdfAConformance.PDF_A_3B, intent)) {

                                int totalPages = src.getNumberOfPages();
                                for (int page = 1; page <= totalPages; page++) {
                                        throwIfCancelled(isCancelled);
                                        src.copyPagesTo(page, page, dst);
                                }

                                throwIfCancelled(isCancelled);
                                if (options.hasRecordId()) {
                                        dst.getCatalog().put(new PdfName("RecordID"), new PdfString(options.recordId));
                                }
                        }
                }
        }

        private static PdfOutputIntent createSrgbOutputIntent() throws IOException {
                byte[] profile;
                try {
                        profile = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();
                } catch (Exception ex) {
                        throw new IOException("Impossibile caricare il profilo colore sRGB di sistema", ex);
                }
                return new PdfOutputIntent("Custom", "", null, OUTPUT_INTENT_NAME, new ByteArrayInputStream(profile));
        }

        private static void log(Consumer<String> logger, String message) {
                if (logger != null && message != null) {
                        logger.accept(message);
                }
        }

        private static void throwIfCancelled(BooleanSupplier cancellationRequested) {
                if (cancellationRequested != null && cancellationRequested.getAsBoolean()) {
                        throw new CancellationException("Operazione interrotta dall'utente.");
                }
        }

        public static final class Options {
                public final boolean enabled;
                public final String recordId;

                public Options(boolean enabled, String recordId) {
                        this.enabled = enabled;
                        this.recordId = recordId == null ? "" : recordId.trim();
                }

                boolean hasRecordId() {
                        return !recordId.isEmpty();
                }
        }
}
