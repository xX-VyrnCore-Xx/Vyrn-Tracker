# Vyrn Tracker

App Android (Kotlin + Jetpack Compose + Material 3) che unisce un **organizzatore di routine** e un **organizzatore finanziario**. Tutti i dati restano **offline sul telefono** (database Room, nessun account).

## Funzioni

### Routine
- **Abitudini**: frequenza flessibile (ogni giorno, giorni specifici, X volte a settimana), abitudini sì/no o quantitative (es. 8 bicchieri), serie (streak), note giornaliere, striscia settimanale per cambiare giorno.
- **Routine a step**: sequenze mattina/pomeriggio/sera con checklist, durata dei passi e avanzamento giornaliero.
- **Task**: scadenza, ora, priorità e promemoria.
- **Calendario**: vista mensile con indicatori di task e abitudini, dettaglio del giorno selezionato.

### Finanza
- **Panoramica**: saldo totale, entrate/uscite del mese, grafico a ciambella delle spese per categoria.
- **Movimenti**: entrate, uscite e trasferimenti tra conti, raggruppati per giorno.
- **Budget** mensili per categoria con avviso di superamento.
- **Obiettivi di risparmio** con progresso e scadenza.
- **Spese ricorrenti e abbonamenti**: i movimenti vengono generati automaticamente alla scadenza.
- **Debiti e prestiti** (chi ti deve / a chi devi).
- **Più conti**, categorie personalizzabili, **import/export CSV**.

### Extra
- Widget in home (abitudini di oggi + saldo totale).
- Notifiche per abitudini, routine, task e budget superato.
- Statistiche con grafici (abitudini ultimi 7 giorni, spese per categoria, entrate vs uscite a 6 mesi).
- Tema Material 3 viola/indaco con modalità chiara e scura automatica; lingua italiana, valuta euro.

## Build

Requisiti: JDK 17 e Android SDK (compileSdk 35).

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

La CI GitHub Actions (`.github/workflows/android.yml`) compila l'APK debug a ogni push e lo pubblica come artefatto.

## Struttura

```
app/src/main/java/com/vyrn/tracker/
├── data/      entità Room, DAO, logica (streak, saldi, ricorrenze), CSV
├── notify/    promemoria (AlarmManager) e ripianificazione al riavvio
├── widget/    widget Glance
├── ui/        tema, componenti, schermate Routine / Calendario / Finanza / Statistiche
├── RoutineViewModel.kt, FinanceViewModel.kt
└── MainActivity.kt
```
