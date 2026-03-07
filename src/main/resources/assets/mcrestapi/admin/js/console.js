var ConsolePage = (function () {
  'use strict';

  var history = [];
  var historyIndex = -1;

  function init() {
    var el = document.getElementById('console-content');
    if (!el) return;

    el.innerHTML =
      '<div class="console-container">' +
        '<div class="console-output" id="console-output">' +
          '<div style="color:var(--text-muted)">Type a command below and press Enter. Commands run with server console privileges.</div>' +
        '</div>' +
        '<div class="console-input-row">' +
          '<span style="color:var(--text-muted);font-family:monospace;padding:10px 0 10px 4px;font-size:13px">&gt;</span>' +
          '<input type="text" id="console-input" class="form-input" placeholder="say Hello World" autocomplete="off">' +
          '<button id="console-send-btn" class="btn btn-primary">Run</button>' +
        '</div>' +
      '</div>';

    var input = document.getElementById('console-input');
    var sendBtn = document.getElementById('console-send-btn');

    input.addEventListener('keydown', function (e) {
      if (e.key === 'Enter') {
        executeCommand();
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        navigateHistory(-1);
      } else if (e.key === 'ArrowDown') {
        e.preventDefault();
        navigateHistory(1);
      }
    });

    sendBtn.addEventListener('click', executeCommand);
    input.focus();
  }

  function destroy() {
    // nothing to clean up
  }

  function navigateHistory(direction) {
    var input = document.getElementById('console-input');
    if (!input || history.length === 0) return;

    historyIndex += direction;
    if (historyIndex < 0) historyIndex = 0;
    if (historyIndex >= history.length) {
      historyIndex = history.length;
      input.value = '';
      return;
    }
    input.value = history[historyIndex];
  }

  async function executeCommand() {
    var input = document.getElementById('console-input');
    var output = document.getElementById('console-output');
    if (!input || !output) return;

    var command = input.value.trim();
    if (!command) return;

    // Add to history
    history.push(command);
    historyIndex = history.length;
    input.value = '';

    // Show command in output
    var entry = document.createElement('div');
    entry.className = 'console-entry';
    entry.innerHTML = '<div class="console-cmd">' + Utils.escHtml(command) + '</div>';
    output.appendChild(entry);

    try {
      var data = await Api.request('/api/command', 'POST', { command: command });
      if (data.output) {
        var result = document.createElement('div');
        result.className = 'console-result';
        result.textContent = data.output;
        entry.appendChild(result);
      }
    } catch (e) {
      var error = document.createElement('div');
      error.className = 'console-error';
      error.textContent = e.message;
      entry.appendChild(error);
    }

    output.scrollTop = output.scrollHeight;
    input.focus();
  }

  return { init: init, destroy: destroy };
})();
