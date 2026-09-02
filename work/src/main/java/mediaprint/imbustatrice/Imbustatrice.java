package mediaprint.imbustatrice;

import com.itextpdf.barcodes.Barcode1D;
import com.itextpdf.barcodes.Barcode128;
import com.itextpdf.barcodes.BarcodeInter25;
import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.barcodes.qrcode.EncodeHintType;
import com.itextpdf.barcodes.qrcode.ErrorCorrectionLevel;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Image;
import com.itextpdf.barcodes.Barcode39;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import com.google.zxing.datamatrix.encoder.SymbolShapeHint;

import font.Font;

import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

/** Utility: barcode imbustatrice, reticolo, resize contenuto. */
public final class Imbustatrice {
    private static final Pattern COMBINING_MARKS_PATTERN = Pattern.compile("\\p{M}+");
    private static final Pattern HORIZONTAL_SPACES_PATTERN = Pattern.compile("[ \\t\\x0B\\f\\r]+");
    private static final int PDF_COMPRESSION_LEVEL = 6;

    private Imbustatrice() {
    }

    /*
     * ===========================
     * === CONFIG & HELPERS ===
     * ===========================
     */

    /** mm ? pt (1 mm ? 2.8346457 pt). */
    public static float mm(float mm) {
        return mm * 2.8346457f;
    }

    private static String twoDigits(int n) {
        return (n < 10 ? "0" : "") + n;
    }

    private static String sixDigits(int n) {
        String s = String.valueOf(n);
        StringBuilder sb = new StringBuilder();
        for (int i = s.length(); i < 6; i++)
            sb.append('0');
        sb.append(s);
        return sb.toString();
    }

    private static long maxValueForDigits(int digits) {
        long value = 0L;
        for (int i = 0; i < digits; i++) {
            value = (value * 10L) + 9L;
        }
        return value;
    }

    /** Valori standard con cui generare il barcode 39 in verticale. */
    public static final class BarcodeStandard {
        public static final float BAR_HEIGHT_PT = 15f;
        public static final float MODULE_WIDTH_PT = 1.2f;
        public static final float FONT_SIZE_PT = 6f;
        public static final float ROTATION_DEG = 90f;
        public static final float Y_OFFSET_PT = 20f;
        public static final int TEXT_ALIGNMENT = Barcode1D.ALIGN_CENTER;
        public static final float TARGET_WIDTH_MM = 40f;
        public static final float TARGET_WIDTH_PT = Imbustatrice.mm(TARGET_WIDTH_MM);
        public static final float LABEL_FONT_SIZE_PT = 9f;

        private BarcodeStandard() {
        }
    }

    /** Valori standard per le tacche OMR (lettura ottica). */
    public static final class OmrStandard {
        public static final float CM_TO_PT = 28.3465f;
        public static final float BAR_THICKNESS_PT = 0.027f * CM_TO_PT;
        public static final float BAR_LENGTH_PT = 0.64f * CM_TO_PT;
        public static final int BIT_COUNT = 7;
        public static final int MC_BITS = 3;
        public static final int SEL_BITS = 1;
        public static final int BIT_COUNT_WITH_MC = BIT_COUNT + MC_BITS + SEL_BITS;
        public static final float EXTENDED_PATTERN_Y_SHIFT_PT = 2f * CM_TO_PT;
        public static final int SHEET_BITS = 3;
        public static final float ROW_SPACING_PT = 0.4233f * CM_TO_PT;
        public static final float START_X_PT = 0.5f * CM_TO_PT;
        public static final float START_Y_PT = 5.7f * CM_TO_PT;

        private OmrStandard() {
        }
    }

    /** Valori standard per il barcode Raccomandata (ITF 2/5). */
    public static final class RaccomandataStandard {
        public static final int IDENTIFIER_DIGITS = 11;
        public static final float NARROW_MODULE_MM = 0.30f;
        public static final float BAR_HEIGHT_MM = 14f;
        public static final float WIDE_TO_NARROW_RATIO = 2.5f;
        public static final float HUMAN_READABLE_FONT_MM = 3f;
        public static final float HUMAN_READABLE_GAP_MM = 2f;
        public static final float PREFIX_GAP_MM = 5.5f;
        public static final float PREFIX_FONT_MM = 4f;
        public static final float OMOLOGAZIONE_FONT_MM = 2.5f;
        public static final float CUSTOM_TEXT_FONT_MM = 2.5f;

        public static final float NARROW_MODULE_PT = Imbustatrice.mm(NARROW_MODULE_MM);
        public static final float BAR_HEIGHT_PT = Imbustatrice.mm(BAR_HEIGHT_MM);
        public static final float HUMAN_READABLE_FONT_PT = Imbustatrice.mm(HUMAN_READABLE_FONT_MM);
        public static final float HUMAN_READABLE_GAP_PT = Imbustatrice.mm(HUMAN_READABLE_GAP_MM);
        public static final float PREFIX_GAP_PT = Imbustatrice.mm(PREFIX_GAP_MM);
        public static final float PREFIX_FONT_PT = Imbustatrice.mm(PREFIX_FONT_MM);
        public static final float OMOLOGAZIONE_FONT_PT = Imbustatrice.mm(OMOLOGAZIONE_FONT_MM);
        public static final float CUSTOM_TEXT_FONT_PT = Imbustatrice.mm(CUSTOM_TEXT_FONT_MM);

        private RaccomandataStandard() {
        }
    }

    /*
     * ===========================
     * === BARCODE OPTIONS ===
     * ===========================
     */

    /** Parametri per il barcode imbustatrice. */
    public static class BarcodeOpts {
        public boolean enabled; // abilita stampa barcode
        public boolean useOmr; // usa tacche OMR in alternativa al C39
        public boolean allegatiPresenti; // se sono presenti allegati
        public boolean targetPdfIsAttachment; // se il PDF su cui si disegna il barcode ? un allegato
        public float posXpt; // posizione X (pt) dal bordo sinistro
        public float posYpt; // posizione Y (pt) dal bordo basso (ancora dell'immagine ruotata)
        public float widthPt; // larghezza del code128 (prima della rotazione)
        public float heightPt; // altezza barre
        public float moduleWidthPt; // larghezza modulo stretto (pt)
        public int groupStartProgressive; // progressivo gruppo iniziale (>=1)
        public boolean labelEnabled; // abilita stampa etichetta gruppo
        public float labelPosXpt; // posizione X etichetta
        public float labelPosYpt; // posizione Y etichetta
        public boolean labelVertical; // orientamento etichetta (true=verticale)
        public String lavorazioneId; // ID lavorazione da UI
        public float labelFontSizePt; // corpo testo etichetta

        public BarcodeOpts(boolean enabled, boolean allegatiPresenti, boolean targetPdfIsAttachment,
                float posXpt, float posYpt,
                float widthPt, float heightPt,
                float moduleWidthPt,
                int groupStartProgressive) {
            this(enabled, false, allegatiPresenti, targetPdfIsAttachment, posXpt, posYpt, widthPt, heightPt,
                    moduleWidthPt,
                    groupStartProgressive,
                    false, posXpt, posYpt, false, "", 7f);
        }

        public BarcodeOpts(boolean enabled, boolean allegatiPresenti, boolean targetPdfIsAttachment,
                float posXpt, float posYpt,
                float widthPt, float heightPt,
                float moduleWidthPt,
                int groupStartProgressive,
                boolean labelEnabled,
                float labelPosXpt,
                float labelPosYpt,
                boolean labelVertical,
                String lavorazioneId,
                float labelFontSizePt) {
            this(enabled, false, allegatiPresenti, targetPdfIsAttachment, posXpt, posYpt, widthPt, heightPt,
                    moduleWidthPt, groupStartProgressive,
                    labelEnabled, labelPosXpt, labelPosYpt, labelVertical, lavorazioneId, labelFontSizePt);
        }

        public BarcodeOpts(boolean enabled, boolean useOmr, boolean allegatiPresenti, boolean targetPdfIsAttachment,
                float posXpt, float posYpt,
                float widthPt, float heightPt,
                float moduleWidthPt,
                int groupStartProgressive,
                boolean labelEnabled,
                float labelPosXpt,
                float labelPosYpt,
                boolean labelVertical,
                String lavorazioneId,
                float labelFontSizePt) {
            this.enabled = enabled;
            this.useOmr = useOmr;
            this.allegatiPresenti = allegatiPresenti;
            this.targetPdfIsAttachment = targetPdfIsAttachment;
            this.posXpt = posXpt;
            this.posYpt = posYpt;
            this.widthPt = Math.max(1f, widthPt);
            this.heightPt = Math.max(1f, heightPt);
            this.moduleWidthPt = moduleWidthPt > 0f ? moduleWidthPt : BarcodeStandard.MODULE_WIDTH_PT;
            this.groupStartProgressive = Math.max(1, groupStartProgressive);
            this.labelEnabled = labelEnabled;
            this.labelPosXpt = labelPosXpt;
            this.labelPosYpt = labelPosYpt;
            this.labelVertical = labelVertical;
            this.lavorazioneId = lavorazioneId == null ? "" : lavorazioneId.trim();
            this.labelFontSizePt = labelFontSizePt > 0f ? labelFontSizePt : 7f;
        }
    }

    /*
     * =======================================================
     * === BARCODE: APPLICA A ULTIMO GRUPPO APPENA COPIATO ===
     * =======================================================
     */

    /**
     * Applica i barcode imbustatrice alle pagine dispari (fronti) dell'ULTIMO
     * gruppo
     * appena copiato nel documento di destinazione.
     *
     * @param dst                   PdfDocument di destinazione (gi? aperto)
     * @param dstStartIndex         numero di pagina 1-based della PRIMA pagina del
     *                              gruppo appena copiato
     * @param copiedCount           quante pagine ha il gruppo in dst (inclusa
     *                              eventuale bianca)
     * @param groupIndex            indice del gruppo corrente (1..N)
     * @param groupStartProgressive progressivo gruppo iniziale (per gli ultimi 6
     *                              digit)
     * @param opts                  opzioni barcode
     */
    public static void applyBarcodesToGroup(
            PdfDocument dst,
            int dstStartIndex,
            int copiedCount,
            int groupIndex,
            int groupStartProgressive,
            BarcodeOpts opts) {
        if (opts == null || copiedCount <= 0)
            return;

        // Il progressivo di gruppo visualizzato nelle ultime 6 cifre e' calcolato
        // rispetto al progressivo iniziale.
        final int groupProgressive = Math.max(1, groupStartProgressive) + (groupIndex - 1);

        // Supporta etichetta gruppo anche quando il barcode e' disabilitato.
        if (!opts.enabled) {
            maybeRenderGroupLabel(dst, dstStartIndex, groupProgressive, opts);
            return;
        }

        // Ogni due pagine del gruppo corrispondono al fronte/retro di un singolo
        // foglio.
        final int totalSheets = (copiedCount + 1) / 2; // 2 pagine = 1 foglio
        final String groupProg6 = sixDigits(groupProgressive);

        for (int j = 0; j < copiedCount; j++) {
            // Solo dispari nel gruppo (fronti)
            if (((j + 1) % 2) == 0)
                continue;

            final int dstPageNo = dstStartIndex + j;
            final int sheetIndex = (j / 2) + 1; // 1..totalSheets
            final int globalSheetIndex = (dstPageNo / 2) + 1;

            if (opts.useOmr) {
                boolean isLastFrontPage = (j >= Math.max(0, copiedCount - 2));
                String omrBinary = buildOmrBinary(dstPageNo, isLastFrontPage, groupProgressive, opts);
                renderOmrMarks(dst, dstPageNo, omrBinary, opts);
            } else {
                // Digit 1
                // Valore che identifica la posizione del foglio all'interno del gruppo
                // (inizio, fine, intermedio o singolo foglio).
                char d1;
                if (totalSheets == 1)
                    d1 = '3';
                else if (sheetIndex == 1)
                    d1 = '1';
                else if (sheetIndex == totalSheets)
                    d1 = '2';
                else
                    d1 = '0';

                // Digit 2
                // Indica la presenza di allegati sull'ultimo foglio del gruppo.
                char d2 = (opts.allegatiPresenti && sheetIndex == totalSheets) ? '1' : '0';

                // Digit 3-4 (01..99)
                // Progressivo pagina a due cifre; puo' riferirsi al gruppo corrente o
                // all'intero
                // documento.
                int sequenceBase = opts.targetPdfIsAttachment ? sheetIndex : globalSheetIndex;
                sequenceBase = Math.max(1, sequenceBase);
                int mod = ((sequenceBase - 1) % 99) + 1;
                String d34 = twoDigits(mod);

                String code = "" + d1 + d2 + d34 + groupProg6;
                renderBarcode39Vertical(dst, dstPageNo, code, opts);
            }
            if (j == 0) {
                maybeRenderGroupLabel(dst, dstPageNo, groupProgressive, opts);
            }
        }
    }

    /**
     * Disegna un Code128 verticale (ruotato 90?) alla posizione/dimensione
     * specificate.
     */
    public static void renderBarcode128Vertical(PdfDocument dst, int pageNo, String code, BarcodeOpts opts) {
        Barcode128 bc = new Barcode128(dst);
        bc.setCodeType(Barcode128.CODE128);
        bc.setCode(code);
        // Altezza della barra originale prima della rotazione; controlla la lunghezza
        // del codice.
        bc.setBarHeight(opts.heightPt);

        Image img = new Image(bc.createFormXObject(dst));
        img.setAutoScale(false);
        img.setWidth(opts.widthPt);
        // L'altezza impostata qui coincide con la lunghezza del codice una volta
        // ruotato.
        img.setHeight(opts.heightPt);

        // Rotazione +90? (verticale).
        img.setRotationAngle((float) Math.PI / 2f);
        // Posiziona il codice ruotato rispetto al bordo inferiore della pagina.
        img.setFixedPosition(pageNo, opts.posXpt, opts.posYpt);

        // ? iText 7.2+ : costruttore Canvas(PdfCanvas, Rectangle)
        PdfPage page = dst.getPage(pageNo);
        ensureIgnorePageRotationForNewContent(page);
        Rectangle pageSize = page.getPageSize();
        Canvas canvas = new Canvas(new PdfCanvas(page), pageSize);
        canvas.add(img);
        canvas.close();
    }

    private static void maybeRenderGroupLabel(PdfDocument dst, int pageNo, int groupProgressive, BarcodeOpts opts) {
        // L'etichetta gruppo ? opzionale: si disegna solo quando abilitata.
        if (!hasLabel(opts)) {
            return;
        }
        String text = formatGroupLabel(groupProgressive, opts.lavorazioneId);
        if (text.isEmpty()) {
            return;
        }

        PdfPage page = dst.getPage(pageNo);
        PdfCanvas pdfCanvas = new PdfCanvas(page);
        pdfCanvas.saveState();
        pdfCanvas.beginText();
        try {
            pdfCanvas.setFontAndSize(Font.fontHELVETICA(dst), opts.labelFontSizePt);
        } catch (Exception e) {
            pdfCanvas.endText();
            pdfCanvas.restoreState();
            throw new RuntimeException("Impossibile caricare il font per l'etichetta gruppo", e);
        }
        pdfCanvas.setFillColor(ColorConstants.BLACK);
        float x = opts.labelPosXpt;
        float y = opts.labelPosYpt;
        if (opts.labelVertical) {
            pdfCanvas.setTextMatrix(0, 1, -1, 0, x, y);
        } else {
            pdfCanvas.setTextMatrix(1, 0, 0, 1, x, y);
        }
        pdfCanvas.showText(text);
        pdfCanvas.endText();
        pdfCanvas.restoreState();
    }

    public static void applyRaccomandataBarcodeToGroup(
            PdfDocument dst,
            int dstStartIndex,
            int copiedCount,
            int groupIndex,
            RaccomandataBarcodeOpts opts) throws IOException {
        if (opts == null || copiedCount <= 0) {
            return;
        }
        boolean barcodeEnabled = opts.enabled;
        boolean omologazioneEnabled = hasRaccomandataOmologazione(opts);
        boolean customTextEnabled = hasRaccomandataCustomText(opts);
        if (!barcodeEnabled && !omologazioneEnabled && !customTextEnabled) {
            return;
        }
        if (groupIndex < 1) {
            throw new IllegalArgumentException("groupIndex deve essere >= 1");
        }
        // Scegliamo quali elementi disegnare (barcode e/o testo di omologazione)
        // evitando duplicazioni.
        if (barcodeEnabled) {
            RaccomandataCode code = buildRaccomandataCode(opts, groupIndex);
            renderRaccomandataBarcode(dst, dstStartIndex, code, opts);
        }
        if (omologazioneEnabled) {
            renderRaccomandataOmologazioneText(dst, dstStartIndex, opts);
        }
        if (customTextEnabled) {
            renderRaccomandataCustomText(dst, dstStartIndex, opts);
        }
    }

    public static void applyQrCodeToGroup(
            PdfDocument dst,
            int dstStartIndex,
            int copiedCount,
            int groupIndex,
            QrCodeOpts opts) {
        if (opts == null || !opts.enabled || copiedCount <= 0) {
            return;
        }
        if (groupIndex < 1) {
            throw new IllegalArgumentException("groupIndex deve essere >= 1");
        }
        // Il contenuto del QR varia con il gruppo, cosi' da mantenere un progressivo
        // univoco.
        String content = buildQrCodeContent(opts, groupIndex);
        renderQrCode(dst, dstStartIndex, opts, content);
    }

    public static void applyCorrectionOverlayToGroup(
            PdfDocument dst,
            int dstStartIndex,
            int copiedCount,
            CorrectionOverlayEntry entry,
            CorrectionOverlayOpts opts) {
        if (opts == null || !opts.enabled || copiedCount <= 0 || entry == null || entry.kind == CorrectionOverlayKind.NONE) {
            return;
        }
        renderCorrectionOverlay(dst, dstStartIndex, entry, opts);
    }

    public static void applyPostaEvolutionDataMatrixToGroup(
            PdfDocument dst,
            int dstStartIndex,
            int copiedCount,
            String payload,
            PostaEvolutionDataMatrixOpts opts) {
        if (opts == null || !opts.enabled || copiedCount <= 0) {
            return;
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Il payload DataMatrix non puo' essere vuoto.");
        }
        renderDataMatrix(dst, dstStartIndex, payload.trim(), opts);
    }

    private static final class RaccomandataCode {
        final String identifierDigits;
        final int checkDigit;
        final String encodedDigits;

        RaccomandataCode(String identifierDigits, int checkDigit) {
            this.identifierDigits = identifierDigits;
            this.checkDigit = checkDigit;
            this.encodedDigits = identifierDigits + checkDigit;
        }

        String printable() {
            return identifierDigits + "-" + checkDigit;
        }
    }

    private static RaccomandataCode buildRaccomandataCode(RaccomandataBarcodeOpts opts, int groupIndex) {
        long identifierValue = opts.startValue + (groupIndex - 1L);
        if (identifierValue > opts.maxIdentifierValue) {
            throw new IllegalArgumentException("Progressivo Raccomandata eccede le "
                    + opts.identifierDigits + " cifre disponibili.");
        }
        // Progressivo formattato sempre sulla stessa lunghezza.
        String identifier = String.format(Locale.US, "%0" + opts.identifierDigits + "d", identifierValue);
        int checkDigit = computeRaccomandataCheckDigit(identifier);
        return new RaccomandataCode(identifier, checkDigit);
    }

    private static int computeRaccomandataCheckDigit(String identifierDigits) {
        int sumEven = 0;
        int sumOdd = 0;
        for (int i = 0; i < identifierDigits.length(); i++) {
            int digit = identifierDigits.charAt(i) - '0';
            // La specifica richiede pesi diversi per posizioni pari e dispari.
            if (((i + 1) % 2) == 0) {
                sumEven += digit;
            } else {
                sumOdd += digit;
            }
        }
        int total = (sumEven * 11) + sumOdd;
        int digitSum = 0;
        while (total > 0) {
            digitSum += total % 10;
            total /= 10;
        }
        return digitSum % 10;
    }

    private static void renderRaccomandataBarcode(
            PdfDocument dst,
            int pageNo,
            RaccomandataCode code,
            RaccomandataBarcodeOpts opts) throws IOException {
        BarcodeInter25 bc = new BarcodeInter25(dst);
        // bc.setChecksum(false);
        bc.setCode(code.encodedDigits);
        bc.setBarHeight(Math.max(1f, opts.barHeightPt));
        bc.setX(Math.max(0.1f, opts.moduleNarrowPt));
        bc.setN(Math.max(1.1f, opts.wideToNarrowRatio));
        bc.setFont(null);// non monstrare il barcode integrato

        Image img = new Image(bc.createFormXObject(dst));
        img.setAutoScale(false);
        // Salviamo la larghezza per centrare la parte leggibile.
        float barcodeWidth = img.getImageScaledWidth();

        PdfPage page = dst.getPage(pageNo);
        Rectangle pageSize = page.getPageSize();
        Canvas canvas = new Canvas(new PdfCanvas(page), pageSize);
        img.setFixedPosition(pageNo, opts.posXpt, opts.posYpt);
        canvas.add(img);
        canvas.close();

        try {
            PdfFont font = Font.fontARIAL(dst);
            PdfCanvas pdfCanvas = new PdfCanvas(page);

            String printable = code.printable();
            float digitsFontSize = opts.humanReadableFontSizePt;
            float digitsWidth = font.getWidth(printable, digitsFontSize);
            float digitsAscent = font.getAscent(printable, digitsFontSize);
            // Il testo e' centrato rispetto all'immagine del barcode.
            float digitsX = opts.posXpt + (barcodeWidth - digitsWidth) / 2f;
            float digitsBaseline = opts.posYpt - opts.humanReadableGapPt - digitsAscent;
            if (digitsBaseline < 0f) {
                digitsBaseline = 0f;
            }

            pdfCanvas.saveState();
            pdfCanvas.beginText();
            pdfCanvas.setFontAndSize(font, digitsFontSize);
            pdfCanvas.setFillColor(ColorConstants.BLACK);
            pdfCanvas.setTextMatrix(digitsX, digitsBaseline);
            pdfCanvas.showText(printable);
            pdfCanvas.endText();

            pdfCanvas.beginText();
            pdfCanvas.setFontAndSize(font, 1f);
            pdfCanvas.setFillColor(ColorConstants.BLACK);
            pdfCanvas.setTextMatrix(0, 0);
            pdfCanvas.showText("barcode->" + printable.replace("-", ""));
            pdfCanvas.endText();
            pdfCanvas.restoreState();

            if (!opts.productPrefix.isEmpty()) {
                String prefix = opts.productPrefix;
                float prefixSize = opts.prefixFontSizePt;
                float prefixWidth = font.getWidth(prefix, prefixSize);
                float prefixAscent = font.getAscent(prefix, prefixSize);
                float prefixDescent = font.getDescent(prefix, prefixSize);
                float prefixHeight = prefixAscent - prefixDescent;
                // Posizioniamo il prefisso allineandolo verticalmente al centro delle barre.
                float prefixBottom = opts.posYpt + (opts.barHeightPt - prefixHeight) / 2f;
                float prefixBaseline = prefixBottom - prefixDescent;
                float prefixX = opts.posXpt - opts.prefixGapPt - prefixWidth;

                pdfCanvas.saveState();
                pdfCanvas.beginText();
                pdfCanvas.setFontAndSize(font, prefixSize);
                pdfCanvas.setFillColor(ColorConstants.BLACK);
                pdfCanvas.setTextMatrix(prefixX, prefixBaseline);
                pdfCanvas.showText(prefix);
                pdfCanvas.endText();
                pdfCanvas.restoreState();
            }
        } catch (Exception e) {
            throw new RuntimeException("Errore durante il rendering del barcode Raccomandata", e);
        }
    }

    private static void renderQrCode(
            PdfDocument dst,
            int pageNo,
            QrCodeOpts opts,
            String content) {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        // L'utente sceglie il livello di correzione, quindi lo forziamo tra le opzioni
        // iText.
        hints.put(EncodeHintType.ERROR_CORRECTION, opts.errorCorrectionLevel);
        BarcodeQRCode qr = new BarcodeQRCode(content, hints);
        PdfFormXObject xobj = qr.createFormXObject(ColorConstants.BLACK, dst);
        Image img = new Image(xobj);
        img.setAutoScale(false);
        img.setWidth(opts.sizePt);
        img.setHeight(opts.sizePt);

        PdfPage page = dst.getPage(pageNo);
        Rectangle pageSize = page.getPageSize();
        Canvas canvas = new Canvas(new PdfCanvas(page), pageSize);
        img.setFixedPosition(pageNo, opts.posXpt, opts.posYpt);
        canvas.add(img);
        canvas.close();

        PdfCanvas pdfCanvas = new PdfCanvas(page);
        try {
            PdfFont font = Font.fontHELVETICA(dst);
            float textSize = 8f;
            float width = font.getWidth(content, textSize);
            float ascent = font.getAscent(content, textSize);
            float descent = font.getDescent(content, textSize);
            float textHeight = ascent - descent;
            float gap = mm(2f);
            // Il testo descrittivo viene posizionato alla sinistra del QR per non coprirlo.
            float textX = opts.posXpt - gap - width;
            // Allineamento verticale centrato, tenendo conto della discesa del font.
            float baseline = opts.posYpt + (opts.sizePt - textHeight) / 2f - descent;

            pdfCanvas.saveState();
            pdfCanvas.beginText();
            pdfCanvas.setFontAndSize(font, textSize);
            pdfCanvas.setFillColor(ColorConstants.BLACK);
            pdfCanvas.setTextMatrix(textX, baseline);
            pdfCanvas.showText(content);
            pdfCanvas.endText();

            textSize = 1f;
            pdfCanvas.beginText();

            pdfCanvas.setFontAndSize(font, textSize);
            pdfCanvas.setFillColor(ColorConstants.BLACK);
            pdfCanvas.setTextMatrix(100, 0);
            pdfCanvas.showText("QR->" + content);
            pdfCanvas.endText();
            pdfCanvas.restoreState();

        } catch (Exception e) {
            throw new RuntimeException("Errore durante il rendering del QR code", e);
        }
    }

    private static void renderDataMatrix(
            PdfDocument dst,
            int pageNo,
            String payload,
            PostaEvolutionDataMatrixOpts opts) {
        final int symbolWidthCells = Math.max(1, opts.widthCells);
        final int symbolHeightCells = Math.max(1, opts.heightCells);
        final int quietZoneCells = Math.max(2, opts.quietZoneCells);
        final BitMatrix bitMatrix;
        Map<com.google.zxing.EncodeHintType, Object> hints = new HashMap<>();
        hints.put(com.google.zxing.EncodeHintType.DATA_MATRIX_SHAPE, SymbolShapeHint.FORCE_RECTANGLE);
        hints.put(com.google.zxing.EncodeHintType.MIN_SIZE,
                new com.google.zxing.Dimension(symbolWidthCells, symbolHeightCells));
        hints.put(com.google.zxing.EncodeHintType.MAX_SIZE,
                new com.google.zxing.Dimension(symbolWidthCells, symbolHeightCells));
        // Obbligatorio per rispettare la capienza alfanumerica del formato 16x48.
        hints.put(com.google.zxing.EncodeHintType.FORCE_C40, Boolean.TRUE);
        bitMatrix = new DataMatrixWriter().encode(
                payload,
                BarcodeFormat.DATA_MATRIX,
                symbolWidthCells,
                symbolHeightCells,
                hints);

        PdfPage page = dst.getPage(pageNo);
        ensureIgnorePageRotationForNewContent(page);
        PdfCanvas pdfCanvas = new PdfCanvas(page);
        pdfCanvas.saveState();
        float module = opts.moduleSizePt;
        float originX = opts.posXpt + (quietZoneCells * module);
        float originY = opts.posYpt + (quietZoneCells * module);
        pdfCanvas.setFillColor(ColorConstants.BLACK);
        int matrixWidth = bitMatrix.getWidth();
        int matrixHeight = bitMatrix.getHeight();
        for (int y = 0; y < matrixHeight; y++) {
            int yy = matrixHeight - 1 - y;
            for (int x = 0; x < matrixWidth; x++) {
                if (bitMatrix.get(x, y)) {
                    float rx = originX + (x * module);
                    float ry = originY + (yy * module);
                    pdfCanvas.rectangle(rx, ry, module, module);
                }
            }
        }
        pdfCanvas.fill();
        pdfCanvas.restoreState();

        try {
            PdfFont font = Font.fontHELVETICA(dst);
            pdfCanvas.saveState();
            pdfCanvas.beginText();
            pdfCanvas.setFontAndSize(font, 1f);
            pdfCanvas.setFillColor(ColorConstants.BLACK);
            pdfCanvas.setTextMatrix(100, 0);
            pdfCanvas.showText("datamatrix->" + payload.replace(" ", "_"));
            pdfCanvas.endText();
            pdfCanvas.restoreState();
        } catch (Exception e) {
            throw new RuntimeException("Errore durante il rendering del testo DataMatrix", e);
        }
    }

    private static void renderCorrectionOverlay(
            PdfDocument dst,
            int pageNo,
            CorrectionOverlayEntry entry,
            CorrectionOverlayOpts opts) {
        PdfPage page = dst.getPage(pageNo);
        ensureIgnorePageRotationForNewContent(page);
        PdfCanvas pdfCanvas = new PdfCanvas(page);
        try {
            if (entry.kind == CorrectionOverlayKind.QUESTION) {
                drawQuestionMarkIcon(pdfCanvas, dst, opts);
                if (entry.xIcon) {
                    drawXIconAt(pdfCanvas, opts, opts.posXpt + Math.max(0f, opts.areaWidthPt - opts.iconSizePt));
                }
            } else if (entry.kind == CorrectionOverlayKind.SEARCH) {
                drawTrailingCorrectionIcons(pdfCanvas, opts, true, entry.plusSearchIcon, entry.xIcon);
            } else if (entry.kind == CorrectionOverlayKind.TEXT && !entry.text.isEmpty()) {
                PdfFont font = Font.fontHELVETICA(dst);
                String textToRender = entry.text;
                boolean showSearchIcon = entry.searchIcon;
                boolean showXIcon = entry.xIcon;
                float availableTextWidth = availableCorrectionTextWidth(opts, showSearchIcon, showXIcon);
                if (font.getWidth(textToRender, opts.fontSizePt) > availableTextWidth) {
                    showSearchIcon = true;
                    availableTextWidth = availableCorrectionTextWidth(opts, showSearchIcon, showXIcon);
                    textToRender = fitCorrectionText(textToRender, font, opts.fontSizePt, availableTextWidth);
                }
                if (!textToRender.isEmpty()) {
                    drawCorrectionText(pdfCanvas, font, textToRender, opts);
                }
                drawTrailingCorrectionIcons(pdfCanvas, opts, showSearchIcon, entry.plusSearchIcon, showXIcon);
            } else if (entry.xIcon) {
                drawXIconAt(pdfCanvas, opts, opts.posXpt + Math.max(0f, opts.areaWidthPt - opts.iconSizePt));
            }
        } catch (Exception e) {
            throw new RuntimeException("Errore durante il rendering correzioni da Excel", e);
        }
    }

    private static float availableCorrectionTextWidth(CorrectionOverlayOpts opts, boolean searchIcon, boolean xIcon) {
        int iconCount = (searchIcon ? 1 : 0) + (xIcon ? 1 : 0);
        if (iconCount <= 0) {
            return opts.areaWidthPt;
        }
        float gap = mm(1f);
        return Math.max(0f, opts.areaWidthPt - (iconCount * opts.iconSizePt) - (iconCount * gap));
    }

    private static String fitCorrectionText(String text, PdfFont font, float fontSizePt, float maxWidthPt) {
        if (maxWidthPt <= 0f) {
            return "";
        }
        String safeText = normalizeFreeText(text);
        if (font.getWidth(safeText, fontSizePt) <= maxWidthPt) {
            return safeText;
        }
        String[] tokens = safeText.split("\\s+");
        StringBuilder fitted = new StringBuilder();
        for (String token : tokens) {
            String candidate = fitted.length() == 0 ? token : fitted + " " + token;
            if (font.getWidth(candidate, fontSizePt) > maxWidthPt) {
                break;
            }
            fitted.setLength(0);
            fitted.append(candidate);
        }
        if (fitted.length() > 0) {
            return fitted.toString();
        }
        StringBuilder chars = new StringBuilder();
        for (int i = 0; i < safeText.length(); i++) {
            String candidate = chars.toString() + safeText.charAt(i);
            if (font.getWidth(candidate, fontSizePt) > maxWidthPt) {
                break;
            }
            chars.append(safeText.charAt(i));
        }
        return chars.toString().trim();
    }

    private static void drawCorrectionText(PdfCanvas pdfCanvas, PdfFont font, String text, CorrectionOverlayOpts opts) {
        pdfCanvas.saveState();
        pdfCanvas.beginText();
        pdfCanvas.setFontAndSize(font, opts.fontSizePt);
        pdfCanvas.setFillColor(ColorConstants.BLACK);
        pdfCanvas.setTextMatrix(opts.posXpt, opts.posYpt);
        pdfCanvas.showText(text);
        pdfCanvas.endText();
        pdfCanvas.restoreState();
    }

    private static void drawSearchIconAt(PdfCanvas pdfCanvas, CorrectionOverlayOpts opts, float iconXpt,
            boolean plusIcon) {
        CorrectionOverlayOpts iconOpts = new CorrectionOverlayOpts(
                opts.enabled,
                iconXpt,
                opts.posYpt,
                opts.areaWidthPt,
                opts.fontSizePt,
                opts.iconSizePt,
                opts.startValue);
        drawSearchIcon(pdfCanvas, iconOpts, plusIcon);
    }

    private static void drawTrailingCorrectionIcons(PdfCanvas pdfCanvas, CorrectionOverlayOpts opts,
            boolean searchIcon, boolean plusIcon, boolean xIcon) {
        float gap = mm(1f);
        int iconIndex = 0;
        if (searchIcon) {
            float iconX = opts.posXpt + Math.max(0f,
                    opts.areaWidthPt - ((xIcon ? 2 : 1) * opts.iconSizePt) - (xIcon ? gap : 0f));
            drawSearchIconAt(pdfCanvas, opts, iconX, plusIcon);
            iconIndex++;
        }
        if (xIcon) {
            float iconX = opts.posXpt + Math.max(0f, opts.areaWidthPt - opts.iconSizePt);
            drawXIconAt(pdfCanvas, opts, iconX);
        }
    }

    private static void drawQuestionMarkIcon(PdfCanvas pdfCanvas, PdfDocument dst, CorrectionOverlayOpts opts)
            throws IOException {
        float size = opts.iconSizePt;
        float centerX = opts.posXpt + size / 2f;
        float centerY = opts.posYpt + size / 2f;
        pdfCanvas.saveState();
        pdfCanvas.setStrokeColor(ColorConstants.BLACK);
        pdfCanvas.setLineWidth(Math.max(0.6f, size / 18f));
        pdfCanvas.circle(centerX, centerY, size / 2f);
        pdfCanvas.stroke();
        pdfCanvas.beginText();
        pdfCanvas.setFontAndSize(Font.fontHELVETICA(dst), size * 0.78f);
        pdfCanvas.setFillColor(ColorConstants.BLACK);
        pdfCanvas.setTextMatrix(opts.posXpt + size * 0.31f, opts.posYpt + size * 0.17f);
        pdfCanvas.showText("?");
        pdfCanvas.endText();
        pdfCanvas.restoreState();
    }

    private static void drawSearchIcon(PdfCanvas pdfCanvas, CorrectionOverlayOpts opts) {
        drawSearchIcon(pdfCanvas, opts, false);
    }

    private static void drawSearchIcon(PdfCanvas pdfCanvas, CorrectionOverlayOpts opts, boolean plusIcon) {
        float size = opts.iconSizePt;
        float radius = size * 0.32f;
        float centerX = opts.posXpt + size * 0.42f;
        float centerY = opts.posYpt + size * 0.58f;
        pdfCanvas.saveState();
        pdfCanvas.setStrokeColor(ColorConstants.BLACK);
        pdfCanvas.setLineWidth(Math.max(0.8f, size / 12f));
        pdfCanvas.circle(centerX, centerY, radius);
        pdfCanvas.stroke();
        pdfCanvas.moveTo(centerX + radius * 0.65f, centerY - radius * 0.65f);
        pdfCanvas.lineTo(opts.posXpt + size * 0.88f, opts.posYpt + size * 0.12f);
        pdfCanvas.stroke();
        if (plusIcon) {
            float plusHalf = radius * 0.42f;
            pdfCanvas.setLineWidth(Math.max(0.6f, size / 18f));
            pdfCanvas.moveTo(centerX - plusHalf, centerY);
            pdfCanvas.lineTo(centerX + plusHalf, centerY);
            pdfCanvas.moveTo(centerX, centerY - plusHalf);
            pdfCanvas.lineTo(centerX, centerY + plusHalf);
            pdfCanvas.stroke();
        }
        pdfCanvas.restoreState();
    }

    private static void drawXIconAt(PdfCanvas pdfCanvas, CorrectionOverlayOpts opts, float iconXpt) {
        float size = opts.iconSizePt;
        float padding = size * 0.22f;
        float x1 = iconXpt + padding;
        float y1 = opts.posYpt + padding;
        float x2 = iconXpt + size - padding;
        float y2 = opts.posYpt + size - padding;
        pdfCanvas.saveState();
        pdfCanvas.setStrokeColor(ColorConstants.BLACK);
        pdfCanvas.setLineWidth(Math.max(0.8f, size / 12f));
        pdfCanvas.moveTo(x1, y1);
        pdfCanvas.lineTo(x2, y2);
        pdfCanvas.moveTo(x1, y2);
        pdfCanvas.lineTo(x2, y1);
        pdfCanvas.stroke();
        pdfCanvas.restoreState();
    }

    private static boolean hasRaccomandataOmologazione(RaccomandataBarcodeOpts opts) {
        return opts != null && opts.omologazioneEnabled && !opts.omologazioneText.isEmpty();
    }

    private static boolean hasRaccomandataCustomText(RaccomandataBarcodeOpts opts) {
        return opts != null && opts.customTextEnabled && !opts.customText.isEmpty();
    }

    private static void renderRaccomandataOmologazioneText(
            PdfDocument dst,
            int pageNo,
            RaccomandataBarcodeOpts opts) {
        renderRaccomandataText(dst, pageNo, opts.omologazioneText, opts.omologazionePosXpt,
                opts.omologazionePosYpt, opts.omologazioneFontSizePt, "dell'omologazione postale");
    }

    private static void renderRaccomandataCustomText(
            PdfDocument dst,
            int pageNo,
            RaccomandataBarcodeOpts opts) {
        renderRaccomandataText(dst, pageNo, opts.customText, opts.customTextPosXpt,
                opts.customTextPosYpt, opts.customTextFontSizePt, "del testo personalizzato Raccomandata");
    }

    private static void renderRaccomandataText(
            PdfDocument dst,
            int pageNo,
            String text,
            float xpt,
            float ypt,
            float fontSizePt,
            String context) {
        PdfPage page = dst.getPage(pageNo);
        PdfCanvas pdfCanvas = new PdfCanvas(page);
        try {
            PdfFont font = Font.fontHELVETICA(dst);
            pdfCanvas.saveState();
            pdfCanvas.beginText();
            pdfCanvas.setFontAndSize(font, fontSizePt);
            pdfCanvas.setFillColor(ColorConstants.BLACK);
            pdfCanvas.setTextMatrix(xpt, ypt);
            pdfCanvas.showText(text);
            pdfCanvas.endText();
            pdfCanvas.restoreState();
        } catch (Exception e) {
            throw new RuntimeException("Errore durante il rendering " + context, e);
        }
    }

    public static void drawGroupPageCounters(
            PdfDocument dst,
            int dstStartIndex,
            int copiedCount,
            PageCounterOpts opts) {
        drawGroupPageCounters(dst, dstStartIndex, copiedCount, opts, 1, "");
    }

    public static void drawGroupPageCounters(
            PdfDocument dst,
            int dstStartIndex,
            int copiedCount,
            PageCounterOpts opts,
            int groupNumber,
            String lavorazioneId) {
        if (opts == null || !opts.enabled || copiedCount <= 0) {
            return;
        }
        final PdfFont font;
        try {
            font = Font.fontHELVETICA(dst);
        } catch (Exception e) {
            throw new RuntimeException("Impossibile caricare il font per il contatore pagine", e);
        }

        for (int i = 0; i < copiedCount; i++) {
            int pageNo = dstStartIndex + i;
            PdfPage page = dst.getPage(pageNo);
            Rectangle size = page.getPageSize();
            float left = size.getLeft();
            float bottom = size.getBottom();
            float top = size.getTop();
            float x = left + Math.max(0f, opts.offsetLeftPt);
            float y = top - Math.max(0f, opts.offsetTopPt);
            if (y < bottom + opts.fontSizePt) {
                y = bottom + opts.fontSizePt;
            }
            // Il testo riporta l'indice pagina relativo al gruppo corrente.
            String text = String.format(Locale.ITALIAN, "%d di %d", i + 1, copiedCount);
            if (i > 0) {
                String label = formatGroupLabel(Math.max(1, groupNumber), lavorazioneId);
                if (!label.isEmpty()) {
                    text = text + " - " + label;
                }
            }

            PdfCanvas canvas = new PdfCanvas(page);
            canvas.saveState();
            canvas.beginText();
            canvas.setFontAndSize(font, opts.fontSizePt);
            canvas.setFillColor(ColorConstants.BLACK);
            if (opts.vertical) {
                canvas.setTextMatrix(0, 1, -1, 0, x, y);
            } else {
                canvas.setTextMatrix(1, 0, 0, 1, x, y);
            }
            canvas.showText(text);
            canvas.endText();
            canvas.restoreState();
        }
    }

    /** Parametri per il barcode Raccomandata (ITF 2/5). */
    public static class RaccomandataBarcodeOpts {
        public final boolean enabled;
        public final float posXpt;
        public final float posYpt;
        public final float barHeightPt;
        public final float moduleNarrowPt;
        public final float wideToNarrowRatio;
        public final float humanReadableFontSizePt;
        public final float humanReadableGapPt;
        public final String productPrefix;
        public final float prefixGapPt;
        public final float prefixFontSizePt;
        public final String startIdentifierDigits;
        public final long startValue;
        public final int identifierDigits;
        public final long maxIdentifierValue;
        public final boolean omologazioneEnabled;
        public final String omologazioneText;
        public final float omologazionePosXpt;
        public final float omologazionePosYpt;
        public final float omologazioneFontSizePt;
        public final boolean customTextEnabled;
        public final String customText;
        public final float customTextPosXpt;
        public final float customTextPosYpt;
        public final float customTextFontSizePt;

        public RaccomandataBarcodeOpts(
                boolean enabled,
                float posXpt,
                float posYpt,
                float barHeightPt,
                float moduleNarrowPt,
                float wideToNarrowRatio,
                float humanReadableFontSizePt,
                float humanReadableGapPt,
                String productPrefix,
                float prefixGapPt,
                float prefixFontSizePt,
                String startIdentifierDigits,
                boolean omologazioneEnabled,
                String omologazioneText,
                float omologazionePosXpt,
                float omologazionePosYpt,
                float omologazioneFontSizePt,
                boolean customTextEnabled,
                String customText,
                float customTextPosXpt,
                float customTextPosYpt,
                float customTextFontSizePt) {
            this.enabled = enabled;
            this.posXpt = Math.max(0f, posXpt);
            this.posYpt = Math.max(0f, posYpt);
            this.barHeightPt = Math.max(1f, barHeightPt);
            this.moduleNarrowPt = moduleNarrowPt > 0f ? moduleNarrowPt : RaccomandataStandard.NARROW_MODULE_PT;
            this.wideToNarrowRatio = wideToNarrowRatio > 1f ? wideToNarrowRatio
                    : RaccomandataStandard.WIDE_TO_NARROW_RATIO;
            this.humanReadableFontSizePt = humanReadableFontSizePt > 0f
                    ? humanReadableFontSizePt
                    : RaccomandataStandard.HUMAN_READABLE_FONT_PT;
            this.humanReadableGapPt = humanReadableGapPt >= 0f
                    ? humanReadableGapPt
                    : RaccomandataStandard.HUMAN_READABLE_GAP_PT;
            this.productPrefix = productPrefix == null ? "" : productPrefix.trim();
            this.prefixGapPt = prefixGapPt >= 0f ? prefixGapPt : RaccomandataStandard.PREFIX_GAP_PT;
            this.prefixFontSizePt = prefixFontSizePt > 0f ? prefixFontSizePt : RaccomandataStandard.PREFIX_FONT_PT;

            this.identifierDigits = RaccomandataStandard.IDENTIFIER_DIGITS;
            if (enabled) {
                this.startIdentifierDigits = normalizeIdentifier(startIdentifierDigits, this.identifierDigits);
                this.startValue = Long.parseLong(this.startIdentifierDigits);
            } else {
                this.startIdentifierDigits = defaultIdentifierDigits(this.identifierDigits);
                this.startValue = Long.parseLong(this.startIdentifierDigits);
            }
            this.maxIdentifierValue = maxValueForDigits(this.identifierDigits);

            this.omologazioneEnabled = omologazioneEnabled;
            this.omologazioneText = normalizeFreeText(omologazioneText);
            this.omologazionePosXpt = Math.max(0f, omologazionePosXpt);
            this.omologazionePosYpt = Math.max(0f, omologazionePosYpt);
            this.omologazioneFontSizePt = omologazioneFontSizePt > 0f
                    ? omologazioneFontSizePt
                    : RaccomandataStandard.OMOLOGAZIONE_FONT_PT;
            if (this.omologazioneEnabled && this.omologazioneText.isEmpty()) {
                throw new IllegalArgumentException("Il testo dell'omologazione postale non puo' essere vuoto.");
            }

            this.customTextEnabled = customTextEnabled;
            this.customText = normalizeFreeText(customText);
            this.customTextPosXpt = Math.max(0f, customTextPosXpt);
            this.customTextPosYpt = Math.max(0f, customTextPosYpt);
            this.customTextFontSizePt = customTextFontSizePt > 0f
                    ? customTextFontSizePt
                    : RaccomandataStandard.CUSTOM_TEXT_FONT_PT;
            if (this.customTextEnabled && this.customText.isEmpty()) {
                throw new IllegalArgumentException("Il testo personalizzato Raccomandata non puo' essere vuoto.");
            }
        }

        private static String defaultIdentifierDigits(int digits) {
            return String.format(Locale.US, "%0" + Math.max(1, digits) + "d", 0);
        }

        private static String normalizeIdentifier(String value, int expectedDigits) {
            if (value == null) {
                throw new IllegalArgumentException("Il barcode iniziale per la Raccomandata ? obbligatorio.");
            }
            String digitsOnly = value.replaceAll("\\s+", "");
            if (digitsOnly.length() != expectedDigits) {
                throw new IllegalArgumentException(
                        "Il barcode iniziale deve contenere esattamente " + expectedDigits + " cifre.");
            }
            if (!digitsOnly.matches("\\d+")) {
                throw new IllegalArgumentException("Il barcode iniziale deve contenere solo cifre.");
            }
            return digitsOnly;
        }
    }

    private static String normalizeFreeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean hasLabel(BarcodeOpts opts) {
        return opts != null && opts.labelEnabled;
    }

    private static String formatGroupLabel(int groupNumber, String lavorazioneId) {
        String trimmed = lavorazioneId == null ? "" : lavorazioneId.trim();
        if (trimmed.isEmpty()) {
            return String.valueOf(groupNumber);
        }
        return trimmed + " - " + groupNumber;
    }

    // private static String abbreviate(String text, int maxLength) {
    // if (text == null) {
    // return "";
    // }
    // if (text.length() <= maxLength) {
    // return text;
    // }
    // if (maxLength <= 3) {
    // return text.substring(0, Math.max(0, maxLength));
    // }
    // return text.substring(0, maxLength - 3) + "...";
    // }

    /**
     * Disegna un Code39 (3 of 9) verticale (ruotato 90?) alla posizione/dimensione
     * specificate.
     */
    public static void renderBarcode39Vertical(PdfDocument dst, int pageNo, String code, BarcodeOpts opts) {
        // Code 39 accetta 0-9 A-Z e pochi simboli; i tuoi codici numerici sono OK.
        // Non aggiungere manualmente gli asterischi *start/stop*: iText li gestisce
        // internamente.
        Barcode39 bc = new Barcode39(dst);
        bc.setCode(code);
        bc.setBarHeight(Math.max(1f, opts.heightPt));
        bc.setX(opts.moduleWidthPt);
        bc.setTextAlignment(BarcodeStandard.TEXT_ALIGNMENT);
        bc.setSize(BarcodeStandard.FONT_SIZE_PT);

        // Opzionali (decommenta se vuoi controllare i moduli):
        // bc.setX(Imbustatrice.mm(0.33f)); // larghezza modulo "narrow" (esempio ~0.33
        // mm)
        // bc.setN(3.0f); // rapporto wide:narrow tipico del 3of9
        // bc.setChecksum(true); // abilita Modulo-43 se richiesto dall'imbustatrice
        // bc.setStartStopText(false); // nasconde testo * * se appare (dipende dalla
        // versione)

        Image img = new Image(bc.createFormXObject(dst));
        img.setAutoScale(false);
        float rotationRad = (float) Math.toRadians(BarcodeStandard.ROTATION_DEG);
        img.setRotationAngle(rotationRad);

        // Posizionamento fisso
        // Mantiene lo scostamento verticale aggiuntivo previsto dallo standard
        // precedente (+20 pt).
        img.setFixedPosition(pageNo, opts.posXpt, opts.posYpt + BarcodeStandard.Y_OFFSET_PT);

        // iText 7.2+ : Canvas(PdfCanvas, Rectangle)
        PdfPage page = dst.getPage(pageNo);
        ensureIgnorePageRotationForNewContent(page);
        Rectangle pageSize = page.getPageSize();
        Canvas canvas = new Canvas(new PdfCanvas(page), pageSize);
        canvas.add(img);
        canvas.close();
    }

    private static String buildOmrBinary(int pageNumber, boolean isLastFrontPage, int groupNumber, BarcodeOpts opts) {
        // Formato legacy (7 bit): BH + EOC + WAS(3) + PR + SAF
        // Formato esteso (11 bit): BH + EOC + WAS(3) + MC(3) + SEL + PR + SAF
        // Il formato esteso si attiva solo quando OMR e allegati presenti sono entrambi attivi.
        int safePageNumber = Math.max(1, pageNumber);
        int safeGroupNumber = Math.max(1, groupNumber);
        int halfPageCounter = safePageNumber / 2;
        String sheetBits = toBinaryString(halfPageCounter, OmrStandard.SHEET_BITS);
        boolean useExtendedPattern = opts != null && opts.useOmr && opts.allegatiPresenti;
        String eocBit = isLastFrontPage ? "1" : "0";
        String patternWithParityPlaceholder;
        if (useExtendedPattern) {
            String mcBits = toBinaryString(safeGroupNumber, OmrStandard.MC_BITS);
            String selBit = isLastFrontPage ? "1" : "0";
            patternWithParityPlaceholder = "1" + eocBit + sheetBits + mcBits + selBit + "X1";
        } else {
            patternWithParityPlaceholder = "1" + eocBit + sheetBits + "X1";
        }
        return applyEvenParity(patternWithParityPlaceholder);
    }

    private static String toBinaryString(int value, int width) {
        int safeWidth = Math.max(1, width);
        int maxMask = (1 << safeWidth) - 1;
        int truncated = Math.max(0, value) & maxMask;
        String binary = Integer.toBinaryString(truncated);
        if (binary.length() >= safeWidth) {
            return binary.substring(binary.length() - safeWidth);
        }
        StringBuilder sb = new StringBuilder(safeWidth);
        for (int i = binary.length(); i < safeWidth; i++) {
            sb.append('0');
        }
        sb.append(binary);
        return sb.toString();
    }

    private static String applyEvenParity(String patternWithX) {
        int ones = countOnes(patternWithX.replace("X", "0"));
        char parityBit = (ones % 2 == 0) ? '0' : '1';
        return patternWithX.replace('X', parityBit);
    }

    private static int countOnes(String binary) {
        if (binary == null || !binary.matches("[01]+")) {
            throw new IllegalArgumentException("La stringa deve contenere solo 0 e 1.");
        }
        int count = 0;
        for (int i = 0; i < binary.length(); i++) {
            if (binary.charAt(i) == '1') {
                count++;
            }
        }
        return count;
    }

    private static boolean[] binaryStringToOmrBits(String binary, int expectedBits) {
        int size = Math.max(1, expectedBits);
        boolean[] bits = new boolean[size];
        String safeBinary = binary == null ? "" : binary.trim();
        if (safeBinary.length() != size) {
            throw new IllegalArgumentException("Pattern OMR non valido: attesi " + size + " bit, trovati "
                    + safeBinary.length());
        }
        for (int i = 0; i < size; i++) {
            char ch = safeBinary.charAt(i);
            if (ch == '0') {
                bits[i] = false;
            } else if (ch == '1') {
                bits[i] = true;
            } else {
                throw new IllegalArgumentException("Pattern OMR non valido: carattere '" + ch + "' alla posizione "
                        + (i + 1));
            }
        }
        return bits;
    }

    public static void renderOmrMarks(PdfDocument dst, int pageNo, String binary, BarcodeOpts opts) {
        int expectedBits = resolveOmrBitCount(binary);
        boolean[] omrBits = binaryStringToOmrBits(binary, expectedBits);

        PdfPage page = dst.getPage(pageNo);
        ensureIgnorePageRotationForNewContent(page);
        PdfCanvas cb = new PdfCanvas(page);
        cb.saveState();
        cb.setFillColor(ColorConstants.BLACK);

        // Posizione OMR fissa.
        float startX = OmrStandard.START_X_PT;
        float startY = OmrStandard.START_Y_PT + resolveOmrVerticalShift(expectedBits);
        float barThickness = OmrStandard.BAR_THICKNESS_PT;
        float barLength = OmrStandard.BAR_LENGTH_PT;
        for (int i = 0; i < omrBits.length; i++) {
            if (!omrBits[i]) {
                continue;
            }
            float y = startY - (i * OmrStandard.ROW_SPACING_PT);
            // Rettangolo pieno: evita rendering "a trattini" con linee molto sottili.
            cb.rectangle(startX, y - (barThickness / 2f), barLength, barThickness);
        }
        cb.fill();
        cb.restoreState();
    }

    private static int resolveOmrBitCount(String binary) {
        String safeBinary = binary == null ? "" : binary.trim();
        int length = safeBinary.length();
        if (length == OmrStandard.BIT_COUNT || length == OmrStandard.BIT_COUNT_WITH_MC) {
            return length;
        }
        throw new IllegalArgumentException(
                "Pattern OMR non valido: attesi " + OmrStandard.BIT_COUNT + " o " + OmrStandard.BIT_COUNT_WITH_MC
                        + " bit, trovati " + length);
    }

    private static float resolveOmrVerticalShift(int bitCount) {
        return bitCount > OmrStandard.BIT_COUNT ? OmrStandard.EXTENDED_PATTERN_Y_SHIFT_PT : 0f;
    }

    private static void ensureIgnorePageRotationForNewContent(PdfPage page) {
        if (page == null) {
            return;
        }
        page.setIgnorePageRotationForContent(true);
    }

    /*
     * ========================================
     * === SCALA + TRASLA CONTENUTO (PCT) ===
     * ========================================
     */

    public static final class ResizePageRotationOpts {
        public final boolean enabled;
        public final String searchText;
        public final int clockwiseDegrees;
        public final boolean ignoreCase;
        public final boolean normalizeAccents;
        public final boolean applyResizeOnMatchedPages;

        public ResizePageRotationOpts(
                boolean enabled,
                String searchText,
                int clockwiseDegrees,
                boolean ignoreCase,
                boolean normalizeAccents,
                boolean applyResizeOnMatchedPages) {
            this.enabled = enabled;
            this.searchText = searchText == null ? "" : searchText.trim();
            this.clockwiseDegrees = normalizeRotationDegrees(clockwiseDegrees);
            this.ignoreCase = ignoreCase;
            this.normalizeAccents = normalizeAccents;
            this.applyResizeOnMatchedPages = applyResizeOnMatchedPages;
            if (this.enabled && this.searchText.isEmpty()) {
                throw new IllegalArgumentException("La stringa per la rotazione pagina non puo' essere vuota.");
            }
        }
    }

    /**
     * Crea un nuovo PDF con lo STESSO formato pagina, ma col contenuto scalato per
     * percentuale e traslato di X/Y.
     * Annotazioni/AcroForm non vengono migrate (si ri-disegna il contenuto come
     * XObject).
     */
    public static void scaleAndTranslateContent(
            String inputPath,
            String outputPath,
            float scalePct,
            float offsetXpt,
            float offsetYpt,
            PdfVersion pdfVersion) throws Exception {
        scaleAndTranslateContent(inputPath, outputPath, scalePct, offsetXpt, offsetYpt, pdfVersion, null, null);
    }

    public static void scaleAndTranslateContent(
            String inputPath,
            String outputPath,
            float scalePct,
            float offsetXpt,
            float offsetYpt,
            PdfVersion pdfVersion,
            boolean forceA4BeforeResize) throws Exception {
        scaleAndTranslateContent(inputPath, outputPath, scalePct, offsetXpt, offsetYpt, pdfVersion, null, null,
                forceA4BeforeResize);
    }

    public static void scaleAndTranslateContent(
            String inputPath,
            String outputPath,
            float scalePct,
            float offsetXpt,
            float offsetYpt,
            PdfVersion pdfVersion,
            ResizePageRotationOpts rotationOpts,
            java.util.function.Consumer<String> logger) throws Exception {
        scaleAndTranslateContent(inputPath, outputPath, scalePct, offsetXpt, offsetYpt, pdfVersion, rotationOpts,
                logger, false);
    }

    public static void scaleAndTranslateContent(
            String inputPath,
            String outputPath,
            float scalePct,
            float offsetXpt,
            float offsetYpt,
            PdfVersion pdfVersion,
            ResizePageRotationOpts rotationOpts,
            java.util.function.Consumer<String> logger,
            boolean forceA4BeforeResize) throws Exception {
        scaleAndTranslateContent(inputPath, outputPath, scalePct, offsetXpt, offsetYpt, pdfVersion, rotationOpts,
                logger, forceA4BeforeResize, null);
    }

    public static void scaleAndTranslateContent(
            String inputPath,
            String outputPath,
            float scalePct,
            float offsetXpt,
            float offsetYpt,
            PdfVersion pdfVersion,
            ResizePageRotationOpts rotationOpts,
            java.util.function.Consumer<String> logger,
            boolean forceA4BeforeResize,
            BooleanSupplier cancellationRequested) throws Exception {
        scaleAndTranslateContent(inputPath, outputPath, scalePct, offsetXpt, offsetYpt, pdfVersion, rotationOpts,
                logger, forceA4BeforeResize, cancellationRequested, true);
    }

    public static void scaleAndTranslateContent(
            String inputPath,
            String outputPath,
            float scalePct,
            float offsetXpt,
            float offsetYpt,
            PdfVersion pdfVersion,
            ResizePageRotationOpts rotationOpts,
            java.util.function.Consumer<String> logger,
            boolean forceA4BeforeResize,
            BooleanSupplier cancellationRequested,
            boolean smartMode) throws Exception {

        if (!(scalePct > 0f))
            throw new IllegalArgumentException("scalePct deve essere > 0");
        final float scale = scalePct / 100f;
        final java.util.function.Consumer<String> log = logger != null ? logger : s -> {
        };
        final BooleanSupplier isCancelled = cancellationRequested != null ? cancellationRequested : () -> false;
        throwIfCancelled(isCancelled);
        final boolean rotateEnabled = rotationOpts != null && rotationOpts.enabled;
        final String searchProcessed = rotateEnabled
                ? preprocessForMatch(rotationOpts.searchText, rotationOpts.ignoreCase, rotationOpts.normalizeAccents)
                : "";
        int rotatedPages = 0;

        PdfVersion targetVersion = pdfVersion != null ? pdfVersion : PdfVersion.PDF_1_7;
        try (PdfReader reader = new PdfReader(inputPath);
                PdfWriter writer = new PdfWriter(outputPath,
                        new WriterProperties()
                                .setFullCompressionMode(true)
                                .setCompressionLevel(PDF_COMPRESSION_LEVEL)
                                .setPdfVersion(targetVersion))) {
            writer.setSmartMode(smartMode);
            try (PdfDocument src = new PdfDocument(reader);
                    PdfDocument dst = new PdfDocument(writer)) {

            int total = src.getNumberOfPages();
            for (int i = 1; i <= total; i++) {
                throwIfCancelled(isCancelled);
                PdfPage srcPage = src.getPage(i);
                Rectangle pageRect = srcPage.getPageSizeWithRotation();
                Rectangle targetPageRect = forceA4BeforeResize ? a4PageSizeFor(pageRect) : pageRect;
                boolean rotateCurrentPage = false;
                if (rotateEnabled) {
                    String text = PdfTextExtractor.getTextFromPage(srcPage, new SimpleTextExtractionStrategy());
                    String processed = preprocessForMatch(text, rotationOpts.ignoreCase, rotationOpts.normalizeAccents);
                    if (processed.contains(searchProcessed)) {
                        rotateCurrentPage = true;
                    }
                }

                PdfPage newPage = dst.addNewPage(new PageSize(targetPageRect));
                PdfCanvas canvas = new PdfCanvas(newPage);

                PdfFormXObject xobj = srcPage.copyAsFormXObject(dst);
                boolean applyResizeCurrentPage = !rotateCurrentPage || rotationOpts == null
                        || rotationOpts.applyResizeOnMatchedPages;
                // Ridisegniamo il contenuto con una matrice di trasformazione affine
                // che applica scala uniforme e traslazione.
                canvas.addXObjectWithTransformationMatrix(
                        xobj,
                        applyResizeCurrentPage ? scale : 1f, 0,
                        0, applyResizeCurrentPage ? scale : 1f,
                        applyResizeCurrentPage ? offsetXpt : 0f,
                        applyResizeCurrentPage ? offsetYpt : 0f);
                if (rotateCurrentPage) {
                    rotatePageClockwise(newPage, rotationOpts.clockwiseDegrees);
                    rotatedPages++;
                    log.accept("Resize: ruotata pagina " + i + " di +" + rotationOpts.clockwiseDegrees + " gradi.");
                    if (!applyResizeCurrentPage) {
                        log.accept("Resize: sulla pagina " + i + " ruotata non e' stato applicato il resize.");
                    }
                }
            }
                throwIfCancelled(isCancelled);
                if (rotateEnabled) {
                    if (rotatedPages == 0) {
                        log.accept("Resize: nessuna pagina trovata con la stringa per la rotazione.");
                    } else {
                        log.accept("Resize: pagine ruotate in totale: " + rotatedPages);
                    }
                }
            }
        }
    }

    private static PageSize a4PageSizeFor(Rectangle sourcePageRect) {
        if (sourcePageRect != null && sourcePageRect.getWidth() > sourcePageRect.getHeight()) {
            return PageSize.A4.rotate();
        }
        return PageSize.A4;
    }

    private static void rotatePageClockwise(PdfPage page, int clockwiseDegrees) {
        if (page == null) {
            return;
        }
        int currentRotation = page.getRotation();
        int normalizedCurrent = ((currentRotation % 360) + 360) % 360;
        int nextRotation = (normalizedCurrent + clockwiseDegrees) % 360;
        page.setRotation(nextRotation);
    }

    private static int normalizeRotationDegrees(int clockwiseDegrees) {
        int normalized = clockwiseDegrees % 360;
        if (normalized < 0) {
            normalized += 360;
        }
        if (normalized == 0) {
            throw new IllegalArgumentException("La rotazione deve essere diversa da 0.");
        }
        if ((normalized % 90) != 0) {
            throw new IllegalArgumentException("La rotazione deve essere multipla di 90 gradi.");
        }
        return normalized;
    }

    private static void throwIfCancelled(BooleanSupplier cancellationRequested) {
        if (cancellationRequested != null && cancellationRequested.getAsBoolean()) {
            throw new CancellationException("Operazione interrotta dall'utente.");
        }
    }

    private static String preprocessForMatch(String value, boolean ignoreCase, boolean normalizeAccents) {
        if (value == null) {
            return "";
        }
        String out = value;
        if (normalizeAccents && mayContainDiacritics(out)) {
            out = COMBINING_MARKS_PATTERN.matcher(Normalizer.normalize(out, Normalizer.Form.NFD))
                    .replaceAll("");
        }
        if (ignoreCase) {
            out = out.toLowerCase(Locale.ITALIAN);
        }
        out = out.replace('\u00A0', ' ');
        out = HORIZONTAL_SPACES_PATTERN.matcher(out).replaceAll(" ");
        return out.trim();
    }

    private static boolean mayContainDiacritics(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > 0x7F) {
                return true;
            }
        }
        return false;
    }

    /**
     * Estrae solo la prima pagina del PDF sorgente e la salva in un nuovo file.
     *
     * @param inputPath  PDF sorgente
     * @param outputPath PDF destinazione contenente esclusivamente la prima pagina
     */
    public static void copyFirstPage(
            String inputPath,
            String outputPath,
            PdfVersion pdfVersion) throws Exception {

        PdfVersion targetVersion = pdfVersion != null ? pdfVersion : PdfVersion.PDF_1_7;
        try (PdfReader reader = new PdfReader(inputPath);
                PdfWriter writer = new PdfWriter(outputPath,
                        new WriterProperties()
                                .setFullCompressionMode(true)
                                .setCompressionLevel(PDF_COMPRESSION_LEVEL)
                                .setPdfVersion(targetVersion))) {
            writer.setSmartMode(true);
            try (PdfDocument src = new PdfDocument(reader);
                    PdfDocument dst = new PdfDocument(writer)) {
                int total = src.getNumberOfPages();
                if (total < 1) {
                    throw new IllegalArgumentException("Il PDF sorgente non contiene pagine.");
                }
                src.copyPagesTo(1, 1, dst);
            }
        }
    }

    /*
     * =================================
     * === ANTEPRIMA RETICOLO/MM ===
     * =================================
     */

    /**
     * Genera un PDF di anteprima con reticolo mm, righelli e croce in (xpt, ypt).
     * Utile per calibrare la posizione del barcode e del contenuto.
     * Se {@code allPages} ? false esporta esclusivamente la prima pagina.
     *
     * @param inputPath  PDF sorgente
     * @param outputPath PDF destinazione (anteprima)
     * @param xpt        posizione X (pt) della croce/reticolo principale
     * @param ypt        posizione Y (pt) della croce/reticolo principale
     * @param allPages   true = applica a tutte le pagine; false = solo prima pagina
     * @param gridStepMm passo reticolo in mm (es. 10 mm)
     */
    public static void createReticlePreview(
            String inputPath,
            String outputPath,
            float xpt,
            float ypt,
            boolean allPages,
            float gridStepMm,
            PdfVersion pdfVersion) throws Exception {
        createReticlePreview(inputPath, outputPath, xpt, ypt, allPages, gridStepMm, null, null, null, null, null, null,
                pdfVersion);
    }

    public static void createReticlePreview(
            String inputPath,
            String outputPath,
            float xpt,
            float ypt,
            boolean allPages,
            float gridStepMm,
            BarcodeOpts barcodeOpts,
            PageCounterOpts pageCounterOpts,
            RaccomandataBarcodeOpts raccomandataOpts,
            QrCodeOpts qrCodeOpts,
            String dataMatrixPayload,
            PostaEvolutionDataMatrixOpts dataMatrixOpts,
            PdfVersion pdfVersion) throws Exception {

        float stepPt = mm(Math.max(1f, gridStepMm)); // evita 0 o negativo

        PdfVersion targetVersion = pdfVersion != null ? pdfVersion : PdfVersion.PDF_1_7;
        try (PdfReader reader = new PdfReader(inputPath);
                PdfWriter writer = new PdfWriter(outputPath,
                        new WriterProperties()
                                .setFullCompressionMode(true)
                                .setCompressionLevel(PDF_COMPRESSION_LEVEL)
                                .setPdfVersion(targetVersion))) {
            writer.setSmartMode(true);
            try (PdfDocument src = new PdfDocument(reader);
                    PdfDocument dst = new PdfDocument(writer)) {

                int total = src.getNumberOfPages();
                int last = allPages ? total : 1;

                for (int i = 1; i <= last; i++) {
                    src.copyPagesTo(i, i, dst);
                    // Disegniamo reticolo e anteprime solo sulle pagine richieste.
                    drawReticleOn(dst, i, xpt, ypt, stepPt, barcodeOpts, raccomandataOpts, qrCodeOpts,
                            dataMatrixPayload, dataMatrixOpts);
                }

                // Eventualmente sovrapponiamo anche il contatore pagina per facilitare la
                // verifica.
                drawGroupPageCounters(dst, 1, last, pageCounterOpts);
            }
        }
    }

    /*
     * ===========================
     * === PAGE COUNTER OPTS ===
     * ===========================
     */

    public static class PageCounterOpts {
        public final boolean enabled;
        public final float offsetLeftPt;
        public final float offsetTopPt;
        public final float fontSizePt;
        public final boolean vertical;

        public PageCounterOpts(boolean enabled, float offsetLeftPt, float offsetTopPt, float fontSizePt,
                boolean vertical) {
            this.enabled = enabled;
            this.offsetLeftPt = Math.max(0f, offsetLeftPt);
            this.offsetTopPt = Math.max(0f, offsetTopPt);
            this.fontSizePt = fontSizePt > 0f ? fontSizePt : 9f;
            this.vertical = vertical;
        }
    }

    /*
     * ============================
     * === QR CODE OPTIONS ===
     * ============================
     */

    public static class QrCodeOpts {
        public final boolean enabled;
        public final float posXpt;
        public final float posYpt;
        public final float sizePt;
        public final String baseText;
        public final long startValue;
        public final int digits;
        public final long maxProgressiveValue;
        public final ErrorCorrectionLevel errorCorrectionLevel;

        public QrCodeOpts(boolean enabled, float posXpt, float posYpt, float sizePt,
                String baseText, long startValue, int digits, String errorCorrectionLevel) {
            this.enabled = enabled;
            this.posXpt = Math.max(0f, posXpt);
            this.posYpt = Math.max(0f, posYpt);
            this.sizePt = Math.max(1f, sizePt);
            this.baseText = normalizeFreeText(baseText);
            this.startValue = Math.max(0L, startValue);
            this.digits = Math.max(1, digits);
            if (this.digits > 18) {
                throw new IllegalArgumentException("Il numero di cifre per il QR code non puo' superare 18.");
            }
            this.maxProgressiveValue = maxValueForDigits(this.digits);
            this.errorCorrectionLevel = resolveErrorCorrection(errorCorrectionLevel);
            if (this.enabled && this.baseText.isEmpty()) {
                throw new IllegalArgumentException("La base alfanumerica del QR code non puo' essere vuota.");
            }
            if (this.startValue > this.maxProgressiveValue) {
                throw new IllegalArgumentException(
                        "Il progressivo iniziale del QR code eccede le " + this.digits + " cifre disponibili.");
            }
        }

        private static ErrorCorrectionLevel resolveErrorCorrection(String level) {
            if (level == null) {
                return ErrorCorrectionLevel.M;
            }
            switch (level.trim().toUpperCase(Locale.ITALIAN)) {
                case "L":
                    return ErrorCorrectionLevel.L;
                case "Q":
                    return ErrorCorrectionLevel.Q;
                case "H":
                    return ErrorCorrectionLevel.H;
                case "M":
                default:
                    return ErrorCorrectionLevel.M;
            }
        }
    }

    public enum CorrectionOverlayKind {
        NONE,
        QUESTION,
        SEARCH,
        TEXT
    }

    public static class CorrectionOverlayEntry {
        public final CorrectionOverlayKind kind;
        public final String text;
        public final boolean searchIcon;
        public final boolean plusSearchIcon;
        public final boolean xIcon;

        public CorrectionOverlayEntry(CorrectionOverlayKind kind, String text) {
            this(kind, text, false);
        }

        public CorrectionOverlayEntry(CorrectionOverlayKind kind, String text, boolean searchIcon) {
            this(kind, text, searchIcon, false);
        }

        public CorrectionOverlayEntry(CorrectionOverlayKind kind, String text, boolean searchIcon,
                boolean plusSearchIcon) {
            this(kind, text, searchIcon, plusSearchIcon, false);
        }

        public CorrectionOverlayEntry(CorrectionOverlayKind kind, String text, boolean searchIcon,
                boolean plusSearchIcon, boolean xIcon) {
            this.kind = kind == null ? CorrectionOverlayKind.NONE : kind;
            this.text = normalizeFreeText(text);
            this.searchIcon = searchIcon || plusSearchIcon;
            this.plusSearchIcon = plusSearchIcon;
            this.xIcon = xIcon;
        }

        public static CorrectionOverlayEntry none() {
            return new CorrectionOverlayEntry(CorrectionOverlayKind.NONE, "");
        }
    }

    public static class CorrectionOverlayOpts {
        public final boolean enabled;
        public final float posXpt;
        public final float posYpt;
        public final float areaWidthPt;
        public final float fontSizePt;
        public final float iconSizePt;
        public final long startValue;

        public CorrectionOverlayOpts(boolean enabled, float posXpt, float posYpt, float areaWidthPt, float fontSizePt,
                float iconSizePt, long startValue) {
            this.enabled = enabled;
            this.posXpt = Math.max(0f, posXpt);
            this.posYpt = Math.max(0f, posYpt);
            this.areaWidthPt = areaWidthPt > 0f ? areaWidthPt : mm(50f);
            this.fontSizePt = fontSizePt > 0f ? fontSizePt : 4.5f;
            this.iconSizePt = iconSizePt > 0f ? iconSizePt : mm(5f);
            this.startValue = Math.max(0L, startValue);
        }
    }

    public static class PostaEvolutionDataMatrixOpts {
        public boolean enabled;
        public float posXpt;
        public float posYpt;
        public float moduleSizePt;
        public int widthCells;
        public int heightCells;
        public int quietZoneCells;

        public PostaEvolutionDataMatrixOpts(
                boolean enabled,
                float posXpt,
                float posYpt,
                float moduleSizePt) {
            this(enabled, posXpt, posYpt, moduleSizePt, 48, 16, 2);
        }

        public PostaEvolutionDataMatrixOpts(
                boolean enabled,
                float posXpt,
                float posYpt,
                float moduleSizePt,
                int widthCells,
                int heightCells,
                int quietZoneCells) {
            this.enabled = enabled;
            this.posXpt = posXpt;
            this.posYpt = posYpt;
            this.moduleSizePt = Math.max(0.1f, moduleSizePt);
            this.widthCells = Math.max(1, widthCells);
            this.heightCells = Math.max(1, heightCells);
            this.quietZoneCells = Math.max(2, quietZoneCells);
        }
    }

    private static String buildQrCodeContent(QrCodeOpts opts, int groupIndex) {
        long value = opts.startValue + (groupIndex - 1L);
        if (value > opts.maxProgressiveValue) {
            throw new IllegalArgumentException(
                    "Progressivo QR eccede le " + opts.digits + " cifre disponibili.");
        }
        String progressive = String.format(Locale.US, "%0" + opts.digits + "d", value);
        return opts.baseText + progressive;
    }

    /**
     * Disegna righelli (in mm), reticolo e croce alla posizione indicata sulla
     * pagina.
     */
    private static void drawReticleOn(
            PdfDocument dst,
            int pageNo,
            float xpt,
            float ypt,
            float stepPt,
            BarcodeOpts barcodeOpts,
            RaccomandataBarcodeOpts raccomandataOpts,
            QrCodeOpts qrCodeOpts,
            String dataMatrixPayload,
            PostaEvolutionDataMatrixOpts dataMatrixOpts) throws Exception {
        PdfPage page = dst.getPage(pageNo);
        Rectangle r = page.getPageSizeWithRotation();
        float W = r.getWidth();
        float H = r.getHeight();

        PdfCanvas c = new PdfCanvas(page);

        // Reticolo principale (griglia) ogni stepPt, grigio chiaro
        c.setLineWidth(0.25f).setStrokeColor(ColorConstants.LIGHT_GRAY);
        for (float x = 0; x <= W; x += stepPt)
            c.moveTo(x, 0).lineTo(x, H);
        for (float y = 0; y <= H; y += stepPt)
            c.moveTo(0, y).lineTo(W, y);
        c.stroke();

        // Righello orizzontale (basso) e verticale (sinistra)
        c.setLineWidth(0.6f).setStrokeColor(ColorConstants.GRAY);

        // Orizzontale bottom
        c.moveTo(0, 0).lineTo(W, 0).stroke();
        for (float x = 0, mmCount = 0; x <= W; x += mm(1), mmCount += 1) {
            float len = (mmCount % 10 == 0) ? mm(4) : (mmCount % 5 == 0 ? mm(3) : mm(2));
            c.moveTo(x, 0).lineTo(x, len);
        }
        c.stroke();

        // Verticale left
        c.moveTo(0, 0).lineTo(0, H).stroke();
        for (float y = 0, mmCount = 0; y <= H; y += mm(1), mmCount += 1) {
            float len = (mmCount % 10 == 0) ? mm(4) : (mmCount % 5 == 0 ? mm(3) : mm(2));
            c.moveTo(0, y).lineTo(len, y);
        }
        c.stroke();

        // Etichette ogni 10 mm (numeri mm)
        // Per evitare ripetizioni di creazione font lo recuperiamo una sola volta.
        PdfFont labelFont = Font.fontHELVETICA(dst);
        c.beginText();
        c.setFontAndSize(labelFont, 6);
        c.setFillColor(ColorConstants.DARK_GRAY);

        // Bottom labels
        for (float x = 0, mmCount = 0; x <= W; x += mm(10), mmCount += 10) {
            c.setTextMatrix(x + mm(1), mm(5));
            c.showText(Integer.toString((int) mmCount) + " mm");
        }
        // Left labels
        for (float y = 0, mmCount = 0; y <= H; y += mm(10), mmCount += 10) {
            c.setTextMatrix(mm(2), y + mm(2));
            c.showText(Integer.toString((int) mmCount) + " mm");
        }
        c.endText();

        // Croce nel punto (xpt,ypt)
        float cross = mm(6);
        c.setLineWidth(0.8f).setStrokeColor(ColorConstants.RED);
        c.moveTo(xpt - cross, ypt).lineTo(xpt + cross, ypt);
        c.moveTo(xpt, ypt - cross).lineTo(xpt, ypt + cross);
        c.stroke();

        // Box attorno al punto (per maggiore visibilit?)
        float box = mm(12);
        c.setLineWidth(0.6f).setStrokeColor(ColorConstants.RED);
        c.rectangle(xpt - box / 2f, ypt - box / 2f, box, box);
        c.stroke();

        // Disegna area barcode + testo anteprima se richiesto
        if (barcodeOpts != null && barcodeOpts.enabled) {
            String previewCode = buildPreviewBarcodeCode(barcodeOpts);
            Rectangle barcodeRect = calculateBarcodePreviewRect(dst, previewCode, barcodeOpts);

            c.setLineWidth(1.0f).setStrokeColor(ColorConstants.RED);
            c.rectangle(barcodeRect.getX(), barcodeRect.getY(), barcodeRect.getWidth(), barcodeRect.getHeight());
            c.stroke();

            PdfFont previewFont = Font.fontHELVETICA(dst);
            float textSize = 8f;
            c.beginText();
            c.setFontAndSize(previewFont, textSize);
            c.setFillColor(ColorConstants.RED);
            float textX = barcodeRect.getX();
            float textY = barcodeRect.getY() + barcodeRect.getHeight() + mm(2f);
            if (textY + textSize > H) {
                textY = barcodeRect.getY() - mm(4f);
            }
            c.setTextMatrix(textX, textY);
            if (barcodeOpts.useOmr) {
                c.showText("OMR anteprima: " + previewCode);
            } else {
                c.showText("Codice anteprima: " + previewCode);
            }
            c.endText();
        }

        // Preview etichetta in blu
        if (barcodeOpts != null && hasLabel(barcodeOpts)) {
            drawLabelPreview(dst, c, barcodeOpts);
        }

        if (raccomandataOpts != null) {
            boolean hasOmologazione = hasRaccomandataOmologazione(raccomandataOpts);
            boolean hasCustomText = hasRaccomandataCustomText(raccomandataOpts);
            if (raccomandataOpts.enabled) {
                RaccomandataCode previewCode = buildRaccomandataCode(raccomandataOpts, 1);
                Rectangle racRect = calculateRaccomandataPreviewRect(dst, previewCode, raccomandataOpts);

                c.setLineWidth(1.0f).setStrokeColor(ColorConstants.BLUE);
                c.rectangle(racRect.getX(), racRect.getY(), racRect.getWidth(), racRect.getHeight());
                c.stroke();

                PdfFont previewFont = Font.fontHELVETICA(dst);
                float textSize = 8f;
                c.beginText();
                c.setFontAndSize(previewFont, textSize);
                c.setFillColor(ColorConstants.BLUE);
                float textX = racRect.getX();
                float textY = racRect.getY() + racRect.getHeight() + mm(2f);
                if (textY + textSize > H) {
                    textY = racRect.getY() - mm(4f);
                }
                c.setTextMatrix(textX, textY);
                c.showText("Raccomandata: " + previewCode.printable());
                c.endText();

                if (hasOmologazione) {
                    drawOmologazionePreview(dst, c, raccomandataOpts);
                }
                if (hasCustomText) {
                    drawCustomTextPreview(dst, c, raccomandataOpts);
                }
            } else if (hasOmologazione) {
                drawOmologazionePreview(dst, c, raccomandataOpts);
                if (hasCustomText) {
                    drawCustomTextPreview(dst, c, raccomandataOpts);
                }
            } else if (hasCustomText) {
                drawCustomTextPreview(dst, c, raccomandataOpts);
            }
        }

        if (qrCodeOpts != null && qrCodeOpts.enabled) {
            String sampleContent = buildQrCodeContent(qrCodeOpts, 1);
            drawQrPreview(dst, c, qrCodeOpts, sampleContent, H);
        }

        if (dataMatrixOpts != null && dataMatrixOpts.enabled && dataMatrixPayload != null
                && !dataMatrixPayload.isBlank()) {
            renderDataMatrix(dst, pageNo, dataMatrixPayload, dataMatrixOpts);
        }
    }

    private static Rectangle calculateBarcodePreviewRect(PdfDocument dst, String code, BarcodeOpts opts) {
        if (opts.useOmr) {
            return calculateOmrPreviewRect(opts);
        }
        Barcode39 bc = new Barcode39(dst);
        bc.setCode(code);
        bc.setBarHeight(Math.max(1f, opts.heightPt));
        bc.setX(opts.moduleWidthPt);
        bc.setTextAlignment(BarcodeStandard.TEXT_ALIGNMENT);
        bc.setSize(BarcodeStandard.FONT_SIZE_PT);

        Image img = new Image(bc.createFormXObject(dst));
        img.setAutoScale(false);
        float width = img.getImageScaledWidth();
        float height = img.getImageScaledHeight();

        if ((int) BarcodeStandard.ROTATION_DEG % 180 != 0) {
            // La preview e' calcolata rispetto all'orientamento finale (verticale).
            float tmp = width;
            width = height;
            height = tmp;
        }

        return new Rectangle(
                opts.posXpt,
                opts.posYpt + BarcodeStandard.Y_OFFSET_PT,
                width,
                height);
    }

    private static Rectangle calculateOmrPreviewRect(BarcodeOpts opts) {
        float lineWidth = OmrStandard.BAR_THICKNESS_PT;
        float barLength = OmrStandard.BAR_LENGTH_PT;
        int bitCount = (opts != null && opts.useOmr && opts.allegatiPresenti)
                ? OmrStandard.BIT_COUNT_WITH_MC
                : OmrStandard.BIT_COUNT;
        float totalHeight = ((bitCount - 1) * OmrStandard.ROW_SPACING_PT) + lineWidth;
        float yTop = OmrStandard.START_Y_PT + resolveOmrVerticalShift(bitCount) + (lineWidth / 2f);
        float yBottom = yTop - totalHeight;
        return new Rectangle(
                OmrStandard.START_X_PT,
                yBottom,
                barLength,
                totalHeight);
    }

    private static Rectangle calculateRaccomandataPreviewRect(
            PdfDocument dst,
            RaccomandataCode code,
            RaccomandataBarcodeOpts opts) throws Exception {
        BarcodeInter25 bc = new BarcodeInter25(dst);
        // bc.setChecksum(false);
        bc.setCode(code.encodedDigits);
        bc.setBarHeight(Math.max(1f, opts.barHeightPt));
        bc.setX(Math.max(0.1f, opts.moduleNarrowPt));
        bc.setN(Math.max(1.1f, opts.wideToNarrowRatio));
        bc.setFont(null);

        Image img = new Image(bc.createFormXObject(dst));
        img.setAutoScale(false);
        float barcodeWidth = img.getImageScaledWidth();
        float barcodeHeight = img.getImageScaledHeight();

        PdfFont font = Font.fontHELVETICA(dst);
        float prefixExtraWidth = 0f;
        if (!opts.productPrefix.isEmpty()) {
            PdfFont font1 = Font.fontARIALBold(dst);
            float prefixSize = opts.prefixFontSizePt;
            float prefixWidth = font1.getWidth(opts.productPrefix, prefixSize);
            // Lo spazio laterale tiene conto sia del testo sia del gap richiesto.
            prefixExtraWidth = prefixWidth + opts.prefixGapPt;
        }

        float previewBottomExtension = 0f;
        if (opts.humanReadableFontSizePt > 0f) {
            String printable = code.printable();
            float digitsAscent = font.getAscent(printable, opts.humanReadableFontSizePt);
            float digitsDescent = font.getDescent(printable, opts.humanReadableFontSizePt);
            float digitsHeight = digitsAscent - digitsDescent;
            // La preview lascia spazio per il testo leggibile e per il gap richiesto.
            previewBottomExtension = opts.humanReadableGapPt + digitsHeight;
        }

        float x = opts.posXpt - prefixExtraWidth;
        float width = barcodeWidth + prefixExtraWidth;
        float y = opts.posYpt - previewBottomExtension;
        float height = barcodeHeight + previewBottomExtension;
        if (y < 0f) {
            height += y; // Se scendiamo sotto lo zero, riduciamo l'altezza visibile.
            y = 0f;
        }
        if (height < 0f) {
            height = barcodeHeight;
        }
        return new Rectangle(x, y, width, height);
    }

    private static String buildPreviewBarcodeCode(BarcodeOpts opts) {
        if (opts.useOmr) {
            int previewGroupNumber = Math.max(1, opts.groupStartProgressive);
            return buildOmrBinary(1, true, previewGroupNumber, opts);
        }
        final int totalSheets = 1;
        final int sheetIndex = 1;

        // I valori sono calcolati per un singolo foglio fittizio da mostrare in
        // anteprima.
        char d1 = '3';
        char d2 = (opts.allegatiPresenti && sheetIndex == totalSheets) ? '1' : '0';
        int sequenceBase = opts.targetPdfIsAttachment ? sheetIndex : 1;
        sequenceBase = Math.max(1, sequenceBase);
        String d34 = twoDigits(((sequenceBase - 1) % 99) + 1);
        int groupProgressive = Math.max(1, opts.groupStartProgressive);
        String groupProg6 = sixDigits(groupProgressive);

        return "" + d1 + d2 + d34 + groupProg6;
    }

    private static void drawOmologazionePreview(PdfDocument dst, PdfCanvas canvas, RaccomandataBarcodeOpts opts)
            throws Exception {
        drawTextPreview(dst, canvas, opts.omologazioneText, opts.omologazionePosXpt, opts.omologazionePosYpt,
                opts.omologazioneFontSizePt);
    }

    private static void drawCustomTextPreview(PdfDocument dst, PdfCanvas canvas, RaccomandataBarcodeOpts opts)
            throws Exception {
        drawTextPreview(dst, canvas, opts.customText, opts.customTextPosXpt, opts.customTextPosYpt,
                opts.customTextFontSizePt);
    }

    private static void drawTextPreview(PdfDocument dst, PdfCanvas canvas, String text, float xpt, float ypt,
            float fontSizePt) throws Exception {
        PdfFont font = Font.fontHELVETICA(dst);
        float size = fontSizePt;
        float width = font.getWidth(text, size);
        float ascent = font.getAscent(text, size);
        float descent = font.getDescent(text, size);
        float baseline = ypt;
        float bottom = baseline + descent;
        float height = ascent - descent;

        canvas.saveState();
        canvas.setLineWidth(0.8f).setStrokeColor(ColorConstants.BLUE);
        // Disegniamo un riquadro per capire rapidamente lo spazio occupato dal testo.
        canvas.rectangle(xpt, bottom, width, height);
        canvas.stroke();

        canvas.beginText();
        canvas.setFontAndSize(font, size);
        canvas.setFillColor(ColorConstants.BLUE);
        canvas.setTextMatrix(xpt, baseline);
        canvas.showText(text);
        canvas.endText();

        canvas.restoreState();
    }

    private static void drawQrPreview(PdfDocument dst, PdfCanvas canvas, QrCodeOpts opts, String sampleContent,
            float pageHeight)
            throws Exception {
        float size = Math.max(1f, opts.sizePt);

        canvas.saveState();
        canvas.setLineWidth(1.0f).setStrokeColor(ColorConstants.GREEN);
        // Evidenziamo l'area quadrata occupata dal QR code.
        canvas.rectangle(opts.posXpt, opts.posYpt, size, size);
        canvas.stroke();

        PdfFont font = Font.fontHELVETICA(dst);
        float textSize = 8f;
        float width = font.getWidth(sampleContent, textSize);
        float ascent = font.getAscent(sampleContent, textSize);
        float descent = font.getDescent(sampleContent, textSize);
        float textHeight = ascent - descent;
        float gap = mm(2f);
        float textX = opts.posXpt - gap - width;
        if (textX < 0f) {
            textX = 0f;
        }
        float baseline = opts.posYpt + (opts.sizePt - textHeight) / 2f - descent;
        if (baseline < 0f) {
            baseline = 0f;
        }
        if (baseline + textHeight > pageHeight) {
            baseline = Math.max(0f, pageHeight - textHeight);
        }
        canvas.beginText();
        canvas.setFontAndSize(font, textSize);
        canvas.setFillColor(ColorConstants.GREEN);
        canvas.setTextMatrix(textX, baseline);
        canvas.showText(sampleContent);
        canvas.endText();
        canvas.restoreState();
    }

    private static void drawLabelPreview(PdfDocument dst, PdfCanvas canvas, BarcodeOpts opts) throws Exception {
        float lineLength = mm(20);
        canvas.saveState();
        canvas.setLineWidth(0.8f).setStrokeColor(ColorConstants.BLUE);
        if (opts.labelVertical) {
            // Linea guida che indica il verso dell'etichetta.
            canvas.moveTo(opts.labelPosXpt, opts.labelPosYpt)
                    .lineTo(opts.labelPosXpt, opts.labelPosYpt + lineLength)
                    .stroke();
        } else {
            canvas.moveTo(opts.labelPosXpt, opts.labelPosYpt)
                    .lineTo(opts.labelPosXpt + lineLength, opts.labelPosYpt)
                    .stroke();
        }

        PdfFont font = Font.fontHELVETICA(dst);
        String text = formatGroupLabel(opts.groupStartProgressive, opts.lavorazioneId);
        float labelSize = Math.max(6f, opts.labelFontSizePt);
        canvas.beginText();
        canvas.setFontAndSize(font, labelSize);
        canvas.setFillColor(ColorConstants.BLUE);
        float tx = opts.labelPosXpt + (opts.labelVertical ? mm(2f) : 0f);
        float ty = opts.labelPosYpt + (opts.labelVertical ? mm(2f) : mm(2f));
        if (opts.labelVertical) {
            canvas.setTextMatrix(0, 1, -1, 0, tx, ty);
        } else {
            canvas.setTextMatrix(1, 0, 0, 1, tx, ty);
        }
        canvas.showText(text);
        canvas.endText();
        canvas.restoreState();
    }
}
