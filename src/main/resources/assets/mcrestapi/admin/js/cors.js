var CorsPage = (function () {
  'use strict';

  function init() {
    loadCors();
    bindEvents();
  }

  function destroy() {}

  function bindEvents() {
    var toggle = document.getElementById('cors-toggle');
    if (toggle && !toggle._bound) {
      toggle._bound = true;
      toggle.addEventListener('change', async function () {
        try {
          await Api.request('/api/admin/cors', 'PUT', { enabled: toggle.checked });
          Utils.toast('CORS ' + (toggle.checked ? 'enabled' : 'disabled'), 'success');
          loadCors();
        } catch (e) {
          Utils.toast('Failed to update CORS: ' + e.message, 'error');
        }
      });
    }

    var addBtn = document.getElementById('add-origin-btn');
    var originInput = document.getElementById('cors-origin-input');

    if (addBtn && !addBtn._bound) {
      addBtn._bound = true;
      addBtn.addEventListener('click', addOrigin);
    }

    if (originInput && !originInput._bound) {
      originInput._bound = true;
      originInput.addEventListener('keydown', function (e) {
        if (e.key === 'Enter') addOrigin();
      });
    }
  }

  async function loadCors() {
    try {
      var data = await Api.request('/api/admin/cors', 'GET');
      document.getElementById('cors-toggle').checked = data.enabled;

      var badge = document.getElementById('page-badge');
      badge.textContent = data.enabled ? 'enabled' : 'disabled';

      var origins = data.allowed_origins || [];
      var list = document.getElementById('cors-origins-list');
      var empty = document.getElementById('cors-empty');

      if (origins.length === 0) {
        list.innerHTML = '';
        empty.classList.remove('hidden');
      } else {
        empty.classList.add('hidden');
        list.innerHTML = origins.map(function (o) {
          return '<div class="origin-item">' +
            '<span>' + Utils.escHtml(o) + '</span>' +
            '<button class="btn btn-danger btn-sm" onclick="CorsPage.removeOrigin(\'' + Utils.escAttr(o) + '\')">' + Utils.icons.close + ' Remove</button>' +
          '</div>';
        }).join('');
      }
    } catch (e) {
      Utils.toast('Failed to load CORS: ' + e.message, 'error');
    }
  }

  async function addOrigin() {
    var input = document.getElementById('cors-origin-input');
    var origin = input.value.trim();
    if (!origin) return;

    try {
      await Api.request('/api/admin/cors', 'POST', { origin: origin });
      input.value = '';
      Utils.toast('Origin added', 'success');
      loadCors();
    } catch (e) {
      Utils.toast('Failed to add origin: ' + e.message, 'error');
    }
  }

  async function removeOrigin(origin) {
    try {
      await Api.request('/api/admin/cors?origin=' + encodeURIComponent(origin), 'DELETE');
      Utils.toast('Origin removed', 'success');
      loadCors();
    } catch (e) {
      Utils.toast('Failed to remove origin: ' + e.message, 'error');
    }
  }

  return {
    init: init,
    destroy: destroy,
    removeOrigin: removeOrigin
  };
})();
