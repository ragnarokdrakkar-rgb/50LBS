# Program +50

Sledilnik za **+50lbs** program moči — več vaj v eni aplikaciji (bench, incline, squat, deadlift, military press + poljubne lastne vaje). Deluje kot spletna PWA: namestljiva na telefon, dela offline, podatki se shranijo lokalno na napravi.

## Funkcije
Aplikacija ima 4 zavihke (spodnji meni):

**Trening**
- Izbira vaje zgoraj (krog ob imenu kaže napredek cikla)
- Max (1RM) in program vaje na enem mestu – tapni za urejanje
- Mreža vseh treningov cikla: zelena = opravljen, modra obroba = na vrsti; pikice označujejo Negativ/Test set
- En trening naenkrat (puščici ‹ › ali tap na mrežo)
- Tapni set, ko ga opraviš → kljukica + štoparica počitka
- **Ponovitve −/+** ob vsakem setu: vpišeš, koliko si dejansko naredil. Ob *Zaključi trening* aplikacija iz teže in ponovitev (Epley) oceni max: če kateri set ni uspel, se max **zniža** po najslabšem setu (vsaj za 2,5 kg), da so naslednji treningi izvedljivi s predpisanimi ponovitvami; če so vsi seti presegli plan, se zviša. Razveljaviš z »Razveljavi« ali »Označi kot neopravljeno«
- Pod vsako težo piše, katere plošče daš na **vsako stran** droga
- Test set: rezultat vneseš kar pod setom (1 rep → −2,5 kg, 2–4 → ostane, 5+ → +2,5 kg), z možnostjo razveljavitve
- **Zaključi trening** (z »Razveljavi«), na koncu cikla gumb **Nov cikel**

**Napredek** – trenutni max, osebni rekord, treningi, graf maxa skozi čas in zgodovina vnosov

**Kalkulator** – ciljna teža (ali hitro % od maxa), izbira droga, slika naloženega droga

**Nastavitve** – kg/lb, vaje (dodaj, uredi, premikaj), programi (vgrajeni + lastni urejevalnik), izvoz/uvoz backupa, namestitev, Android aplikacija, pomoč

Ostalo:
- Vgrajeni programi: +50 lbs (14 treningov), 5×5 progresija, 5/3/1
- Max se ob prvem opravljenem treningu cikla zaklene (Test seti ga še vedno prilagodijo)
- Gumb »nazaj« na Androidu zapre odprto okno namesto aplikacije
- Deluje brez povezave, podatki ostanejo na napravi

## Datoteke
```
index.html              glavna aplikacija
android/                Android aplikacija (WebView + posodobitve z GitHuba)
.github/workflows/      samodejna gradnja APK-ja
manifest.webmanifest    PWA manifest
sw.js                   service worker (offline)
icon.svg                ikona (vektorska)
icon-192.png            ikona PWA
icon-512.png            ikona PWA
icon-512-maskable.png   ikona PWA (maskable)
icon-180.png            ikona za iOS (apple-touch-icon)
```

## Android aplikacija
Mapa `android/` vsebuje majhno Android aplikacijo (Java, WebView), v katero je zapakiran `index.html`.

- **Gradnja:** ob vsakem pushu na `main`, ki spremeni aplikacijo, GitHub Actions (`.github/workflows/android.yml`) zgradi APK in ga objavi kot izdajo `v<številka>` z datoteko `plus50.apk`. Ročno: *Actions → Android APK → Run workflow*.
- **Prenos:** v spletni aplikaciji *Nastavitve → Android aplikacija → Prenesi Android aplikacijo* (povezava na najnovejši `plus50.apk`).
- **Prenos podatkov:** v brskalniku na telefonu *Nastavitve → Prenesi podatke v Android aplikacijo* odpre aplikacijo in uvozi vse podatke (ob potrditvi). Druga možnost je *Izvozi backup* → *Uvozi backup*.
- **Posodobitve:** aplikacija ob zagonu preveri najnovejšo izdajo na GitHubu in ponudi *Posodobi* (prenos + namestitev); ročno *Nastavitve → Preveri posodobitve*. Podatki ostanejo.
- **Podpis (enkratna nastavitev):** podpisni ključ ni v repozitoriju. V *Settings → Secrets and variables → Actions* dodaj:
  - `KEYSTORE_BASE64` – ključ (`plus50.keystore`) kodiran v base64,
  - `KEYSTORE_PASSWORD` – geslo ključa (alias `plus50`).
  Vse različice morajo biti podpisane z istim ključem, sicer se posodobitev ne namesti – ključ shrani tudi na varno.
- Lokalna gradnja: `cd android && KEYSTORE_FILE=/pot/do/plus50.keystore KEYSTORE_PASSWORD=... ./gradlew assembleRelease` (potreben Android SDK).

## Objava na GitHub Pages
1. Naredi nov repozitorij na GitHubu (npr. `plus50-trainer`).
2. Naloži vse zgornje datoteke v koren repozitorija (root), ne v podmapo.
3. **Settings → Pages → Build and deployment → Source: Deploy from a branch.**
4. Izberi vejo `main` in mapo `/ (root)`, shrani.
5. Po nekaj minutah je aplikacija na:
   `https://<tvoje-uporabnisko-ime>.github.io/plus50-trainer/`

### Namestitev na telefon
- **Android (Chrome):** odpri povezavo → meni ⋮ → *Dodaj na začetni zaslon*.
- **iPhone (Safari):** odpri povezavo → Deli → *Dodaj na začetni zaslon*.

## Posodabljanje
`index.html` se naloži sveže ob vsakem odprtju (network-first), zato ga ni treba posebej označevati. Ko spremeniš ikone, manifest ali `sw.js`, **povečaj številko različice** v `sw.js`:
```js
const STATIC = 'plus50-static-v3';   // v2 -> v3
```
Aplikacija nato pokaže »Nova različica je na voljo → Osveži«.

## Podatki / zasebnost
Vse je shranjeno lokalno v brskalniku (`localStorage`), nič ne gre na strežnik. Za varnost ali prenos na drugo napravo uporabi **Izvozi backup** in **Uvozi backup**.

## Opomba o natančnosti
Originalna aplikacija sproti prilagaja 1RM glede na Test sete, zato se lahko kakšna teža razlikuje za ~2,5 kg. Polje **Max** je tvoj nadzor — posodobi ga po vsakem Testu (gumbi to naredijo samodejno).
