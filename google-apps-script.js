/**
 * Google Apps Script for CV Generator History Logging
 *
 * Features:
 * - Saves PDFs to Google Drive folder
 * - Logs generation history to Google Sheets
 * - Clean, formatted spreadsheet with PDF links
 */

// Configuration
const SHEET_NAME = "CV Generation History";
const FOLDER_ID = "1w1OVjXeIy0S8Za-W5DSxlrJuzZpCsepf"; // Your Google Drive folder
const PDF_SUBFOLDER_NAME = "Generated CVs"; // Subfolder for PDFs

/**
 * Handles POST requests from the CV Generator app
 */
function doPost(e) {
  try {
    const data = JSON.parse(e.postData.contents);
    const sheet = getOrCreateSheet();

    // Save PDF to Drive if provided
    let pdfUrl = "";
    if (data.pdfBase64 && data.pdfFilename) {
      pdfUrl = savePdfToDrive(data.pdfBase64, data.pdfFilename);
    }

    // Parse coach brief to extract key info
    const coachData = parseCoachBrief(data.coachBrief);

    // Format date nicely
    const formattedDate = data.date ?
      Utilities.formatDate(new Date(data.date), Session.getScriptTimeZone(), "dd-MMM-yyyy HH:mm") :
      Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "dd-MMM-yyyy HH:mm");

    // Clean job description - extract only key info
    const cleanJD = cleanJobDescription(data.jobDescription);

    // Append the new row
    sheet.appendRow([
      formattedDate,
      data.company || "Unknown",
      cleanJD,
      data.matchScore || 0,
      pdfUrl,  // PDF Link column
      coachData.skillGaps,
      coachData.sevenDays,
      coachData.fourteenDays,
      coachData.interviewQuestions
    ]);

    // Auto-resize and format the new row
    const lastRow = sheet.getLastRow();
    formatDataRow(sheet, lastRow);

    return ContentService
      .createTextOutput(JSON.stringify({ success: true, pdfUrl: pdfUrl }))
      .setMimeType(ContentService.MimeType.JSON);

  } catch (error) {
    Logger.log("Error: " + error.toString());
    return ContentService
      .createTextOutput(JSON.stringify({ success: false, error: error.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

/**
 * Saves PDF to Google Drive and returns the shareable link
 */
function savePdfToDrive(base64Data, filename) {
  try {
    // Get or create PDF subfolder
    const mainFolder = DriveApp.getFolderById(FOLDER_ID);
    let pdfFolder;

    const subfolders = mainFolder.getFoldersByName(PDF_SUBFOLDER_NAME);
    if (subfolders.hasNext()) {
      pdfFolder = subfolders.next();
    } else {
      pdfFolder = mainFolder.createFolder(PDF_SUBFOLDER_NAME);
    }

    // Decode base64 and create file
    const decodedData = Utilities.base64Decode(base64Data);
    const blob = Utilities.newBlob(decodedData, 'application/pdf', filename);

    // Save to Drive
    const file = pdfFolder.createFile(blob);

    // Set sharing to "Anyone with link can view"
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);

    // Return the shareable link
    return file.getUrl();

  } catch (error) {
    Logger.log("Error saving PDF: " + error.toString());
    return "Error saving PDF";
  }
}

/**
 * Handles GET requests (for testing)
 */
function doGet(e) {
  return ContentService
    .createTextOutput(JSON.stringify({
      status: "CV Generator Logger is running",
      sheetUrl: getOrCreateSheet().getParent().getUrl()
    }))
    .setMimeType(ContentService.MimeType.JSON);
}

/**
 * Cleans job description - extracts key info only
 */
function cleanJobDescription(jd) {
  if (!jd) return "";

  // Remove extra whitespace and newlines
  let clean = jd.replace(/\s+/g, ' ').trim();

  // Extract just the job title and key requirements (first 200 chars)
  if (clean.length > 200) {
    clean = clean.substring(0, 200) + "...";
  }

  return clean;
}

/**
 * Parses coach brief JSON and extracts readable sections
 */
function parseCoachBrief(coachBriefJson) {
  const result = {
    skillGaps: "",
    sevenDays: "",
    fourteenDays: "",
    interviewQuestions: ""
  };

  if (!coachBriefJson) return result;

  try {
    const coach = JSON.parse(coachBriefJson);

    // Skill gaps - bullet points
    if (coach.skill_gaps && Array.isArray(coach.skill_gaps)) {
      result.skillGaps = coach.skill_gaps.slice(0, 5).map(s => "• " + s).join("\n");
    }

    // Learning roadmap
    if (coach.learning_roadmap) {
      if (coach.learning_roadmap["7_days"]) {
        result.sevenDays = coach.learning_roadmap["7_days"].slice(0, 3).map(s => "• " + s).join("\n");
      }
      if (coach.learning_roadmap["14_days"]) {
        result.fourteenDays = coach.learning_roadmap["14_days"].slice(0, 3).map(s => "• " + s).join("\n");
      }
    }

    // Interview questions
    if (coach.interview_questions && Array.isArray(coach.interview_questions)) {
      result.interviewQuestions = coach.interview_questions.slice(0, 5).map((q, i) => (i+1) + ". " + q).join("\n");
    }

  } catch (e) {
    Logger.log("Error parsing coach brief: " + e.toString());
    result.skillGaps = coachBriefJson.substring(0, 500);
  }

  return result;
}

/**
 * Formats a data row with proper styling
 */
function formatDataRow(sheet, row) {
  const range = sheet.getRange(row, 1, 1, 9);

  // Wrap text for better readability
  range.setWrap(true);

  // Vertical alignment top
  range.setVerticalAlignment("top");

  // Set row height for readability
  sheet.setRowHeight(row, 100);

  // Alternate row colors
  if (row % 2 === 0) {
    range.setBackground("#f8f9fa");
  } else {
    range.setBackground("#ffffff");
  }

  // Make PDF link clickable (column 5)
  const pdfCell = sheet.getRange(row, 5);
  const pdfUrl = pdfCell.getValue();
  if (pdfUrl && pdfUrl.startsWith("http")) {
    pdfCell.setFontColor("#1a73e8");
  }
}

/**
 * Gets existing sheet or creates a new one with proper formatting
 */
function getOrCreateSheet() {
  const folder = DriveApp.getFolderById(FOLDER_ID);
  const files = folder.getFilesByName(SHEET_NAME);

  if (files.hasNext()) {
    const file = files.next();
    return SpreadsheetApp.open(file).getActiveSheet();
  }

  // Create new spreadsheet
  const spreadsheet = SpreadsheetApp.create(SHEET_NAME);
  const sheet = spreadsheet.getActiveSheet();

  // Move to target folder
  const spreadsheetFile = DriveApp.getFileById(spreadsheet.getId());
  folder.addFile(spreadsheetFile);
  DriveApp.getRootFolder().removeFile(spreadsheetFile);

  // Set up headers
  const headers = [
    "Date",
    "Company",
    "Job Description",
    "Match %",
    "PDF Link",
    "Skill Gaps",
    "7-Day Plan",
    "14-Day Plan",
    "Interview Questions"
  ];

  sheet.getRange(1, 1, 1, headers.length).setValues([headers]);

  // Format header row
  const headerRange = sheet.getRange(1, 1, 1, headers.length);
  headerRange.setBackground("#1a73e8");
  headerRange.setFontColor("white");
  headerRange.setFontWeight("bold");
  headerRange.setHorizontalAlignment("center");
  headerRange.setVerticalAlignment("middle");

  // Set column widths for readability
  sheet.setColumnWidth(1, 130);   // Date
  sheet.setColumnWidth(2, 120);   // Company
  sheet.setColumnWidth(3, 200);   // Job Description
  sheet.setColumnWidth(4, 70);    // Match %
  sheet.setColumnWidth(5, 150);   // PDF Link
  sheet.setColumnWidth(6, 200);   // Skill Gaps
  sheet.setColumnWidth(7, 200);   // 7-Day Plan
  sheet.setColumnWidth(8, 200);   // 14-Day Plan
  sheet.setColumnWidth(9, 250);   // Interview Questions

  // Freeze header row
  sheet.setFrozenRows(1);

  // Set header row height
  sheet.setRowHeight(1, 40);

  Logger.log("Created new sheet: " + spreadsheet.getUrl());

  return sheet;
}

/**
 * Run this to test the setup
 */
function testLogging() {
  const testData = {
    postData: {
      contents: JSON.stringify({
        date: new Date().toISOString(),
        company: "Google",
        jobDescription: "Senior Java Developer - Dublin. Requirements: 5+ years Java, Spring Boot, Microservices.",
        matchScore: 78,
        pdfBase64: "", // Empty for test
        pdfFilename: "TestCV.pdf",
        coachBrief: JSON.stringify({
          skill_gaps: [
            "5+ years commercial experience",
            "Microservices architecture",
            "Cloud certifications"
          ],
          learning_roadmap: {
            "7_days": [
              "Complete Spring Boot tutorial",
              "Build microservice demo",
              "Review system design"
            ],
            "14_days": [
              "Deploy to AWS",
              "Add Docker",
              "Practice interviews"
            ]
          },
          interview_questions: [
            "Explain dependency injection",
            "Design a URL shortener",
            "CI/CD experience?",
            "Challenging bug you fixed",
            "Code quality practices"
          ]
        })
      })
    }
  };

  const result = doPost(testData);
  Logger.log(result.getContent());
}

/**
 * Run this first to create/verify the sheet
 */
function createSheetManually() {
  const sheet = getOrCreateSheet();
  Logger.log("Sheet URL: " + sheet.getParent().getUrl());

  // Also create PDF subfolder
  const mainFolder = DriveApp.getFolderById(FOLDER_ID);
  const subfolders = mainFolder.getFoldersByName(PDF_SUBFOLDER_NAME);
  if (!subfolders.hasNext()) {
    mainFolder.createFolder(PDF_SUBFOLDER_NAME);
    Logger.log("Created PDF subfolder: " + PDF_SUBFOLDER_NAME);
  }
}

/**
 * Run this to clean up and reformat existing sheet
 */
function reformatExistingSheet() {
  const sheet = getOrCreateSheet();

  // Update headers
  const headers = [
    "Date",
    "Company",
    "Job Description",
    "Match %",
    "PDF Link",
    "Skill Gaps",
    "7-Day Plan",
    "14-Day Plan",
    "Interview Questions"
  ];

  sheet.getRange(1, 1, 1, headers.length).setValues([headers]);

  // Reformat header
  const headerRange = sheet.getRange(1, 1, 1, headers.length);
  headerRange.setBackground("#1a73e8");
  headerRange.setFontColor("white");
  headerRange.setFontWeight("bold");
  headerRange.setHorizontalAlignment("center");

  // Set column widths
  sheet.setColumnWidth(1, 130);
  sheet.setColumnWidth(2, 120);
  sheet.setColumnWidth(3, 200);
  sheet.setColumnWidth(4, 70);
  sheet.setColumnWidth(5, 150);
  sheet.setColumnWidth(6, 200);
  sheet.setColumnWidth(7, 200);
  sheet.setColumnWidth(8, 200);
  sheet.setColumnWidth(9, 250);

  // Format all data rows
  const lastRow = sheet.getLastRow();
  if (lastRow > 1) {
    const dataRange = sheet.getRange(2, 1, lastRow - 1, 9);
    dataRange.setWrap(true);
    dataRange.setVerticalAlignment("top");
  }

  Logger.log("Sheet reformatted!");
}
