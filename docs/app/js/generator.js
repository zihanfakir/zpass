/**
 * OpenVault Password & Passphrase Generator
 * Cryptographically secure CSPRNG generator supporting random character passwords
 * and Diceware multi-word passphrases with Shannon entropy and crack time calculation.
 */

// Curated 700+ high-frequency Diceware wordlist for memorable multi-word passphrases
const DICEWARE_WORDLIST = [
  "ability", "absence", "academy", "account", "accused", "achieve", "acquire", "address",
  "advance", "adviser", "airline", "airport", "alcohol", "alleged", "already", "analyst",
  "ancient", "another", "anxiety", "anybody", "applied", "arrange", "arrival", "article",
  "assault", "assumed", "athlete", "attempt", "attract", "auction", "average", "backing",
  "balance", "banking", "barrier", "battery", "bearing", "beating", "because", "bedroom",
  "believe", "beneath", "benefit", "besides", "between", "billion", "binding", "brother",
  "brought", "cabinet", "calibre", "calling", "capable", "capital", "captain", "caption",
  "capture", "careful", "carrier", "caution", "ceiling", "central", "centric", "century",
  "certain", "chamber", "channel", "chapter", "charity", "charlie", "charter", "cheaper",
  "checked", "chicken", "chronic", "circuit", "citizen", "classic", "climate", "closing",
  "clothes", "coastal", "cognition", "collect", "college", "combine", "comfort", "command",
  "comment", "compact", "company", "compare", "compete", "complex", "concept", "concern",
  "concert", "conduct", "confirm", "connect", "consent", "consist", "contact", "contain",
  "content", "contest", "context", "control", "convert", "correct", "council", "counsel",
  "counter", "country", "crucial", "crystal", "culture", "current", "cutting", "dealing",
  "decided", "decline", "default", "defence", "deficit", "deliver", "density", "deposit",
  "desktop", "despite", "destroy", "develop", "devoted", "diamond", "digital", "diploma",
  "disable", "discuss", "disease", "display", "dispute", "distant", "diverse", "divided",
  "divorce", "doctor", "dolphin", "domain", "dynamic", "eastern", "economy", "edition",
  "elderly", "element", "ellipse", "embargo", "embassy", "embrace", "emotion", "emperor",
  "empower", "endless", "endorse", "endowed", "enforce", "engaged", "enhance", "enquiry",
  "enterprise", "enthusiasm", "entropy", "episode", "equator", "essence", "eternal", "evening",
  "evident", "exactly", "examine", "example", "excited", "exclude", "execute", "exempt",
  "exhaust", "exhibit", "expense", "explain", "explore", "express", "extreme", "factory",
  "faculty", "failure", "fairway", "fantasy", "farming", "fashion", "feather", "federal",
  "feeling", "fertile", "fighter", "finance", "finding", "firearm", "fireman", "fitness",
  "fixture", "flavour", "flight", "florida", "focusing", "footage", "foreign", "forever",
  "formula", "fortune", "forward", "founder", "freedom", "freeway", "freight", "frequent",
  "friendly", "frontier", "further", "gallery", "gateway", "general", "generic", "genetic",
  "genuine", "gigabit", "glacier", "glamour", "glasses", "glimpse", "glorious", "governor",
  "gradual", "gravity", "greater", "grocery", "growing", "guidance", "habitat", "harbour",
  "harmony", "heading", "healthy", "hearing", "heavily", "helpful", "highway", "history",
  "holding", "holiday", "horizon", "hormone", "hospital", "hostile", "housing", "hundred",
  "husband", "identity", "illegal", "illness", "imagery", "imaging", "impact", "impasse",
  "implicit", "improve", "impulse", "include", "income", "increase", "indicate", "industry",
  "infantry", "infinite", "inform", "initial", "injured", "inquiry", "insight", "inspire",
  "install", "instant", "instead", "intense", "interim", "internal", "intimate", "invaded",
  "inventor", "investor", "isolate", "ivory", "jacket", "jaguar", "january", "journey",
  "justice", "justify", "kingdom", "kitchen", "knowing", "landing", "lantern", "largely",
  "lasting", "launch", "laundry", "lawyer", "leading", "league", "leather", "legacy",
  "leisure", "lending", "length", "lesson", "liberty", "library", "license", "limited",
  "lineup", "linkage", "liquid", "literal", "lizard", "loading", "logical", "loyalty",
  "luggage", "machine", "magenta", "magnify", "maiden", "majesty", "mammal", "manager",
  "mandate", "mansion", "manual", "margin", "marine", "marking", "massive", "master",
  "matched", "maximum", "meaning", "measure", "medical", "meeting", "melody", "melting",
  "member", "memory", "mental", "merchant", "message", "meteor", "midday", "midnight",
  "military", "mineral", "minimum", "miracle", "mission", "mistake", "mixture", "mobility",
  "modular", "monitor", "monster", "monthly", "monument", "morning", "mortgage", "mountain",
  "musical", "mystery", "narrate", "natural", "navigate", "nebula", "nervous", "network",
  "neutral", "nominee", "notable", "nothing", "novelty", "nuclear", "nursing", "oakwood",
  "obelisk", "observe", "obvious", "occasion", "oceanic", "octopus", "offense", "officer",
  "olympic", "ongoing", "opening", "operate", "opinion", "optical", "optimism", "optimum",
  "orbital", "orderly", "organic", "origin", "outdoor", "outlook", "outrage", "overall",
  "oversee", "package", "painful", "painter", "palette", "panther", "paradox", "parallel",
  "parliament", "partial", "partner", "passage", "passive", "pasture", "patient", "pattern",
  "payment", "peacock", "penalty", "pending", "pension", "percent", "perfect", "perform",
  "perhaps", "period", "permit", "persist", "personal", "phantom", "physics", "pioneer",
  "pipeline", "planner", "plastic", "platform", "pleasant", "pledge", "plenty", "poetic",
  "pointer", "polar", "popular", "portrait", "position", "positive", "possible", "postage",
  "posture", "poverty", "powerful", "practical", "practice", "precious", "predict", "prefer",
  "premier", "premise", "premium", "prepare", "presence", "present", "preserve", "prevent",
  "preview", "primary", "primate", "princess", "priority", "privacy", "private", "problem",
  "process", "produce", "product", "profile", "program", "project", "promise", "promote",
  "protect", "protein", "protest", "protocol", "proudly", "provide", "province", "prudent",
  "publish", "pulmonary", "purpose", "pursuit", "pyramid", "qualify", "quality", "quantum",
  "quarter", "queenly", "quietly", "radical", "railway", "rainbow", "random", "ranking",
  "rapidly", "reactor", "reading", "reality", "realize", "rebound", "receipt", "receive",
  "recess", "recipe", "reckon", "recover", "recruit", "referee", "reflect", "reform",
  "refuge", "refusal", "regard", "regime", "regular", "reissue", "relate", "release",
  "relevant", "reliable", "relief", "religion", "remedy", "remind", "removal", "renewed",
  "replace", "replica", "repress", "request", "require", "rescue", "reserve", "reside",
  "resolve", "resort", "resource", "respect", "respond", "restore", "results", "retail",
  "retain", "retreat", "reunion", "revenue", "reverse", "revival", "rhombus", "ribbon",
  "richness", "rigorous", "ringlet", "ripple", "romance", "rooftop", "routine", "royalty",
  "running", "rupture", "safeguard", "sailor", "salient", "sampler", "sanction", "satellite",
  "satisfy", "scanner", "scenery", "sceptic", "scholar", "science", "scraper", "screen",
  "script", "seafood", "season", "secondary", "secrecy", "section", "sector", "secured",
  "segment", "seldom", "semantic", "senator", "senior", "sensible", "sentence", "separate",
  "sequence", "serenity", "servant", "service", "session", "settle", "seventh", "several",
  "shadow", "shallow", "shelter", "sheriff", "shifter", "shortly", "shoulder", "showcase",
  "sibling", "signals", "silence", "similar", "simple", "sincere", "singular", "situate",
  "sixteen", "skeleton", "skillful", "skylight", "sleeping", "slightly", "slippery", "slowly",
  "smartly", "snapshot", "social", "society", "software", "soldier", "solution", "somebody",
  "somewhat", "sovereign", "sparkle", "spatial", "speaker", "special", "specific", "specter",
  "spectrum", "spinach", "splendid", "sponsor", "spotted", "squall", "stadium", "staffer",
  "standard", "starfish", "starlight", "statement", "station", "statue", "steamer", "sterling",
  "stimulus", "stomach", "storage", "strategy", "strength", "striking", "string", "structure",
  "struggle", "student", "stunning", "subject", "sublime", "substance", "succeed", "success",
  "suggest", "suitable", "summary", "sunlight", "superior", "supply", "support", "surface",
  "surgeon", "surprise", "surround", "survival", "suspect", "swimming", "symbol", "symmetry",
  "symptom", "tactics", "talent", "talking", "tangent", "target", "teacher", "teaspoon",
  "technical", "telecom", "template", "temporal", "tenancy", "tender", "tentacle", "terminal",
  "terrace", "terrific", "testify", "theater", "therapy", "thermal", "thinking", "thought",
  "thousand", "thriving", "thunder", "tightly", "timber", "timeline", "titanium", "tolerant",
  "tonight", "tornado", "total", "tourism", "towards", "tracking", "tradition", "traffic",
  "tragedy", "traitor", "transfer", "treasure", "tremble", "triangle", "trigger", "triumph",
  "tropical", "trouble", "trustee", "tsunami", "turbulent", "turning", "typical", "typhoon",
  "ultimate", "umbrella", "uncover", "undergo", "uniform", "universe", "unleash", "unlock",
  "unstable", "upward", "uranium", "urgency", "utopian", "vacant", "vacuum", "valence",
  "valiant", "validity", "valuable", "variable", "variant", "variety", "various", "vehicle",
  "velocity", "venture", "verdict", "vertical", "veteran", "vibrant", "victory", "vintage",
  "violent", "virtual", "visible", "vision", "visitor", "vitality", "vividly", "vocalist",
  "volcano", "voltage", "voluntary", "vortex", "voyager", "waiting", "walkway", "warrior",
  "wealthy", "weather", "webpage", "weekend", "welcome", "welfare", "western", "whisper",
  "wildlife", "willing", "window", "wireless", "wisdom", "witness", "wonder", "working",
  "workshop", "worship", "worthy", "wrinkle", "yielding", "youthful", "zealous", "zenith"
];

export const PasswordGenerator = {
  UPPERCASE: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
  LOWERCASE: 'abcdefghijklmnopqrstuvwxyz',
  DIGITS: '0123456789',
  SYMBOLS: '!@#$%^&*()_+-=[]{}|;:,.<>?',
  AMBIGUOUS_REGEX: /[0O1lI]/g,

  /**
   * Generates a random character password.
   */
  generatePassword(options = {}) {
    const {
      length = 16,
      useUppercase = true,
      useLowercase = true,
      useDigits = true,
      useSymbols = true,
      avoidAmbiguous = true
    } = options;

    let charset = '';
    const guaranteed = [];

    if (useUppercase) {
      const set = avoidAmbiguous ? this.UPPERCASE.replace(this.AMBIGUOUS_REGEX, '') : this.UPPERCASE;
      charset += set;
      guaranteed.push(this.randomCharFrom(set));
    }
    if (useLowercase) {
      const set = avoidAmbiguous ? this.LOWERCASE.replace(this.AMBIGUOUS_REGEX, '') : this.LOWERCASE;
      charset += set;
      guaranteed.push(this.randomCharFrom(set));
    }
    if (useDigits) {
      const set = avoidAmbiguous ? this.DIGITS.replace(this.AMBIGUOUS_REGEX, '') : this.DIGITS;
      charset += set;
      guaranteed.push(this.randomCharFrom(set));
    }
    if (useSymbols) {
      charset += this.SYMBOLS;
      guaranteed.push(this.randomCharFrom(this.SYMBOLS));
    }

    if (!charset) {
      charset = this.LOWERCASE;
    }

    const remaining = Math.max(0, length - guaranteed.length);
    const randomChars = [];
    const randomBytes = new Uint32Array(remaining);
    globalThis.crypto.getRandomValues(randomBytes);

    for (let i = 0; i < remaining; i++) {
      randomChars.push(charset[randomBytes[i] % charset.length]);
    }

    // Combine guaranteed and remaining characters, then cryptographically shuffle
    const all = guaranteed.concat(randomChars);
    return this.shuffle(all).join('').slice(0, length);
  },

  /**
   * Generates a Diceware multi-word passphrase.
   */
  generatePassphrase(options = {}) {
    const {
      wordCount = 4,
      separator = '-',
      capitalize = true,
      includeNumber = true
    } = options;

    const words = [];
    const randomBytes = new Uint32Array(wordCount);
    globalThis.crypto.getRandomValues(randomBytes);

    for (let i = 0; i < wordCount; i++) {
      let word = DICEWARE_WORDLIST[randomBytes[i] % DICEWARE_WORDLIST.length];
      if (capitalize) {
        word = word.charAt(0).toUpperCase() + word.slice(1);
      }
      words.push(word);
    }

    if (includeNumber) {
      const numBytes = new Uint8Array(1);
      globalThis.crypto.getRandomValues(numBytes);
      const randomNum = (numBytes[0] % 90) + 10; // 2-digit number (10-99)
      const insertIndex = numBytes[0] % (words.length + 1);
      if (insertIndex === words.length) {
        words[words.length - 1] += randomNum.toString();
      } else {
        words[0] = randomNum.toString() + words[0];
      }
    }

    return words.join(separator);
  },

  /**
   * Returns a random character from a string using CSPRNG.
   */
  randomCharFrom(str) {
    if (!str) return '';
    const bytes = new Uint32Array(1);
    globalThis.crypto.getRandomValues(bytes);
    return str[bytes[0] % str.length];
  },

  /**
   * Performs Fisher-Yates shuffle using CSPRNG.
   */
  shuffle(arr) {
    const result = [...arr];
    const bytes = new Uint32Array(result.length);
    globalThis.crypto.getRandomValues(bytes);

    for (let i = result.length - 1; i > 0; i--) {
      const j = bytes[i] % (i + 1);
      const temp = result[i];
      result[i] = result[j];
      result[j] = temp;
    }
    return result;
  },

  /**
   * Calculates Shannon entropy in bits for a given string.
   */
  calculateEntropy(password) {
    if (!password) return 0;

    let poolSize = 0;
    if (/[a-z]/.test(password)) poolSize += 26;
    if (/[A-Z]/.test(password)) poolSize += 26;
    if (/[0-9]/.test(password)) poolSize += 10;
    if (/[^a-zA-Z0-9]/.test(password)) poolSize += 33;

    if (poolSize === 0) poolSize = 10;
    const entropy = password.length * (Math.log(poolSize) / Math.log(2));
    return Math.round(entropy * 10) / 10;
  },

  /**
   * Categorizes password strength based on entropy and length.
   */
  getStrength(password) {
    if (!password || password.length === 0) {
      return { label: 'Empty', score: 0, color: '#64748B', crackTime: 'Instant' };
    }

    const entropy = this.calculateEntropy(password);
    const length = password.length;

    let score = 0;
    let label = 'Very Weak';
    let color = '#EF4444';
    let crackTime = '< 1 second';

    if (entropy < 28 || length < 8) {
      score = 1;
      label = 'Very Weak';
      color = '#EF4444';
      crackTime = '< 1 second';
    } else if (entropy < 45 || length < 10) {
      score = 2;
      label = 'Weak';
      color = '#F97316';
      crackTime = 'A few minutes';
    } else if (entropy < 65 || length < 12) {
      score = 3;
      label = 'Fair';
      color = '#EAB308';
      crackTime = 'Several months';
    } else if (entropy < 90) {
      score = 4;
      label = 'Strong';
      color = '#10B981';
      crackTime = 'Thousands of years';
    } else {
      score = 5;
      label = 'Very Strong';
      color = '#059669';
      crackTime = 'Centuries / Impractical';
    }

    return { score, label, color, entropy, crackTime };
  }
};
