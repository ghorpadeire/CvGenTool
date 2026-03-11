/* ============================================================
   popup.js — CV Generator Browser Extension
   Handles: page extraction → API call → polling → PDF download
   ============================================================ */

const DEFAULT_BACKEND = 'http://localhost:8055';
const POLL_INTERVAL_MS = 2000;

// ── DOM refs ──────────────────────────────────────────────────
const detectionBanner  = document.getElementById('detectionBanner');
const detectionIcon    = document.getElementById('detectionIcon');
const detectionText    = document.getElementById('detectionText');
const jobDescEl        = document.getElementById('jobDescription');
const charCountEl      = document.getElementById('charCount');
const extractionStatus = document.getElementById('extractionStatus');
const generateBtn      = document.getElementById('generateBtn');
const progressSection  = document.getElementById('progressSection');
const progressLabel    = document.getElementById('progressLabel');
const progressPct      = document.getElementById('progressPct');
const progressBarFill  = document.getElementById('progressBarFill');
const resultsSection   = document.getElementById('resultsSection');
const matchScoresEl    = document.getElementById('matchScores');
const downloadBtn      = document.getElementById('downloadBtn');
const previewBtn       = document.getElementById('previewBtn');
const errorBox         = document.getElementById('errorBox');
const errorMsg         = document.getElementById('errorMsg');
const footerStatus     = document.getElementById('footerStatus');
const settingsToggle   = document.getElementById('settingsToggle');
const settingsPanel    = document.getElementById('settingsPanel');
const backendUrlInput  = document.getElementById('backendUrl');
const saveSettingsBtn  = document.getElementById('saveSettings');
const settingsSavedEl  = document.getElementById('settingsSaved');
const cardExperienced  = document.getElementById('cardExperienced');
const cardEntryLevel   = document.getElementById('cardEntryLevel');

let backendUrl = DEFAULT_BACKEND;
let pollTimer  = null;
let currentId  = null;

// ── Init ──────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
  await loadSettings();
  updateRadioCardStyles();
  await extractJobDescription();
});

// ── Settings ──────────────────────────────────────────────────
async function loadSettings() {
  return new Promise(resolve => {
    chrome.storage.local.get(['backendUrl'], result => {
      backendUrl = result.backendUrl || DEFAULT_BACKEND;
      backendUrlInput.value = backendUrl;
      resolve();
    });
  });
}

settingsToggle.addEventListener('click', () => {
  const isHidden = settingsPanel.style.display === 'none';
  settingsPanel.style.display = isHidden ? 'block' : 'none';
});

saveSettingsBtn.addEventListener('click', () => {
  const url = backendUrlInput.value.trim().replace(/\/$/, '');
  if (!url) return;
  backendUrl = url;
  chrome.storage.local.set({ backendUrl: url }, () => {
    settingsSavedEl.style.display = 'inline';
    setTimeout(() => { settingsSavedEl.style.display = 'none'; }, 2000);
  });
});

// ── Radio card visual update ───────────────────────────────────
document.querySelectorAll('input[name="experienceLevel"]').forEach(radio => {
  radio.addEventListener('change', updateRadioCardStyles);
});

function updateRadioCardStyles() {
  const val = getExperienceLevel();
  cardExperienced.classList.toggle('selected', val === 'EXPERIENCED');
  cardEntryLevel.classList.toggle('selected', val === 'ENTRY_LEVEL');
}

function getExperienceLevel() {
  const checked = document.querySelector('input[name="experienceLevel"]:checked');
  return checked ? checked.value : 'EXPERIENCED';
}

// ── Char counter ───────────────────────────────────────────────
jobDescEl.addEventListener('input', () => {
  const len = jobDescEl.value.length;
  charCountEl.textContent = `${len.toLocaleString()} / 50,000`;
  charCountEl.classList.toggle('warn', len > 45000);
  generateBtn.disabled = len < 50;
});

// ── Job description extraction ────────────────────────────────
async function extractJobDescription() {
  showBanner('📄', 'Scanning page...', '');

  let tab;
  try {
    [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  } catch {
    showBanner('⚠️', 'Cannot access current tab', 'warning');
    extractionStatus.textContent = 'Paste the job description manually.';
    return;
  }

  if (!tab || !tab.id) {
    showBanner('⚠️', 'No active tab found', 'warning');
    return;
  }

  // Skip extension pages / chrome:// pages
  if (!tab.url || tab.url.startsWith('chrome://') || tab.url.startsWith('about:')) {
    showBanner('ℹ️', 'Open a job posting page first', 'warning');
    extractionStatus.textContent = 'Navigate to a job posting and try again.';
    return;
  }

  try {
    const results = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      func: pageExtractor
    });

    const info = results && results[0] && results[0].result;
    if (!info) throw new Error('No result from page');

    if (info.jobDescription && info.jobDescription.length > 50) {
      jobDescEl.value = info.jobDescription.substring(0, 50000);
      jobDescEl.dispatchEvent(new Event('input'));

      const site = getSiteName(tab.url);
      showBanner('✅', `Job extracted from ${site}`, 'success');
      extractionStatus.textContent = `${info.jobDescription.length.toLocaleString()} characters extracted.`;
    } else {
      showBanner('⚠️', 'Could not auto-extract — paste manually', 'warning');
      extractionStatus.textContent = 'Paste the job description in the box above.';
    }

  } catch (err) {
    // scripting permission denied on this page (e.g., Chrome Web Store)
    showBanner('⚠️', 'Cannot read this page — paste manually', 'warning');
    extractionStatus.textContent = 'Paste the job description in the box above.';
  }
}

// ── Injected page extractor (runs in page context) ─────────────
function pageExtractor() {
  const result = { jobDescription: '' };
  const hostname = window.location.hostname;

  // Site-specific selectors (most reliable)
  const siteSelectors = {
    'linkedin.com':  ['.jobs-description__content', '#job-details', '.jobs-box__html-content'],
    'indeed.com':    ['#jobDescriptionText', '.jobsearch-jobDescriptionText'],
    'glassdoor.com': ['[data-test="jobDescription"]', '.JobDetails_jobDescription__uW_fK', '.desc'],
    'jobs.ie':       ['.job-description', '.jobdetails__description'],
    'irishjobs.ie':  ['.job-description', '.jobBody'],
    'monster.com':   ['#jobDescription', '.job-description'],
    'reed.co.uk':    ['[data-qa="job-description"]', '.description'],
    'totaljobs.com': ['.job-description', '#job-content'],
    'workday.com':   ['[data-automation-id="jobPostingDescription"]'],
    'greenhouse.io': ['#content', '.job__description'],
    'lever.co':      ['.posting-description', '.section-wrapper'],
    'jobs.ashbyhq.com': ['.job-description']
  };

  // Try site-specific first
  for (const [domain, selectors] of Object.entries(siteSelectors)) {
    if (hostname.includes(domain)) {
      for (const sel of selectors) {
        const el = document.querySelector(sel);
        if (el && el.innerText.trim().length > 100) {
          result.jobDescription = el.innerText.trim();
          return result;
        }
      }
    }
  }

  // Generic fallbacks
  const genericSelectors = [
    '[class*="job-description"]', '[id*="job-description"]',
    '[class*="jobDescription"]',  '[id*="jobDescription"]',
    '[class*="job_description"]', '[id*="job_description"]',
    '[class*="JobDescription"]',
    '[data-testid*="description"]', '[data-qa*="description"]',
    '.description', '#description',
    'article[class*="job"]', 'section[class*="job"]'
  ];

  for (const sel of genericSelectors) {
    const el = document.querySelector(sel);
    if (el && el.innerText.trim().length > 200) {
      result.jobDescription = el.innerText.trim();
      return result;
    }
  }

  // Last resort: find the largest meaningful text block
  let best = null, bestLen = 0;
  document.querySelectorAll('div, section, article').forEach(el => {
    const text = el.innerText ? el.innerText.trim() : '';
    const childCount = el.children.length;
    // Must be substantial text, not the whole page, and have some structure
    if (text.length > bestLen && text.length < 40000 && text.length > 500 && childCount >= 2) {
      bestLen = text.length;
      best = el;
    }
  });

  if (best) result.jobDescription = best.innerText.trim();
  return result;
}

function getSiteName(url) {
  try {
    const hostname = new URL(url).hostname.replace('www.', '');
    const parts = hostname.split('.');
    return parts.length > 1
      ? parts[parts.length - 2].charAt(0).toUpperCase() + parts[parts.length - 2].slice(1)
      : hostname;
  } catch {
    return 'page';
  }
}

function showBanner(icon, text, type) {
  detectionBanner.style.display = 'flex';
  detectionIcon.textContent = icon;
  detectionText.textContent = text;
  detectionBanner.className = 'detection-banner' + (type ? ` ${type}` : '');
}

// ── Generate CV ───────────────────────────────────────────────
generateBtn.addEventListener('click', generateCv);

async function generateCv() {
  const jd = jobDescEl.value.trim();
  if (jd.length < 50) {
    showError('Job description must be at least 50 characters.');
    return;
  }

  const level = getExperienceLevel();

  // Reset UI
  hideError();
  hideResults();
  showProgress('Sending to backend...', 5);
  setGenerating(true);
  setFooterStatus('Generating...');

  try {
    const response = await fetch(`${backendUrl}/api/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        jobDescription: jd,
        experienceLevel: level,
        forceRegenerate: false
      })
    });

    if (!response.ok) {
      const err = await response.text();
      throw new Error(`Backend error (${response.status}): ${err}`);
    }

    const data = await response.json();
    currentId = data.id;

    if (data.status === 'COMPLETED') {
      // Cache hit — immediately show results
      showProgress('Loaded from cache!', 100);
      setTimeout(() => showResults(data), 400);
    } else {
      // Start polling
      startPolling(currentId);
    }

  } catch (err) {
    setGenerating(false);
    hideProgress();
    setFooterStatus('Error');
    if (err.message.includes('Failed to fetch') || err.message.includes('NetworkError')) {
      showError(`Cannot reach backend at ${backendUrl}.\nCheck Settings and make sure your CV Generator app is running.`);
    } else {
      showError(err.message);
    }
  }
}

// ── Polling ───────────────────────────────────────────────────
function startPolling(id) {
  clearInterval(pollTimer);
  pollTimer = setInterval(() => poll(id), POLL_INTERVAL_MS);
}

async function poll(id) {
  try {
    const response = await fetch(`${backendUrl}/api/status/${id}`);
    if (!response.ok) throw new Error(`Status check failed (${response.status})`);

    const data = await response.json();

    if (data.status === 'COMPLETED') {
      clearInterval(pollTimer);
      showProgress(data.currentStep || 'Done!', 100);
      setTimeout(() => showResults(data), 400);
      return;
    }

    if (data.status === 'FAILED') {
      clearInterval(pollTimer);
      setGenerating(false);
      hideProgress();
      setFooterStatus('Failed');
      showError(data.errorMessage || 'CV generation failed. Please try again.');
      return;
    }

    // PROCESSING
    const pct  = data.progress || 10;
    const step = data.currentStep || 'Processing...';
    showProgress(step, pct);

  } catch (err) {
    // Don't abort on transient network errors — keep polling
    console.warn('Poll error:', err.message);
  }
}

// ── Results ───────────────────────────────────────────────────
function showResults(data) {
  setGenerating(false);
  hideProgress();
  setFooterStatus('Ready');

  // Match scores
  matchScoresEl.innerHTML = '';
  if (data.matchScore) {
    if (data.matchScore.keywordCoverage != null) {
      matchScoresEl.innerHTML += `<span class="score-pill">Keywords: ${data.matchScore.keywordCoverage}%</span>`;
    }
    if (data.matchScore.recruiterFit != null) {
      matchScoresEl.innerHTML += `<span class="score-pill">Fit: ${data.matchScore.recruiterFit}%</span>`;
    }
  }
  if (data.recruiterDomain) {
    matchScoresEl.innerHTML += `<span class="score-pill">${capitalize(data.recruiterDomain)}</span>`;
  }

  // Wire buttons
  downloadBtn.onclick = () => {
    chrome.tabs.create({ url: `${backendUrl}/api/download/${data.id}/pdf` });
  };
  previewBtn.onclick = () => {
    chrome.tabs.create({ url: `${backendUrl}/api/download/${data.id}/preview` });
  };

  resultsSection.style.display = 'flex';
  showBanner('🎉', `CV ready! ${data.companyName ? '· ' + data.companyName : ''}`, 'success');
}

// ── UI helpers ────────────────────────────────────────────────
function setGenerating(loading) {
  generateBtn.disabled = loading;
  generateBtn.classList.toggle('loading', loading);
  const btnText = generateBtn.querySelector('.btn-text');
  btnText.textContent = loading ? 'Generating...' : 'Generate CV';
  const btnIcon = generateBtn.querySelector('.btn-icon');
  btnIcon.textContent = loading ? '⏳' : '→';
}

function showProgress(label, pct) {
  progressSection.style.display = 'flex';
  progressLabel.textContent = label;
  progressPct.textContent = `${pct}%`;
  progressBarFill.style.width = `${pct}%`;
}

function hideProgress() {
  progressSection.style.display = 'none';
}

function hideResults() {
  resultsSection.style.display = 'none';
}

function showError(msg) {
  errorBox.style.display = 'flex';
  errorMsg.textContent = msg;
}

function hideError() {
  errorBox.style.display = 'none';
}

function setFooterStatus(status) {
  footerStatus.textContent = status;
}

function capitalize(str) {
  return str ? str.charAt(0).toUpperCase() + str.slice(1) : '';
}
