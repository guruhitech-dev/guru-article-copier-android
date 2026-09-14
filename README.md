# Copia articolo

App Android senza account, pubblicità o tracciamento che riceve un link dal menu **Condividi**, scarica la pagina e la presenta subito in una modalità lettura pulita. Titolo, sottotitoli, paragrafi ed elenchi restano selezionabili e possono essere copiati insieme negli appunti.

## Funzioni

- destinazione di condivisione per URL inviati da browser, Discover, Telegram e altre app;
- estrazione locale del contenuto con rimozione di navigazione, banner, sidebar, correlati, commenti e footer;
- **Copia tutto**, selezione manuale del testo e **Apri originale**;
- campo per aprire un URL incollato manualmente;
- nessun permesso oltre a Internet e nessun servizio remoto oltre al sito richiesto.

## Compilazione

Richiede JDK 17, Android SDK 35 e Gradle 8.9 (compatibile con Android Gradle Plugin 8.7.3).

```bash
gradle assembleDebug
```

L'APK locale viene creato in `app/build/outputs/apk/debug/app-debug.apk`.

## Scaricare l'APK da GitHub

1. Apri la scheda **Actions** del repository.
2. Seleziona il workflow **Build debug APK** e l'esecuzione più recente riuscita.
3. Nella sezione **Artifacts**, scarica **copia-articolo-debug-apk**.
4. Estrai lo ZIP e installa `app-debug.apk` sul dispositivo (potrebbe essere necessario consentire l'installazione da questa fonte).

Il workflow parte automaticamente a ogni push e pull request e può anche essere avviato manualmente.
