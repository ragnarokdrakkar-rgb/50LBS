# Program +50

Sledilnik za **+50lbs** program moči — več vaj v eni aplikaciji (bench, incline, squat, deadlift, military press + poljubne lastne vaje). Deluje kot spletna PWA: namestljiva na telefon, dela offline, podatki se shranijo lokalno na napravi.

## Funkcije
- Vseh 14 treningov programa, preračunanih po tvojem 1RM za vsako vajo
- Tipi setov: Normalno / Negativ / Test (do odpovedi)
- Test set sam prilagodi max: 1 rep → −2,5 kg, 2–4 → ostane, 5+ → +2,5 kg
- Dodajanje, preimenovanje, brisanje in premikanje vaj
- kg / lb preklop
- Kalkulator uteži (koliko plošč na vsako stran)
- Backup: izvoz/uvoz JSON
- Sledenje napredka (kljukice) na vajo

## Datoteke
```
index.html              glavna aplikacija
manifest.webmanifest    PWA manifest
sw.js                   service worker (offline)
icon.svg                ikona (vektorska)
icon-192.png            ikona PWA
icon-512.png            ikona PWA
icon-512-maskable.png   ikona PWA (maskable)
icon-180.png            ikona za iOS (apple-touch-icon)
```

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
Service worker predpomni datoteke. Ko spremeniš katero koli datoteko, **povečaj številko različice** v `sw.js`:
```js
const CACHE = 'plus50-v2';   // v1 -> v2
```
Tako bodo uporabniki dobili novo različico ob naslednjem odprtju.

## Podatki / zasebnost
Vse je shranjeno lokalno v brskalniku (`localStorage`), nič ne gre na strežnik. Za varnost ali prenos na drugo napravo uporabi **Izvozi backup** in **Uvozi backup**.

## Opomba o natančnosti
Originalna aplikacija sproti prilagaja 1RM glede na Test sete, zato se lahko kakšna teža razlikuje za ~2,5 kg. Polje **Max** je tvoj nadzor — posodobi ga po vsakem Testu (gumbi to naredijo samodejno).
