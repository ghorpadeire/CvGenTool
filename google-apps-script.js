/**
 * Google Apps Script for CV Generator History Logging
 *
 * SETUP INSTRUCTIONS:
 *
 * 1. Go to: https://script.google.com
 * 2. Click "New Project"
 * 3. Delete the default code and paste this entire file
 * 4. Click "Deploy" > "New deployment"
 * 5. Select type: "Web app"
 * 6. Settings:
 *    - Description: "CV Generator Logger"
 *    - Execute as: "Me"
 *    - Who has access: "Anyone"
 * 7. Click "Deploy"
 * 8. Copy the Web App URL (looks like: https://script.google.com/macros/s/xxxxx/exec)
 * 9. Add to Render environment variables:
 *    - Key: GOOGLE_SHEETS_WEBHOOK
 *    - Value: <paste the URL>
 *
 * This script will:
 * - Create a Google Sheet named "CV Generation History" in your specified folder
 * - Log each CV generation with: Date, Company, JD, Match Score, Coach Brief
 */

// Configuration
const SHEET_NAME = "CV Generation History";
const FOLDER_ID = "1w1OVjXeIy0S8Za-W5DSxlrJuzZpCsepf"; // Your "Claude code Cv gen2" folder

/**
 * Handles POST requests from the CV Generator app
 */
function doPost(e) {
  try {
    const data = JSON.parse(e.postData.contents);

    // Get or create the spreadsheet
    const sheet = getOrCreateSheet();

    // Append the new row
    sheet.appendRow([
      data.date || new Date().toISOString(),
      data.company || "Unknown",
      data.jobDescription || "",
      data.matchScore || 0,
      data.pdfUrl || "",
      data.coachBrief || ""
    ]);

    return ContentService
      .createTextOutput(JSON.stringify({ success: true }))
      .setMimeType(ContentService.MimeType.JSON);

  } catch (error) {
    return ContentService
      .createTextOutput(JSON.stringify({ success: false, error: error.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
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
 * Gets existing sheet or creates a new one in the specified folder
 */
function getOrCreateSheet() {
  // Get the target folder
  const folder = DriveApp.getFolderById(FOLDER_ID);

  // Try to find existing spreadsheet in the folder
  const files = folder.getFilesByName(SHEET_NAME);

  if (files.hasNext()) {
    const file = files.next();
    return SpreadsheetApp.open(file).getActiveSheet();
  }

  // Create new spreadsheet
  const spreadsheet = SpreadsheetApp.create(SHEET_NAME);
  const sheet = spreadsheet.getActiveSheet();

  // Move the spreadsheet to the target folder
  const spreadsheetFile = DriveApp.getFileById(spreadsheet.getId());
  folder.addFile(spreadsheetFile);
  DriveApp.getRootFolder().removeFile(spreadsheetFile); // Remove from root

  // Add headers
  const headers = [
    "Date",
    "Company",
    "Job Description (Summary)",
    "Match Score %",
    "PDF Link",
    "Coach Brief / Interview Prep"
  ];

  sheet.getRange(1, 1, 1, headers.length).setValues([headers]);

  // Format header row
  sheet.getRange(1, 1, 1, headers.length)
    .setBackground("#4285f4")
    .setFontColor("white")
    .setFontWeight("bold");

  // Set column widths
  sheet.setColumnWidth(1, 150);  // Date
  sheet.setColumnWidth(2, 150);  // Company
  sheet.setColumnWidth(3, 300);  // JD
  sheet.setColumnWidth(4, 100);  // Match Score
  sheet.setColumnWidth(5, 200);  // PDF Link
  sheet.setColumnWidth(6, 400);  // Coach Brief

  // Freeze header row
  sheet.setFrozenRows(1);

  Logger.log("Created new sheet in folder: " + spreadsheet.getUrl());

  return sheet;
}

/**
 * Test function - run this to verify the script works
 * Click "Run" button in Apps Script editor to test
 */
function testLogging() {
  const testData = {
    postData: {
      contents: JSON.stringify({
        date: new Date().toISOString(),
        company: "Test Company",
        jobDescription: "This is a test job description for a Java Developer role...",
        matchScore: 85,
        pdfUrl: "https://example.com/test.pdf",
        coachBrief: "Focus on: Spring Boot, REST APIs, System Design"
      })
    }
  };

  const result = doPost(testData);
  Logger.log(result.getContent());
  Logger.log("Check your folder for the new sheet!");
}

/**
 * Run this function first to create the sheet manually
 * This helps verify the folder access is working
 */
function createSheetManually() {
  const sheet = getOrCreateSheet();
  Logger.log("Sheet created/found at: " + sheet.getParent().getUrl());
}
