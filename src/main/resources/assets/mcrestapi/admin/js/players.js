var PlayersPage = (function () {
  'use strict';

  var interval = null;

  function init() {
    load();
    interval = setInterval(load, 10000);
  }

  function destroy() {
    if (interval) { clearInterval(interval); interval = null; }
  }

  async function load() {
    try {
      var data = await Api.request('/api/players', 'GET');
      render(data);
    } catch (e) {
      // silent
    }
  }

  function render(data) {
    var el = document.getElementById('players-content');
    if (!el) return;

    var badge = document.getElementById('page-badge');
    badge.textContent = (data.count || 0) + ' online';

    if (!data.players || data.players.length === 0) {
      el.innerHTML = '<div class="empty-state">No players online</div>';
      return;
    }

    el.innerHTML = '<div class="player-grid">' +
      data.players.map(function (p) {
        return '<div class="player-card">' +
          '<img class="player-avatar" src="' + Utils.escAttr(p.skin_head_url || '') + '" alt="' + Utils.escAttr(p.name) + '" onerror="this.style.display=\'none\'">' +
          '<div class="player-info">' +
            '<div class="player-name">' +
              Utils.escHtml(p.name) +
              (p.is_op ? ' <span class="op-badge">OP</span>' : '') +
            '</div>' +
            '<div class="player-meta">' +
              Utils.escHtml(p.dimension || '') + ' &middot; ' + Utils.escHtml(p.game_mode || '') + ' &middot; ' + (p.ping_ms || 0) + 'ms' +
            '</div>' +
            healthBar(p.health || 0) +
            foodBar(p.food_level || 0) +
            '<div class="player-stats">' +
              '<span style="color:var(--text-muted)">X: ' + Math.round(p.position.x) + ' Y: ' + Math.round(p.position.y) + ' Z: ' + Math.round(p.position.z) + '</span>' +
            '</div>' +
          '</div>' +
        '</div>';
      }).join('') +
    '</div>';
  }

  function healthBar(health) {
    var full = Math.floor(health / 2);
    var max = 10;
    var html = '<div class="health-bar">';
    for (var i = 0; i < max; i++) {
      html += '<div class="heart' + (i >= full ? ' empty' : '') + '"></div>';
    }
    html += '</div>';
    return html;
  }

  function foodBar(food) {
    var full = Math.floor(food / 2);
    var max = 10;
    var html = '<div class="food-bar">';
    for (var i = 0; i < max; i++) {
      html += '<div class="drumstick' + (i >= full ? ' empty' : '') + '"></div>';
    }
    html += '</div>';
    return html;
  }

  return { init: init, destroy: destroy };
})();
