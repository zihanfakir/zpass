/**
 * OpenVault Zero-Knowledge Local Storage
 * Stores salt, encrypted vault key, and encrypted item payloads.
 * No plaintext passwords ever hit disk or localStorage.
 */

import { CryptoEngine } from './crypto.js';

export const VaultStorage = {
  METADATA_KEY: 'openvault_metadata_v1',
  ITEMS_KEY: 'openvault_items_v1',

  /**
   * Checks whether a vault exists in localStorage.
   */
  hasVault() {
    return localStorage.getItem(this.METADATA_KEY) !== null;
  },

  /**
   * Reads raw vault metadata.
   */
  getMetadata() {
    const raw = localStorage.getItem(this.METADATA_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  },

  /**
   * Initializes a new vault with a master password.
   */
  async initializeVault(masterPassword) {
    const salt = CryptoEngine.generateSalt();
    const iterations = CryptoEngine.DEFAULT_ITERATIONS;

    // Derive Master Encryption Key
    const masterKey = await CryptoEngine.deriveKey(masterPassword, salt, iterations);

    // Generate fresh 256-bit Symmetric Vault Key
    const vaultKeyBytes = CryptoEngine.generateVaultKey();

    // Encrypt the Symmetric Vault Key with the Master Key
    const encVaultKey = await CryptoEngine.encrypt(vaultKeyBytes, masterKey);

    const metadata = {
      version: 1,
      createdAt: Date.now(),
      kdfAlgorithm: 'PBKDF2-HMAC-SHA256',
      kdfIterations: iterations,
      saltBase64: CryptoEngine.bytesToBase64(salt),
      vaultKeyEncryptedBase64: CryptoEngine.bytesToBase64(encVaultKey.ciphertext),
      vaultKeyIvBase64: CryptoEngine.bytesToBase64(encVaultKey.iv),
      settings: {
        autoLockMinutes: 5,
        clipboardClearSeconds: 30,
        lockOnTabHide: false,
        theme: 'dark'
      }
    };

    localStorage.setItem(this.METADATA_KEY, JSON.stringify(metadata));
    localStorage.setItem(this.ITEMS_KEY, JSON.stringify([]));

    // Return the active CryptoKey for immediate session use
    const activeVaultKey = await CryptoEngine.importRawAesKey(vaultKeyBytes);
    CryptoEngine.zeroize(vaultKeyBytes);
    return { metadata, activeVaultKey };
  },

  /**
   * Unlocks the vault by decrypting the Symmetric Vault Key.
   * Throws an error if the master password is incorrect.
   */
  async unlockVault(masterPassword) {
    const metadata = this.getMetadata();
    if (!metadata) {
      throw new Error('No vault found. Please create a vault first.');
    }

    const salt = CryptoEngine.base64ToBytes(metadata.saltBase64);
    const iterations = metadata.kdfIterations || CryptoEngine.DEFAULT_ITERATIONS;

    // Derive Master Encryption Key
    const masterKey = await CryptoEngine.deriveKey(masterPassword, salt, iterations);

    const encVaultKey = CryptoEngine.base64ToBytes(metadata.vaultKeyEncryptedBase64);
    const iv = CryptoEngine.base64ToBytes(metadata.vaultKeyIvBase64);

    try {
      const rawVaultKeyBytes = await CryptoEngine.decrypt(encVaultKey, masterKey, iv);
      const activeVaultKey = await CryptoEngine.importRawAesKey(rawVaultKeyBytes);
      CryptoEngine.zeroize(rawVaultKeyBytes);
      return activeVaultKey;
    } catch {
      throw new Error('Incorrect master password');
    }
  },

  /**
   * Changes the master password by re-encrypting the Symmetric Vault Key.
   */
  async changeMasterPassword(newPassword, activeVaultKey) {
    const metadata = this.getMetadata();
    if (!metadata) throw new Error('No vault found');

    const newSalt = CryptoEngine.generateSalt();
    const iterations = CryptoEngine.DEFAULT_ITERATIONS;

    // Export raw vault key bytes to re-encrypt
    const rawVaultKeyBuffer = await window.crypto.subtle.exportKey('raw', activeVaultKey);
    const rawVaultKeyBytes = new Uint8Array(rawVaultKeyBuffer);

    // Derive new Master Encryption Key
    const newMasterKey = await CryptoEngine.deriveKey(newPassword, newSalt, iterations);

    // Re-encrypt the existing vault key with the new master key
    const encVaultKey = await CryptoEngine.encrypt(rawVaultKeyBytes, newMasterKey);
    CryptoEngine.zeroize(rawVaultKeyBytes);

    metadata.saltBase64 = CryptoEngine.bytesToBase64(newSalt);
    metadata.kdfIterations = iterations;
    metadata.vaultKeyEncryptedBase64 = CryptoEngine.bytesToBase64(encVaultKey.ciphertext);
    metadata.vaultKeyIvBase64 = CryptoEngine.bytesToBase64(encVaultKey.iv);
    metadata.updatedAt = Date.now();

    localStorage.setItem(this.METADATA_KEY, JSON.stringify(metadata));
  },

  /**
   * Loads and decrypts all vault items.
   */
  async loadItems(activeVaultKey) {
    const raw = localStorage.getItem(this.ITEMS_KEY);
    if (!raw) return [];

    let encryptedRecords;
    try {
      encryptedRecords = JSON.parse(raw);
    } catch {
      return [];
    }

    const decryptedItems = [];
    for (const record of encryptedRecords) {
      try {
        const payloadJson = await CryptoEngine.decryptString(
          record.encryptedPayloadBase64,
          record.ivBase64,
          activeVaultKey
        );
        const payload = JSON.parse(payloadJson);

        decryptedItems.push({
          id: record.id,
          title: record.title,
          type: record.type || 'LOGIN',
          categoryId: record.categoryId || 'logins',
          isFavorite: !!record.isFavorite,
          createdAt: record.createdAt || Date.now(),
          updatedAt: record.updatedAt || Date.now(),
          username: payload.username || '',
          password: payload.password || '',
          url: payload.url || '',
          notes: payload.notes || '',
          totpSecret: payload.totpSecret || '',
          customFields: payload.customFields || []
        });
      } catch (err) {
        console.error(`Failed to decrypt item ${record.id}:`, err);
      }
    }

    return decryptedItems;
  },

  /**
   * Encrypts and saves an item (insert or update).
   */
  async saveItem(item, activeVaultKey) {
    const raw = localStorage.getItem(this.ITEMS_KEY);
    const records = raw ? JSON.parse(raw) : [];

    const payload = {
      username: item.username || '',
      password: item.password || '',
      url: item.url || '',
      notes: item.notes || '',
      totpSecret: item.totpSecret || '',
      customFields: item.customFields || []
    };

    const enc = await CryptoEngine.encryptString(JSON.stringify(payload), activeVaultKey);

    const now = Date.now();
    const itemRecord = {
      id: item.id || crypto.randomUUID(),
      title: item.title,
      type: item.type || 'LOGIN',
      categoryId: item.categoryId || 'logins',
      isFavorite: !!item.isFavorite,
      createdAt: item.createdAt || now,
      updatedAt: now,
      encryptedPayloadBase64: enc.ciphertextBase64,
      ivBase64: enc.ivBase64
    };

    const existingIndex = records.findIndex((r) => r.id === itemRecord.id);
    if (existingIndex >= 0) {
      records[existingIndex] = itemRecord;
    } else {
      records.unshift(itemRecord);
    }

    localStorage.setItem(this.ITEMS_KEY, JSON.stringify(records));
    return { ...item, id: itemRecord.id, updatedAt: now };
  },

  /**
   * Deletes an item by ID.
   */
  deleteItem(id) {
    const raw = localStorage.getItem(this.ITEMS_KEY);
    if (!raw) return;
    const records = JSON.parse(raw).filter((r) => r.id !== id);
    localStorage.setItem(this.ITEMS_KEY, JSON.stringify(records));
  },

  /**
   * Saves settings in metadata.
   */
  saveSettings(newSettings) {
    const metadata = this.getMetadata();
    if (!metadata) return;
    metadata.settings = { ...(metadata.settings || {}), ...newSettings };
    localStorage.setItem(this.METADATA_KEY, JSON.stringify(metadata));
  },

  /**
   * Permanently wipes the entire local vault.
   */
  wipeVault() {
    localStorage.removeItem(this.METADATA_KEY);
    localStorage.removeItem(this.ITEMS_KEY);
  }
};
