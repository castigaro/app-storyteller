# StoryTeller

Interaktiver KI-Geschichtenerzähler als Android-App — inspiriert vom
Konzept des Open-Source-Projekts [StoryTeller](https://github.com/marceld23/StoryTeller)
(dort ein Raspberry-Pi-/Web-System), hier als eigenständige,
neu geschriebene Mobile-App.

## Was die App macht

- **Welt beschreiben, Geschichte bekommen:** Ein Satz genügt („Eine Burg im
  Nebel …“) — die KI erfindet einen Titel und erzählt den ersten Abschnitt.
- **Mitspielen statt nur zuhören:** Jeder Abschnitt endet mit einer Frage
  oder Entscheidung. Alles, was der Spieler erwähnt (Figuren, Gegenstände,
  Wendungen), wird in die weitere Handlung eingewoben.
- **Drei-Akte-Bogen:** Einführung, Verwicklung mit Höhepunkt, Auflösung —
  die Geschichte kommt von selbst zu einem stimmigen Ende.
- **Sprechen & Vorlesen:** Antworten per Android-Spracherkennung diktieren
  (System-Dialog, keine eigene Mikrofon-Berechtigung), neue Abschnitte auf
  Wunsch per Android-Sprachausgabe vorlesen lassen.
- **Mehrere Geschichten:** Angefangene Geschichten werden lokal gespeichert
  und können jederzeit fortgesetzt werden.
- **Anbieterwahl:** Anthropic (Claude) oder OpenAI (GPT) mit eigenem
  API-Key; Modell frei wählbar. Der Key bleibt auf dem Gerät.

## Technik

- Kotlin, ViewBinding, Material Components; gemeinsames Basis-Theme aus
  [`android-apps-common`](https://github.com/appsonar/android-apps-common)
  (`Theme.AppSonar`).
- Direkte HTTP-Anbindung (OkHttp) an die Anthropic Messages API bzw.
  OpenAI Chat Completions API — kein SDK.
- Geschichten liegen als JSON in den App-Dateien (`stories.json`).
- minSdk 26, targetSdk 34.

## Build

CI baut per `.github/workflows/build.yml` und veröffentlicht die APKs samt
Versions-Manifest im GitHub-Release `latest`;
[appsonar.de](https://appsonar.de) verlinkt direkt darauf.
Die App prüft beim Start und über „Nach Update suchen“ gegen das Manifest
auf neue Versionen (gemeinsamer `UpdateChecker` aus `android-apps-common`).

## Lizenz

Copyright © 2026 Torsten Klein

Dieses Projekt steht unter der **GNU Affero General Public License v3.0 oder
später** (AGPL-3.0-or-later), siehe [LICENSE](LICENSE): Wer den Code — auch als
Netzwerkdienst — weiterverwendet oder verändert, muss den Quellcode unter
derselben Lizenz offenlegen. Eine kommerzielle Lizenz ist auf Anfrage möglich.
