# Creazione Pacchetto Distribuzione

## Prerequisiti
- Java 11+ installato
- Maven installato e disponibile nel PATH
- `jpackage` disponibile (incluso nei JDK recenti)

## Comandi (PowerShell)
```powershell
cd work
mvn clean package -DskipTests
Remove-Item -Recurse -Force target\jpackage-input,target\installer -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force target\jpackage-input | Out-Null
Copy-Item target\work-1.2.3.jar target\jpackage-input\
jpackage --type exe --name WorkPDF --input target\jpackage-input --main-jar work-1.2.3.jar --main-class mediaprint.work.AppLauncher --dest target\installer --app-version 1.2.3 --vendor Mediaprint --win-menu --win-shortcut --icon src\main\resources\logo.ico --add-modules java.desktop,java.logging,java.security.jgss,java.xml.crypto,jdk.charsets,jdk.management
```

## Avvio dalla distribuzione
Opzione 1: esegui direttamente il jar prodotto:

```powershell
java -jar target\work-1.2.3.jar
```

Opzione 2: installa l'app eseguendo l'installer generato:

```powershell
target\installer\WorkPDF-1.2.3.exe
```

Se l'app installata mostra "Failed to launch JVM", avviala da PowerShell dalla
cartella di installazione per vedere l'errore completo:

```powershell
& "$env:LOCALAPPDATA\WorkPDF\WorkPDF.exe"
```

Il runtime creato da `jpackage` deve includere `jdk.management`, perche'
`AppLauncher` usa `com.sun.management.OperatingSystemMXBean` per calcolare la
memoria disponibile prima di avviare la GUI.

## Output da distribuire
Distribuire uno dei seguenti pacchetti:
- `work\target\work-1.2.3.jar`
- `work\target\installer\WorkPDF-1.2.3.exe`
