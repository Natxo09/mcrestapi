var WorldPage = (function () {
  'use strict';

  var interval = null;

  function init() {
    load();
    interval = setInterval(load, 30000);
  }

  function destroy() {
    if (interval) { clearInterval(interval); interval = null; }
  }

  async function load() {
    try {
      var data = await Api.request('/api/world', 'GET');
      render(data);
    } catch (e) {
      // silent
    }
  }

  function render(data) {
    var el = document.getElementById('world-content');
    if (!el) return;

    var g = data.global || {};
    var time = g.time || {};
    var weather = g.weather || {};
    var spawn = g.spawn || {};
    var dims = data.dimensions || [];

    var weatherSvg = weather.thundering
      ? '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="currentColor" viewBox="0 0 256 256"><path d="M215.79,118.17a8,8,0,0,0-5-5.66L153.18,90.9l14.66-73.33a8,8,0,0,0-13.69-7l-112,120a8,8,0,0,0,3,13l57.63,21.61L88.16,238.43a8,8,0,0,0,13.69,7l112-120A8,8,0,0,0,215.79,118.17ZM109.37,214l10.47-52.38a8,8,0,0,0-5-9.06L62,132.71l84.62-90.66L136.16,94.43a8,8,0,0,0,5,9.06l52.8,19.8Z"/></svg>'
      : weather.raining
      ? '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="currentColor" viewBox="0 0 256 256"><path d="M158.66,196.44l-32,48a8,8,0,1,1-13.32-8.88l32-48a8,8,0,0,1,13.32,8.88ZM232,92a76.08,76.08,0,0,1-76,76H132.28l-29.62,44.44a8,8,0,1,1-13.32-8.88L113.05,168H76A52,52,0,0,1,76,64a53.26,53.26,0,0,1,8.92.76A76.08,76.08,0,0,1,232,92Zm-16,0A60.06,60.06,0,0,0,96,88.46a8,8,0,0,1-16-.92q.21-3.66.77-7.23A38.11,38.11,0,0,0,76,80a36,36,0,0,0,0,72h80A60.07,60.07,0,0,0,216,92Z"/></svg>'
      : '<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="currentColor" viewBox="0 0 256 256"><path d="M120,40V16a8,8,0,0,1,16,0V40a8,8,0,0,1-16,0Zm72,88a64,64,0,1,1-64-64A64.07,64.07,0,0,1,192,128Zm-16,0a48,48,0,1,0-48,48A48.05,48.05,0,0,0,176,128ZM58.34,69.66A8,8,0,0,0,69.66,58.34l-16-16A8,8,0,0,0,42.34,53.66Zm0,116.68-16,16a8,8,0,0,0,11.32,11.32l16-16a8,8,0,0,0-11.32-11.32ZM192,72a8,8,0,0,0,5.66-2.34l16-16a8,8,0,0,0-11.32-11.32l-16,16A8,8,0,0,0,192,72Zm5.66,114.34a8,8,0,0,0-11.32,11.32l16,16a8,8,0,0,0,11.32-11.32ZM48,128a8,8,0,0,0-8-8H16a8,8,0,0,0,0,16H40A8,8,0,0,0,48,128Zm80,80a8,8,0,0,0-8,8v24a8,8,0,0,0,16,0V216A8,8,0,0,0,128,208Zm112-88H216a8,8,0,0,0,0,16h24a8,8,0,0,0,0-16Z"/></svg>';
    var weatherText = weather.thundering ? 'Thunder' : weather.raining ? 'Rain' : 'Clear';

    var dayTicks = time.day_time || 0;
    var hours = Math.floor(((dayTicks + 6000) % 24000) / 1000);
    var mins = Math.floor((((dayTicks + 6000) % 24000) % 1000) / 16.67);
    var timeStr = String(hours).padStart(2, '0') + ':' + String(mins).padStart(2, '0');
    var isNight = dayTicks >= 13000 && dayTicks < 23000;

    el.innerHTML =
      // Global info cards
      '<div class="grid grid-4">' +
        '<div class="stat-card">' +
          '<div class="stat-label">Time</div>' +
          '<div class="stat-value">' + timeStr + '</div>' +
          '<div class="stat-sub">Day ' + (time.day_count || 0) + ' &middot; ' + (isNight ? 'Night' : 'Day') + '</div>' +
        '</div>' +
        '<div class="stat-card">' +
          '<div class="stat-label">Weather</div>' +
          '<div class="stat-value"><span class="world-weather-icon">' + weatherSvg + '</span>' + weatherText + '</div>' +
          '<div class="stat-sub">&nbsp;</div>' +
        '</div>' +
        '<div class="stat-card">' +
          '<div class="stat-label">Difficulty</div>' +
          '<div class="stat-value" style="text-transform:capitalize">' + Utils.escHtml(g.difficulty || 'normal') + '</div>' +
          '<div class="stat-sub">' + (g.hardcore ? 'Hardcore' : 'Normal') + (g.difficulty_locked ? ' (locked)' : '') + '</div>' +
        '</div>' +
        '<div class="stat-card">' +
          '<div class="stat-label">Spawn</div>' +
          '<div class="stat-value mono" style="font-size:18px">' + (spawn.x || 0) + ', ' + (spawn.y || 0) + ', ' + (spawn.z || 0) + '</div>' +
          '<div class="stat-sub">PvP: ' + (g.pvp ? 'On' : 'Off') + ' &middot; Seed: ' + (g.seed || '?') + '</div>' +
        '</div>' +
      '</div>' +

      // Dimensions
      '<div class="card" style="margin-top:16px">' +
        '<div class="card-header"><h3>Dimensions</h3></div>' +
        '<div class="grid grid-3">' +
          dims.map(function (dim) {
            var name = dim.name || '';
            var shortName = name.replace('minecraft:', '');
            var icon = shortName === 'overworld'
              ? '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 256 256"><path d="M128,24A104,104,0,1,0,232,128,104.11,104.11,0,0,0,128,24Zm88,104a87.62,87.62,0,0,1-6.4,32.94l-44.7-27.49a15.92,15.92,0,0,0-6.24-2.23l-22.82-3.08a16.11,16.11,0,0,0-16,7.86h-8.72l-3.8-7.86a15.91,15.91,0,0,0-11-8.67l-8-1.73L96.14,104h16.71a16.06,16.06,0,0,0,7.73-2l12.25-6.76a16.62,16.62,0,0,0,3-2.14l26.91-24.34A15.93,15.93,0,0,0,166,49.1l-.36-.65A88.11,88.11,0,0,1,216,128ZM143.31,41.34,152,56.9,125.09,81.24,112.85,88H96.14a16,16,0,0,0-13.88,8l-8.73,15.23L63.38,84.19,74.32,58.32a87.87,87.87,0,0,1,69-17ZM40,128a87.53,87.53,0,0,1,8.54-37.8l11.34,30.27a16,16,0,0,0,11.62,10l21.43,4.61L96.74,143a16.09,16.09,0,0,0,14.4,9h1.48l-7.23,16.23a16,16,0,0,0,2.86,17.37l.14.14L128,205.94l-1.94,10A88.11,88.11,0,0,1,40,128Zm102.58,86.78,1.13-5.81a16.09,16.09,0,0,0-4-13.9,1.85,1.85,0,0,1-.14-.14L120,174.74,133.7,144l22.82,3.08,45.72,28.12A88.18,88.18,0,0,1,142.58,214.78Z"/></svg>'
              : shortName === 'the_nether'
              ? '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 256 256"><path d="M183.89,153.34a57.6,57.6,0,0,1-46.56,46.55A8.75,8.75,0,0,1,136,200a8,8,0,0,1-1.32-15.89c16.57-2.79,30.63-16.85,33.44-33.45a8,8,0,0,1,15.78,2.68ZM216,144a88,88,0,0,1-176,0c0-27.92,11-56.47,32.66-84.85a8,8,0,0,1,11.93-.89l24.12,23.41,22-60.41a8,8,0,0,1,12.63-3.41C165.21,36,216,84.55,216,144Zm-16,0c0-46.09-35.79-85.92-58.21-106.33L119.52,98.74a8,8,0,0,1-13.09,3L80.06,76.16C64.09,99.21,56,122,56,144a72,72,0,0,0,144,0Z"/></svg>'
              : shortName === 'the_end'
              ? '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 256 256"><path d="M239.18,97.26A16.38,16.38,0,0,0,224.92,86l-59-4.76L143.14,26.15a16.36,16.36,0,0,0-30.27,0L90.11,81.23,31.08,86a16.46,16.46,0,0,0-9.37,28.86l45,38.83L53,211.75a16.38,16.38,0,0,0,24.5,17.82L128,198.49l50.53,31.08A16.4,16.4,0,0,0,203,211.75l-13.76-58.07,45-38.83A16.43,16.43,0,0,0,239.18,97.26Zm-15.34,5.47-48.7,42a8,8,0,0,0-2.56,7.91l14.88,62.8a.37.37,0,0,1-.17.48c-.18.14-.23.11-.38,0l-54.72-33.65a8,8,0,0,0-8.38,0L69.09,215.94c-.15.09-.19.12-.38,0a.37.37,0,0,1-.17-.48l14.88-62.8a8,8,0,0,0-2.56-7.91l-48.7-42c-.12-.1-.23-.19-.13-.5s.18-.27.33-.29l63.92-5.16A8,8,0,0,0,103,91.86l24.62-59.61c.08-.17.11-.25.35-.25s.27.08.35.25L153,91.86a8,8,0,0,0,6.75,4.92l63.92,5.16c.15,0,.24,0,.33.29S224,102.63,223.84,102.73Z"/></svg>'
              : '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 256 256"><path d="M128,24h0A104,104,0,1,0,232,128,104.12,104.12,0,0,0,128,24Zm88,104a87.61,87.61,0,0,1-3.33,24H174.16a157.44,157.44,0,0,0,0-48h38.51A87.61,87.61,0,0,1,216,128ZM102,168H154a115.11,115.11,0,0,1-26,45A115.27,115.27,0,0,1,102,168Zm-3.9-16a140.84,140.84,0,0,1,0-48h59.88a140.84,140.84,0,0,1,0,48ZM40,128a87.61,87.61,0,0,1,3.33-24H81.84a157.44,157.44,0,0,0,0,48H43.33A87.61,87.61,0,0,1,40,128ZM154,88H102a115.11,115.11,0,0,1,26-45A115.27,115.27,0,0,1,154,88Z"/></svg>';
            var wb = dim.world_border || {};

            return '<div class="dimension-card">' +
              '<h4>' + icon + ' <span style="text-transform:capitalize">' + Utils.escHtml(shortName.replace(/_/g, ' ')) + '</span></h4>' +
              '<div class="dim-stats">' +
                dimRow('Loaded Chunks', Utils.formatNumber(dim.loaded_chunks || 0)) +
                dimRow('Entities', Utils.formatNumber(dim.entity_count || 0)) +
                dimRow('Sea Level', dim.sea_level) +
                dimRow('Flat World', dim.flat ? 'Yes' : 'No') +
                dimRow('Border Size', wb.size ? Utils.formatNumber(Math.round(wb.size)) : '-') +
                dimRow('Border Center', wb.center_x != null ? Math.round(wb.center_x) + ', ' + Math.round(wb.center_z) : '-') +
              '</div>' +
            '</div>';
          }).join('') +
        '</div>' +
      '</div>';
  }

  function dimRow(label, value) {
    return '<span class="dim-stat-label">' + label + '</span>' +
           '<span class="dim-stat-value">' + (value != null ? value : '-') + '</span>';
  }

  return { init: init, destroy: destroy };
})();
