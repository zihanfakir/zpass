/**
 * OpenVault Security Audit Engine
 * Evaluates vault health: identifies weak passwords, duplicate/reused credentials,
 * logins missing 2FA / TOTP, and generates a security health score (0 - 100).
 */

import { PasswordGenerator } from './generator.js';

export const SecurityAudit = {
  analyzeVault(items) {
    const logins = items.filter((item) => (item.type || 'LOGIN') === 'LOGIN');

    const weakItems = [];
    const reusedItemsMap = new Map(); // password -> [item]
    const missingTotpItems = [];

    for (const item of logins) {
      const pwd = item.password || '';

      // Check strength
      const strength = PasswordGenerator.getStrength(pwd);
      if (strength.score <= 2) {
        weakItems.push({ item, strength });
      }

      // Check reuse
      if (pwd.length > 0) {
        if (!reusedItemsMap.has(pwd)) {
          reusedItemsMap.set(pwd, []);
        }
        reusedItemsMap.get(pwd).push(item);
      }

      // Check TOTP
      if (!item.totpSecret || item.totpSecret.trim().length === 0) {
        missingTotpItems.push(item);
      }
    }

    // Filter to only passwords used more than once
    const reusedGroups = [];
    for (const [password, group] of reusedItemsMap.entries()) {
      if (group.length > 1) {
        reusedGroups.push({
          passwordMasked: '••••••••',
          count: group.length,
          items: group
        });
      }
    }

    // Calculate overall security health score (0 - 100)
    let score = 100;
    if (logins.length > 0) {
      // Deduct for weak passwords: up to 40 points
      const weakPenalty = Math.min(40, (weakItems.length / logins.length) * 50);
      // Deduct for reused passwords: up to 35 points
      const totalReused = reusedGroups.reduce((acc, g) => acc + g.count, 0);
      const reusePenalty = Math.min(35, (totalReused / logins.length) * 40);
      // Deduct for missing 2FA: up to 25 points
      const totpPenalty = Math.min(25, (missingTotpItems.length / logins.length) * 25);

      score = Math.max(0, Math.round(100 - weakPenalty - reusePenalty - totpPenalty));
    }

    return {
      totalLogins: logins.length,
      healthScore: score,
      weakItems,
      reusedGroups,
      missingTotpItems
    };
  }
};
