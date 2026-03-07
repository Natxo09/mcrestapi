var KeysPage = (function () {
  'use strict';

  function init() {
    loadKeys();
    bindEvents();
  }

  function destroy() {}

  function bindEvents() {
    var createBtn = document.getElementById('create-key-btn');
    if (createBtn && !createBtn._bound) {
      createBtn._bound = true;
      createBtn.addEventListener('click', function () {
        document.getElementById('new-key-name').value = '';
        document.querySelectorAll('#create-key-modal input[type=checkbox]').forEach(function (cb) { cb.checked = false; cb.disabled = false; });
        document.getElementById('create-key-modal').classList.remove('hidden');
      });
    }

    var cancelCreate = document.getElementById('cancel-create-btn');
    if (cancelCreate && !cancelCreate._bound) {
      cancelCreate._bound = true;
      cancelCreate.addEventListener('click', function () {
        document.getElementById('create-key-modal').classList.add('hidden');
      });
    }

    var permAll = document.getElementById('perm-all');
    if (permAll && !permAll._bound) {
      permAll._bound = true;
      permAll.addEventListener('change', function () {
        document.querySelectorAll('#create-key-modal .perm-single').forEach(function (cb) {
          cb.disabled = permAll.checked;
          if (permAll.checked) cb.checked = false;
        });
      });
    }

    var confirmCreate = document.getElementById('confirm-create-btn');
    if (confirmCreate && !confirmCreate._bound) {
      confirmCreate._bound = true;
      confirmCreate.addEventListener('click', createKey);
    }

    var closeCreated = document.getElementById('close-created-btn');
    if (closeCreated && !closeCreated._bound) {
      closeCreated._bound = true;
      closeCreated.addEventListener('click', function () {
        document.getElementById('key-created-modal').classList.add('hidden');
      });
    }

    var editPermAll = document.getElementById('edit-perm-all');
    if (editPermAll && !editPermAll._bound) {
      editPermAll._bound = true;
      editPermAll.addEventListener('change', function () {
        document.querySelectorAll('.edit-perm-single').forEach(function (cb) {
          cb.disabled = editPermAll.checked;
          if (editPermAll.checked) cb.checked = false;
        });
      });
    }

    var cancelEdit = document.getElementById('cancel-edit-btn');
    if (cancelEdit && !cancelEdit._bound) {
      cancelEdit._bound = true;
      cancelEdit.addEventListener('click', function () {
        document.getElementById('edit-key-modal').classList.add('hidden');
      });
    }

    var confirmEdit = document.getElementById('confirm-edit-btn');
    if (confirmEdit && !confirmEdit._bound) {
      confirmEdit._bound = true;
      confirmEdit.addEventListener('click', updateKey);
    }
  }

  async function loadKeys() {
    try {
      var data = await Api.request('/api/admin/keys', 'GET');
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
          return '<span class="' + cls + '">' + Utils.escHtml(p) + '</span>';
        }).join('');

        var date = k.created_at ? Utils.formatDate(k.created_at) : '-';

        return '<tr>' +
          '<td><strong>' + Utils.escHtml(k.name) + '</strong></td>' +
          '<td class="mono text-muted">' + Utils.escHtml(k.id) + '</td>' +
          '<td>' + perms + '</td>' +
          '<td class="text-muted">' + date + '</td>' +
          '<td style="text-align:right">' +
            '<button class="btn btn-ghost btn-sm" onclick="KeysPage.editKey(\'' + Utils.escAttr(k.id) + '\')">' + Utils.icons.edit + ' Edit</button> ' +
            '<button class="btn btn-danger btn-sm" onclick="KeysPage.deleteKey(\'' + Utils.escAttr(k.id) + '\', \'' + Utils.escAttr(k.name) + '\')">' + Utils.icons.trash + ' Revoke</button>' +
          '</td>' +
        '</tr>';
      }).join('');
    } catch (e) {
      Utils.toast('Failed to load keys: ' + e.message, 'error');
    }
  }

  async function createKey() {
    var name = document.getElementById('new-key-name').value.trim();
    if (!name) { Utils.toast('Name is required', 'error'); return; }

    var perms = [];
    if (document.getElementById('perm-all').checked) {
      perms = ['*'];
    } else {
      document.querySelectorAll('#create-key-modal .perm-single:checked').forEach(function (cb) {
        perms.push(cb.value);
      });
    }
    if (perms.length === 0) { Utils.toast('Select at least one permission', 'error'); return; }

    try {
      var data = await Api.request('/api/admin/keys', 'POST', { name: name, permissions: perms });
      document.getElementById('create-key-modal').classList.add('hidden');
      document.getElementById('created-key-value').textContent = data.key;
      document.getElementById('key-created-modal').classList.remove('hidden');
      Utils.toast('API key created', 'success');
      loadKeys();
    } catch (e) {
      Utils.toast('Failed to create key: ' + e.message, 'error');
    }
  }

  async function deleteKey(id, name) {
    if (!confirm('Revoke key "' + name + '"? This cannot be undone.')) return;
    try {
      await Api.request('/api/admin/keys?id=' + encodeURIComponent(id), 'DELETE');
      Utils.toast('Key revoked', 'success');
      loadKeys();
    } catch (e) {
      Utils.toast('Failed to revoke key: ' + e.message, 'error');
    }
  }

  async function editKey(id) {
    try {
      var data = await Api.request('/api/admin/keys', 'GET');
      var key = (data.keys || []).find(function (k) { return k.id === id; });
      if (!key) { Utils.toast('Key not found', 'error'); return; }

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
      Utils.toast('Failed to load key: ' + e.message, 'error');
    }
  }

  async function updateKey() {
    var id = document.getElementById('edit-key-id').value;
    var name = document.getElementById('edit-key-name').value.trim();
    if (!name) { Utils.toast('Name is required', 'error'); return; }

    var perms = [];
    if (document.getElementById('edit-perm-all').checked) {
      perms = ['*'];
    } else {
      document.querySelectorAll('.edit-perm-single:checked').forEach(function (cb) {
        perms.push(cb.value);
      });
    }
    if (perms.length === 0) { Utils.toast('Select at least one permission', 'error'); return; }

    try {
      await Api.request('/api/admin/keys?id=' + encodeURIComponent(id), 'PUT', { name: name, permissions: perms });
      document.getElementById('edit-key-modal').classList.add('hidden');
      Utils.toast('Key updated', 'success');
      loadKeys();
    } catch (e) {
      Utils.toast('Failed to update key: ' + e.message, 'error');
    }
  }

  function copyCreatedKey() {
    var text = document.getElementById('created-key-value').textContent;
    navigator.clipboard.writeText(text).then(function () {
      Utils.toast('Copied to clipboard', 'success');
    });
  }

  return {
    init: init,
    destroy: destroy,
    editKey: editKey,
    deleteKey: deleteKey,
    copyCreatedKey: copyCreatedKey
  };
})();
