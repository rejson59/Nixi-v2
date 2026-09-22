# NIXI v2

Profesjonalna asystentka AI: obsługa głosowa, Gemini, Supabase, Spotify, alerty i automatyzacja telefonu.
Aplikacja **natywna na Androida** (Kotlin + Jetpack Compose, Gradle Kotlin DSL, AGP 9.1.1).

> Ten README opisuje tylko **jak zbudować gotowy plik APK przez GitHub Actions** — bez zmieniania żadnych plików projektu.

---

## 0. Czego potrzebuje ten projekt (do wiadomości)

| Element | Wartość w repo |
|---|---|
| Android Gradle Plugin | `9.1.1` → wymaga **Gradle ≥ 9.1.0** i **JDK ≥ 17** |
| Gradle (wrapper) | `9.3.1` (`gradle/wrapper/gradle-wrapper.properties`) |
| Kotlin / Compose | `2.2.10` / BOM `2024.09.00` (AGP 9 = built-in Kotlin, `kotlin-android` celowo nie jest użyty) |
| KSP | `2.3.11` (poprzednia `2.3.5` crashowała na CI — patrz sekcja 5) |
| `compileSdk` / `targetSdk` / `minSdk` | `36.1` / `36` / `24` |
| `applicationId` | `com.aistudio.nixi.vxaklm` |
| Klucze API | przez Secrets Gradle Plugin z pliku **`.env`** w katalogu głównym (`.env.example` to szablon) |
| Podpisywanie `debug` | zakłada plik **`debug.keystore`** w katalogu głównym (alias `androiddebugkey`, hasła `android`) |
| Podpisywanie `release` | `KEYSTORE_PATH` + `STORE_PASSWORD` + `KEY_PASSWORD` (alias klucza: `upload`), domyślnie `my-upload-key.jks` w katalogu głównym |
| `google-services.json` | opcjonalny — brak pliku tylko ostrzega (Firebase App Check bez niego nie będzie działać) |

**Ważne:** w repo **nie ma** `gradlew` / `gradle-wrapper.jar` (jest tylko `gradle-wrapper.properties`). Dlatego workflow poniżej pobiera Gradle 9.3.1 i używa go bezpośrednio — bez generowania wrappera i bez ruszania projektu.
Jeśli wolisz `./gradlew`, raz uruchom lokalnie `gradle wrapper --gradle-version 9.3.1` i zacommituj `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`.

---

## 1. APK w 3 krokach (debug, do zainstalowania na telefonie)

1. **Skopiuj szablon do `.github/workflows/`**: plik `build-apk.yml` leży w katalogu głównym repo,
   więc wystarczy `mkdir -p .github/workflows && cp build-apk.yml .github/workflows/` i commit
   (albo `Add file → Create new file` na GitHubie i wklejenie zawartości z surowego widoku, sekcja 2).
2. **Dodaj secrets** — `Settings → Secrets and variables → Actions → New repository secret`:
   `GEMINI_API_KEY` (klucz z Google AI Studio), opcjonalnie `SUPABASE_URL` i `SUPABASE_ANON_KEY`.
   (Można pominąć — wtedy klucz wpisujesz po prostu w ustawieniach aplikacji na telefonie.)
3. **Uruchom build** — zakładka `Actions → Build NIXI APK (debug) → Run workflow → Run workflow`.
   Po 10–20 min (pierwszy raz) w tym runie pojawi się **artifact `NIXI-apk-debug`** — pobierasz,
   kopiujesz na telefon i instalujesz (Android: *Ustawienia → Zainstaluj przez USB / nieznane źródła*).
   Alternatywnie: `adb install -r app-debug.apk`.

---

## 2. Plik `build-apk.yml` → `.github/workflows/build-apk.yml`

Szablon jest w repo pod **`build-apk.yml`** (katalog główny). Do `.github/workflows/` musisz go
wrzucić **sam** — to ograniczenie uprawnień, nie błąd YAML-a: konto bota/CI bez uprawnienia
`workflows` nie może utworzyć pliku w `.github/workflows/`
(`refusing to allow a GitHub App to create or update workflow ... without 'workflows' permission`).
Stąd plik leży obok `README.md`, a nie w `.github/workflows/`.

```bash
mkdir -p .github/workflows
cp build-apk.yml .github/workflows/build-apk.yml
git add .github/workflows/build-apk.yml && git commit -m "ci: build APK on Actions" && git push
```

Przez przeglądarkę: **`Actions → New workflow → Set up a workflow yourself → wklej zawartość`**
i **`Commit changes`**. Wklejaj w trybie edycji (surowy tekst), nie z podglądu wyrenderowanego
README. **Sprawdzony przebieg błędu „error on line 2": w 1. wierszu pliku zostało samo słowo
`yaml`** — resztka nagłówka bloku kodu z README. Wtedy 2. linia (`name: Build NIXI APK (debug)`)
przestaje być częścią dokumentu YAML i na niej parser się wywala. Leczy to skreślenie wszystkiego
przed `name:`; najlepiej w ogóle nie przepisywać, tylko skopiować plik z repo.

Szybka kontrola po wklejeniu:

```bash
head -2 .github/workflows/build-apk.yml
# oczekiwany wynik:  1) name: Build NIXI APK (debug)   2) linia pusta

python3 -c "import yaml,sys; yaml.safe_load(open(sys.argv[1])); print('YAML OK')" \
  .github/workflows/build-apk.yml   # bez pyyaml: pip install pyyaml
```
W pliku nie może być tabulatorów (tylko spacje), znaków `` ``` `` ani żadnego tekstu przed `name:`.
Bez Pythona wystarczy `cat -A | head -2` — znaki `^I` to tabulatory, `^M` to Windows CRLF.

Poniższy blok to kopia 1:1 tego samego pliku (z raportem błędu jako issue, bo logi CI są poza zasięgiem tej maszyny):

```yaml
name: Build NIXI APK (debug)

on:
  workflow_dispatch:
  push:
    branches:
      - main

permissions:
  contents: read
  issues: write

jobs:
  build:
    name: assembleDebug
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: Set up Gradle 9.3.1
        run: |
          set -euo pipefail
          curl -fsSL -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-9.3.1-bin.zip
          unzip -q /tmp/gradle.zip -d /opt
          echo "/opt/gradle-9.3.1/bin" >> "$GITHUB_PATH"

      - name: Create debug.keystore (signingConfig "debugConfig")
        run: |
          keytool -genkeypair -v \
            -keystore debug.keystore \
            -storepass android -keypass android \
            -alias androiddebugkey -dname "CN=Android Debug,O=Android,C=US" \
            -keyalg RSA -keysize 2048 -validity 10000

      - name: Create .env from secrets
        run: |
          {
            echo "GEMINI_API_KEY=${GEMINI_API_KEY}"
            echo "SUPABASE_URL=${SUPABASE_URL}"
            echo "SUPABASE_ANON_KEY=${SUPABASE_ANON_KEY}"
          } > .env
        env:
          GEMINI_API_KEY: ${{ secrets.GEMINI_API_KEY }}
          SUPABASE_URL: ${{ secrets.SUPABASE_URL }}
          SUPABASE_ANON_KEY: ${{ secrets.SUPABASE_ANON_KEY }}

      - name: Show toolchain
        run: |
          java -version
          gradle --version
          echo "ANDROID_HOME=${ANDROID_HOME:-brak}"
          ls "${ANDROID_HOME:-/opt/android-sdk}/platforms" 2>/dev/null || true

      - name: Build debug APK
        run: gradle --no-daemon --stacktrace=full assembleDebug 2>&1 | tee /tmp/build.log

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: NIXI-apk-debug
          path: app/build/outputs/apk/debug/app-debug.apk
          if-no-files-found: error
          retention-days: 30

      # Logi CI leza na innej domenie niz api.github.com - ten krok wynosi blad tam,
      # skad da sie go odczytac (issue + job summary), zamiast klikac "View raw logs".
      - name: Publish build report (issue + job summary)
        if: failure()
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          grep -E '^(FAILURE|> Task .*FAILED|BUILD FAILED|What went wrong|Caused by|e: )|^[[:space:]]+at ' /tmp/build.log \
            | head -n 80 > /tmp/err.txt
          {
            echo "## Build APK - raport bledu (run $GITHUB_RUN_ID)"
            echo
            echo "- commit: `$GITHUB_SHA` | branch: `$GITHUB_REF_NAME`"
            echo "- pelny log: $GITHUB_SERVER_URL/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID"
            echo
            sed 's/^/    /' /tmp/err.txt
            echo
            echo "### Ogon logu"
            tail -n 100 /tmp/build.log | sed 's/^/    /'
          } > /tmp/report.md
          { echo "### Build APK - blad (skrot)"; echo; sed 's/^/    /' /tmp/err.txt; } >> "$GITHUB_STEP_SUMMARY"
          gh issue create \
            --repo "$GITHUB_REPOSITORY" \
            --title "CI: APK build failed on $GITHUB_REF_NAME (${GITHUB_SHA:0:7})" \
            --body-file /tmp/report.md
```'
            tail -n 160 /tmp/build.log 2>/dev/null || echo "brak /tmp/build.log"
            grep -E '^(FAILURE|> Task .*FAILED|BUILD FAILED|What went wrong|Caused by|e: )' /tmp/build.log 2>/dev/null | head -40 || true
            echo '```'
          } > /tmp/report.md
          tail -n 80 /tmp/report.md >> "$GITHUB_STEP_SUMMARY"
          gh issue create \
            --repo "$GITHUB_REPOSITORY" \
            --title "CI: APK build failed on ${GITHUB_REF_NAME} (${GITHUB_SHA::7})" \
            --body-file /tmp/report.md \
```'
            tail -n 160 /tmp/build.log 2>/dev/null || echo "brak /tmp/build.log"
            grep -E '^(FAILURE|> Task .*FAILED|BUILD FAILED|What went wrong|Caused by|e: )' /tmp/build.log 2>/dev/null | head -40 || true
            echo '```'
          } > /tmp/report.md
          tail -n 80 /tmp/report.md >> "$GITHUB_STEP_SUMMARY"
          gh issue create \
            --repo "$GITHUB_REPOSITORY" \
            --title "CI: APK build failed on ${GITHUB_REF_NAME} (${GITHUB_SHA::7})" \
            --body-file /tmp/report.md \
```

APK jest w środku: **`app/build/outputs/apk/debug/app-debug.apk`**.

---

## 3. Sygnowany release (APK pod Play Store / dystrybucję)

Release wymaga **własnego keystore** — inaczej `assembleRelease` się nie uda (brak `my-upload-key.jks`).

1. Wygeneruj keystore **lokalnie** (JDK 17+), nigdy go nie commituj:

```bash
keytool -genkeypair -v \
  -keystore my-upload-key.jks \
  -alias upload \
  -keyalg RSA -keysize 2048 -validity 10000
```

2. Secrets w `Settings → Secrets and variables → Actions`:

| Secret | Zawartość |
|---|---|
| `KEYSTORE_BASE64` | wynik `base64 -w0 my-upload-key.jks` |
| `STORE_PASSWORD` | hasło keystore |
| `KEY_PASSWORD` | hasło klucza (`upload`) |
| `GEMINI_API_KEY` *(opcjonalnie)* | jak wyżej |

3. Drugi workflow — `.github/workflows/build-release-apk.yml` — identyczny jak wyżej, tylko:

```yaml
name: Build NIXI APK (release, signed)

on:
  workflow_dispatch:

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - name: Set up Gradle 9.3.1
        run: |
          set -euo pipefail
          curl -fsSL -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-9.3.1-bin.zip
          unzip -q /tmp/gradle.zip -d /opt
          echo "/opt/gradle-9.3.1/bin" >> "$GITHUB_PATH"
      - name: Restore keystore from secret
        run: echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > my-upload-key.jks
      - name: Create .env from secrets
        run: printf 'GEMINI_API_KEY=%s\n' "${{ secrets.GEMINI_API_KEY }}" > .env
      - name: Build release APK
        run: gradle --no-daemon assembleRelease
        env:
          KEYSTORE_PATH: ${{ github.workspace }}/my-upload-key.jks
          STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: NIXI-apk-release
          path: app/build/outputs/apk/release/app-release.apk
          if-no-files-found: error
```

Zamiast `assembleRelease` można użyć `bundleRelease` — wtedy artifact to `.aab`
(`app/build/outputs/bundle/release/app-release.aab`) do wgrania na Play Console.
Jeśli `isMinifyEnabled = false` (jak teraz), release APK też nadaje się do instalacji na telefonie.

---

## 4. Budowanie lokalnie (ta sama komenda, co w CI)

```bash
# wymagane: JDK 17+ i Android SDK z platform 36 (+ build-tools 36.0.0)
keytool -genkeypair -keystore debug.keystore -storepass android -keypass android \
  -alias androiddebugkey -dname "CN=Android Debug,O=Android,C=US" -keyalg RSA -validity 10000
gradle --no-daemon assembleDebug      # albo ./gradlew assembleDebug, jeśli dodasz wrapper
```

Brak `ANDROID_HOME`? Ustaw `sdk.dir=/ścieżka/do/android-sdk` w pliku `local.properties`
(albo zainstaluj przez Android Studio — samo to dopisze).

---

## 5. Czego zabrakło / najczęstsze błędy w CI

| Błąd | Przyczyna i naprawa |
|---|---|
| `Invalid workflow file ... error in your yaml syntax on line 2` | pierwszy wiersz pliku to nie YAML: wkleił się nagłówek/tytuł README, ``` ``` ``` albo tabulatory zamiast spacji. Poprawka: `cp build-apk.yml .github/workflows/` z repo (sekcja 2) albo usunięcie śmieci sprzed `name:`. Sprawdź: `head -2` musi dać `name: Build NIXI APK (debug)` i pusty wiersz |
| `NullPointerException: Cannot invoke "ksp.com.intellij.openapi.application.Application.getService(...)" because ... ApplicationManager.getApplication() is null` (krok `Build debug APK`, fail przy `kspDebugKotlin`) | **znany błąd KSP 2.3.5 poza IntelliJ/na CI** ([google/ksp#2763](https://github.com/google/ksp/issues/2763)) — w tym projekcie `gradle/libs.versions.toml` ma `googleDevtoolsKsp = "2.3.5"`. **To repo ma już poprawkę:** `gradle/libs.versions.toml` → `googleDevtoolsKsp = "2.3.11"` (naprawka ≥ 2.3.6) oraz usunięty `kotlin.compiler.execution.strategy=in-process` z `gradle.properties`. U siebie podnieś tylko tę jedną liczbę, jeśli nadal widzisz ten błąd |
| `refusing to allow a GitHub App to create or update workflow ... without 'workflows' permission` | push pliku z `.github/workflows/` przez zewnętrzną integrację/bota — utwórz ten plik własnym kontem (web editor albo lokalnie), ewentualnie `Settings → GitHub Apps → Configure → Repository access / Workflows: Read & write` |
| `Could not find debug.keystore` / `Keystore file ... not found` | `build.gradle.kts` wymaga `debug.keystore` w katalogu głównym → krok *Create debug.keystore* (sekcja 2) |
| `Unsupported class file major version` / build gradle fail | zła wersja Gradle lub JDK — musi być Gradle ≥ 9.1 i JDK ≥ 17 (workflow pinuje 9.3.1 + JDK 21) |
| `plugins block ... com.android.application` / brak wrapper jar | nie używaj `./gradlew` jeśli `gradle/wrapper/gradle-wrapper.jar` nie jest w repo — użyj `gradle` z kroku *Set up Gradle* albo zacommituj wrapper |
| `missing google-services.json` | tylko ostrzeżenie (strategia `WARN`); jeśli potrzebujesz App Check/Firebase — dodaj `app/google-services.json` i zakoduj go jako secret `GOOGLE_SERVICES_JSON` (base64) z krokiem `base64 -d > app/google-services.json` |
| APK instaluje się, ale „brak klucza API Gemini” | sekrety `.env` nie były ustawione — podaj klucz w ustawieniach NIXI na telefonie albo dodaj secret `GEMINI_API_KEY` i przebuduj |
| Artifacts nie widać | zbudował się inny wariant (np. `assembleBundle`) albo ścieżka inna niż `app/build/outputs/apk/<variant>/` |
| `A secret ... is not available` przy PR z forków | secrets nie są udostępniane dla forków — buduj z gałęzi własnej lub przez `workflow_dispatch` |

Wskazówki: pierwszy build bywa wolny (pobiera zależności); kolejne szybciej, jeśli dodasz
`uses: gradle/actions/setup-gradle@v4` **zamiast** kroku z `curl` (ten action sam zarządza cache).
W `Actions → General → Workflow permissions` ustaw **Read and write permissions**.
