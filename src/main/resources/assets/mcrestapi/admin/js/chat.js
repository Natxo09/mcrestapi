var ChatPage = (function () {
  'use strict';

  var eventSource = null;
  var activeFilters = new Set();
  var allTypes = ['chat', 'command', 'join', 'leave', 'death', 'game'];

  var eventIcons = {
    command: '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" fill="currentColor" viewBox="0 0 256 256"><path d="M215.79,118.17a8,8,0,0,0-5-5.66L153.18,90.9l14.66-73.33a8,8,0,0,0-13.69-7l-112,120a8,8,0,0,0,3,13l57.63,21.61L88.16,238.43a8,8,0,0,0,13.69,7l112-120A8,8,0,0,0,215.79,118.17ZM109.37,214l10.47-52.38a8,8,0,0,0-5-9.06L62,132.71l84.62-90.66L136.16,94.43a8,8,0,0,0,5,9.06l52.8,19.8Z"/></svg>',
    join: '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" fill="currentColor" viewBox="0 0 256 256"><path d="M224,128a8,8,0,0,1-8,8H136v80a8,8,0,0,1-16,0V136H40a8,8,0,0,1,0-16h80V40a8,8,0,0,1,16,0v80h80A8,8,0,0,1,224,128Z"/></svg>',
    leave: '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" fill="currentColor" viewBox="0 0 256 256"><path d="M224,128a8,8,0,0,1-8,8H40a8,8,0,0,1,0-16H216A8,8,0,0,1,224,128Z"/></svg>',
    death: '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" fill="currentColor" viewBox="0 0 256 256"><path d="M92,104a28,28,0,1,0,28,28A28,28,0,0,0,92,104Zm0,40a12,12,0,1,1,12-12A12,12,0,0,1,92,144Zm72-40a28,28,0,1,0,28,28A28,28,0,0,0,164,104Zm0,40a12,12,0,1,1,12-12A12,12,0,0,1,164,144ZM128,16C70.65,16,24,60.86,24,116c0,34.1,18.27,66,48,84.28V216a16,16,0,0,0,16,16h80a16,16,0,0,0,16-16V200.28C213.73,182,232,150.1,232,116,232,60.86,185.35,16,128,16Zm44.12,172.69a8,8,0,0,0-4.12,7V216H152V192a8,8,0,0,0-16,0v24H120V192a8,8,0,0,0-16,0v24H88V195.69a8,8,0,0,0-4.12-7C56.81,173.69,40,145.84,40,116c0-46.32,39.48-84,88-84s88,37.68,88,84C216,145.83,199.19,173.69,172.12,188.69Z"/></svg>',
    game: '<svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" fill="currentColor" viewBox="0 0 256 256"><path d="M128,80a48,48,0,1,0,48,48A48.05,48.05,0,0,0,128,80Zm0,80a32,32,0,1,1,32-32A32,32,0,0,1,128,160Zm88-29.84q.06-2.16,0-4.32l14.92-18.64a8,8,0,0,0,1.48-7.06,107.21,107.21,0,0,0-10.88-26.25,8,8,0,0,0-6-3.93l-23.72-2.64q-1.48-1.56-3-3L186,40.54a8,8,0,0,0-3.94-6,107.71,107.71,0,0,0-26.25-10.87,8,8,0,0,0-7.06,1.49L130.16,40Q128,40,125.84,40L107.2,25.11a8,8,0,0,0-7.06-1.48A107.6,107.6,0,0,0,73.89,34.51a8,8,0,0,0-3.93,6L67.32,64.27q-1.56,1.49-3,3L40.54,70a8,8,0,0,0-6,3.94,107.71,107.71,0,0,0-10.87,26.25,8,8,0,0,0,1.49,7.06L40,125.84Q40,128,40,130.16L25.11,148.8a8,8,0,0,0-1.48,7.06,107.21,107.21,0,0,0,10.88,26.25,8,8,0,0,0,6,3.93l23.72,2.64q1.49,1.56,3,3L70,215.46a8,8,0,0,0,3.94,6,107.71,107.71,0,0,0,26.25,10.87,8,8,0,0,0,7.06-1.49L125.84,216q2.16.06,4.32,0l18.64,14.92a8,8,0,0,0,7.06,1.48,107.21,107.21,0,0,0,26.25-10.88,8,8,0,0,0,3.93-6l2.64-23.72q1.56-1.48,3-3L215.46,186a8,8,0,0,0,6-3.94,107.71,107.71,0,0,0,10.87-26.25,8,8,0,0,0-1.49-7.06ZM199.87,123.66a73.93,73.93,0,0,1,0,8.68,8,8,0,0,0,1.74,5.48l14.19,17.73a91.57,91.57,0,0,1-6.23,15L187,173.11a8,8,0,0,0-5.1,2.64,74.11,74.11,0,0,1-6.14,6.14,8,8,0,0,0-2.64,5.1l-2.51,22.58a91.32,91.32,0,0,1-15,6.23l-17.74-14.19a8,8,0,0,0-5-1.75h-.48a73.93,73.93,0,0,1-8.68,0,8,8,0,0,0-5.48,1.74L100.45,215.8a91.57,91.57,0,0,1-15-6.23L82.89,187a8,8,0,0,0-2.64-5.1,74.11,74.11,0,0,1-6.14-6.14,8,8,0,0,0-5.1-2.64L46.43,170.6a91.32,91.32,0,0,1-6.23-15l14.19-17.74a8,8,0,0,0,1.74-5.48,73.93,73.93,0,0,1,0-8.68,8,8,0,0,0-1.74-5.48L40.2,100.45a91.57,91.57,0,0,1,6.23-15L69,82.89a8,8,0,0,0,5.1-2.64,74.11,74.11,0,0,1,6.14-6.14A8,8,0,0,0,82.89,69L85.4,46.43a91.32,91.32,0,0,1,15-6.23l17.74,14.19a8,8,0,0,0,5.48,1.74,73.93,73.93,0,0,1,8.68,0,8,8,0,0,0,5.48-1.74L155.55,40.2a91.57,91.57,0,0,1,15,6.23L173.11,69a8,8,0,0,0,2.64,5.1,74.11,74.11,0,0,1,6.14,6.14,8,8,0,0,0,5.1,2.64l22.58,2.51a91.32,91.32,0,0,1,6.23,15l-14.19,17.74A8,8,0,0,0,199.87,123.66Z"/></svg>'
  };

  function playerHeadUrl(name) {
    return 'https://mc-heads.net/avatar/' + encodeURIComponent(name) + '/32';
  }

  function init() {
    var el = document.getElementById('chat-content');
    if (!el) return;

    el.innerHTML =
      '<div class="chat-container">' +
        '<div class="chat-filters" id="chat-filters"></div>' +
        '<div class="chat-status" id="chat-status">' +
          '<span class="status-dot"></span> Connecting...' +
        '</div>' +
        '<div class="chat-feed" id="chat-feed"></div>' +
      '</div>';

    renderFilters();
    loadHistory();
    connectSSE();
  }

  function destroy() {
    if (eventSource) {
      eventSource.close();
      eventSource = null;
    }
    activeFilters.clear();
  }

  function renderFilters() {
    var container = document.getElementById('chat-filters');
    if (!container) return;

    container.innerHTML = allTypes.map(function (type) {
      var active = activeFilters.size === 0 || activeFilters.has(type);
      return '<button class="chat-filter' + (active ? ' active' : '') + '" data-type="' + type + '">' +
        type +
      '</button>';
    }).join('');

    container.querySelectorAll('.chat-filter').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var type = btn.dataset.type;
        if (activeFilters.has(type)) {
          activeFilters.delete(type);
        } else {
          activeFilters.add(type);
        }
        if (activeFilters.size === allTypes.length) {
          activeFilters.clear();
        }
        renderFilters();
        applyFilters();
      });
    });
  }

  function applyFilters() {
    var feed = document.getElementById('chat-feed');
    if (!feed) return;

    feed.querySelectorAll('.chat-event').forEach(function (el) {
      var type = el.dataset.type;
      var visible = activeFilters.size === 0 || activeFilters.has(type);
      el.style.display = visible ? '' : 'none';
    });
  }

  async function loadHistory() {
    try {
      var data = await Api.request('/api/chat?limit=100', 'GET');
      var feed = document.getElementById('chat-feed');
      if (!feed || !data.events) return;

      data.events.forEach(function (event) {
        feed.appendChild(createEventElement(event));
      });
      scrollToBottom();
    } catch (e) {
      // silent
    }
  }

  function connectSSE() {
    var statusEl = document.getElementById('chat-status');

    eventSource = Api.sseConnect('/api/events/stream', function (type, event) {
      var feed = document.getElementById('chat-feed');
      if (!feed) return;

      var el = createEventElement(event);
      feed.appendChild(el);

      if (activeFilters.size > 0 && !activeFilters.has(event.type)) {
        el.style.display = 'none';
      }

      scrollToBottom();
    }, function () {
      if (statusEl) {
        statusEl.innerHTML = '<span class="status-dot disconnected"></span> Disconnected — retrying...';
      }
    });

    eventSource.onopen = function () {
      if (statusEl) {
        statusEl.innerHTML = '<span class="status-dot"></span> Connected — live';
      }
    };
  }

  function createEventElement(event) {
    var div = document.createElement('div');
    div.dataset.type = event.type;

    var isPlayerMessage = event.type === 'chat' || event.type === 'command';
    var hasPlayer = event.player && event.player !== 'Server';

    div.className = 'chat-event chat-message chat-type-' + event.type;

    // Avatar: player head or event icon
    var avatarHtml;
    if (hasPlayer) {
      avatarHtml = '<img class="chat-avatar" src="' + playerHeadUrl(event.player) + '" alt="" onerror="this.style.visibility=\'hidden\'">';
    } else {
      avatarHtml = '<div class="chat-avatar chat-avatar-icon">' + (eventIcons[event.type] || eventIcons.game) + '</div>';
    }

    // Name line
    var nameHtml;
    if (event.type === 'chat' || event.type === 'command') {
      nameHtml = Utils.escHtml(event.player || 'Server');
    } else if (event.type === 'join') {
      nameHtml = Utils.escHtml(event.player || '') + ' <span class="chat-event-label chat-label-join">joined</span>';
    } else if (event.type === 'leave') {
      nameHtml = Utils.escHtml(event.player || '') + ' <span class="chat-event-label chat-label-leave">left</span>';
    } else if (event.type === 'death') {
      nameHtml = '<span class="chat-event-label chat-label-death">' + Utils.escHtml(event.player || 'Player') + ' died</span>';
    } else {
      nameHtml = '<span class="chat-event-label chat-label-game">Server</span>';
    }

    // Content
    var contentHtml;
    if (event.type === 'chat') {
      contentHtml = Utils.escHtml(event.content || '');
    } else if (event.type === 'command') {
      contentHtml = '<span class="cmd-text">/' + Utils.escHtml(event.content || '') + '</span>';
    } else {
      contentHtml = Utils.escHtml(event.content || '');
    }

    div.innerHTML =
      avatarHtml +
      '<div class="chat-msg-body">' +
        '<div class="chat-msg-header">' +
          '<span class="chat-msg-name">' + nameHtml + '</span>' +
          '<span class="chat-msg-time">' + Utils.formatTime(event.timestamp) + '</span>' +
        '</div>' +
        '<div class="chat-msg-text">' + contentHtml + '</div>' +
      '</div>';

    return div;
  }

  function scrollToBottom() {
    var feed = document.getElementById('chat-feed');
    if (feed) {
      feed.scrollTop = feed.scrollHeight;
    }
  }

  return { init: init, destroy: destroy };
})();
