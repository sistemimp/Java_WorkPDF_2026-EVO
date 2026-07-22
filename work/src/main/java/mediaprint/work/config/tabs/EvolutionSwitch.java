package mediaprint.work.config.tabs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class EvolutionSwitch {

    public static final String EXTERNAL_PRESETS_DIR = "preset-evolution";
    public static final String REMOTE_PRESETS_URL =
            "https://gestionale.mediaprint.it/ElectronAppUpdate/WorkPDF/preset-evolution/index.php";
    public static final String PRESET_MASSIVA = "Posta Massiva";
    private static final int REMOTE_TIMEOUT_MS = 5000;
    private static final Pattern REMOTE_CONF_LINK = Pattern.compile("(?i)href\\s*=\\s*['\"]([^'\"]+\\.conf)['\"]");
    private static final Pattern REMOTE_CONF_TEXT = Pattern.compile("(?i)([A-Za-z0-9_. -]+\\.conf)");
    private static final Pattern REMOTE_DOWNLOAD_URL = Pattern.compile(
            "(?i)\"downloadUrl\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern REMOTE_FILE_NAME = Pattern.compile("(?i)\"fileName\"\\s*:\\s*\"([^\"]+\\.conf)\"");

    private EvolutionSwitch() {
    }

    public static Map<String, PresetConfig> available(PostaEvolutionTabDefaults defaults) {
        return available(defaults, externalPresetsDir(), true);
    }

    public static Map<String, PresetConfig> availableLocal(PostaEvolutionTabDefaults defaults) {
        return available(defaults, externalPresetsDir(), false);
    }

    static Map<String, PresetConfig> available(PostaEvolutionTabDefaults defaults, Path externalDir) {
        return available(defaults, externalDir, false);
    }

    private static Map<String, PresetConfig> available(PostaEvolutionTabDefaults defaults, Path externalDir,
            boolean syncRemote) {
        LinkedHashMap<String, PresetConfig> configs = new LinkedHashMap<>();

        PresetConfig massiva = PresetConfig.fromDefaults(defaults);
        configs.put(PRESET_MASSIVA, massiva);

        loadExternalPresets(configs, defaults, externalDir, syncRemote);
        return configs;
    }

    public static SyncResult syncExternalPresets() {
        return syncExternalPresetsDir(externalPresetsDir());
    }

    private static Path externalPresetsDir() {
        String override = System.getProperty("mediaprint.evolution.presets.dir");
        if (override != null && !override.trim().isEmpty()) {
            return Paths.get(override.trim());
        }
        return installDir().resolve(EXTERNAL_PRESETS_DIR);
    }

    private static Path installDir() {
        try {
            CodeSource source = EvolutionSwitch.class.getProtectionDomain().getCodeSource();
            if (source != null && source.getLocation() != null) {
                Path location = Paths.get(source.getLocation().toURI()).toAbsolutePath();
                if (Files.isRegularFile(location)) {
                    return location.getParent();
                }
                return location;
            }
        } catch (URISyntaxException | IllegalArgumentException e) {
            // Fall through to user.dir.
        }
        return Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
    }

    private static void loadExternalPresets(LinkedHashMap<String, PresetConfig> configs,
            PostaEvolutionTabDefaults defaults,
            Path externalDir,
            boolean syncRemote) {
        if (syncRemote) {
            syncExternalPresetsDir(externalDir);
        }
        if (externalDir == null || !Files.isDirectory(externalDir)) {
            return;
        }
        List<ExternalPreset> externalPresets = new ArrayList<>();
        try (Stream<Path> files = Files.list(externalDir)) {
            files.filter(Files::isRegularFile)
                    .filter(EvolutionSwitch::isPresetFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .map(EvolutionSwitch::readExternalPreset)
                    .filter(preset -> preset != null)
                    .forEach(externalPresets::add);
        } catch (IOException e) {
            // Preset esterni opzionali: in caso di errore restano disponibili quelli integrati.
        }
        applyExternalPresets(configs, defaults, externalPresets);
    }

    private static SyncResult syncExternalPresetsDir(Path externalDir) {
        if (externalDir == null) {
            return SyncResult.failed("Cartella preset non valida.");
        }
        try {
            Files.createDirectories(externalDir);
            return syncRemotePresets(externalDir);
        } catch (IOException e) {
            return SyncResult.failed(e.getMessage());
        }
    }

    private static SyncResult syncRemotePresets(Path externalDir) throws IOException {
        URL baseUrl = new URL(remotePresetsUrl());
        String index = readRemoteText(baseUrl);
        int downloaded = 0;
        int updated = 0;
        int unchanged = 0;
        int failed = 0;
        for (RemotePresetFile remotePreset : parseRemotePresetLinks(index, baseUrl)) {
            URL presetUrl = remotePreset.url;
            Path target = externalDir.resolve(remotePreset.fileName).normalize();
            if (!target.startsWith(externalDir)) {
                continue;
            }
            try (InputStream in = openRemoteStream(presetUrl)) {
                byte[] remoteBytes = in.readAllBytes();
                if (isPresetChanged(target, remoteBytes)) {
                    boolean existed = Files.exists(target);
                    Files.write(target, remoteBytes);
                    if (existed) {
                        updated++;
                    } else {
                        downloaded++;
                    }
                } else {
                    unchanged++;
                }
            } catch (IOException e) {
                failed++;
            }
        }
        return SyncResult.completed(downloaded, updated, unchanged, failed);
    }

    private static boolean isPresetChanged(Path target, byte[] remoteBytes) throws IOException {
        if (!Files.exists(target) || Files.size(target) != remoteBytes.length) {
            return true;
        }
        return !Arrays.equals(Files.readAllBytes(target), remoteBytes);
    }

    private static String remotePresetsUrl() {
        String override = System.getProperty("mediaprint.evolution.presets.url");
        String url = override == null || override.trim().isEmpty() ? REMOTE_PRESETS_URL : override.trim();
        if (!url.contains("://")) {
            url = "https://" + url;
        }
        if (url.contains("?") || url.toLowerCase().endsWith(".php")) {
            return url;
        }
        return url.endsWith("/") ? url : url + "/";
    }

    private static String readRemoteText(URL url) throws IOException {
        try (InputStream in = openRemoteStream(url)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InputStream openRemoteStream(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(REMOTE_TIMEOUT_MS);
        connection.setReadTimeout(REMOTE_TIMEOUT_MS);
        return connection.getInputStream();
    }

    private static List<RemotePresetFile> parseRemotePresetLinks(String index, URL baseUrl) throws IOException {
        List<RemotePresetFile> jsonPresets = parseJsonRemotePresets(index, baseUrl);
        if (!jsonPresets.isEmpty()) {
            return jsonPresets;
        }

        Set<String> links = new LinkedHashSet<>();
        Matcher hrefMatcher = REMOTE_CONF_LINK.matcher(index);
        while (hrefMatcher.find()) {
            links.add(hrefMatcher.group(1));
        }
        Matcher textMatcher = REMOTE_CONF_TEXT.matcher(index);
        while (textMatcher.find()) {
            links.add(textMatcher.group(1));
        }
        List<RemotePresetFile> presets = new ArrayList<>();
        for (String link : links) {
            URL url = new URL(baseUrl, decodeJsonString(link));
            presets.add(new RemotePresetFile(fileNameFromUrl(url), url));
        }
        return presets;
    }

    private static List<RemotePresetFile> parseJsonRemotePresets(String index, URL baseUrl) throws IOException {
        List<String> downloadUrls = new ArrayList<>();
        Matcher urlMatcher = REMOTE_DOWNLOAD_URL.matcher(index);
        while (urlMatcher.find()) {
            downloadUrls.add(decodeJsonString(urlMatcher.group(1)));
        }
        if (downloadUrls.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> fileNames = new ArrayList<>();
        Matcher fileMatcher = REMOTE_FILE_NAME.matcher(index);
        while (fileMatcher.find()) {
            fileNames.add(decodeJsonString(fileMatcher.group(1)));
        }

        List<RemotePresetFile> presets = new ArrayList<>();
        for (int i = 0; i < downloadUrls.size(); i++) {
            URL url = new URL(baseUrl, downloadUrls.get(i));
            String fileName = i < fileNames.size() ? fileNames.get(i) : fileNameFromUrl(url);
            presets.add(new RemotePresetFile(fileName, url));
        }
        return presets;
    }

    private static String decodeJsonString(String value) {
        return value.replace("\\/", "/").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String fileNameFromUrl(URL url) {
        String path = url.getPath();
        int slash = path.lastIndexOf('/');
        String fileName = slash >= 0 ? path.substring(slash + 1) : path;
        return URLDecoder.decode(fileName, StandardCharsets.UTF_8);
    }

    public static final class SyncResult {
        public final boolean success;
        public final int downloaded;
        public final int updated;
        public final int unchanged;
        public final int failed;
        public final String errorMessage;

        private SyncResult(boolean success, int downloaded, int updated, int unchanged, int failed,
                String errorMessage) {
            this.success = success;
            this.downloaded = downloaded;
            this.updated = updated;
            this.unchanged = unchanged;
            this.failed = failed;
            this.errorMessage = errorMessage;
        }

        private static SyncResult completed(int downloaded, int updated, int unchanged, int failed) {
            return new SyncResult(true, downloaded, updated, unchanged, failed, "");
        }

        private static SyncResult failed(String errorMessage) {
            return new SyncResult(false, 0, 0, 0, 0, errorMessage == null ? "" : errorMessage);
        }
    }

    private static boolean isPresetFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".conf") || fileName.endsWith(".xlm") || fileName.endsWith(".xml");
    }

    private static ExternalPreset readExternalPreset(Path path) {
        Properties props = loadPresetProperties(path);
        if (props == null) {
            return null;
        }
        String presetName = firstNonBlank(props.getProperty("name"), props.getProperty("nome"),
                props.getProperty("presetName"), fileNameWithoutExtension(path));
        String basePreset = firstNonBlank(props.getProperty("basePreset"), props.getProperty("base"),
                PRESET_MASSIVA);
        return new ExternalPreset(presetName, basePreset, props);
    }

    private static void applyExternalPresets(LinkedHashMap<String, PresetConfig> configs,
            PostaEvolutionTabDefaults defaults,
            List<ExternalPreset> externalPresets) {
        List<ExternalPreset> pending = new ArrayList<>(externalPresets);
        boolean progressed;
        do {
            progressed = false;
            for (Iterator<ExternalPreset> it = pending.iterator(); it.hasNext();) {
                ExternalPreset preset = it.next();
                if (canApplyExternalPreset(configs, pending, preset)) {
                    applyExternalPreset(configs, defaults, preset);
                    it.remove();
                    progressed = true;
                }
            }
        } while (progressed && !pending.isEmpty());

        for (ExternalPreset preset : pending) {
            applyExternalPreset(configs, defaults, preset);
        }
    }

    private static boolean canApplyExternalPreset(LinkedHashMap<String, PresetConfig> configs,
            List<ExternalPreset> pending,
            ExternalPreset preset) {
        if (preset.name.equals(preset.basePreset) || configs.containsKey(preset.basePreset)) {
            return true;
        }
        for (ExternalPreset other : pending) {
            if (other != preset && other.name.equals(preset.basePreset)) {
                return false;
            }
        }
        return true;
    }

    private static void applyExternalPreset(LinkedHashMap<String, PresetConfig> configs,
            PostaEvolutionTabDefaults defaults,
            ExternalPreset preset) {
        PresetConfig base = configs.get(preset.basePreset);
        PresetConfig cfg = base == null ? PresetConfig.fromDefaults(defaults) : new PresetConfig(base);
        applyProperties(cfg, preset.props);
        configs.put(preset.name, cfg);
    }

    private static Properties loadPresetProperties(Path path) {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.loadFromXML(in);
            return props;
        } catch (IOException | IllegalArgumentException e) {
            return loadSimpleXmlProperties(path);
        }
    }

    private static Properties loadSimpleXmlProperties(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(in);
            Element root = document.getDocumentElement();
            Properties props = new Properties();
            copyAttributes(root, props);
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node instanceof Element) {
                    props.setProperty(node.getNodeName(), node.getTextContent() == null ? "" : node.getTextContent());
                }
            }
            return props;
        } catch (Exception e) {
            return null;
        }
    }

    private static void copyAttributes(Element element, Properties props) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            props.setProperty(attribute.getNodeName(), attribute.getNodeValue());
        }
    }

    private static String fileNameWithoutExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static void applyProperties(PresetConfig cfg, Properties props) {
        cfg.enabled = readBoolean(props, cfg.enabled, "enabled", "evolutionEnabled");
        cfg.dataMatrixXmm = readFloat(props, cfg.dataMatrixXmm, "dataMatrixXmm", "evolutionDataMatrixXmm");
        cfg.dataMatrixYmm = readFloat(props, cfg.dataMatrixYmm, "dataMatrixYmm", "evolutionDataMatrixYmm");
        cfg.dataMatrixModuleMm = readFloat(props, cfg.dataMatrixModuleMm, "dataMatrixModuleMm",
                "evolutionDataMatrixModuleMm");
        cfg.dataMatrixWidthCells = readInt(props, cfg.dataMatrixWidthCells, "dataMatrixWidthCells",
                "evolutionDataMatrixWidthCells");
        cfg.dataMatrixHeightCells = readInt(props, cfg.dataMatrixHeightCells, "dataMatrixHeightCells",
                "evolutionDataMatrixHeightCells");
        cfg.gamma = readString(props, cfg.gamma, "gamma", "evolutionGamma");
        cfg.sapId = readString(props, cfg.sapId, "sapId", "evolutionSapId");
        cfg.clientId = readString(props, cfg.clientId, "clientId", "evolutionClientId");
        cfg.classe = readString(props, cfg.classe, "classe", "evolutionClasse");
        cfg.tipoProdotto = readString(props, cfg.tipoProdotto, "tipoProdotto", "evolutionTipoProdotto");
        cfg.capDestFallback = readString(props, cfg.capDestFallback, "capDestFallback",
                "evolutionCapDestFallback");
        cfg.codTecDest = readString(props, cfg.codTecDest, "codTecDest", "evolutionCodTecDest");
        cfg.capMitt = readString(props, cfg.capMitt, "capMitt", "evolutionCapMitt");
        cfg.codTecMitt = readString(props, cfg.codTecMitt, "codTecMitt", "evolutionCodTecMitt");
        cfg.idPrenFiglio = readString(props, cfg.idPrenFiglio, "idPrenFiglio", "evolutionPrenFiglio");
        cfg.idStampatore = readString(props, cfg.idStampatore, "idStampatore", "evolutionStampatore");
        cfg.startOggetto = readLong(props, cfg.startOggetto, "startOggetto", "evolutionStartOggetto");
        cfg.causale = readString(props, cfg.causale, "causale", "evolutionCausale");
        cfg.omologazioneDm = readString(props, cfg.omologazioneDm, "omologazioneDm",
                "evolutionOmologazioneDm");
        cfg.campo16 = readString(props, cfg.campo16, "campo16", "evolutionCampo16");
        cfg.servizi = readString(props, cfg.servizi, "servizi", "evolutionServizi");
        cfg.excelPath = readString(props, cfg.excelPath, "excelPath", "evolutionExcelPath");
        cfg.duEnabled = readBoolean(props, cfg.duEnabled, "duEnabled", "evolutionDuEnabled");
        cfg.duTipoAccettazioneFile = readString(props, cfg.duTipoAccettazioneFile, "duTipoAccettazioneFile",
                "evolutionDuTipoAccettazioneFile");
        cfg.duProgressivo = readString(props, cfg.duProgressivo, "duProgressivo", "evolutionDuProgressivo");
        cfg.duUtenzaOperatore = readString(props, cfg.duUtenzaOperatore, "duUtenzaOperatore",
                "evolutionDuUtenzaOperatore");
        cfg.duIdPrenotazione = readString(props, cfg.duIdPrenotazione, "duIdPrenotazione",
                "evolutionDuIdPrenotazione");
        cfg.duDataPostalizzazione = readString(props, cfg.duDataPostalizzazione, "duDataPostalizzazione",
                "evolutionDuDataPostalizzazione");
        cfg.duFrazionario = readString(props, cfg.duFrazionario, "duFrazionario", "evolutionDuFrazionario");
        cfg.duTipologiaProdotto = readString(props, cfg.duTipologiaProdotto, "duTipologiaProdotto",
                "evolutionDuTipologiaProdotto");
        cfg.duCodiceProdotto = readString(props, cfg.duCodiceProdotto, "duCodiceProdotto",
                "evolutionDuCodiceProdotto");
        cfg.duServizioAccessorio = readString(props, cfg.duServizioAccessorio, "duServizioAccessorio",
                "duCodiceServizioAccessorio", "evolutionDuServizioAccessorio");
        cfg.duCodiceTipologiaAccettazione = readString(props, cfg.duCodiceTipologiaAccettazione,
                "duCodiceTipologiaAccettazione", "evolutionDuCodiceTipologiaAccettazione");
        cfg.duTipologiaTracciatura = readString(props, cfg.duTipologiaTracciatura, "duTipologiaTracciatura",
                "evolutionDuTipologiaTracciatura");
        cfg.duCodiceConto = readString(props, cfg.duCodiceConto, "duCodiceConto", "evolutionDuCodiceConto");
        cfg.duDescrizione = readString(props, cfg.duDescrizione, "duDescrizione", "evolutionDuDescrizione");
        cfg.duCodiceOmologazione = readString(props, cfg.duCodiceOmologazione, "duCodiceOmologazione",
                "evolutionDuCodiceOmologazione");
        cfg.duFormato = readString(props, cfg.duFormato, "duFormato", "evolutionDuFormato");
        cfg.duIdHu = readString(props, cfg.duIdHu, "duIdHu", "evolutionDuIdHu");
        cfg.duIdScatola = readString(props, cfg.duIdScatola, "duIdScatola", "evolutionDuIdScatola");
    }

    private static String readString(Properties props, String defaultValue, String... keys) {
        String value = readRaw(props, keys);
        return value == null ? defaultValue : value.trim();
    }

    private static boolean readBoolean(Properties props, boolean defaultValue, String... keys) {
        String value = readRaw(props, keys);
        return value == null ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    private static float readFloat(Properties props, float defaultValue, String... keys) {
        String value = readRaw(props, keys);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int readInt(Properties props, int defaultValue, String... keys) {
        String value = readRaw(props, keys);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static long readLong(Properties props, long defaultValue, String... keys) {
        String value = readRaw(props, keys);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String readRaw(Properties props, String... keys) {
        for (String key : keys) {
            String value = props.getProperty(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static final class ExternalPreset {
        private final String name;
        private final String basePreset;
        private final Properties props;

        private ExternalPreset(String name, String basePreset, Properties props) {
            this.name = name;
            this.basePreset = basePreset;
            this.props = props;
        }
    }

    private static final class RemotePresetFile {
        private final String fileName;
        private final URL url;

        private RemotePresetFile(String fileName, URL url) {
            this.fileName = fileName;
            this.url = url;
        }
    }

    public static final class PresetConfig {
        public boolean enabled;
        public float dataMatrixXmm;
        public float dataMatrixYmm;
        public float dataMatrixModuleMm;
        public int dataMatrixWidthCells;
        public int dataMatrixHeightCells;
        public String gamma;
        public String sapId;
        public String clientId;
        public String classe;
        public String tipoProdotto;
        public String capDestFallback;
        public String codTecDest;
        public String capMitt;
        public String codTecMitt;
        public String idPrenFiglio;
        public String idStampatore;
        public long startOggetto;
        public String causale;
        public String omologazioneDm;
        public String campo16;
        public String servizi;
        public String excelPath;
        public boolean duEnabled;
        public String duTipoAccettazioneFile;
        public String duProgressivo;
        public String duUtenzaOperatore;
        public String duIdPrenotazione;
        public String duDataPostalizzazione;
        public String duFrazionario;
        public String duTipologiaProdotto;
        public String duCodiceProdotto;
        public String duServizioAccessorio;
        public String duCodiceTipologiaAccettazione;
        public String duTipologiaTracciatura;
        public String duCodiceConto;
        public String duDescrizione;
        public String duCodiceOmologazione;
        public String duFormato;
        public String duIdHu;
        public String duIdScatola;

        public PresetConfig() {
        }

        public PresetConfig(PresetConfig other) {
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

        private static PresetConfig fromDefaults(PostaEvolutionTabDefaults defaults) {
            PresetConfig cfg = new PresetConfig();
            cfg.enabled = defaults.getBoolean("enabled");
            cfg.dataMatrixXmm = defaults.getFloat("dataMatrixXmm");
            cfg.dataMatrixYmm = defaults.getFloat("dataMatrixYmm");
            cfg.dataMatrixModuleMm = defaults.getFloat("dataMatrixModuleMm");
            cfg.dataMatrixWidthCells = defaults.getInt("dataMatrixWidthCells");
            cfg.dataMatrixHeightCells = defaults.getInt("dataMatrixHeightCells");
            cfg.gamma = defaults.getString("gamma");
            cfg.sapId = defaults.getString("sapId");
            cfg.clientId = defaults.getString("clientId");
            cfg.classe = defaults.getString("classe");
            cfg.tipoProdotto = defaults.getString("tipoProdotto");
            cfg.capDestFallback = defaults.getString("capDestFallback");
            cfg.codTecDest = defaults.getString("codTecDest");
            cfg.capMitt = defaults.getString("capMitt");
            cfg.codTecMitt = defaults.getString("codTecMitt");
            cfg.idPrenFiglio = defaults.getString("idPrenFiglio");
            cfg.idStampatore = defaults.getString("idStampatore");
            cfg.startOggetto = defaults.getLong("startOggetto");
            cfg.causale = defaults.getString("causale");
            cfg.omologazioneDm = defaults.getString("omologazioneDm");
            cfg.campo16 = defaults.getString("campo16");
            cfg.servizi = defaults.getString("servizi");
            cfg.excelPath = defaults.getString("excelPath");
            cfg.duEnabled = defaults.getBoolean("duEnabled");
            cfg.duTipoAccettazioneFile = defaults.getString("duTipoAccettazioneFile");
            cfg.duProgressivo = defaults.getString("duProgressivo");
            cfg.duUtenzaOperatore = defaults.getString("duUtenzaOperatore");
            cfg.duIdPrenotazione = defaults.getString("duIdPrenotazione");
            cfg.duDataPostalizzazione = defaults.getString("duDataPostalizzazione");
            cfg.duFrazionario = defaults.getString("duFrazionario");
            cfg.duTipologiaProdotto = defaults.getString("duTipologiaProdotto");
            cfg.duCodiceProdotto = defaults.getString("duCodiceProdotto");
            cfg.duServizioAccessorio = defaults.getString("duCodiceServizioAccessorio");
            cfg.duCodiceTipologiaAccettazione = defaults.getString("duCodiceTipologiaAccettazione");
            cfg.duTipologiaTracciatura = defaults.getString("duTipologiaTracciatura");
            cfg.duCodiceConto = defaults.getString("duCodiceConto");
            cfg.duDescrizione = defaults.getString("duDescrizione");
            cfg.duCodiceOmologazione = defaults.getString("duCodiceOmologazione");
            cfg.duFormato = defaults.getString("duFormato");
            cfg.duIdHu = defaults.getString("duIdHu");
            cfg.duIdScatola = defaults.getString("duIdScatola");
            return cfg;
        }
    }
}
