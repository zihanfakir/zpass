/**
 * OpenVault Cross-Platform Backup Import/Export
 * 100% interoperable with OpenVault Android (.openvault encrypted backups).
 * Also supports Bitwarden JSON and standard CSV.
 */

import { CryptoEngine } from './crypto.js';

export const ImportExport = {
  /**
   * Exports full vault into an encrypted .openvault backup string.
   * Matches OpenVault Android's `VaultExporter.exportEncryptedBackup`.
   */
  async exportEncryptedBackup(items, backupPassword, iterations = CryptoEngine.DEFAULT_ITERATIONS) {
    const backupData = {
      exportedAt: Date.now(),
      items: items.map((item) => ({
        id: item.id || crypto.randomUUID(),
        title: item.title,
        type: item.type || 'LOGIN',
        categoryId: item.categoryId || 'logins',
        username: item.username || '',
        password: item.password || '',
        url: item.url || '',
        notes: item.notes || '',
        totpSecret: item.totpSecret || '',
        customFields: item.customFields || [],
        isFavorite: !!item.isFavorite,
        createdAt: item.createdAt || Date.now(),
        updatedAt: item.updatedAt || Date.now(),
        isDeleted: false
      })),
      categories: [
        { id: 'logins', name: 'Logins', icon: 'key', isDefault: true, sortOrder: 0 },
        { id: 'notes', name: 'Secure Notes', icon: 'note', isDefault: true, sortOrder: 1 },
        { id: 'cards', name: 'Cards', icon: 'credit_card', isDefault: true, sortOrder: 2 },
        { id: 'identities', name: 'Identities', icon: 'badge', isDefault: true, sortOrder: 3 }
      ]
    };

    const plaintextJson = JSON.stringify(backupData);
    const salt = CryptoEngine.generateSalt();
    const backupKey = await CryptoEngine.deriveKey(backupPassword, salt, iterations);

    const encOutput = await CryptoEngine.encrypt(
      new TextEncoder().encode(plaintextJson),
      backupKey
    );

    const container = {
      format: 'openvault-backup',
      version: 1,
      exportedAt: Date.now(),
      kdfAlgorithm: 'PBKDF2-HMAC-SHA256',
      kdfIterations: iterations,
      saltBase64: CryptoEngine.bytesToBase64(salt),
      ivBase64: CryptoEngine.bytesToBase64(encOutput.iv),
      encryptedPayloadBase64: CryptoEngine.bytesToBase64(encOutput.ciphertext)
    };

    return JSON.stringify(container, null, 2);
  },

  /**
   * Imports an encrypted .openvault backup string.
   * Interoperable with backups created on Android or Web.
   */
  async importEncryptedBackup(backupJsonString, backupPassword) {
    let container;
    try {
      container = JSON.parse(backupJsonString);
    } catch {
      throw new Error('Invalid backup file: Not valid JSON');
    }

    if (container.format !== 'openvault-backup') {
      throw new Error(`Unsupported backup format: ${container.format || 'unknown'}`);
    }

    const salt = CryptoEngine.base64ToBytes(container.saltBase64);
    const iv = CryptoEngine.base64ToBytes(container.ivBase64);
    const ciphertext = CryptoEngine.base64ToBytes(container.encryptedPayloadBase64);
    const iterations = container.kdfIterations || CryptoEngine.DEFAULT_ITERATIONS;

    const backupKey = await CryptoEngine.deriveKey(backupPassword, salt, iterations);

    let decryptedBytes;
    try {
      decryptedBytes = await CryptoEngine.decrypt(ciphertext, backupKey, iv);
    } catch {
      throw new Error('Incorrect backup password or corrupted backup file');
    }

    const decryptedJson = new TextDecoder('utf-8').decode(decryptedBytes);
    const backupData = JSON.parse(decryptedJson);

    return backupData.items || [];
  },

  /**
   * Imports items from Bitwarden JSON export.
   */
  importBitwardenJson(jsonString) {
    const data = JSON.parse(jsonString);
    const rawItems = data.items || [];

    return rawItems.map((raw) => {
      const login = raw.login || {};
      const uris = login.uris || [];
      const url = uris.length > 0 ? uris[0].uri || '' : '';

      const customFields = (raw.fields || []).map((f) => ({
        id: crypto.randomUUID(),
        name: f.name || 'Field',
        value: f.value || '',
        type: f.type === 1 ? 'CONCEALED' : 'TEXT'
      }));

      let type = 'LOGIN';
      if (raw.type === 2) type = 'SECURE_NOTE';
      else if (raw.type === 3) type = 'CARD';
      else if (raw.type === 4) type = 'IDENTITY';

      return {
        id: crypto.randomUUID(),
        title: raw.name || 'Untitled',
        type,
        categoryId: type === 'SECURE_NOTE' ? 'notes' : type === 'CARD' ? 'cards' : 'logins',
        username: login.username || '',
        password: login.password || '',
        url: url,
        notes: raw.notes || '',
        totpSecret: login.totp || '',
        customFields,
        isFavorite: !!raw.favorite,
        createdAt: raw.creationDate ? new Date(raw.creationDate).getTime() : Date.now(),
        updatedAt: raw.revisionDate ? new Date(raw.revisionDate).getTime() : Date.now(),
        isDeleted: false
      };
    });
  },

  /**
   * Exports items to plain CSV string.
   */
  exportPlaintextCsv(items) {
    const lines = ['title,type,username,password,url,notes,totpSecret,favorite'];
    for (const item of items) {
      const row = [
        this.escapeCsv(item.title),
        this.escapeCsv(item.type || 'LOGIN'),
        this.escapeCsv(item.username || ''),
        this.escapeCsv(item.password || ''),
        this.escapeCsv(item.url || ''),
        this.escapeCsv((item.notes || '').replace(/\r?\n/g, ' ')),
        this.escapeCsv(item.totpSecret || ''),
        item.isFavorite ? '1' : '0'
      ].join(',');
      lines.push(row);
    }
    return lines.join('\n');
  },

  /**
   * Imports items from CSV string.
   */
  importPlaintextCsv(csvString) {
    const lines = csvString.split(/\r?\n/).filter((l) => l.trim().length > 0);
    if (lines.length < 2) return [];

    const items = [];
    // Skip header line
    for (let i = 1; i < lines.length; i++) {
      const cols = this.parseCsvLine(lines[i]);
      if (cols.length >= 4) {
        items.push({
          id: crypto.randomUUID(),
          title: cols[0] || 'Untitled',
          type: cols[1] || 'LOGIN',
          categoryId: (cols[1] || '').toLowerCase().includes('note') ? 'notes' : 'logins',
          username: cols[2] || '',
          password: cols[3] || '',
          url: cols[4] || '',
          notes: cols[5] || '',
          totpSecret: cols[6] || '',
          customFields: [],
          isFavorite: cols[7] === '1' || cols[7] === 'true',
          createdAt: Date.now(),
          updatedAt: Date.now(),
          isDeleted: false
        });
      }
    }
    return items;
  },

  escapeCsv(val) {
    const str = String(val || '');
    if (str.includes(',') || str.includes('"') || str.includes('\n')) {
      return `"${str.replace(/"/g, '""')}"`;
    }
    return str;
  },

  parseCsvLine(text) {
    const p = [];
    let cur = '';
    let inQuotes = false;
    for (let i = 0; i < text.length; i++) {
      const c = text[i];
      if (c === '"') {
        if (inQuotes && text[i + 1] === '"') {
          cur += '"';
          i++;
        } else {
          inQuotes = !inQuotes;
        }
      } else if (c === ',' && !inQuotes) {
        p.push(cur);
        cur = '';
      } else {
        cur += c;
      }
    }
    p.push(cur);
    return p;
  },

  /**
   * Helper to trigger a browser file download.
   */
  downloadFile(content, fileName, mimeType = 'application/json') {
    const blob = new Blob([content], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    setTimeout(() => {
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }, 100);
  }
};
