(() => {
  const $ = (selector, scope = document) => scope.querySelector(selector);
  const $$ = (selector, scope = document) => [...scope.querySelectorAll(selector)];

  const appShell = $('#appShell');
  const appScroll = $('#appScroll');
  const toast = $('#toast');
  const toastText = $('span', toast);
  let toastTimer;
  let searchMode = 'Web';

  function showToast(message) {
    window.clearTimeout(toastTimer);
    toastText.textContent = message;
    toast.classList.add('show');
    toastTimer = window.setTimeout(() => toast.classList.remove('show'), 2500);
  }

  // Live status clock in the product demo's location.
  function updateClock() {
    const formatted = new Intl.DateTimeFormat('en-US', {
      timeZone: 'Asia/Vientiane',
      hour: 'numeric',
      minute: '2-digit',
      hour12: false
    }).format(new Date());
    $('#statusTime').textContent = formatted.replace(/^24:/, '00:');
  }
  updateClock();
  window.setInterval(updateClock, 30000);

  // Search mode and simulated omnibox behavior.
  $$('.mode-button').forEach(button => {
    button.addEventListener('click', () => {
      $$('.mode-button').forEach(item => item.classList.remove('active'));
      button.classList.add('active');
      searchMode = button.dataset.mode;
      const input = $('#searchInput');
      input.placeholder = searchMode === 'Qur’an' ? 'Search surah, ayah or topic' : 'Search or enter address';
      input.focus();
    });
  });

  $('#searchForm').addEventListener('submit', event => {
    event.preventDefault();
    const query = $('#searchInput').value.trim();
    if (!query) {
      $('#searchInput').focus();
      showToast(`Type something to search ${searchMode}`);
      return;
    }
    const isAddress = /^(https?:\/\/|[\w-]+\.[a-z]{2,})/i.test(query);
    showToast(isAddress && searchMode === 'Web' ? `Opening ${query.replace(/^https?:\/\//, '')}` : `Searching ${searchMode} for “${query}”`);
  });

  $('#voiceButton').addEventListener('click', event => {
    const button = event.currentTarget;
    if (button.classList.contains('listening')) return;
    button.classList.add('listening');
    showToast('Listening… speak now');
    window.setTimeout(() => {
      button.classList.remove('listening');
      showToast('Voice search is ready in the full app');
    }, 2200);
  });

  // Bottom sheets.
  const backdrop = $('#sheetBackdrop');
  let openSheet = null;

  function showSheet(id) {
    if (openSheet) closeSheets();
    const sheet = document.getElementById(id);
    if (!sheet) return;
    openSheet = sheet;
    backdrop.classList.add('open');
    backdrop.setAttribute('aria-hidden', 'false');
    sheet.classList.add('open');
    sheet.setAttribute('aria-hidden', 'false');
    window.setTimeout(() => $('.sheet-close', sheet)?.focus(), 220);
  }

  function closeSheets() {
    if (!openSheet) return;
    openSheet.classList.remove('open');
    openSheet.setAttribute('aria-hidden', 'true');
    backdrop.classList.remove('open');
    backdrop.setAttribute('aria-hidden', 'true');
    openSheet = null;
  }

  $$('[data-open-sheet]').forEach(button => {
    button.addEventListener('click', event => {
      event.stopPropagation();
      showSheet(button.dataset.openSheet);
    });
  });
  $$('.sheet-close').forEach(button => button.addEventListener('click', closeSheets));
  backdrop.addEventListener('click', closeSheets);
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape') closeSheets();
  });

  // Prayer countdown uses local Vientiane demo times.
  const prayers = [
    { name: 'Fajr', minutes: 4 * 60 + 41 },
    { name: 'Dhuhr', minutes: 11 * 60 + 59 },
    { name: 'Asr', minutes: 15 * 60 + 19 },
    { name: 'Maghrib', minutes: 17 * 60 + 58 },
    { name: 'Isha', minutes: 19 * 60 + 8 }
  ];

  function getVientianeTimeParts() {
    const parts = new Intl.DateTimeFormat('en-GB', {
      timeZone: 'Asia/Vientiane', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
    }).formatToParts(new Date());
    const get = type => Number(parts.find(part => part.type === type)?.value || 0);
    return { hour: get('hour') % 24, minute: get('minute'), second: get('second') };
  }

  function updatePrayerCountdown() {
    const now = getVientianeTimeParts();
    const currentSeconds = now.hour * 3600 + now.minute * 60 + now.second;
    let nextIndex = prayers.findIndex(prayer => prayer.minutes * 60 > currentSeconds);
    let difference;
    if (nextIndex === -1) {
      nextIndex = 0;
      difference = (24 * 60 + prayers[0].minutes) * 60 - currentSeconds;
    } else {
      difference = prayers[nextIndex].minutes * 60 - currentSeconds;
    }
    const hours = Math.floor(difference / 3600);
    const minutes = Math.floor((difference % 3600) / 60);
    $('#nextPrayerName').textContent = prayers[nextIndex].name;
    $('#prayerCountdown').textContent = hours > 0 ? `${hours}h ${minutes}m` : `${Math.max(1, minutes)}m`;
    $$('.prayer-row').forEach((row, index) => row.classList.toggle('active', index === nextIndex));
  }
  updatePrayerCountdown();
  window.setInterval(updatePrayerCountdown, 30000);

  $$('.prayer-row').forEach(row => row.addEventListener('click', () => {
    const name = $('span', row).textContent.trim();
    showToast(`${name} reminder options`);
  }));
  $('#nextPrayerButton').addEventListener('click', () => showToast('Prayer notification is enabled'));

  // Content actions.
  $$('.shortcut[data-label]').forEach(button => button.addEventListener('click', () => showToast(`${button.dataset.label} opens here`)));
  $$('.site-tile').forEach(button => button.addEventListener('click', () => showToast(`Opening ${button.dataset.label}`)));
  $$('[data-demo]').forEach(button => button.addEventListener('click', () => {
    const label = button.dataset.demo;
    closeSheets();
    window.setTimeout(() => showToast(`${label} · visual demo`), 100);
  }));

  $('#continueButton').addEventListener('click', () => {
    showToast('Resuming Surah Al-Baqarah · Ayah 255');
    const progress = $('.reading-progress i');
    progress.style.width = '68%';
  });

  const saveButton = $('#saveButton');
  try {
    if (localStorage.getItem('inweb-daily-saved') === 'yes') saveButton.classList.add('saved');
  } catch (_) {}
  saveButton.addEventListener('click', () => {
    const saved = saveButton.classList.toggle('saved');
    try { localStorage.setItem('inweb-daily-saved', saved ? 'yes' : 'no'); } catch (_) {}
    showToast(saved ? 'Hadith saved to bookmarks' : 'Removed from bookmarks');
  });

  $('#shareButton').addEventListener('click', async () => {
    const text = '“The best among you are those who learn the Qur’an and teach it.” — Sahih al-Bukhari, 5027';
    try {
      if (navigator.share) await navigator.share({ title: 'Daily Wisdom', text });
      else if (navigator.clipboard) await navigator.clipboard.writeText(text);
      showToast(navigator.share ? 'Share sheet opened' : 'Hadith copied to clipboard');
    } catch (error) {
      if (error?.name !== 'AbortError') showToast('Share is ready in the full app');
    }
  });

  // Quick Access editing.
  const quickGrid = $('#quickGrid');
  const editQuickButton = $('#editQuickButton');
  editQuickButton.addEventListener('click', () => {
    const editing = quickGrid.classList.toggle('editing');
    $('span', editQuickButton).textContent = editing ? 'Done' : 'Edit';
    showToast(editing ? 'Tap a shortcut to remove it' : 'Quick Access updated');
  });

  quickGrid.addEventListener('click', event => {
    const tile = event.target.closest('.quick-tile');
    if (!tile || tile.classList.contains('add-site')) return;
    if (quickGrid.classList.contains('editing')) {
      const label = tile.dataset.label;
      tile.style.transform = 'scale(.75)';
      tile.style.opacity = '0';
      window.setTimeout(() => tile.remove(), 180);
      showToast(`${label} removed`);
    } else {
      showToast(`Opening ${tile.dataset.label}`);
    }
  });

  $('#addShortcutForm').addEventListener('submit', event => {
    event.preventDefault();
    const nameInput = $('#shortcutName');
    const urlInput = $('#shortcutUrl');
    const name = nameInput.value.trim();
    if (!name) return;
    const tile = document.createElement('button');
    tile.type = 'button';
    tile.className = 'quick-tile';
    tile.dataset.label = name;
    tile.innerHTML = `<i class="remove-dot">×</i><span class="quick-logo add">${name.charAt(0).toUpperCase()}</span><small></small>`;
    $('small', tile).textContent = name;
    const addTile = $('.add-site', quickGrid);
    quickGrid.insertBefore(tile, addTile);
    nameInput.value = '';
    urlInput.value = '';
    closeSheets();
    window.setTimeout(() => showToast(`${name} added to Quick Access`), 120);
  });

  // Protection switches are interactive in this visual prototype.
  $$('.protection-list button').forEach((button, index) => {
    button.addEventListener('click', () => {
      const toggle = $('.toggle', button);
      const label = $('b', button)?.textContent;
      if (toggle) {
        const enabled = toggle.classList.toggle('on');
        showToast(`${label} ${enabled ? 'enabled' : 'paused'}`);
      } else if (index === 4) {
        closeSheets();
        window.setTimeout(() => showToast('Opening a private tab'), 100);
      }
    });
  });

  $$('.extension-item').forEach(button => {
    button.addEventListener('click', () => {
      const toggle = $('.toggle', button);
      const label = $('b', button).textContent;
      const enabled = toggle.classList.toggle('on');
      showToast(`${label} ${enabled ? 'enabled' : 'disabled'}`);
    });
  });

  // Navigation preserves Home as the actual rendered page; other items preview their destination.
  $$('.nav-item').forEach(button => {
    button.addEventListener('click', () => {
      const destination = button.dataset.nav;
      if (destination === 'Menu') {
        showSheet('menuSheet');
        return;
      }
      if (destination === 'Extensions') {
        showSheet('extensionsSheet');
        return;
      }
      if (destination === 'Home') {
        appScroll.scrollTo({ top: 0, behavior: 'smooth' });
        showToast('Home');
        return;
      }
      $$('.nav-item').forEach(item => item.classList.toggle('active', item === button));
      showToast(`${destination} · visual demo`);
      window.setTimeout(() => {
        $$('.nav-item').forEach(item => item.classList.toggle('active', item.dataset.nav === 'Home'));
      }, 1400);
    });
  });
})();
