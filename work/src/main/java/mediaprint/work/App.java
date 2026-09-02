package mediaprint.work;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import com.itextpdf.kernel.pdf.PdfVersion;

import mediaprint.imbustatrice.Imbustatrice;
import mediaprint.normalizza.PdfDuplexGrouper;
import mediaprint.normalizza.RisoOptimizer;
import mediaprint.work.config.tabs.AddressBlockTabDefaults;
import mediaprint.work.config.tabs.BarcodeImbustatriceTabDefaults;
import mediaprint.work.config.tabs.BarcodeRaccomandataTabDefaults;
import mediaprint.work.config.tabs.CounterTabDefaults;
import mediaprint.work.config.tabs.EvolutionSwitch;
import mediaprint.work.config.tabs.GeneralTabDefaults;
import mediaprint.work.config.tabs.LogTabDefaults;
import mediaprint.work.config.tabs.OmologazionePostaleTabDefaults;
import mediaprint.work.config.tabs.PostaEvolutionTabDefaults;
import mediaprint.work.config.tabs.QrCodeTabDefaults;
import mediaprint.work.config.tabs.RisoTabDefaults;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class App extends JFrame {

        private static final String RAC_PRESET_CUSTOM = "Personalizzato";
        private static final String RAC_PRESET_AR = "AR";
        private static final String RAC_PRESET_AG = "AG";
        private static final String RAC_RETURN_TEXT_DEFAULT =
                        "Per inesitati restituire a : Mediaprint S.r.l. 64015 Nereto TE";

        private static final String RC_OMOLOG_PRESET_MASSIVA = "Posta massiva (DCOOS2065)";
        private static final String RC_OMOLOG_PRESET_RACCOMANDATA = "Raccomandata (DCOCC0015)";
        private static final String RC_OMOLOG_PRESET_AG = "AG (DCOPD1063)";
        private static final String RC_OMOLOG_PRESET_CUSTOM = "Personalizzata";
        private static final String TAB_LOG = "Log";
        private static final String HEAP_ERROR_TITLE = "Memoria insufficiente";
        private static final String HEAP_ERROR_MESSAGE = "Memoria insufficiente durante l'elaborazione.\n"
                        + "Il programma ha raggiunto il limite di memoria Java heap space.\n"
                        + "Chiudi altri programmi o prova con un PDF piu' piccolo.";

        private static final GeneralTabDefaults GENERAL_DEFAULTS = GeneralTabDefaults.getInstance();
        private static final CounterTabDefaults COUNTER_DEFAULTS = CounterTabDefaults.getInstance();
        private static final BarcodeImbustatriceTabDefaults BARCODE_IMBUSTATRICE_DEFAULTS = BarcodeImbustatriceTabDefaults
                        .getInstance();
        private static final BarcodeRaccomandataTabDefaults BARCODE_RACCOMANDATA_DEFAULTS = BarcodeRaccomandataTabDefaults
                        .getInstance();
        private static final OmologazionePostaleTabDefaults OMOLOGAZIONE_DEFAULTS = OmologazionePostaleTabDefaults
                        .getInstance();
        private static final QrCodeTabDefaults QR_CODE_DEFAULTS = QrCodeTabDefaults.getInstance();
        private static final AddressBlockTabDefaults ADDRESS_BLOCK_DEFAULTS = AddressBlockTabDefaults.getInstance();
        private static final RisoTabDefaults RISO_DEFAULTS = RisoTabDefaults.getInstance();
        private static final PostaEvolutionTabDefaults EVOLUTION_DEFAULTS = PostaEvolutionTabDefaults.getInstance();
        private static final LogTabDefaults LOG_DEFAULTS = LogTabDefaults.getInstance();
        private static final AtomicBoolean HEAP_ERROR_DIALOG_VISIBLE = new AtomicBoolean(false);

        // --- Base ---
        private JTextField txtInput;
        private JTextField txtOutput;
        private JTextField txtMarker;
        private JCheckBox chkIgnoreCase;
        private JCheckBox chkNormalize;
        private JCheckBox chkPdfSmartMode;
        private JCheckBox chkRotateByText;
        private JCheckBox chkResizeOnRotatedPages;
        private JTextField txtRotateByText;
        private JComboBox<String> cmbRotateByTextDegrees;
        private JButton btnRun;
        private JButton btnCancel;
        private JButton btnReadAddress;
        private JButton btnImportConfig;
        private JButton btnExportConfig;
        private JButton btnInputBrowse;
        private JButton btnOutputBrowse;
        private JComboBox<PdfVersionChoice> cmbPdfVersion;
        private JProgressBar progressBar;
        private JTextArea txtLog;
        private JTabbedPane contentTabs;
        private SwingWorker<?, ?> currentWorker;

        // --- Resize contenuto ---
        private JCheckBox chkApplyResize;
        private JCheckBox chkForceA4BeforeResize;
        private JTextField txtScalePct; // es. "92"
        private JTextField txtOffsetXmm; // es. "6"
        private JTextField txtOffsetYmm; // es. "8"

        // --- Barcode imbustatrice ---
        private JCheckBox chkBarcode;
        private JCheckBox chkBarcodeOmr;
        private JCheckBox chkAllegati;
        private JCheckBox chkBarcodeOnAttachment;
        private JTextField txtBcXmm;
        private JTextField txtBcYmm;
        private JTextField txtBcXpt;
        private JTextField txtBcYpt;
        private JTextField txtBcModulePt;
        private JTextField txtBcBarHeightPt;
        private JTextField txtBcFontSizePt;
        private JTextField txtBcRotationDeg;
        private JTextField txtBcYOffsetPt;
        private JTextField txtBcStartProg;
        private JCheckBox chkGroupLabel;
        private JTextField txtLabelId;
        private JTextField txtLabelXmm;
        private JTextField txtLabelYmm;
        private JTextField txtLabelXpt;
        private JTextField txtLabelYpt;
        private JRadioButton rdbLabelHorizontal;
        private JRadioButton rdbLabelVertical;
        private JButton btnReticolo;

        // --- Barcode Raccomandata ---
        private JCheckBox chkRaccomandataBarcode;
        private JTextField txtRcXmm;
        private JTextField txtRcYmm;
        private JTextField txtRcXpt;
        private JTextField txtRcYpt;
        private JTextField txtRcHeightMm;
        private JTextField txtRcModuleMm;
        private JTextField txtRcHumanFontPt;
        private JTextField txtRcHumanGapMm;
        private JTextField txtRcPrefix;
        private JTextField txtRcPrefixGapMm;
        private JTextField txtRcPrefixFontPt;
        private JTextField txtRcStartCode;
        private JComboBox<String> cmbRacPreset;
        private final Map<String, RacPreset> racPresets = new LinkedHashMap<>();
        private String currentRacPresetKey;
        private boolean updatingRacFields;
        private JCheckBox chkRcCustomText;
        private JTextField txtRcCustomText;
        private JTextField txtRcCustomTextXmm;
        private JTextField txtRcCustomTextYmm;
        private JTextField txtRcCustomTextXpt;
        private JTextField txtRcCustomTextYpt;
        private JTextField txtRcCustomTextFontPt;

        private JCheckBox chkRcOmologazione;
        private JComboBox<String> cmbRcOmologazionePreset;
        private JTextField txtRcOmologazioneText;
        private JTextField txtRcOmologazioneXmm;
        private JTextField txtRcOmologazioneYmm;
        private JTextField txtRcOmologazioneXpt;
        private JTextField txtRcOmologazioneYpt;
        private JTextField txtRcOmologazioneFontPt;
        private final Map<String, String> omologazioneOptions = new LinkedHashMap<>();
        private String currentOmologazionePresetKey;
        private boolean updatingOmologazioneFields;

        // --- Contatore pagine ---
        private JCheckBox chkPageCounter;
        private JTextField txtCounterXmm;
        private JTextField txtCounterYmm;
        private JTextField txtCounterXpt;
        private JTextField txtCounterYpt;
        private JTextField txtCounterFontSizePt;
        private JRadioButton rdbCounterHorizontal;
        private JRadioButton rdbCounterVertical;

        // --- QR Code ---
        private JCheckBox chkQrCode;
        private JTextField txtQrBase;
        private JTextField txtQrDigits;
        private JTextField txtQrStart;
        private JTextField txtQrExample;
        private JTextField txtQrSizeMm;
        private JTextField txtQrSizePt;
        private JTextField txtQrXmm;
        private JTextField txtQrYmm;
        private JTextField txtQrXpt;
        private JTextField txtQrYpt;
        private JComboBox<String> cmbQrErrorCorrection;
        private JCheckBox chkQrCorrections;
        private JTextField txtQrCorrectionsExcelPath;
        private JButton btnQrCorrectionsExcelBrowse;
        private JTextField txtQrCorrectionsXmm;
        private JTextField txtQrCorrectionsYmm;
        private JTextField txtQrCorrectionsXpt;
        private JTextField txtQrCorrectionsYpt;
        private JTextField txtQrCorrectionsWidthMm;
        private JTextField txtQrCorrectionsWidthPt;
        private JTextField txtQrCorrectionsFontPt;
        private JTextField txtQrCorrectionsIconMm;

        // --- Blocco indirizzo ---
        private JCheckBox chkAddressBlock;
        private JTextField txtAddressXmm;
        private JTextField txtAddressYmm;
        private JTextField txtAddressWidthMm;
        private JTextField txtAddressHeightMm;
        private JTextField txtAddressXpt;
        private JTextField txtAddressYpt;
        private JTextField txtAddressWidthPt;
        private JTextField txtAddressHeightPt;
        private JCheckBox chkAddressKeyRead;
        private JTextField txtAddressKeyString;

        // --- Ottimizzazione Riso ---
        private JCheckBox chkRisoOptimization;
        private JTextField txtRisoRecordId;

        // --- Posta Evolution ---
        private JCheckBox chkEvolution;
        private JTextField txtEvolutionXmm;
        private JTextField txtEvolutionYmm;
        private JTextField txtEvolutionXpt;
        private JTextField txtEvolutionYpt;
        private JTextField txtEvolutionModuleMm;
        private JTextField txtEvolutionWidthCells;
        private JTextField txtEvolutionHeightCells;
        private JTextField txtEvolutionGamma;
        private JTextField txtEvolutionSapId;
        private JTextField txtEvolutionClientId;
        private JTextField txtEvolutionClasse;
        private JTextField txtEvolutionTipoProdotto;
        private JTextField txtEvolutionCapDestFallback;
        private JTextField txtEvolutionCodTecDest;
        private JTextField txtEvolutionCapMitt;
        private JTextField txtEvolutionCodTecMitt;
        private JTextField txtEvolutionPrenFiglio;
        private JTextField txtEvolutionStampatore;
        private JTextField txtEvolutionStartOggetto;
        private JTextField txtEvolutionCausale;
        private JTextField txtEvolutionOmologazioneDm;
        private JTextField txtEvolutionCampo16;
        private JTextField txtEvolutionServizi;
        private JTextField txtEvolutionExcelPath;
        private JButton btnEvolutionExcelBrowse;
        private JCheckBox chkEvolutionDu;
        private JTextField txtEvolutionDuTipoAccettazioneFile;
        private JTextField txtEvolutionDuProgressivo;
        private JTextField txtEvolutionDuUtenzaOperatore;
        private JTextField txtEvolutionDuIdPrenotazione;
        private JTextField txtEvolutionDuDataPostalizzazione;
        private JTextField txtEvolutionDuFrazionario;
        private JTextField txtEvolutionDuTipologiaProdotto;
        private JTextField txtEvolutionDuCodiceProdotto;
        private JTextField txtEvolutionDuServizioAccessorio;
        private JTextField txtEvolutionDuCodiceTipologiaAccettazione;
        private JTextField txtEvolutionDuTipologiaTracciatura;
        private JTextField txtEvolutionDuCodiceConto;
        private JTextField txtEvolutionDuDescrizione;
        private JTextField txtEvolutionDuCodiceOmologazione;
        private JTextField txtEvolutionDuFormato;
        private JTextField txtEvolutionDuIdHu;
        private JTextField txtEvolutionDuIdScatola;
        private JComboBox<String> cmbEvolutionPreset;
        private JButton btnEvolutionPresetUpdate;
        private final Map<String, EvolutionPreset> evolutionPresets = new LinkedHashMap<>();
        private String currentEvolutionPresetKey;
        private boolean updatingEvolutionPreset;

        public App() {
                setTitle("PDF Work - Itext7 - Adobe 1.7");
                setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                setMinimumSize(new Dimension(1120, 640));
                initComponents();
                setLocationRelativeTo(null);
        }

        private void initComponents() {
                setLayout(new BorderLayout());

                txtInput = new JTextField();
                txtOutput = new JTextField();
                txtMarker = new JTextField();
                cmbPdfVersion = new JComboBox<>(PdfVersionChoice.values());

                chkIgnoreCase = new JCheckBox("Ignora maiuscole/minuscole");
                chkNormalize = new JCheckBox("Normalizza accenti/diacritici");
                chkPdfSmartMode = new JCheckBox("Smart mode iText");
                chkRotateByText = new JCheckBox("Ruota pagina in base a stringa");
                chkResizeOnRotatedPages = new JCheckBox("Applica resize anche alle pagine ruotate");
                txtRotateByText = new JTextField();
                txtRotateByText.setColumns(24);
                cmbRotateByTextDegrees = new JComboBox<>(new String[] { "90", "180", "270" });
                btnRun = new JButton("Esegui");
                btnCancel = new JButton("Interrompi");
                btnCancel.setEnabled(false);
                btnReadAddress = new JButton("Solo lettura indirizzo");
                btnImportConfig = new JButton("Importa configurazione");
                btnExportConfig = new JButton("Esporta configurazione");
                btnInputBrowse = new JButton("Scegli...");
                btnOutputBrowse = new JButton("Scegli...");

                chkApplyResize = new JCheckBox("Applica resize (post-normalizzazione)");
                chkForceA4BeforeResize = new JCheckBox("Forza foglio A4 prima del resize");
                txtScalePct = new JTextField();
                txtScalePct.setColumns(4);
                txtOffsetXmm = new JTextField();
                txtOffsetXmm.setColumns(4);
                txtOffsetYmm = new JTextField();
                txtOffsetYmm.setColumns(4);

                chkBarcode = new JCheckBox("Inserisci barcode imbustatrice");
                chkBarcodeOmr = new JCheckBox("Usa OMR (lettura ottica) al posto del C39");
                chkAllegati = new JCheckBox("Allegati presenti");
                chkAllegati.setEnabled(false);
                chkBarcodeOnAttachment = new JCheckBox("Il PDF target è un allegato");

                txtBcXmm = new JTextField();
                txtBcXmm.setColumns(6);
                txtBcYmm = new JTextField();
                txtBcYmm.setColumns(6);
                txtBcXpt = readOnlyField();
                txtBcYpt = readOnlyField();
                txtBcModulePt = new JTextField();
                txtBcModulePt.setColumns(6);
                txtBcBarHeightPt = new JTextField();
                txtBcBarHeightPt.setColumns(6);
                txtBcFontSizePt = readOnlyField();
                txtBcRotationDeg = readOnlyField();
                txtBcYOffsetPt = readOnlyField();
                txtBcStartProg = new JTextField();
                txtBcStartProg.setColumns(6);

                chkRaccomandataBarcode = new JCheckBox("Inserisci barcode Raccomandata");

                txtRcXmm = new JTextField();
                txtRcXmm.setColumns(6);
                txtRcYmm = new JTextField();
                txtRcYmm.setColumns(6);
                txtRcXpt = readOnlyField();
                txtRcXpt.setColumns(6);
                txtRcYpt = readOnlyField();
                txtRcYpt.setColumns(6);
                txtRcHeightMm = new JTextField();
                txtRcHeightMm.setColumns(6);
                txtRcModuleMm = new JTextField();
                txtRcModuleMm.setColumns(6);
                txtRcHumanFontPt = new JTextField();
                txtRcHumanFontPt.setColumns(6);
                txtRcHumanGapMm = new JTextField();
                txtRcHumanGapMm.setColumns(6);
                txtRcPrefix = new JTextField();
                txtRcPrefix.setColumns(4);
                txtRcPrefixGapMm = new JTextField();
                txtRcPrefixGapMm.setColumns(6);
                txtRcPrefixFontPt = new JTextField();
                txtRcPrefixFontPt.setColumns(6);
                txtRcStartCode = new JTextField();
                txtRcStartCode.setColumns(14);
                applyDigitFilter(txtRcStartCode, Imbustatrice.RaccomandataStandard.IDENTIFIER_DIGITS);
                chkRcCustomText = new JCheckBox("Testo personalizzato");
                txtRcCustomText = new JTextField();
                txtRcCustomText.setColumns(42);
                txtRcCustomTextXmm = new JTextField();
                txtRcCustomTextXmm.setColumns(6);
                txtRcCustomTextYmm = new JTextField();
                txtRcCustomTextYmm.setColumns(6);
                txtRcCustomTextXpt = readOnlyField();
                txtRcCustomTextXpt.setColumns(6);
                txtRcCustomTextYpt = readOnlyField();
                txtRcCustomTextYpt.setColumns(6);
                txtRcCustomTextFontPt = new JTextField();
                txtRcCustomTextFontPt.setColumns(6);

                initOmologazioneOptions();
                initRaccomandataPresets();
                initEvolutionPresets();
                cmbRacPreset = new JComboBox<>(racPresets.keySet().toArray(new String[0]));
                chkRcOmologazione = new JCheckBox("Omologazione postale");
                cmbRcOmologazionePreset = new JComboBox<>(omologazioneOptions.keySet().toArray(new String[0]));
                cmbRcOmologazionePreset.addActionListener(e -> handleOmologazionePresetSelection());
                txtRcOmologazioneText = new JTextField();
                txtRcOmologazioneText.setColumns(16);
                txtRcOmologazioneXmm = new JTextField();
                txtRcOmologazioneXmm.setColumns(6);
                txtRcOmologazioneYmm = new JTextField();
                txtRcOmologazioneYmm.setColumns(6);
                txtRcOmologazioneXpt = readOnlyField();
                txtRcOmologazioneXpt.setColumns(6);
                txtRcOmologazioneYpt = readOnlyField();
                txtRcOmologazioneYpt.setColumns(6);
                txtRcOmologazioneFontPt = new JTextField();
                txtRcOmologazioneFontPt.setColumns(6);

                chkGroupLabel = new JCheckBox("Etichetta gruppo/ID");
                txtLabelId = new JTextField();
                txtLabelId.setColumns(12);
                txtLabelXmm = new JTextField();
                txtLabelXmm.setColumns(6);
                txtLabelYmm = new JTextField();
                txtLabelYmm.setColumns(6);
                txtLabelXpt = readOnlyField();
                txtLabelYpt = readOnlyField();
                rdbLabelHorizontal = new JRadioButton("Orizzontale");
                rdbLabelVertical = new JRadioButton("Verticale");
                ButtonGroup labelOrientationGroup = new ButtonGroup();
                labelOrientationGroup.add(rdbLabelHorizontal);
                labelOrientationGroup.add(rdbLabelVertical);

                btnReticolo = new JButton("Anteprima reticolo");

                chkPageCounter = new JCheckBox("Contatore pagine gruppo");
                rdbCounterHorizontal = new JRadioButton("Orizzontale");
                rdbCounterVertical = new JRadioButton("Verticale");
                ButtonGroup counterOrientationGroup = new ButtonGroup();
                counterOrientationGroup.add(rdbCounterHorizontal);
                counterOrientationGroup.add(rdbCounterVertical);
                txtCounterXmm = new JTextField();
                txtCounterXmm.setColumns(6);
                txtCounterYmm = new JTextField();
                txtCounterYmm.setColumns(6);
                txtCounterXpt = readOnlyField();
                txtCounterYpt = readOnlyField();
                txtCounterFontSizePt = new JTextField();
                txtCounterFontSizePt.setColumns(6);

                chkQrCode = new JCheckBox("Inserisci QR code");
                txtQrBase = new JTextField();
                txtQrBase.setColumns(18);
                txtQrDigits = new JTextField();
                txtQrDigits.setColumns(4);
                txtQrStart = new JTextField();
                txtQrStart.setColumns(20);
                txtQrExample = readOnlyField();
                txtQrExample.setColumns(18);
                txtQrSizeMm = new JTextField();
                txtQrSizeMm.setColumns(6);
                txtQrSizePt = readOnlyField();
                txtQrXmm = new JTextField();
                txtQrXmm.setColumns(6);
                txtQrYmm = new JTextField();
                txtQrYmm.setColumns(6);
                txtQrXpt = readOnlyField();
                txtQrYpt = readOnlyField();
                cmbQrErrorCorrection = new JComboBox<>(new String[] { "L", "M", "Q", "H" });
                chkQrCorrections = new JCheckBox("Inserisci correzioni da Excel");
                txtQrCorrectionsExcelPath = new JTextField();
                txtQrCorrectionsExcelPath.setColumns(30);
                btnQrCorrectionsExcelBrowse = new JButton("Scegli...");
                txtQrCorrectionsXmm = new JTextField();
                txtQrCorrectionsXmm.setColumns(6);
                txtQrCorrectionsYmm = new JTextField();
                txtQrCorrectionsYmm.setColumns(6);
                txtQrCorrectionsXpt = readOnlyField();
                txtQrCorrectionsXpt.setColumns(6);
                txtQrCorrectionsYpt = readOnlyField();
                txtQrCorrectionsYpt.setColumns(6);
                txtQrCorrectionsWidthMm = new JTextField();
                txtQrCorrectionsWidthMm.setColumns(6);
                txtQrCorrectionsWidthPt = readOnlyField();
                txtQrCorrectionsWidthPt.setColumns(6);
                txtQrCorrectionsFontPt = new JTextField();
                txtQrCorrectionsFontPt.setColumns(6);
                txtQrCorrectionsIconMm = new JTextField();
                txtQrCorrectionsIconMm.setColumns(6);
                applyDigitFilter(txtQrDigits, 2);

                chkAddressBlock = new JCheckBox("Leggi blocco indirizzo");
                txtAddressXmm = new JTextField();
                txtAddressXmm.setColumns(6);
                txtAddressYmm = new JTextField();
                txtAddressYmm.setColumns(6);
                txtAddressWidthMm = new JTextField();
                txtAddressWidthMm.setColumns(6);
                txtAddressHeightMm = new JTextField();
                txtAddressHeightMm.setColumns(6);
                txtAddressXpt = readOnlyField();
                txtAddressXpt.setColumns(6);
                txtAddressYpt = readOnlyField();
                txtAddressYpt.setColumns(6);
                txtAddressWidthPt = readOnlyField();
                txtAddressWidthPt.setColumns(6);
                txtAddressHeightPt = readOnlyField();
                txtAddressHeightPt.setColumns(6);
                chkAddressKeyRead = new JCheckBox("Lettura stringa chiave su PDF");
                txtAddressKeyString = new JTextField();
                txtAddressKeyString.setColumns(12);
                applyDigitFilter(txtQrStart, 9);

                chkRisoOptimization = new JCheckBox("Ottimizza PDF per Riso GL9730 (PDF/A-3B)");
                txtRisoRecordId = new JTextField();
                txtRisoRecordId.setColumns(24);

                chkEvolution = new JCheckBox("Abilita elaborazione Posta Evolution");
                txtEvolutionXmm = new JTextField();
                txtEvolutionXmm.setColumns(6);
                txtEvolutionYmm = new JTextField();
                txtEvolutionYmm.setColumns(6);
                txtEvolutionXpt = readOnlyField();
                txtEvolutionXpt.setColumns(6);
                txtEvolutionYpt = readOnlyField();
                txtEvolutionYpt.setColumns(6);
                txtEvolutionModuleMm = new JTextField();
                txtEvolutionModuleMm.setColumns(6);
                txtEvolutionWidthCells = new JTextField();
                txtEvolutionWidthCells.setColumns(4);
                txtEvolutionHeightCells = new JTextField();
                txtEvolutionHeightCells.setColumns(4);
                txtEvolutionGamma = new JTextField();
                txtEvolutionGamma.setColumns(3);
                txtEvolutionSapId = new JTextField();
                txtEvolutionSapId.setColumns(10);
                txtEvolutionClientId = new JTextField();
                txtEvolutionClientId.setColumns(6);
                txtEvolutionClasse = new JTextField();
                txtEvolutionClasse.setColumns(3);
                txtEvolutionTipoProdotto = new JTextField();
                txtEvolutionTipoProdotto.setColumns(3);
                txtEvolutionCapDestFallback = new JTextField();
                txtEvolutionCapDestFallback.setColumns(6);
                txtEvolutionCodTecDest = new JTextField();
                txtEvolutionCodTecDest.setColumns(6);
                txtEvolutionCapMitt = new JTextField();
                txtEvolutionCapMitt.setColumns(6);
                txtEvolutionCodTecMitt = new JTextField();
                txtEvolutionCodTecMitt.setColumns(6);
                txtEvolutionPrenFiglio = new JTextField();
                txtEvolutionPrenFiglio.setColumns(6);
                txtEvolutionStampatore = new JTextField();
                txtEvolutionStampatore.setColumns(4);
                txtEvolutionStartOggetto = new JTextField();
                txtEvolutionStartOggetto.setColumns(8);
                txtEvolutionCausale = new JTextField();
                txtEvolutionCausale.setColumns(4);
                txtEvolutionOmologazioneDm = new JTextField();
                txtEvolutionOmologazioneDm.setColumns(8);
                txtEvolutionCampo16 = new JTextField();
                txtEvolutionCampo16.setColumns(10);
                txtEvolutionServizi = new JTextField();
                txtEvolutionServizi.setColumns(10);
                txtEvolutionExcelPath = new JTextField();
                txtEvolutionExcelPath.setColumns(30);
                btnEvolutionExcelBrowse = new JButton("Scegli...");
                chkEvolutionDu = new JCheckBox("Genera report DU");
                txtEvolutionDuTipoAccettazioneFile = new JTextField();
                txtEvolutionDuTipoAccettazioneFile.setColumns(3);
                txtEvolutionDuProgressivo = new JTextField();
                txtEvolutionDuProgressivo.setColumns(4);
                txtEvolutionDuUtenzaOperatore = new JTextField();
                txtEvolutionDuUtenzaOperatore.setColumns(16);
                txtEvolutionDuIdPrenotazione = new JTextField();
                txtEvolutionDuIdPrenotazione.setColumns(10);
                txtEvolutionDuDataPostalizzazione = new JTextField();
                txtEvolutionDuDataPostalizzazione.setColumns(10);
                txtEvolutionDuFrazionario = new JTextField();
                txtEvolutionDuFrazionario.setColumns(6);
                txtEvolutionDuTipologiaProdotto = new JTextField();
                txtEvolutionDuTipologiaProdotto.setColumns(4);
                txtEvolutionDuCodiceProdotto = new JTextField();
                txtEvolutionDuCodiceProdotto.setColumns(4);
                txtEvolutionDuServizioAccessorio = new JTextField();
                txtEvolutionDuServizioAccessorio.setColumns(8);
                txtEvolutionDuCodiceTipologiaAccettazione = new JTextField();
                txtEvolutionDuCodiceTipologiaAccettazione.setColumns(6);
                txtEvolutionDuTipologiaTracciatura = new JTextField();
                txtEvolutionDuTipologiaTracciatura.setColumns(4);
                txtEvolutionDuCodiceConto = new JTextField();
                txtEvolutionDuCodiceConto.setColumns(16);
                txtEvolutionDuDescrizione = new JTextField();
                txtEvolutionDuDescrizione.setColumns(20);
                txtEvolutionDuCodiceOmologazione = new JTextField();
                txtEvolutionDuCodiceOmologazione.setColumns(10);
                txtEvolutionDuFormato = new JTextField();
                txtEvolutionDuFormato.setColumns(6);
                txtEvolutionDuIdHu = new JTextField();
                txtEvolutionDuIdHu.setColumns(12);
                txtEvolutionDuIdScatola = new JTextField();
                txtEvolutionDuIdScatola.setColumns(12);
                cmbEvolutionPreset = new JComboBox<>(evolutionPresets.keySet().toArray(new String[0]));
                btnEvolutionPresetUpdate = new JButton("Aggiorna preset");

                txtLog = new JTextArea();
                progressBar = new JProgressBar(LOG_DEFAULTS.getInt("progressMin"), LOG_DEFAULTS.getInt("progressMax"));
                progressBar.setStringPainted(true);

                applyTabDefaults();

                loadRacPreset(currentRacPresetKey);
                handleOmologazionePresetSelection();
                cmbRacPreset.addActionListener(e -> handleRacPresetSelection());
                cmbEvolutionPreset.addActionListener(e -> handleEvolutionPresetSelection());
                btnEvolutionPresetUpdate.addActionListener(e -> refreshEvolutionPresets());

                JPanel container = new JPanel(new BorderLayout(12, 12));
                container.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

                contentTabs = buildContentTabs();
                JPanel actionsPanel = buildGlobalActionsPanel();
                JPanel tabsWrapper = new JPanel(new BorderLayout());
                tabsWrapper.add(contentTabs, BorderLayout.CENTER);
                tabsWrapper.add(actionsPanel, BorderLayout.SOUTH);

                container.add(tabsWrapper, BorderLayout.CENTER);
                add(container, BorderLayout.CENTER);

                // --- Actions ---
                btnInputBrowse.addActionListener(e -> chooseInput());
                btnOutputBrowse.addActionListener(e -> chooseOutput());
                btnQrCorrectionsExcelBrowse.addActionListener(e -> chooseQrCorrectionsExcel());
                btnEvolutionExcelBrowse.addActionListener(e -> chooseEvolutionExcel());
                btnImportConfig.addActionListener(e -> importConfiguration());
                btnExportConfig.addActionListener(e -> exportConfiguration());
                btnRun.addActionListener(e -> {
                        showLogTab();
                        runProcess();
                });
                btnCancel.addActionListener(e -> cancelCurrentWorker());
                btnReadAddress.addActionListener(e -> runAddressRead());
                btnReticolo.addActionListener(e -> runReticlePreview());

                chkApplyResize.addActionListener(e -> toggleResizeFields(chkApplyResize.isSelected()));
                chkRotateByText.addActionListener(e -> toggleRotateByTextFields(chkRotateByText.isSelected()));
                chkBarcode.addActionListener(e -> {
                        toggleBarcodeFields(chkBarcode.isSelected());
                        updateReticleButtonState();
                });
                chkRaccomandataBarcode.addActionListener(e -> {
                        toggleRaccomandataFields(chkRaccomandataBarcode.isSelected());
                        updateOmologazioneFieldsState();
                        updateReticleButtonState();
                });
                chkRcOmologazione.addActionListener(e -> {
                        updateOmologazioneFieldsState();
                        storeActiveRacPresetFromFields();
                });
                chkRcCustomText.addActionListener(e -> {
                        updateRacCustomTextFieldsState();
                        storeActiveRacPresetFromFields();
                        updateReticleButtonState();
                });
                chkGroupLabel.addActionListener(
                                e -> toggleLabelFields(chkGroupLabel.isSelected()));
                chkPageCounter.addActionListener(e -> togglePageCounterFields(chkPageCounter.isSelected()));
                chkQrCode.addActionListener(e -> {
                        toggleQrFields(chkQrCode.isSelected());
                        updateQrDerivedFields();
                        updateReticleButtonState();
                });
                chkQrCorrections.addActionListener(e -> {
                        toggleQrCorrectionsFields(chkQrCorrections.isSelected());
                        updateQrCorrectionsDerivedFields();
                        updateReticleButtonState();
                });
                chkAddressBlock.addActionListener(e -> {
                        toggleAddressBlockFields(chkAddressBlock.isSelected());
                        updateAddressBlockDerivedFields();
                });
                chkAddressKeyRead.addActionListener(e -> toggleAddressKeyReadFields(chkAddressKeyRead.isSelected()));
                chkRisoOptimization.addActionListener(e -> toggleRisoFields(chkRisoOptimization.isSelected()));
                chkEvolution.addActionListener(e -> toggleEvolutionFields(chkEvolution.isSelected()));
                chkEvolutionDu.addActionListener(e -> toggleEvolutionDuFields(chkEvolution.isSelected()
                                && chkEvolutionDu.isSelected()));
                attachBarcodePositionListeners();
                attachRaccomandataListeners();
                attachQrListeners();
                attachQrCorrectionsListeners();
                attachAddressBlockListeners();
                attachEvolutionListeners();
                applyBarcodeFixedDefaults();
                updateRaccomandataDerivedFields();
                updateQrDerivedFields();
                updateQrCorrectionsDerivedFields();
                updateAddressBlockDerivedFields();
                updateEvolutionDerivedFields();
                toggleResizeFields(chkApplyResize.isSelected());
                toggleRotateByTextFields(chkRotateByText.isSelected());
                toggleBarcodeFields(chkBarcode.isSelected());
                toggleLabelFields(chkGroupLabel.isSelected());
                toggleRaccomandataFields(chkRaccomandataBarcode.isSelected());
                updateOmologazioneFieldsState();
                updateRacCustomTextFieldsState();
                togglePageCounterFields(chkPageCounter.isSelected());
                toggleQrFields(chkQrCode.isSelected());
                toggleQrCorrectionsFields(chkQrCorrections.isSelected());
                toggleAddressBlockFields(chkAddressBlock.isSelected());
                toggleAddressKeyReadFields(chkAddressKeyRead.isSelected());
                toggleRisoFields(chkRisoOptimization.isSelected());
                toggleEvolutionFields(chkEvolution.isSelected());
                updateReticleButtonState();
        }

        private void showLogTab() {
                if (contentTabs == null) {
                        return;
                }
                int logIndex = contentTabs.indexOfTab(TAB_LOG);
                if (logIndex >= 0) {
                        contentTabs.setSelectedIndex(logIndex);
                }
        }

        private void applyTabDefaults() {
                applyGeneralDefaults();
                applyBarcodeDefaults();
                applyRaccomandataDefaults();
                applyOmologazioneDefaults();
                applyCounterDefaults();
                applyQrDefaults();
                applyAddressDefaults();
                applyRisoDefaults();
                applyEvolutionDefaults();
                applyLogDefaults();
        }

        private void applyGeneralDefaults() {
                txtInput.setText(GENERAL_DEFAULTS.getString("pdfInputPath"));
                txtOutput.setText(GENERAL_DEFAULTS.getString("pdfOutputPath"));
                txtMarker.setText(GENERAL_DEFAULTS.getString("marker"));

                PdfVersion pdfVersion = GENERAL_DEFAULTS.getValue("pdfVersion", PdfVersion.class);
                cmbPdfVersion.setSelectedItem(PdfVersionChoice.fromVersion(pdfVersion));

                chkIgnoreCase.setSelected(GENERAL_DEFAULTS.getBoolean("ignoreCase"));
                chkNormalize.setSelected(GENERAL_DEFAULTS.getBoolean("normalizeAccents"));
                chkPdfSmartMode.setSelected(GENERAL_DEFAULTS.getBoolean("pdfSmartMode"));
                chkRotateByText.setSelected(GENERAL_DEFAULTS.getBoolean("rotateByTextEnabled"));
                chkResizeOnRotatedPages.setSelected(GENERAL_DEFAULTS.getBoolean("rotateApplyResizeOnMatchedPages"));
                txtRotateByText.setText(GENERAL_DEFAULTS.getString("rotateByTextString"));
                cmbRotateByTextDegrees
                                .setSelectedItem(GENERAL_DEFAULTS.getString("rotateByTextDegrees"));

                chkApplyResize.setSelected(GENERAL_DEFAULTS.getBoolean("resizeEnabled"));
                chkForceA4BeforeResize.setSelected(GENERAL_DEFAULTS.getBoolean("resizeForceA4BeforeResize"));
                txtScalePct.setText(GENERAL_DEFAULTS.getString("scalePercent"));
                txtOffsetXmm.setText(GENERAL_DEFAULTS.getString("offsetXmm"));
                txtOffsetYmm.setText(GENERAL_DEFAULTS.getString("offsetYmm"));

                chkGroupLabel.setSelected(GENERAL_DEFAULTS.getBoolean("groupLabelEnabled"));
                txtLabelId.setText(GENERAL_DEFAULTS.getString("labelIdentifier"));
                txtLabelXmm.setText(GENERAL_DEFAULTS.getString("labelXmm"));
                txtLabelYmm.setText(GENERAL_DEFAULTS.getString("labelYmm"));
                applyLabelOrientation(GENERAL_DEFAULTS.getValue("labelOrientation",
                                GeneralTabDefaults.LabelOrientation.class));
        }

        private void applyBarcodeDefaults() {
                chkBarcode.setSelected(BARCODE_IMBUSTATRICE_DEFAULTS.getBoolean("barcodeEnabled"));
                chkBarcodeOmr.setSelected(BARCODE_IMBUSTATRICE_DEFAULTS.getBoolean("useOmr"));
                chkAllegati.setSelected(BARCODE_IMBUSTATRICE_DEFAULTS.getBoolean("allegatiPresenti"));
                chkBarcodeOnAttachment
                                .setSelected(BARCODE_IMBUSTATRICE_DEFAULTS.getBoolean("barcodeOnAttachment"));
                txtBcXmm.setText(formatFloat(BARCODE_IMBUSTATRICE_DEFAULTS.getFloat("positionXmm"), 2));
                txtBcYmm.setText(formatFloat(BARCODE_IMBUSTATRICE_DEFAULTS.getFloat("positionYmm"), 2));
                txtBcModulePt.setText(
                                formatFloat(BARCODE_IMBUSTATRICE_DEFAULTS.getFloat("moduleWidthPt"), 2));
                txtBcBarHeightPt.setText(
                                formatFloat(BARCODE_IMBUSTATRICE_DEFAULTS.getFloat("barHeightPt"), 2));
                txtBcFontSizePt.setText(
                                formatFloat(BARCODE_IMBUSTATRICE_DEFAULTS.getFloat("labelFontSizePt"), 2));
                txtBcRotationDeg.setText(
                                formatFloat(BARCODE_IMBUSTATRICE_DEFAULTS.getFloat("rotationDegrees"), 0));
                txtBcYOffsetPt.setText(
                                formatFloat(BARCODE_IMBUSTATRICE_DEFAULTS.getFloat("verticalOffsetPt"), 2));
                txtBcStartProg.setText(BARCODE_IMBUSTATRICE_DEFAULTS.getString("startProgressive"));
        }

        private void applyRaccomandataDefaults() {
                chkRaccomandataBarcode.setSelected(BARCODE_RACCOMANDATA_DEFAULTS.getBoolean("barcodeEnabled"));
                txtRcHeightMm.setText(
                                formatFloat(BARCODE_RACCOMANDATA_DEFAULTS.getFloat("barcodeHeightMm"), 1));
                txtRcModuleMm.setText(
                                formatFloat(BARCODE_RACCOMANDATA_DEFAULTS.getFloat("moduleWidthMm"), 2));
                txtRcHumanFontPt.setText(
                                formatFloat(BARCODE_RACCOMANDATA_DEFAULTS.getFloat("humanReadableFontPt"), 2));
                txtRcHumanGapMm.setText(
                                formatFloat(BARCODE_RACCOMANDATA_DEFAULTS.getFloat("humanReadableGapMm"), 1));
                txtRcPrefix.setText(BARCODE_RACCOMANDATA_DEFAULTS.getString("prefix"));
                txtRcPrefixGapMm.setText(
                                formatFloat(BARCODE_RACCOMANDATA_DEFAULTS.getFloat("prefixGapMm"), 1));
                txtRcPrefixFontPt.setText(
                                formatFloat(BARCODE_RACCOMANDATA_DEFAULTS.getFloat("prefixFontPt"), 2));
                txtRcStartCode.setText(BARCODE_RACCOMANDATA_DEFAULTS.getString("startCode"));
                txtRcXmm.setText(formatFloat(BARCODE_RACCOMANDATA_DEFAULTS.getFloat("positionXmm"), 2));
                txtRcYmm.setText(formatFloat(BARCODE_RACCOMANDATA_DEFAULTS.getFloat("positionYmm"), 2));

                currentRacPresetKey = BARCODE_RACCOMANDATA_DEFAULTS.getString("presetKey");
                if (!racPresets.containsKey(currentRacPresetKey)) {
                        currentRacPresetKey = RAC_PRESET_AR;
                }
                if (cmbRacPreset != null) {
                        cmbRacPreset.setSelectedItem(currentRacPresetKey);
                }
        }

        private void applyOmologazioneDefaults() {
                chkRcOmologazione.setSelected(OMOLOGAZIONE_DEFAULTS.getBoolean("omologazioneEnabled"));
                currentOmologazionePresetKey = OMOLOGAZIONE_DEFAULTS.getString("presetKey");
                if (!omologazioneOptions.containsKey(currentOmologazionePresetKey)) {
                        currentOmologazionePresetKey = RC_OMOLOG_PRESET_CUSTOM;
                }
                if (cmbRcOmologazionePreset != null) {
                        cmbRcOmologazionePreset.setSelectedItem(currentOmologazionePresetKey);
                }
                txtRcOmologazioneText.setText(OMOLOGAZIONE_DEFAULTS.getString("codice"));
                txtRcOmologazioneXmm.setText(
                                formatFloat(OMOLOGAZIONE_DEFAULTS.getFloat("positionXmm"), 2));
                txtRcOmologazioneYmm.setText(
                                formatFloat(OMOLOGAZIONE_DEFAULTS.getFloat("positionYmm"), 2));
                txtRcOmologazioneFontPt.setText(
                                formatFloat(OMOLOGAZIONE_DEFAULTS.getFloat("fontHeightPt"), 2));
        }

        private void applyCounterDefaults() {
                chkPageCounter.setSelected(COUNTER_DEFAULTS.getBoolean("pageCounterEnabled"));
                CounterTabDefaults.Orientation orientation = COUNTER_DEFAULTS.getValue("orientation",
                                CounterTabDefaults.Orientation.class);
                boolean vertical = orientation == null || orientation == CounterTabDefaults.Orientation.VERTICAL;
                rdbCounterVertical.setSelected(vertical);
                rdbCounterHorizontal.setSelected(!vertical);
                txtCounterXmm.setText(COUNTER_DEFAULTS.getString("positionXmm"));
                txtCounterYmm.setText(COUNTER_DEFAULTS.getString("positionYmm"));
                txtCounterFontSizePt.setText(COUNTER_DEFAULTS.getString("fontSizePt"));
        }

        private void applyQrDefaults() {
                chkQrCode.setSelected(QR_CODE_DEFAULTS.getBoolean("qrEnabled"));
                txtQrBase.setText(QR_CODE_DEFAULTS.getString("base"));
                txtQrDigits.setText(QR_CODE_DEFAULTS.getString("digits"));
                txtQrStart.setText(QR_CODE_DEFAULTS.getString("startValue"));
                txtQrExample.setText(QR_CODE_DEFAULTS.getString("example"));
                txtQrSizeMm.setText(QR_CODE_DEFAULTS.getString("sizeMm"));
                txtQrXmm.setText(QR_CODE_DEFAULTS.getString("positionXmm"));
                txtQrYmm.setText(QR_CODE_DEFAULTS.getString("positionYmm"));
                cmbQrErrorCorrection.setSelectedItem(QR_CODE_DEFAULTS.getString("errorCorrectionLevel"));
                chkQrCorrections.setSelected(QR_CODE_DEFAULTS.getBoolean("correctionsEnabled"));
                txtQrCorrectionsExcelPath.setText(QR_CODE_DEFAULTS.getString("correctionsExcelPath"));
                txtQrCorrectionsXmm.setText(QR_CODE_DEFAULTS.getString("correctionsXmm"));
                txtQrCorrectionsYmm.setText(QR_CODE_DEFAULTS.getString("correctionsYmm"));
                txtQrCorrectionsWidthMm.setText(QR_CODE_DEFAULTS.getString("correctionsWidthMm"));
                txtQrCorrectionsFontPt.setText(QR_CODE_DEFAULTS.getString("correctionsFontPt"));
                txtQrCorrectionsIconMm.setText(QR_CODE_DEFAULTS.getString("correctionsIconMm"));
        }

        private void applyAddressDefaults() {
                chkAddressBlock.setSelected(ADDRESS_BLOCK_DEFAULTS.getBoolean("addressBlockEnabled"));
                txtAddressXmm.setText(ADDRESS_BLOCK_DEFAULTS.getString("positionXmm"));
                txtAddressYmm.setText(ADDRESS_BLOCK_DEFAULTS.getString("positionYmm"));
                txtAddressWidthMm.setText(ADDRESS_BLOCK_DEFAULTS.getString("widthMm"));
                txtAddressHeightMm.setText(ADDRESS_BLOCK_DEFAULTS.getString("heightMm"));
                chkAddressKeyRead.setSelected(ADDRESS_BLOCK_DEFAULTS.getBoolean("keyReadEnabled"));
                txtAddressKeyString.setText(ADDRESS_BLOCK_DEFAULTS.getString("keyString"));
        }

        private void applyRisoDefaults() {
                chkRisoOptimization.setSelected(RISO_DEFAULTS.getBoolean("optimizationEnabled"));
                txtRisoRecordId.setText(RISO_DEFAULTS.getString("recordId"));
        }

        private void applyEvolutionDefaults() {
                currentEvolutionPresetKey = firstEvolutionPresetKey();
                txtEvolutionExcelPath.setText(EVOLUTION_DEFAULTS.getString("excelPath"));
                txtEvolutionWidthCells.setText(EVOLUTION_DEFAULTS.getString("dataMatrixWidthCells"));
                txtEvolutionHeightCells.setText(EVOLUTION_DEFAULTS.getString("dataMatrixHeightCells"));
                loadEvolutionPreset(currentEvolutionPresetKey);
                if (cmbEvolutionPreset != null) {
                        updatingEvolutionPreset = true;
                        try {
                                cmbEvolutionPreset.setSelectedItem(currentEvolutionPresetKey);
                        } finally {
                                updatingEvolutionPreset = false;
                        }
                }
        }

        private String firstEvolutionPresetKey() {
                return evolutionPresets.keySet().stream().findFirst().orElse("");
        }

        private void applyLogDefaults() {
                txtLog.setRows(LOG_DEFAULTS.getInt("rows"));
                txtLog.setEditable(LOG_DEFAULTS.getBoolean("editable"));
                txtLog.setLineWrap(LOG_DEFAULTS.getBoolean("lineWrap"));
                txtLog.setWrapStyleWord(LOG_DEFAULTS.getBoolean("wrapStyleWord"));
                int initialProgress = LOG_DEFAULTS.getInt("initialProgressValue");
                progressBar.setValue(initialProgress);
                progressBar.setString(String.format(Locale.ITALIAN, "%d%%", initialProgress));
        }

        private void applyLabelOrientation(GeneralTabDefaults.LabelOrientation orientation) {
                boolean vertical = orientation == null
                                || orientation == GeneralTabDefaults.LabelOrientation.VERTICAL;
                rdbLabelVertical.setSelected(vertical);
                rdbLabelHorizontal.setSelected(!vertical);
        }

        private JTabbedPane buildContentTabs() {
                JTabbedPane tabs = new JTabbedPane();
                tabs.addTab("Generale", buildGeneralTab());
                tabs.addTab("Contatori", buildCounterTab());
                tabs.addTab("Barcode Imbustatrice", buildBarcodeTab());
                tabs.addTab("Barcode Raccomandata", buildBarcodeRaccomandataTab());
                tabs.addTab("Omologazione Postale", buildOmologazioneTab());
                tabs.addTab("QR Code", buildQrCodeTab());
                tabs.addTab("Blocco indirizzo", buildAddressBlockTab());
                tabs.addTab("Riso GL9730", buildRisoTab());
                tabs.addTab("Posta Evolution", buildPostaEvolutionTab());
                tabs.addTab(TAB_LOG, buildLogPanel());
                return tabs;
        }

        private JPanel buildGlobalActionsPanel() {
                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
                actions.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
                actions.add(btnRun);
                actions.add(btnCancel);
                actions.add(btnReadAddress);
                actions.add(btnReticolo);
                actions.add(btnImportConfig);
                actions.add(btnExportConfig);
                return actions;
        }

        private JPanel buildGeneralTab() {
                JPanel root = new JPanel();
                root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
                root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                root.add(createTitledPanel("File", createFileSection()));
                root.add(Box.createVerticalStrut(10));
                root.add(createTitledPanel("Opzioni", createGeneralOptionsSection()));
                root.add(Box.createVerticalStrut(10));
                root.add(createTitledPanel("Rotazione pagina", createPageRotationSection()));
                root.add(Box.createVerticalStrut(10));
                root.add(createTitledPanel("Etichetta gruppo", createLabelSection()));
                root.add(Box.createVerticalStrut(10));

                root.add(createTitledPanel("Resize", createResizeSection()));
                root.add(Box.createVerticalGlue());

                return root;
        }

        private JPanel createFileSection() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);

                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(4, 4, 4, 4);
                gbc.anchor = GridBagConstraints.WEST;

                gbc.gridx = 0;
                gbc.gridy = 0;
                panel.add(new JLabel("PDF input:"), gbc);

                gbc.gridx = 1;
                gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                panel.add(txtInput, gbc);

                gbc.gridx = 2;
                gbc.weightx = 0;
                gbc.fill = GridBagConstraints.NONE;
                panel.add(btnInputBrowse, gbc);

                gbc.gridx = 0;
                gbc.gridy = 1;
                panel.add(new JLabel("PDF output:"), gbc);

                gbc.gridx = 1;
                gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                panel.add(txtOutput, gbc);

                gbc.gridx = 2;
                gbc.weightx = 0;
                gbc.fill = GridBagConstraints.NONE;
                panel.add(btnOutputBrowse, gbc);

                gbc.gridx = 0;
                gbc.gridy = 2;
                gbc.gridwidth = 1;
                gbc.fill = GridBagConstraints.NONE;
                panel.add(new JLabel("Versione PDF output:"), gbc);

                gbc.gridx = 1;
                gbc.weightx = 0.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                panel.add(cmbPdfVersion, gbc);

                gbc.gridx = 0;
                gbc.gridy = 3;
                gbc.gridwidth = 3;
                gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.NONE;
                panel.add(new JLabel("Stringa marcatore (inizio lettera):"), gbc);

                gbc.gridy = 4;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                panel.add(txtMarker, gbc);

                return panel;
        }

        private JPanel createGeneralOptionsSection() {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(chkIgnoreCase);
                panel.add(chkNormalize);
                panel.add(chkPdfSmartMode);
                return panel;
        }

        private JPanel createPageRotationSection() {
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);

                chkRotateByText.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(chkRotateByText);
                panel.add(Box.createVerticalStrut(6));

                JPanel values = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                values.add(new JLabel("Stringa da cercare"));
                values.add(txtRotateByText);
                values.add(Box.createHorizontalStrut(12));
                values.add(new JLabel("Rotazione (gradi)"));
                values.add(cmbRotateByTextDegrees);
                panel.add(values);
                panel.add(Box.createVerticalStrut(6));
                chkResizeOnRotatedPages.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(chkResizeOnRotatedPages);
                return panel;
        }

        private JPanel createResizeSection() {
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);

                chkApplyResize.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(chkApplyResize);
                panel.add(Box.createVerticalStrut(6));
                chkForceA4BeforeResize.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(chkForceA4BeforeResize);
                panel.add(Box.createVerticalStrut(6));

                JPanel values = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                values.add(new JLabel("Scala (%)"));
                values.add(txtScalePct);
                values.add(Box.createHorizontalStrut(12));
                values.add(new JLabel("Offset X (mm)"));
                values.add(txtOffsetXmm);
                values.add(Box.createHorizontalStrut(12));
                values.add(new JLabel("Offset Y (mm)"));
                values.add(txtOffsetYmm);
                values.setAlignmentX(Component.LEFT_ALIGNMENT);
                panel.add(values);

                return panel;
        }

        private JPanel buildBarcodeTab() {
                JPanel root = new JPanel();
                root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
                root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JPanel toggles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                toggles.setAlignmentX(Component.LEFT_ALIGNMENT);
                toggles.add(chkBarcode);
                toggles.add(chkBarcodeOmr);
                toggles.add(chkAllegati);
                toggles.add(chkBarcodeOnAttachment);
                root.add(toggles);
                root.add(Box.createVerticalStrut(10));

                root.add(createTitledPanel("Posizionamento", createBarcodePositionSection()));
                root.add(Box.createVerticalStrut(10));
                root.add(createTitledPanel("Impostazioni barcode", createBarcodeAdvancedSection()));
                root.add(Box.createVerticalGlue());

                return root;
        }

        private JPanel buildBarcodeRaccomandataTab() {
                JPanel root = new JPanel();
                root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
                root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JPanel toggles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                toggles.setAlignmentX(Component.LEFT_ALIGNMENT);
                toggles.add(chkRaccomandataBarcode);
                root.add(toggles);
                root.add(Box.createVerticalStrut(10));

                root.add(createTitledPanel("Barcode Raccomandata", createRaccomandataSection()));
                root.add(Box.createVerticalStrut(10));
                root.add(createTitledPanel("Testo personalizzato Raccomandata", createRaccomandataCustomTextSection()));
                root.add(Box.createVerticalGlue());

                return root;
        }

        private JPanel createBarcodePositionSection() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(panel, 0, new JLabel("X (mm)"), txtBcXmm, new JLabel("X (pt)"), txtBcXpt);
                addFormRow(panel, 1, new JLabel("Y (mm)"), txtBcYmm, new JLabel("Y (pt)"), txtBcYpt);
                return panel;
        }

        private JPanel createBarcodeAdvancedSection() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(panel, 0, new JLabel("Modulo (pt)"), txtBcModulePt, new JLabel("Altezza barre (pt)"),
                                txtBcBarHeightPt);
                addFormRow(panel, 1, new JLabel("Font size (pt)"), txtBcFontSizePt, new JLabel("Rotazione (Â°)"),
                                txtBcRotationDeg);
                addFormRow(panel, 2, new JLabel("Offset Y extra (pt)"), txtBcYOffsetPt,
                                new JLabel("Progressivo gruppo iniziale"), txtBcStartProg);
                return panel;
        }

        private JPanel createRaccomandataSection() {
                JPanel container = new JPanel();
                container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
                container.setAlignmentX(Component.LEFT_ALIGNMENT);

                JPanel barcodePanel = new JPanel(new GridBagLayout());
                barcodePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(barcodePanel, 0, new JLabel("Profilo"), cmbRacPreset, null, null);
                addFormRow(barcodePanel, 1, new JLabel("X (mm)"), txtRcXmm, new JLabel("X (pt)"), txtRcXpt);
                addFormRow(barcodePanel, 2, new JLabel("Y (mm)"), txtRcYmm, new JLabel("Y (pt)"), txtRcYpt);
                addFormRow(barcodePanel, 3, new JLabel("Altezza barre (mm)"), txtRcHeightMm,
                                new JLabel("Modulo stretto (mm)"), txtRcModuleMm);
                addFormRow(barcodePanel, 4, new JLabel("Font numerazione (pt)"), txtRcHumanFontPt,
                                new JLabel("Gap numerazione (mm)"), txtRcHumanGapMm);
                addFormRow(barcodePanel, 5, new JLabel("Prefisso"), txtRcPrefix,
                                new JLabel("Gap prefisso (mm)"), txtRcPrefixGapMm);
                addFormRow(barcodePanel, 6, new JLabel("Altezza prefisso (pt)"), txtRcPrefixFontPt,
                                new JLabel("Codice iniziale (11 cifre)"), txtRcStartCode);

                container.add(barcodePanel);
                return container;
        }

        private JPanel createRaccomandataCustomTextSection() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(panel, 0, new JLabel("Abilita"), chkRcCustomText, null, null);
                addFormRow(panel, 1, new JLabel("Testo"), txtRcCustomText, null, null);
                addFormRow(panel, 2, new JLabel("X testo (mm)"), txtRcCustomTextXmm, new JLabel("X testo (pt)"),
                                txtRcCustomTextXpt);
                addFormRow(panel, 3, new JLabel("Y testo (mm)"), txtRcCustomTextYmm, new JLabel("Y testo (pt)"),
                                txtRcCustomTextYpt);
                addFormRow(panel, 4, new JLabel("Font testo (pt)"), txtRcCustomTextFontPt, null, null);
                return panel;
        }

        private JPanel createOmologazioneSection() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(panel, 0, new JLabel("Abilita"), chkRcOmologazione, new JLabel("Preset"),
                                cmbRcOmologazionePreset);
                addFormRow(panel, 1, new JLabel("Testo"), txtRcOmologazioneText, null, null);
                addFormRow(panel, 2, new JLabel("X testo (mm)"), txtRcOmologazioneXmm, new JLabel("X testo (pt)"),
                                txtRcOmologazioneXpt);
                addFormRow(panel, 3, new JLabel("Y testo (mm)"), txtRcOmologazioneYmm, new JLabel("Y testo (pt)"),
                                txtRcOmologazioneYpt);
                addFormRow(panel, 4, new JLabel("Font omologazione (pt)"), txtRcOmologazioneFontPt, null, null);
                return panel;
        }

        private JPanel buildOmologazioneTab() {
                JPanel root = new JPanel();
                root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
                root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                root.add(createTitledPanel("Omologazione Postale", createOmologazioneSection()));
                root.add(Box.createVerticalGlue());

                return root;
        }

        private JPanel buildQrCodeTab() {
                JPanel root = new JPanel();
                root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
                root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                togglePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                togglePanel.add(chkQrCode);
                root.add(togglePanel);
                root.add(Box.createVerticalStrut(10));

                root.add(createTitledPanel("QR Code", createQrCodeSection()));
                root.add(Box.createVerticalStrut(10));
                root.add(createTitledPanel("Correzioni da Excel", createQrCorrectionsSection()));
                root.add(Box.createVerticalGlue());

                return root;
        }

        private JPanel createQrCodeSection() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(panel, 0, new JLabel("Base alfanumerica"), txtQrBase, new JLabel("Progressivo iniziale"),
                                txtQrStart);
                addFormRow(panel, 1, new JLabel("Cifre progressivo"), txtQrDigits, new JLabel("Esempio"),
                                txtQrExample);
                addFormRow(panel, 2, new JLabel("Dimensione (mm)"), txtQrSizeMm, new JLabel("Dimensione (pt)"),
                                txtQrSizePt);
                addFormRow(panel, 3, new JLabel("Posizione X (mm)"), txtQrXmm, new JLabel("Posizione X (pt)"),
                                txtQrXpt);
                addFormRow(panel, 4, new JLabel("Posizione Y (mm)"), txtQrYmm, new JLabel("Posizione Y (pt)"),
                                txtQrYpt);
                addFormRow(panel, 5, new JLabel("Correzione errore"), cmbQrErrorCorrection, null, null);
                return panel;
        }

        private JPanel createQrCorrectionsSection() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(panel, 0, new JLabel("Abilita"), chkQrCorrections, null, null);
                addFormRow(panel, 1, new JLabel("File Excel correzioni"),
                                createFilePickerField(txtQrCorrectionsExcelPath, btnQrCorrectionsExcelBrowse), null,
                                null);
                addFormRow(panel, 2, new JLabel("X area (mm)"), txtQrCorrectionsXmm, new JLabel("X area (pt)"),
                                txtQrCorrectionsXpt);
                addFormRow(panel, 3, new JLabel("Y area (mm)"), txtQrCorrectionsYmm, new JLabel("Y area (pt)"),
                                txtQrCorrectionsYpt);
                addFormRow(panel, 4, new JLabel("Larghezza area (mm)"), txtQrCorrectionsWidthMm,
                                new JLabel("Larghezza area (pt)"), txtQrCorrectionsWidthPt);
                addFormRow(panel, 5, new JLabel("Font testo (pt)"), txtQrCorrectionsFontPt,
                                new JLabel("Icona (mm)"), txtQrCorrectionsIconMm);
                return panel;
        }

        private JPanel buildAddressBlockTab() {
                JPanel root = new JPanel();
                root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
                root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                togglePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                togglePanel.add(chkAddressBlock);
                root.add(togglePanel);
                root.add(Box.createVerticalStrut(10));

                root.add(createTitledPanel("Area di lettura", createAddressBlockSection()));
                root.add(Box.createVerticalStrut(10));
                root.add(createTitledPanel("Lettura stringa chiave", createAddressKeyReadSection()));
                root.add(Box.createVerticalGlue());

                return root;
        }

        private JPanel createAddressBlockSection() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(panel, 0, new JLabel("Posizione X (mm)"), txtAddressXmm, new JLabel("Posizione X (pt)"),
                                txtAddressXpt);
                addFormRow(panel, 1, new JLabel("Posizione Y (mm)"), txtAddressYmm, new JLabel("Posizione Y (pt)"),
                                txtAddressYpt);
                addFormRow(panel, 2, new JLabel("Larghezza (mm)"), txtAddressWidthMm,
                                new JLabel("Larghezza (pt)"), txtAddressWidthPt);
                addFormRow(panel, 3, new JLabel("Altezza (mm)"), txtAddressHeightMm, new JLabel("Altezza (pt)"),
                                txtAddressHeightPt);
                return panel;
        }

        private JPanel createAddressKeyReadSection() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(panel, 0, new JLabel("Abilita"), chkAddressKeyRead, new JLabel("Stringa chiave"),
                                txtAddressKeyString);
                addFormRow(panel, 1, new JLabel("Output"), new JLabel("lettura.xlsx nella cartella output/input"), null,
                                null);
                return panel;
        }

        private JPanel buildRisoTab() {
                JPanel root = new JPanel();
                root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
                root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                togglePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                togglePanel.add(chkRisoOptimization);
                root.add(togglePanel);
                root.add(Box.createVerticalStrut(10));

                root.add(createTitledPanel("Metadati PDF/A", createRisoSection()));
                root.add(Box.createVerticalGlue());
                return root;
        }

        private JPanel createRisoSection() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(panel, 0, new JLabel("Record ID"), txtRisoRecordId, null, null);
                return panel;
        }

        private JPanel buildPostaEvolutionTab() {
                JPanel root = new JPanel();
                root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
                root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                togglePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                togglePanel.add(chkEvolution);
                togglePanel.add(chkEvolutionDu);
                root.add(togglePanel);
                root.add(Box.createVerticalStrut(10));

                JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                presetPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                presetPanel.add(new JLabel("Preset rapido"));
                presetPanel.add(cmbEvolutionPreset);
                presetPanel.add(btnEvolutionPresetUpdate);
                root.add(presetPanel);
                root.add(Box.createVerticalStrut(10));

                root.add(createTitledPanel("Sorgente indirizzi", createPostaEvolutionAddressSourceSection()));
                root.add(Box.createVerticalStrut(10));
                root.add(createTitledPanel("DataMatrix", createPostaEvolutionDataMatrixSection()));
                root.add(Box.createVerticalStrut(10));
                root.add(createTitledPanel("Report DU", createPostaEvolutionDuSection()));
                root.add(Box.createVerticalGlue());
                return root;
        }

        private JPanel createPostaEvolutionDataMatrixSection() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(panel, 0, new JLabel("X (mm)"), txtEvolutionXmm, new JLabel("X (pt)"), txtEvolutionXpt);
                addFormRow(panel, 1, new JLabel("Y (mm)"), txtEvolutionYmm, new JLabel("Y (pt)"), txtEvolutionYpt);
                addFormRow(panel, 2, new JLabel("Modulo (mm)"), txtEvolutionModuleMm, new JLabel("Gamma"),
                                txtEvolutionGamma);
                addFormRow(panel, 3, new JLabel("Celle X"), txtEvolutionWidthCells, new JLabel("Celle Y"),
                                txtEvolutionHeightCells);
                addFormRow(panel, 4, new JLabel("SAP cliente (8)"), txtEvolutionSapId, new JLabel("ID cliente (3)"),
                                txtEvolutionClientId);
                addFormRow(panel, 5, new JLabel("Classe"), txtEvolutionClasse, null, null);
                addFormRow(panel, 6, new JLabel("Tipo prodotto"), txtEvolutionTipoProdotto,
                                new JLabel("CAP destinatario fallback"), txtEvolutionCapDestFallback);
                addFormRow(panel, 7, new JLabel("Cod. tecnico destinatario"), txtEvolutionCodTecDest,
                                new JLabel("CAP mittente"), txtEvolutionCapMitt);
                addFormRow(panel, 8, new JLabel("Cod. tecnico mittente"), txtEvolutionCodTecMitt,
                                boldLabel("ID prenotazione figlio"), txtEvolutionPrenFiglio);
                addFormRow(panel, 9, new JLabel("ID stampatore"), txtEvolutionStampatore,
                                new JLabel("Oggetto iniziale"), txtEvolutionStartOggetto);
                addFormRow(panel, 10, new JLabel("Causale"), txtEvolutionCausale,
                                new JLabel("Omologazione DM (6)"), txtEvolutionOmologazioneDm);
                addFormRow(panel, 11, new JLabel("Campo cliente (9)"), txtEvolutionCampo16,
                                new JLabel("Servizi accessori (8)"), txtEvolutionServizi);
                return panel;
        }

        private JPanel createPostaEvolutionAddressSourceSection() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(panel, 0, new JLabel("File Excel destinatari"),
                                createFilePickerField(txtEvolutionExcelPath, btnEvolutionExcelBrowse), null, null);
                addFormRow(panel, 1, new JLabel("Nota"),
                                new JLabel("Se vuoto, i dati indirizzo continuano a essere letti dal PDF."), null,
                                null);
                return panel;
        }

        private JPanel createPostaEvolutionDuSection() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(panel, 0, new JLabel("Tipo accettazione file"), txtEvolutionDuTipoAccettazioneFile,
                                new JLabel("Progressivo file"), txtEvolutionDuProgressivo);
                addFormRow(panel, 1, new JLabel("Utenza operatore"), txtEvolutionDuUtenzaOperatore,
                                boldLabel("ID prenotazione"), txtEvolutionDuIdPrenotazione);
                addFormRow(panel, 2, boldLabel("Data postalizzazione"), txtEvolutionDuDataPostalizzazione,
                                new JLabel("Frazionario centro"), txtEvolutionDuFrazionario);
                addFormRow(panel, 3, new JLabel("Tipologia prodotto"), txtEvolutionDuTipologiaProdotto,
                                new JLabel("Codice prodotto"), txtEvolutionDuCodiceProdotto);
                addFormRow(panel, 4, new JLabel("Servizio accessorio"), txtEvolutionDuServizioAccessorio,
                                new JLabel("Codice tipologia accettazione"), txtEvolutionDuCodiceTipologiaAccettazione);
                addFormRow(panel, 5, new JLabel("Tipologia tracciatura"), txtEvolutionDuTipologiaTracciatura,
                                new JLabel("Conto contrattuale"), txtEvolutionDuCodiceConto);
                addFormRow(panel, 6, new JLabel("Descrizione"), txtEvolutionDuDescrizione,
                                new JLabel("Codice omologazione"), txtEvolutionDuCodiceOmologazione);
                addFormRow(panel, 7, new JLabel("Formato"), txtEvolutionDuFormato, new JLabel("ID HU"),
                                txtEvolutionDuIdHu);
                addFormRow(panel, 8, new JLabel("ID scatola"), txtEvolutionDuIdScatola, null, null);
                return panel;
        }

        private JPanel createLabelSection() {
                JPanel container = new JPanel();
                container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
                container.setAlignmentX(Component.LEFT_ALIGNMENT);

                JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                header.setAlignmentX(Component.LEFT_ALIGNMENT);
                header.add(chkGroupLabel);
                header.add(new JLabel("ID lavorazione"));
                header.add(txtLabelId);
                header.add(Box.createHorizontalStrut(12));
                header.add(rdbLabelHorizontal);
                header.add(rdbLabelVertical);
                container.add(header);
                container.add(Box.createVerticalStrut(6));

                JPanel coords = new JPanel(new GridBagLayout());
                coords.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(coords, 0, new JLabel("X testo (mm)"), txtLabelXmm, new JLabel("X testo (pt)"), txtLabelXpt);
                addFormRow(coords, 1, new JLabel("Y testo (mm)"), txtLabelYmm, new JLabel("Y testo (pt)"), txtLabelYpt);
                container.add(coords);

                return container;
        }

        private JPanel buildCounterTab() {
                JPanel root = new JPanel();
                root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
                root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JPanel toggles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
                toggles.setAlignmentX(Component.LEFT_ALIGNMENT);
                toggles.add(chkPageCounter);
                toggles.add(rdbCounterHorizontal);
                toggles.add(rdbCounterVertical);
                root.add(toggles);
                root.add(Box.createVerticalStrut(10));

                root.add(createTitledPanel("Posizionamento", createCounterPositionSection()));
                root.add(Box.createVerticalStrut(10));
                root.add(createTitledPanel("Aspetto", createCounterAppearanceSection()));
                root.add(Box.createVerticalGlue());

                return root;
        }

        private JPanel createCounterPositionSection() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(panel, 0, new JLabel("X (mm da sinistra)"), txtCounterXmm, new JLabel("X (pt)"),
                                txtCounterXpt);
                addFormRow(panel, 1, new JLabel("Y (mm dall'alto)"), txtCounterYmm, new JLabel("Y (pt)"),
                                txtCounterYpt);
                return panel;
        }

        private JPanel createCounterAppearanceSection() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                addFormRow(panel, 0, new JLabel("Dimensione testo (pt)"), txtCounterFontSizePt, null, null);
                return panel;
        }

        private JPanel buildLogPanel() {
                JPanel panel = new JPanel(new BorderLayout(8, 8));
                JPanel top = new JPanel(new BorderLayout());
                top.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
                top.add(progressBar, BorderLayout.CENTER);
                panel.add(top, BorderLayout.NORTH);
                JScrollPane scroll = new JScrollPane(txtLog, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                panel.add(scroll, BorderLayout.CENTER);
                return panel;
        }

        private JPanel createTitledPanel(String title, JComponent content) {
                JPanel wrapper = new JPanel(new BorderLayout());
                wrapper.setBorder(BorderFactory.createTitledBorder(title));
                wrapper.add(content, BorderLayout.CENTER);
                wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
                return wrapper;
        }

        private JLabel boldLabel(String text) {
                JLabel label = new JLabel(text);
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                return label;
        }

        private void addFormRow(JPanel panel, int row, JComponent label1, JComponent field1, JComponent label2,
                        JComponent field2) {
                Insets insets = new Insets(4, 4, 4, 4);

                GridBagConstraints leftLabel = new GridBagConstraints();
                leftLabel.gridx = 0;
                leftLabel.gridy = row;
                leftLabel.insets = insets;
                leftLabel.anchor = GridBagConstraints.WEST;
                panel.add(label1, leftLabel);

                GridBagConstraints leftField = new GridBagConstraints();
                leftField.gridx = 1;
                leftField.gridy = row;
                leftField.insets = insets;
                leftField.fill = GridBagConstraints.HORIZONTAL;
                leftField.weightx = 1.0;
                if (label2 == null || field2 == null) {
                        leftField.gridwidth = 3;
                }
                panel.add(field1, leftField);

                if (label2 != null && field2 != null) {
                        GridBagConstraints rightLabel = new GridBagConstraints();
                        rightLabel.gridx = 2;
                        rightLabel.gridy = row;
                        rightLabel.insets = insets;
                        rightLabel.anchor = GridBagConstraints.WEST;
                        panel.add(label2, rightLabel);

                        GridBagConstraints rightField = new GridBagConstraints();
                        rightField.gridx = 3;
                        rightField.gridy = row;
                        rightField.insets = insets;
                        rightField.fill = GridBagConstraints.HORIZONTAL;
                        rightField.weightx = 1.0;
                        panel.add(field2, rightField);
                }
        }

        /*
         * =========================
         * === Actions / Helpers ===
         * =========================
         */

        private void runProcess() {
                String in = safe(txtInput.getText());
                String out = safe(txtOutput.getText());
                String marker = safe(txtMarker.getText());
                boolean ignoreCase = chkIgnoreCase.isSelected();
                boolean normalize = chkNormalize.isSelected();
                boolean pdfSmartMode = chkPdfSmartMode.isSelected();
                boolean rotateByText = chkRotateByText.isSelected();
                boolean rotateApplyResizeOnMatchedPages = chkResizeOnRotatedPages.isSelected();
                PdfVersion outputPdfVersion = getSelectedPdfVersion();
                boolean applyResize = chkApplyResize.isSelected();
                boolean forceA4BeforeResize = chkForceA4BeforeResize.isSelected();
                boolean applyBarcode = chkBarcode.isSelected();
                boolean risoOptimization = chkRisoOptimization.isSelected();
                String risoRecordId = safe(txtRisoRecordId.getText());

                txtLog.setText("");

                // Validazioni
                if (chkAddressKeyRead.isSelected() && (isBlank(out) || isBlank(marker))) {
                        appendLog("Lettura stringa chiave attiva: eseguo solo la creazione di lettura.xlsx.");
                        runAddressRead();
                        return;
                }
                if (isBlank(in) || isBlank(out) || isBlank(marker)) {
                        appendLog("Compila input, output e marker.");
                        return;
                }
                Path inPath = Paths.get(in);
                if (!Files.exists(inPath)) {
                        appendLog("Il file di input non esiste: " + in);
                        return;
                }
                if (in.equals(out)) {
                        appendLog("Il file di output deve essere diverso dall'input.");
                        return;
                }
                if (risoOptimization && isBlank(risoRecordId)) {
                        appendLog("Specificare il Record ID per l'ottimizzazione Riso GL9730.");
                        return;
                }
                if (rotateByText && isBlank(txtRotateByText.getText())) {
                        appendLog("Specificare la stringa da cercare per la rotazione pagina.");
                        return;
                }
                if (rotateByText && !applyResize) {
                        appendLog("Per ruotare la pagina in base alla stringa, attiva anche il resize.");
                        return;
                }

                storeActiveRacPresetFromFields();

                // --- Resize params (se abilitato)
                final float scalePct, offXpt, offYpt;
                if (applyResize) {
                        scalePct = parseFloatOrDefault(txtScalePct.getText(), 100f);
                        if (scalePct <= 0f) {
                                appendLog("La scala (%) deve essere > 0");
                                return;
                        }
                        offXpt = Imbustatrice.mm(parseFloatOrDefault(txtOffsetXmm.getText(), 0f));
                        offYpt = Imbustatrice.mm(parseFloatOrDefault(txtOffsetYmm.getText(), 0f));
                } else {
                        scalePct = 100f;
                        offXpt = 0f;
                        offYpt = 0f;
                }

                // --- Barcode/etichetta opts (etichetta disponibile anche senza barcode)
                final Imbustatrice.BarcodeOpts bcOpts;
                boolean labelEnabled = chkGroupLabel.isSelected();
                if (applyBarcode || labelEnabled) {
                        boolean barcodeUseOmr = chkBarcodeOmr.isSelected();
                        boolean allegati = chkAllegati.isSelected();
                        boolean barcodeOnAttachment = chkBarcodeOnAttachment.isSelected();
                        float xpt = Imbustatrice.mm(parseFloatOrDefault(txtBcXmm.getText(), 10f));
                        float ypt = Imbustatrice.mm(parseFloatOrDefault(txtBcYmm.getText(), 20f));
                        float wpt = Imbustatrice.BarcodeStandard.TARGET_WIDTH_PT;
                        float modulePt = parseFloatOrDefault(txtBcModulePt.getText(),
                                        Imbustatrice.BarcodeStandard.MODULE_WIDTH_PT);
                        if (applyBarcode && modulePt <= 0f) {
                                appendLog("Il modulo (pt) deve essere > 0");
                                return;
                        }
                        float hpt = parseFloatOrDefault(txtBcBarHeightPt.getText(),
                                        Imbustatrice.BarcodeStandard.BAR_HEIGHT_PT);
                        if (applyBarcode && hpt <= 0f) {
                                appendLog("L'altezza barre (pt) deve essere > 0");
                                return;
                        }
                        int startProg = (int) parseFloatOrDefault(txtBcStartProg.getText(), 1f);
                        float labelXpt = Imbustatrice.mm(parseFloatOrDefault(txtLabelXmm.getText(), 0f));
                        float labelYpt = Imbustatrice.mm(parseFloatOrDefault(txtLabelYmm.getText(), 0f));
                        boolean labelVertical = rdbLabelVertical.isSelected();
                        String lavorazioneId = safe(txtLabelId.getText());
                        float labelFontSize = Imbustatrice.BarcodeStandard.LABEL_FONT_SIZE_PT;
                        bcOpts = new Imbustatrice.BarcodeOpts(applyBarcode, barcodeUseOmr, allegati,
                                        barcodeOnAttachment, xpt, ypt,
                                        wpt, hpt,
                                        modulePt, startProg,
                                        labelEnabled, labelXpt, labelYpt, labelVertical, lavorazioneId, labelFontSize);
                } else {
                        bcOpts = null;
                }

                final Imbustatrice.RaccomandataBarcodeOpts raccomandataOpts;
                boolean racBarcodeEnabled = chkRaccomandataBarcode.isSelected();
                boolean racOmologazioneEnabled = chkRcOmologazione.isSelected();
                if (racBarcodeEnabled || racOmologazioneEnabled) {
                        float rcXmm = parseFloatOrDefault(txtRcXmm.getText(), 30f);
                        float rcYmm = parseFloatOrDefault(txtRcYmm.getText(), 225f);
                        float rcHeightMm = parseFloatOrDefault(txtRcHeightMm.getText(),
                                        Imbustatrice.RaccomandataStandard.BAR_HEIGHT_MM);
                        if (racBarcodeEnabled && rcHeightMm <= 0f) {
                                appendLog("L'altezza barre (mm) per la Raccomandata deve essere > 0");
                                return;
                        }
                        float rcModuleMm = parseFloatOrDefault(txtRcModuleMm.getText(),
                                        Imbustatrice.RaccomandataStandard.NARROW_MODULE_MM);
                        if (racBarcodeEnabled && rcModuleMm <= 0f) {
                                appendLog("Il modulo stretto (mm) per la Raccomandata deve essere > 0");
                                return;
                        }
                        float rcHumanFontPtValue = parseFloatOrDefault(txtRcHumanFontPt.getText(),
                                        Imbustatrice.RaccomandataStandard.HUMAN_READABLE_FONT_PT);
                        if (racBarcodeEnabled && rcHumanFontPtValue <= 0f) {
                                appendLog("Il font della numerazione (pt) deve essere > 0");
                                return;
                        }
                        float rcPrefixFontPtValue = parseFloatOrDefault(txtRcPrefixFontPt.getText(),
                                        Imbustatrice.RaccomandataStandard.PREFIX_FONT_PT);
                        if (racBarcodeEnabled && rcPrefixFontPtValue <= 0f) {
                                appendLog("L'altezza del prefisso (pt) deve essere > 0");
                                return;
                        }

                        float rcXpt = Imbustatrice.mm(rcXmm);
                        float rcYpt = Imbustatrice.mm(rcYmm);
                        float rcHeightPt = Imbustatrice.mm(rcHeightMm);
                        float rcModulePt = Imbustatrice.mm(rcModuleMm);
                        float rcHumanGapPt = Imbustatrice.mm(parseFloatOrDefault(txtRcHumanGapMm.getText(),
                                        Imbustatrice.RaccomandataStandard.HUMAN_READABLE_GAP_MM));
                        float rcPrefixGapPt = Imbustatrice.mm(parseFloatOrDefault(txtRcPrefixGapMm.getText(),
                                        Imbustatrice.RaccomandataStandard.PREFIX_GAP_MM));
                        String prefix = safe(txtRcPrefix.getText());
                        String startCode = safe(txtRcStartCode.getText());

                        String omologText = safe(txtRcOmologazioneText.getText());
                        if (!RC_OMOLOG_PRESET_CUSTOM.equals(currentOmologazionePresetKey)) {
                                omologText = omologazioneOptions.getOrDefault(currentOmologazionePresetKey, omologText);
                        }
                        omologText = omologText.toUpperCase(Locale.ITALIAN);
                        float omologXmm = parseFloatOrDefault(txtRcOmologazioneXmm.getText(), 20f);
                        float omologYmm = parseFloatOrDefault(txtRcOmologazioneYmm.getText(), 260f);
                        float omologFontPt = parseFloatOrDefault(txtRcOmologazioneFontPt.getText(),
                                        Imbustatrice.RaccomandataStandard.OMOLOGAZIONE_FONT_PT);
                        if (racOmologazioneEnabled && omologText.isEmpty()) {
                                appendLog("Il testo dell'omologazione postale non puo' essere vuoto");
                                return;
                        }
                        if (racOmologazioneEnabled && omologFontPt <= 0f) {
                                appendLog("La dimensione del font per l'omologazione deve essere > 0");
                                return;
                        }

                        float omologXpt = Imbustatrice.mm(omologXmm);
                        float omologYpt = Imbustatrice.mm(omologYmm);
                        boolean racCustomTextEnabled = chkRcCustomText.isSelected();
                        String racCustomText = safe(txtRcCustomText.getText());
                        float racCustomTextXmm = parseFloatOrDefault(txtRcCustomTextXmm.getText(), 20f);
                        float racCustomTextYmm = parseFloatOrDefault(txtRcCustomTextYmm.getText(), 248f);
                        float racCustomTextFontPt = parseFloatOrDefault(txtRcCustomTextFontPt.getText(),
                                        Imbustatrice.RaccomandataStandard.CUSTOM_TEXT_FONT_PT);
                        if (racCustomTextEnabled && racCustomText.isEmpty()) {
                                appendLog("Il testo personalizzato Raccomandata non puo' essere vuoto");
                                return;
                        }
                        if (racCustomTextEnabled && racCustomTextFontPt <= 0f) {
                                appendLog("La dimensione del font per il testo personalizzato Raccomandata deve essere > 0");
                                return;
                        }
                        float racCustomTextXpt = Imbustatrice.mm(racCustomTextXmm);
                        float racCustomTextYpt = Imbustatrice.mm(racCustomTextYmm);
                        try {
                                raccomandataOpts = new Imbustatrice.RaccomandataBarcodeOpts(
                                                racBarcodeEnabled,
                                                rcXpt,
                                                rcYpt,
                                                rcHeightPt,
                                                rcModulePt,
                                                Imbustatrice.RaccomandataStandard.WIDE_TO_NARROW_RATIO,
                                                rcHumanFontPtValue,
                                                rcHumanGapPt,
                                                prefix,
                                                rcPrefixGapPt,
                                                rcPrefixFontPtValue,
                                                startCode,
                                                racOmologazioneEnabled,
                                                omologText,
                                                omologXpt,
                                                omologYpt,
                                                omologFontPt,
                                                racCustomTextEnabled,
                                                racCustomText,
                                                racCustomTextXpt,
                                                racCustomTextYpt,
                                                racCustomTextFontPt);
                        } catch (IllegalArgumentException ex) {
                                appendLog("Parametri barcode Raccomandata non validi: " + ex.getMessage());
                                return;
                        }
                } else {
                        raccomandataOpts = null;
                }

                // --- Page counter opts (se abilitato)
                final Imbustatrice.PageCounterOpts pageCounterOpts;
                if (chkPageCounter.isSelected()) {
                        float counterXmm = parseFloatOrDefault(txtCounterXmm.getText(), 0f);
                        float counterYmm = parseFloatOrDefault(txtCounterYmm.getText(), 0f);
                        float counterFontPt = parseFloatOrDefault(txtCounterFontSizePt.getText(), 9f);
                        if (counterFontPt <= 0f) {
                                appendLog("La dimensione del testo del contatore deve essere > 0");
                                return;
                        }
                        float counterXpt = Imbustatrice.mm(counterXmm);
                        float counterYpt = Imbustatrice.mm(counterYmm);
                        boolean counterVertical = rdbCounterVertical.isSelected();
                        pageCounterOpts = new Imbustatrice.PageCounterOpts(true, counterXpt, counterYpt, counterFontPt,
                                        counterVertical);
                } else {
                        pageCounterOpts = null;
                }

                final Imbustatrice.QrCodeOpts qrCodeOpts;
                if (chkQrCode.isSelected()) {
                        String qrBase = safe(txtQrBase.getText());
                        if (qrBase.isEmpty()) {
                                appendLog("La base alfanumerica del QR code non puo' essere vuota");
                                return;
                        }
                        float qrSizeMm = parseFloatOrDefault(txtQrSizeMm.getText(), 0f);
                        if (qrSizeMm <= 0f) {
                                appendLog("La dimensione del QR code (mm) deve essere > 0");
                                return;
                        }
                        int qrDigits = Math.max(1, parseIntOrDefault(txtQrDigits.getText(), 1));
                        if (qrDigits > 18) {
                                appendLog("Il numero di cifre per il QR code deve essere <= 18");
                                return;
                        }
                        long qrStartVal = Math.max(0L, parseLongOrDefault(txtQrStart.getText(), 0L));
                        float qrPosXmm = parseFloatOrDefault(txtQrXmm.getText(), 0f);
                        float qrPosYmm = parseFloatOrDefault(txtQrYmm.getText(), 0f);
                        float qrSizePt = Imbustatrice.mm(qrSizeMm);
                        float qrPosXpt = Imbustatrice.mm(qrPosXmm);
                        float qrPosYpt = Imbustatrice.mm(qrPosYmm);
                        String ecLevel = Objects.toString(cmbQrErrorCorrection.getSelectedItem(), "M");
                        try {
                                qrCodeOpts = new Imbustatrice.QrCodeOpts(true, qrPosXpt, qrPosYpt, qrSizePt, qrBase,
                                                qrStartVal, qrDigits, ecLevel);
                        } catch (IllegalArgumentException ex) {
                                appendLog("Parametri QR code non validi: " + ex.getMessage());
                                return;
                        }
                } else {
                        qrCodeOpts = null;
                }

                final Imbustatrice.CorrectionOverlayOpts correctionOverlayOpts;
                final String correctionExcelPath;
                if (chkQrCorrections.isSelected()) {
                        correctionExcelPath = safe(txtQrCorrectionsExcelPath.getText());
                        if (correctionExcelPath.isEmpty()) {
                                appendLog("Seleziona il file Excel correzioni nella tab QR");
                                return;
                        }
                        if (!Files.exists(Paths.get(correctionExcelPath))) {
                                appendLog("Il file Excel correzioni non esiste: " + correctionExcelPath);
                                return;
                        }
                        float correctionXmm = parseFloatOrDefault(txtQrCorrectionsXmm.getText(), 0f);
                        float correctionYmm = parseFloatOrDefault(txtQrCorrectionsYmm.getText(), 0f);
                        float correctionWidthMm = parseFloatOrDefault(txtQrCorrectionsWidthMm.getText(), 50f);
                        float correctionFontPt = parseFloatOrDefault(txtQrCorrectionsFontPt.getText(), 4.5f);
                        float correctionIconMm = parseFloatOrDefault(txtQrCorrectionsIconMm.getText(), 5f);
                        if (correctionWidthMm <= 0f) {
                                appendLog("La larghezza area correzioni QR deve essere > 0");
                                return;
                        }
                        if (correctionFontPt <= 0f) {
                                appendLog("Il font testo correzioni QR deve essere > 0");
                                return;
                        }
                        if (correctionIconMm <= 0f) {
                                appendLog("La dimensione icona correzioni QR deve essere > 0");
                                return;
                        }
                        long correctionStartVal = Math.max(0L, parseLongOrDefault(txtQrStart.getText(), 0L));
                        correctionOverlayOpts = new Imbustatrice.CorrectionOverlayOpts(
                                        true,
                                        Imbustatrice.mm(correctionXmm),
                                        Imbustatrice.mm(correctionYmm),
                                        Imbustatrice.mm(correctionWidthMm),
                                        correctionFontPt,
                                        Imbustatrice.mm(correctionIconMm),
                                        correctionStartVal);
                } else {
                        correctionExcelPath = null;
                        correctionOverlayOpts = null;
                }

                final PdfDuplexGrouper.AddressBlockOpts addressBlockOpts = buildAddressBlockOpts(false);
                if (chkAddressBlock.isSelected() && addressBlockOpts == null) {
                        return;
                }

                final Imbustatrice.ResizePageRotationOpts resizeRotationOpts;
                if (rotateByText) {
                        String rotateSearchText = safe(txtRotateByText.getText());
                        String selectedDegrees = Objects.toString(cmbRotateByTextDegrees.getSelectedItem(), "90");
                        int clockwiseDegrees = parseIntOrDefault(selectedDegrees, 90);
                        try {
                                resizeRotationOpts = new Imbustatrice.ResizePageRotationOpts(
                                                true, rotateSearchText, clockwiseDegrees, ignoreCase, normalize,
                                                rotateApplyResizeOnMatchedPages);
                        } catch (IllegalArgumentException ex) {
                                appendLog("Parametri rotazione pagina non validi: " + ex.getMessage());
                                return;
                        }
                } else {
                        resizeRotationOpts = null;
                }

                final PdfDuplexGrouper.PostaEvolutionOpts postaEvolutionOpts;
                if (chkEvolution.isSelected()) {
                        postaEvolutionOpts = buildPostaEvolutionOpts(addressBlockOpts, true,
                                        chkEvolutionDu.isSelected());
                        if (postaEvolutionOpts == null) {
                                return;
                        }
                } else {
                        postaEvolutionOpts = null;
                }

                final RisoOptimizer.Options risoOptions = risoOptimization
                                ? new RisoOptimizer.Options(true, risoRecordId)
                                : null;
                final boolean keyedReadAfterProcess = chkAddressKeyRead.isSelected();
                final String keyedReadString = safe(txtAddressKeyString.getText());
                final Path keyedReadExcelPath = resolveKeyedReadExcelPath(in);
                if (keyedReadAfterProcess && keyedReadString.isEmpty()) {
                        appendLog("Indicare la stringa chiave per la lettura speciale.");
                        return;
                }

                setUiEnabled(false);
                progressBar.setIndeterminate(false);
                progressBar.setValue(0);
                progressBar.setString(String.format(Locale.ITALIAN, "%d%%", 0));

                AtomicBoolean done = new AtomicBoolean(false);

                SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
                        @Override
                        protected Void doInBackground() {
                                setProgress(0);
                                try {
                                        String sourcePdf = in;
                                        String resizeTmp = null;
                                        String risoTmp = null;
                                        try {
                                                if (applyResize) {
                                                        appendLog(String.format(
                                                                        "Applico resize preliminare: %.2f%%, offsetX=%.2f pt, offsetY=%.2f pt%s",
                                                                        scalePct, offXpt, offYpt,
                                                                        forceA4BeforeResize ? ", foglio=A4" : ""));
                                                        resizeTmp = tempNameFor(out, "_resize_tmp.pdf");
                                                        Imbustatrice.scaleAndTranslateContent(in, resizeTmp, scalePct,
                                                                        offXpt, offYpt, outputPdfVersion,
                                                                        resizeRotationOpts,
                                                                        App.this::appendLog,
                                                                        forceA4BeforeResize,
                                                                        this::isCancelled,
                                                                        pdfSmartMode);
                                                        sourcePdf = resizeTmp;
                                                        if (applyBarcode && rotateByText) {
                                                                appendLog("Ordine confermato: barcode imbustatrice inserito dopo la rotazione (fase resize).");
                                                        }
                                                }

                                                appendLog("Normalizzazione fronte/retro in corso...");
                                                PdfDuplexGrouper.process(sourcePdf, out, marker, ignoreCase, normalize,
                                                                outputPdfVersion, App.this::appendLog, bcOpts,
                                                                pageCounterOpts, raccomandataOpts, qrCodeOpts,
                                                                postaEvolutionOpts,
                                                                correctionExcelPath,
                                                                correctionOverlayOpts,
                                                                null,
                                                                addressBlockOpts,
                                                                this::setProgress,
                                                                this::isCancelled,
                                                                pdfSmartMode);
                                                if (isCancelled()) {
                                                        throw new CancellationException();
                                                }
                                                if (risoOptions != null && risoOptions.enabled) {
                                                        appendLog("Ottimizzazione Riso GL9730 in corso...");
                                                        risoTmp = tempNameFor(out, "_riso_tmp.pdf");
                                                        RisoOptimizer.optimize(out, risoTmp, risoOptions,
                                                                        App.this::appendLog,
                                                                        this::isCancelled);
                                                        if (isCancelled()) {
                                                                throw new CancellationException();
                                                        }
                                                        Files.move(Paths.get(risoTmp), Paths.get(out),
                                                                        StandardCopyOption.REPLACE_EXISTING);
                                                        appendLog("Ottimizzazione Riso completata.");
                                                }
                                                if (keyedReadAfterProcess) {
                                                        if (isCancelled()) {
                                                                throw new CancellationException();
                                                        }
                                                        exportKeyedRead(in, keyedReadString, keyedReadExcelPath);
                                                }
                                        } finally {
                                                deleteTemporaryFile(resizeTmp, App.this::appendLog);
                                                deleteTemporaryFile(risoTmp, App.this::appendLog);
                                        }
                                        done.set(true);
                                } catch (CancellationException ex) {
                                        appendLog("Operazione interrotta dall'utente.");
                                } catch (OutOfMemoryError ex) {
                                        handleHeapSpaceError("ERRORE");
                                } catch (Exception ex) {
                                        appendLog("ERRORE: " + formatErrorMessage(ex));
                                }
                                return null;
                        }

                        @Override
                        protected void done() {
                                progressBar.setIndeterminate(false);
                                if (done.get()) {
                                        progressBar.setValue(100);
                                        progressBar.setString("Completato");
                                } else if (isCancelled()) {
                                        progressBar.setValue(0);
                                        progressBar.setString("Interrotto");
                                }
                                setUiEnabled(true);
                                currentWorker = null;
                                requestMemoryCleanup();
                                if (done.get()) {
                                        appendLog("Operazione completata.\nOutput: " + out);
                                        autoSaveConfigurationNextTo(out);
                                        JOptionPane.showMessageDialog(App.this,
                                                        "Completato.\nOutput:\n" + out,
                                                        "Fatto", JOptionPane.INFORMATION_MESSAGE);
                                }
                        }
                };
                worker.addPropertyChangeListener(evt -> {
                        if ("progress".equals(evt.getPropertyName())) {
                                Object newVal = evt.getNewValue();
                                if (newVal instanceof Integer) {
                                        int value = (Integer) newVal;
                                        progressBar.setValue(value);
                                        if (value >= 100 && !done.get()) {
                                                progressBar.setString("Salvataggio in corso");
                                        } else {
                                                progressBar.setString(String.format(Locale.ITALIAN, "%d%%", value));
                                        }
                                }
                        }
                });
                currentWorker = worker;
                btnCancel.setEnabled(true);
                worker.execute();
        }

        private void runAddressRead() {
                showLogTab();
                String in = safe(txtInput.getText());
                if (isBlank(in)) {
                        appendLog("Seleziona un input PDF per la lettura del blocco indirizzo.");
                        return;
                }
                if (!Files.exists(Paths.get(in))) {
                        appendLog("Il file di input non esiste: " + in);
                        return;
                }
                final boolean keyedRead = chkAddressKeyRead.isSelected();
                final String keyString = safe(txtAddressKeyString.getText());
                final Path keyedReadExcelPath = resolveKeyedReadExcelPath(in);
                final PdfDuplexGrouper.AddressBlockOpts addressBlockOpts;
                if (keyedRead) {
                        if (keyString.isEmpty()) {
                                appendLog("Indicare la stringa chiave per la lettura speciale.");
                                return;
                        }
                        addressBlockOpts = null;
                } else {
                        addressBlockOpts = buildAddressBlockOpts(true);
                        if (addressBlockOpts == null) {
                                return;
                        }
                }

                setUiEnabled(false);
                progressBar.setIndeterminate(true);
                progressBar.setValue(0);
                progressBar.setString(keyedRead ? "Lettura stringa chiave..." : "Lettura blocco indirizzo...");

                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                                try {
                                        if (isCancelled()) {
                                                throw new CancellationException();
                                        }
                                        if (keyedRead) {
                                                exportKeyedRead(in, keyString, keyedReadExcelPath);
                                        } else {
                                                appendLog("Lettura blocco indirizzo (pagina 1)...");
                                                if (isCancelled()) {
                                                        throw new CancellationException();
                                                }
                                                PdfDuplexGrouper.AddressComponents address = PdfDuplexGrouper
                                                                .readAddressBlock(in, addressBlockOpts);
                                                if (isCancelled()) {
                                                        throw new CancellationException();
                                                }
                                                logAddressComponents(address);
                                        }
                                } catch (CancellationException ex) {
                                        appendLog("Lettura interrotta dall'utente.");
                                } catch (OutOfMemoryError ex) {
                                        handleHeapSpaceError("ERRORE lettura");
                                } catch (Exception ex) {
                                        appendLog("ERRORE lettura: " + formatErrorMessage(ex));
                                }
                                return null;
                        }

                        @Override
                        protected void done() {
                                progressBar.setIndeterminate(false);
                                progressBar.setValue(0);
                                progressBar.setString(isCancelled() ? "Interrotto"
                                                : String.format(Locale.ITALIAN, "%d%%", 0));
                                setUiEnabled(true);
                                currentWorker = null;
                        }
                };
                currentWorker = worker;
                btnCancel.setEnabled(true);
                worker.execute();
        }

        private void exportKeyedRead(String inputPdfPath, String keyString, Path excelPath) throws Exception {
                appendLog("Lettura stringa chiave \"" + keyString + "\" su tutte le pagine...");
                appendLog("Output XLSX lettura: " + excelPath);
                PdfDuplexGrouper.KeyedStringReadResult result = PdfDuplexGrouper
                                .exportKeyedStringsToExcel(inputPdfPath, keyString, excelPath);
                appendLog("Creato XLSX lettura: " + result.excelPath);
                appendLog("Righe estratte: " + result.rowCount);
        }

        private Path resolveKeyedReadExcelPath(String inputPdfPath) {
                String configuredOutput = safe(txtOutput.getText());
                Path basePath;
                if (!configuredOutput.isEmpty()) {
                        basePath = Paths.get(configuredOutput).toAbsolutePath().normalize();
                } else {
                        basePath = Paths.get(inputPdfPath).toAbsolutePath().normalize();
                }
                Path parent = basePath.getParent();
                return (parent == null ? Paths.get("lettura.xlsx") : parent.resolve("lettura.xlsx"))
                                .toAbsolutePath()
                                .normalize();
        }

        private void logAddressComponents(PdfDuplexGrouper.AddressComponents components) {
                if (components == null) {
                        appendLog("Nessun dato disponibile per il blocco indirizzo.");
                        return;
                }
                boolean empty = isBlank(components.rawText) && isBlank(components.nominativo)
                                && isBlank(components.indirizzo) && isBlank(components.cap)
                                && isBlank(components.comune) && isBlank(components.provincia);
                if (empty) {
                        appendLog("Nessun testo rilevato nell'area del blocco indirizzo.");
                        return;
                }
                appendLog("Risultato blocco indirizzo:");
                appendLog("  rawText   : " + nullToEmpty(components.rawText));
                appendLog("  nominativo: " + nullToEmpty(components.nominativo));
                appendLog("  indirizzo : " + nullToEmpty(components.indirizzo));
                appendLog("  CAP       : " + nullToEmpty(components.cap));
                appendLog("  comune    : " + nullToEmpty(components.comune));
                appendLog("  provincia : " + nullToEmpty(components.provincia));
        }

        private void runReticlePreview() {
                String in = safe(txtInput.getText());
                if (isBlank(in)) {
                        appendLog("Seleziona un input PDF per l'anteprima.");
                        return;
                }

                String previewOut;
                String out = safe(txtOutput.getText());
                if (isBlank(out)) {
                        previewOut = defaultPreviewName(in);
                } else {
                        previewOut = defaultPreviewName(out);
                }

                storeActiveRacPresetFromFields();

                PdfVersion outputPdfVersion = getSelectedPdfVersion();

                boolean applyResize = chkApplyResize.isSelected();
                boolean forceA4BeforeResize = chkForceA4BeforeResize.isSelected();
                float scalePct = 100f;
                float offXpt = 0f;
                float offYpt = 0f;
                if (applyResize) {
                        scalePct = parseFloatOrDefault(txtScalePct.getText(), 100f);
                        if (scalePct <= 0f) {
                                appendLog("La scala (%) deve essere > 0");
                                return;
                        }
                        offXpt = Imbustatrice.mm(parseFloatOrDefault(txtOffsetXmm.getText(), 0f));
                        offYpt = Imbustatrice.mm(parseFloatOrDefault(txtOffsetYmm.getText(), 0f));
                }

                float reticleXpt = Imbustatrice.mm(parseFloatOrDefault(txtBcXmm.getText(), 10f));
                float reticleYpt = Imbustatrice.mm(parseFloatOrDefault(txtBcYmm.getText(), 20f));
                if (!chkBarcode.isSelected() && chkRaccomandataBarcode.isSelected()) {
                        if (chkRcOmologazione.isSelected()) {
                                reticleXpt = Imbustatrice.mm(parseFloatOrDefault(txtRcOmologazioneXmm.getText(), 20f));
                                reticleYpt = Imbustatrice.mm(parseFloatOrDefault(txtRcOmologazioneYmm.getText(), 260f));
                        } else {
                                reticleXpt = Imbustatrice.mm(parseFloatOrDefault(txtRcXmm.getText(), 30f));
                                reticleYpt = Imbustatrice.mm(parseFloatOrDefault(txtRcYmm.getText(), 235f));
                        }
                } else if (!chkBarcode.isSelected() && !chkRaccomandataBarcode.isSelected() && chkQrCode.isSelected()) {
                        reticleXpt = Imbustatrice.mm(parseFloatOrDefault(txtQrXmm.getText(), 20f));
                        reticleYpt = Imbustatrice.mm(parseFloatOrDefault(txtQrYmm.getText(), 20f));
                } else if (!chkBarcode.isSelected() && !chkRaccomandataBarcode.isSelected() && !chkQrCode.isSelected()
                                && chkQrCorrections.isSelected()) {
                        reticleXpt = Imbustatrice.mm(parseFloatOrDefault(txtQrCorrectionsXmm.getText(), 20f));
                        reticleYpt = Imbustatrice.mm(parseFloatOrDefault(txtQrCorrectionsYmm.getText(), 20f));
                } else if (!chkBarcode.isSelected() && !chkRaccomandataBarcode.isSelected() && !chkQrCode.isSelected()
                                && !chkQrCorrections.isSelected()
                                && chkEvolution.isSelected()) {
                        reticleXpt = Imbustatrice.mm(parseFloatOrDefault(txtEvolutionXmm.getText(), 20f));
                        reticleYpt = Imbustatrice.mm(parseFloatOrDefault(txtEvolutionYmm.getText(), 20f));
                }

                Imbustatrice.BarcodeOpts previewOpts = null;
                boolean previewBarcodeEnabled = chkBarcode.isSelected();
                boolean previewLabelEnabled = chkGroupLabel.isSelected();
                if (previewBarcodeEnabled || previewLabelEnabled) {
                        float xmm = parseFloatOrDefault(txtBcXmm.getText(), 10f);
                        float ymm = parseFloatOrDefault(txtBcYmm.getText(), 20f);
                        float xpt = Imbustatrice.mm(xmm);
                        float ypt = Imbustatrice.mm(ymm);
                        boolean barcodeUseOmr = chkBarcodeOmr.isSelected();
                        boolean allegati = chkAllegati.isSelected();
                        boolean barcodeOnAttachment = chkBarcodeOnAttachment.isSelected();
                        int startProg = (int) parseFloatOrDefault(txtBcStartProg.getText(), 1f);
                        float modulePt = parseFloatOrDefault(txtBcModulePt.getText(),
                                        Imbustatrice.BarcodeStandard.MODULE_WIDTH_PT);
                        if (previewBarcodeEnabled && modulePt <= 0f) {
                                appendLog("Il modulo (pt) deve essere > 0 per l'anteprima.");
                                return;
                        }
                        float barHeightPt = parseFloatOrDefault(txtBcBarHeightPt.getText(),
                                        Imbustatrice.BarcodeStandard.BAR_HEIGHT_PT);
                        if (previewBarcodeEnabled && barHeightPt <= 0f) {
                                appendLog("L'altezza barre (pt) deve essere > 0 per l'anteprima.");
                                return;
                        }
                        float labelXpt = Imbustatrice.mm(parseFloatOrDefault(txtLabelXmm.getText(), 0f));
                        float labelYpt = Imbustatrice.mm(parseFloatOrDefault(txtLabelYmm.getText(), 0f));
                        boolean labelVertical = rdbLabelVertical.isSelected();
                        String lavorazioneId = safe(txtLabelId.getText());
                        previewOpts = new Imbustatrice.BarcodeOpts(
                                        previewBarcodeEnabled,
                                        barcodeUseOmr,
                                        allegati,
                                        barcodeOnAttachment,
                                        xpt,
                                        ypt,
                                        Imbustatrice.BarcodeStandard.TARGET_WIDTH_PT,
                                        barHeightPt,
                                        modulePt,
                                        startProg,
                                        previewLabelEnabled,
                                        labelXpt,
                                        labelYpt,
                                        labelVertical,
                                        lavorazioneId,
                                        Imbustatrice.BarcodeStandard.LABEL_FONT_SIZE_PT);
                }

                Imbustatrice.PageCounterOpts counterPreviewOpts = null;
                if (chkPageCounter.isSelected()) {
                        float counterXmm = parseFloatOrDefault(txtCounterXmm.getText(), 0f);
                        float counterYmm = parseFloatOrDefault(txtCounterYmm.getText(), 0f);
                        float counterFontPt = parseFloatOrDefault(txtCounterFontSizePt.getText(), 9f);
                        if (counterFontPt <= 0f) {
                                appendLog("La dimensione del testo del contatore deve essere > 0");
                                return;
                        }
                        float counterXpt = Imbustatrice.mm(counterXmm);
                        float counterYpt = Imbustatrice.mm(counterYmm);
                        boolean counterVertical = rdbCounterVertical.isSelected();
                        counterPreviewOpts = new Imbustatrice.PageCounterOpts(true, counterXpt, counterYpt,
                                        counterFontPt, counterVertical);
                }

                Imbustatrice.RaccomandataBarcodeOpts racPreviewOpts = null;
                boolean racPreviewBarcodeEnabled = chkRaccomandataBarcode.isSelected();
                boolean racPreviewOmologEnabled = chkRcOmologazione.isSelected();
                if (racPreviewBarcodeEnabled || racPreviewOmologEnabled) {
                        float rcXmm = parseFloatOrDefault(txtRcXmm.getText(), 30f);
                        float rcYmm = parseFloatOrDefault(txtRcYmm.getText(), 225f);
                        float rcHeightMm = parseFloatOrDefault(txtRcHeightMm.getText(),
                                        Imbustatrice.RaccomandataStandard.BAR_HEIGHT_MM);
                        if (racPreviewBarcodeEnabled && rcHeightMm <= 0f) {
                                appendLog("L'altezza barre (mm) per la Raccomandata deve essere > 0");
                                return;
                        }
                        float rcModuleMm = parseFloatOrDefault(txtRcModuleMm.getText(),
                                        Imbustatrice.RaccomandataStandard.NARROW_MODULE_MM);
                        if (racPreviewBarcodeEnabled && rcModuleMm <= 0f) {
                                appendLog("Il modulo stretto (mm) per la Raccomandata deve essere > 0");
                                return;
                        }
                        float rcHumanFontPtValue = parseFloatOrDefault(txtRcHumanFontPt.getText(),
                                        Imbustatrice.RaccomandataStandard.HUMAN_READABLE_FONT_PT);
                        if (racPreviewBarcodeEnabled && rcHumanFontPtValue <= 0f) {
                                appendLog("Il font della numerazione (pt) deve essere > 0");
                                return;
                        }
                        float rcPrefixFontPtValue = parseFloatOrDefault(txtRcPrefixFontPt.getText(),
                                        Imbustatrice.RaccomandataStandard.PREFIX_FONT_PT);
                        if (racPreviewBarcodeEnabled && rcPrefixFontPtValue <= 0f) {
                                appendLog("L'altezza del prefisso (pt) deve essere > 0");
                                return;
                        }

                        float rcXpt = Imbustatrice.mm(rcXmm);
                        float rcYpt = Imbustatrice.mm(rcYmm);
                        float rcHeightPt = Imbustatrice.mm(rcHeightMm);
                        float rcModulePt = Imbustatrice.mm(rcModuleMm);
                        float rcHumanGapPt = Imbustatrice.mm(parseFloatOrDefault(txtRcHumanGapMm.getText(),
                                        Imbustatrice.RaccomandataStandard.HUMAN_READABLE_GAP_MM));
                        float rcPrefixGapPt = Imbustatrice.mm(parseFloatOrDefault(txtRcPrefixGapMm.getText(),
                                        Imbustatrice.RaccomandataStandard.PREFIX_GAP_MM));
                        String prefix = safe(txtRcPrefix.getText());
                        String startCode = safe(txtRcStartCode.getText());

                        String omologText = safe(txtRcOmologazioneText.getText());
                        if (!RC_OMOLOG_PRESET_CUSTOM.equals(currentOmologazionePresetKey)) {
                                omologText = omologazioneOptions.getOrDefault(currentOmologazionePresetKey, omologText);
                        }
                        omologText = omologText.toUpperCase(Locale.ITALIAN);
                        float omologXmm = parseFloatOrDefault(txtRcOmologazioneXmm.getText(), 20f);
                        float omologYmm = parseFloatOrDefault(txtRcOmologazioneYmm.getText(), 260f);
                        float omologFontPt = parseFloatOrDefault(txtRcOmologazioneFontPt.getText(),
                                        Imbustatrice.RaccomandataStandard.OMOLOGAZIONE_FONT_PT);
                        if (racPreviewOmologEnabled && omologText.isEmpty()) {
                                appendLog("Il testo dell'omologazione postale non puo' essere vuoto");
                                return;
                        }
                        if (racPreviewOmologEnabled && omologFontPt <= 0f) {
                                appendLog("La dimensione del font per l'omologazione deve essere > 0");
                                return;
                        }

                        float omologXpt = Imbustatrice.mm(omologXmm);
                        float omologYpt = Imbustatrice.mm(omologYmm);
                        boolean racCustomTextEnabled = chkRcCustomText.isSelected();
                        String racCustomText = safe(txtRcCustomText.getText());
                        float racCustomTextXmm = parseFloatOrDefault(txtRcCustomTextXmm.getText(), 20f);
                        float racCustomTextYmm = parseFloatOrDefault(txtRcCustomTextYmm.getText(), 248f);
                        float racCustomTextFontPt = parseFloatOrDefault(txtRcCustomTextFontPt.getText(),
                                        Imbustatrice.RaccomandataStandard.CUSTOM_TEXT_FONT_PT);
                        if (racCustomTextEnabled && racCustomText.isEmpty()) {
                                appendLog("Il testo personalizzato Raccomandata non puo' essere vuoto");
                                return;
                        }
                        if (racCustomTextEnabled && racCustomTextFontPt <= 0f) {
                                appendLog("La dimensione del font per il testo personalizzato Raccomandata deve essere > 0");
                                return;
                        }
                        float racCustomTextXpt = Imbustatrice.mm(racCustomTextXmm);
                        float racCustomTextYpt = Imbustatrice.mm(racCustomTextYmm);

                        try {
                                racPreviewOpts = new Imbustatrice.RaccomandataBarcodeOpts(
                                                racPreviewBarcodeEnabled,
                                                rcXpt,
                                                rcYpt,
                                                rcHeightPt,
                                                rcModulePt,
                                                Imbustatrice.RaccomandataStandard.WIDE_TO_NARROW_RATIO,
                                                rcHumanFontPtValue,
                                                rcHumanGapPt,
                                                prefix,
                                                rcPrefixGapPt,
                                                rcPrefixFontPtValue,
                                                startCode,
                                                racPreviewOmologEnabled,
                                                omologText,
                                                omologXpt,
                                                omologYpt,
                                                omologFontPt,
                                                racCustomTextEnabled,
                                                racCustomText,
                                                racCustomTextXpt,
                                                racCustomTextYpt,
                                                racCustomTextFontPt);
                        } catch (IllegalArgumentException ex) {
                                appendLog("Parametri barcode Raccomandata non validi: " + ex.getMessage());
                                return;
                        }
                }

                Imbustatrice.QrCodeOpts qrPreviewOpts = null;
                if (chkQrCode.isSelected()) {
                        String qrBase = safe(txtQrBase.getText());
                        if (qrBase.isEmpty()) {
                                appendLog("La base alfanumerica del QR code non puo' essere vuota");
                                return;
                        }
                        float qrSizeMm = parseFloatOrDefault(txtQrSizeMm.getText(), 0f);
                        if (qrSizeMm <= 0f) {
                                appendLog("La dimensione del QR code (mm) deve essere > 0");
                                return;
                        }
                        int qrDigits = Math.max(1, parseIntOrDefault(txtQrDigits.getText(), 1));
                        if (qrDigits > 18) {
                                appendLog("Il numero di cifre per il QR code deve essere <= 18");
                                return;
                        }
                        long qrStartVal = Math.max(0L, parseLongOrDefault(txtQrStart.getText(), 0L));
                        float qrPosXmm = parseFloatOrDefault(txtQrXmm.getText(), 0f);
                        float qrPosYmm = parseFloatOrDefault(txtQrYmm.getText(), 0f);
                        float qrSizePt = Imbustatrice.mm(qrSizeMm);
                        float qrPosXpt = Imbustatrice.mm(qrPosXmm);
                        float qrPosYpt = Imbustatrice.mm(qrPosYmm);
                        String ecLevel = Objects.toString(cmbQrErrorCorrection.getSelectedItem(), "M");
                        try {
                                qrPreviewOpts = new Imbustatrice.QrCodeOpts(true, qrPosXpt, qrPosYpt, qrSizePt, qrBase,
                                                qrStartVal, qrDigits, ecLevel);
                        } catch (IllegalArgumentException ex) {
                                appendLog("Parametri QR code non validi: " + ex.getMessage());
                                return;
                        }
                }

                Imbustatrice.PostaEvolutionDataMatrixOpts evolutionPreviewOpts = null;
                String evolutionPreviewPayload = null;
                if (chkEvolution.isSelected()) {
                        PdfDuplexGrouper.PostaEvolutionOpts previewEvolution = buildPostaEvolutionOpts(
                                        buildAddressBlockOpts(true), false, false);
                        if (previewEvolution == null) {
                                return;
                        }
                        evolutionPreviewOpts = previewEvolution.dataMatrixOpts;
                        evolutionPreviewPayload = PdfDuplexGrouper.buildPostaEvolutionDataMatrixValue(
                                        previewEvolution, 1, PdfDuplexGrouper.AddressComponents.empty());
                }

                float gridStepMm = 10f; // reticolo ogni 10 mm (puoi parametrizzarlo)

                String previewSource = in;
                String firstPageTmp = null;
                String resizeTmp = null;
                try {
                        if (applyResize) {
                                appendLog(String.format(
                                                "Applico resize preliminare all'anteprima: %.2f%%, offsetX=%.2f pt, offsetY=%.2f pt%s",
                                                scalePct, offXpt, offYpt,
                                                forceA4BeforeResize ? ", foglio=A4" : ""));
                                firstPageTmp = tempNameFor(previewOut, "_first_tmp.pdf");
                                Imbustatrice.copyFirstPage(previewSource, firstPageTmp, outputPdfVersion);
                                resizeTmp = tempNameFor(previewOut, "_resize_tmp.pdf");
                                Imbustatrice.scaleAndTranslateContent(firstPageTmp, resizeTmp, scalePct, offXpt,
                                                offYpt, outputPdfVersion, forceA4BeforeResize);
                                previewSource = resizeTmp;
                        }

                        appendLog("Genero anteprima reticolo (prima pagina)...");
                        Imbustatrice.createReticlePreview(previewSource, previewOut,
                                        reticleXpt,
                                        reticleYpt,
                                        false, gridStepMm, previewOpts, counterPreviewOpts, racPreviewOpts,
                                        qrPreviewOpts, evolutionPreviewPayload, evolutionPreviewOpts,
                                        outputPdfVersion);
                        appendLog("Anteprima: " + previewOut);
                        autoSaveConfigurationNextTo(previewOut);
                        JOptionPane.showMessageDialog(this, "Anteprima creata:\n" + previewOut);
                } catch (OutOfMemoryError ex) {
                        handleHeapSpaceError("ERRORE anteprima");
                } catch (Exception ex) {
                        appendLog("ERRORE anteprima: " + formatErrorMessage(ex));
                } finally {
                        deleteTemporaryFile(firstPageTmp, this::appendLog);
                        deleteTemporaryFile(resizeTmp, this::appendLog);
                }
        }

        private void exportConfiguration() {
                storeActiveRacPresetFromFields();
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter("Configurazione XML (*.xml)", "xml"));
                String outPath = safe(txtOutput.getText());
                if (!isBlank(outPath)) {
                        File baseFile = new File(outPath);
                        File parent = baseFile.getParentFile();
                        if (parent != null && parent.isDirectory()) {
                                fc.setCurrentDirectory(parent);
                        }
                        String baseName = baseFile.getName();
                        if (baseName.toLowerCase(Locale.ITALIAN).endsWith(".pdf")) {
                                baseName = baseName.substring(0, baseName.length() - 4);
                        }
                        File suggestion = parent != null ? new File(parent, baseName + "_config.xml")
                                        : new File(baseName + "_config.xml");
                        fc.setSelectedFile(suggestion);
                }
                int r = fc.showSaveDialog(this);
                if (r == JFileChooser.APPROVE_OPTION) {
                        File selected = fc.getSelectedFile();
                        if (selected == null) {
                                return;
                        }
                        File chosen = ensureXmlExtension(selected);
                        if (chosen == null) {
                                return;
                        }
                        try {
                                WorkConfiguration cfg = snapshotConfiguration();
                                cfg.save(chosen.toPath());
                                appendLog("Configurazione salvata: " + chosen.getAbsolutePath());
                                JOptionPane.showMessageDialog(this,
                                                "Configurazione salvata:\n" + chosen.getAbsolutePath());
                        } catch (Exception ex) {
                                appendLog("ERRORE salvataggio configurazione: " + ex.getMessage());
                                JOptionPane.showMessageDialog(this,
                                                "Errore durante il salvataggio:\n" + ex.getMessage(),
                                                "Errore",
                                                JOptionPane.ERROR_MESSAGE);
                        }
                }
        }

        private void importConfiguration() {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter("Configurazione XML (*.xml)", "xml"));
                int r = fc.showOpenDialog(this);
                if (r == JFileChooser.APPROVE_OPTION) {
                        File chosen = fc.getSelectedFile();
                        if (chosen == null) {
                                return;
                        }
                        try {
                                WorkConfiguration cfg = WorkConfiguration.load(chosen.toPath());
                                applyConfiguration(cfg);
                                appendLog("Configurazione caricata: " + chosen.getAbsolutePath());
                                JOptionPane.showMessageDialog(this,
                                                "Configurazione caricata:\n" + chosen.getAbsolutePath());
                        } catch (Exception ex) {
                                appendLog("ERRORE caricamento configurazione: " + ex.getMessage());
                                JOptionPane.showMessageDialog(this,
                                                "Errore durante il caricamento:\n" + ex.getMessage(),
                                                "Errore",
                                                JOptionPane.ERROR_MESSAGE);
                        }
                }
        }

        private WorkConfiguration snapshotConfiguration() {
                storeActiveRacPresetFromFields();
                WorkConfiguration cfg = new WorkConfiguration();
                cfg.inputPath = safe(txtInput.getText());
                cfg.outputPath = safe(txtOutput.getText());
                cfg.marker = safe(txtMarker.getText());
                cfg.ignoreCase = chkIgnoreCase.isSelected();
                cfg.normalize = chkNormalize.isSelected();
                cfg.pdfSmartMode = chkPdfSmartMode.isSelected();
                cfg.rotateByTextEnabled = chkRotateByText.isSelected();
                cfg.rotateApplyResizeOnMatchedPages = chkResizeOnRotatedPages.isSelected();
                cfg.rotateByTextString = safe(txtRotateByText.getText());
                cfg.rotateByTextDegrees = Objects.toString(cmbRotateByTextDegrees.getSelectedItem(), "90");
                cfg.pdfVersionKey = getSelectedPdfVersionChoice().name();

                cfg.resizeEnabled = chkApplyResize.isSelected();
                cfg.resizeForceA4BeforeResize = chkForceA4BeforeResize.isSelected();
                cfg.scalePct = safe(txtScalePct.getText());
                cfg.offsetXmm = safe(txtOffsetXmm.getText());
                cfg.offsetYmm = safe(txtOffsetYmm.getText());

                cfg.barcodeEnabled = chkBarcode.isSelected();
                cfg.barcodeUseOmr = chkBarcodeOmr.isSelected();
                cfg.allegatiPresenti = chkAllegati.isSelected();
                cfg.barcodeOnAttachment = chkBarcodeOnAttachment.isSelected();
                cfg.bcXmm = safe(txtBcXmm.getText());
                cfg.bcYmm = safe(txtBcYmm.getText());
                cfg.bcModulePt = safe(txtBcModulePt.getText());
                cfg.bcBarHeightPt = safe(txtBcBarHeightPt.getText());
                cfg.bcStartProg = safe(txtBcStartProg.getText());
                cfg.groupLabelEnabled = chkGroupLabel.isSelected();
                cfg.labelId = safe(txtLabelId.getText());
                cfg.labelXmm = safe(txtLabelXmm.getText());
                cfg.labelYmm = safe(txtLabelYmm.getText());
                cfg.labelVertical = rdbLabelVertical.isSelected();

                cfg.racBarcodeEnabled = chkRaccomandataBarcode.isSelected();
                cfg.racOmologazioneEnabled = chkRcOmologazione.isSelected();
                cfg.racPresetKey = currentRacPresetKey == null ? RAC_PRESET_CUSTOM : currentRacPresetKey;
                cfg.racXmm = safe(txtRcXmm.getText());
                cfg.racYmm = safe(txtRcYmm.getText());
                cfg.racHeightMm = safe(txtRcHeightMm.getText());
                cfg.racModuleMm = safe(txtRcModuleMm.getText());
                cfg.racHumanFontPt = safe(txtRcHumanFontPt.getText());
                cfg.racHumanGapMm = safe(txtRcHumanGapMm.getText());
                cfg.racPrefix = safe(txtRcPrefix.getText());
                cfg.racPrefixGapMm = safe(txtRcPrefixGapMm.getText());
                cfg.racPrefixFontPt = safe(txtRcPrefixFontPt.getText());
                cfg.racStartCode = safe(txtRcStartCode.getText());
                cfg.omologazionePresetKey = currentOmologazionePresetKey == null
                                ? RC_OMOLOG_PRESET_CUSTOM
                                : currentOmologazionePresetKey;
                cfg.omologazioneText = safe(txtRcOmologazioneText.getText());
                cfg.omologazioneXmm = safe(txtRcOmologazioneXmm.getText());
                cfg.omologazioneYmm = safe(txtRcOmologazioneYmm.getText());
                cfg.omologazioneFontPt = safe(txtRcOmologazioneFontPt.getText());
                cfg.racCustomTextEnabled = chkRcCustomText.isSelected();
                cfg.racCustomText = safe(txtRcCustomText.getText());
                cfg.racCustomTextXmm = safe(txtRcCustomTextXmm.getText());
                cfg.racCustomTextYmm = safe(txtRcCustomTextYmm.getText());
                cfg.racCustomTextFontPt = safe(txtRcCustomTextFontPt.getText());

                cfg.pageCounterEnabled = chkPageCounter.isSelected();
                cfg.pageCounterVertical = rdbCounterVertical.isSelected();
                cfg.pageCounterXmm = safe(txtCounterXmm.getText());
                cfg.pageCounterYmm = safe(txtCounterYmm.getText());
                cfg.pageCounterFontPt = safe(txtCounterFontSizePt.getText());

                cfg.qrEnabled = chkQrCode.isSelected();
                cfg.qrBase = safe(txtQrBase.getText());
                cfg.qrDigits = safe(txtQrDigits.getText());
                cfg.qrStart = safe(txtQrStart.getText());
                cfg.qrSizeMm = safe(txtQrSizeMm.getText());
                cfg.qrXmm = safe(txtQrXmm.getText());
                cfg.qrYmm = safe(txtQrYmm.getText());
                cfg.qrErrorCorrection = Objects.toString(cmbQrErrorCorrection.getSelectedItem(), "M");
                cfg.qrCorrectionsEnabled = chkQrCorrections.isSelected();
                cfg.qrCorrectionsExcelPath = safe(txtQrCorrectionsExcelPath.getText());
                cfg.qrCorrectionsXmm = safe(txtQrCorrectionsXmm.getText());
                cfg.qrCorrectionsYmm = safe(txtQrCorrectionsYmm.getText());
                cfg.qrCorrectionsWidthMm = safe(txtQrCorrectionsWidthMm.getText());
                cfg.qrCorrectionsFontPt = safe(txtQrCorrectionsFontPt.getText());
                cfg.qrCorrectionsIconMm = safe(txtQrCorrectionsIconMm.getText());

                cfg.addressBlockEnabled = chkAddressBlock.isSelected();
                cfg.addressXmm = safe(txtAddressXmm.getText());
                cfg.addressYmm = safe(txtAddressYmm.getText());
                cfg.addressWidthMm = safe(txtAddressWidthMm.getText());
                cfg.addressHeightMm = safe(txtAddressHeightMm.getText());
                cfg.addressKeyReadEnabled = chkAddressKeyRead.isSelected();
                cfg.addressKeyString = safe(txtAddressKeyString.getText());

                cfg.risoOptimizationEnabled = chkRisoOptimization.isSelected();
                cfg.risoRecordId = safe(txtRisoRecordId.getText());

                cfg.evolutionEnabled = chkEvolution.isSelected();
                cfg.evolutionDataMatrixXmm = safe(txtEvolutionXmm.getText());
                cfg.evolutionDataMatrixYmm = safe(txtEvolutionYmm.getText());
                cfg.evolutionDataMatrixModuleMm = safe(txtEvolutionModuleMm.getText());
                cfg.evolutionDataMatrixWidthCells = safe(txtEvolutionWidthCells.getText());
                cfg.evolutionDataMatrixHeightCells = safe(txtEvolutionHeightCells.getText());
                cfg.evolutionGamma = safe(txtEvolutionGamma.getText());
                cfg.evolutionSapId = safe(txtEvolutionSapId.getText());
                cfg.evolutionClientId = safe(txtEvolutionClientId.getText());
                cfg.evolutionClasse = safe(txtEvolutionClasse.getText());
                cfg.evolutionTipoProdotto = safe(txtEvolutionTipoProdotto.getText());
                cfg.evolutionCapDestFallback = safe(txtEvolutionCapDestFallback.getText());
                cfg.evolutionCodTecDest = safe(txtEvolutionCodTecDest.getText());
                cfg.evolutionCapMitt = safe(txtEvolutionCapMitt.getText());
                cfg.evolutionCodTecMitt = safe(txtEvolutionCodTecMitt.getText());
                cfg.evolutionPrenFiglio = safe(txtEvolutionPrenFiglio.getText());
                cfg.evolutionStampatore = safe(txtEvolutionStampatore.getText());
                cfg.evolutionStartOggetto = safe(txtEvolutionStartOggetto.getText());
                cfg.evolutionCausale = safe(txtEvolutionCausale.getText());
                cfg.evolutionOmologazioneDm = safe(txtEvolutionOmologazioneDm.getText());
                cfg.evolutionCampo16 = safe(txtEvolutionCampo16.getText());
                cfg.evolutionServizi = safe(txtEvolutionServizi.getText());
                cfg.evolutionExcelPath = safe(txtEvolutionExcelPath.getText());
                cfg.evolutionDuEnabled = chkEvolutionDu.isSelected();
                cfg.evolutionDuTipoAccettazioneFile = safe(txtEvolutionDuTipoAccettazioneFile.getText());
                cfg.evolutionDuProgressivo = safe(txtEvolutionDuProgressivo.getText());
                cfg.evolutionDuUtenzaOperatore = safe(txtEvolutionDuUtenzaOperatore.getText());
                cfg.evolutionDuIdPrenotazione = safe(txtEvolutionDuIdPrenotazione.getText());
                cfg.evolutionDuDataPostalizzazione = safe(txtEvolutionDuDataPostalizzazione.getText());
                cfg.evolutionDuFrazionario = safe(txtEvolutionDuFrazionario.getText());
                cfg.evolutionDuTipologiaProdotto = safe(txtEvolutionDuTipologiaProdotto.getText());
                cfg.evolutionDuCodiceProdotto = safe(txtEvolutionDuCodiceProdotto.getText());
                cfg.evolutionDuServizioAccessorio = safe(txtEvolutionDuServizioAccessorio.getText());
                cfg.evolutionDuCodiceTipologiaAccettazione = safe(txtEvolutionDuCodiceTipologiaAccettazione.getText());
                cfg.evolutionDuTipologiaTracciatura = safe(txtEvolutionDuTipologiaTracciatura.getText());
                cfg.evolutionDuCodiceConto = safe(txtEvolutionDuCodiceConto.getText());
                cfg.evolutionDuDescrizione = safe(txtEvolutionDuDescrizione.getText());
                cfg.evolutionDuCodiceOmologazione = safe(txtEvolutionDuCodiceOmologazione.getText());
                cfg.evolutionDuFormato = safe(txtEvolutionDuFormato.getText());
                cfg.evolutionDuIdHu = safe(txtEvolutionDuIdHu.getText());
                cfg.evolutionDuIdScatola = safe(txtEvolutionDuIdScatola.getText());

                return cfg;
        }

        private void applyConfiguration(WorkConfiguration cfg) {
                if (cfg == null) {
                        return;
                }
                if (!WorkConfiguration.VERSION.equals(cfg.version)) {
                        appendLog("Attenzione: versione configurazione non riconosciuta (" + cfg.version + ")");
                }

                txtInput.setText(nullToEmpty(cfg.inputPath));
                txtOutput.setText(nullToEmpty(cfg.outputPath));
                txtMarker.setText(nullToEmpty(cfg.marker));
                chkIgnoreCase.setSelected(cfg.ignoreCase);
                chkNormalize.setSelected(cfg.normalize);
                chkPdfSmartMode.setSelected(cfg.pdfSmartMode);
                chkRotateByText.setSelected(cfg.rotateByTextEnabled);
                chkResizeOnRotatedPages.setSelected(cfg.rotateApplyResizeOnMatchedPages);
                txtRotateByText.setText(nullToEmpty(cfg.rotateByTextString));
                cmbRotateByTextDegrees.setSelectedItem(nullToEmpty(cfg.rotateByTextDegrees));
                if (cmbRotateByTextDegrees.getSelectedItem() == null) {
                        cmbRotateByTextDegrees.setSelectedItem("90");
                }
                cmbPdfVersion.setSelectedItem(PdfVersionChoice.fromKey(cfg.pdfVersionKey));

                chkApplyResize.setSelected(cfg.resizeEnabled);
                chkForceA4BeforeResize.setSelected(cfg.resizeForceA4BeforeResize);
                txtScalePct.setText(nullToEmpty(cfg.scalePct));
                txtOffsetXmm.setText(nullToEmpty(cfg.offsetXmm));
                txtOffsetYmm.setText(nullToEmpty(cfg.offsetYmm));

                chkBarcode.setSelected(cfg.barcodeEnabled);
                chkBarcodeOmr.setSelected(cfg.barcodeUseOmr);
                chkAllegati.setSelected(cfg.allegatiPresenti);
                chkBarcodeOnAttachment.setSelected(cfg.barcodeOnAttachment);
                txtBcXmm.setText(nullToEmpty(cfg.bcXmm));
                txtBcYmm.setText(nullToEmpty(cfg.bcYmm));
                txtBcModulePt.setText(nullToEmpty(cfg.bcModulePt));
                txtBcBarHeightPt.setText(nullToEmpty(cfg.bcBarHeightPt));
                txtBcStartProg.setText(nullToEmpty(cfg.bcStartProg));
                chkGroupLabel.setSelected(cfg.groupLabelEnabled);
                txtLabelId.setText(nullToEmpty(cfg.labelId));
                txtLabelXmm.setText(nullToEmpty(cfg.labelXmm));
                txtLabelYmm.setText(nullToEmpty(cfg.labelYmm));
                if (cfg.labelVertical) {
                        rdbLabelVertical.setSelected(true);
                } else {
                        rdbLabelHorizontal.setSelected(true);
                }

                chkRaccomandataBarcode.setSelected(cfg.racBarcodeEnabled);
                String racKey = cfg.racPresetKey;
                if (racKey == null || !racPresets.containsKey(racKey)) {
                        racKey = RAC_PRESET_CUSTOM;
                }
                currentRacPresetKey = racKey;
                if (cmbRacPreset != null) {
                        boolean previous = updatingRacFields;
                        updatingRacFields = true;
                        try {
                                cmbRacPreset.setSelectedItem(currentRacPresetKey);
                        } finally {
                                updatingRacFields = previous;
                        }
                }
                updatingRacFields = true;
                try {
                        txtRcXmm.setText(nullToEmpty(cfg.racXmm));
                        txtRcYmm.setText(nullToEmpty(cfg.racYmm));
                        txtRcHeightMm.setText(nullToEmpty(cfg.racHeightMm));
                        txtRcModuleMm.setText(nullToEmpty(cfg.racModuleMm));
                        txtRcHumanFontPt.setText(nullToEmpty(cfg.racHumanFontPt));
                        txtRcHumanGapMm.setText(nullToEmpty(cfg.racHumanGapMm));
                        txtRcPrefix.setText(nullToEmpty(cfg.racPrefix));
                        txtRcPrefixGapMm.setText(nullToEmpty(cfg.racPrefixGapMm));
                        txtRcPrefixFontPt.setText(nullToEmpty(cfg.racPrefixFontPt));
                        txtRcStartCode.setText(nullToEmpty(cfg.racStartCode));
                } finally {
                        updatingRacFields = false;
                }

                chkRcOmologazione.setSelected(cfg.racOmologazioneEnabled);
                String omoKey = cfg.omologazionePresetKey;
                if (omoKey == null || !omologazioneOptions.containsKey(omoKey)) {
                        omoKey = RC_OMOLOG_PRESET_CUSTOM;
                }
                currentOmologazionePresetKey = omoKey;
                if (cmbRcOmologazionePreset != null) {
                        boolean prev = updatingOmologazioneFields;
                        updatingOmologazioneFields = true;
                        try {
                                cmbRcOmologazionePreset.setSelectedItem(currentOmologazionePresetKey);
                        } finally {
                                updatingOmologazioneFields = prev;
                        }
                }
                updatingOmologazioneFields = true;
                try {
                        txtRcOmologazioneText.setText(nullToEmpty(cfg.omologazioneText));
                        txtRcOmologazioneXmm.setText(nullToEmpty(cfg.omologazioneXmm));
                        txtRcOmologazioneYmm.setText(nullToEmpty(cfg.omologazioneYmm));
                        txtRcOmologazioneFontPt.setText(nullToEmpty(cfg.omologazioneFontPt));
                } finally {
                        updatingOmologazioneFields = false;
                }
                chkRcCustomText.setSelected(cfg.racCustomTextEnabled);
                txtRcCustomText.setText(nullToEmpty(cfg.racCustomText));
                txtRcCustomTextXmm.setText(nullToEmpty(cfg.racCustomTextXmm));
                txtRcCustomTextYmm.setText(nullToEmpty(cfg.racCustomTextYmm));
                txtRcCustomTextFontPt.setText(nullToEmpty(cfg.racCustomTextFontPt));

                chkPageCounter.setSelected(cfg.pageCounterEnabled);
                txtCounterXmm.setText(nullToEmpty(cfg.pageCounterXmm));
                txtCounterYmm.setText(nullToEmpty(cfg.pageCounterYmm));
                txtCounterFontSizePt.setText(nullToEmpty(cfg.pageCounterFontPt));
                if (cfg.pageCounterVertical) {
                        rdbCounterVertical.setSelected(true);
                } else {
                        rdbCounterHorizontal.setSelected(true);
                }

                chkQrCode.setSelected(cfg.qrEnabled);
                txtQrBase.setText(nullToEmpty(cfg.qrBase));
                txtQrDigits.setText(nullToEmpty(cfg.qrDigits));
                txtQrStart.setText(nullToEmpty(cfg.qrStart));
                txtQrSizeMm.setText(nullToEmpty(cfg.qrSizeMm));
                txtQrXmm.setText(nullToEmpty(cfg.qrXmm));
                txtQrYmm.setText(nullToEmpty(cfg.qrYmm));
                String ecLevel = cfg.qrErrorCorrection == null ? "M" : cfg.qrErrorCorrection;
                cmbQrErrorCorrection.setSelectedItem(ecLevel);
                chkQrCorrections.setSelected(cfg.qrCorrectionsEnabled);
                txtQrCorrectionsExcelPath.setText(nullToEmpty(cfg.qrCorrectionsExcelPath));
                txtQrCorrectionsXmm.setText(nullToEmpty(cfg.qrCorrectionsXmm));
                txtQrCorrectionsYmm.setText(nullToEmpty(cfg.qrCorrectionsYmm));
                txtQrCorrectionsWidthMm.setText(nullToEmpty(cfg.qrCorrectionsWidthMm));
                txtQrCorrectionsFontPt.setText(nullToEmpty(cfg.qrCorrectionsFontPt));
                txtQrCorrectionsIconMm.setText(nullToEmpty(cfg.qrCorrectionsIconMm));

                chkAddressBlock.setSelected(cfg.addressBlockEnabled);
                txtAddressXmm.setText(nullToEmpty(cfg.addressXmm));
                txtAddressYmm.setText(nullToEmpty(cfg.addressYmm));
                txtAddressWidthMm.setText(nullToEmpty(cfg.addressWidthMm));
                txtAddressHeightMm.setText(nullToEmpty(cfg.addressHeightMm));
                chkAddressKeyRead.setSelected(cfg.addressKeyReadEnabled);
                txtAddressKeyString.setText(nullToEmpty(cfg.addressKeyString));

                chkRisoOptimization.setSelected(cfg.risoOptimizationEnabled);
                txtRisoRecordId.setText(nullToEmpty(cfg.risoRecordId));

                chkEvolution.setSelected(cfg.evolutionEnabled);
                txtEvolutionXmm.setText(nullToEmpty(cfg.evolutionDataMatrixXmm));
                txtEvolutionYmm.setText(nullToEmpty(cfg.evolutionDataMatrixYmm));
                txtEvolutionModuleMm.setText(nullToEmpty(cfg.evolutionDataMatrixModuleMm));
                txtEvolutionWidthCells.setText(nullToEmpty(cfg.evolutionDataMatrixWidthCells));
                txtEvolutionHeightCells.setText(nullToEmpty(cfg.evolutionDataMatrixHeightCells));
                txtEvolutionGamma.setText(nullToEmpty(cfg.evolutionGamma));
                txtEvolutionSapId.setText(nullToEmpty(cfg.evolutionSapId));
                txtEvolutionClientId.setText(nullToEmpty(cfg.evolutionClientId));
                txtEvolutionClasse.setText(nullToEmpty(cfg.evolutionClasse));
                txtEvolutionTipoProdotto.setText(nullToEmpty(cfg.evolutionTipoProdotto));
                txtEvolutionCapDestFallback.setText(nullToEmpty(cfg.evolutionCapDestFallback));
                txtEvolutionCodTecDest.setText(nullToEmpty(cfg.evolutionCodTecDest));
                txtEvolutionCapMitt.setText(nullToEmpty(cfg.evolutionCapMitt));
                txtEvolutionCodTecMitt.setText(nullToEmpty(cfg.evolutionCodTecMitt));
                txtEvolutionPrenFiglio.setText(nullToEmpty(cfg.evolutionPrenFiglio));
                txtEvolutionStampatore.setText(nullToEmpty(cfg.evolutionStampatore));
                txtEvolutionStartOggetto.setText(nullToEmpty(cfg.evolutionStartOggetto));
                txtEvolutionCausale.setText(nullToEmpty(cfg.evolutionCausale));
                txtEvolutionOmologazioneDm.setText(nullToEmpty(cfg.evolutionOmologazioneDm));
                txtEvolutionCampo16.setText(nullToEmpty(cfg.evolutionCampo16));
                txtEvolutionServizi.setText(nullToEmpty(cfg.evolutionServizi));
                txtEvolutionExcelPath.setText(nullToEmpty(cfg.evolutionExcelPath));
                chkEvolutionDu.setSelected(cfg.evolutionDuEnabled);
                txtEvolutionDuTipoAccettazioneFile.setText(nullToEmpty(cfg.evolutionDuTipoAccettazioneFile));
                txtEvolutionDuProgressivo.setText(nullToEmpty(cfg.evolutionDuProgressivo));
                txtEvolutionDuUtenzaOperatore.setText(nullToEmpty(cfg.evolutionDuUtenzaOperatore));
                txtEvolutionDuIdPrenotazione.setText(nullToEmpty(cfg.evolutionDuIdPrenotazione));
                txtEvolutionDuDataPostalizzazione.setText(nullToEmpty(cfg.evolutionDuDataPostalizzazione));
                txtEvolutionDuFrazionario.setText(nullToEmpty(cfg.evolutionDuFrazionario));
                txtEvolutionDuTipologiaProdotto.setText(nullToEmpty(cfg.evolutionDuTipologiaProdotto));
                txtEvolutionDuCodiceProdotto.setText(nullToEmpty(cfg.evolutionDuCodiceProdotto));
                txtEvolutionDuServizioAccessorio.setText(nullToEmpty(cfg.evolutionDuServizioAccessorio));
                txtEvolutionDuCodiceTipologiaAccettazione
                                .setText(nullToEmpty(cfg.evolutionDuCodiceTipologiaAccettazione));
                txtEvolutionDuTipologiaTracciatura.setText(nullToEmpty(cfg.evolutionDuTipologiaTracciatura));
                txtEvolutionDuCodiceConto.setText(nullToEmpty(cfg.evolutionDuCodiceConto));
                txtEvolutionDuDescrizione.setText(nullToEmpty(cfg.evolutionDuDescrizione));
                txtEvolutionDuCodiceOmologazione.setText(nullToEmpty(cfg.evolutionDuCodiceOmologazione));
                txtEvolutionDuFormato.setText(nullToEmpty(cfg.evolutionDuFormato));
                txtEvolutionDuIdHu.setText(nullToEmpty(cfg.evolutionDuIdHu));
                txtEvolutionDuIdScatola.setText(nullToEmpty(cfg.evolutionDuIdScatola));

                updateBarcodeDerivedFields();
                updateRaccomandataDerivedFields();
                updatePageCounterDerivedFields();
                updateQrDerivedFields();
                updateQrCorrectionsDerivedFields();
                updateAddressBlockDerivedFields();
                updateEvolutionDerivedFields();
                updateOmologazioneFieldsState();
                toggleResizeFields(chkApplyResize.isSelected());
                toggleRotateByTextFields(chkRotateByText.isSelected());
                toggleBarcodeFields(chkBarcode.isSelected());
                toggleRaccomandataFields(chkRaccomandataBarcode.isSelected());
                togglePageCounterFields(chkPageCounter.isSelected());
                toggleQrFields(chkQrCode.isSelected());
                toggleQrCorrectionsFields(chkQrCorrections.isSelected());
                toggleAddressBlockFields(chkAddressBlock.isSelected());
                toggleAddressKeyReadFields(chkAddressKeyRead.isSelected());
                toggleRisoFields(chkRisoOptimization.isSelected());
                toggleEvolutionFields(chkEvolution.isSelected());
                toggleLabelFields(chkGroupLabel.isSelected());
                updateReticleButtonState();

                storeRacPreset(currentRacPresetKey);
        }

        // --- File choosers ---
        private void chooseInput() {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter("PDF (*.pdf)", "pdf"));
                String cur = txtInput.getText();
                if (!isBlank(cur))
                        fc.setSelectedFile(new java.io.File(cur));
                int r = fc.showOpenDialog(this);
                if (r == JFileChooser.APPROVE_OPTION) {
                        txtInput.setText(fc.getSelectedFile().getAbsolutePath());
                        if (isBlank(txtOutput.getText())) {
                                String p = fc.getSelectedFile().getAbsolutePath();
                                txtOutput.setText(p.toLowerCase().endsWith(".pdf")
                                                ? p.substring(0, p.length() - 4) + "_duplex.pdf"
                                                : p + "_duplex.pdf");
                        }
                }
        }

        private PdfDuplexGrouper.PostaEvolutionOpts buildPostaEvolutionOpts(
                        PdfDuplexGrouper.AddressBlockOpts addressBlockOpts,
                        boolean validateAddressSource,
                        boolean duEnabled) {
                String evolutionExcelPath = safe(txtEvolutionExcelPath.getText());
                if (validateAddressSource) {
                        if (addressBlockOpts == null && evolutionExcelPath.isEmpty()) {
                                appendLog(
                                                "Per Posta Evolution e' obbligatorio abilitare il blocco indirizzo oppure indicare un file Excel destinatari.");
                                return null;
                        }
                        if (!evolutionExcelPath.isEmpty() && !Files.exists(Paths.get(evolutionExcelPath))) {
                                appendLog("Il file Excel destinatari non esiste: " + evolutionExcelPath);
                                return null;
                        }
                }

                float evolutionXpt = Imbustatrice.mm(parseFloatOrDefault(txtEvolutionXmm.getText(), 0f));
                float evolutionYpt = Imbustatrice.mm(parseFloatOrDefault(txtEvolutionYmm.getText(), 0f));
                float evolutionModulePt = Imbustatrice.mm(
                                parseFloatOrDefault(txtEvolutionModuleMm.getText(), 0.508f));
                int evolutionWidthCells = Math.max(0, parseIntOrDefault(txtEvolutionWidthCells.getText(), 48));
                int evolutionHeightCells = Math.max(0, parseIntOrDefault(txtEvolutionHeightCells.getText(), 16));
                Imbustatrice.PostaEvolutionDataMatrixOpts dmOpts = new Imbustatrice.PostaEvolutionDataMatrixOpts(
                                true, evolutionXpt, evolutionYpt, evolutionModulePt, evolutionWidthCells,
                                evolutionHeightCells, 2);

                return new PdfDuplexGrouper.PostaEvolutionOpts(
                                true,
                                dmOpts,
                                evolutionExcelPath,
                                safe(txtEvolutionGamma.getText()),
                                safe(txtEvolutionSapId.getText()),
                                safe(txtEvolutionClientId.getText()),
                                safe(txtEvolutionClasse.getText()),
                                safe(txtEvolutionTipoProdotto.getText()),
                                safe(txtEvolutionCapDestFallback.getText()),
                                safe(txtEvolutionCodTecDest.getText()),
                                safe(txtEvolutionCapMitt.getText()),
                                safe(txtEvolutionCodTecMitt.getText()),
                                safe(txtEvolutionPrenFiglio.getText()),
                                safe(txtEvolutionStampatore.getText()),
                                Math.max(0L, parseLongOrDefault(txtEvolutionStartOggetto.getText(), 1L)),
                                safe(txtEvolutionCausale.getText()),
                                safe(txtEvolutionOmologazioneDm.getText()),
                                safe(txtEvolutionCampo16.getText()),
                                safe(txtEvolutionServizi.getText()),
                                duEnabled,
                                safe(txtEvolutionDuTipoAccettazioneFile.getText()),
                                safe(txtEvolutionDuProgressivo.getText()),
                                "",
                                safe(txtEvolutionDuUtenzaOperatore.getText()),
                                safe(txtEvolutionDuIdPrenotazione.getText()),
                                "",
                                "",
                                safe(txtEvolutionDuDataPostalizzazione.getText()),
                                safe(txtEvolutionDuFrazionario.getText()),
                                safe(txtEvolutionDuTipologiaProdotto.getText()),
                                safe(txtEvolutionDuCodiceProdotto.getText()),
                                safe(txtEvolutionDuServizioAccessorio.getText()),
                                safe(txtEvolutionDuCodiceTipologiaAccettazione.getText()),
                                safe(txtEvolutionDuTipologiaTracciatura.getText()),
                                safe(txtEvolutionDuCodiceConto.getText()),
                                safe(txtEvolutionDuDescrizione.getText()),
                                safe(txtEvolutionDuCodiceOmologazione.getText()),
                                safe(txtEvolutionDuFormato.getText()),
                                safe(txtEvolutionDuIdHu.getText()),
                                safe(txtEvolutionDuIdScatola.getText()),
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "",
                                "");
        }

        private void chooseOutput() {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter("PDF (*.pdf)", "pdf"));
                String cur = txtOutput.getText();
                if (!isBlank(cur))
                        fc.setSelectedFile(new java.io.File(cur));
                int r = fc.showSaveDialog(this);
                if (r == JFileChooser.APPROVE_OPTION) {
                        String p = fc.getSelectedFile().getAbsolutePath();
                        if (!p.toLowerCase().endsWith(".pdf"))
                                p += ".pdf";
                        txtOutput.setText(p);
                }
        }

        private void chooseEvolutionExcel() {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
                String cur = txtEvolutionExcelPath.getText();
                if (!isBlank(cur)) {
                        fc.setSelectedFile(new File(cur));
                }
                int r = fc.showOpenDialog(this);
                if (r == JFileChooser.APPROVE_OPTION) {
                        txtEvolutionExcelPath.setText(fc.getSelectedFile().getAbsolutePath());
                }
        }

        private void chooseQrCorrectionsExcel() {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
                String cur = txtQrCorrectionsExcelPath.getText();
                if (!isBlank(cur)) {
                        fc.setSelectedFile(new File(cur));
                }
                int r = fc.showOpenDialog(this);
                if (r == JFileChooser.APPROVE_OPTION) {
                        txtQrCorrectionsExcelPath.setText(fc.getSelectedFile().getAbsolutePath());
                }
        }

        // --- Enable/Disable ---
        private void toggleRotateByTextFields(boolean enabled) {
                txtRotateByText.setEnabled(enabled);
                cmbRotateByTextDegrees.setEnabled(enabled);
                chkResizeOnRotatedPages.setEnabled(enabled);
        }

        private void toggleResizeFields(boolean enabled) {
                txtScalePct.setEnabled(enabled);
                txtOffsetXmm.setEnabled(enabled);
                txtOffsetYmm.setEnabled(enabled);
                chkForceA4BeforeResize.setEnabled(enabled);
        }

        private void toggleBarcodeFields(boolean enabled) {
                chkBarcodeOmr.setEnabled(enabled);
                chkAllegati.setEnabled(enabled);
                chkBarcodeOnAttachment.setEnabled(enabled);
                txtBcXmm.setEnabled(enabled);
                txtBcYmm.setEnabled(enabled);
                txtBcXpt.setEnabled(enabled);
                txtBcYpt.setEnabled(enabled);
                txtBcModulePt.setEnabled(enabled);
                txtBcBarHeightPt.setEnabled(enabled);
                txtBcFontSizePt.setEnabled(enabled);
                txtBcRotationDeg.setEnabled(enabled);
                txtBcYOffsetPt.setEnabled(enabled);
                updateGroupStartProgressiveFieldState();
                updateReticleButtonState();
        }

        private void toggleLabelFields(boolean enabled) {
                txtLabelId.setEnabled(enabled);
                txtLabelXmm.setEnabled(enabled);
                txtLabelYmm.setEnabled(enabled);
                txtLabelXpt.setEnabled(enabled);
                txtLabelYpt.setEnabled(enabled);
                rdbLabelHorizontal.setEnabled(enabled);
                rdbLabelVertical.setEnabled(enabled);
                updateGroupStartProgressiveFieldState();
        }

        private void updateGroupStartProgressiveFieldState() {
                if (txtBcStartProg == null) {
                        return;
                }
                boolean barcodeActive = chkBarcode != null && chkBarcode.isEnabled() && chkBarcode.isSelected();
                boolean labelActive = chkGroupLabel != null && chkGroupLabel.isEnabled() && chkGroupLabel.isSelected();
                txtBcStartProg.setEnabled(barcodeActive || labelActive);
        }

        private void toggleRaccomandataFields(boolean enabled) {
                if (txtRcXmm == null) {
                        return;
                }
                txtRcXmm.setEnabled(enabled);
                txtRcYmm.setEnabled(enabled);
                txtRcXpt.setEnabled(enabled);
                txtRcYpt.setEnabled(enabled);
                txtRcHeightMm.setEnabled(enabled);
                txtRcModuleMm.setEnabled(enabled);
                txtRcHumanFontPt.setEnabled(enabled);
                txtRcHumanGapMm.setEnabled(enabled);
                txtRcPrefix.setEnabled(enabled);
                txtRcPrefixGapMm.setEnabled(enabled);
                txtRcPrefixFontPt.setEnabled(enabled);
                txtRcStartCode.setEnabled(enabled);
                if (cmbRacPreset != null) {
                        cmbRacPreset.setEnabled(enabled);
                }
                if (chkRcCustomText != null) {
                        chkRcCustomText.setEnabled(enabled);
                }
                updateRacCustomTextFieldsState();
                updateReticleButtonState();
        }

        private void toggleOmologazioneFields(boolean enabled) {
                if (cmbRcOmologazionePreset == null) {
                        return;
                }
                cmbRcOmologazionePreset.setEnabled(enabled);
                boolean allowTextEdit = enabled && RC_OMOLOG_PRESET_CUSTOM.equals(currentOmologazionePresetKey);
                txtRcOmologazioneText.setEnabled(enabled);
                txtRcOmologazioneText.setEditable(allowTextEdit);
                txtRcOmologazioneXmm.setEnabled(enabled);
                txtRcOmologazioneYmm.setEnabled(enabled);
                txtRcOmologazioneXpt.setEnabled(enabled);
                txtRcOmologazioneYpt.setEnabled(enabled);
                txtRcOmologazioneFontPt.setEnabled(enabled);
        }

        private void toggleRacCustomTextFields(boolean enabled) {
                if (txtRcCustomText == null) {
                        return;
                }
                txtRcCustomText.setEnabled(enabled);
                txtRcCustomTextXmm.setEnabled(enabled);
                txtRcCustomTextYmm.setEnabled(enabled);
                txtRcCustomTextXpt.setEnabled(enabled);
                txtRcCustomTextYpt.setEnabled(enabled);
                txtRcCustomTextFontPt.setEnabled(enabled);
        }

        private void updateRacCustomTextFieldsState() {
                if (chkRcCustomText == null) {
                        return;
                }
                boolean enabled = chkRcCustomText.isEnabled() && chkRcCustomText.isSelected();
                toggleRacCustomTextFields(enabled);
        }

        private void updateOmologazioneFieldsState() {
                if (chkRcOmologazione == null) {
                        return;
                }
                boolean enabled = chkRcOmologazione.isEnabled() && chkRcOmologazione.isSelected();
                toggleOmologazioneFields(enabled);
        }

        private void togglePageCounterFields(boolean enabled) {
                txtCounterXmm.setEnabled(enabled);
                txtCounterYmm.setEnabled(enabled);
                txtCounterXpt.setEnabled(enabled);
                txtCounterYpt.setEnabled(enabled);
                txtCounterFontSizePt.setEnabled(enabled);
                rdbCounterHorizontal.setEnabled(enabled);
                rdbCounterVertical.setEnabled(enabled);
        }

        private void toggleQrFields(boolean enabled) {
                txtQrBase.setEnabled(enabled);
                txtQrDigits.setEnabled(enabled);
                txtQrStart.setEnabled(enabled);
                txtQrExample.setEnabled(enabled);
                txtQrSizeMm.setEnabled(enabled);
                txtQrSizePt.setEnabled(enabled);
                txtQrXmm.setEnabled(enabled);
                txtQrYmm.setEnabled(enabled);
                txtQrXpt.setEnabled(enabled);
                txtQrYpt.setEnabled(enabled);
                cmbQrErrorCorrection.setEnabled(enabled);
        }

        private void toggleQrCorrectionsFields(boolean enabled) {
                txtQrCorrectionsExcelPath.setEnabled(enabled);
                btnQrCorrectionsExcelBrowse.setEnabled(enabled);
                txtQrCorrectionsXmm.setEnabled(enabled);
                txtQrCorrectionsYmm.setEnabled(enabled);
                txtQrCorrectionsXpt.setEnabled(enabled);
                txtQrCorrectionsYpt.setEnabled(enabled);
                txtQrCorrectionsWidthMm.setEnabled(enabled);
                txtQrCorrectionsWidthPt.setEnabled(enabled);
                txtQrCorrectionsFontPt.setEnabled(enabled);
                txtQrCorrectionsIconMm.setEnabled(enabled);
        }

        private void toggleAddressBlockFields(boolean enabled) {
                txtAddressXmm.setEnabled(enabled);
                txtAddressYmm.setEnabled(enabled);
                txtAddressWidthMm.setEnabled(enabled);
                txtAddressHeightMm.setEnabled(enabled);
                txtAddressXpt.setEnabled(enabled);
                txtAddressYpt.setEnabled(enabled);
                txtAddressWidthPt.setEnabled(enabled);
                txtAddressHeightPt.setEnabled(enabled);
        }

        private void toggleAddressKeyReadFields(boolean enabled) {
                txtAddressKeyString.setEnabled(enabled);
        }

        private PdfDuplexGrouper.AddressBlockOpts buildAddressBlockOpts(boolean requireEnabled) {
                if (!chkAddressBlock.isSelected()) {
                        if (requireEnabled) {
                                appendLog("Abilitare \"Leggi blocco indirizzo\" e configurare le coordinate.");
                        }
                        return null;
                }
                float addrXmm = parseFloatOrDefault(txtAddressXmm.getText(), 0f);
                float addrYmm = parseFloatOrDefault(txtAddressYmm.getText(), 0f);
                float addrWidthMm = parseFloatOrDefault(txtAddressWidthMm.getText(), 0f);
                float addrHeightMm = parseFloatOrDefault(txtAddressHeightMm.getText(), 0f);
                if (addrXmm < 0f || addrYmm < 0f) {
                        appendLog("Le coordinate del blocco indirizzo devono essere >= 0");
                        return null;
                }
                if (addrWidthMm <= 0f || addrHeightMm <= 0f) {
                        appendLog("Le dimensioni del blocco indirizzo devono essere > 0");
                        return null;
                }
                float addrXpt = Imbustatrice.mm(addrXmm);
                float addrYpt = Imbustatrice.mm(addrYmm);
                float addrWidthPt = Imbustatrice.mm(addrWidthMm);
                float addrHeightPt = Imbustatrice.mm(addrHeightMm);
                return new PdfDuplexGrouper.AddressBlockOpts(true, addrXpt, addrYpt, addrWidthPt, addrHeightPt);
        }

        private void toggleRisoFields(boolean enabled) {
                txtRisoRecordId.setEnabled(enabled);
        }

        private void toggleEvolutionFields(boolean enabled) {
                cmbEvolutionPreset.setEnabled(enabled);
                txtEvolutionXmm.setEnabled(enabled);
                txtEvolutionYmm.setEnabled(enabled);
                txtEvolutionXpt.setEnabled(enabled);
                txtEvolutionYpt.setEnabled(enabled);
                txtEvolutionModuleMm.setEnabled(enabled);
                txtEvolutionWidthCells.setEnabled(enabled);
                txtEvolutionHeightCells.setEnabled(enabled);
                txtEvolutionGamma.setEnabled(enabled);
                txtEvolutionSapId.setEnabled(enabled);
                txtEvolutionClientId.setEnabled(enabled);
                txtEvolutionClasse.setEnabled(enabled);
                txtEvolutionTipoProdotto.setEnabled(enabled);
                txtEvolutionCapDestFallback.setEnabled(enabled);
                txtEvolutionCodTecDest.setEnabled(enabled);
                txtEvolutionCapMitt.setEnabled(enabled);
                txtEvolutionCodTecMitt.setEnabled(enabled);
                txtEvolutionPrenFiglio.setEnabled(enabled);
                txtEvolutionStampatore.setEnabled(enabled);
                txtEvolutionStartOggetto.setEnabled(enabled);
                txtEvolutionCausale.setEnabled(enabled);
                txtEvolutionOmologazioneDm.setEnabled(enabled);
                txtEvolutionCampo16.setEnabled(enabled);
                txtEvolutionServizi.setEnabled(enabled);
                txtEvolutionExcelPath.setEnabled(enabled);
                btnEvolutionExcelBrowse.setEnabled(enabled);
                chkEvolutionDu.setEnabled(enabled);
                toggleEvolutionDuFields(enabled && chkEvolutionDu.isSelected());
        }

        private void toggleEvolutionDuFields(boolean enabled) {
                txtEvolutionDuTipoAccettazioneFile.setEnabled(enabled);
                txtEvolutionDuProgressivo.setEnabled(enabled);
                txtEvolutionDuUtenzaOperatore.setEnabled(enabled);
                txtEvolutionDuIdPrenotazione.setEnabled(enabled);
                txtEvolutionDuDataPostalizzazione.setEnabled(enabled);
                txtEvolutionDuFrazionario.setEnabled(enabled);
                txtEvolutionDuTipologiaProdotto.setEnabled(enabled);
                txtEvolutionDuCodiceProdotto.setEnabled(enabled);
                txtEvolutionDuServizioAccessorio.setEnabled(enabled);
                txtEvolutionDuCodiceTipologiaAccettazione.setEnabled(enabled);
                txtEvolutionDuTipologiaTracciatura.setEnabled(enabled);
                txtEvolutionDuCodiceConto.setEnabled(enabled);
                txtEvolutionDuDescrizione.setEnabled(enabled);
                txtEvolutionDuCodiceOmologazione.setEnabled(enabled);
                txtEvolutionDuFormato.setEnabled(enabled);
                txtEvolutionDuIdHu.setEnabled(enabled);
                txtEvolutionDuIdScatola.setEnabled(enabled);
        }

        private void updateReticleButtonState() {
                if (btnReticolo == null) {
                        return;
                }
                boolean baseEnabled = btnRun != null && btnRun.isEnabled();
                boolean anyBarcode = (chkBarcode != null && chkBarcode.isSelected())
                                || (chkRaccomandataBarcode != null && chkRaccomandataBarcode.isSelected())
                                || (chkQrCode != null && chkQrCode.isSelected())
                                || (chkQrCorrections != null && chkQrCorrections.isSelected());
                btnReticolo.setEnabled(baseEnabled && anyBarcode);
        }

        private void attachBarcodePositionListeners() {
                DocumentListener listener = new DocumentListener() {
                        @Override
                        public void insertUpdate(DocumentEvent e) {
                                updateBarcodeDerivedFields();
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e) {
                                updateBarcodeDerivedFields();
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                                updateBarcodeDerivedFields();
                        }
                };
                txtBcXmm.getDocument().addDocumentListener(listener);
                txtBcYmm.getDocument().addDocumentListener(listener);
                txtLabelXmm.getDocument().addDocumentListener(listener);
                txtLabelYmm.getDocument().addDocumentListener(listener);
                txtCounterXmm.getDocument().addDocumentListener(listener);
                txtCounterYmm.getDocument().addDocumentListener(listener);
        }

        private void attachRaccomandataListeners() {
                if (txtRcXmm == null) {
                        return;
                }
                DocumentListener listener = new DocumentListener() {
                        @Override
                        public void insertUpdate(DocumentEvent e) {
                                handle();
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e) {
                                handle();
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                                handle();
                        }

                        private void handle() {
                                updateRaccomandataDerivedFields();
                                storeActiveRacPresetFromFields();
                        }
                };
                txtRcXmm.getDocument().addDocumentListener(listener);
                txtRcYmm.getDocument().addDocumentListener(listener);
                txtRcPrefix.getDocument().addDocumentListener(listener);
                txtRcStartCode.getDocument().addDocumentListener(listener);
                txtRcOmologazioneXmm.getDocument().addDocumentListener(listener);
                txtRcOmologazioneYmm.getDocument().addDocumentListener(listener);
                txtRcOmologazioneFontPt.getDocument().addDocumentListener(listener);
                txtRcOmologazioneText.getDocument().addDocumentListener(listener);
                txtRcCustomText.getDocument().addDocumentListener(listener);
                txtRcCustomTextXmm.getDocument().addDocumentListener(listener);
                txtRcCustomTextYmm.getDocument().addDocumentListener(listener);
                txtRcCustomTextFontPt.getDocument().addDocumentListener(listener);
        }

        private void attachQrListeners() {
                DocumentListener listener = new DocumentListener() {
                        @Override
                        public void insertUpdate(DocumentEvent e) {
                                updateQrDerivedFields();
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e) {
                                updateQrDerivedFields();
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                                updateQrDerivedFields();
                        }
                };
                txtQrBase.getDocument().addDocumentListener(listener);
                txtQrDigits.getDocument().addDocumentListener(listener);
                txtQrStart.getDocument().addDocumentListener(listener);
                txtQrSizeMm.getDocument().addDocumentListener(listener);
                txtQrXmm.getDocument().addDocumentListener(listener);
                txtQrYmm.getDocument().addDocumentListener(listener);
        }

        private void attachQrCorrectionsListeners() {
                DocumentListener listener = new DocumentListener() {
                        @Override
                        public void insertUpdate(DocumentEvent e) {
                                updateQrCorrectionsDerivedFields();
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e) {
                                updateQrCorrectionsDerivedFields();
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                                updateQrCorrectionsDerivedFields();
                        }
                };
                txtQrCorrectionsXmm.getDocument().addDocumentListener(listener);
                txtQrCorrectionsYmm.getDocument().addDocumentListener(listener);
                txtQrCorrectionsWidthMm.getDocument().addDocumentListener(listener);
        }

        private void attachAddressBlockListeners() {
                DocumentListener listener = new DocumentListener() {
                        @Override
                        public void insertUpdate(DocumentEvent e) {
                                updateAddressBlockDerivedFields();
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e) {
                                updateAddressBlockDerivedFields();
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                                updateAddressBlockDerivedFields();
                        }
                };
                txtAddressXmm.getDocument().addDocumentListener(listener);
                txtAddressYmm.getDocument().addDocumentListener(listener);
                txtAddressWidthMm.getDocument().addDocumentListener(listener);
                txtAddressHeightMm.getDocument().addDocumentListener(listener);
        }

        private void attachEvolutionListeners() {
                DocumentListener listener = new DocumentListener() {
                        @Override
                        public void insertUpdate(DocumentEvent e) {
                                updateEvolutionDerivedFields();
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e) {
                                updateEvolutionDerivedFields();
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                                updateEvolutionDerivedFields();
                        }
                };
                txtEvolutionXmm.getDocument().addDocumentListener(listener);
                txtEvolutionYmm.getDocument().addDocumentListener(listener);
        }

        private void applyBarcodeFixedDefaults() {
                txtBcModulePt.setText(
                                formatFloat(BARCODE_IMBUSTATRICE_DEFAULTS.getFloat("moduleWidthPt"), 2));
                txtBcBarHeightPt.setText(
                                formatFloat(BARCODE_IMBUSTATRICE_DEFAULTS.getFloat("barHeightPt"), 2));
                txtBcFontSizePt.setText(
                                formatFloat(BARCODE_IMBUSTATRICE_DEFAULTS.getFloat("labelFontSizePt"), 2));
                txtBcRotationDeg.setText(
                                formatFloat(BARCODE_IMBUSTATRICE_DEFAULTS.getFloat("rotationDegrees"), 0));
                txtBcYOffsetPt.setText(
                                formatFloat(BARCODE_IMBUSTATRICE_DEFAULTS.getFloat("verticalOffsetPt"), 2));
                updateBarcodeDerivedFields();
        }

        private void updateBarcodeDerivedFields() {
                float xmm = parseFloatOrDefault(txtBcXmm.getText(), 0f);
                float ymm = parseFloatOrDefault(txtBcYmm.getText(), 0f);
                txtBcXpt.setText(formatFloat(Imbustatrice.mm(xmm), 2));
                txtBcYpt.setText(formatFloat(Imbustatrice.mm(ymm), 2));

                float labelXmm = parseFloatOrDefault(txtLabelXmm.getText(), 0f);
                float labelYmm = parseFloatOrDefault(txtLabelYmm.getText(), 0f);
                txtLabelXpt.setText(formatFloat(Imbustatrice.mm(labelXmm), 2));
                txtLabelYpt.setText(formatFloat(Imbustatrice.mm(labelYmm), 2));

                updateRaccomandataDerivedFields();
                updatePageCounterDerivedFields();
        }

        private void updateRaccomandataDerivedFields() {
                if (txtRcXpt == null || txtRcYpt == null)
                        return;
                float barcodeXmm = parseFloatOrDefault(txtRcXmm.getText(), 0f);
                float barcodeYmm = parseFloatOrDefault(txtRcYmm.getText(), 0f);
                txtRcXpt.setText(formatFloat(Imbustatrice.mm(barcodeXmm), 2));
                txtRcYpt.setText(formatFloat(Imbustatrice.mm(barcodeYmm), 2));

                if (txtRcOmologazioneXpt != null && txtRcOmologazioneYpt != null) {
                        float omologXmm = parseFloatOrDefault(txtRcOmologazioneXmm.getText(), 0f);
                        float omologYmm = parseFloatOrDefault(txtRcOmologazioneYmm.getText(), 0f);
                        txtRcOmologazioneXpt.setText(formatFloat(Imbustatrice.mm(omologXmm), 2));
                        txtRcOmologazioneYpt.setText(formatFloat(Imbustatrice.mm(omologYmm), 2));
                }
                if (txtRcCustomTextXpt != null && txtRcCustomTextYpt != null) {
                        float textXmm = parseFloatOrDefault(txtRcCustomTextXmm.getText(), 0f);
                        float textYmm = parseFloatOrDefault(txtRcCustomTextYmm.getText(), 0f);
                        txtRcCustomTextXpt.setText(formatFloat(Imbustatrice.mm(textXmm), 2));
                        txtRcCustomTextYpt.setText(formatFloat(Imbustatrice.mm(textYmm), 2));
                }
        }

        private void updatePageCounterDerivedFields() {
                if (txtCounterXpt == null || txtCounterYpt == null)
                        return;
                float counterXmm = parseFloatOrDefault(txtCounterXmm.getText(), 0f);
                float counterYmm = parseFloatOrDefault(txtCounterYmm.getText(), 0f);
                txtCounterXpt.setText(formatFloat(Imbustatrice.mm(counterXmm), 2));
                txtCounterYpt.setText(formatFloat(Imbustatrice.mm(counterYmm), 2));
        }

        private void updateQrDerivedFields() {
                if (txtQrSizePt == null || txtQrXpt == null || txtQrYpt == null)
                        return;
                float sizeMm = parseFloatOrDefault(txtQrSizeMm.getText(), 0f);
                txtQrSizePt.setText(formatFloat(Imbustatrice.mm(sizeMm), 2));
                float xMm = parseFloatOrDefault(txtQrXmm.getText(), 0f);
                float yMm = parseFloatOrDefault(txtQrYmm.getText(), 0f);
                txtQrXpt.setText(formatFloat(Imbustatrice.mm(xMm), 2));
                txtQrYpt.setText(formatFloat(Imbustatrice.mm(yMm), 2));

                String base = safe(txtQrBase.getText());
                int digits = Math.max(1, Math.min(18, parseIntOrDefault(txtQrDigits.getText(), 1)));
                long startValue = Math.max(0L, parseLongOrDefault(txtQrStart.getText(), 0L));
                String padded = String.format(Locale.US, "%0" + digits + "d", startValue);
                txtQrExample.setText(base + padded);
        }

        private void updateQrCorrectionsDerivedFields() {
                if (txtQrCorrectionsXpt == null || txtQrCorrectionsYpt == null || txtQrCorrectionsWidthPt == null) {
                        return;
                }
                float xMm = parseFloatOrDefault(txtQrCorrectionsXmm.getText(), 0f);
                float yMm = parseFloatOrDefault(txtQrCorrectionsYmm.getText(), 0f);
                float widthMm = parseFloatOrDefault(txtQrCorrectionsWidthMm.getText(), 0f);
                txtQrCorrectionsXpt.setText(formatFloat(Imbustatrice.mm(xMm), 2));
                txtQrCorrectionsYpt.setText(formatFloat(Imbustatrice.mm(yMm), 2));
                txtQrCorrectionsWidthPt.setText(formatFloat(Imbustatrice.mm(widthMm), 2));
        }

        private void updateAddressBlockDerivedFields() {
                if (txtAddressXpt == null || txtAddressYpt == null)
                        return;
                float xMm = parseFloatOrDefault(txtAddressXmm.getText(), 0f);
                float yMm = parseFloatOrDefault(txtAddressYmm.getText(), 0f);
                float widthMm = Math.max(0f, parseFloatOrDefault(txtAddressWidthMm.getText(), 0f));
                float heightMm = Math.max(0f, parseFloatOrDefault(txtAddressHeightMm.getText(), 0f));
                txtAddressXpt.setText(formatFloat(Imbustatrice.mm(xMm), 2));
                txtAddressYpt.setText(formatFloat(Imbustatrice.mm(yMm), 2));
                txtAddressWidthPt.setText(formatFloat(Imbustatrice.mm(widthMm), 2));
                txtAddressHeightPt.setText(formatFloat(Imbustatrice.mm(heightMm), 2));
        }

        private void updateEvolutionDerivedFields() {
                if (txtEvolutionXpt == null || txtEvolutionYpt == null) {
                        return;
                }
                float xMm = parseFloatOrDefault(txtEvolutionXmm.getText(), 0f);
                float yMm = parseFloatOrDefault(txtEvolutionYmm.getText(), 0f);
                txtEvolutionXpt.setText(formatFloat(Imbustatrice.mm(xMm), 2));
                txtEvolutionYpt.setText(formatFloat(Imbustatrice.mm(yMm), 2));
        }

        private void initOmologazioneOptions() {
                omologazioneOptions.clear();
                omologazioneOptions.put(RC_OMOLOG_PRESET_MASSIVA, "DCOOS2065");
                omologazioneOptions.put(RC_OMOLOG_PRESET_RACCOMANDATA, "DCOCC0015");
                omologazioneOptions.put(RC_OMOLOG_PRESET_AG, "DCOPD1063");
                omologazioneOptions.put(RC_OMOLOG_PRESET_CUSTOM, "");
        }

        private void initRaccomandataPresets() {
                racPresets.clear();

                RacPreset custom = new RacPreset("", "11000000001", 30f, 225f);
                custom.omologazioneEnabled = false;
                custom.omologazionePresetKey = RC_OMOLOG_PRESET_CUSTOM;
                custom.omologazioneText = "";
                custom.customTextEnabled = false;
                racPresets.put(RAC_PRESET_CUSTOM, custom);

                RacPreset ar = new RacPreset(BARCODE_RACCOMANDATA_DEFAULTS.getString("prefix"),
                                BARCODE_RACCOMANDATA_DEFAULTS.getString("startCode"),
                                BARCODE_RACCOMANDATA_DEFAULTS.getFloat("positionXmm"),
                                BARCODE_RACCOMANDATA_DEFAULTS.getFloat("positionYmm"));
                ar.omologazioneEnabled = OMOLOGAZIONE_DEFAULTS.getBoolean("omologazioneEnabled");
                String arPresetKey = OMOLOGAZIONE_DEFAULTS.getString("presetKey");
                if (!omologazioneOptions.containsKey(arPresetKey)) {
                        arPresetKey = RC_OMOLOG_PRESET_RACCOMANDATA;
                }
                ar.omologazionePresetKey = arPresetKey;
                ar.omologazioneText = OMOLOGAZIONE_DEFAULTS.getString("codice");
                ar.omologazioneXmm = OMOLOGAZIONE_DEFAULTS.getFloat("positionXmm");
                ar.omologazioneYmm = OMOLOGAZIONE_DEFAULTS.getFloat("positionYmm");
                ar.omologazioneFontPt = OMOLOGAZIONE_DEFAULTS.getFloat("fontHeightPt");
                ar.customTextEnabled = false;
                racPresets.put(RAC_PRESET_AR, ar);

                RacPreset ag = new RacPreset("AG", "31000000001", 30f, 225f);
                ag.omologazioneEnabled = true;
                ag.omologazionePresetKey = RC_OMOLOG_PRESET_AG;
                ag.omologazioneText = omologazioneOptions.getOrDefault(RC_OMOLOG_PRESET_AG, "DCOPD1063");
                ag.omologazioneXmm = 20f;
                ag.omologazioneYmm = 260f;
                ag.omologazioneFontPt = Imbustatrice.RaccomandataStandard.OMOLOGAZIONE_FONT_PT;
                ag.customTextEnabled = true;
                ag.customText = RAC_RETURN_TEXT_DEFAULT;
                ag.customTextXmm = 20f;
                ag.customTextYmm = 248f;
                ag.customTextFontPt = Imbustatrice.RaccomandataStandard.CUSTOM_TEXT_FONT_PT;
                racPresets.put(RAC_PRESET_AG, ag);

        }

        private void initEvolutionPresets() {
                evolutionPresets.clear();
                evolutionPresets.putAll(loadEvolutionPresets(true));
        }

        private Map<String, EvolutionPreset> loadEvolutionPresets(boolean syncRemote) {
                LinkedHashMap<String, EvolutionPreset> loaded = new LinkedHashMap<>();
                Map<String, EvolutionSwitch.PresetConfig> source = syncRemote
                                ? EvolutionSwitch.available(EVOLUTION_DEFAULTS)
                                : EvolutionSwitch.availableLocal(EVOLUTION_DEFAULTS);
                for (Map.Entry<String, EvolutionSwitch.PresetConfig> entry : source
                                .entrySet()) {
                        loaded.put(entry.getKey(), toEvolutionPreset(entry.getValue()));
                }
                return loaded;
        }

        private void refreshEvolutionPresets() {
                if (btnEvolutionPresetUpdate == null || !btnEvolutionPresetUpdate.isEnabled()) {
                        return;
                }
                String previousSelection = currentEvolutionPresetKey;
                btnEvolutionPresetUpdate.setEnabled(false);
                appendLog("Verifica aggiornamenti preset Posta Evolution...");

                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        private Map<String, EvolutionPreset> loadedPresets;
                        private EvolutionSwitch.SyncResult syncResult;
                        private String selectedAfterReload;
                        private int presetCount;
                        private Exception failure;

                        @Override
                        protected Void doInBackground() {
                                try {
                                        syncResult = EvolutionSwitch.syncExternalPresets();
                                        loadedPresets = loadEvolutionPresets(false);
                                        selectedAfterReload = loadedPresets.containsKey(previousSelection)
                                                        ? previousSelection
                                                        : loadedPresets.keySet().stream().findFirst().orElse("");
                                        presetCount = loadedPresets.size();
                                } catch (Exception ex) {
                                        failure = ex;
                                }
                                return null;
                        }

                        @Override
                        protected void done() {
                                btnEvolutionPresetUpdate.setEnabled(true);
                                if (failure != null) {
                                        appendLog("ERRORE aggiornamento preset Evolution: " + failure.getMessage());
                                        JOptionPane.showMessageDialog(App.this,
                                                        "Errore durante l'aggiornamento preset:\n"
                                                                        + failure.getMessage(),
                                                        "Errore",
                                                        JOptionPane.ERROR_MESSAGE);
                                        return;
                                }
                                evolutionPresets.clear();
                                evolutionPresets.putAll(loadedPresets);
                                reloadEvolutionPresetCombo(selectedAfterReload);
                                if (syncResult != null && !syncResult.success) {
                                        appendLog("Aggiornamento preset Evolution non riuscito: "
                                                        + syncResult.errorMessage);
                                        JOptionPane.showMessageDialog(App.this,
                                                        "Preset locali ricaricati, ma il download remoto non e' riuscito:\n"
                                                                        + syncResult.errorMessage,
                                                        "Aggiornamento preset",
                                                        JOptionPane.WARNING_MESSAGE);
                                        return;
                                }
                                String detail = syncResult == null ? ""
                                                : "Scaricati: " + syncResult.downloaded
                                                                + ", aggiornati: " + syncResult.updated
                                                                + ", invariati: " + syncResult.unchanged
                                                                + ", errori file: " + syncResult.failed + ". ";
                                appendLog("Preset Posta Evolution aggiornati. " + detail + "Totale caricati: "
                                                + presetCount);
                                JOptionPane.showMessageDialog(App.this,
                                                "Preset Posta Evolution aggiornati.\n" + detail
                                                                + "\nTotale caricati: " + presetCount,
                                                "Preset aggiornati",
                                                JOptionPane.INFORMATION_MESSAGE);
                        }
                };
                worker.execute();
        }

        private void reloadEvolutionPresetCombo(String selectedKey) {
                updatingEvolutionPreset = true;
                try {
                        cmbEvolutionPreset.removeAllItems();
                        for (String key : evolutionPresets.keySet()) {
                                cmbEvolutionPreset.addItem(key);
                        }
                        currentEvolutionPresetKey = selectedKey;
                        cmbEvolutionPreset.setSelectedItem(currentEvolutionPresetKey);
                } finally {
                        updatingEvolutionPreset = false;
                }
                loadEvolutionPreset(currentEvolutionPresetKey);
        }

        private EvolutionPreset toEvolutionPreset(EvolutionSwitch.PresetConfig config) {
                EvolutionPreset preset = new EvolutionPreset();
                preset.enabled = config.enabled;
                preset.dataMatrixXmm = formatFloat(config.dataMatrixXmm, 2);
                preset.dataMatrixYmm = formatFloat(config.dataMatrixYmm, 2);
                preset.dataMatrixModuleMm = formatFloat(config.dataMatrixModuleMm, 3);
                preset.dataMatrixWidthCells = Integer.toString(config.dataMatrixWidthCells);
                preset.dataMatrixHeightCells = Integer.toString(config.dataMatrixHeightCells);
                preset.gamma = config.gamma;
                preset.sapId = config.sapId;
                preset.clientId = config.clientId;
                preset.classe = config.classe;
                preset.tipoProdotto = config.tipoProdotto;
                preset.capDestFallback = config.capDestFallback;
                preset.codTecDest = config.codTecDest;
                preset.capMitt = config.capMitt;
                preset.codTecMitt = config.codTecMitt;
                preset.idPrenFiglio = config.idPrenFiglio;
                preset.idStampatore = config.idStampatore;
                preset.startOggetto = Long.toString(config.startOggetto);
                preset.causale = config.causale;
                preset.omologazioneDm = config.omologazioneDm;
                preset.campo16 = config.campo16;
                preset.servizi = config.servizi;
                preset.excelPath = config.excelPath;
                preset.duEnabled = config.duEnabled;
                preset.duTipoAccettazioneFile = config.duTipoAccettazioneFile;
                preset.duProgressivo = config.duProgressivo;
                preset.duUtenzaOperatore = config.duUtenzaOperatore;
                preset.duIdPrenotazione = config.duIdPrenotazione;
                preset.duDataPostalizzazione = config.duDataPostalizzazione;
                preset.duFrazionario = config.duFrazionario;
                preset.duTipologiaProdotto = config.duTipologiaProdotto;
                preset.duCodiceProdotto = config.duCodiceProdotto;
                preset.duServizioAccessorio = config.duServizioAccessorio;
                preset.duCodiceTipologiaAccettazione = config.duCodiceTipologiaAccettazione;
                preset.duTipologiaTracciatura = config.duTipologiaTracciatura;
                preset.duCodiceConto = config.duCodiceConto;
                preset.duDescrizione = config.duDescrizione;
                preset.duCodiceOmologazione = config.duCodiceOmologazione;
                preset.duFormato = config.duFormato;
                preset.duIdHu = config.duIdHu;
                preset.duIdScatola = config.duIdScatola;
                return preset;
        }

        private void handleEvolutionPresetSelection() {
                if (cmbEvolutionPreset == null || updatingEvolutionPreset) {
                        return;
                }
                Object selected = cmbEvolutionPreset.getSelectedItem();
                if (selected == null) {
                        return;
                }
                String selectedKey = selected.toString();
                if (!evolutionPresets.containsKey(selectedKey)
                                || Objects.equals(selectedKey, currentEvolutionPresetKey)) {
                        return;
                }
                currentEvolutionPresetKey = selectedKey;
                loadEvolutionPreset(currentEvolutionPresetKey);
        }

        private void loadEvolutionPreset(String key) {
                EvolutionPreset preset = evolutionPresets.get(key);
                if (preset == null) {
                        return;
                }
                chkEvolution.setSelected(preset.enabled);
                txtEvolutionXmm.setText(preset.dataMatrixXmm);
                txtEvolutionYmm.setText(preset.dataMatrixYmm);
                txtEvolutionModuleMm.setText(preset.dataMatrixModuleMm);
                txtEvolutionWidthCells.setText(preset.dataMatrixWidthCells);
                txtEvolutionHeightCells.setText(preset.dataMatrixHeightCells);
                txtEvolutionGamma.setText(preset.gamma);
                txtEvolutionSapId.setText(preset.sapId);
                txtEvolutionClientId.setText(preset.clientId);
                txtEvolutionClasse.setText(preset.classe);
                txtEvolutionTipoProdotto.setText(preset.tipoProdotto);
                txtEvolutionCapDestFallback.setText(preset.capDestFallback);
                txtEvolutionCodTecDest.setText(preset.codTecDest);
                txtEvolutionCapMitt.setText(preset.capMitt);
                txtEvolutionCodTecMitt.setText(preset.codTecMitt);
                txtEvolutionPrenFiglio.setText(preset.idPrenFiglio);
                txtEvolutionStampatore.setText(preset.idStampatore);
                txtEvolutionStartOggetto.setText(preset.startOggetto);
                txtEvolutionCausale.setText(preset.causale);
                txtEvolutionOmologazioneDm.setText(preset.omologazioneDm);
                txtEvolutionCampo16.setText(preset.campo16);
                txtEvolutionServizi.setText(preset.servizi);
                chkEvolutionDu.setSelected(preset.duEnabled);
                txtEvolutionDuTipoAccettazioneFile.setText(preset.duTipoAccettazioneFile);
                txtEvolutionDuProgressivo.setText(preset.duProgressivo);
                txtEvolutionDuUtenzaOperatore.setText(preset.duUtenzaOperatore);
                txtEvolutionDuIdPrenotazione.setText(preset.duIdPrenotazione);
                txtEvolutionDuDataPostalizzazione.setText(preset.duDataPostalizzazione);
                txtEvolutionDuFrazionario.setText(preset.duFrazionario);
                txtEvolutionDuTipologiaProdotto.setText(preset.duTipologiaProdotto);
                txtEvolutionDuCodiceProdotto.setText(preset.duCodiceProdotto);
                txtEvolutionDuServizioAccessorio.setText(preset.duServizioAccessorio);
                txtEvolutionDuCodiceTipologiaAccettazione.setText(preset.duCodiceTipologiaAccettazione);
                txtEvolutionDuTipologiaTracciatura.setText(preset.duTipologiaTracciatura);
                txtEvolutionDuCodiceConto.setText(preset.duCodiceConto);
                txtEvolutionDuDescrizione.setText(preset.duDescrizione);
                txtEvolutionDuCodiceOmologazione.setText(preset.duCodiceOmologazione);
                txtEvolutionDuFormato.setText(preset.duFormato);
                txtEvolutionDuIdHu.setText(preset.duIdHu);
                txtEvolutionDuIdScatola.setText(preset.duIdScatola);
        }

        private void handleRacPresetSelection() {
                if (cmbRacPreset == null)
                        return;
                String selected = (String) cmbRacPreset.getSelectedItem();
                if (selected == null || Objects.equals(selected, currentRacPresetKey))
                        return;
                storeRacPreset(currentRacPresetKey);
                currentRacPresetKey = selected;
                loadRacPreset(selected);
        }

        private void handleOmologazionePresetSelection() {
                if (cmbRcOmologazionePreset == null) {
                        return;
                }
                Object selected = cmbRcOmologazionePreset.getSelectedItem();
                if (selected == null) {
                        return;
                }
                String key = selected.toString();
                boolean changed = !Objects.equals(key, currentOmologazionePresetKey);
                currentOmologazionePresetKey = key;
                updatingOmologazioneFields = true;
                try {
                        boolean isCustom = RC_OMOLOG_PRESET_CUSTOM.equals(key);
                        String code = omologazioneOptions.getOrDefault(key, "");
                        if (!isCustom) {
                                txtRcOmologazioneText.setText(code);
                        }
                        txtRcOmologazioneText.setEditable(isCustom);
                } finally {
                        updatingOmologazioneFields = false;
                }
                updateOmologazioneFieldsState();
                if (changed) {
                        storeActiveRacPresetFromFields();
                }
        }

        private void loadRacPreset(String key) {
                RacPreset preset = racPresets.get(key);
                if (preset == null)
                        return;
                if (currentOmologazionePresetKey == null)
                        currentOmologazionePresetKey = RC_OMOLOG_PRESET_CUSTOM;
                updatingRacFields = true;
                txtRcPrefix.setText(preset.prefix);
                txtRcStartCode.setText(preset.startCode);
                txtRcXmm.setText(formatFloat(preset.xMm, 2));
                txtRcYmm.setText(formatFloat(preset.yMm, 2));
                updatingRacFields = false;

                updatingOmologazioneFields = true;
                chkRcOmologazione.setSelected(preset.omologazioneEnabled);
                currentOmologazionePresetKey = preset.omologazionePresetKey;
                if (!omologazioneOptions.containsKey(currentOmologazionePresetKey)) {
                        currentOmologazionePresetKey = RC_OMOLOG_PRESET_CUSTOM;
                }
                if (cmbRcOmologazionePreset != null) {
                        cmbRcOmologazionePreset.setSelectedItem(currentOmologazionePresetKey);
                }
                txtRcOmologazioneText.setText(preset.omologazioneText);
                txtRcOmologazioneXmm.setText(formatFloat(preset.omologazioneXmm, 2));
                txtRcOmologazioneYmm.setText(formatFloat(preset.omologazioneYmm, 2));
                txtRcOmologazioneFontPt.setText(formatFloat(preset.omologazioneFontPt, 2));
                updatingOmologazioneFields = false;

                updatingRacFields = true;
                try {
                        chkRcCustomText.setSelected(preset.customTextEnabled);
                        txtRcCustomText.setText(preset.customText);
                        txtRcCustomTextXmm.setText(formatFloat(preset.customTextXmm, 2));
                        txtRcCustomTextYmm.setText(formatFloat(preset.customTextYmm, 2));
                        txtRcCustomTextFontPt.setText(formatFloat(preset.customTextFontPt, 2));
                } finally {
                        updatingRacFields = false;
                }

                handleOmologazionePresetSelection();
                toggleRaccomandataFields(chkRaccomandataBarcode.isSelected());
                updateOmologazioneFieldsState();
                updateRacCustomTextFieldsState();
                updateRaccomandataDerivedFields();
        }

        private void storeActiveRacPresetFromFields() {
                if (updatingRacFields || updatingOmologazioneFields)
                        return;
                storeRacPreset(currentRacPresetKey);
        }

        private void storeRacPreset(String key) {
                if (key == null)
                        return;
                RacPreset preset = racPresets.get(key);
                if (preset == null)
                        return;
                if (currentOmologazionePresetKey == null)
                        currentOmologazionePresetKey = RC_OMOLOG_PRESET_CUSTOM;
                preset.prefix = safe(txtRcPrefix.getText());
                preset.startCode = safe(txtRcStartCode.getText());
                preset.xMm = parseFloatOrDefault(txtRcXmm.getText(), preset.xMm);
                preset.yMm = parseFloatOrDefault(txtRcYmm.getText(), preset.yMm);
                preset.omologazioneEnabled = chkRcOmologazione.isSelected();
                preset.omologazionePresetKey = currentOmologazionePresetKey;
                preset.omologazioneText = safe(txtRcOmologazioneText.getText()).toUpperCase(Locale.ITALIAN);
                preset.omologazioneXmm = parseFloatOrDefault(txtRcOmologazioneXmm.getText(), preset.omologazioneXmm);
                preset.omologazioneYmm = parseFloatOrDefault(txtRcOmologazioneYmm.getText(), preset.omologazioneYmm);
                preset.omologazioneFontPt = parseFloatOrDefault(txtRcOmologazioneFontPt.getText(),
                                preset.omologazioneFontPt);
                preset.customTextEnabled = chkRcCustomText.isSelected();
                preset.customText = safe(txtRcCustomText.getText());
                preset.customTextXmm = parseFloatOrDefault(txtRcCustomTextXmm.getText(), preset.customTextXmm);
                preset.customTextYmm = parseFloatOrDefault(txtRcCustomTextYmm.getText(), preset.customTextYmm);
                preset.customTextFontPt = parseFloatOrDefault(txtRcCustomTextFontPt.getText(), preset.customTextFontPt);
        }

        private void applyDigitFilter(JTextField field, int maxLength) {
                if (field.getDocument() instanceof AbstractDocument) {
                        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DigitsDocumentFilter(maxLength));
                }
        }

        private PdfVersionChoice getSelectedPdfVersionChoice() {
                if (cmbPdfVersion == null) {
                        return PdfVersionChoice.defaultChoice();
                }
                Object selected = cmbPdfVersion.getSelectedItem();
                if (selected instanceof PdfVersionChoice) {
                        return (PdfVersionChoice) selected;
                }
                return PdfVersionChoice.defaultChoice();
        }

        private PdfVersion getSelectedPdfVersion() {
                return getSelectedPdfVersionChoice().version;
        }

        private void setUiEnabled(boolean enabled) {
                txtInput.setEnabled(enabled);
                txtOutput.setEnabled(enabled);
                txtMarker.setEnabled(enabled);
                cmbPdfVersion.setEnabled(enabled);
                chkIgnoreCase.setEnabled(enabled);
                chkNormalize.setEnabled(enabled);
                chkPdfSmartMode.setEnabled(enabled);
                chkRotateByText.setEnabled(enabled);
                chkResizeOnRotatedPages.setEnabled(enabled);
                chkApplyResize.setEnabled(enabled);
                chkForceA4BeforeResize.setEnabled(enabled);
                chkBarcode.setEnabled(enabled);
                chkRaccomandataBarcode.setEnabled(enabled);
                chkRcOmologazione.setEnabled(enabled);
                chkQrCode.setEnabled(enabled);
                chkQrCorrections.setEnabled(enabled);
                chkPageCounter.setEnabled(enabled);
                chkGroupLabel.setEnabled(enabled);
                chkAddressBlock.setEnabled(enabled);
                chkAddressKeyRead.setEnabled(enabled);
                chkRisoOptimization.setEnabled(enabled);
                chkEvolution.setEnabled(enabled);
                btnEvolutionPresetUpdate.setEnabled(enabled);

                // dipendenti
                toggleResizeFields(enabled && chkApplyResize.isSelected());
                toggleRotateByTextFields(enabled && chkRotateByText.isSelected());
                toggleBarcodeFields(enabled && chkBarcode.isSelected());
                toggleLabelFields(enabled && chkGroupLabel.isSelected());
                toggleRaccomandataFields(enabled && chkRaccomandataBarcode.isSelected());
                updateOmologazioneFieldsState();
                togglePageCounterFields(enabled && chkPageCounter.isSelected());
                toggleQrFields(enabled && chkQrCode.isSelected());
                toggleQrCorrectionsFields(enabled && chkQrCorrections.isSelected());
                toggleAddressBlockFields(enabled && chkAddressBlock.isSelected());
                toggleAddressKeyReadFields(enabled && chkAddressKeyRead.isSelected());
                toggleRisoFields(enabled && chkRisoOptimization.isSelected());
                toggleEvolutionFields(enabled && chkEvolution.isSelected());

                btnRun.setEnabled(enabled);
                btnCancel.setEnabled(!enabled && currentWorker != null && !currentWorker.isDone());
                btnReadAddress.setEnabled(enabled);
                btnImportConfig.setEnabled(enabled);
                btnExportConfig.setEnabled(enabled);
                btnInputBrowse.setEnabled(enabled);
                btnOutputBrowse.setEnabled(enabled);
                updateReticleButtonState();
        }

        private void cancelCurrentWorker() {
                SwingWorker<?, ?> worker = currentWorker;
                if (worker == null || worker.isDone()) {
                        return;
                }
                btnCancel.setEnabled(false);
                progressBar.setIndeterminate(false);
                progressBar.setString("Interruzione richiesta...");
                appendLog("Interruzione richiesta dall'utente...");
                worker.cancel(true);
        }

        // --- Util ---
        private static JTextField readOnlyField() {
                JTextField field = new JTextField();
                field.setEditable(false);
                field.setFocusable(false);
                field.setColumns(6);
                field.setHorizontalAlignment(JTextField.RIGHT);
                return field;
        }

        private JComponent createFilePickerField(JTextField field, JButton button) {
                JPanel panel = new JPanel(new BorderLayout(6, 0));
                panel.add(field, BorderLayout.CENTER);
                panel.add(button, BorderLayout.EAST);
                return panel;
        }

        private static String formatFloat(float value, int decimals) {
                return String.format(Locale.US, "%." + decimals + "f", value);
        }

        private static boolean isBlank(String s) {
                return s == null || s.trim().isEmpty();
        }

        private static String safe(String s) {
                return s == null ? "" : s.trim();
        }

        private static String nullToEmpty(String s) {
                return s == null ? "" : s;
        }

        private static float parseFloatOrDefault(String s, float defVal) {
                if (s == null)
                        return defVal;
                try {
                        return Float.parseFloat(s.trim().replace(',', '.'));
                } catch (Exception e) {
                        return defVal;
                }
        }

        private static int parseIntOrDefault(String s, int defVal) {
                if (s == null)
                        return defVal;
                try {
                        return Integer.parseInt(s.trim());
                } catch (Exception e) {
                        return defVal;
                }
        }

        private static long parseLongOrDefault(String s, long defVal) {
                if (s == null)
                        return defVal;
                try {
                        return Long.parseLong(s.trim());
                } catch (Exception e) {
                        return defVal;
                }
        }

        private static File ensureXmlExtension(File file) {
                if (file == null) {
                        return null;
                }
                String path = file.getAbsolutePath();
                if (path.toLowerCase(Locale.ITALIAN).endsWith(".xml")) {
                        return file;
                }
                return new File(path + ".xml");
        }

        private static String tempNameFor(String finalOut, String suffix) {
                String p = finalOut;
                return p.toLowerCase().endsWith(".pdf") ? p.substring(0, p.length() - 4) + suffix : p + suffix;
        }

        static boolean deleteTemporaryFile(String temporaryPath, Consumer<String> logger) {
                if (isBlank(temporaryPath)) {
                        return true;
                }
                Path path = Paths.get(temporaryPath);
                IOException lastFailure = null;
                for (int attempt = 0; attempt < 5; attempt++) {
                        try {
                                Files.deleteIfExists(path);
                                return true;
                        } catch (IOException ex) {
                                lastFailure = ex;
                                if (attempt == 4) {
                                        break;
                                }
                                try {
                                        Thread.sleep(50L * (attempt + 1));
                                } catch (InterruptedException interrupted) {
                                        Thread.currentThread().interrupt();
                                        break;
                                }
                        }
                }
                if (logger != null) {
                        String reason = lastFailure == null ? "operazione interrotta" : formatErrorMessage(lastFailure);
                        logger.accept("ATTENZIONE: impossibile eliminare il file temporaneo " + path + ": " + reason);
                }
                return false;
        }

        private static String defaultPreviewName(String base) {
                return base.toLowerCase().endsWith(".pdf") ? base.substring(0, base.length() - 4) + "_preview.pdf"
                                : base + "_preview.pdf";
        }

        private enum PdfVersionChoice {
                PDF_1_0("PDF 1.0", PdfVersion.PDF_1_0),
                PDF_1_1("PDF 1.1", PdfVersion.PDF_1_1),
                PDF_1_2("PDF 1.2", PdfVersion.PDF_1_2),
                PDF_1_3("PDF 1.3", PdfVersion.PDF_1_3),
                PDF_1_4("PDF 1.4", PdfVersion.PDF_1_4),
                PDF_1_5("PDF 1.5", PdfVersion.PDF_1_5),
                PDF_1_6("PDF 1.6", PdfVersion.PDF_1_6),
                PDF_1_7("PDF 1.7", PdfVersion.PDF_1_7),
                PDF_2_0("PDF 2.0", PdfVersion.PDF_2_0);

                final String label;
                final PdfVersion version;

                PdfVersionChoice(String label, PdfVersion version) {
                        this.label = label;
                        this.version = version;
                }

                static PdfVersionChoice defaultChoice() {
                        return PDF_1_7;
                }

                static PdfVersionChoice fromKey(String key) {
                        if (key != null) {
                                for (PdfVersionChoice choice : values()) {
                                        if (choice.name().equals(key)) {
                                                return choice;
                                        }
                                }
                        }
                        return defaultChoice();
                }

                static PdfVersionChoice fromVersion(PdfVersion version) {
                        if (version != null) {
                                for (PdfVersionChoice choice : values()) {
                                        if (choice.version == version) {
                                                return choice;
                                        }
                                }
                        }
                        return defaultChoice();
                }

                @Override
                public String toString() {
                        return label;
                }
        }

        private void autoSaveConfigurationNextTo(String targetPdfPath) {
                if (isBlank(targetPdfPath)) {
                        return;
                }
                Path pdfPath;
                try {
                        pdfPath = Paths.get(targetPdfPath).toAbsolutePath().normalize();
                } catch (Exception ex) {
                        appendLog("ERRORE salvataggio configurazione automatica: percorso non valido (" + targetPdfPath
                                        + ")");
                        return;
                }
                Path dir = pdfPath.getParent();
                if (dir == null) {
                        dir = Paths.get(".").toAbsolutePath().normalize();
                }
                try {
                        if (!Files.exists(dir)) {
                                Files.createDirectories(dir);
                        }
                        String fileName = pdfPath.getFileName().toString();
                        String baseName = fileName.toLowerCase(Locale.ITALIAN).endsWith(".pdf")
                                        ? fileName.substring(0, fileName.length() - 4)
                                        : fileName;
                        Path cfgPath = dir.resolve(baseName + "_config.xml");
                        WorkConfiguration cfg = snapshotConfiguration();
                        cfg.save(cfgPath);
                        appendLog("Configurazione salvata automaticamente: " + cfgPath);
                } catch (Exception ex) {
                        appendLog("ERRORE salvataggio configurazione automatica: " + ex.getMessage());
                }
        }

        private static final class WorkConfiguration {
                private static final String VERSION = "4";

                String version = VERSION;
                String inputPath;
                String outputPath;
                String marker;
                boolean ignoreCase;
                boolean normalize;
                boolean pdfSmartMode;
                boolean rotateByTextEnabled;
                boolean rotateApplyResizeOnMatchedPages;
                String rotateByTextString;
                String rotateByTextDegrees;
                String pdfVersionKey;

                boolean resizeEnabled;
                boolean resizeForceA4BeforeResize;
                String scalePct;
                String offsetXmm;
                String offsetYmm;

                boolean barcodeEnabled;
                boolean barcodeUseOmr;
                boolean allegatiPresenti;
                boolean barcodeOnAttachment;
                String bcXmm;
                String bcYmm;
                String bcModulePt;
                String bcBarHeightPt;
                String bcStartProg;
                boolean groupLabelEnabled;
                String labelId;
                String labelXmm;
                String labelYmm;
                boolean labelVertical;

                boolean racBarcodeEnabled;
                boolean racOmologazioneEnabled;
                String racPresetKey;
                String racXmm;
                String racYmm;
                String racHeightMm;
                String racModuleMm;
                String racHumanFontPt;
                String racHumanGapMm;
                String racPrefix;
                String racPrefixGapMm;
                String racPrefixFontPt;
                String racStartCode;
                String omologazionePresetKey;
                String omologazioneText;
                String omologazioneXmm;
                String omologazioneYmm;
                String omologazioneFontPt;
                boolean racCustomTextEnabled;
                String racCustomText;
                String racCustomTextXmm;
                String racCustomTextYmm;
                String racCustomTextFontPt;

                boolean pageCounterEnabled;
                boolean pageCounterVertical;
                String pageCounterXmm;
                String pageCounterYmm;
                String pageCounterFontPt;

                boolean qrEnabled;
                String qrBase;
                String qrDigits;
                String qrStart;
                String qrSizeMm;
                String qrXmm;
                String qrYmm;
                String qrErrorCorrection;
                boolean qrCorrectionsEnabled;
                String qrCorrectionsExcelPath;
                String qrCorrectionsXmm;
                String qrCorrectionsYmm;
                String qrCorrectionsWidthMm;
                String qrCorrectionsFontPt;
                String qrCorrectionsIconMm;

                boolean addressBlockEnabled;
                String addressXmm;
                String addressYmm;
                String addressWidthMm;
                String addressHeightMm;
                boolean addressKeyReadEnabled;
                String addressKeyString;

                boolean risoOptimizationEnabled;
                String risoRecordId;

                boolean evolutionEnabled;
                String evolutionDataMatrixXmm;
                String evolutionDataMatrixYmm;
                String evolutionDataMatrixModuleMm;
                String evolutionDataMatrixWidthCells;
                String evolutionDataMatrixHeightCells;
                String evolutionGamma;
                String evolutionSapId;
                String evolutionClientId;
                String evolutionClasse;
                String evolutionTipoProdotto;
                String evolutionCapDestFallback;
                String evolutionCodTecDest;
                String evolutionCapMitt;
                String evolutionCodTecMitt;
                String evolutionPrenFiglio;
                String evolutionStampatore;
                String evolutionStartOggetto;
                String evolutionCausale;
                String evolutionOmologazioneDm;
                String evolutionCampo16;
                String evolutionServizi;
                String evolutionExcelPath;
                boolean evolutionDuEnabled;
                String evolutionDuTipoAccettazioneFile;
                String evolutionDuProgressivo;
                String evolutionDuUtenzaOperatore;
                String evolutionDuIdPrenotazione;
                String evolutionDuDataPostalizzazione;
                String evolutionDuFrazionario;
                String evolutionDuTipologiaProdotto;
                String evolutionDuCodiceProdotto;
                String evolutionDuServizioAccessorio;
                String evolutionDuCodiceTipologiaAccettazione;
                String evolutionDuTipologiaTracciatura;
                String evolutionDuCodiceConto;
                String evolutionDuDescrizione;
                String evolutionDuCodiceOmologazione;
                String evolutionDuFormato;
                String evolutionDuIdHu;
                String evolutionDuIdScatola;

                private Properties toProperties() {
                        Properties props = new Properties();
                        put(props, "version", version);
                        put(props, "inputPath", inputPath);
                        put(props, "outputPath", outputPath);
                        put(props, "marker", marker);
                        putBoolean(props, "ignoreCase", ignoreCase);
                        putBoolean(props, "normalize", normalize);
                        putBoolean(props, "pdfSmartMode", pdfSmartMode);
                        putBoolean(props, "rotateByTextEnabled", rotateByTextEnabled);
                        putBoolean(props, "rotateApplyResizeOnMatchedPages", rotateApplyResizeOnMatchedPages);
                        put(props, "rotateByTextString", rotateByTextString);
                        put(props, "rotateByTextDegrees", rotateByTextDegrees);
                        put(props, "pdfVersionKey", pdfVersionKey);

                        putBoolean(props, "resizeEnabled", resizeEnabled);
                        putBoolean(props, "resizeForceA4BeforeResize", resizeForceA4BeforeResize);
                        put(props, "scalePct", scalePct);
                        put(props, "offsetXmm", offsetXmm);
                        put(props, "offsetYmm", offsetYmm);

                        putBoolean(props, "barcodeEnabled", barcodeEnabled);
                        putBoolean(props, "barcodeUseOmr", barcodeUseOmr);
                        putBoolean(props, "allegatiPresenti", allegatiPresenti);
                        putBoolean(props, "barcodeOnAttachment", barcodeOnAttachment);
                        put(props, "bcXmm", bcXmm);
                        put(props, "bcYmm", bcYmm);
                        put(props, "bcModulePt", bcModulePt);
                        put(props, "bcBarHeightPt", bcBarHeightPt);
                        put(props, "bcStartProg", bcStartProg);
                        putBoolean(props, "groupLabelEnabled", groupLabelEnabled);
                        put(props, "labelId", labelId);
                        put(props, "labelXmm", labelXmm);
                        put(props, "labelYmm", labelYmm);
                        putBoolean(props, "labelVertical", labelVertical);

                        putBoolean(props, "racBarcodeEnabled", racBarcodeEnabled);
                        putBoolean(props, "racOmologazioneEnabled", racOmologazioneEnabled);
                        put(props, "racPresetKey", racPresetKey);
                        put(props, "racXmm", racXmm);
                        put(props, "racYmm", racYmm);
                        put(props, "racHeightMm", racHeightMm);
                        put(props, "racModuleMm", racModuleMm);
                        put(props, "racHumanFontPt", racHumanFontPt);
                        put(props, "racHumanGapMm", racHumanGapMm);
                        put(props, "racPrefix", racPrefix);
                        put(props, "racPrefixGapMm", racPrefixGapMm);
                        put(props, "racPrefixFontPt", racPrefixFontPt);
                        put(props, "racStartCode", racStartCode);
                        put(props, "omologazionePresetKey", omologazionePresetKey);
                        put(props, "omologazioneText", omologazioneText);
                        put(props, "omologazioneXmm", omologazioneXmm);
                        put(props, "omologazioneYmm", omologazioneYmm);
                        put(props, "omologazioneFontPt", omologazioneFontPt);
                        putBoolean(props, "racCustomTextEnabled", racCustomTextEnabled);
                        put(props, "racCustomText", racCustomText);
                        put(props, "racCustomTextXmm", racCustomTextXmm);
                        put(props, "racCustomTextYmm", racCustomTextYmm);
                        put(props, "racCustomTextFontPt", racCustomTextFontPt);

                        putBoolean(props, "pageCounterEnabled", pageCounterEnabled);
                        putBoolean(props, "pageCounterVertical", pageCounterVertical);
                        put(props, "pageCounterXmm", pageCounterXmm);
                        put(props, "pageCounterYmm", pageCounterYmm);
                        put(props, "pageCounterFontPt", pageCounterFontPt);

                        putBoolean(props, "qrEnabled", qrEnabled);
                        put(props, "qrBase", qrBase);
                        put(props, "qrDigits", qrDigits);
                        put(props, "qrStart", qrStart);
                        put(props, "qrSizeMm", qrSizeMm);
                        put(props, "qrXmm", qrXmm);
                        put(props, "qrYmm", qrYmm);
                        put(props, "qrErrorCorrection", qrErrorCorrection);
                        putBoolean(props, "qrCorrectionsEnabled", qrCorrectionsEnabled);
                        put(props, "qrCorrectionsExcelPath", qrCorrectionsExcelPath);
                        put(props, "qrCorrectionsXmm", qrCorrectionsXmm);
                        put(props, "qrCorrectionsYmm", qrCorrectionsYmm);
                        put(props, "qrCorrectionsWidthMm", qrCorrectionsWidthMm);
                        put(props, "qrCorrectionsFontPt", qrCorrectionsFontPt);
                        put(props, "qrCorrectionsIconMm", qrCorrectionsIconMm);
                        putBoolean(props, "addressBlockEnabled", addressBlockEnabled);
                        put(props, "addressXmm", addressXmm);
                        put(props, "addressYmm", addressYmm);
                        put(props, "addressWidthMm", addressWidthMm);
                        put(props, "addressHeightMm", addressHeightMm);
                        putBoolean(props, "addressKeyReadEnabled", addressKeyReadEnabled);
                        put(props, "addressKeyString", addressKeyString);
                        putBoolean(props, "risoOptimizationEnabled", risoOptimizationEnabled);
                        put(props, "risoRecordId", risoRecordId);
                        putBoolean(props, "evolutionEnabled", evolutionEnabled);
                        put(props, "evolutionDataMatrixXmm", evolutionDataMatrixXmm);
                        put(props, "evolutionDataMatrixYmm", evolutionDataMatrixYmm);
                        put(props, "evolutionDataMatrixModuleMm", evolutionDataMatrixModuleMm);
                        put(props, "evolutionDataMatrixWidthCells", evolutionDataMatrixWidthCells);
                        put(props, "evolutionDataMatrixHeightCells", evolutionDataMatrixHeightCells);
                        put(props, "evolutionGamma", evolutionGamma);
                        put(props, "evolutionSapId", evolutionSapId);
                        put(props, "evolutionClientId", evolutionClientId);
                        put(props, "evolutionClasse", evolutionClasse);
                        put(props, "evolutionTipoProdotto", evolutionTipoProdotto);
                        put(props, "evolutionCapDestFallback", evolutionCapDestFallback);
                        put(props, "evolutionCodTecDest", evolutionCodTecDest);
                        put(props, "evolutionCapMitt", evolutionCapMitt);
                        put(props, "evolutionCodTecMitt", evolutionCodTecMitt);
                        put(props, "evolutionPrenFiglio", evolutionPrenFiglio);
                        put(props, "evolutionStampatore", evolutionStampatore);
                        put(props, "evolutionStartOggetto", evolutionStartOggetto);
                        put(props, "evolutionCausale", evolutionCausale);
                        put(props, "evolutionOmologazioneDm", evolutionOmologazioneDm);
                        put(props, "evolutionCampo16", evolutionCampo16);
                        put(props, "evolutionServizi", evolutionServizi);
                        put(props, "evolutionExcelPath", evolutionExcelPath);
                        putBoolean(props, "evolutionDuEnabled", evolutionDuEnabled);
                        put(props, "evolutionDuTipoAccettazioneFile", evolutionDuTipoAccettazioneFile);
                        put(props, "evolutionDuProgressivo", evolutionDuProgressivo);
                        put(props, "evolutionDuUtenzaOperatore", evolutionDuUtenzaOperatore);
                        put(props, "evolutionDuIdPrenotazione", evolutionDuIdPrenotazione);
                        put(props, "evolutionDuDataPostalizzazione", evolutionDuDataPostalizzazione);
                        put(props, "evolutionDuFrazionario", evolutionDuFrazionario);
                        put(props, "evolutionDuTipologiaProdotto", evolutionDuTipologiaProdotto);
                        put(props, "evolutionDuCodiceProdotto", evolutionDuCodiceProdotto);
                        put(props, "evolutionDuServizioAccessorio", evolutionDuServizioAccessorio);
                        put(props, "evolutionDuCodiceTipologiaAccettazione", evolutionDuCodiceTipologiaAccettazione);
                        put(props, "evolutionDuTipologiaTracciatura", evolutionDuTipologiaTracciatura);
                        put(props, "evolutionDuCodiceConto", evolutionDuCodiceConto);
                        put(props, "evolutionDuDescrizione", evolutionDuDescrizione);
                        put(props, "evolutionDuCodiceOmologazione", evolutionDuCodiceOmologazione);
                        put(props, "evolutionDuFormato", evolutionDuFormato);
                        put(props, "evolutionDuIdHu", evolutionDuIdHu);
                        put(props, "evolutionDuIdScatola", evolutionDuIdScatola);
                        return props;
                }

                void save(Path path) throws IOException {
                        try (OutputStream os = Files.newOutputStream(path)) {
                                toProperties().storeToXML(os, "Configurazione imbustatrice", "UTF-8");
                        }
                }

                static WorkConfiguration load(Path path) throws IOException {
                        Properties props = new Properties();
                        try (InputStream is = Files.newInputStream(path)) {
                                props.loadFromXML(is);
                        }
                        return fromProperties(props);
                }

                private static WorkConfiguration fromProperties(Properties props) {
                        WorkConfiguration cfg = new WorkConfiguration();
                        cfg.version = props.getProperty("version", VERSION);
                        cfg.inputPath = props.getProperty("inputPath", "");
                        cfg.outputPath = props.getProperty("outputPath", "");
                        cfg.marker = props.getProperty("marker", "");
                        cfg.ignoreCase = getBoolean(props, "ignoreCase", false);
                        cfg.normalize = getBoolean(props, "normalize", false);
                        cfg.pdfSmartMode = getBoolean(props, "pdfSmartMode", true);
                        cfg.rotateByTextEnabled = getBoolean(props, "rotateByTextEnabled", false);
                        cfg.rotateApplyResizeOnMatchedPages = getBoolean(props,
                                        "rotateApplyResizeOnMatchedPages", true);
                        cfg.rotateByTextString = props.getProperty("rotateByTextString", "");
                        cfg.rotateByTextDegrees = props.getProperty("rotateByTextDegrees", "90");
                        cfg.pdfVersionKey = props.getProperty("pdfVersionKey",
                                        PdfVersionChoice.defaultChoice().name());

                        cfg.resizeEnabled = getBoolean(props, "resizeEnabled", false);
                        cfg.resizeForceA4BeforeResize = getBoolean(props, "resizeForceA4BeforeResize", false);
                        cfg.scalePct = props.getProperty("scalePct", "97");
                        cfg.offsetXmm = props.getProperty("offsetXmm", "5");
                        cfg.offsetYmm = props.getProperty("offsetYmm", "5");

                        cfg.barcodeEnabled = getBoolean(props, "barcodeEnabled", false);
                        cfg.barcodeUseOmr = getBoolean(props, "barcodeUseOmr", false);
                        cfg.allegatiPresenti = getBoolean(props, "allegatiPresenti", false);
                        cfg.barcodeOnAttachment = getBoolean(props, "barcodeOnAttachment", false);
                        cfg.bcXmm = props.getProperty("bcXmm", "0");
                        cfg.bcYmm = props.getProperty("bcYmm", "100");
                        cfg.bcModulePt = props.getProperty("bcModulePt",
                                        formatFloat(Imbustatrice.BarcodeStandard.MODULE_WIDTH_PT, 2));
                        cfg.bcBarHeightPt = props.getProperty("bcBarHeightPt",
                                        formatFloat(Imbustatrice.BarcodeStandard.BAR_HEIGHT_PT, 2));
                        cfg.bcStartProg = props.getProperty("bcStartProg", "1");
                        cfg.groupLabelEnabled = getBoolean(props, "groupLabelEnabled", true);
                        cfg.labelId = props.getProperty("labelId", "");
                        cfg.labelXmm = props.getProperty("labelXmm", "10");
                        cfg.labelYmm = props.getProperty("labelYmm", "270");
                        cfg.labelVertical = getBoolean(props, "labelVertical", true);

                        cfg.racBarcodeEnabled = getBoolean(props, "racBarcodeEnabled", false);
                        cfg.racOmologazioneEnabled = getBoolean(props, "racOmologazioneEnabled", false);
                        cfg.racPresetKey = props.getProperty("racPresetKey", RAC_PRESET_AR);
                        cfg.racXmm = props.getProperty("racXmm", "30");
                        cfg.racYmm = props.getProperty("racYmm", "225");
                        cfg.racHeightMm = props.getProperty("racHeightMm",
                                        formatFloat(Imbustatrice.RaccomandataStandard.BAR_HEIGHT_MM, 1));
                        cfg.racModuleMm = props.getProperty("racModuleMm",
                                        formatFloat(Imbustatrice.RaccomandataStandard.NARROW_MODULE_MM, 2));
                        cfg.racHumanFontPt = readFontPt(props,
                                        "racHumanFontPt",
                                        "racHumanFontMm",
                                        Imbustatrice.RaccomandataStandard.HUMAN_READABLE_FONT_MM,
                                        Imbustatrice.RaccomandataStandard.HUMAN_READABLE_FONT_PT);
                        cfg.racHumanGapMm = props.getProperty("racHumanGapMm",
                                        formatFloat(Imbustatrice.RaccomandataStandard.HUMAN_READABLE_GAP_MM, 1));
                        cfg.racPrefix = props.getProperty("racPrefix", "R");
                        cfg.racPrefixGapMm = props.getProperty("racPrefixGapMm",
                                        formatFloat(Imbustatrice.RaccomandataStandard.PREFIX_GAP_MM, 1));
                        cfg.racPrefixFontPt = readFontPt(props,
                                        "racPrefixFontPt",
                                        "racPrefixFontMm",
                                        Imbustatrice.RaccomandataStandard.PREFIX_FONT_MM,
                                        Imbustatrice.RaccomandataStandard.PREFIX_FONT_PT);
                        cfg.racStartCode = props.getProperty("racStartCode", "61000000001");
                        cfg.omologazionePresetKey = props.getProperty("omologazionePresetKey", RC_OMOLOG_PRESET_CUSTOM);
                        cfg.omologazioneText = props.getProperty("omologazioneText", "");
                        cfg.omologazioneXmm = props.getProperty("omologazioneXmm", "110");
                        cfg.omologazioneYmm = props.getProperty("omologazioneYmm", "240");
                        cfg.omologazioneFontPt = readFontPt(props,
                                        "omologazioneFontPt",
                                        "omologazioneFontMm",
                                        Imbustatrice.RaccomandataStandard.OMOLOGAZIONE_FONT_MM,
                                        Imbustatrice.RaccomandataStandard.OMOLOGAZIONE_FONT_PT);
                        cfg.racCustomTextEnabled = getBoolean(props, "racCustomTextEnabled", false);
                        cfg.racCustomText = props.getProperty("racCustomText", RAC_RETURN_TEXT_DEFAULT);
                        cfg.racCustomTextXmm = props.getProperty("racCustomTextXmm", "20");
                        cfg.racCustomTextYmm = props.getProperty("racCustomTextYmm", "248");
                        cfg.racCustomTextFontPt = readFontPt(props,
                                        "racCustomTextFontPt",
                                        "racCustomTextFontMm",
                                        Imbustatrice.RaccomandataStandard.CUSTOM_TEXT_FONT_MM,
                                        Imbustatrice.RaccomandataStandard.CUSTOM_TEXT_FONT_PT);

                        cfg.pageCounterEnabled = getBoolean(props, "pageCounterEnabled", true);
                        cfg.pageCounterVertical = getBoolean(props, "pageCounterVertical", true);
                        cfg.pageCounterXmm = props.getProperty("pageCounterXmm", "5");
                        cfg.pageCounterYmm = props.getProperty("pageCounterYmm", "15");
                        cfg.pageCounterFontPt = props.getProperty("pageCounterFontPt", "6");

                        cfg.qrEnabled = getBoolean(props, "qrEnabled", false);
                        cfg.qrBase = props.getProperty("qrBase", "acquisisci da sito postanetwork");
                        cfg.qrDigits = props.getProperty("qrDigits", "6");
                        cfg.qrStart = props.getProperty("qrStart", "1");
                        cfg.qrSizeMm = props.getProperty("qrSizeMm", "8");
                        cfg.qrXmm = props.getProperty("qrXmm", "170");
                        cfg.qrYmm = props.getProperty("qrYmm", "240");
                        cfg.qrErrorCorrection = props.getProperty("qrErrorCorrection", "M");
                        cfg.qrCorrectionsEnabled = getBoolean(props, "qrCorrectionsEnabled",
                                        QR_CODE_DEFAULTS.getBoolean("correctionsEnabled"));
                        cfg.qrCorrectionsExcelPath = props.getProperty("qrCorrectionsExcelPath",
                                        QR_CODE_DEFAULTS.getString("correctionsExcelPath"));
                        cfg.qrCorrectionsXmm = props.getProperty("qrCorrectionsXmm",
                                        QR_CODE_DEFAULTS.getString("correctionsXmm"));
                        cfg.qrCorrectionsYmm = props.getProperty("qrCorrectionsYmm",
                                        QR_CODE_DEFAULTS.getString("correctionsYmm"));
                        cfg.qrCorrectionsWidthMm = props.getProperty("qrCorrectionsWidthMm",
                                        QR_CODE_DEFAULTS.getString("correctionsWidthMm"));
                        cfg.qrCorrectionsFontPt = props.getProperty("qrCorrectionsFontPt",
                                        QR_CODE_DEFAULTS.getString("correctionsFontPt"));
                        cfg.qrCorrectionsIconMm = props.getProperty("qrCorrectionsIconMm",
                                        QR_CODE_DEFAULTS.getString("correctionsIconMm"));

                        cfg.addressBlockEnabled = getBoolean(props, "addressBlockEnabled", false);
                        cfg.addressXmm = props.getProperty("addressXmm", "20");
                        cfg.addressYmm = props.getProperty("addressYmm", "120");
                        cfg.addressWidthMm = props.getProperty("addressWidthMm", "90");
                        cfg.addressHeightMm = props.getProperty("addressHeightMm", "45");
                        cfg.addressKeyReadEnabled = getBoolean(props, "addressKeyReadEnabled",
                                        ADDRESS_BLOCK_DEFAULTS.getBoolean("keyReadEnabled"));
                        cfg.addressKeyString = props.getProperty("addressKeyString",
                                        ADDRESS_BLOCK_DEFAULTS.getString("keyString"));
                        cfg.risoOptimizationEnabled = getBoolean(props, "risoOptimizationEnabled", false);
                        cfg.risoRecordId = props.getProperty("risoRecordId", "");
                        cfg.evolutionEnabled = getBoolean(props, "evolutionEnabled",
                                        EVOLUTION_DEFAULTS.getBoolean("enabled"));
                        cfg.evolutionDataMatrixXmm = props.getProperty("evolutionDataMatrixXmm",
                                        formatFloat(EVOLUTION_DEFAULTS.getFloat("dataMatrixXmm"), 2));
                        cfg.evolutionDataMatrixYmm = props.getProperty("evolutionDataMatrixYmm",
                                        formatFloat(EVOLUTION_DEFAULTS.getFloat("dataMatrixYmm"), 2));
                        cfg.evolutionDataMatrixModuleMm = props.getProperty("evolutionDataMatrixModuleMm",
                                        formatFloat(EVOLUTION_DEFAULTS.getFloat("dataMatrixModuleMm"), 3));
                        cfg.evolutionDataMatrixWidthCells = props.getProperty("evolutionDataMatrixWidthCells",
                                        EVOLUTION_DEFAULTS.getString("dataMatrixWidthCells"));
                        cfg.evolutionDataMatrixHeightCells = props.getProperty("evolutionDataMatrixHeightCells",
                                        EVOLUTION_DEFAULTS.getString("dataMatrixHeightCells"));
                        cfg.evolutionGamma = props.getProperty("evolutionGamma",
                                        EVOLUTION_DEFAULTS.getString("gamma"));
                        cfg.evolutionSapId = props.getProperty("evolutionSapId", EVOLUTION_DEFAULTS.getString("sapId"));
                        cfg.evolutionClientId = props.getProperty("evolutionClientId",
                                        EVOLUTION_DEFAULTS.getString("clientId"));
                        cfg.evolutionClasse = props.getProperty("evolutionClasse",
                                        EVOLUTION_DEFAULTS.getString("classe"));
                        cfg.evolutionTipoProdotto = props.getProperty("evolutionTipoProdotto",
                                        EVOLUTION_DEFAULTS.getString("tipoProdotto"));
                        cfg.evolutionCapDestFallback = props.getProperty("evolutionCapDestFallback",
                                        EVOLUTION_DEFAULTS.getString("capDestFallback"));
                        cfg.evolutionCodTecDest = props.getProperty("evolutionCodTecDest",
                                        EVOLUTION_DEFAULTS.getString("codTecDest"));
                        cfg.evolutionCapMitt = props.getProperty("evolutionCapMitt",
                                        EVOLUTION_DEFAULTS.getString("capMitt"));
                        cfg.evolutionCodTecMitt = props.getProperty("evolutionCodTecMitt",
                                        EVOLUTION_DEFAULTS.getString("codTecMitt"));
                        cfg.evolutionPrenFiglio = props.getProperty("evolutionPrenFiglio",
                                        EVOLUTION_DEFAULTS.getString("idPrenFiglio"));
                        cfg.evolutionStampatore = props.getProperty("evolutionStampatore",
                                        EVOLUTION_DEFAULTS.getString("idStampatore"));
                        cfg.evolutionStartOggetto = props.getProperty("evolutionStartOggetto",
                                        Long.toString(EVOLUTION_DEFAULTS.getLong("startOggetto")));
                        cfg.evolutionCausale = props.getProperty("evolutionCausale",
                                        EVOLUTION_DEFAULTS.getString("causale"));
                        cfg.evolutionOmologazioneDm = props.getProperty("evolutionOmologazioneDm",
                                        EVOLUTION_DEFAULTS.getString("omologazioneDm"));
                        cfg.evolutionCampo16 = props.getProperty("evolutionCampo16",
                                        EVOLUTION_DEFAULTS.getString("campo16"));
                        cfg.evolutionServizi = props.getProperty("evolutionServizi",
                                        EVOLUTION_DEFAULTS.getString("servizi"));
                        cfg.evolutionExcelPath = props.getProperty("evolutionExcelPath",
                                        EVOLUTION_DEFAULTS.getString("excelPath"));
                        cfg.evolutionDuEnabled = getBoolean(props, "evolutionDuEnabled",
                                        EVOLUTION_DEFAULTS.getBoolean("duEnabled"));
                        cfg.evolutionDuTipoAccettazioneFile = props.getProperty("evolutionDuTipoAccettazioneFile",
                                        EVOLUTION_DEFAULTS.getString("duTipoAccettazioneFile"));
                        cfg.evolutionDuProgressivo = props.getProperty("evolutionDuProgressivo",
                                        EVOLUTION_DEFAULTS.getString("duProgressivo"));
                        cfg.evolutionDuUtenzaOperatore = props.getProperty("evolutionDuUtenzaOperatore",
                                        EVOLUTION_DEFAULTS.getString("duUtenzaOperatore"));
                        cfg.evolutionDuIdPrenotazione = props.getProperty("evolutionDuIdPrenotazione",
                                        EVOLUTION_DEFAULTS.getString("duIdPrenotazione"));
                        cfg.evolutionDuDataPostalizzazione = props.getProperty("evolutionDuDataPostalizzazione",
                                        EVOLUTION_DEFAULTS.getString("duDataPostalizzazione"));
                        cfg.evolutionDuFrazionario = props.getProperty("evolutionDuFrazionario",
                                        EVOLUTION_DEFAULTS.getString("duFrazionario"));
                        cfg.evolutionDuTipologiaProdotto = props.getProperty("evolutionDuTipologiaProdotto",
                                        EVOLUTION_DEFAULTS.getString("duTipologiaProdotto"));
                        cfg.evolutionDuCodiceProdotto = props.getProperty("evolutionDuCodiceProdotto",
                                        EVOLUTION_DEFAULTS.getString("duCodiceProdotto"));
                        cfg.evolutionDuServizioAccessorio = props.getProperty("evolutionDuServizioAccessorio",
                                        EVOLUTION_DEFAULTS.getString("duCodiceServizioAccessorio"));
                        cfg.evolutionDuCodiceTipologiaAccettazione = props.getProperty(
                                        "evolutionDuCodiceTipologiaAccettazione",
                                        EVOLUTION_DEFAULTS.getString("duCodiceTipologiaAccettazione"));
                        cfg.evolutionDuTipologiaTracciatura = props.getProperty("evolutionDuTipologiaTracciatura",
                                        EVOLUTION_DEFAULTS.getString("duTipologiaTracciatura"));
                        cfg.evolutionDuCodiceConto = props.getProperty("evolutionDuCodiceConto",
                                        EVOLUTION_DEFAULTS.getString("duCodiceConto"));
                        cfg.evolutionDuDescrizione = props.getProperty("evolutionDuDescrizione",
                                        EVOLUTION_DEFAULTS.getString("duDescrizione"));
                        cfg.evolutionDuCodiceOmologazione = props.getProperty("evolutionDuCodiceOmologazione",
                                        EVOLUTION_DEFAULTS.getString("duCodiceOmologazione"));
                        cfg.evolutionDuFormato = props.getProperty("evolutionDuFormato",
                                        EVOLUTION_DEFAULTS.getString("duFormato"));
                        cfg.evolutionDuIdHu = props.getProperty("evolutionDuIdHu",
                                        EVOLUTION_DEFAULTS.getString("duIdHu"));
                        cfg.evolutionDuIdScatola = props.getProperty("evolutionDuIdScatola",
                                        EVOLUTION_DEFAULTS.getString("duIdScatola"));

                        return cfg;
                }

                private static void put(Properties props, String key, String value) {
                        props.setProperty(key, value == null ? "" : value);
                }

                private static void putBoolean(Properties props, String key, boolean value) {
                        props.setProperty(key, Boolean.toString(value));
                }

                private static boolean getBoolean(Properties props, String key, boolean defaultValue) {
                        String value = props.getProperty(key);
                        if (value == null) {
                                return defaultValue;
                        }
                        return Boolean.parseBoolean(value);
                }

                private static String readFontPt(Properties props, String ptKey, String legacyMmKey,
                                float legacyDefaultMm, float defaultPt) {
                        String ptValue = props.getProperty(ptKey);
                        if (ptValue != null && !ptValue.trim().isEmpty()) {
                                return ptValue;
                        }
                        String mmValue = props.getProperty(legacyMmKey);
                        if (mmValue != null && !mmValue.trim().isEmpty()) {
                                float mm = parseFloatOrDefault(mmValue, legacyDefaultMm);
                                return formatFloat(Imbustatrice.mm(mm), 2);
                        }
                        return formatFloat(defaultPt, 2);
                }
        }

        private static final class RacPreset {
                String prefix;
                String startCode;
                float xMm;
                float yMm;
                boolean omologazioneEnabled;
                String omologazionePresetKey;
                String omologazioneText;
                float omologazioneXmm;
                float omologazioneYmm;
                float omologazioneFontPt;
                boolean customTextEnabled;
                String customText;
                float customTextXmm;
                float customTextYmm;
                float customTextFontPt;

                RacPreset(String prefix, String startCode, float xMm, float yMm) {
                        this.prefix = prefix;
                        this.startCode = startCode;
                        this.xMm = xMm;
                        this.yMm = yMm;
                        this.omologazioneEnabled = false;
                        this.omologazionePresetKey = RC_OMOLOG_PRESET_CUSTOM;
                        this.omologazioneText = "";
                        this.omologazioneXmm = 110f;
                        this.omologazioneYmm = 240f;
                        this.omologazioneFontPt = Imbustatrice.RaccomandataStandard.OMOLOGAZIONE_FONT_PT;
                        this.customTextEnabled = false;
                        this.customText = RAC_RETURN_TEXT_DEFAULT;
                        this.customTextXmm = 20f;
                        this.customTextYmm = 248f;
                        this.customTextFontPt = Imbustatrice.RaccomandataStandard.CUSTOM_TEXT_FONT_PT;
                }
        }

        private static final class EvolutionPreset {
                boolean enabled;
                String dataMatrixXmm;
                String dataMatrixYmm;
                String dataMatrixModuleMm;
                String dataMatrixWidthCells;
                String dataMatrixHeightCells;
                String gamma;
                String sapId;
                String clientId;
                String classe;
                String tipoProdotto;
                String capDestFallback;
                String codTecDest;
                String capMitt;
                String codTecMitt;
                String idPrenFiglio;
                String idStampatore;
                String startOggetto;
                String causale;
                String omologazioneDm;
                String campo16;
                String servizi;
                String excelPath;
                boolean duEnabled;
                String duTipoAccettazioneFile;
                String duProgressivo;
                String duUtenzaOperatore;
                String duIdPrenotazione;
                String duDataPostalizzazione;
                String duFrazionario;
                String duTipologiaProdotto;
                String duCodiceProdotto;
                String duServizioAccessorio;
                String duCodiceTipologiaAccettazione;
                String duTipologiaTracciatura;
                String duCodiceConto;
                String duDescrizione;
                String duCodiceOmologazione;
                String duFormato;
                String duIdHu;
                String duIdScatola;

                EvolutionPreset() {
                }

                EvolutionPreset(EvolutionPreset other) {
                        this.enabled = other.enabled;
                        this.dataMatrixXmm = other.dataMatrixXmm;
                        this.dataMatrixYmm = other.dataMatrixYmm;
                        this.dataMatrixModuleMm = other.dataMatrixModuleMm;
                        this.dataMatrixWidthCells = other.dataMatrixWidthCells;
                        this.dataMatrixHeightCells = other.dataMatrixHeightCells;
                        this.gamma = other.gamma;
                        this.sapId = other.sapId;
                        this.clientId = other.clientId;
                        this.classe = other.classe;
                        this.tipoProdotto = other.tipoProdotto;
                        this.capDestFallback = other.capDestFallback;
                        this.codTecDest = other.codTecDest;
                        this.capMitt = other.capMitt;
                        this.codTecMitt = other.codTecMitt;
                        this.idPrenFiglio = other.idPrenFiglio;
                        this.idStampatore = other.idStampatore;
                        this.startOggetto = other.startOggetto;
                        this.causale = other.causale;
                        this.omologazioneDm = other.omologazioneDm;
                        this.campo16 = other.campo16;
                        this.servizi = other.servizi;
                        this.excelPath = other.excelPath;
                        this.duEnabled = other.duEnabled;
                        this.duTipoAccettazioneFile = other.duTipoAccettazioneFile;
                        this.duProgressivo = other.duProgressivo;
                        this.duUtenzaOperatore = other.duUtenzaOperatore;
                        this.duIdPrenotazione = other.duIdPrenotazione;
                        this.duDataPostalizzazione = other.duDataPostalizzazione;
                        this.duFrazionario = other.duFrazionario;
                        this.duTipologiaProdotto = other.duTipologiaProdotto;
                        this.duCodiceProdotto = other.duCodiceProdotto;
                        this.duServizioAccessorio = other.duServizioAccessorio;
                        this.duCodiceTipologiaAccettazione = other.duCodiceTipologiaAccettazione;
                        this.duTipologiaTracciatura = other.duTipologiaTracciatura;
                        this.duCodiceConto = other.duCodiceConto;
                        this.duDescrizione = other.duDescrizione;
                        this.duCodiceOmologazione = other.duCodiceOmologazione;
                        this.duFormato = other.duFormato;
                        this.duIdHu = other.duIdHu;
                        this.duIdScatola = other.duIdScatola;
                }
        }

        private static final class DigitsDocumentFilter extends DocumentFilter {
                private final int maxLength;

                DigitsDocumentFilter(int maxLength) {
                        this.maxLength = Math.max(1, maxLength);
                }

                @Override
                public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                                throws BadLocationException {
                        replace(fb, offset, 0, string, attr);
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                                throws BadLocationException {
                        String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                        int newLength = Math.max(0, current.length() - length);
                        String sanitized = sanitize(text);
                        int available = maxLength - newLength;
                        if (available < 0) {
                                available = 0;
                        }
                        if (sanitized.length() > available) {
                                sanitized = sanitized.substring(0, available);
                        }
                        if (newLength + sanitized.length() <= maxLength) {
                                super.replace(fb, offset, length, sanitized, attrs);
                        }
                }

                @Override
                public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                        super.remove(fb, offset, length);
                }

                private String sanitize(String input) {
                        if (input == null || input.isEmpty()) {
                                return "";
                        }
                        String digitsOnly = input.replaceAll("\\D", "");
                        if (digitsOnly.length() > maxLength) {
                                return digitsOnly.substring(0, maxLength);
                        }
                        return digitsOnly;
                }
        }

        public static void main(String[] args) {
                forceUtf8DefaultCharset();
                AppLauncher.configureGraphicsProperties();
                installGlobalExceptionHandler();
                if (!Boolean.getBoolean(AppLauncher.LAUNCHER_ACTIVE_PROPERTY)) {
                        AppLauncher.main(args);
                        return;
                }
                try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }
                EventQueue.invokeLater(() -> new App().setVisible(true));
        }

        static void installGlobalExceptionHandler() {
                Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                        if (isHeapSpaceError(throwable)) {
                                showHeapSpaceError(null);
                        } else {
                                throwable.printStackTrace();
                        }
                });
        }

        private static boolean isHeapSpaceError(Throwable throwable) {
                Throwable current = throwable;
                while (current != null) {
                        if (current instanceof OutOfMemoryError) {
                                String message = current.getMessage();
                                return message == null || message.toLowerCase(Locale.ITALIAN).contains("heap space");
                        }
                        current = current.getCause();
                }
                return false;
        }

        private static void showHeapSpaceError(Component parent) {
                if (!HEAP_ERROR_DIALOG_VISIBLE.compareAndSet(false, true)) {
                        return;
                }
                System.gc();
                SwingUtilities.invokeLater(() -> {
                        try {
                                JOptionPane.showMessageDialog(parent, HEAP_ERROR_MESSAGE,
                                                HEAP_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
                        } finally {
                                HEAP_ERROR_DIALOG_VISIBLE.set(false);
                        }
                });
        }

        private void handleHeapSpaceError(String context) {
                appendLog(context + ": memoria insufficiente (Java heap space).");
                SwingUtilities.invokeLater(() -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setString("Memoria insufficiente");
                });
                showHeapSpaceError(this);
        }

        private static void requestMemoryCleanup() {
                Thread cleanupThread = new Thread(() -> {
                        System.gc();
                        try {
                                Thread.sleep(250L);
                        } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                return;
                        }
                        System.gc();
                }, "workpdf-memory-cleanup");
                cleanupThread.setDaemon(true);
                cleanupThread.start();
        }

        private static void forceUtf8DefaultCharset() {
                System.setProperty("file.encoding", "UTF-8");
                System.setProperty("native.encoding", "UTF-8");
                System.setProperty("sun.jnu.encoding", "UTF-8");
                try {
                        Field defaultCharset = Charset.class.getDeclaredField("defaultCharset");
                        defaultCharset.setAccessible(true);
                        defaultCharset.set(null, null);
                } catch (Exception ignored) {
                }
        }

        private static String formatErrorMessage(Exception ex) {
                Throwable cause = ex;
                while (cause != null) {
                        if (cause instanceof UnsupportedCharsetException
                                        && "MacRoman".equalsIgnoreCase(
                                                        ((UnsupportedCharsetException) cause).getCharsetName())) {
                                return "MacRoman: il runtime dell'EXE non include il modulo jdk.charsets. "
                                                + "Rigenera l'installer con jpackage --add-modules jdk.charsets.";
                        }
                        cause = cause.getCause();
                }
                String message = ex.getMessage();
                return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
        }

        private void appendLog(String s) {
                SwingUtilities.invokeLater(() -> {
                        txtLog.append(s + System.lineSeparator());
                        txtLog.setCaretPosition(txtLog.getDocument().getLength());
                });
        }
}
