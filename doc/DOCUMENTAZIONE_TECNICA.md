# WorkPDF 2026 EVO - Documentazione tecnica

## 1. Panoramica

Il repository contiene un'applicazione desktop Java 11/Maven, versione 1.1.3,
con UI Swing. Il codice e' organizzato come un singolo modulo `work`; non sono
presenti servizi server o database. Input, output, configurazioni e report sono
file locali, salvo la sincronizzazione opzionale dei preset Posta Evolution.

Dipendenze principali:

| Libreria | Versione | Impiego |
|---|---:|---|
| iText Core/PDF-A | 9.2.0 | lettura, copia, modifica, barcode e PDF/A |
| Apache POI OOXML | 5.4.1 | import/export XLSX |
| ZXing Core | 3.5.3 | codifica QR/DataMatrix |
| JUnit | 3.8.1 | test legacy |

`commons-io` 2.20.0 e `commons-codec` 1.20.0 sono fissate esplicitamente nel
POM. Maven segnala che l'artefatto `itext7-core` e' stato ricollocato a
`itext-core`; la build corrente resta funzionante.

## 2. Struttura del codice

| Percorso | Responsabilita' |
|---|---|
| `mediaprint.work.AppLauncher` | entry point del JAR e tentativo di forzare UTF-8 |
| `mediaprint.work.App` | costruzione UI, validazione, configurazione e orchestrazione asincrona |
| `mediaprint.normalizza.PdfDuplexGrouper` | ricerca marker, gruppi duplex, estrazioni, report XLSX/DU |
| `mediaprint.imbustatrice.Imbustatrice` | rendering e trasformazioni PDF |
| `mediaprint.normalizza.RisoOptimizer` | conversione PDF/A-3B e `RecordID` |
| `mediaprint.normalizza.Resize` | wrapper incompleto, attualmente non usato dalla UI |
| `mediaprint.work.config.tabs` | singleton dei default e gestione preset Evolution |
| `src/main/resources` | logo, icona e PDF tecnico di riferimento |

`App.java` e' il principale punto di accoppiamento: contiene componenti Swing,
DTO di configurazione, listener e conversione dei campi UI negli oggetti opzione.

## 3. Avvio e threading

Il manifest shaded punta a `mediaprint.work.AppLauncher`, che imposta proprieta'
di encoding e azzera via reflection la cache del charset predefinito, quindi
delega ad `App.main`. La finestra Swing viene creata sull'Event Dispatch Thread.

Le elaborazioni lunghe usano `SwingWorker<Void,String>`. La progress bar riceve
la property `progress`; il pulsante di interruzione invoca `cancel(true)` e le
routine verificano un `BooleanSupplier` nei punti principali. Gli aggiornamenti
del log sono inoltrati alla UI.

## 4. Pipeline di elaborazione

`App.runProcess()` esegue validazione e costruisce gli oggetti opzione. Nel
`SwingWorker` l'ordine e' il seguente:

1. `Imbustatrice.scaleAndTranslateContent`: resize, eventuale A4 e rotazione per
   testo, su `<output>_resize_tmp.pdf`.
2. `PdfDuplexGrouper.process`: ricerca marker, copia gruppi, pagine bianche,
   overlay e report, direttamente sul PDF finale.
3. `RisoOptimizer.optimize`: conversione del PDF finale in un temporaneo PDF/A,
   poi sostituzione atomica logica tramite `Files.move(REPLACE_EXISTING)`.
4. `exportKeyedStringsToExcel`: eventuale `lettura.xlsx`, leggendo il PDF input.
5. eliminazione best-effort dei temporanei.
6. salvataggio di `<base-output>_config.xml` al completamento.

Il resize precede gli overlay: le coordinate dei codici si riferiscono quindi
alla geometria della pagina gia' trasformata.

## 5. Algoritmo duplex

`PdfDuplexGrouper.process` apre sorgente e destinazione iText e ricava il testo
pagina per pagina. `preprocess` puo':

- decomporre Unicode NFD e rimuovere i combining mark;
- convertire in minuscolo con locale italiano;
- sostituire NBSP e comprimere gli spazi orizzontali.

Gli indici 1-based delle pagine contenenti il marker formano `groupStarts`.
Le pagine precedenti al primo indice sono copiate come prefazione. Per ogni
gruppo viene copiato l'intervallo fino all'indice successivo meno uno; un numero
dispari causa l'aggiunta di una pagina A4 vuota.

L'ordine degli interventi sul gruppo e':

1. barcode Code 39 oppure OMR ed etichetta;
2. barcode/testi Raccomandata;
3. QR Code;
4. overlay correzione;
5. contatori pagina;
6. estrazione/associazione indirizzo;
7. DataMatrix Posta Evolution;
8. accumulo dati per report.

Al termine vengono scritti report XLSX e, se richiesto, DU.

## 6. Coordinate e unita'

La UI espone prevalentemente millimetri. `Imbustatrice.mm` converte con
`72 / 25.4` punti per millimetro. I campi derivati in punti sono di sola lettura.
Attenzione alle diverse convenzioni dichiarate nell'interfaccia: alcuni Y sono
misurati dall'alto, mentre le primitive PDF usano l'origine in basso a sinistra;
la conversione avviene nelle routine di rendering specifiche.

Costanti significative:

- Code 39: larghezza target 40 mm, modulo 1.2 pt, altezza 15 pt;
- OMR: 7 bit base, pattern esteso con bit MC/selezione;
- Raccomandata: 11 cifre identificative, modulo 0.30 mm, altezza 14 mm,
  rapporto largo/stretto 2.5;
- compressione PDF impostata a livello 6.

## 7. Modelli opzione

Le API di dominio ricevono DTO immutabili annidati:

- `BarcodeOpts`;
- `RaccomandataBarcodeOpts`;
- `PageCounterOpts`;
- `QrCodeOpts` e `CorrectionOverlayOpts`;
- `ResizePageRotationOpts`;
- `PostaEvolutionDataMatrixOpts`;
- `AddressBlockOpts`, `PageRotationByTextOpts` e `PostaEvolutionOpts`;
- `RisoOptimizer.Options`.

La convenzione e' passare `null` per una funzione completamente inattiva oppure
un oggetto con flag `enabled`. In alcune aree, per esempio etichetta e testo di
omologazione, l'oggetto deve esistere anche con barcode principale disabilitato.

## 8. Barcode e progressivi

### Imbustatrice

`applyBarcodesToGroup` lavora sui fronti del gruppo normalizzato. Il progressivo
effettivo e' `groupStartProgressive + groupIndex - 1`. OMR costruisce un pattern
binario con parita' pari e varianti per ultima pagina e allegati.

### Raccomandata

Il codice usa un identificativo iniziale a 11 cifre, incrementato per gruppo.
`computeRaccomandataCheckDigit` calcola la cifra di controllo; report e rendering
mantengono sia versione formattata sia sole cifre.

### QR e correzioni

Il QR concatena base e progressivo zero-padded. Il limite UI e' 18 cifre. Le
correzioni sono caricate in una mappa indicizzata dal progressivo normalizzato;
il testo viene adattato alla larghezza disponibile e puo' essere affiancato da
icone di ricerca/attenzione.

## 9. Indirizzi ed Excel

L'estrazione PDF usa una regione rettangolare della pagina iniziale del gruppo e
interpreta le righe in `AddressComponents`: `rawText`, nominativo, indirizzo,
CAP, comune e provincia.

Con Posta Evolution e un XLSX configurato, `loadAddressComponentsFromExcel`
identifica le colonne tramite intestazioni normalizzate e alias, con fallback a
indici noti. Le righe sono associate in ordine ai gruppi; righe insufficienti
generano `IllegalStateException`.

`DettaglioElaborazione.xlsx`, foglio di dettaglio, contiene:

1. numero gruppo;
2. pagina sorgente iniziale e finale;
3. numero pagine normalizzato, inclusa bianca;
4. testo e componenti indirizzo;
5. valore QR;
6. Raccomandata formattata e senza trattino.

`exportKeyedStringsToExcel` produce il foglio `lettura` e salva di default nella
cartella dell'input o nel percorso risolto dalla UI.

## 10. Posta Evolution

`PostaEvolutionOpts` contiene sia campi DataMatrix sia campi del tracciato DU.
Il payload viene normalizzato a lunghezze e caratteri richiesti tramite helper
alfanumerici/numerici. Il numero oggetto parte da
`identificativoOggettoStart` e avanza per gruppo.

Il DU e' scritto con suffisso `.DU` accanto all'output. Le specifiche di dominio
incluse nel repository sono:

- `doc/specifiche-tecniche-codice-datamatrix-standard-unico-traccia.pdf`;
- `doc/specifiche-tecniche-distinta-du.pdf`.

### Preset esterni

`EvolutionSwitch` combina il preset incorporato con `.conf`/XML in
`preset-evolution`. Il parser XML disabilita DOCTYPE ed entita' esterne. La
sincronizzazione remota usa HTTPS, timeout connessione/lettura di 5 secondi e
puo' essere sovrascritta dalla configurazione prevista nel codice. La UI carica
i preset locali all'avvio; l'aggiornamento remoto e' esplicito.

## 11. Configurazione XML

`App.WorkConfiguration` fotografa lo stato dei tab in `Properties` e usa
`storeToXML`/`loadFromXML`. Le chiavi comprendono percorsi, booleani, coordinate,
progressivi, preset e campi DU. L'import applica i valori e aggiorna stati
abilitati e campi derivati.

Il salvataggio automatico usa il nome del PDF senza `.pdf`, seguito da
`_config.xml`. Non contiene il PDF, i file Excel o i preset: conserva soltanto i
valori e i percorsi verso tali risorse.

## 12. PDF/A Riso

`RisoOptimizer` crea un `PdfADocument` conforme PDF/A-3B, con profilo ICC sRGB
ottenuto dal runtime Java. Copia le pagine una a una e inserisce `RecordID` nel
catalogo. La conversione non effettua rasterizzazione; PDF problematici o con
risorse non conformi possono comunque causare eccezioni iText.

## 13. Build, test e distribuzione

Dalla cartella `work`:

```powershell
mvn clean test
mvn package
java -jar target\work-1.1.3.jar
```

Il `maven-shade-plugin` produce un JAR con dipendenze e main class
`mediaprint.work.AppLauncher`. Per l'installer Windows vedere
`doc/creazione-pacchetto.md`; richiede un JDK con `jpackage`.

Test presenti:

- `AppTest`: smoke test placeholder;
- `EvolutionSwitchTest`: caricamento preset XML Properties e XML semplice.

Alla data di questa documentazione `mvn test` esegue 3 test con esito positivo.
Non esistono test automatici per raggruppamento, coordinate, barcode, report,
annullamento o conversione PDF/A.

## 14. Logging ed errori

Le eccezioni del worker sono convertite in una riga `ERRORE` nel tab Log. Le
validazioni UI terminano prima di creare file. La pulizia dei temporanei e'
best-effort e ignora gli errori di cancellazione. Il completamento visualizza un
dialog e salva la configurazione; un errore non mostra il dialog di successo.

Per diagnosi riproducibili conservare input, `_config.xml`, file Excel associati
e contenuto del log.

## 15. Limiti e debito tecnico rilevato

- `App.java` supera 4.900 righe e combina UI, mapping, persistenza e workflow.
- `PdfDuplexGrouper` e `Imbustatrice` sono classi statiche molto ampie.
- `Resize.scaleAndTranslateContent` ha corpo vuoto; non deve essere usato come
  API finche' non viene implementato o rimosso.
- La copertura test e' insufficiente rispetto alla criticita' dei documenti.
- Il forcing UTF-8 usa reflection su un dettaglio interno del JDK e puo' non
  funzionare con forte incapsulamento nei JDK moderni.
- Il nome `testLoadsExternalXlmPreset` e l'estensione `.xlm` nel test sembrano un
  refuso, anche se il loader accetta il contenuto XML.
- I file di report hanno nomi fissi nella cartella di output e una lavorazione
  successiva puo' sovrascriverli.

## 16. Strategia di evoluzione consigliata

1. Introdurre test fixture PDF minime per marker, prefazione e pagine dispari.
2. Testare payload, check digit, progressivi e schema dei report senza UI.
3. Separare `ProcessingRequest` dalla lettura dei widget Swing.
4. Estrarre servizi per grouping, overlay, report e preset dietro interfacce.
5. Rendere esplicita la policy di sovrascrittura dei report.
6. Aggiungere test end-to-end headless con confronto di numero pagine, testo e
   metadati, evitando confronti binari fragili dei PDF.
7. Aggiornare JUnit e correggere la dipendenza iText ricollocata.

## 17. Verifica manuale per una modifica

Per ogni variazione alla pipeline:

1. eseguire `mvn test` e `mvn package`;
2. processare un PDF con prefazione, due gruppi e almeno un gruppo dispari;
3. verificare numero e ordine pagine;
4. provare annullamento e pulizia temporanei;
5. ispezionare codici sul primo e ultimo gruppo;
6. controllare colonne e progressivi nel report;
7. ripetere con resize/rotazione e poi con Riso;
8. importare il file `_config.xml` generato e confrontare i campi UI.
