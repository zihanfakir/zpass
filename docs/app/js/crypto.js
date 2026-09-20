/**
 * OpenVault Web Cryptography Engine
 * Conforms to NIST SP 800-38D (AES-256-GCM) and NIST SP 800-132 (PBKDF2-HMAC-SHA256).
 * 100% interoperable with OpenVault Android.
 */

export const CryptoEngine = {
  DEFAULT_ITERATIONS: 600000,
  SALT_LENGTH_BYTES: 32,
  IV_LENGTH_BYTES: 12,
  KEY_LENGTH_BITS: 256,
  TAG_LENGTH_BITS: 128,

  /**
   * Generates cryptographically secure random bytes.
   */
  getRandomBytes(length) {
    const array = new Uint8Array(length);
    globalThis.crypto.getRandomValues(array);
    return array;
  },

  /**
   * Generates a 32-byte salt for PBKDF2.
   */
  generateSalt() {
    return this.getRandomBytes(this.SALT_LENGTH_BYTES);
  },

  /**
   * Generates a 12-byte initialization vector for AES-256-GCM.
   */
  generateIv() {
    return this.getRandomBytes(this.IV_LENGTH_BYTES);
  },

  /**
   * Generates a random 256-bit Symmetric Vault Key.
   */
  generateVaultKey() {
    return this.getRandomBytes(32);
  },

  /**
   * Derives a 256-bit AES-GCM CryptoKey from a master password using PBKDF2-HMAC-SHA256.
   * @param {string} password - The master password string
   * @param {Uint8Array} salt - 32-byte salt
   * @param {number} iterations - PBKDF2 iteration count (default 600,000)
   * @returns {Promise<CryptoKey>} Derived AES-GCM key
   */
  async deriveKey(password, salt, iterations = this.DEFAULT_ITERATIONS) {
    const enc = new TextEncoder();
    const passwordBuffer = enc.encode(password);

    // Import the password as base key material
    const baseKey = await globalThis.crypto.subtle.importKey(
      'raw',
      passwordBuffer,
      { name: 'PBKDF2' },
      false,
      ['deriveKey', 'deriveBits']
    );

    // Derive 256-bit AES-GCM key
    return await globalThis.crypto.subtle.deriveKey(
      {
        name: 'PBKDF2',
        salt: salt,
        iterations: iterations,
        hash: 'SHA-256'
      },
      baseKey,
      {
        name: 'AES-GCM',
        length: this.KEY_LENGTH_BITS
      },
      true, // extractable for raw byte zeroization if needed
      ['encrypt', 'decrypt']
    );
  },

  /**
   * Derives raw key bytes from a password and salt using PBKDF2.
   */
  async deriveRawBytes(password, salt, iterations = this.DEFAULT_ITERATIONS) {
    const enc = new TextEncoder();
    const passwordBuffer = enc.encode(password);

    const baseKey = await globalThis.crypto.subtle.importKey(
      'raw',
      passwordBuffer,
      { name: 'PBKDF2' },
      false,
      ['deriveBits']
    );

    const bits = await globalThis.crypto.subtle.deriveBits(
      {
        name: 'PBKDF2',
        salt: salt,
        iterations: iterations,
        hash: 'SHA-256'
      },
      baseKey,
      this.KEY_LENGTH_BITS
    );

    return new Uint8Array(bits);
  },

  /**
   * Imports raw 256-bit key bytes into an AES-GCM CryptoKey.
   */
  async importRawAesKey(rawBytes) {
    return await globalThis.crypto.subtle.importKey(
      'raw',
      rawBytes,
      { name: 'AES-GCM', length: 256 },
      false,
      ['encrypt', 'decrypt']
    );
  },

  /**
   * Encrypts plaintext bytes using AES-256-GCM.
   * @param {Uint8Array} plaintext - Plaintext data
   * @param {CryptoKey} key - AES-GCM CryptoKey
   * @param {Uint8Array} [optionalIv] - 12-byte IV (generated if omitted)
   * @param {Uint8Array} [optionalAad] - Associated Authenticated Data
   * @returns {Promise<{ ciphertext: Uint8Array, iv: Uint8Array }>}
   */
  async encrypt(plaintext, key, optionalIv = null, optionalAad = null) {
    const iv = optionalIv || this.generateIv();
    const algorithm = {
      name: 'AES-GCM',
      iv: iv,
      tagLength: this.TAG_LENGTH_BITS
    };

    if (optionalAad) {
      algorithm.additionalData = optionalAad;
    }

    const ciphertextBuffer = await globalThis.crypto.subtle.encrypt(
      algorithm,
      key,
      plaintext
    );

    return {
      ciphertext: new Uint8Array(ciphertextBuffer),
      iv: iv
    };
  },

  /**
   * Decrypts ciphertext bytes using AES-256-GCM.
   * @param {Uint8Array} ciphertext - Encrypted bytes (including 16-byte auth tag)
   * @param {CryptoKey} key - AES-GCM CryptoKey
   * @param {Uint8Array} iv - 12-byte IV
   * @param {Uint8Array} [optionalAad] - Associated Authenticated Data
   * @returns {Promise<Uint8Array>} Decrypted plaintext
   */
  async decrypt(ciphertext, key, iv, optionalAad = null) {
    const algorithm = {
      name: 'AES-GCM',
      iv: iv,
      tagLength: this.TAG_LENGTH_BITS
    };

    if (optionalAad) {
      algorithm.additionalData = optionalAad;
    }

    const decryptedBuffer = await globalThis.crypto.subtle.decrypt(
      algorithm,
      key,
      ciphertext
    );

    return new Uint8Array(decryptedBuffer);
  },

  /**
   * Encrypts a UTF-8 string into Base64 ciphertext and IV.
   */
  async encryptString(text, key, optionalAad = null) {
    const enc = new TextEncoder();
    const plaintextBytes = enc.encode(text);
    const result = await this.encrypt(plaintextBytes, key, null, optionalAad);
    return {
      ciphertextBase64: this.bytesToBase64(result.ciphertext),
      ivBase64: this.bytesToBase64(result.iv)
    };
  },

  /**
   * Decrypts a Base64 ciphertext and IV into a UTF-8 string.
   */
  async decryptString(ciphertextBase64, ivBase64, key, optionalAad = null) {
    const ciphertext = this.base64ToBytes(ciphertextBase64);
    const iv = this.base64ToBytes(ivBase64);
    const decryptedBytes = await this.decrypt(ciphertext, key, iv, optionalAad);
    const dec = new TextDecoder('utf-8');
    return dec.decode(decryptedBytes);
  },

  /**
   * Converts Uint8Array to Base64.
   */
  bytesToBase64(bytes) {
    let binary = '';
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return globalThis.btoa ? globalThis.btoa(binary) : Buffer.from(bytes).toString('base64');
  },

  /**
   * Converts Base64 to Uint8Array.
   */
  base64ToBytes(base64) {
    if (globalThis.atob) {
      const binary = globalThis.atob(base64);
      const len = binary.length;
      const bytes = new Uint8Array(len);
      for (let i = 0; i < len; i++) {
        bytes[i] = binary.charCodeAt(i);
      }
      return bytes;
    }
    return new Uint8Array(Buffer.from(base64, 'base64'));
  },

  /**
   * Converts Uint8Array to Hex string.
   */
  bytesToHex(bytes) {
    return Array.from(bytes)
      .map((b) => b.toString(16).padStart(2, '0'))
      .join('');
  },

  /**
   * Overwrites sensitive memory buffer with zeroes.
   */
  zeroize(typedArray) {
    if (typedArray && typedArray.fill) {
      typedArray.fill(0);
    }
  }
};
