(function () {
  'use strict';

  var currentPage = null;

  var pages = {
    dashboard:  { title: 'Dashboard',  module: DashboardPage },
    players:    { title: 'Players',    module: PlayersPage },
    world:      { title: 'World',      module: WorldPage },
    mods:       { title: 'Mods',       module: ModsPage },
    chat:       { title: 'Chat',       module: ChatPage },
    console:    { title: 'Console',    module: ConsolePage },
    keys:       { title: 'API Keys',   module: KeysPage },
    cors:       { title: 'CORS',       module: CorsPage },
    settings:   { title: 'Settings',   module: SettingsPage }
  };

  // ─── Login ──────────────────────────────

  document.getElementById('login-btn').addEventListener('click', doLogin);
  document.getElementById('master-key-input').addEventListener('keydown', function (e) {
    if (e.key === 'Enter') doLogin();
  });

  async function doLogin() {
    var input = document.getElementById('master-key-input');
    var key = input.value.trim();
    if (!key) return;

    Api.setKey(key);

    try {
      await Api.request('/api/server', 'GET');
      sessionStorage.setItem('mk', key);
      showApp();
    } catch (e) {
      Api.setKey('');
      Utils.toast('Invalid key', 'error');
    }
  }

  // Auto-login from session
  var savedKey = sessionStorage.getItem('mk');
  if (savedKey) {
    Api.setKey(savedKey);
    Api.request('/api/server', 'GET')
      .then(function () { showApp(); })
      .catch(function () {
        sessionStorage.removeItem('mk');
        Api.setKey('');
      });
  }

  function showApp() {
    document.getElementById('login-screen').classList.add('hidden');
    document.getElementById('app').classList.remove('hidden');
    navigateTo('dashboard');
  }

  // ─── Logout ─────────────────────────────

  document.getElementById('logout-btn').addEventListener('click', function () {
    if (currentPage && pages[currentPage]) {
      pages[currentPage].module.destroy();
    }
    currentPage = null;
    sessionStorage.removeItem('mk');
    Api.setKey('');
    document.getElementById('app').classList.add('hidden');
    document.getElementById('login-screen').classList.remove('hidden');
    document.getElementById('master-key-input').value = '';
  });

  // ─── Navigation ─────────────────────────

  document.querySelectorAll('.nav-item').forEach(function (item) {
    item.addEventListener('click', function () {
      navigateTo(item.dataset.page);
    });
  });

  function navigateTo(page) {
    if (!pages[page]) return;

    // Destroy current page
    if (currentPage && pages[currentPage]) {
      pages[currentPage].module.destroy();
    }

    currentPage = page;

    // Update nav
    document.querySelectorAll('.nav-item').forEach(function (n) {
      n.classList.toggle('active', n.dataset.page === page);
    });

    // Update header
    document.getElementById('page-title').textContent = pages[page].title;
    document.getElementById('page-badge').textContent = '';

    // Show page content
    document.querySelectorAll('.page').forEach(function (p) { p.classList.add('hidden'); });
    var pageEl = document.getElementById('page-' + page);
    if (pageEl) {
      pageEl.classList.remove('hidden');
    }

    // Init page module
    pages[page].module.init();
  }
})();
