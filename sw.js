// Network-first for the app shell (HTML) => koda se posodablja samodejno ob vsakem odprtju,
// dokler je na voljo internet. Ikone in ostalo se strežejo iz predpomnilnika (cache-first).
// sw.js verzije ni treba ročno bumpati za spremembe index.html.
const STATIC = 'plus50-static-v1';
const ASSETS = [
  './manifest.webmanifest',
  './icon.svg',
  './icon-192.png',
  './icon-512.png',
  './icon-512-maskable.png',
  './icon-180.png'
];

self.addEventListener('install', function (e) {
  e.waitUntil(caches.open(STATIC).then(function (c) { return c.addAll(ASSETS); }).catch(function(){}));
  // NE klicemo skipWaiting tukaj: nova verzija caka, dokler uporabnik ne tapne "Osvezi".
});

self.addEventListener('activate', function (e) {
  e.waitUntil(self.clients.claim());
});

self.addEventListener('message', function (e) {
  if (e.data === 'skipWaiting') self.skipWaiting();
});

self.addEventListener('fetch', function (e) {
  var req = e.request;
  if (req.method !== 'GET') return;
  var url = new URL(req.url);
  var isHTML = req.mode === 'navigate' || req.destination === 'document'
            || url.pathname.endsWith('.html') || url.pathname.endsWith('/');

  if (isHTML) {
    // network-first: vedno poskusi sveze, ob offline padi na predpomnjeno
    e.respondWith(
      fetch(req).then(function (res) {
        var copy = res.clone();
        caches.open(STATIC).then(function (c) { c.put('./index.html', copy); });
        return res;
      }).catch(function () { return caches.match('./index.html'); })
    );
    return;
  }

  // staticne datoteke: cache-first
  e.respondWith(
    caches.match(req).then(function (hit) {
      return hit || fetch(req).then(function (res) {
        if (url.origin === location.origin && res.ok) {
          var copy = res.clone();
          caches.open(STATIC).then(function (c) { c.put(req, copy); });
        }
        return res;
      }).catch(function () { return hit; });
    })
  );
});
