package mediaprint.work.config.tabs;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

public class EvolutionSwitchTest extends TestCase {

    public void testLoadsExternalXlmPreset() throws Exception {
        Path dir = Files.createTempDirectory("evolution-presets");
        Path presetFile = dir.resolve("NuovoPreset.xlm");

        Properties props = new Properties();
        props.setProperty("name", "Nuovo Preset");
        props.setProperty("basePreset", "Target Basic");
        props.setProperty("gamma", "Z");
        props.setProperty("sapId", "12345678");
        props.setProperty("duCodiceProdotto", "999");
        props.setProperty("enabled", "true");

        try (OutputStream out = Files.newOutputStream(presetFile)) {
            props.storeToXML(out, "Preset Posta Evolution", "UTF-8");
        }

        Map<String, EvolutionSwitch.PresetConfig> presets = EvolutionSwitch
                .available(PostaEvolutionTabDefaults.getInstance(), dir);
        EvolutionSwitch.PresetConfig loaded = presets.get("Nuovo Preset");

        assertNotNull(loaded);
        assertTrue(loaded.enabled);
        assertEquals("Z", loaded.gamma);
        assertEquals("12345678", loaded.sapId);
        assertEquals("999", loaded.duCodiceProdotto);
        assertEquals("DIC", loaded.clientId);
    }

    public void testLoadsSimpleXmlPreset() throws Exception {
        Path dir = Files.createTempDirectory("evolution-presets-simple");
        Path presetFile = dir.resolve("Semplice.conf");

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<preset name=\"Preset Semplice\" basePreset=\"" + EvolutionSwitch.PRESET_MASSIVA + "\">\n"
                + "  <gamma>Y</gamma>\n"
                + "  <sapId>87654321</sapId>\n"
                + "  <duCodiceProdotto>123</duCodiceProdotto>\n"
                + "</preset>\n";
        Files.write(presetFile, xml.getBytes("UTF-8"));

        Map<String, EvolutionSwitch.PresetConfig> presets = EvolutionSwitch
                .available(PostaEvolutionTabDefaults.getInstance(), dir);
        EvolutionSwitch.PresetConfig loaded = presets.get("Preset Semplice");

        assertNotNull(loaded);
        assertEquals("Y", loaded.gamma);
        assertEquals("87654321", loaded.sapId);
        assertEquals("123", loaded.duCodiceProdotto);
    }
}
