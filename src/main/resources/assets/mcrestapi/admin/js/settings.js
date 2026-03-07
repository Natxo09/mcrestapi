var SettingsPage = (function () {
  'use strict';

  function init() {
    loadSettings();
    bindEvents();
  }

  function destroy() {}

  function bindEvents() {
    var toggle = document.getElementById('swagger-toggle');
    if (toggle && !toggle._bound) {
      toggle._bound = true;
      toggle.addEventListener('change', async function () {
        try {
          await Api.request('/api/admin/settings', 'PUT', { swagger_enabled: toggle.checked });
          Utils.toast('Swagger ' + (toggle.checked ? 'enabled' : 'disabled') + ' (restart required)', 'success');
        } catch (e) {
          Utils.toast('Failed to update settings: ' + e.message, 'error');
        }
      });
    }
  }

  async function loadSettings() {
    try {
      var data = await Api.request('/api/admin/settings', 'GET');
      document.getElementById('swagger-toggle').checked = data.swagger_enabled;

      document.getElementById('page-badge').textContent = '';

      var info = document.getElementById('settings-info');
      info.innerHTML =
        '<div class="toggle-row">' +
          '<div><div class="toggle-label">Port</div><div class="toggle-desc">HTTP server port</div></div>' +
          '<span class="mono">' + data.port + '</span>' +
        '</div>' +
        '<div class="toggle-row">' +
          '<div><div class="toggle-label">Bind Address</div><div class="toggle-desc">Network interface</div></div>' +
          '<span class="mono">' + Utils.escHtml(data.bind_address) + '</span>' +
        '</div>' +
        '<div class="toggle-row">' +
          '<div><div class="toggle-label">Max Connections</div><div class="toggle-desc">Connection backlog</div></div>' +
          '<span class="mono">' + data.max_connections + '</span>' +
        '</div>';
    } catch (e) {
      Utils.toast('Failed to load settings: ' + e.message, 'error');
    }
  }

  return { init: init, destroy: destroy };
})();
