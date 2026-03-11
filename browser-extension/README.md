# CV Generator – Browser Extension

A Chrome/Edge browser extension that extracts job descriptions from any job posting page, lets you pick your profile type, and delivers a tailored PDF CV — triggering the full backend pipeline (Google Sheet, Google Drive, LaTeX).

---

## How it works

1. Open any job posting (LinkedIn, Indeed, Glassdoor, Jobs.ie, etc.)
2. Click the **CV Generator** extension icon
3. The job description is **auto-extracted** from the page
4. Choose your profile:
   - **Experienced** — 2-page CV (Red Fibre + SecurePoint + Tesco)
   - **Entry Level** — 1-page CV (Fresh grad, Tesco only)
5. Click **Generate CV**
6. Wait ~30-60 seconds while the backend runs Claude AI + LaTeX compilation
7. Click **Download PDF** — the PDF opens in a new tab

Behind the scenes, the backend also updates:
- Google Drive (PDF + LaTeX files)
- Google Sheets (job log with match scores)

---

## Installation (Developer Mode)

### Chrome / Edge

1. Open `chrome://extensions` (or `edge://extensions`)
2. Enable **Developer mode** (top-right toggle)
3. Click **Load unpacked**
4. Select the `browser-extension/` folder from this repo
5. The extension icon appears in your toolbar

### Firefox

1. Open `about:debugging#/runtime/this-firefox`
2. Click **Load Temporary Add-on...**
3. Select `browser-extension/manifest.json`

---

## First-time Setup

1. Click the ⚙️ gear icon in the extension popup
2. Set your **Backend URL** (e.g. `https://your-app.onrender.com`)
3. Click **Save**

> Default is `http://localhost:8055` for local development.

---

## Files

```
browser-extension/
├── manifest.json    Chrome Manifest V3
├── popup.html       Extension popup UI
├── popup.css        Dark theme styles
├── popup.js         Logic: extraction, API calls, polling, download
└── README.md        This file
```

---

## Supported Job Sites

Auto-extraction works on:

| Site | Status |
|------|--------|
| LinkedIn Jobs | ✅ |
| Indeed | ✅ |
| Glassdoor | ✅ |
| Jobs.ie | ✅ |
| IrishJobs.ie | ✅ |
| Monster | ✅ |
| Reed.co.uk | ✅ |
| TotalJobs | ✅ |
| Workday | ✅ |
| Greenhouse | ✅ |
| Lever | ✅ |
| Ashby | ✅ |
| Any other site | ✅ (auto-detect) |

If auto-extraction fails on a site, paste the job description manually.

---

## Permissions

| Permission | Why |
|------------|-----|
| `activeTab` | Read the current job posting tab |
| `scripting` | Inject the job description extractor |
| `storage` | Save your backend URL setting |
| `host_permissions: <all_urls>` | Call your backend from any origin |
