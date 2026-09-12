// 페이지 자체를 캐시해서 비행기 모드에서도 열리게 한다.
// 환율 값은 localStorage 에 따로 저장되므로 오프라인에서도 마지막 값으로 계산된다.
const CACHE = "tokrw-v1";
const SHELL = ["./", "./index.html", "./manifest.webmanifest", "./rates.json",
               "./icons/icon-192.png", "./icons/icon-512.png", "./icons/icon-180.png"];

self.addEventListener("install", event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(SHELL)));
  self.skipWaiting();
});

self.addEventListener("activate", event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))));
  self.clients.claim();
});

self.addEventListener("fetch", event => {
  const request = event.request;
  // 환율 요청은 항상 네트워크로. 캐시하면 오래된 값이 굳는다.
  if (request.method !== "GET" || new URL(request.url).origin !== location.origin) return;

  // 페이지는 네트워크를 먼저 보고, 안 되면 캐시로 연다.
  event.respondWith(
    fetch(request)
      .then(response => {
        const copy = response.clone();
        caches.open(CACHE).then(cache => cache.put(request, copy));
        return response;
      })
      .catch(() => caches.match(request).then(hit => hit || caches.match("./index.html")))
  );
});
