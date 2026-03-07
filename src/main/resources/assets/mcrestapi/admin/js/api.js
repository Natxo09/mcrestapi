var Api = (function () {
  'use strict';

  var authKey = '';
  var baseUrl = window.location.origin;

  function setKey(key) {
    authKey = key;
  }

  function getKey() {
    return authKey;
  }

  async function request(path, method, body) {
    var opts = {
      method: method,
      headers: {
        'Authorization': 'Bearer ' + authKey,
        'Content-Type': 'application/json'
      }
    };
    if (body) opts.body = JSON.stringify(body);

    var res = await fetch(baseUrl + path, opts);
    var text = await res.text();
    var data = text ? JSON.parse(text) : {};

    if (!res.ok) {
      throw new Error(data.error || 'Request failed');
    }
    return data;
  }

  function sseConnect(path, onEvent, onError) {
    var url = baseUrl + path;
    var separator = path.indexOf('?') !== -1 ? '&' : '?';
    url += separator + '_auth=' + encodeURIComponent(authKey);

    var es = new EventSource(url);

    es.addEventListener('chat', function (e) { onEvent('chat', JSON.parse(e.data)); });
    es.addEventListener('command', function (e) { onEvent('command', JSON.parse(e.data)); });
    es.addEventListener('join', function (e) { onEvent('join', JSON.parse(e.data)); });
    es.addEventListener('leave', function (e) { onEvent('leave', JSON.parse(e.data)); });
    es.addEventListener('death', function (e) { onEvent('death', JSON.parse(e.data)); });
    es.addEventListener('game', function (e) { onEvent('game', JSON.parse(e.data)); });

    es.onerror = function () {
      if (onError) onError();
    };

    return es;
  }

  return {
    setKey: setKey,
    getKey: getKey,
    request: request,
    sseConnect: sseConnect
  };
})();
