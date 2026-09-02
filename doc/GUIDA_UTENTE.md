# WorkPDF 2026 EVO - Guida utente

## 1. Scopo dell'applicazione

WorkPDF e' un'applicazione desktop per preparare PDF destinati a stampa,
imbustamento e postalizzazione. Identifica l'inizio di ogni documento tramite
una stringa marcatore, crea gruppi fronte/retro, completa con una pagina bianca
i gruppi dispari e puo' aggiungere barcode, OMR, QR Code, DataMatrix, contatori,
testi postali e report Excel.

L'elaborazione non modifica il PDF sorgente. Il file di output deve avere un
percorso diverso dall'input.

## 2. Avvio e requisiti

Requisiti per il JAR:

- Windows, Linux o macOS con Java 11 o superiore;
- permessi di lettura sul PDF sorgente;
- permessi di scrittura nella cartella di output;
- spazio disponibile per PDF temporanei e report.

Avvio da PowerShell, nella cartella `work`:

```powershell
java -jar target\work-1.1.3.jar
```

In alternativa, in ambiente di sviluppo:

```powershell
mvn exec:java
```

## 3. Procedura minima

1. Nel tab **Generale**, scegliere il PDF di input.
2. Indicare un PDF di output diverso dall'input.
3. Inserire la stringa che identifica l'inizio di ogni lettera.
4. Attivare, se necessario, le opzioni negli altri tab.
5. Usare **Anteprima reticolo** per verificare le coordinate degli elementi.
6. Premere **Esegui** e seguire avanzamento e messaggi nel tab **Log**.
7. Verificare PDF, report Excel e configurazione XML prodotti.

Per fermare un lavoro in corso premere **Interrompi**. L'interruzione e'
cooperativa: puo' richiedere il completamento dell'operazione PDF corrente.

## 4. Logica di raggruppamento duplex

La stringa marcatore viene cercata nel testo estratto da ogni pagina. Ogni
pagina che la contiene apre un nuovo gruppo. Le pagine precedenti al primo
marcatore sono copiate all'inizio senza appartenere a un gruppo.

Per ogni gruppo:

- vengono copiate tutte le pagine fino al marcatore successivo;
- se il totale e' dispari, viene aggiunta una pagina A4 bianca;
- codici e contatori sono applicati in base al gruppo e ai fronti;
- viene aggiunta una riga al report di dettaglio.

Se il marcatore non viene trovato, l'elaborazione non puo' creare gruppi utili:
controllare il testo realmente estraibile dal PDF. Un PDF composto solo da
immagini richiede OCR esterno, che WorkPDF non esegue.

## 5. Tab Generale

### File e ricerca

- **PDF input**: documento sorgente.
- **PDF output**: risultato finale.
- **Versione PDF output**: versione dichiarata dal PDF generato.
- **Stringa marcatore**: testo che apre un gruppo.
- **Ignora maiuscole/minuscole**: confronto senza distinzione di caso.
- **Normalizza accenti/diacritici**: tratta lettere accentate e non accentate
  come equivalenti e normalizza gli spazi orizzontali.

### Rotazione e resize

**Ruota pagina in base a stringa** cerca una stringa e ruota la pagina trovata
di 90, 180 o 270 gradi. Questa funzione richiede che **Applica resize** sia
attivo. **Applica resize anche alle pagine ruotate** decide se scalare anche la
pagina individuata.

Il resize usa:

- scala percentuale, obbligatoriamente maggiore di zero;
- offset X e Y in millimetri;
- eventuale forzatura del foglio ad A4 prima della trasformazione.

Il resize precede raggruppamento e inserimento dei codici.

### Etichetta gruppo

L'etichetta gruppo puo' essere usata anche senza barcode imbustatrice. Mostra
il progressivo e, se valorizzato, l'ID lavorazione. Sono configurabili posizione
e orientamento orizzontale/verticale.

## 6. Tab Contatori

Il contatore viene scritto sulle pagine del gruppo e contiene progressivi di
gruppo/pagina. Configurare:

- posizione X da sinistra e Y dall'alto;
- dimensione del testo;
- orientamento orizzontale o verticale.

Se e' presente il barcode imbustatrice, il progressivo iniziale di quel barcode
determina anche il numero gruppo usato dal contatore e dalle correzioni QR.

## 7. Tab Barcode Imbustatrice

Attivare **Inserisci barcode imbustatrice** per applicare Code 39 verticali sui
fronti dei gruppi. La larghezza target e' 40 mm; modulo e altezza barre devono
essere maggiori di zero. Posizione, progressivo iniziale e parametri tipografici
sono configurabili.

Opzioni operative:

- **Usa OMR** sostituisce Code 39 con tacche per lettura ottica;
- **Allegati presenti** usa il pattern esteso previsto per gli allegati;
- **Il PDF target e' un allegato** modifica il trattamento del documento;
- il progressivo iniziale alimenta anche etichetta, contatore e lookup delle
  correzioni.

## 8. Tab Barcode Raccomandata

Genera un barcode Interleaved 2 of 5 per ogni gruppo. Il codice iniziale deve
essere un identificativo numerico di 11 cifre; l'app incrementa il progressivo
e calcola il check digit. Sono disponibili preset AR, AG e personalizzato.

Si possono configurare posizione, modulo stretto, altezza barre, testo leggibile
e prefisso. I valori dimensionali e i font devono essere maggiori di zero.

Il testo personalizzato consente righe aggiuntive, per esempio istruzioni di
restituzione. Se attivato, testo e dimensione font sono obbligatori.

## 9. Tab Omologazione Postale

Inserisce un testo di omologazione anche indipendentemente dal barcode
Raccomandata. Scegliere un preset oppure il valore personalizzato e impostare
posizione e font. Il testo viene convertito in maiuscolo; testo vuoto e font non
positivo bloccano l'avvio.

## 10. Tab QR Code

Il valore generato e' formato dalla base alfanumerica e da un progressivo
riempito al numero di cifre indicato. Sono richiesti:

- base non vuota;
- progressivo iniziale non negativo;
- da 1 a 18 cifre;
- dimensione maggiore di zero;
- posizione X/Y e livello di correzione errore.

### Correzioni da Excel

L'opzione applica un testo o un'icona nell'area configurata quando il
progressivo del gruppo trova corrispondenza nel file XLSX. Il file deve esistere;
larghezza area, font e dimensione icona devono essere maggiori di zero.

Prima di una produzione, verificare il file con un campione e controllare il
log, che riporta il numero di correzioni caricate.

## 11. Tab Blocco indirizzo

Definisce un rettangolo della prima pagina di ogni gruppo tramite X, Y,
larghezza e altezza. Il testo estratto viene interpretato come nominativo,
indirizzo, CAP, comune e provincia e confluisce nel report; puo' alimentare anche
Posta Evolution.

**Solo lettura indirizzo** permette di provare l'estrazione sul PDF senza
eseguire la normalizzazione. I risultati sono mostrati nel log.

**Lettura stringa chiave su PDF** cerca la stringa scelta in tutte le pagine ed
esporta le righe in `lettura.xlsx`. Se output o marcatore non sono compilati,
**Esegui** avvia soltanto questa esportazione.

## 12. Tab Riso GL9730

Converte il risultato finale in PDF/A-3B con output intent sRGB e aggiunge al
catalogo PDF il metadato `RecordID`. Quando l'opzione e' attiva, Record ID e'
obbligatorio. La conversione avviene dopo tutte le altre elaborazioni PDF.

## 13. Tab Posta Evolution

L'opzione genera un DataMatrix per gruppo. Il payload comprende campi tecnici
di mittente e destinatario, progressivo oggetto, causale, omologazione e servizi.
Usare preferibilmente un preset approvato e non modificare i campi tecnici senza
conoscere la specifica postale.

### Fonte indirizzi

- Senza Excel, l'indirizzo viene letto dall'area configurata nel PDF.
- Con Excel, le righe valide sono associate ai gruppi in ordine.
- Il file deve contenere almeno tante righe valide quanti sono i gruppi; in caso
  contrario l'elaborazione termina con errore.

### Report DU

**Genera report DU** crea il tracciato `.DU` accanto al PDF. Compilare i campi
di accettazione, operatore, prenotazione, prodotto, tracciatura, conto,
omologazione, formato e identificativi secondo le specifiche presenti in `doc`.

### Preset

Il preset **Posta Massiva** e' incorporato. File `.conf`/XML aggiuntivi sono
letti dalla cartella `preset-evolution` della distribuzione. **Aggiorna preset**
contatta il server configurato, con timeout di 5 secondi, e aggiorna i preset
locali. L'elaborazione PDF non richiede la sincronizzazione remota.

## 14. Anteprima reticolo

L'anteprima crea un PDF separato usando la prima pagina del sorgente, una griglia
in millimetri e i riquadri degli elementi abilitati. Serve a validare coordinate,
dimensioni e rotazioni prima della produzione. La configurazione e' salvata
automaticamente anche accanto al PDF di anteprima.

## 15. File prodotti

Accanto al PDF di output possono comparire:

- `<nome-output>.pdf`: documento elaborato;
- `DettaglioElaborazione.xlsx`: gruppo, intervallo pagine, conteggio, indirizzo,
  QR e codice Raccomandata;
- `<nome-output>.DU`: report Posta Evolution, quando abilitato;
- `lettura.xlsx`: esportazione per stringa chiave;
- `<nome-output>_config.xml`: configurazione completa salvata automaticamente.

I file temporanei con suffisso `_resize_tmp.pdf` o `_riso_tmp.pdf` vengono
normalmente eliminati al termine.

## 16. Importazione ed esportazione configurazione

**Esporta configurazione** salva manualmente tutti i campi dell'interfaccia in
XML. **Importa configurazione** ripristina valori, opzioni e preset. Verificare
sempre i percorsi dei file esterni dopo aver trasferito la configurazione su
un'altra postazione.

## 17. Risoluzione problemi

| Sintomo | Controllo consigliato |
|---|---|
| Nessun gruppo rilevato | Verificare marcatore, opzioni caso/accenti e testo estraibile dal PDF. |
| Output non creato | Aprire il tab Log, controllare permessi e percorso differente dall'input. |
| Elementi fuori pagina | Generare il reticolo e ricontrollare coordinate e origine degli assi. |
| Rotazione rifiutata | Attivare resize e usare 90, 180 o 270 gradi. |
| Evolution fallisce | Verificare numero righe Excel, campi obbligatori e formato dei codici. |
| Riso fallisce | Compilare Record ID e verificare che il PDF sorgente sia leggibile. |
| Testo errato o vuoto | Il PDF potrebbe avere font/mappatura testo non estraibili; provare OCR esterno. |
| File bloccato | Chiudere PDF e XLSX nelle altre applicazioni e ripetere. |

## 18. Checklist prima della produzione

1. Conservare una copia del PDF originale.
2. Provare il marcatore su un campione rappresentativo.
3. Generare e ispezionare il reticolo.
4. Controllare primo e ultimo progressivo di ogni codice.
5. Verificare fronte/retro e pagine bianche aggiunte.
6. Aprire `DettaglioElaborazione.xlsx` e confrontare il numero di gruppi.
7. Per Evolution, validare DataMatrix e file DU con gli strumenti del flusso.
8. Archiviare il file `_config.xml` insieme alla lavorazione.
