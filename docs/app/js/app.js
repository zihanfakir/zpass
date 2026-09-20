/**
 * OpenVault Web Application Controller
 * Manages zero-knowledge session state, responsive dual-pane UI,
 * real-time TOTP generation, clipboard clearing, and desktop keyboard shortcuts.
 */

import { CryptoEngine } from './crypto.js';
import { VaultStorage } from './storage.js';
import { TotpEngine } from './totp.js';
import { PasswordGenerator } from './generator.js';
import { ImportExport } from './importexport.js';
import { SecurityAudit } from './audit.js';

class OpenVaultApp {
  constructor() {
    this.activeVaultKey = null;
    this.items = [];
    this.selectedItemId = null;
    this.currentCategory = 'all';
    this.searchQuery = '';
    this.isUnlocked = false;

    // Timers
    this.autoLockTimeoutId = null;
    this.totpIntervalId = null;
    this.clipboardClearTimeoutId = null;

    // Settings cached
    this.autoLockMinutes = 5;
    this.clipboardClearSeconds = 30;

    // DOM Elements
    this.dom = {};
  }

  init() {
    this.cacheDomElements();
    this.bindEvents();
    this.bindKeyboardShortcuts();
    this.checkInitialAuthState();
  }

  cacheDomElements() {
    this.dom = {
      // Auth Screen
      authOverlay: document.getElementById('auth-overlay'),
      authTitle: document.getElementById('auth-title'),
      authDesc: document.getElementById('auth-desc'),
      authForm: document.getElementById('auth-form'),
      setupFields: document.getElementById('setup-fields'),
      unlockFields: document.getElementById('unlock-fields'),
      setupPassword: document.getElementById('setup-password'),
      setupConfirmPassword: document.getElementById('setup-confirm-password'),
      unlockPassword: document.getElementById('unlock-password'),
      btnAuthSubmit: document.getElementById('btn-auth-submit'),
      btnSwitchAuthMode: document.getElementById('btn-switch-auth-mode'),
      btnToggleSetupPwd: document.getElementById('btn-toggle-setup-pwd'),
      btnToggleUnlockPwd: document.getElementById('btn-toggle-unlock-pwd'),
      setupStrengthLabel: document.getElementById('setup-strength-label'),
      setupEntropyLabel: document.getElementById('setup-entropy-label'),
      capsLockWarning: document.getElementById('caps-lock-warning'),

      // App Container
      appContainer: document.getElementById('app-container'),
      btnManualLock: document.getElementById('btn-manual-lock'),
      searchInput: document.getElementById('search-input'),
      itemList: document.getElementById('item-list'),
      itemCountLabel: document.getElementById('item-count-label'),
      btnOpenAddModal: document.getElementById('btn-open-add-modal'),

      // Badges
      badgeAll: document.getElementById('badge-all'),
      badgeFavorites: document.getElementById('badge-favorites'),
      badgeLogins: document.getElementById('badge-logins'),
      badgeNotes: document.getElementById('badge-notes'),
      badgeCards: document.getElementById('badge-cards'),
      badgeIdentities: document.getElementById('badge-identities'),
      badgeAuditScore: document.getElementById('badge-audit-score'),

      // Detail Pane
      emptyDetailState: document.getElementById('empty-detail-state'),
      activeDetailContent: document.getElementById('active-detail-content'),
      detailAvatar: document.getElementById('detail-avatar'),
      detailTitle: document.getElementById('detail-title'),
      detailCategoryBadge: document.getElementById('detail-category-badge'),
      btnDetailFav: document.getElementById('btn-detail-fav'),
      btnDetailEdit: document.getElementById('btn-detail-edit'),
      btnDetailDelete: document.getElementById('btn-detail-delete'),
      detailUsername: document.getElementById('detail-username'),
      detailPassword: document.getElementById('detail-password'),
      btnTogglePwdVisibility: document.getElementById('btn-toggle-pwd-visibility'),
      btnCopyUsername: document.getElementById('btn-copy-username'),
      btnCopyPassword: document.getElementById('btn-copy-password'),
      detailUrl: document.getElementById('detail-url'),
      linkExternalUrl: document.getElementById('link-external-url'),
      btnCopyUrl: document.getElementById('btn-copy-url'),
      detailNotes: document.getElementById('detail-notes'),
      detailCustomFieldsContainer: document.getElementById('detail-custom-fields-container'),

      // TOTP
      detailTotpCard: document.getElementById('detail-totp-card'),
      detailTotpCode: document.getElementById('detail-totp-code'),
      totpProgressRing: document.getElementById('totp-progress-ring'),
      totpSecondsText: document.getElementById('totp-seconds-text'),
      btnCopyTotp: document.getElementById('btn-copy-totp'),

      // Item Modal
      modalItem: document.getElementById('modal-item'),
      modalItemTitle: document.getElementById('modal-item-title'),
      formItemId: document.getElementById('form-item-id'),
      formItemName: document.getElementById('form-item-name'),
      formItemCategory: document.getElementById('form-item-category'),
      formItemFav: document.getElementById('form-item-fav'),
      formItemUsername: document.getElementById('form-item-username'),
      formItemPassword: document.getElementById('form-item-password'),
      btnFormTogglePwd: document.getElementById('btn-form-toggle-pwd'),
      btnQuickGenPwd: document.getElementById('btn-quick-gen-pwd'),
      formItemUrl: document.getElementById('form-item-url'),
      formItemTotp: document.getElementById('form-item-totp'),
      formItemNotes: document.getElementById('form-item-notes'),
      btnAddCustomField: document.getElementById('btn-add-custom-field'),
      formCustomFieldsList: document.getElementById('form-custom-fields-list'),
      btnSaveItem: document.getElementById('btn-save-item'),

      // Generator Modal
      modalGenerator: document.getElementById('modal-generator'),
      navGenerator: document.getElementById('nav-generator'),
      genOutput: document.getElementById('gen-output'),
      btnGenRefresh: document.getElementById('btn-gen-refresh'),
      btnGenCopy: document.getElementById('btn-gen-copy'),
      genStrengthBadge: document.getElementById('gen-strength-badge'),
      genCrackTime: document.getElementById('gen-crack-time'),
      genEntropyVal: document.getElementById('gen-entropy-val'),
      btnGenModePwd: document.getElementById('btn-gen-mode-pwd'),
      btnGenModePassphrase: document.getElementById('btn-gen-mode-passphrase'),
      genControlsPwd: document.getElementById('gen-controls-pwd'),
      genControlsPassphrase: document.getElementById('gen-controls-passphrase'),
      genSliderLength: document.getElementById('gen-slider-length'),
      genLengthVal: document.getElementById('gen-length-val'),
      genOptUpper: document.getElementById('gen-opt-upper'),
      genOptLower: document.getElementById('gen-opt-lower'),
      genOptDigits: document.getElementById('gen-opt-digits'),
      genOptSymbols: document.getElementById('gen-opt-symbols'),
      genOptAvoidAmbig: document.getElementById('gen-opt-avoid-ambig'),
      genSliderWords: document.getElementById('gen-slider-words'),
      genWordsVal: document.getElementById('gen-words-val'),
      genPassphraseSep: document.getElementById('gen-passphrase-sep'),
      genPassphraseCap: document.getElementById('gen-passphrase-cap'),
      genPassphraseNum: document.getElementById('gen-passphrase-num'),

      // Audit Modal
      modalAudit: document.getElementById('modal-audit'),
      navAudit: document.getElementById('nav-audit'),
      auditScoreNumber: document.getElementById('audit-score-number'),
      auditScoreDesc: document.getElementById('audit-score-desc'),
      auditWeakCount: document.getElementById('audit-weak-count'),
      auditWeakList: document.getElementById('audit-weak-list'),
      auditReusedCount: document.getElementById('audit-reused-count'),
      auditReusedList: document.getElementById('audit-reused-list'),
      auditTotpCount: document.getElementById('audit-totp-count'),
      auditTotpList: document.getElementById('audit-totp-list'),

      // Settings Modal
      modalSettings: document.getElementById('modal-settings'),
      navSettings: document.getElementById('nav-settings'),
      prefAutolock: document.getElementById('pref-autolock'),
      prefClipboard: document.getElementById('pref-clipboard'),
      prefTheme: document.getElementById('pref-theme'),
      btnExportBackup: document.getElementById('btn-export-backup'),
      btnImportBackup: document.getElementById('btn-import-backup'),
      fileImportInput: document.getElementById('file-import-input'),
      btnExportCsv: document.getElementById('btn-export-csv'),
      btnChangePassword: document.getElementById('btn-change-password'),
      btnWipeVault: document.getElementById('btn-wipe-vault'),

      // Toast
      toastContainer: document.getElementById('toast-container')
    };
  }

  checkInitialAuthState() {
    const hasVault = VaultStorage.hasVault();
    if (!hasVault) {
      this.showSetupMode();
    } else {
      this.showUnlockMode();
    }
  }

  showSetupMode() {
    this.isSetupMode = true;
    this.dom.authTitle.textContent = 'Create Master Password';
    this.dom.authDesc.textContent = 'Set a master password to encrypt your zero-knowledge vault locally.';
    this.dom.setupFields.style.display = 'flex';
    this.dom.unlockFields.style.display = 'none';
    this.dom.btnAuthSubmit.innerHTML = '<span>Initialize Vault</span> 🚀';
    this.dom.btnSwitchAuthMode.style.display = 'none';
    this.dom.setupPassword.focus();
  }

  showUnlockMode() {
    this.isSetupMode = false;
    this.dom.authTitle.textContent = 'Unlock OpenVault';
    this.dom.authDesc.textContent = 'Enter your master password to decrypt your zero-knowledge vault.';
    this.dom.setupFields.style.display = 'none';
    this.dom.unlockFields.style.display = 'flex';
    this.dom.btnAuthSubmit.innerHTML = '<span>Unlock Vault</span> 🔓';
    this.dom.btnSwitchAuthMode.textContent = 'Need to reset or start over? Create new vault';
    this.dom.btnSwitchAuthMode.style.display = 'inline-block';
    this.dom.unlockPassword.focus();
  }

  bindEvents() {
    // Auth Form Submit
    this.dom.authForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      await this.handleAuthSubmit();
    });

    // Toggle Password Visibility on Auth Screen
    this.dom.btnToggleSetupPwd.addEventListener('click', () => {
      const type = this.dom.setupPassword.type === 'password' ? 'text' : 'password';
      this.dom.setupPassword.type = type;
      this.dom.setupConfirmPassword.type = type;
      this.dom.btnToggleSetupPwd.textContent = type === 'password' ? '👁️' : '🙈';
    });

    this.dom.btnToggleUnlockPwd.addEventListener('click', () => {
      const type = this.dom.unlockPassword.type === 'password' ? 'text' : 'password';
      this.dom.unlockPassword.type = type;
      this.dom.btnToggleUnlockPwd.textContent = type === 'password' ? '👁️' : '🙈';
    });

    // Master Password Strength Meter
    this.dom.setupPassword.addEventListener('input', () => {
      const pwd = this.dom.setupPassword.value;
      const strength = PasswordGenerator.getStrength(pwd);
      this.dom.setupStrengthLabel.textContent = `Strength: ${strength.label}`;
      this.dom.setupStrengthLabel.style.color = strength.color;
      this.dom.setupEntropyLabel.textContent = `${strength.entropy} bits`;
    });

    // Caps Lock Warning
    const checkCapsLock = (e) => {
      if (e.getModifierState && e.getModifierState('CapsLock')) {
        this.dom.capsLockWarning.style.display = 'block';
      } else {
        this.dom.capsLockWarning.style.display = 'none';
      }
    };
    this.dom.unlockPassword.addEventListener('keydown', checkCapsLock);
    this.dom.unlockPassword.addEventListener('keyup', checkCapsLock);

    // Switch auth mode (reset vault)
    this.dom.btnSwitchAuthMode.addEventListener('click', () => {
      if (confirm('Are you sure you want to create a new vault? Any existing local data will be replaced.')) {
        VaultStorage.wipeVault();
        this.showSetupMode();
      }
    });

    // Manual Lock
    this.dom.btnManualLock.addEventListener('click', () => this.lockVault());

    // Search Input
    this.dom.searchInput.addEventListener('input', (e) => {
      this.searchQuery = e.target.value.toLowerCase().trim();
      this.renderItemList();
    });

    // Category Nav Items
    document.querySelectorAll('.sidebar-nav .nav-item[data-category]').forEach((el) => {
      el.addEventListener('click', () => {
        document.querySelectorAll('.sidebar-nav .nav-item[data-category]').forEach((n) => n.classList.remove('active'));
        el.classList.add('active');
        this.currentCategory = el.getAttribute('data-category');
        this.renderItemList();
      });
    });

    // Modal Close buttons
    document.querySelectorAll('[data-close]').forEach((el) => {
      el.addEventListener('click', () => {
        const modalId = el.getAttribute('data-close');
        document.getElementById(modalId).style.display = 'none';
      });
    });

    // Detail Favorite Toggle
    this.dom.btnDetailFav.addEventListener('click', () => this.toggleActiveItemFavorite());

    // Detail Edit
    this.dom.btnDetailEdit.addEventListener('click', () => this.openEditItemModal());

    // Detail Delete
    this.dom.btnDetailDelete.addEventListener('click', () => this.deleteActiveItem());

    // Copy actions with toast and clipboard wipe
    this.dom.btnCopyUsername.addEventListener('click', () => {
      const val = this.dom.detailUsername.textContent;
      if (val && val !== '-') this.copyToClipboard(val, 'Username copied to clipboard');
    });

    this.dom.btnCopyPassword.addEventListener('click', () => {
      const activeItem = this.getActiveItem();
      if (activeItem && activeItem.password) {
        this.copyToClipboard(activeItem.password, 'Password copied (clears in 30s)');
      }
    });

    this.dom.btnCopyUrl.addEventListener('click', () => {
      const val = this.dom.detailUrl.textContent;
      if (val && val !== '-') this.copyToClipboard(val, 'URL copied to clipboard');
    });

    this.dom.btnCopyTotp.addEventListener('click', () => {
      const code = this.dom.detailTotpCode.textContent;
      if (code && code !== '------') {
        this.copyToClipboard(code, '2FA Code copied to clipboard');
      }
    });

    // Password visibility toggle in detail pane
    this.dom.btnTogglePwdVisibility.addEventListener('click', () => {
      const activeItem = this.getActiveItem();
      if (!activeItem) return;
      const isMasked = this.dom.detailPassword.textContent === '••••••••••••';
      this.dom.detailPassword.textContent = isMasked ? activeItem.password : '••••••••••••';
      this.dom.btnTogglePwdVisibility.textContent = isMasked ? '🙈' : '👁️';
    });

    // Open Add Modal
    this.dom.btnOpenAddModal.addEventListener('click', () => this.openAddItemModal());

    // Form Quick Generate Password
    this.dom.btnQuickGenPwd.addEventListener('click', () => {
      const pwd = PasswordGenerator.generatePassword({ length: 20 });
      this.dom.formItemPassword.value = pwd;
      this.dom.formItemPassword.type = 'text';
      this.dom.btnFormTogglePwd.textContent = '🙈';
      this.showToast('Generated strong 20-character password');
    });

    this.dom.btnFormTogglePwd.addEventListener('click', () => {
      const type = this.dom.formItemPassword.type === 'password' ? 'text' : 'password';
      this.dom.formItemPassword.type = type;
      this.dom.btnFormTogglePwd.textContent = type === 'password' ? '👁️' : '🙈';
    });

    // Add Custom Field Row
    this.dom.btnAddCustomField.addEventListener('click', () => this.addCustomFieldRow());

    // Save Item Submit
    this.dom.btnSaveItem.addEventListener('click', () => this.handleSaveItem());

    // Nav Tools: Generator
    this.dom.navGenerator.addEventListener('click', () => {
      this.dom.modalGenerator.style.display = 'flex';
      this.regeneratePasswordModal();
    });

    // Generator Modal Controls
    this.dom.btnGenRefresh.addEventListener('click', () => this.regeneratePasswordModal());
    this.dom.btnGenCopy.addEventListener('click', () => {
      const val = this.dom.genOutput.textContent.trim();
      this.copyToClipboard(val, 'Generated credential copied');
    });

    this.dom.btnGenModePwd.addEventListener('click', () => {
      this.dom.btnGenModePwd.className = 'btn-primary';
      this.dom.btnGenModePassphrase.className = 'btn-secondary';
      this.dom.genControlsPwd.style.display = 'flex';
      this.dom.genControlsPassphrase.style.display = 'none';
      this.regeneratePasswordModal();
    });

    this.dom.btnGenModePassphrase.addEventListener('click', () => {
      this.dom.btnGenModePassphrase.className = 'btn-primary';
      this.dom.btnGenModePwd.className = 'btn-secondary';
      this.dom.genControlsPwd.style.display = 'none';
      this.dom.genControlsPassphrase.style.display = 'flex';
      this.regeneratePasswordModal();
    });

    this.dom.genSliderLength.addEventListener('input', () => {
      this.dom.genLengthVal.textContent = this.dom.genSliderLength.value;
      this.regeneratePasswordModal();
    });

    [
      this.dom.genOptUpper,
      this.dom.genOptLower,
      this.dom.genOptDigits,
      this.dom.genOptSymbols,
      this.dom.genOptAvoidAmbig
    ].forEach((el) => el.addEventListener('change', () => this.regeneratePasswordModal()));

    this.dom.genSliderWords.addEventListener('input', () => {
      this.dom.genWordsVal.textContent = this.dom.genSliderWords.value;
      this.regeneratePasswordModal();
    });

    [
      this.dom.genPassphraseSep,
      this.dom.genPassphraseCap,
      this.dom.genPassphraseNum
    ].forEach((el) => el.addEventListener('change', () => this.regeneratePasswordModal()));

    // Nav Tools: Audit
    this.dom.navAudit.addEventListener('click', () => {
      this.dom.modalAudit.style.display = 'flex';
      this.runSecurityAuditModal();
    });

    // Nav Tools: Settings
    this.dom.navSettings.addEventListener('click', () => {
      this.dom.modalSettings.style.display = 'flex';
      this.loadSettingsToModal();
    });

    // Settings save
    this.dom.prefAutolock.addEventListener('change', (e) => {
      this.autoLockMinutes = parseInt(e.target.value, 10);
      VaultStorage.saveSettings({ autoLockMinutes: this.autoLockMinutes });
      this.resetAutoLockTimer();
    });

    this.dom.prefClipboard.addEventListener('change', (e) => {
      this.clipboardClearSeconds = parseInt(e.target.value, 10);
      VaultStorage.saveSettings({ clipboardClearSeconds: this.clipboardClearSeconds });
    });

    this.dom.prefTheme.addEventListener('change', (e) => {
      const theme = e.target.value;
      document.body.setAttribute('data-theme', theme);
      VaultStorage.saveSettings({ theme });
    });

    // Settings: Export .openvault Backup
    this.dom.btnExportBackup.addEventListener('click', async () => {
      const pwd = prompt('Enter a password to encrypt your backup file:');
      if (!pwd) return;
      try {
        const backupJson = await ImportExport.exportEncryptedBackup(this.items, pwd);
        const fileName = `openvault-backup-${new Date().toISOString().slice(0, 10)}.openvault`;
        ImportExport.downloadFile(backupJson, fileName, 'application/json');
        this.showToast('Encrypted backup exported successfully');
      } catch (err) {
        this.showToast(`Export failed: ${err.message}`, true);
      }
    });

    // Settings: Import Backup File
    this.dom.btnImportBackup.addEventListener('click', () => {
      this.dom.fileImportInput.click();
    });

    this.dom.fileImportInput.addEventListener('change', async (e) => {
      const file = e.target.files[0];
      if (!file) return;
      e.target.value = '';

      const content = await file.text();
      const fileName = file.name.toLowerCase();

      try {
        let imported = [];
        if (fileName.endsWith('.openvault') || content.includes('openvault-backup')) {
          const pwd = prompt('Enter the password for this .openvault backup:');
          if (!pwd) return;
          imported = await ImportExport.importEncryptedBackup(content, pwd);
        } else if (fileName.endsWith('.json')) {
          imported = ImportExport.importBitwardenJson(content);
        } else if (fileName.endsWith('.csv')) {
          imported = ImportExport.importPlaintextCsv(content);
        }

        if (imported.length === 0) {
          this.showToast('No items found to import', true);
          return;
        }

        for (const item of imported) {
          await VaultStorage.saveItem(item, this.activeVaultKey);
        }

        await this.reloadItemsFromStorage();
        this.showToast(`Successfully imported ${imported.length} items`);
      } catch (err) {
        this.showToast(`Import failed: ${err.message}`, true);
      }
    });

    // Settings: Export CSV (Warning)
    this.dom.btnExportCsv.addEventListener('click', () => {
      if (confirm('WARNING: This will export all passwords in UNENCRYPTED plain text. Anyone with this file can view your passwords. Proceed?')) {
        const csv = ImportExport.exportPlaintextCsv(this.items);
        const fileName = `openvault-unencrypted-${new Date().toISOString().slice(0, 10)}.csv`;
        ImportExport.downloadFile(csv, fileName, 'text/csv');
        this.showToast('Plaintext CSV exported', true);
      }
    });

    // Settings: Change Password
    this.dom.btnChangePassword.addEventListener('click', async () => {
      const newPwd = prompt('Enter new Master Password (at least 10 chars recommended):');
      if (!newPwd) return;
      const confirmPwd = prompt('Confirm new Master Password:');
      if (newPwd !== confirmPwd) {
        alert('Passwords do not match!');
        return;
      }
      try {
        await VaultStorage.changeMasterPassword(newPwd, this.activeVaultKey);
        this.showToast('Master Password changed successfully!');
      } catch (err) {
        this.showToast(`Error changing password: ${err.message}`, true);
      }
    });

    // Settings: Wipe Vault
    this.dom.btnWipeVault.addEventListener('click', () => {
      if (confirm('DANGER: This will permanently delete your entire vault from this browser. This cannot be undone! Are you sure?')) {
        VaultStorage.wipeVault();
        this.lockVault();
        this.showSetupMode();
        this.showToast('Vault wiped completely', true);
      }
    });

    // Inactivity reset on user action
    ['mousemove', 'keydown', 'click', 'scroll'].forEach((evt) => {
      window.addEventListener(evt, () => this.resetAutoLockTimer(), { passive: true });
    });
  }

  bindKeyboardShortcuts() {
    window.addEventListener('keydown', (e) => {
      // Ctrl+L or Cmd+L -> Lock Vault
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'l') {
        e.preventDefault();
        this.lockVault();
      }
      // Ctrl+N or Cmd+N -> New Item
      else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'n') {
        e.preventDefault();
        if (this.isUnlocked) {
          this.openAddItemModal();
        }
      }
      // Ctrl+F or Cmd+F -> Focus Search
      else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'f') {
        if (this.isUnlocked) {
          e.preventDefault();
          this.dom.searchInput.focus();
          this.dom.searchInput.select();
        }
      }
      // Escape -> Close Modals
      else if (e.key === 'Escape') {
        document.querySelectorAll('.modal-overlay').forEach((m) => {
          m.style.display = 'none';
        });
      }
    });
  }

  async handleAuthSubmit() {
    this.dom.btnAuthSubmit.disabled = true;
    this.dom.btnAuthSubmit.textContent = 'Deriving Key (600k rounds)...';

    try {
      if (this.isSetupMode) {
        const pwd = this.dom.setupPassword.value;
        const confirmPwd = this.dom.setupConfirmPassword.value;

        if (!pwd || pwd.length < 8) {
          throw new Error('Master password must be at least 8 characters');
        }
        if (pwd !== confirmPwd) {
          throw new Error('Passwords do not match');
        }

        const { activeVaultKey } = await VaultStorage.initializeVault(pwd);
        this.activeVaultKey = activeVaultKey;
        this.dom.setupPassword.value = '';
        this.dom.setupConfirmPassword.value = '';
        this.showToast('Vault created and encrypted with PBKDF2 600,000 rounds');
      } else {
        const pwd = this.dom.unlockPassword.value;
        if (!pwd) throw new Error('Please enter your master password');

        this.activeVaultKey = await VaultStorage.unlockVault(pwd);
        this.dom.unlockPassword.value = '';
        this.showToast('Vault unlocked successfully');
      }

      await this.enterUnlockedState();
    } catch (err) {
      alert(err.message);
      this.dom.btnAuthSubmit.disabled = false;
      this.dom.btnAuthSubmit.innerHTML = this.isSetupMode ?
        '<span>Initialize Vault</span> 🚀' : '<span>Unlock Vault</span> 🔓';
    }
  }

  async enterUnlockedState() {
    this.isUnlocked = true;
    this.dom.authOverlay.style.display = 'none';
    this.dom.appContainer.style.display = 'flex';

    // Load settings
    const metadata = VaultStorage.getMetadata();
    if (metadata && metadata.settings) {
      this.autoLockMinutes = metadata.settings.autoLockMinutes || 5;
      this.clipboardClearSeconds = metadata.settings.clipboardClearSeconds || 30;
      if (metadata.settings.theme) {
        document.body.setAttribute('data-theme', metadata.settings.theme);
      }
    }

    await this.reloadItemsFromStorage();
    this.resetAutoLockTimer();
    this.startTotpInterval();
  }

  lockVault() {
    this.isUnlocked = false;
    this.activeVaultKey = null;
    this.items = [];
    this.selectedItemId = null;

    if (this.autoLockTimeoutId) clearTimeout(this.autoLockTimeoutId);
    if (this.totpIntervalId) clearInterval(this.totpIntervalId);

    // Hide modals
    document.querySelectorAll('.modal-overlay').forEach((m) => (m.style.display = 'none'));

    // Switch screens
    this.dom.appContainer.style.display = 'none';
    this.dom.authOverlay.style.display = 'flex';
    this.dom.btnAuthSubmit.disabled = false;
    this.showUnlockMode();
    this.showToast('Vault locked and memory cleared');
  }

  resetAutoLockTimer() {
    if (this.autoLockTimeoutId) clearTimeout(this.autoLockTimeoutId);
    if (!this.isUnlocked || this.autoLockMinutes <= 0) return;

    this.autoLockTimeoutId = setTimeout(() => {
      this.lockVault();
    }, this.autoLockMinutes * 60 * 1000);
  }

  startTotpInterval() {
    if (this.totpIntervalId) clearInterval(this.totpIntervalId);

    const updateTicker = async () => {
      if (!this.isUnlocked || !this.selectedItemId) return;
      const activeItem = this.getActiveItem();
      if (!activeItem || !activeItem.totpSecret) return;

      try {
        const code = await TotpEngine.generateCode(activeItem.totpSecret);
        const { remainingSeconds, progressRatio } = TotpEngine.getRemainingTime();

        this.dom.detailTotpCode.textContent = code;
        this.dom.totpSecondsText.textContent = remainingSeconds;

        // SVG circle circumference = 2 * PI * 18 ≈ 113.1
        const circumference = 113.1;
        const offset = circumference * (1 - progressRatio);
        this.dom.totpProgressRing.style.strokeDashoffset = offset;

        if (remainingSeconds <= 5) {
          this.dom.totpProgressRing.style.stroke = '#EF4444';
        } else {
          this.dom.totpProgressRing.style.stroke = 'var(--accent-emerald)';
        }
      } catch (err) {
        this.dom.detailTotpCode.textContent = 'INVALID';
      }
    };

    updateTicker();
    this.totpIntervalId = setInterval(updateTicker, 1000);
  }

  async reloadItemsFromStorage() {
    this.items = await VaultStorage.loadItems(this.activeVaultKey);
    this.updateCategoryBadges();
    this.renderItemList();

    if (this.selectedItemId) {
      const exists = this.items.find((i) => i.id === this.selectedItemId);
      if (exists) {
        this.renderDetailPane(exists);
      } else {
        this.clearDetailPane();
      }
    } else if (this.items.length > 0) {
      this.selectItem(this.items[0].id);
    } else {
      this.clearDetailPane();
    }

    // Refresh audit score badge
    const audit = SecurityAudit.analyzeVault(this.items);
    this.dom.badgeAuditScore.textContent = `${audit.healthScore}%`;
  }

  updateCategoryBadges() {
    const counts = {
      all: this.items.length,
      favorites: this.items.filter((i) => i.isFavorite).length,
      logins: this.items.filter((i) => (i.categoryId || 'logins') === 'logins').length,
      notes: this.items.filter((i) => i.categoryId === 'notes').length,
      cards: this.items.filter((i) => i.categoryId === 'cards').length,
      identities: this.items.filter((i) => i.categoryId === 'identities').length
    };

    this.dom.badgeAll.textContent = counts.all;
    this.dom.badgeFavorites.textContent = counts.favorites;
    this.dom.badgeLogins.textContent = counts.logins;
    this.dom.badgeNotes.textContent = counts.notes;
    this.dom.badgeCards.textContent = counts.cards;
    this.dom.badgeIdentities.textContent = counts.identities;
  }

  getFilteredItems() {
    return this.items.filter((item) => {
      // Category filter
      if (this.currentCategory === 'favorites' && !item.isFavorite) return false;
      if (this.currentCategory !== 'all' && this.currentCategory !== 'favorites') {
        if ((item.categoryId || 'logins') !== this.currentCategory) return false;
      }

      // Search filter
      if (this.searchQuery) {
        const t = (item.title || '').toLowerCase();
        const u = (item.username || '').toLowerCase();
        const url = (item.url || '').toLowerCase();
        return t.includes(this.searchQuery) || u.includes(this.searchQuery) || url.includes(this.searchQuery);
      }

      return true;
    });
  }

  renderItemList() {
    const filtered = this.getFilteredItems();
    this.dom.itemCountLabel.textContent = `${filtered.length} item${filtered.length === 1 ? '' : 's'}`;

    this.dom.itemList.innerHTML = '';

    if (filtered.length === 0) {
      this.dom.itemList.innerHTML = `
        <div style="text-align: center; color: var(--text-sub); padding: 30px 10px; font-size: 0.85rem;">
          No items found
        </div>
      `;
      return;
    }

    filtered.forEach((item) => {
      const card = document.createElement('div');
      card.className = `item-card ${item.id === this.selectedItemId ? 'active' : ''}`;
      card.addEventListener('click', () => this.selectItem(item.id));

      const icon = item.type === 'SECURE_NOTE' ? '📝' :
                   item.type === 'CARD' ? '💳' :
                   item.type === 'IDENTITY' ? '🪪' : '🔑';

      const subtitle = item.username || item.url || (item.notes ? item.notes.slice(0, 30) : 'Login');

      card.innerHTML = `
        <div class="item-card-left">
          <div class="item-icon-box">${icon}</div>
          <div class="item-text-info">
            <span class="item-title">${this.escapeHtml(item.title)}</span>
            <span class="item-subtitle">${this.escapeHtml(subtitle)}</span>
          </div>
        </div>
        <div class="item-badges">
          ${item.totpSecret ? '<span class="totp-pill">2FA</span>' : ''}
          ${item.isFavorite ? '<span class="fav-star">⭐</span>' : ''}
        </div>
      `;

      this.dom.itemList.appendChild(card);
    });
  }

  selectItem(id) {
    this.selectedItemId = id;
    const activeItem = this.getActiveItem();
    if (activeItem) {
      this.renderDetailPane(activeItem);
      this.renderItemList();
    }
  }

  getActiveItem() {
    return this.items.find((i) => i.id === this.selectedItemId) || null;
  }

  renderDetailPane(item) {
    this.dom.emptyDetailState.style.display = 'none';
    this.dom.activeDetailContent.style.display = 'flex';

    this.dom.detailTitle.textContent = item.title;
    this.dom.detailCategoryBadge.textContent = item.categoryId.toUpperCase();

    const icon = item.type === 'SECURE_NOTE' ? '📝' :
                 item.type === 'CARD' ? '💳' :
                 item.type === 'IDENTITY' ? '🪪' : '🔑';
    this.dom.detailAvatar.textContent = icon;

    this.dom.btnDetailFav.textContent = item.isFavorite ? '⭐' : '☆';
    this.dom.btnDetailFav.title = item.isFavorite ? 'Remove from favorites' : 'Add to favorites';

    // Username
    this.dom.detailUsername.textContent = item.username || '-';

    // Password
    this.dom.detailPassword.textContent = '••••••••••••';
    this.dom.btnTogglePwdVisibility.textContent = '👁️';

    // URL
    this.dom.detailUrl.textContent = item.url || '-';
    if (item.url && (item.url.startsWith('http://') || item.url.startsWith('https://'))) {
      this.dom.linkExternalUrl.href = item.url;
      this.dom.linkExternalUrl.style.display = 'flex';
    } else {
      this.dom.linkExternalUrl.style.display = 'none';
    }

    // Notes
    this.dom.detailNotes.textContent = item.notes || 'No notes added.';

    // 2FA TOTP Card
    if (item.totpSecret && item.totpSecret.trim().length > 0) {
      this.dom.detailTotpCard.style.display = 'flex';
    } else {
      this.dom.detailTotpCard.style.display = 'none';
    }

    // Custom Fields
    this.dom.detailCustomFieldsContainer.innerHTML = '';
    if (item.customFields && item.customFields.length > 0) {
      item.customFields.forEach((cf) => {
        const fieldEl = document.createElement('div');
        fieldEl.className = 'field-card';
        const isConcealed = cf.type === 'CONCEALED';
        fieldEl.innerHTML = `
          <span class="field-label">${this.escapeHtml(cf.name)}</span>
          <div class="field-value-row">
            <span class="field-value custom-field-val" data-raw="${this.escapeHtml(cf.value)}" data-concealed="${isConcealed ? 'true' : 'false'}">
              ${isConcealed ? '••••••••••••' : this.escapeHtml(cf.value)}
            </span>
            <div class="field-btn-group">
              ${isConcealed ? '<button class="btn-icon btn-cf-toggle" title="Toggle visibility">👁️</button>' : ''}
              <button class="btn-icon btn-cf-copy" title="Copy value">📋</button>
            </div>
          </div>
        `;

        if (isConcealed) {
          fieldEl.querySelector('.btn-cf-toggle').addEventListener('click', (e) => {
            const valEl = fieldEl.querySelector('.custom-field-val');
            const hidden = valEl.getAttribute('data-concealed') === 'true';
            valEl.textContent = hidden ? valEl.getAttribute('data-raw') : '••••••••••••';
            valEl.setAttribute('data-concealed', hidden ? 'false' : 'true');
            e.currentTarget.textContent = hidden ? '🙈' : '👁️';
          });
        }

        fieldEl.querySelector('.btn-cf-copy').addEventListener('click', () => {
          this.copyToClipboard(cf.value, `Copied "${cf.name}"`);
        });

        this.dom.detailCustomFieldsContainer.appendChild(fieldEl);
      });
    }
  }

  clearDetailPane() {
    this.selectedItemId = null;
    this.dom.emptyDetailState.style.display = 'flex';
    this.dom.activeDetailContent.style.display = 'none';
  }

  async toggleActiveItemFavorite() {
    const item = this.getActiveItem();
    if (!item) return;

    item.isFavorite = !item.isFavorite;
    await VaultStorage.saveItem(item, this.activeVaultKey);
    await this.reloadItemsFromStorage();
  }

  async deleteActiveItem() {
    const item = this.getActiveItem();
    if (!item) return;

    if (confirm(`Are you sure you want to delete "${item.title}"?`)) {
      VaultStorage.deleteItem(item.id);
      this.selectedItemId = null;
      await this.reloadItemsFromStorage();
      this.showToast('Item deleted');
    }
  }

  openAddItemModal() {
    this.dom.modalItemTitle.textContent = 'Add New Item';
    this.dom.formItemId.value = '';
    this.dom.formItemName.value = '';
    this.dom.formItemCategory.value = this.currentCategory !== 'all' && this.currentCategory !== 'favorites' ? this.currentCategory : 'logins';
    this.dom.formItemFav.checked = false;
    this.dom.formItemUsername.value = '';
    this.dom.formItemPassword.value = '';
    this.dom.formItemUrl.value = '';
    this.dom.formItemTotp.value = '';
    this.dom.formItemNotes.value = '';
    this.dom.formCustomFieldsList.innerHTML = '';
    this.dom.modalItem.style.display = 'flex';
    this.dom.formItemName.focus();
  }

  openEditItemModal() {
    const item = this.getActiveItem();
    if (!item) return;

    this.dom.modalItemTitle.textContent = 'Edit Item';
    this.dom.formItemId.value = item.id;
    this.dom.formItemName.value = item.title;
    this.dom.formItemCategory.value = item.categoryId || 'logins';
    this.dom.formItemFav.checked = !!item.isFavorite;
    this.dom.formItemUsername.value = item.username || '';
    this.dom.formItemPassword.value = item.password || '';
    this.dom.formItemUrl.value = item.url || '';
    this.dom.formItemTotp.value = item.totpSecret || '';
    this.dom.formItemNotes.value = item.notes || '';

    this.dom.formCustomFieldsList.innerHTML = '';
    if (item.customFields) {
      item.customFields.forEach((cf) => this.addCustomFieldRow(cf.name, cf.value, cf.type));
    }

    this.dom.modalItem.style.display = 'flex';
    this.dom.formItemName.focus();
  }

  addCustomFieldRow(name = '', value = '', type = 'TEXT') {
    const row = document.createElement('div');
    row.className = 'cf-form-row';
    row.style.cssText = 'display: grid; grid-template-columns: 1fr 1fr 90px 30px; gap: 8px; align-items: center;';

    row.innerHTML = `
      <input type="text" class="input-field cf-input-name" placeholder="Field name" value="${this.escapeHtml(name)}" />
      <input type="${type === 'CONCEALED' ? 'password' : 'text'}" class="input-field cf-input-val" placeholder="Value" value="${this.escapeHtml(value)}" />
      <select class="input-field cf-select-type" style="padding: 8px 4px; font-size: 0.8rem;">
        <option value="TEXT" ${type === 'TEXT' ? 'selected' : ''}>Text</option>
        <option value="CONCEALED" ${type === 'CONCEALED' ? 'selected' : ''}>Secret</option>
      </select>
      <button type="button" class="btn-icon cf-btn-del" style="color: var(--danger);" title="Remove field">✕</button>
    `;

    const typeSelect = row.querySelector('.cf-select-type');
    const valInput = row.querySelector('.cf-input-val');
    typeSelect.addEventListener('change', () => {
      valInput.type = typeSelect.value === 'CONCEALED' ? 'password' : 'text';
    });

    row.querySelector('.cf-btn-del').addEventListener('click', () => {
      row.remove();
    });

    this.dom.formCustomFieldsList.appendChild(row);
  }

  async handleSaveItem() {
    const title = this.dom.formItemName.value.trim();
    if (!title) {
      alert('Please enter an item title');
      return;
    }

    // Collect custom fields
    const customFields = [];
    document.querySelectorAll('.cf-form-row').forEach((row) => {
      const fName = row.querySelector('.cf-input-name').value.trim();
      const fVal = row.querySelector('.cf-input-val').value;
      const fType = row.querySelector('.cf-select-type').value;
      if (fName) {
        customFields.push({ id: crypto.randomUUID(), name: fName, value: fVal, type: fType });
      }
    });

    const categoryId = this.dom.formItemCategory.value;
    const type = categoryId === 'notes' ? 'SECURE_NOTE' :
                 categoryId === 'cards' ? 'CARD' :
                 categoryId === 'identities' ? 'IDENTITY' : 'LOGIN';

    const item = {
      id: this.dom.formItemId.value || crypto.randomUUID(),
      title,
      type,
      categoryId,
      isFavorite: this.dom.formItemFav.checked,
      username: this.dom.formItemUsername.value.trim(),
      password: this.dom.formItemPassword.value,
      url: this.dom.formItemUrl.value.trim(),
      totpSecret: this.dom.formItemTotp.value.trim(),
      notes: this.dom.formItemNotes.value,
      customFields
    };

    await VaultStorage.saveItem(item, this.activeVaultKey);
    this.dom.modalItem.style.display = 'none';
    this.selectedItemId = item.id;
    await this.reloadItemsFromStorage();
    this.showToast(`Saved "${title}"`);
  }

  regeneratePasswordModal() {
    const isPassphraseMode = this.dom.genControlsPassphrase.style.display !== 'none';
    let output = '';

    if (isPassphraseMode) {
      const wordCount = parseInt(this.dom.genSliderWords.value, 10);
      const separator = this.dom.genPassphraseSep.value;
      const capitalize = this.dom.genPassphraseCap.checked;
      const includeNumber = this.dom.genPassphraseNum.checked;

      output = PasswordGenerator.generatePassphrase({ wordCount, separator, capitalize, includeNumber });
    } else {
      const length = parseInt(this.dom.genSliderLength.value, 10);
      const useUppercase = this.dom.genOptUpper.checked;
      const useLowercase = this.dom.genOptLower.checked;
      const useDigits = this.dom.genOptDigits.checked;
      const useSymbols = this.dom.genOptSymbols.checked;
      const avoidAmbiguous = this.dom.genOptAvoidAmbig.checked;

      output = PasswordGenerator.generatePassword({
        length,
        useUppercase,
        useLowercase,
        useDigits,
        useSymbols,
        avoidAmbiguous
      });
    }

    this.dom.genOutput.textContent = output;

    const strength = PasswordGenerator.getStrength(output);
    this.dom.genStrengthBadge.textContent = strength.label;
    this.dom.genStrengthBadge.style.color = strength.color;
    this.dom.genCrackTime.textContent = `Crack Time: ${strength.crackTime}`;
    this.dom.genEntropyVal.textContent = `${strength.entropy} bits`;
  }

  runSecurityAuditModal() {
    const audit = SecurityAudit.analyzeVault(this.items);

    this.dom.auditScoreNumber.textContent = `${audit.healthScore}%`;
    this.dom.auditScoreNumber.style.color =
      audit.healthScore >= 80 ? 'var(--accent-emerald)' :
      audit.healthScore >= 50 ? 'var(--warning)' : 'var(--danger)';

    this.dom.auditScoreDesc.textContent =
      audit.healthScore >= 80 ? 'Strong vault health! Passwords adhere to security best practices.' :
      audit.healthScore >= 50 ? 'Moderate security risks detected. Review weak or reused passwords below.' :
      'High security vulnerability! Multiple weak or duplicated credentials found.';

    // Weak List
    this.dom.auditWeakCount.textContent = audit.weakItems.length;
    this.dom.auditWeakList.innerHTML = audit.weakItems.map((w) => `
      <div style="background: var(--surface-card); padding: 6px 10px; border-radius: var(--radius-sm); font-size: 0.82rem; display: flex; justify-content: space-between;">
        <span>${this.escapeHtml(w.item.title)} (${this.escapeHtml(w.item.username || 'No user')})</span>
        <span style="color: ${w.strength.color}; font-weight: 600;">${w.strength.label}</span>
      </div>
    `).join('') || '<span style="font-size: 0.8rem; color: var(--text-sub);">None detected!</span>';

    // Reused List
    this.dom.auditReusedCount.textContent = audit.reusedGroups.length;
    this.dom.auditReusedList.innerHTML = audit.reusedGroups.map((g) => `
      <div style="background: var(--surface-card); padding: 6px 10px; border-radius: var(--radius-sm); font-size: 0.82rem;">
        <strong>Shared by ${g.count} accounts:</strong> ${g.items.map((i) => this.escapeHtml(i.title)).join(', ')}
      </div>
    `).join('') || '<span style="font-size: 0.8rem; color: var(--text-sub);">None detected!</span>';

    // Missing 2FA List
    this.dom.auditTotpCount.textContent = audit.missingTotpItems.length;
    this.dom.auditTotpList.innerHTML = audit.missingTotpItems.slice(0, 10).map((i) => `
      <div style="background: var(--surface-card); padding: 6px 10px; border-radius: var(--radius-sm); font-size: 0.82rem;">
        ${this.escapeHtml(i.title)} (${this.escapeHtml(i.username || 'Login')})
      </div>
    `).join('') || '<span style="font-size: 0.8rem; color: var(--text-sub);">All logins have 2FA configured!</span>';
  }

  loadSettingsToModal() {
    this.dom.prefAutolock.value = String(this.autoLockMinutes);
    this.dom.prefClipboard.value = String(this.clipboardClearSeconds);
    const theme = document.body.getAttribute('data-theme') || 'dark';
    this.dom.prefTheme.value = theme;
  }

  copyToClipboard(text, successMessage) {
    if (!text) return;
    navigator.clipboard.writeText(text).then(() => {
      this.showToast(successMessage);

      // Auto-clear clipboard after timeout
      if (this.clipboardClearTimeoutId) clearTimeout(this.clipboardClearTimeoutId);
      if (this.clipboardClearSeconds > 0) {
        this.clipboardClearTimeoutId = setTimeout(() => {
          navigator.clipboard.writeText('').catch(() => {});
        }, this.clipboardClearSeconds * 1000);
      }
    }).catch(() => {
      this.showToast('Failed to copy to clipboard', true);
    });
  }

  showToast(message, isError = false) {
    const toast = document.createElement('div');
    toast.className = `toast ${isError ? 'toast-danger' : 'toast-success'}`;
    toast.innerHTML = `<span>${isError ? '⚠️' : '✅'}</span> <span>${this.escapeHtml(message)}</span>`;
    this.dom.toastContainer.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, 3000);
  }

  escapeHtml(str) {
    return String(str || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
}

// Initialize Application on DOMContentLoaded
window.addEventListener('DOMContentLoaded', () => {
  const app = new OpenVaultApp();
  app.init();
});
