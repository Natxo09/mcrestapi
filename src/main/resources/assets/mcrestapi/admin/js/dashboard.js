var DashboardPage = (function () {
  'use strict';

  var interval = null;

  function init() {
    load();
    interval = setInterval(load, 5000);
  }

  function destroy() {
    if (interval) { clearInterval(interval); interval = null; }
  }

  async function load() {
    try {
      var data = await Api.request('/api/server', 'GET');
      render(data);
    } catch (e) {
      // silent on interval errors
    }
  }

  function render(d) {
    var el = document.getElementById('dashboard-content');
    if (!el) return;

    var tpsClass = d.tps >= 19 ? 'stat-good' : d.tps >= 15 ? 'stat-warn' : 'stat-bad';
    var msptClass = d.mspt <= 50 ? 'stat-good' : d.mspt <= 100 ? 'stat-warn' : 'stat-bad';

    var mem = d.memory || {};
    var memPercent = mem.usage_percent || 0;
    var memBarClass = memPercent < 70 ? '' : memPercent < 90 ? 'warn' : 'danger';

    var cpu = d.cpu || {};
    var players = d.players || {};
    var props = d.properties || {};

    el.innerHTML =
      // Server info bar
      '<div class="server-info-bar">' +
        '<img class="server-icon" src="/api/server/icon" alt="" onerror="this.style.display=\'none\'">' +
        '<div class="server-details">' +
          '<h2>' + Utils.escHtml(d.motd || 'Minecraft Server') + '</h2>' +
          '<p>' + Utils.escHtml(d.version || '') + ' &middot; Port ' + (d.server_port || '25565') + ' &middot; Uptime ' + Utils.formatUptime(d.uptime_seconds || 0) + '</p>' +
        '</div>' +
      '</div>' +

      // Stat cards
      '<div class="grid grid-4">' +
        // TPS
        '<div class="stat-card">' +
          '<div class="stat-label">TPS</div>' +
          '<div class="stat-value ' + tpsClass + '">' + (d.tps || 0) + '</div>' +
          '<div class="stat-sub">of 20.0 max</div>' +
        '</div>' +

        // MSPT
        '<div class="stat-card">' +
          '<div class="stat-label">MSPT</div>' +
          '<div class="stat-value ' + msptClass + '">' + (d.mspt || 0) + '</div>' +
          '<div class="stat-sub">ms per tick</div>' +
        '</div>' +

        // Players
        '<div class="stat-card">' +
          '<div class="stat-label">Players</div>' +
          '<div class="stat-value">' + (players.online || 0) + '<span style="font-size:16px;color:var(--text-muted)"> / ' + (players.max || 20) + '</span></div>' +
          '<div class="stat-sub">online now</div>' +
        '</div>' +

        // CPU
        '<div class="stat-card">' +
          '<div class="stat-label">CPU Load</div>' +
          '<div class="stat-value">' + (cpu.process_load != null ? cpu.process_load.toFixed(1) + '%' : 'N/A') + '</div>' +
          '<div class="stat-sub">' + (cpu.available_processors || '?') + ' cores &middot; sys ' + (cpu.system_load != null ? cpu.system_load.toFixed(1) + '%' : 'N/A') + '</div>' +
        '</div>' +
      '</div>' +

      // Memory
      '<div class="card" style="margin-top:16px">' +
        '<div class="card-header">' +
          '<h3>Memory</h3>' +
          '<span class="text-muted">' + (mem.used_mb || 0) + ' MB / ' + (mem.max_mb || 0) + ' MB (' + memPercent.toFixed(1) + '%)</span>' +
        '</div>' +
        '<div class="progress-bar">' +
          '<div class="progress-fill ' + memBarClass + '" style="width:' + memPercent + '%"></div>' +
        '</div>' +
        '<div style="display:flex;justify-content:space-between;margin-top:8px;font-size:12px;color:var(--text-muted)">' +
          '<span>Used: ' + (mem.used_mb || 0) + ' MB</span>' +
          '<span>Allocated: ' + (mem.total_mb || 0) + ' MB</span>' +
          '<span>Max: ' + (mem.max_mb || 0) + ' MB</span>' +
        '</div>' +
      '</div>' +

      // Properties
      '<div class="card">' +
        '<div class="card-header"><h3>Server Properties</h3></div>' +
        '<div class="table-container">' +
          '<table>' +
            '<thead><tr><th>Property</th><th style="text-align:right">Value</th></tr></thead>' +
            '<tbody>' +
              propRow('Gamemode', props.gamemode) +
              propRow('Difficulty', props.difficulty) +
              propRow('Hardcore', props.hardcore) +
              propRow('Online Mode', d.online_mode) +
              propRow('Whitelist', props.whitelist) +
              propRow('Allow Flight', props.allow_flight) +
              propRow('View Distance', props.view_distance) +
              propRow('Sim Distance', props.simulation_distance) +
              propRow('Spawn Protection', props.spawn_protection) +
              propRow('Max World Size', props.max_world_size ? Utils.formatNumber(props.max_world_size) : undefined) +
            '</tbody>' +
          '</table>' +
        '</div>' +
      '</div>';
  }

  function propRow(label, value) {
    if (value === undefined || value === null) return '';
    var display;
    if (typeof value === 'boolean') {
      display = value
        ? '<span style="color:var(--accent)">Yes</span>'
        : '<span style="color:var(--text-muted)">No</span>';
    } else {
      display = '<span class="mono">' + Utils.escHtml(String(value)) + '</span>';
    }
    return '<tr>' +
      '<td>' + Utils.escHtml(label) + '</td>' +
      '<td style="text-align:right">' + display + '</td>' +
    '</tr>';
  }

  return { init: init, destroy: destroy };
})();
