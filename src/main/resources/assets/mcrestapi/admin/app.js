(function () {
  'use strict';

  let masterKey = '';
  const baseUrl = window.location.origin;

  // ─── API helper ─────────────────────────

  async function api(path, method, body) {
    const opts = {
      method,
      headers: {
        'Authorization': 'Bearer ' + masterKey,
        'Content-Type': 'application/json'
      }
    };
    if (body) opts.body = JSON.stringify(body);

    const res = await fetch(baseUrl + path, opts);
    const text = await res.text();
    const data = text ? JSON.parse(text) : {};

    if (!res.ok) {
      throw new Error(data.error || 'Request failed');
    }
    return data;
  }

  // ─── Toast ──────────────────────────────

  function toast(message, type) {
    const container = document.getElementById('toast-container');
    const el = document.createElement('div');
    el.className = 'toast ' + type;
    el.textContent = message;
    container.appendChild(el);
    setTimeout(function () { el.remove(); }, 3000);
  }

  // ─── Login ──────────────────────────────

  document.getElementById('login-btn').addEventListener('click', doLogin);
  document.getElementById('master-key-input').addEventListener('keydown', function (e) {
    if (e.key === 'Enter') doLogin();
  });

  async function doLogin() {
    const input = document.getElementById('master-key-input');
    const key = input.value.trim();
    if (!key) return;

    masterKey = key;

    try {
      await api('/api/admin/keys', 'GET');
      sessionStorage.setItem('mk', key);
      document.getElementById('login-screen').classList.add('hidden');
      document.getElementById('app').classList.remove('hidden');
      loadKeys();
    } catch (e) {
      masterKey = '';
      toast('Invalid master key', 'error');
    }
  }

  // Auto-login from session
  const savedKey = sessionStorage.getItem('mk');
  if (savedKey) {
    masterKey = savedKey;
    api('/api/admin/keys', 'GET')
      .then(function () {
        document.getElementById('login-screen').classList.add('hidden');
        document.getElementById('app').classList.remove('hidden');
        loadKeys();
      })
      .catch(function () {
        sessionStorage.removeItem('mk');
        masterKey = '';
      });
  }

  // ─── Logout ─────────────────────────────

  document.getElementById('logout-btn').addEventListener('click', function () {
    sessionStorage.removeItem('mk');
    masterKey = '';
    document.getElementById('app').classList.add('hidden');
    document.getElementById('login-screen').classList.remove('hidden');
    document.getElementById('master-key-input').value = '';
  });

  // ─── Navigation ─────────────────────────

  var navItems = document.querySelectorAll('.nav-item');
  navItems.forEach(function (item) {
    item.addEventListener('click', function () {
      var page = item.dataset.page;
      navItems.forEach(function (n) { n.classList.remove('active'); });
      item.classList.add('active');

      document.querySelectorAll('.page').forEach(function (p) { p.classList.add('hidden'); });
      document.getElementById('page-' + page).classList.remove('hidden');

      var titles = { keys: 'API Keys', cors: 'CORS', settings: 'Settings' };
      document.getElementById('page-title').textContent = titles[page] || page;

      if (page === 'keys') loadKeys();
      if (page === 'cors') loadCors();
      if (page === 'settings') loadSettings();
    });
  });

  // ─── Keys ───────────────────────────────

  async function loadKeys() {
    try {
      var data = await api('/api/admin/keys', 'GET');
      var keys = data.keys || [];
      var tbody = document.getElementById('keys-table-body');
      var empty = document.getElementById('keys-empty');
      var badge = document.getElementById('page-badge');

      badge.textContent = keys.length + ' key' + (keys.length !== 1 ? 's' : '');

      if (keys.length === 0) {
        tbody.innerHTML = '';
        empty.classList.remove('hidden');
        return;
      }

      empty.classList.add('hidden');
      tbody.innerHTML = keys.map(function (k) {
        var perms = k.permissions.map(function (p) {
          var cls = p === '*' ? 'tag wildcard' : 'tag';
          return '<span class="' + cls + '">' + escHtml(p) + '</span>';
        }).join('');

        var date = k.created_at ? new Date(k.created_at).toLocaleDateString() : '-';

        return '<tr>' +
          '<td><strong>' + escHtml(k.name) + '</strong></td>' +
          '<td class="mono text-muted">' + escHtml(k.id) + '</td>' +
          '<td>' + perms + '</td>' +
          '<td class="text-muted">' + date + '</td>' +
          '<td style="text-align:right">' +
            '<button class="btn btn-ghost btn-sm" onclick="editKey(\'' + escAttr(k.id) + '\')">Edit</button> ' +
            '<button class="btn btn-danger btn-sm" onclick="deleteKey(\'' + escAttr(k.id) + '\', \'' + escAttr(k.name) + '\')">Revoke</button>' +
          '</td>' +
        '</tr>';
      }).join('');
    } catch (e) {
      toast('Failed to load keys: ' + e.message, 'error');
    }
  }

  // Create key modal
  document.getElementById('create-key-btn').addEventListener('click', function () {
    document.getElementById('new-key-name').value = '';
    document.querySelectorAll('#create-key-modal input[type=checkbox]').forEach(function (cb) { cb.checked = false; });
    document.getElementById('create-key-modal').classList.remove('hidden');
  });

  document.getElementById('cancel-create-btn').addEventListener('click', function () {
    document.getElementById('create-key-modal').classList.add('hidden');
  });

  // Wildcard toggle disables individual perms
  document.getElementById('perm-all').addEventListener('change', function () {
    var singles = document.querySelectorAll('#create-key-modal .perm-single');
    singles.forEach(function (cb) {
      cb.disabled = document.getElementById('perm-all').checked;
      if (document.getElementById('perm-all').checked) cb.checked = false;
    });
  });

  document.getElementById('confirm-create-btn').addEventListener('click', async function () {
    var name = document.getElementById('new-key-name').value.trim();
    if (!name) { toast('Name is required', 'error'); return; }

    var perms = [];
    if (document.getElementById('perm-all').checked) {
      perms = ['*'];
    } else {
      document.querySelectorAll('#create-key-modal .perm-single:checked').forEach(function (cb) {
        perms.push(cb.value);
      });
    }
    if (perms.length === 0) { toast('Select at least one permission', 'error'); return; }

    try {
      var data = await api('/api/admin/keys', 'POST', { name: name, permissions: perms });
      document.getElementById('create-key-modal').classList.add('hidden');
      document.getElementById('created-key-value').textContent = data.key;
      document.getElementById('key-created-modal').classList.remove('hidden');
      toast('API key created', 'success');
      loadKeys();
    } catch (e) {
      toast('Failed to create key: ' + e.message, 'error');
    }
  });

  document.getElementById('close-created-btn').addEventListener('click', function () {
    document.getElementById('key-created-modal').classList.add('hidden');
  });

  window.copyCreatedKey = function () {
    var text = document.getElementById('created-key-value').textContent;
    navigator.clipboard.writeText(text).then(function () {
      toast('Copied to clipboard', 'success');
    });
  };

  // Delete key
  window.deleteKey = async function (id, name) {
    if (!confirm('Revoke key "' + name + '"? This cannot be undone.')) return;
    try {
      await api('/api/admin/keys?id=' + encodeURIComponent(id), 'DELETE');
      toast('Key revoked', 'success');
      loadKeys();
    } catch (e) {
      toast('Failed to revoke key: ' + e.message, 'error');
    }
  };

  // Edit key
  window.editKey = async function (id) {
    try {
      var data = await api('/api/admin/keys', 'GET');
      var key = (data.keys || []).find(function (k) { return k.id === id; });
      if (!key) { toast('Key not found', 'error'); return; }

      document.getElementById('edit-key-id').value = key.id;
      document.getElementById('edit-key-name').value = key.name;

      var hasAll = key.permissions.indexOf('*') !== -1;
      document.getElementById('edit-perm-all').checked = hasAll;
      document.querySelectorAll('.edit-perm-single').forEach(function (cb) {
        cb.disabled = hasAll;
        cb.checked = !hasAll && key.permissions.indexOf(cb.value) !== -1;
      });

      document.getElementById('edit-key-modal').classList.remove('hidden');
    } catch (e) {
      toast('Failed to load key: ' + e.message, 'error');
    }
  };

  document.getElementById('edit-perm-all').addEventListener('change', function () {
    var singles = document.querySelectorAll('.edit-perm-single');
    singles.forEach(function (cb) {
      cb.disabled = document.getElementById('edit-perm-all').checked;
      if (document.getElementById('edit-perm-all').checked) cb.checked = false;
    });
  });

  document.getElementById('cancel-edit-btn').addEventListener('click', function () {
    document.getElementById('edit-key-modal').classList.add('hidden');
  });

  document.getElementById('confirm-edit-btn').addEventListener('click', async function () {
    var id = document.getElementById('edit-key-id').value;
    var name = document.getElementById('edit-key-name').value.trim();
    if (!name) { toast('Name is required', 'error'); return; }

    var perms = [];
    if (document.getElementById('edit-perm-all').checked) {
      perms = ['*'];
    } else {
      document.querySelectorAll('.edit-perm-single:checked').forEach(function (cb) {
        perms.push(cb.value);
      });
    }
    if (perms.length === 0) { toast('Select at least one permission', 'error'); return; }

    try {
      await api('/api/admin/keys?id=' + encodeURIComponent(id), 'PUT', { name: name, permissions: perms });
      document.getElementById('edit-key-modal').classList.add('hidden');
      toast('Key updated', 'success');
      loadKeys();
    } catch (e) {
      toast('Failed to update key: ' + e.message, 'error');
    }
  });

  // ─── CORS ───────────────────────────────

  async function loadCors() {
    try {
      var data = await api('/api/admin/cors', 'GET');
      document.getElementById('cors-toggle').checked = data.enabled;

      var badge = document.getElementById('page-badge');
      badge.textContent = (data.enabled ? 'enabled' : 'disabled');

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
            '<span>' + escHtml(o) + '</span>' +
            '<button class="btn btn-danger btn-sm" onclick="removeOrigin(\'' + escAttr(o) + '\')">Remove</button>' +
          '</div>';
        }).join('');
      }
    } catch (e) {
      toast('Failed to load CORS: ' + e.message, 'error');
    }
  }

  document.getElementById('cors-toggle').addEventListener('change', async function () {
    try {
      await api('/api/admin/cors', 'PUT', { enabled: this.checked });
      toast('CORS ' + (this.checked ? 'enabled' : 'disabled'), 'success');
      loadCors();
    } catch (e) {
      toast('Failed to update CORS: ' + e.message, 'error');
    }
  });

  document.getElementById('add-origin-btn').addEventListener('click', addOrigin);
  document.getElementById('cors-origin-input').addEventListener('keydown', function (e) {
    if (e.key === 'Enter') addOrigin();
  });

  async function addOrigin() {
    var input = document.getElementById('cors-origin-input');
    var origin = input.value.trim();
    if (!origin) return;

    try {
      await api('/api/admin/cors', 'POST', { origin: origin });
      input.value = '';
      toast('Origin added', 'success');
      loadCors();
    } catch (e) {
      toast('Failed to add origin: ' + e.message, 'error');
    }
  }

  window.removeOrigin = async function (origin) {
    try {
      await api('/api/admin/cors?origin=' + encodeURIComponent(origin), 'DELETE');
      toast('Origin removed', 'success');
      loadCors();
    } catch (e) {
      toast('Failed to remove origin: ' + e.message, 'error');
    }
  };

  // ─── Settings ───────────────────────────

  async function loadSettings() {
    try {
      var data = await api('/api/admin/settings', 'GET');
      document.getElementById('swagger-toggle').checked = data.swagger_enabled;

      var badge = document.getElementById('page-badge');
      badge.textContent = '';

      var info = document.getElementById('settings-info');
      info.innerHTML =
        '<div class="toggle-row">' +
          '<div><div class="toggle-label">Port</div><div class="toggle-desc">HTTP server port</div></div>' +
          '<span class="mono">' + data.port + '</span>' +
        '</div>' +
        '<div class="toggle-row">' +
          '<div><div class="toggle-label">Bind Address</div><div class="toggle-desc">Network interface</div></div>' +
          '<span class="mono">' + escHtml(data.bind_address) + '</span>' +
        '</div>' +
        '<div class="toggle-row">' +
          '<div><div class="toggle-label">Max Connections</div><div class="toggle-desc">Connection backlog</div></div>' +
          '<span class="mono">' + data.max_connections + '</span>' +
        '</div>';
    } catch (e) {
      toast('Failed to load settings: ' + e.message, 'error');
    }
  }

  document.getElementById('swagger-toggle').addEventListener('change', async function () {
    try {
      await api('/api/admin/settings', 'PUT', { swagger_enabled: this.checked });
      toast('Swagger ' + (this.checked ? 'enabled' : 'disabled') + ' (restart required)', 'success');
    } catch (e) {
      toast('Failed to update settings: ' + e.message, 'error');
    }
  });

  // ─── Helpers ────────────────────────────

  function escHtml(str) {
    var div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }

  function escAttr(str) {
    return str.replace(/'/g, "\\'").replace(/"/g, '&quot;');
  }
})();
