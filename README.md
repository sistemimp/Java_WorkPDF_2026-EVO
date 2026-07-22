# Java WorkPDF 2026 EVO

Workspace Java/Maven per l'elaborazione di PDF destinati a flussi di stampa, imbustamento e postalizzazione. L'applicazione principale e' una GUI Swing che usa iText 7, Apache POI e ZXing per normalizzare PDF, raggruppare documenti duplex, applicare codici e produrre report operativi.

## Funzionalita' principali

- **Interfaccia desktop Swing**: selezione PDF di input/output, esecuzione asincrona, barra di avanzamento, log operativo, interruzione dell'elaborazione e scelta della versione PDF di output.
- **Raggruppamento duplex per marker**: ricerca una stringa marker nel PDF, divide il documento in gruppi, mantiene il fronte/retro, aggiunge pagine bianche quando un gruppo ha numero dispari di pagine e copia eventuali pagine iniziali prima del primo gruppo.
- **Ricerca testo configurabile**: supporto a ricerca case-insensitive e normalizzazione di accenti/diacritici per marker e rotazioni basate su testo.
- **Rotazione pagina da stringa**: individua una pagina contenente una stringa configurata e la ruota di 90, 180 o 270 gradi; puo' applicare resize anche alle pagine ruotate.
- **Resize contenuto PDF**: scala e trasla il contenuto del PDF dopo la normalizzazione, con opzione per forzare il formato A4 prima del resize.
- **Barcode imbustatrice**: inserisce barcode Code 39 verticali sui fronti dei gruppi, con progressivo di gruppo, gestione allegati, target PDF allegato, modulo/altezza barre configurabili e posizione in millimetri.
- **OMR imbustatrice**: alternativa al barcode C39 con tacche OMR per lettura ottica, inclusa gestione pattern esteso per allegati.
- **Etichetta gruppo/ID lavorazione**: stampa un'etichetta di gruppo indipendente dal barcode, con progressivo e ID lavorazione, orientamento orizzontale o verticale.
- **Barcode Raccomandata**: genera barcode Interleaved 2 of 5 con identificativo a 11 cifre, check digit, testo leggibile, prefisso prodotto e preset AR/AG/personalizzato.
- **Omologazione postale**: aggiunge testi di omologazione con preset per Posta massiva, Raccomandata, AG o valore personalizzato.
- **Testo personalizzato Raccomandata**: consente di inserire righe aggiuntive, ad esempio testo di restituzione per inesitati, con posizione e font configurabili.
- **Contatore pagine gruppo**: applica numerazione per gruppo/pagine con posizione, font e orientamento configurabili.
- **QR Code progressivi**: genera QR code per gruppo con testo base, numero di cifre, progressivo iniziale, dimensione, posizione e livello di correzione errore.
- **Correzioni QR da Excel**: legge file XLSX con progressivi e campi di correzione, poi applica overlay testuali o icone di ricerca/attenzione sul PDF.
- **Blocco indirizzo**: legge un'area del PDF per estrarre nominativo, indirizzo, CAP, comune e provincia; include una modalita' "solo lettura indirizzo".
- **Lettura stringa chiave**: esporta in `lettura.xlsx` le righe estratte da tutte le pagine in base a una stringa chiave.
- **Posta Evolution**: genera DataMatrix per gruppo con dati mittente/destinatario, codici tecnici, omologazione, servizi accessori e progressivi; puo' usare indirizzi estratti dal PDF o caricati da Excel.
- **Report Posta Evolution DU**: genera un file `.DU` con i dati richiesti dal flusso Evolution, quando abilitato.
- **Preset Posta Evolution**: include il preset "Posta Massiva" e supporta preset esterni `.conf`/XML nella cartella `preset-evolution`, con sincronizzazione remota configurata in `EvolutionSwitch`.
- **Ottimizzazione Riso GL9730**: converte il PDF in PDF/A-3B con output intent sRGB e metadato `RecordID`, pensato per la stampante Riso GL9730.
- **Anteprima reticolo**: crea un PDF di anteprima con griglia in millimetri, righelli, croce di posizionamento e riquadri di preview per barcode, Raccomandata, QR e DataMatrix.
- **Report di elaborazione Excel**: produce `DettaglioElaborazione.xlsx` con dettaglio gruppi, pagine, indirizzi, QR, Raccomandata e DataMatrix.
- **Import/export configurazione**: salva e ricarica i parametri della GUI in file XML/properties, inclusi valori dei tab e preset selezionati.
- **Gestione font Unicode**: carica font compatibili dal sistema e mantiene cache per documento PDF.
- **Avvio UTF-8**: `AppLauncher` forza la codifica UTF-8 per ridurre problemi runtime con charset e packaging.

## Struttura della workspace

- `work/pom.xml`: progetto Maven, dipendenze e configurazione del jar eseguibile tramite `maven-shade-plugin`.
- `work/src/main/java/mediaprint/work/App.java`: GUI Swing e orchestrazione dei flussi di elaborazione.
- `work/src/main/java/mediaprint/work/AppLauncher.java`: entry point del jar.
- `work/src/main/java/mediaprint/normalizza/PdfDuplexGrouper.java`: raggruppamento duplex, lettura indirizzi, report Excel, DataMatrix Evolution e report DU.
- `work/src/main/java/mediaprint/imbustatrice/Imbustatrice.java`: rendering di barcode, OMR, QR, DataMatrix, contatori, testi e reticoli.
- `work/src/main/java/mediaprint/normalizza/RisoOptimizer.java`: conversione PDF/A-3B e metadati Riso GL9730.
- `work/src/main/java/mediaprint/work/config/tabs/`: default e preset dei tab della GUI.
- `work/src/main/resources/`: logo e PDF di esempio/risorsa.
- `work/src/test/java/`: test JUnit presenti nel progetto.

## Comandi utili

Da eseguire nella cartella `work`:

```powershell
mvn test
mvn exec:java
mvn package
```

Il packaging produce un jar shaded con main class `mediaprint.work.AppLauncher`.
