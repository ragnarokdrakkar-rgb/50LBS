// Network-first for the app shell (HTML) => koda se posodablja samodejno ob vsakem odprtju,
// dokler je na voljo internet. Ikone, pisave in ostalo se strežejo iz predpomnilnika (cache-first).
// sw.js verzije ni treba ročno bumpati za spremembe index.html.
const STATIC = 'plus50-static-v2';
const ASSETS = [
  './',
  './index.html',
  './manifest.webmanifest',
  './icon.svg',
  './icon-192.png',
  './icon-512.png',
  './icon-512-maskable.png',
  './icon-180.png'
];
const FONT_HOSTS = ['fonts.googleapis.com', 'fonts.gstatic.com'];

self.addEventListener('install', function (e) {
  // posamezne datoteke, da ena manjkajoča ne prepreči predpomnjenja ostalih
  e.waitUntil(caches.open(STATIC).then(function (c) {
    return Promise.all(ASSETS.map(function (a) { return c.add(a).catch(function () {}); }));
  }));
  // NE klicemo skipWaiting tukaj: nova verzija caka, dokler uporabnik ne tapne "Osveži".
});

self.addEventListener('activate', function (e) {
  e.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(keys.filter(function (k) { return k !== STATIC; }).map(function (k) { return caches.delete(k); }));
    }).then(function () { return self.clients.claim(); })
  );
});

self.addEventListener('message', function (e) {
  if (e.data === 'skipWaiting') self.skipWaiting();
});

self.addEventListener('fetch', function (e) {
  var req = e.request;
  if (req.method !== 'GET') return;
  var url = new URL(req.url);
  var sameOrigin = url.origin === location.origin;
  var isFont = FONT_HOSTS.indexOf(url.hostname) !== -1;
  if (!sameOrigin && !isFont) return;

  var isHTML = req.mode === 'navigate' || req.destination === 'document'
            || (sameOrigin && (url.pathname.endsWith('.html') || url.pathname.endsWith('/')));

  if (isHTML) {
    // network-first: vedno poskusi sveze, ob offline padi na predpomnjeno
    e.respondWith(
      fetch(req).then(function (res) {
        if (res.ok) {
          var copy = res.clone();
          caches.open(STATIC).then(function (c) { c.put('./index.html', copy); });
        }
        return res;
      }).catch(function () {
        return caches.match('./index.html').then(function (hit) { return hit || caches.match('./'); });
      })
    );
    return;
  }

  // staticne datoteke in pisave: cache-first
  e.respondWith(
    caches.match(req).then(function (hit) {
      return hit || fetch(req).then(function (res) {
        if (res.ok || (isFont && res.type === 'opaque')) {
          var copy = res.clone();
          caches.open(STATIC).then(function (c) { c.put(req, copy); });
        }
        return res;
      });
    })
  );
});
