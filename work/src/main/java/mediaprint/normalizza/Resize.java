package mediaprint.normalizza;

import mediaprint.imbustatrice.Imbustatrice;

/** Wrapper opzionale: delega il resize contenuto alla utility Imbustatrice. */
public class Resize {

    /**
     * Scala e trasla il contenuto mantenendo dimensioni pagina invariate.
     * Vedi Imbustatrice.scaleAndTranslateContent per dettagli.
     */
    public static void scaleAndTranslateContent(
            String inputPath,
            String outputPath,
            float scalePct,
            float offsetXpt,
            float offsetYpt
    ) throws Exception {

    }
    

    /** Utility: mm → pt (1 mm ≈ 2.8346457 pt). */
    public static float mm(float mm) { return Imbustatrice.mm(mm); }
    
    
    
}
