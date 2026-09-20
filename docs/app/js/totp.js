/**
 * OpenVault TOTP Engine
 * RFC 6238 (TOTP) and RFC 4226 (HOTP) compliant 2FA authenticator engine.
 * Pure Web Crypto implementation supporting SHA-1, SHA-256, SHA-512, 6/8 digits.
 */

export const TotpEngine = {
  /**
   * Base32 alphabet characters.
   */
  BASE32_CHARS: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567',

  /**
   * Decodes a Base32 string into a Uint8Array.
   */
  base32ToBytes(base32) {
    const cleanStr = base32.toUpperCase().replace(/[\s=-]/g, '');
    if (!cleanStr) return new Uint8Array(0);

    let bits = 0;
    let value = 0;
    const output = [];

    for (let i = 0; i < cleanStr.length; i++) {
      const idx = this.BASE32_CHARS.indexOf(cleanStr[i]);
      if (idx === -1) {
        throw new Error(`Invalid Base32 character: ${cleanStr[i]}`);
      }

      value = (value << 5) | idx;
      bits += 5;

      if (bits >= 8) {
        output.push((value >>> (bits - 8)) & 255);
        bits -= 8;
      }
    }

    return new Uint8Array(output);
  },

  /**
   * Generates a TOTP code for a given secret at the current or specified time.
   * @param {string} secret - Base32 encoded secret or otpauth URI
   * @param {number} [timestamp] - Current epoch time in milliseconds (defaults to Date.now())
   * @param {number} [period=30] - Time step in seconds
   * @param {number} [digits=6] - Number of output digits (6 or 8)
   * @param {string} [algorithm='SHA-1'] - 'SHA-1', 'SHA-256', or 'SHA-512'
   * @returns {Promise<string>} Formatted token string (e.g., '123456')
   */
  async generateCode(secret, timestamp = Date.now(), period = 30, digits = 6, algorithm = 'SHA-1') {
    let cleanSecret = secret.trim();
    if (cleanSecret.startsWith('otpauth://')) {
      const parsed = this.parseOtpAuthUri(cleanSecret);
      cleanSecret = parsed.secret;
      period = parsed.period || period;
      digits = parsed.digits || digits;
      algorithm = parsed.algorithm || algorithm;
    }

    const keyBytes = this.base32ToBytes(cleanSecret);
    if (keyBytes.length === 0) return '------';

    const epochSeconds = Math.floor(timestamp / 1000);
    const counter = Math.floor(epochSeconds / period);

    // Encode counter as 8-byte big-endian Uint8Array
    const counterBuffer = new ArrayBuffer(8);
    const counterView = new DataView(counterBuffer);
    counterView.setBigUint64(0, BigInt(counter), false);

    // WebCrypto HMAC algorithm mapping
    const hashName = algorithm.toUpperCase().replace('-', '');
    const webCryptoHash = hashName === 'SHA1' ? 'SHA-1' :
                          hashName === 'SHA256' ? 'SHA-256' :
                          hashName === 'SHA512' ? 'SHA-512' : 'SHA-1';

    const hmacKey = await globalThis.crypto.subtle.importKey(
      'raw',
      keyBytes,
      { name: 'HMAC', hash: { name: webCryptoHash } },
      false,
      ['sign']
    );

    const signature = await globalThis.crypto.subtle.sign('HMAC', hmacKey, counterBuffer);
    const hash = new Uint8Array(signature);

    // Dynamic truncation (RFC 4226 Section 5.4)
    const offset = hash[hash.length - 1] & 0x0f;
    const binary =
      ((hash[offset] & 0x7f) << 24) |
      ((hash[offset + 1] & 0xff) << 16) |
      ((hash[offset + 2] & 0xff) << 8) |
      (hash[offset + 3] & 0xff);

    const modulo = Math.pow(10, digits);
    const codeNum = binary % modulo;

    return codeNum.toString().padStart(digits, '0');
  },

  /**
   * Calculates the remaining seconds and progress ratio in the current TOTP period.
   * @param {number} [period=30]
   * @param {number} [timestamp=Date.now()]
   * @returns {{ remainingSeconds: number, progressRatio: number }}
   */
  getRemainingTime(period = 30, timestamp = Date.now()) {
    const epochSeconds = Math.floor(timestamp / 1000);
    const elapsedSeconds = epochSeconds % period;
    const remainingSeconds = period - elapsedSeconds;
    const progressRatio = remainingSeconds / period;
    return { remainingSeconds, progressRatio };
  },

  /**
   * Parses an `otpauth://totp/...` URI into its component parameters.
   */
  parseOtpAuthUri(uriString) {
    try {
      const url = new URL(uriString);
      if (url.protocol !== 'otpauth:') {
        throw new Error('Not an otpauth URI');
      }

      const params = url.searchParams;
      const secret = params.get('secret') || '';
      const issuer = params.get('issuer') || '';
      const period = parseInt(params.get('period') || '30', 10);
      const digits = parseInt(params.get('digits') || '6', 10);
      const algorithm = (params.get('algorithm') || 'SHA1').toUpperCase();

      let label = decodeURIComponent(url.pathname.replace(/^\/+/, ''));
      let accountName = label;
      if (label.includes(':')) {
        const parts = label.split(':');
        accountName = parts[1].trim();
      }

      return {
        secret,
        issuer,
        accountName,
        period,
        digits,
        algorithm
      };
    } catch {
      return {
        secret: uriString,
        issuer: '',
        accountName: '',
        period: 30,
        digits: 6,
        algorithm: 'SHA-1'
      };
    }
  }
};
