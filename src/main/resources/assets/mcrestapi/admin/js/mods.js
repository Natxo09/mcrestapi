var ModsPage = (function () {
  'use strict';

  var allMods = [];
  var showAll = false;
  var viewMode = 'list'; // 'list' or 'grid'

  var INTERNAL_IDS = ['minecraft', 'java', 'fabricloader', 'mixinextras'];

  function isInternal(mod) {
    var id = mod.id || '';
    if (INTERNAL_IDS.indexOf(id) !== -1) return true;
    if (id.indexOf('fabric-') === 0 && id !== 'fabric-api') return true;
    return false;
  }

  function init() {
    load();
  }

  function destroy() {
    allMods = [];
  }

  async function load() {
    try {
      var data = await Api.request('/api/mods', 'GET');
      allMods = data.mods || [];
      render();
    } catch (e) {
      var el = document.getElementById('mods-content');
      if (el) el.innerHTML = '<div class="empty-state">Failed to load mods. Make sure your API key has the <strong>mods.read</strong> permission.</div>';
    }
  }

  function getVisibleMods() {
    if (showAll) return allMods;
    return allMods.filter(function (m) { return !isInternal(m); });
  }

  function render() {
    var el = document.getElementById('mods-content');
    if (!el) return;

    var mods = getVisibleMods();
    var internalCount = allMods.length - allMods.filter(function (m) { return !isInternal(m); }).length;

    document.getElementById('page-badge').textContent = allMods.length + ' loaded';

    // Toolbar
    var toolbar =
      '<div class="mods-toolbar">' +
        '<div class="mods-toolbar-left">' +
          '<span class="text-muted">' + mods.length + ' mod' + (mods.length !== 1 ? 's' : '') +
            (showAll ? '' : ' &middot; ' + internalCount + ' internal hidden') +
          '</span>' +
        '</div>' +
        '<div class="mods-toolbar-right">' +
          '<button class="btn btn-ghost btn-sm" id="mods-toggle-internal">' +
            (showAll ? 'Hide internal' : 'Show all (' + allMods.length + ')') +
          '</button>' +
          '<div class="view-toggle">' +
            '<button class="view-btn' + (viewMode === 'list' ? ' active' : '') + '" id="mods-view-list" title="List view">' +
              '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 256 256"><path d="M224,128a8,8,0,0,1-8,8H40a8,8,0,0,1,0-16H216A8,8,0,0,1,224,128ZM40,72H216a8,8,0,0,0,0-16H40a8,8,0,0,0,0,16ZM216,184H40a8,8,0,0,0,0,16H216a8,8,0,0,0,0-16Z"/></svg>' +
            '</button>' +
            '<button class="view-btn' + (viewMode === 'grid' ? ' active' : '') + '" id="mods-view-grid" title="Grid view">' +
              '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 256 256"><path d="M104,40H56A16,16,0,0,0,40,56v48a16,16,0,0,0,16,16h48a16,16,0,0,0,16-16V56A16,16,0,0,0,104,40Zm0,64H56V56h48Zm96-64H152a16,16,0,0,0-16,16v48a16,16,0,0,0,16,16h48a16,16,0,0,0,16-16V56A16,16,0,0,0,200,40Zm0,64H152V56h48Zm-96,32H56a16,16,0,0,0-16,16v48a16,16,0,0,0,16,16h48a16,16,0,0,0,16-16V152A16,16,0,0,0,104,136Zm0,64H56V152h48Zm96-64H152a16,16,0,0,0-16,16v48a16,16,0,0,0,16,16h48a16,16,0,0,0,16-16V152A16,16,0,0,0,200,136Zm0,64H152V152h48Z"/></svg>' +
            '</button>' +
          '</div>' +
        '</div>' +
      '</div>';

    // Mods content
    var modsHtml;
    if (viewMode === 'grid') {
      modsHtml = '<div class="mod-grid">' + mods.map(renderCard).join('') + '</div>';
    } else {
      modsHtml = '<div class="mod-list">' + mods.map(renderRow).join('') + '</div>';
    }

    el.innerHTML = toolbar + modsHtml;

    // Bind events
    document.getElementById('mods-toggle-internal').addEventListener('click', function () {
      showAll = !showAll;
      render();
    });
    document.getElementById('mods-view-list').addEventListener('click', function () {
      viewMode = 'list';
      render();
    });
    document.getElementById('mods-view-grid').addEventListener('click', function () {
      viewMode = 'grid';
      render();
    });
  }

  function envBadge(mod) {
    var envClass = mod.environment === 'SERVER' ? 'env-server'
      : mod.environment === 'CLIENT' ? 'env-client'
      : 'env-universal';
    var envLabel = mod.environment === 'UNIVERSAL' ? 'Universal'
      : mod.environment === 'SERVER' ? 'Server'
      : 'Client';
    return '<span class="mod-env ' + envClass + '">' + envLabel + '</span>';
  }

  function contactLinks(mod) {
    if (!mod.contact) return '';
    var links = [];
    if (mod.contact.homepage) links.push('<a href="' + Utils.escAttr(mod.contact.homepage) + '" target="_blank" rel="noopener">Homepage</a>');
    if (mod.contact.sources) links.push('<a href="' + Utils.escAttr(mod.contact.sources) + '" target="_blank" rel="noopener">Source</a>');
    if (mod.contact.issues) links.push('<a href="' + Utils.escAttr(mod.contact.issues) + '" target="_blank" rel="noopener">Issues</a>');
    if (links.length === 0) return '';
    return '<div class="mod-links">' + links.join(' &middot; ') + '</div>';
  }

  function renderRow(mod) {
    var authors = (mod.authors || []).join(', ');
    var licenses = (mod.license || []).join(', ');

    return '<div class="mod-row">' +
      '<div class="mod-row-main">' +
        '<span class="mod-name">' + Utils.escHtml(mod.name || mod.id) + '</span>' +
        '<span class="mod-version">' + Utils.escHtml(mod.version || '') + '</span>' +
        envBadge(mod) +
      '</div>' +
      '<div class="mod-row-details">' +
        '<span class="mod-id mono">' + Utils.escHtml(mod.id) + '</span>' +
        (authors ? '<span class="mod-row-author">by ' + Utils.escHtml(authors) + '</span>' : '') +
        (licenses ? '<span class="mod-license">' + Utils.escHtml(licenses) + '</span>' : '') +
      '</div>' +
      (mod.description ? '<div class="mod-desc">' + Utils.escHtml(mod.description) + '</div>' : '') +
      contactLinks(mod) +
    '</div>';
  }

  function renderCard(mod) {
    var authors = (mod.authors || []).join(', ');
    var licenses = (mod.license || []).join(', ');

    return '<div class="mod-card">' +
      '<div class="mod-header">' +
        '<div class="mod-title">' +
          '<span class="mod-name">' + Utils.escHtml(mod.name || mod.id) + '</span>' +
          '<span class="mod-version">' + Utils.escHtml(mod.version || '') + '</span>' +
        '</div>' +
        envBadge(mod) +
      '</div>' +
      '<div class="mod-id mono">' + Utils.escHtml(mod.id) + '</div>' +
      (mod.description ? '<div class="mod-desc">' + Utils.escHtml(mod.description) + '</div>' : '') +
      '<div class="mod-meta">' +
        (authors ? '<span>By ' + Utils.escHtml(authors) + '</span>' : '') +
        (licenses ? '<span class="mod-license">' + Utils.escHtml(licenses) + '</span>' : '') +
      '</div>' +
      contactLinks(mod) +
    '</div>';
  }

  return { init: init, destroy: destroy };
})();
