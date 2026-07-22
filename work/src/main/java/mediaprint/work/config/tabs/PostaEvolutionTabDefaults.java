package mediaprint.work.config.tabs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default values for the "Posta Evolution" tab.
 */
public final class PostaEvolutionTabDefaults implements TabDefaults {

    private static final PostaEvolutionTabDefaults INSTANCE = new PostaEvolutionTabDefaults();

    private final Map<String, Object> defaults;

    private PostaEvolutionTabDefaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", Boolean.FALSE);
        values.put("dataMatrixXmm", 120f);
        values.put("dataMatrixYmm", 250f);
        values.put("dataMatrixModuleMm", 0.508f);
        values.put("dataMatrixWidthCells", 48);
        values.put("dataMatrixHeightCells", 16);
        values.put("gamma", "B");
        values.put("sapId", "30037962");
        values.put("clientId", "DIC");
        values.put("classe", "1");
        values.put("tipoProdotto", "W");
        values.put("capDestFallback", "");
        values.put("codTecDest", "");
        values.put("capMitt", "64015");
        values.put("codTecMitt", "");
        values.put("idPrenFiglio", "00000");
        values.put("idStampatore", "EE");
        values.put("startOggetto", 1L);
        values.put("causale", "000");
        values.put("omologazioneDm", "OS2065");
        values.put("campo16", "");
        values.put("servizi", "OOOOOOOO");
        values.put("excelPath", "");
        values.put("duEnabled", Boolean.TRUE);
        values.put("duTipoAccettazioneFile", "G");
        values.put("duProgressivo", "01");
        values.put("duUtenzaOperatore", "daniele.sciarretta.mediaprint");
        values.put("duIdPrenotazione", "0000000");
        values.put("duDataPostalizzazione", "aaaa-mm-gg");
        values.put("duFrazionario", "05141");
        values.put("duTipologiaProdotto", "B");
        values.put("duCodiceProdotto", "65");
        values.put("duCodiceServizioAccessorio", "CT");
        values.put("duCodiceTipologiaAccettazione", "SMA");
        values.put("duTipologiaTracciatura", "1");
        values.put("duCodiceConto", "30037962-007");
        values.put("duDescrizione", "Stampa di prova");
        values.put("duCodiceOmologazione", "DCOOS2065");
        values.put("duFormato", "P");
        values.put("duIdHu", "bancale");
        values.put("duIdScatola", "");
        defaults = Collections.unmodifiableMap(values);
    }

    public static PostaEvolutionTabDefaults getInstance() {
        return INSTANCE;
    }

    @Override
    public String tabName() {
        return "Posta Evolution";
    }

    @Override
    public Map<String, Object> parameters() {
        return defaults;
    }
}
