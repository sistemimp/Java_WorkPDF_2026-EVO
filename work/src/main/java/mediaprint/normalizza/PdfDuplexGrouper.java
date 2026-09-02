package mediaprint.normalizza;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.filter.TextRegionEventFilter;
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy;
import com.itextpdf.kernel.pdf.canvas.parser.listener.FilteredTextEventListener;
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy;

import mediaprint.imbustatrice.Imbustatrice;
import mediaprint.imbustatrice.Imbustatrice.BarcodeOpts;
import mediaprint.imbustatrice.Imbustatrice.PageCounterOpts;
import mediaprint.imbustatrice.Imbustatrice.QrCodeOpts;
import mediaprint.imbustatrice.Imbustatrice.RaccomandataBarcodeOpts;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PdfDuplexGrouper {

    private static final String REPORT_FILE_NAME = "DettaglioElaborazione.xlsx";
    private static final String REPORT_SHEET_NAME = "Dettaglio";
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String DU_SUFFIX = ".DU";
    private static final int PDF_COMPRESSION_LEVEL = 6;
    private static final Pattern COMBINING_MARKS_PATTERN = Pattern.compile("\\p{M}+");
    private static final Pattern HORIZONTAL_SPACES_PATTERN = Pattern.compile("[ \\t\\x0B\\f\\r]+");

    private static final class GroupReport {
        final int groupNumber;
        final int pageFrom;
        final int pageTo;
        final int pageCount;
        final AddressComponents address;
        final String qrValue;
        final RaccomandataInfo raccomandata;
        final String dataMatrixValue;

        GroupReport(int groupNumber, int pageFrom, int pageTo, int pageCount, AddressComponents address,
                String qrValue, RaccomandataInfo raccomandata, String dataMatrixValue) {
            this.groupNumber = groupNumber;
            this.pageFrom = pageFrom;
            this.pageTo = pageTo;
            this.pageCount = pageCount;
            this.address = address;
            this.qrValue = qrValue;
            this.raccomandata = raccomandata;
            this.dataMatrixValue = dataMatrixValue;
        }
    }

    public static final class AddressBlockOpts {
        public final boolean enabled;
        public final float posXpt;
        public final float posYpt;
        public final float widthPt;
        public final float heightPt;

        public AddressBlockOpts(boolean enabled, float posXpt, float posYpt, float widthPt, float heightPt) {
            this.enabled = enabled;
            this.posXpt = posXpt;
            this.posYpt = posYpt;
            this.widthPt = widthPt;
            this.heightPt = heightPt;
        }
    }

    public static final class PageRotationByTextOpts {
        public final boolean enabled;
        public final String searchText;
        public final int clockwiseDegrees;

        public PageRotationByTextOpts(boolean enabled, String searchText, int clockwiseDegrees) {
            this.enabled = enabled;
            this.searchText = safe(searchText);
            this.clockwiseDegrees = normalizeRotationDegrees(clockwiseDegrees);
            if (this.enabled && this.searchText.isEmpty()) {
                throw new IllegalArgumentException("La stringa per la rotazione pagina non puo' essere vuota.");
            }
        }
    }

    public static final class AddressComponents {
        public final String rawText;
        public final String nominativo;
        public final String indirizzo;
        public final String cap;
        public final String comune;
        public final String provincia;

        AddressComponents(String rawText, String nominativo, String indirizzo, String cap, String comune,
                String provincia) {
            this.rawText = rawText;
            this.nominativo = nominativo;
            this.indirizzo = indirizzo;
            this.cap = cap;
            this.comune = comune;
            this.provincia = provincia;
        }

        public static AddressComponents empty() {
            return new AddressComponents("", "", "", "", "", "");
        }

    }

    public static final class KeyedStringReadResult {
        public final Path excelPath;
        public final int rowCount;

        KeyedStringReadResult(Path excelPath, int rowCount) {
            this.excelPath = excelPath;
            this.rowCount = rowCount;
        }
    }

    private static final class RaccomandataInfo {
        final String formatted;
        final String digitsOnly;

        RaccomandataInfo(String formatted, String digitsOnly) {
            this.formatted = formatted;
            this.digitsOnly = digitsOnly;
        }

        static RaccomandataInfo empty() {
            return new RaccomandataInfo("", "");
        }
    }

    public static final class PostaEvolutionOpts {
        public final boolean enabled;
        public final Imbustatrice.PostaEvolutionDataMatrixOpts dataMatrixOpts;
        public final String addressExcelPath;

        public final String gamma;
        public final String idClienteSap;
        public final String idClienteMittente;
        public final String classe;
        public final String tipoProdotto;
        public final String capDestinatarioFallback;
        public final String codiceTecnicoDestinatario;
        public final String capMittente;
        public final String codiceTecnicoMittente;
        public final String codicePrenotazioneFiglio;
        public final String identificativoStampatore;
        public final long identificativoOggettoStart;
        public final String causale;
        public final String codiceOmologazioneDatamatrix;
        public final String disponibileCliente;
        public final String serviziAccessori;

        public final boolean duEnabled;
        public final String duTipoAccettazioneFile;
        public final String duProgressivo;
        public final String duSiglaIds;
        public final String duUtenzaOperatore;
        public final String duIdPrenotazione;
        public final String duDataRicezioneFlusso;
        public final String duDataFineStampa;
        public final String duDataPostalizzazione;
        public final String duFrazionarioCentro;
        public final String duTipologiaProdotto;
        public final String duCodiceProdotto;
        public final String duCodiceServizioAccessorio;
        public final String duCodiceTipologiaAccettazione;
        public final String duTipologiaCodiceTracciatura;
        public final String duCodiceContoContrattuale;
        public final String duDescrizionePostalizzazione;
        public final String duCodiceOmologazione;
        public final String duFormato;
        public final String duIdHu;
        public final String duIdScatola;
        public final String duIdUtenzaCliente;
        public final String duNumeroFattura;
        public final String duDataScadenzaFattura;
        public final String duSpare1;
        public final String duSpare2;
        public final String duSpare3;
        public final String duSpare4;
        public final String duSpare5;
        public final String duSpare6;
        public final String duSpare7;
        public final String duSpare8;
        public final String duSpare9;
        public final String duSpare10;

        public PostaEvolutionOpts(
                boolean enabled,
                Imbustatrice.PostaEvolutionDataMatrixOpts dataMatrixOpts,
                String addressExcelPath,
                String gamma,
                String idClienteSap,
                String idClienteMittente,
                String classe,
                String tipoProdotto,
                String capDestinatarioFallback,
                String codiceTecnicoDestinatario,
                String capMittente,
                String codiceTecnicoMittente,
                String codicePrenotazioneFiglio,
                String identificativoStampatore,
                long identificativoOggettoStart,
                String causale,
                String codiceOmologazioneDatamatrix,
                String disponibileCliente,
                String serviziAccessori,
                boolean duEnabled,
                String duTipoAccettazioneFile,
                String duProgressivo,
                String duSiglaIds,
                String duUtenzaOperatore,
                String duIdPrenotazione,
                String duDataRicezioneFlusso,
                String duDataFineStampa,
                String duDataPostalizzazione,
                String duFrazionarioCentro,
                String duTipologiaProdotto,
                String duCodiceProdotto,
                String duCodiceServizioAccessorio,
                String duCodiceTipologiaAccettazione,
                String duTipologiaCodiceTracciatura,
                String duCodiceContoContrattuale,
                String duDescrizionePostalizzazione,
                String duCodiceOmologazione,
                String duFormato,
                String duIdHu,
                String duIdScatola,
                String duIdUtenzaCliente,
                String duNumeroFattura,
                String duDataScadenzaFattura,
                String duSpare1,
                String duSpare2,
                String duSpare3,
                String duSpare4,
                String duSpare5,
                String duSpare6,
                String duSpare7,
                String duSpare8,
                String duSpare9,
                String duSpare10) {
            this.enabled = enabled;
            this.dataMatrixOpts = dataMatrixOpts;
            this.addressExcelPath = addressExcelPath;
            this.gamma = gamma;
            this.idClienteSap = idClienteSap;
            this.idClienteMittente = idClienteMittente;
            this.classe = classe;
            this.tipoProdotto = tipoProdotto;
            this.capDestinatarioFallback = capDestinatarioFallback;
            this.codiceTecnicoDestinatario = codiceTecnicoDestinatario;
            this.capMittente = capMittente;
            this.codiceTecnicoMittente = codiceTecnicoMittente;
            this.codicePrenotazioneFiglio = codicePrenotazioneFiglio;
            this.identificativoStampatore = identificativoStampatore;
            this.identificativoOggettoStart = Math.max(0L, identificativoOggettoStart);
            this.causale = causale;
            this.codiceOmologazioneDatamatrix = codiceOmologazioneDatamatrix;
            this.disponibileCliente = disponibileCliente;
            this.serviziAccessori = serviziAccessori;
            this.duEnabled = duEnabled;
            this.duTipoAccettazioneFile = duTipoAccettazioneFile;
            this.duProgressivo = duProgressivo;
            this.duSiglaIds = duSiglaIds;
            this.duUtenzaOperatore = duUtenzaOperatore;
            this.duIdPrenotazione = duIdPrenotazione;
            this.duDataRicezioneFlusso = duDataRicezioneFlusso;
            this.duDataFineStampa = duDataFineStampa;
            this.duDataPostalizzazione = duDataPostalizzazione;
            this.duFrazionarioCentro = duFrazionarioCentro;
            this.duTipologiaProdotto = duTipologiaProdotto;
            this.duCodiceProdotto = duCodiceProdotto;
            this.duCodiceServizioAccessorio = duCodiceServizioAccessorio;
            this.duCodiceTipologiaAccettazione = duCodiceTipologiaAccettazione;
            this.duTipologiaCodiceTracciatura = duTipologiaCodiceTracciatura;
            this.duCodiceContoContrattuale = duCodiceContoContrattuale;
            this.duDescrizionePostalizzazione = duDescrizionePostalizzazione;
            this.duCodiceOmologazione = duCodiceOmologazione;
            this.duFormato = duFormato;
            this.duIdHu = duIdHu;
            this.duIdScatola = duIdScatola;
            this.duIdUtenzaCliente = duIdUtenzaCliente;
            this.duNumeroFattura = duNumeroFattura;
            this.duDataScadenzaFattura = duDataScadenzaFattura;
            this.duSpare1 = duSpare1;
            this.duSpare2 = duSpare2;
            this.duSpare3 = duSpare3;
            this.duSpare4 = duSpare4;
            this.duSpare5 = duSpare5;
            this.duSpare6 = duSpare6;
            this.duSpare7 = duSpare7;
            this.duSpare8 = duSpare8;
            this.duSpare9 = duSpare9;
            this.duSpare10 = duSpare10;
        }

        public PostaEvolutionOpts(
                boolean enabled,
                Imbustatrice.PostaEvolutionDataMatrixOpts dataMatrixOpts,
                String addressExcelPath,
                String gamma,
                String idClienteSap,
                String idClienteMittente,
                String classe,
                String tipoProdotto,
                String capDestinatarioFallback,
                String codiceTecnicoDestinatario,
                String capMittente,
                String codiceTecnicoMittente,
                String codicePrenotazioneFiglio,
                String identificativoStampatore,
                long identificativoOggettoStart,
                String causale,
                String codiceOmologazioneDatamatrix,
                String disponibileCliente,
                String serviziAccessori,
                boolean duEnabled,
                String duTipoAccettazioneFile,
                String duProgressivo,
                String duSiglaIds,
                String duUtenzaOperatore,
                String duIdPrenotazione,
                String duDataRicezioneFlusso,
                String duDataFineStampa,
                String duDataPostalizzazione,
                String duFrazionarioCentro,
                String duTipologiaProdotto,
                String duCodiceProdotto,
                String duCodiceServizioAccessorio,
                String duCodiceTipologiaAccettazione,
                String duTipologiaCodiceTracciatura,
                String duCodiceContoContrattuale,
                String duDescrizionePostalizzazione,
                String duCodiceOmologazione,
                String duFormato,
                String duIdHu,
                String duIdScatola,
                String duIdUtenzaCliente,
                String duNumeroFattura,
                String duDataScadenzaFattura,
                String duSpare1,
                String duSpare2,
                String duSpare3,
                String duSpare4,
                String duSpare5,
                String duSpare6,
                String duSpare7,
                String duSpare8,
                String duSpare9) {
            this(
                    enabled,
                    dataMatrixOpts,
                    addressExcelPath,
                    gamma,
                    idClienteSap,
                    idClienteMittente,
                    classe,
                    tipoProdotto,
                    capDestinatarioFallback,
                    codiceTecnicoDestinatario,
                    capMittente,
                    codiceTecnicoMittente,
                    codicePrenotazioneFiglio,
                    identificativoStampatore,
                    identificativoOggettoStart,
                    causale,
                    codiceOmologazioneDatamatrix,
                    disponibileCliente,
                    serviziAccessori,
                    duEnabled,
                    duTipoAccettazioneFile,
                    duProgressivo,
                    duSiglaIds,
                    duUtenzaOperatore,
                    duIdPrenotazione,
                    duDataRicezioneFlusso,
                    duDataFineStampa,
                    duDataPostalizzazione,
                    duFrazionarioCentro,
                    duTipologiaProdotto,
                    duCodiceProdotto,
                    duCodiceServizioAccessorio,
                    duCodiceTipologiaAccettazione,
                    duTipologiaCodiceTracciatura,
                    duCodiceContoContrattuale,
                    duDescrizionePostalizzazione,
                    duCodiceOmologazione,
                    duFormato,
                    duIdHu,
                    duIdScatola,
                    duIdUtenzaCliente,
                    duNumeroFattura,
                    duDataScadenzaFattura,
                    duSpare1,
                    duSpare2,
                    duSpare3,
                    duSpare4,
                    duSpare5,
                    duSpare6,
                    duSpare7,
                    duSpare8,
                    duSpare9,
                    "");
        }
    }

    public static void process(
            String inputPath,
            String outputPath,
            String marker,
            boolean ignoreCase,
            boolean normalize,
            PdfVersion pdfVersion,
            Consumer<String> logger,
            BarcodeOpts barcodeOpts,
            PageCounterOpts pageCounterOpts,
            RaccomandataBarcodeOpts raccomandataOpts,
            QrCodeOpts qrCodeOpts,
            PostaEvolutionOpts postaEvolutionOpts,
            String correctionExcelPath,
            Imbustatrice.CorrectionOverlayOpts correctionOverlayOpts,
            PageRotationByTextOpts pageRotationOpts,
            AddressBlockOpts addressBlockOpts,
            IntConsumer progressListener) throws Exception {
        process(inputPath, outputPath, marker, ignoreCase, normalize, pdfVersion, logger, barcodeOpts, pageCounterOpts,
                raccomandataOpts, qrCodeOpts, postaEvolutionOpts, correctionExcelPath, correctionOverlayOpts,
                pageRotationOpts, addressBlockOpts, progressListener, null);
    }

    public static void process(
            String inputPath,
            String outputPath,
            String marker,
            boolean ignoreCase,
            boolean normalize,
            PdfVersion pdfVersion,
            Consumer<String> logger,
            BarcodeOpts barcodeOpts,
            PageCounterOpts pageCounterOpts,
            RaccomandataBarcodeOpts raccomandataOpts,
            QrCodeOpts qrCodeOpts,
            PostaEvolutionOpts postaEvolutionOpts,
            String correctionExcelPath,
            Imbustatrice.CorrectionOverlayOpts correctionOverlayOpts,
            PageRotationByTextOpts pageRotationOpts,
            AddressBlockOpts addressBlockOpts,
            IntConsumer progressListener,
            BooleanSupplier cancellationRequested) throws Exception {
        process(inputPath, outputPath, marker, ignoreCase, normalize, pdfVersion, logger, barcodeOpts, pageCounterOpts,
                raccomandataOpts, qrCodeOpts, postaEvolutionOpts, correctionExcelPath, correctionOverlayOpts,
                pageRotationOpts, addressBlockOpts, progressListener, cancellationRequested, true);
    }

    public static void process(
            String inputPath,
            String outputPath,
            String marker,
            boolean ignoreCase,
            boolean normalize,
            PdfVersion pdfVersion,
            Consumer<String> logger,
            BarcodeOpts barcodeOpts,
            PageCounterOpts pageCounterOpts,
            RaccomandataBarcodeOpts raccomandataOpts,
            QrCodeOpts qrCodeOpts,
            PostaEvolutionOpts postaEvolutionOpts,
            String correctionExcelPath,
            Imbustatrice.CorrectionOverlayOpts correctionOverlayOpts,
            PageRotationByTextOpts pageRotationOpts,
            AddressBlockOpts addressBlockOpts,
            IntConsumer progressListener,
            BooleanSupplier cancellationRequested,
            boolean smartMode) throws Exception {

        Consumer<String> log = (logger != null) ? logger : System.out::println;
        IntConsumer progress = (progressListener != null) ? progressListener : value -> {
        };
        BooleanSupplier isCancelled = cancellationRequested != null ? cancellationRequested : () -> false;
        throwIfCancelled(isCancelled);
        if (marker == null || marker.trim().isEmpty()) {
            throw new IllegalArgumentException("Il marker non puo' essere vuoto.");
        }
        final String markerProcessed = preprocess(marker, ignoreCase, normalize);
        final boolean rotateByTextEnabled = pageRotationOpts != null && pageRotationOpts.enabled;
        final String rotateByTextProcessed = rotateByTextEnabled
                ? preprocess(pageRotationOpts.searchText, ignoreCase, normalize)
                : "";
        int rotateSourcePage = -1;

        log.accept("Apro input: " + inputPath);
        PdfVersion targetVersion = (pdfVersion == null) ? PdfVersion.PDF_1_7 : pdfVersion;
        try (PdfReader reader = new PdfReader(inputPath);
                PdfWriter writer = new PdfWriter(outputPath, new WriterProperties()
                        .setFullCompressionMode(true)
                        .setCompressionLevel(PDF_COMPRESSION_LEVEL)
                        .setPdfVersion(targetVersion))) {
            writer.setSmartMode(smartMode);
            try (PdfDocument src = new PdfDocument(reader);
                    PdfDocument dst = new PdfDocument(writer)) {

            int totalPages = src.getNumberOfPages();
            int totalUnits = Math.max(1, totalPages * 2);
            int completedUnits = 0;
            emitProgress(progress, completedUnits, totalUnits);
            log.accept("Pagine totali: " + totalPages);

            List<Integer> groupStarts = new ArrayList<>();
            List<GroupReport> groupReports = new ArrayList<>();
            boolean evolutionEnabled = postaEvolutionOpts != null && postaEvolutionOpts.enabled;
            boolean correctionOverlayEnabled = correctionOverlayOpts != null && correctionOverlayOpts.enabled;
            String correctionExcel = safe(correctionExcelPath);
            boolean useExcelAddresses = evolutionEnabled && !safe(postaEvolutionOpts.addressExcelPath).isEmpty();
            boolean captureAddress = addressBlockOpts != null && addressBlockOpts.enabled
                    && addressBlockOpts.widthPt > 0f && addressBlockOpts.heightPt > 0f;
            if (correctionOverlayEnabled && correctionExcel.isEmpty()) {
                throw new IllegalArgumentException("Per le correzioni QR e' necessario indicare il file Excel.");
            }
            if (evolutionEnabled && !useExcelAddresses && !captureAddress) {
                throw new IllegalArgumentException(
                        "Per Posta Evolution e' necessario abilitare e configurare il blocco indirizzo oppure indicare un file Excel destinatari.");
            }
            for (int i = 1; i <= totalPages; i++) {
                throwIfCancelled(isCancelled);
                String textPage = PdfTextExtractor.getTextFromPage(src.getPage(i), new SimpleTextExtractionStrategy());
                String processed = preprocess(textPage, ignoreCase, normalize);
                boolean pageContainsMarker = processed.contains(markerProcessed);
                if (pageContainsMarker) {
                    groupStarts.add(i);
                }
                if (rotateByTextEnabled && rotateSourcePage < 0 && processed.contains(rotateByTextProcessed)) {
                    rotateSourcePage = i;
                }
                if (i == 1 && !pageContainsMarker) {
                    throw new IllegalStateException("Il marker non e' stato trovato sul primo foglio.");
                }
                completedUnits++;
                emitProgress(progress, completedUnits, totalUnits);
            }

            if (groupStarts.isEmpty()) {
                throw new IllegalStateException("Il marker non e' stato trovato sul primo foglio.");
            }
            if (rotateByTextEnabled) {
                if (rotateSourcePage > 0) {
                    log.accept("Rotazione pagina attiva: pagina sorgente " + rotateSourcePage
                            + " (+" + pageRotationOpts.clockwiseDegrees + " gradi).");
                } else {
                    log.accept("Rotazione pagina: nessuna pagina trovata con la stringa richiesta.");
                }
            }

            List<AddressComponents> excelAddresses = null;
            if (useExcelAddresses) {
                throwIfCancelled(isCancelled);
                excelAddresses = loadAddressComponentsFromExcel(postaEvolutionOpts.addressExcelPath);
                if (excelAddresses.isEmpty()) {
                    throw new IllegalStateException("Il file Excel destinatari non contiene righe valide.");
                }
                if (excelAddresses.size() < groupStarts.size()) {
                    throw new IllegalStateException("Il file Excel destinatari contiene " + excelAddresses.size()
                            + " righe valide ma i gruppi rilevati nel PDF sono " + groupStarts.size() + ".");
                }
                log.accept("Indirizzi Evolution caricati da Excel: " + excelAddresses.size());
            }

            Map<String, Imbustatrice.CorrectionOverlayEntry> correctionEntries = new HashMap<>();
            if (correctionOverlayEnabled) {
                throwIfCancelled(isCancelled);
                correctionEntries = loadCorrectionOverlayEntriesFromExcel(correctionExcel);
                log.accept("Correzioni QR caricate da Excel: " + correctionEntries.size());
            }

            if (groupStarts.get(0) > 1) {
                throwIfCancelled(isCancelled);
                int prefaceStart = 1;
                int prefaceEnd = groupStarts.get(0) - 1;
                src.copyPagesTo(prefaceStart, prefaceEnd, dst);
                if (rotateByTextEnabled && rotateSourcePage >= prefaceStart && rotateSourcePage <= prefaceEnd) {
                    int dstPage = rotateSourcePage - prefaceStart + 1;
                    rotatePageClockwise(dst.getPage(dstPage), pageRotationOpts.clockwiseDegrees);
                    log.accept("  -> ruotata pagina output " + dstPage + " (sorgente " + rotateSourcePage + ")");
                }
                int prefaceCount = prefaceEnd - prefaceStart + 1;
                if (prefaceCount > 0) {
                    completedUnits += prefaceCount;
                    emitProgress(progress, completedUnits, totalUnits);
                }
            }

            int groupNum = 0;
            for (int g = 0; g < groupStarts.size(); g++) {
                throwIfCancelled(isCancelled);
                final int startPage = groupStarts.get(g);
                final int endPage = (g + 1 < groupStarts.size()) ? groupStarts.get(g + 1) - 1 : totalPages;
                if (startPage > endPage) {
                    continue;
                }

                groupNum++;
                log.accept("Gruppo " + groupNum + ": sorgente " + startPage + "-" + endPage);

                final int beforeDstPages = dst.getNumberOfPages();
                src.copyPagesTo(startPage, endPage, dst);
                if (rotateByTextEnabled && rotateSourcePage >= startPage && rotateSourcePage <= endPage) {
                    int dstPage = beforeDstPages + (rotateSourcePage - startPage + 1);
                    rotatePageClockwise(dst.getPage(dstPage), pageRotationOpts.clockwiseDegrees);
                    log.accept("  -> ruotata pagina output " + dstPage + " (sorgente " + rotateSourcePage + ")");
                }

                int copiedCount = endPage - startPage + 1;
                if (copiedCount > 0) {
                    completedUnits += copiedCount;
                    emitProgress(progress, completedUnits, totalUnits);
                }

                if ((copiedCount % 2) == 1) {
                    PageSize size = PageSize.A4;
                    dst.addNewPage(new PageSize(size));
                    copiedCount++;
                    log.accept("  -> aggiunta pagina bianca (copiedCount=" + copiedCount + ")");
                }

                final int dstStartIndex = beforeDstPages + 1;

                if (barcodeOpts != null) {
                    if (barcodeOpts.enabled) {
                        log.accept("  -> barcode: dstStart=" + dstStartIndex +
                                ", copiedCount=" + copiedCount + ", groupNum=" + groupNum);
                    } else if (barcodeOpts.labelEnabled) {
                        log.accept("  -> etichetta gruppo: dstStart=" + dstStartIndex +
                                ", groupNum=" + groupNum);
                    }
                    Imbustatrice.applyBarcodesToGroup(
                            dst,
                            dstStartIndex,
                            copiedCount,
                            groupNum,
                            barcodeOpts.groupStartProgressive,
                            barcodeOpts);
                }

                boolean renderRaccomandata = raccomandataOpts != null && (raccomandataOpts.enabled
                        || (raccomandataOpts.omologazioneEnabled && !raccomandataOpts.omologazioneText.isEmpty()));
                if (renderRaccomandata) {
                    Imbustatrice.applyRaccomandataBarcodeToGroup(
                            dst,
                            dstStartIndex,
                            copiedCount,
                            groupNum,
                            raccomandataOpts);
                }

                if (qrCodeOpts != null && qrCodeOpts.enabled) {
                    Imbustatrice.applyQrCodeToGroup(
                            dst,
                            dstStartIndex,
                            copiedCount,
                            groupNum,
                            qrCodeOpts);
                }

                String groupLabelId = barcodeOpts != null ? barcodeOpts.lavorazioneId : "";
                int groupCounterNumber = groupNum;
                if (barcodeOpts != null) {
                    groupCounterNumber = Math.max(1, barcodeOpts.groupStartProgressive) + (groupNum - 1);
                }

                if (correctionOverlayEnabled) {
                    String correctionKey = Long.toString(groupCounterNumber);
                    Imbustatrice.CorrectionOverlayEntry correctionEntry = correctionEntries.get(correctionKey);
                    if (correctionEntry != null) {
                        Imbustatrice.applyCorrectionOverlayToGroup(
                                dst,
                                dstStartIndex,
                                copiedCount,
                                correctionEntry,
                                correctionOverlayOpts);
                    }
                }

                Imbustatrice.drawGroupPageCounters(
                        dst,
                        dstStartIndex,
                        copiedCount,
                        pageCounterOpts,
                        groupCounterNumber,
                        groupLabelId);
                AddressComponents address = useExcelAddresses
                        ? excelAddresses.get(groupNum - 1)
                        : (captureAddress ? extractAddressBlock(src, startPage, addressBlockOpts) : null);
                throwIfCancelled(isCancelled);
                String dataMatrixValue = "";
                if (evolutionEnabled) {
                    dataMatrixValue = buildPostaEvolutionDataMatrixValue(postaEvolutionOpts, groupNum, address);
                    if (log != null) {
                        log.accept("  -> datamatrix gruppo " + groupNum + ": payload=" + dataMatrixValue
                                + ", cap=" + safe(address == null ? "" : address.cap)
                                + ", sorgente=" + (useExcelAddresses ? "excel" : "pdf"));
                    }
                    try {
                        Imbustatrice.applyPostaEvolutionDataMatrixToGroup(
                                dst,
                                dstStartIndex,
                                copiedCount,
                                dataMatrixValue,
                                postaEvolutionOpts.dataMatrixOpts);
                    } catch (Exception ex) {
                        throw new IllegalStateException(
                                "Errore DataMatrix gruppo " + groupNum + " (payload=" + dataMatrixValue + ")", ex);
                    }
                }
                String qrValue = buildQrValue(qrCodeOpts, groupNum);
                RaccomandataInfo racCode = buildRaccomandataCodeValue(raccomandataOpts, groupNum);
                groupReports.add(
                        new GroupReport(groupNum, startPage, endPage, copiedCount, address, qrValue, racCode,
                                dataMatrixValue));
            }

            throwIfCancelled(isCancelled);
            exportGroupReport(outputPath, groupReports, log, marker);
            throwIfCancelled(isCancelled);
            exportEvolutionDuReport(outputPath, groupReports, postaEvolutionOpts, log);
            log.accept("Completato. Output: " + outputPath);
                emitProgress(progress, totalUnits, totalUnits);
            }
        }
    }

    // Overload compatibilita': senza PDF version esplicita
    public static void process(
            String inputPath,
            String outputPath,
            String marker,
            boolean ignoreCase,
            boolean normalize,
            Consumer<String> logger,
            BarcodeOpts barcodeOpts,
            PageCounterOpts pageCounterOpts,
            RaccomandataBarcodeOpts raccomandataOpts,
            QrCodeOpts qrCodeOpts,
            PostaEvolutionOpts postaEvolutionOpts,
            PageRotationByTextOpts pageRotationOpts,
            AddressBlockOpts addressBlockOpts,
            IntConsumer progressListener) throws Exception {
        process(inputPath, outputPath, marker, ignoreCase, normalize, null, logger, barcodeOpts, pageCounterOpts,
                raccomandataOpts, qrCodeOpts, postaEvolutionOpts, null, null, pageRotationOpts, addressBlockOpts,
                progressListener);
    }

    // Overload compatibilita': senza barcode
    public static void process(
            String inputPath,
            String outputPath,
            String marker,
            boolean ignoreCase,
            boolean normalize,
            Consumer<String> logger,
            IntConsumer progressListener) throws Exception {
        process(inputPath, outputPath, marker, ignoreCase, normalize, null, logger, null, null, null, null, null,
                null, null, null, null, progressListener);
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

    private static void rotatePageClockwise(PdfPage page, int clockwiseDegrees) {
        if (page == null) {
            return;
        }
        int currentRotation = page.getRotation();
        int normalizedCurrent = ((currentRotation % 360) + 360) % 360;
        int nextRotation = (normalizedCurrent + clockwiseDegrees) % 360;
        page.setRotation(nextRotation);
    }

    private static String preprocess(String s, boolean ignoreCase, boolean normalize) {
        if (s == null)
            return "";
        String out = s;
        if (normalize && mayContainDiacritics(out)) {
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

    private static void emitProgress(IntConsumer progress, int completedUnits, int totalUnits) {
        if (totalUnits <= 0) {
            progress.accept(100);
            return;
        }
        double ratio = Math.min(Math.max(completedUnits, 0), totalUnits) / (double) totalUnits;
        int percent = (int) Math.round(ratio * 100.0);
        percent = Math.max(0, Math.min(100, percent));
        progress.accept(percent);
    }

    private static void throwIfCancelled(BooleanSupplier cancellationRequested) {
        if (cancellationRequested != null && cancellationRequested.getAsBoolean()) {
            throw new CancellationException("Operazione interrotta dall'utente.");
        }
    }

    /**
     * API indipendente per leggere il blocco indirizzo da un PDF senza dover
     * eseguire l'intero processo di normalizzazione.
     *
     * @param inputPath  percorso del PDF sorgente
     * @param pageNumber numero di pagina 1-based (default 1 se <= 0)
     * @param opts       coordinate/dimensioni del blocco indirizzo
     * @return componenti dell'indirizzo estratto oppure valori vuoti se non
     *         disponibili
     * @throws Exception in caso di errori di lettura del PDF
     */
    public static AddressComponents readAddressBlock(String inputPath, int pageNumber, AddressBlockOpts opts)
            throws Exception {
        if (inputPath == null || inputPath.isBlank()) {
            return AddressComponents.empty();
        }
        int safePage = pageNumber <= 0 ? 1 : pageNumber;
        try (PdfReader reader = new PdfReader(inputPath); PdfDocument document = new PdfDocument(reader)) {
            return extractAddressBlock(document, safePage, opts);
        }
    }

    /**
     * Variante che legge sempre dalla prima pagina del PDF.
     */
    public static AddressComponents readAddressBlock(String inputPath, AddressBlockOpts opts) throws Exception {
        return readAddressBlock(inputPath, 1, opts);
    }

    public static KeyedStringReadResult exportKeyedStringsToExcel(String inputPath, String key) throws Exception {
        Path pdfPath = Paths.get(safe(inputPath));
        Path parent = pdfPath.getParent();
        Path excelPath = parent == null ? Paths.get("lettura.xlsx") : parent.resolve("lettura.xlsx");
        return exportKeyedStringsToExcel(inputPath, key, excelPath);
    }

    public static KeyedStringReadResult exportKeyedStringsToExcel(String inputPath, String key, Path excelPath)
            throws Exception {
        if (inputPath == null || inputPath.isBlank()) {
            throw new IllegalArgumentException("Il PDF di input non puo' essere vuoto.");
        }
        String safeKey = safe(key);
        if (safeKey.isEmpty()) {
            throw new IllegalArgumentException("La stringa chiave non puo' essere vuota.");
        }
        Path safeExcelPath = excelPath == null ? Paths.get("lettura.xlsx") : excelPath;
        List<List<String>> rows = new ArrayList<>();

        try (PdfReader reader = new PdfReader(inputPath); PdfDocument document = new PdfDocument(reader)) {
            int totalPages = document.getNumberOfPages();
            for (int pageNumber = 1; pageNumber <= totalPages; pageNumber++) {
                String text = PdfTextExtractor.getTextFromPage(
                        document.getPage(pageNumber),
                        new SimpleTextExtractionStrategy());
                collectKeyedRows(text, safeKey, rows);
            }
        }

        writeKeyedRowsExcel(safeExcelPath, rows);
        return new KeyedStringReadResult(safeExcelPath, rows.size());
    }

    private static void collectKeyedRows(String text, String key, List<List<String>> rows) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n");
        for (String line : lines) {
            int index = line.indexOf(key);
            if (index < 0) {
                continue;
            }
            String value = line.substring(index + key.length()).trim();
            if (value.isEmpty()) {
                continue;
            }
            String[] parts = value.split("\\|", -1);
            List<String> columns = new ArrayList<>(parts.length);
            for (String part : parts) {
                columns.add(safe(part));
            }
            rows.add(columns);
        }
    }

    private static void writeKeyedRowsExcel(Path excelPath, List<List<String>> rows) throws Exception {
        Path parent = excelPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("lettura");
            int maxColumns = 0;
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex);
                List<String> columns = rows.get(rowIndex);
                maxColumns = Math.max(maxColumns, columns.size());
                for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                    row.createCell(columnIndex).setCellValue(columns.get(columnIndex));
                }
            }
            for (int columnIndex = 0; columnIndex < maxColumns; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
            }
            try (OutputStream out = Files.newOutputStream(excelPath)) {
                workbook.write(out);
            }
        }
    }

    private static AddressComponents extractAddressBlock(PdfDocument src, int pageNumber, AddressBlockOpts opts) {
        if (src == null || opts == null || !opts.enabled) {
            return AddressComponents.empty();
        }
        if (pageNumber < 1 || pageNumber > src.getNumberOfPages()) {
            return AddressComponents.empty();
        }
        if (opts.widthPt <= 0f || opts.heightPt <= 0f) {
            return AddressComponents.empty();
        }
        Rectangle region = new Rectangle(opts.posXpt, opts.posYpt, opts.widthPt, opts.heightPt);
        TextRegionEventFilter filter = new TextRegionEventFilter(region);
        FilteredTextEventListener listener = new FilteredTextEventListener(new LocationTextExtractionStrategy(),
                filter);
        try {
            String text = PdfTextExtractor.getTextFromPage(src.getPage(pageNumber), listener);
            if (text == null) {
                return AddressComponents.empty();
            }
            return parseAddressComponents(text);
        } catch (Exception ex) {
            return AddressComponents.empty();
        }
    }

    private static String buildQrValue(Imbustatrice.QrCodeOpts opts, int groupNumber) {
        if (opts == null || !opts.enabled) {
            return "";
        }
        try {
            long value = opts.startValue + (groupNumber - 1L);
            if (value < 0 || value > opts.maxProgressiveValue) {
                return "";
            }
            String progressive = String.format(Locale.US, "%0" + opts.digits + "d", value);
            return opts.baseText + progressive;
        } catch (Exception ex) {
            return "";
        }
    }

    private static RaccomandataInfo buildRaccomandataCodeValue(Imbustatrice.RaccomandataBarcodeOpts opts,
            int groupNumber) {
        if (opts == null || !opts.enabled) {
            return RaccomandataInfo.empty();
        }
        try {
            long identifierValue = opts.startValue + (groupNumber - 1L);
            if (identifierValue < 0 || identifierValue > opts.maxIdentifierValue) {
                return RaccomandataInfo.empty();
            }
            String identifier = String.format(Locale.US, "%0" + opts.identifierDigits + "d", identifierValue);
            int checkDigit = computeRaccomandataCheckDigit(identifier);
            String formatted = identifier + "-" + checkDigit;
            String digitsOnly = identifier + checkDigit;
            return new RaccomandataInfo(formatted, digitsOnly);
        } catch (Exception ex) {
            return RaccomandataInfo.empty();
        }
    }

    private static int computeRaccomandataCheckDigit(String identifierDigits) {
        if (identifierDigits == null || identifierDigits.isEmpty()) {
            return 0;
        }
        int sumEven = 0;
        int sumOdd = 0;
        for (int i = 0; i < identifierDigits.length(); i++) {
            int digit = identifierDigits.charAt(i) - '0';
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

    public static String buildPostaEvolutionDataMatrixValue(PostaEvolutionOpts opts, int groupNumber,
            AddressComponents address) {
        if (opts == null || !opts.enabled) {
            return "";
        }
        String capDest = normalizeNumeric(
                safe(address == null ? "" : address.cap).isEmpty() ? opts.capDestinatarioFallback : address.cap,
                5, '0');
        String objectId = normalizeNumeric(
                Long.toString(opts.identificativoOggettoStart + Math.max(0, groupNumber - 1)),
                6, '0');
        String servizi = normalizeAlphaNum(safe(opts.serviziAccessori).isEmpty() ? "OOOOOOOO" : opts.serviziAccessori,
                8,
                'O');

        StringBuilder sb = new StringBuilder(72);
        sb.append(normalizeAlphaNum("1", 1, '1'));
        sb.append(normalizeAlphaNum(opts.gamma, 1, 'B'));
        sb.append(normalizeNumeric(opts.idClienteSap, 8, '0'));
        sb.append(normalizeAlphaNum(opts.idClienteMittente, 3, 'X'));
        sb.append(normalizeAlphaNum(opts.classe, 1, '1'));
        sb.append(normalizeAlphaNum(opts.tipoProdotto, 1, 'W'));
        sb.append(capDest);
        sb.append(normalizeAlphaNum(opts.codiceTecnicoDestinatario, 4, ' '));
        sb.append(normalizeNumeric(opts.capMittente, 5, '0'));
        sb.append(normalizeAlphaNum(opts.codiceTecnicoMittente, 4, ' '));
        sb.append(normalizeAlphaNum(opts.codicePrenotazioneFiglio, 5, 'Z'));
        sb.append(normalizeAlphaNum(opts.identificativoStampatore, 2, 'X'));
        sb.append(objectId);
        sb.append(normalizeAlphaNum(opts.causale, 3, '0'));
        sb.append(normalizeDataMatrixOmologazione(opts.codiceOmologazioneDatamatrix));
        sb.append(normalizeAlphaNum(opts.disponibileCliente, 9, ' '));
        sb.append(servizi);
        return sb.toString();
    }

    private static void exportEvolutionDuReport(
            String outputPath,
            List<GroupReport> groupReports,
            PostaEvolutionOpts opts,
            Consumer<String> log) throws Exception {
        if (opts == null || !opts.enabled || !opts.duEnabled || groupReports == null || groupReports.isEmpty()) {
            return;
        }

        String sap8 = normalizeNumeric(opts.idClienteSap, 8, '0');
        String idPrenotazione = normalizeNumeric(opts.duIdPrenotazione, 7, '0');
        String tipoAccettazioneFile = normalizeAlphaNum(opts.duTipoAccettazioneFile, 1, 'G');
        String progressivo = normalizeNumeric(opts.duProgressivo, 2, '0');
        String siglaIds = normalizeAlphaNum(opts.duSiglaIds, 12, ' ').trim();
        String fileName = "NPSO_" + tipoAccettazioneFile + "_DU_ID" + sap8 + "_" + idPrenotazione + "_" + progressivo
                + "_" + siglaIds + DU_SUFFIX;

        Path pdfPath = Paths.get(outputPath);
        Path duPath = pdfPath.resolveSibling(fileName);

        List<String> lines = new ArrayList<>();
        String headerLine = String.join("|", Arrays.asList(
                "00" + sap8,
                safe(opts.duUtenzaOperatore),
                idPrenotazione,
                safe(opts.duDataRicezioneFlusso),
                safe(opts.duDataFineStampa),
                safeDate(opts.duDataPostalizzazione),
                opts.duFrazionarioCentro,
                opts.duTipologiaProdotto,
                opts.duCodiceProdotto,
                safe(opts.duCodiceServizioAccessorio),
                Integer.toString(groupReports.size()),
                safe(opts.duCodiceTipologiaAccettazione),
                safe(opts.duTipologiaCodiceTracciatura),
                safe(opts.duCodiceContoContrattuale),
                safe(opts.duDescrizionePostalizzazione)));
        lines.add(headerLine);

        for (GroupReport report : groupReports) {
            AddressComponents address = report.address == null ? AddressComponents.empty() : report.address;
            String[] streetAndCivic = splitStreetAndCivic(address.indirizzo);
            List<String> body = new ArrayList<>();
            body.add(safe(report.dataMatrixValue));
            body.add(safe(address.nominativo));
            body.add(streetAndCivic[0]);
            body.add(streetAndCivic[1]);
            body.add(normalizeNumeric(address.cap, 5, '0'));
            body.add(safe(address.comune));
            body.add(safe(address.provincia));
            body.add("");
            body.add("");
            body.add("");
            body.add("");
            body.add("");
            body.add("");
            body.add("");
            body.add("");
            body.add("");
            body.add("");
            body.add("");
            body.add("");
            body.add("");
            body.add("");
            body.add("");
            body.add(safe(opts.duCodiceOmologazione));
            body.add(normalizeAlphaNum(opts.idClienteMittente, 3, 'X'));
            body.add(safe(opts.duFormato));
            body.add(safe(opts.duIdHu));
            body.add(safe(opts.duIdScatola));
            body.add(safe(opts.duIdUtenzaCliente));
            body.add(safe(opts.duNumeroFattura));
            body.add(safe(opts.duDataScadenzaFattura));
            body.add(safe(opts.duSpare1));
            body.add(safe(opts.duSpare2));
            body.add(safe(opts.duSpare3));
            body.add(safe(opts.duSpare4));
            body.add(safe(opts.duSpare5));
            body.add(safe(opts.duSpare6));
            body.add(safe(opts.duSpare7));
            body.add(safe(opts.duSpare8));
            body.add(safe(opts.duSpare9));
            body.add(safe(opts.duSpare10));
            lines.add(String.join("|", body));
        }

        String text = String.join("\r\n", lines) + "\r\n";
        Files.writeString(duPath, text, StandardCharsets.UTF_8);
        if (log != null) {
            log.accept("Creato report DU: " + duPath);
        }
    }

    private static void exportGroupReport(String outputPath, List<GroupReport> groupReports, Consumer<String> log,
            String marker)
            throws Exception {
        if (groupReports == null || groupReports.isEmpty()) {
            return;
        }

        Path pdfPath = Paths.get(outputPath);
        Path excelPath = pdfPath.resolveSibling(REPORT_FILE_NAME);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(REPORT_SHEET_NAME);

            int rowIndex = 0;
            Row header = sheet.createRow(rowIndex++);
            header.createCell(0).setCellValue("n. gruppo");
            header.createCell(1).setCellValue("pagina da");
            header.createCell(2).setCellValue("pagina a");
            header.createCell(3).setCellValue("numero di pagine");
            header.createCell(4).setCellValue("rawText");
            header.createCell(5).setCellValue("nominativo");
            header.createCell(6).setCellValue("indirizzo");
            header.createCell(7).setCellValue("cap");
            header.createCell(8).setCellValue("comune");
            header.createCell(9).setCellValue("provincia");
            header.createCell(10).setCellValue("qr");
            header.createCell(11).setCellValue("barcode raccomandata");
            header.createCell(12).setCellValue("barcode raccomandata (senza trattino)");

            for (GroupReport report : groupReports) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(report.groupNumber);
                row.createCell(1).setCellValue(report.pageFrom);
                row.createCell(2).setCellValue(report.pageTo);
                row.createCell(3).setCellValue(report.pageCount);

                AddressComponents address = report.address;
                String nominativo = (address == null) ? "" : nullToEmpty(address.nominativo);
                String indirizzo = (address == null) ? "" : nullToEmpty(address.indirizzo);
                String cap = (address == null) ? "" : nullToEmpty(address.cap);
                String comune = (address == null) ? "" : nullToEmpty(address.comune);
                String provincia = (address == null) ? "" : nullToEmpty(address.provincia);

                String rawText = (address == null) ? "" : nullToEmpty(address.rawText.replace(marker.trim(), ""));
                row.createCell(4).setCellValue(rawText);
                row.createCell(5).setCellValue(nominativo);
                row.createCell(6).setCellValue(indirizzo);
                row.createCell(7).setCellValue(cap);
                row.createCell(8).setCellValue(comune);
                row.createCell(9).setCellValue(provincia);
                row.createCell(10).setCellValue(nullToEmpty(report.qrValue));
                String racFormatted = report.raccomandata == null ? "" : nullToEmpty(report.raccomandata.formatted);
                String racDigits = report.raccomandata == null ? "" : nullToEmpty(report.raccomandata.digitsOnly);
                row.createCell(11).setCellValue(racFormatted);
                row.createCell(12).setCellValue(racDigits);
            }

            for (int col = 0; col <= 11; col++) {
                sheet.autoSizeColumn(col);
            }

            try (OutputStream out = Files.newOutputStream(excelPath)) {
                workbook.write(out);
            }
        }

        if (log != null) {
            log.accept("Creato report Excel: " + excelPath);
        }
    }

    private static AddressComponents parseAddressComponents(String rawText) {
        if (rawText == null) {
            return AddressComponents.empty();
        }
        String normalized = rawText.replace('\r', '\n').replace("(", "").replace(")", "").replace(" - ", " ").trim();
        if (normalized.isEmpty()) {
            return AddressComponents.empty();
        }
        String[] split = normalized.split("\\n");
        List<String> lines = new ArrayList<>();
        for (String line : split) {
            String trimmed = line == null ? "" : line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        String nominativo = lines.isEmpty() ? "" : lines.get(0);
        String indirizzo = lines.size() > 1 ? lines.get(1) : "";
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < lines.size(); i++) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(lines.get(i));
        }
        String remaining = sb.toString().trim();
        String cap = "";
        String comune = "";
        String provincia = "";

        String searchArea = remaining.isEmpty() ? indirizzo : remaining;
        String sanitized = searchArea.replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (!sanitized.isEmpty()) {
            Matcher capMatcher = Pattern.compile("(\\d{5})").matcher(sanitized);
            if (capMatcher.find()) {
                cap = capMatcher.group(1);
                String afterCap = sanitized.substring(capMatcher.end()).trim();
                if (!afterCap.isEmpty()) {
                    String[] tokens = afterCap.split("\\s+");
                    if (tokens.length > 0) {
                        String lastToken = tokens[tokens.length - 1];
                        if (lastToken.matches("[A-Z]{2}")) {
                            provincia = lastToken;
                            StringBuilder comuneBuilder = new StringBuilder();
                            for (int i = 0; i < tokens.length - 1; i++) {
                                if (comuneBuilder.length() > 0) {
                                    comuneBuilder.append(' ');
                                }
                                comuneBuilder.append(tokens[i]);
                            }
                            comune = comuneBuilder.toString().trim();
                        } else {
                            comune = afterCap;
                        }
                    }
                }
            }
        }
        return new AddressComponents(normalized, nominativo, indirizzo, cap, comune, provincia);
    }

    private static List<AddressComponents> loadAddressComponentsFromExcel(String excelPath) throws Exception {
        Path path = Paths.get(safe(excelPath));
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File Excel destinatari non trovato: " + path);
        }

        List<AddressComponents> addresses = new ArrayList<>();
        try (InputStream in = Files.newInputStream(path);
                Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return addresses;
            }

            DataFormatter formatter = new DataFormatter(Locale.ITALIAN);
            int headerRowIndex = sheet.getFirstRowNum();
            Row firstRow = sheet.getRow(headerRowIndex);
            ColumnIndexes columns = ColumnIndexes.fromRow(firstRow, formatter);
            boolean hasHeader = columns.hasAnyMappedColumn();
            int startRow = hasHeader ? headerRowIndex + 1 : headerRowIndex;

            for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                String nominativo = readExcelValue(row, formatter, columns.nominativoIndex, 0);
                String indirizzo = readExcelValue(row, formatter, columns.indirizzoIndex, 1);
                String cap = readExcelValue(row, formatter, columns.capIndex, 2);
                String comune = readExcelValue(row, formatter, columns.comuneIndex, 3);
                String provincia = readExcelValue(row, formatter, columns.provinciaIndex, 4);

                if (nominativo.isEmpty() && indirizzo.isEmpty() && cap.isEmpty() && comune.isEmpty()
                        && provincia.isEmpty()) {
                    continue;
                }

                String rawText = String.join("\n", Arrays.asList(nominativo, indirizzo,
                        String.join(" ", Arrays.asList(cap, comune, provincia)).trim())).trim();
                addresses.add(new AddressComponents(rawText, nominativo, indirizzo, cap, comune, provincia));
            }
        }
        return addresses;
    }

    private static Map<String, Imbustatrice.CorrectionOverlayEntry> loadCorrectionOverlayEntriesFromExcel(
            String excelPath) throws Exception {
        Path path = Paths.get(safe(excelPath));
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File Excel correzioni QR non trovato: " + path);
        }

        Map<String, Imbustatrice.CorrectionOverlayEntry> entries = new HashMap<>();
        try (InputStream in = Files.newInputStream(path);
                Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return entries;
            }

            DataFormatter formatter = new DataFormatter(Locale.ITALIAN);
            int headerRowIndex = sheet.getFirstRowNum();
            Row firstRow = sheet.getRow(headerRowIndex);
            CorrectionColumnIndexes columns = CorrectionColumnIndexes.fromRow(firstRow, formatter);
            boolean hasHeader = columns.hasAnyMappedColumn();
            int startRow = hasHeader ? headerRowIndex + 1 : headerRowIndex;

            for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                String progressive = readExcelValue(row, formatter, columns.progressivoIndex, 0);
                String progressiveKey = normalizeProgressiveKey(progressive);
                if (progressiveKey.isEmpty()) {
                    continue;
                }

                String via = readExcelValue(row, formatter, columns.viaIndex, 1);
                String contaNote = readExcelValue(row, formatter, columns.contaNoteIndex, 1);
                String civico = readExcelValue(row, formatter, columns.civicoIndex, 3);
                String subcivico = readExcelValue(row, formatter, columns.subcivicoIndex, 4);
                String altro = readExcelValue(row, formatter, columns.altroIndex, 5);
                String check = readExcelValue(row, formatter, columns.checkIndex, 6);

                Imbustatrice.CorrectionOverlayEntry entry = buildCorrectionOverlayEntry(
                        via, civico, subcivico, altro, check, contaNote);
                if (entry.kind != Imbustatrice.CorrectionOverlayKind.NONE) {
                    entries.put(progressiveKey, entry);
                }
            }
        }
        return entries;
    }

    private static Imbustatrice.CorrectionOverlayEntry buildCorrectionOverlayEntry(
            String via, String civico, String subcivico, String altro, String check, String contaNote) {
        boolean hasXIcon = "NOPE".equalsIgnoreCase(safe(check));
        if (safe(check).contains("?")) {
            return new Imbustatrice.CorrectionOverlayEntry(
                    Imbustatrice.CorrectionOverlayKind.QUESTION,
                    "",
                    false,
                    isCountGreaterThanOne(contaNote),
                    hasXIcon);
        }
        boolean hasAltro = !safe(altro).isEmpty();
        boolean hasMultipleNotes = isCountGreaterThanOne(contaNote);
        if (safe(check).contains("=")) {
            List<String> equalsParts = new ArrayList<>();
            if (!safe(civico).isEmpty()) {
                equalsParts.add(safe(civico));
            }
            if (!safe(subcivico).isEmpty()) {
                equalsParts.add(safe(subcivico));
            }
            if (!safe(check).isEmpty()) {
                equalsParts.add(safe(check));
            }
            if (equalsParts.isEmpty()) {
                return (hasAltro || hasMultipleNotes)
                        ? new Imbustatrice.CorrectionOverlayEntry(
                                Imbustatrice.CorrectionOverlayKind.SEARCH,
                                "",
                                false,
                                hasMultipleNotes,
                                hasXIcon)
                        : Imbustatrice.CorrectionOverlayEntry.none();
            }
            return new Imbustatrice.CorrectionOverlayEntry(
                    Imbustatrice.CorrectionOverlayKind.TEXT,
                    String.join(" ", equalsParts),
                    hasAltro,
                    hasMultipleNotes,
                    hasXIcon);
        }
        List<String> parts = new ArrayList<>();
        if (!safe(via).isEmpty()) {
            parts.add(safe(via));
        }
        if (!safe(civico).isEmpty()) {
            parts.add(safe(civico));
        }
        if (!safe(subcivico).isEmpty()) {
            parts.add(safe(subcivico));
        }
        if (parts.isEmpty()) {
            return (hasAltro || hasMultipleNotes)
                    ? new Imbustatrice.CorrectionOverlayEntry(
                            Imbustatrice.CorrectionOverlayKind.SEARCH,
                            "",
                            false,
                            hasMultipleNotes,
                            hasXIcon)
                    : (hasXIcon
                            ? new Imbustatrice.CorrectionOverlayEntry(
                                    Imbustatrice.CorrectionOverlayKind.TEXT,
                                    "",
                                    false,
                                    false,
                                    true)
                            : Imbustatrice.CorrectionOverlayEntry.none());
        }
        return new Imbustatrice.CorrectionOverlayEntry(
                Imbustatrice.CorrectionOverlayKind.TEXT,
                String.join(" ", parts),
                hasAltro,
                hasMultipleNotes,
                hasXIcon);
    }

    private static boolean isCountGreaterThanOne(String value) {
        String text = safe(value).replace(',', '.');
        if (text.isEmpty()) {
            return false;
        }
        try {
            return Double.parseDouble(text) > 1d;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static String normalizeProgressiveKey(String value) {
        String text = safe(value);
        if (text.isEmpty()) {
            return "";
        }
        text = text.replace(',', '.');
        if (text.matches("\\d+\\.0+")) {
            text = text.substring(0, text.indexOf('.'));
        }
        if (text.matches("\\d+")) {
            text = text.replaceFirst("^0+(?!$)", "");
        }
        return text;
    }

    private static String readExcelValue(Row row, DataFormatter formatter, int mappedIndex, int fallbackIndex) {
        int columnIndex = mappedIndex >= 0 ? mappedIndex : fallbackIndex;
        if (columnIndex < 0) {
            return "";
        }
        return safe(formatter.formatCellValue(row.getCell(columnIndex)));
    }

    private static final class ColumnIndexes {
        final int nominativoIndex;
        final int indirizzoIndex;
        final int capIndex;
        final int comuneIndex;
        final int provinciaIndex;

        ColumnIndexes(int nominativoIndex, int indirizzoIndex, int capIndex, int comuneIndex, int provinciaIndex) {
            this.nominativoIndex = nominativoIndex;
            this.indirizzoIndex = indirizzoIndex;
            this.capIndex = capIndex;
            this.comuneIndex = comuneIndex;
            this.provinciaIndex = provinciaIndex;
        }

        boolean hasAnyMappedColumn() {
            return nominativoIndex >= 0 || indirizzoIndex >= 0 || capIndex >= 0 || comuneIndex >= 0
                    || provinciaIndex >= 0;
        }

        static ColumnIndexes fromRow(Row row, DataFormatter formatter) {
            if (row == null) {
                return new ColumnIndexes(-1, -1, -1, -1, -1);
            }
            int nominativo = -1;
            int indirizzo = -1;
            int cap = -1;
            int comune = -1;
            int provincia = -1;
            short firstCell = row.getFirstCellNum();
            short lastCell = row.getLastCellNum();
            if (firstCell < 0 || lastCell < 0) {
                return new ColumnIndexes(-1, -1, -1, -1, -1);
            }
            for (int cellIndex = firstCell; cellIndex < lastCell; cellIndex++) {
                String normalized = normalizeHeader(formatter.formatCellValue(row.getCell(cellIndex)));
                if (nominativo < 0 && matchesHeader(normalized, "destinatario", "nominativo", "ragionesociale",
                        "ragione_sociale", "nome")) {
                    nominativo = cellIndex;
                } else if (indirizzo < 0 && matchesHeader(normalized, "indirizzo", "via", "street")) {
                    indirizzo = cellIndex;
                } else if (cap < 0 && matchesHeader(normalized, "cap", "zipcode", "postalcode")) {
                    cap = cellIndex;
                } else if (comune < 0 && matchesHeader(normalized, "citta", "citta_", "comune", "city")) {
                    comune = cellIndex;
                } else if (provincia < 0 && matchesHeader(normalized, "provincia", "prov", "pr")) {
                    provincia = cellIndex;
                }
            }
            return new ColumnIndexes(nominativo, indirizzo, cap, comune, provincia);
        }
    }

    private static final class CorrectionColumnIndexes {
        final int progressivoIndex;
        final int contaNoteIndex;
        final int viaIndex;
        final int civicoIndex;
        final int subcivicoIndex;
        final int altroIndex;
        final int checkIndex;

        CorrectionColumnIndexes(int progressivoIndex, int contaNoteIndex, int viaIndex, int civicoIndex, int subcivicoIndex,
                int altroIndex, int checkIndex) {
            this.progressivoIndex = progressivoIndex;
            this.contaNoteIndex = contaNoteIndex;
            this.viaIndex = viaIndex;
            this.civicoIndex = civicoIndex;
            this.subcivicoIndex = subcivicoIndex;
            this.altroIndex = altroIndex;
            this.checkIndex = checkIndex;
        }

        boolean hasAnyMappedColumn() {
            return progressivoIndex >= 0 || contaNoteIndex >= 0 || viaIndex >= 0 || civicoIndex >= 0 || subcivicoIndex >= 0
                    || altroIndex >= 0 || checkIndex >= 0;
        }

        static CorrectionColumnIndexes fromRow(Row row, DataFormatter formatter) {
            if (row == null) {
                return new CorrectionColumnIndexes(-1, -1, -1, -1, -1, -1, -1);
            }
            int progressivo = -1;
            int contaNote = -1;
            int via = -1;
            int civico = -1;
            int subcivico = -1;
            int altro = -1;
            int check = -1;
            short firstCell = row.getFirstCellNum();
            short lastCell = row.getLastCellNum();
            if (firstCell < 0 || lastCell < 0) {
                return new CorrectionColumnIndexes(-1, -1, -1, -1, -1, -1, -1);
            }
            for (int cellIndex = firstCell; cellIndex < lastCell; cellIndex++) {
                String normalized = normalizeHeader(formatter.formatCellValue(row.getCell(cellIndex)));
                if (progressivo < 0 && matchesHeader(normalized, "progressivo")) {
                    progressivo = cellIndex;
                } else if (contaNote < 0 && matchesHeader(normalized, "contanote")) {
                    contaNote = cellIndex;
                } else if (via < 0 && matchesHeader(normalized, "correzionivianormalizzata")) {
                    via = cellIndex;
                } else if (civico < 0 && matchesHeader(normalized, "correzionicivico")) {
                    civico = cellIndex;
                } else if (subcivico < 0 && matchesHeader(normalized, "correzionisubcivico")) {
                    subcivico = cellIndex;
                } else if (altro < 0 && matchesHeader(normalized, "correzionialtro")) {
                    altro = cellIndex;
                } else if (check < 0 && matchesHeader(normalized, "check")) {
                    check = cellIndex;
                }
            }
            return new CorrectionColumnIndexes(progressivo, contaNote, via, civico, subcivico, altro, check);
        }
    }

    private static String normalizeHeader(String value) {
        return safe(value)
                .toLowerCase(Locale.ITALIAN)
                .replace('à', 'a')
                .replace('è', 'e')
                .replace('é', 'e')
                .replace('ì', 'i')
                .replace('ò', 'o')
                .replace('ù', 'u')
                .replaceAll("[^a-z0-9]+", "");
    }

    private static boolean matchesHeader(String normalizedHeader, String... aliases) {
        if (normalizedHeader.isEmpty()) {
            return false;
        }
        for (String alias : aliases) {
            if (normalizedHeader.equals(alias)) {
                return true;
            }
        }
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeDate(String value) {
        String trimmed = safe(value);
        if (trimmed.isEmpty()) {
            return ISO_DATE.format(LocalDate.now());
        }
        return trimmed;
    }

    private static String normalizeAlphaNum(String value, int len, char pad) {
        String input = safe(value).toUpperCase(Locale.ITALIAN).replaceAll("[^A-Z0-9]", "");
        if (input.length() > len) {
            input = input.substring(0, len);
        }
        StringBuilder sb = new StringBuilder(input);
        while (sb.length() < len) {
            sb.append(pad);
        }
        return sb.toString();
    }

    private static String normalizeNumeric(String value, int len, char pad) {
        String input = safe(value).replaceAll("[^0-9]", "");
        if (input.length() > len) {
            input = input.substring(input.length() - len);
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() + input.length() < len) {
            sb.append(pad);
        }
        sb.append(input);
        return sb.toString();
    }

    private static String normalizeDataMatrixOmologazione(String value) {
        String input = safe(value).toUpperCase(Locale.ITALIAN).replaceAll("[^A-Z0-9]", "");
        if (input.startsWith("DCO") && input.length() > 3) {
            input = input.substring(3);
        }
        if (input.length() > 6) {
            input = input.substring(0, 6);
        }
        StringBuilder sb = new StringBuilder(input);
        while (sb.length() < 6) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String[] splitStreetAndCivic(String streetLine) {
        String street = safe(streetLine);
        if (street.isEmpty()) {
            return new String[] { "", "" };
        }
        Matcher matcher = Pattern.compile("^(.*?)(?:\\s+(\\d+[A-Z0-9/\\-]*))?$").matcher(street);
        if (!matcher.matches()) {
            return new String[] { street, "" };
        }
        String line = safe(matcher.group(1));
        String civic = safe(matcher.group(2));
        return new String[] { line, civic };
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
