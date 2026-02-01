/**
 * Google Apps Script for CV Generator History Logging
 *
 * Creates a clean, well-formatted spreadsheet for tracking CV generations
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
    const sheet = getOrCreateSheet();

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
      coachData.skillGaps,
      coachData.sevenDays,
      coachData.fourteenDays,
      coachData.interviewQuestions
    ]);

    // Auto-resize and format the new row
    const lastRow = sheet.getLastRow();
    formatDataRow(sheet, lastRow);

    return ContentService
      .createTextOutput(JSON.stringify({ success: true }))
      .setMimeType(ContentService.MimeType.JSON);

  } catch (error) {
    Logger.log("Error: " + error.toString());
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
    // If JSON parsing fails, just show raw text (truncated)
    result.skillGaps = coachBriefJson.substring(0, 500);
  }

  return result;
}

/**
 * Formats a data row with proper styling
 */
function formatDataRow(sheet, row) {
  const range = sheet.getRange(row, 1, 1, 8);

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
    "Skill Gaps",
    "7-Day Learning Plan",
    "14-Day Learning Plan",
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
  sheet.setColumnWidth(3, 250);   // Job Description
  sheet.setColumnWidth(4, 70);    // Match %
  sheet.setColumnWidth(5, 250);   // Skill Gaps
  sheet.setColumnWidth(6, 250);   // 7-Day Plan
  sheet.setColumnWidth(7, 250);   // 14-Day Plan
  sheet.setColumnWidth(8, 300);   // Interview Questions

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
        jobDescription: "Senior Java Developer - Dublin. Requirements: 5+ years Java, Spring Boot, Microservices, Cloud experience preferred.",
        matchScore: 78,
        coachBrief: JSON.stringify({
          skill_gaps: [
            "5+ years commercial experience (you have project experience)",
            "Microservices architecture hands-on",
            "Cloud certifications"
          ],
          learning_roadmap: {
            "7_days": [
              "Complete Spring Boot REST API tutorial",
              "Build a microservice demo project",
              "Review system design basics"
            ],
            "14_days": [
              "Deploy project to AWS",
              "Add Docker containerization",
              "Practice coding interviews"
            ]
          },
          interview_questions: [
            "Explain dependency injection in Spring",
            "How would you design a URL shortener?",
            "What is your experience with CI/CD?",
            "Describe a challenging bug you fixed",
            "How do you ensure code quality?"
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
    "Skill Gaps",
    "7-Day Learning Plan",
    "14-Day Learning Plan",
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
  sheet.setColumnWidth(3, 250);
  sheet.setColumnWidth(4, 70);
  sheet.setColumnWidth(5, 250);
  sheet.setColumnWidth(6, 250);
  sheet.setColumnWidth(7, 250);
  sheet.setColumnWidth(8, 300);

  // Format all data rows
  const lastRow = sheet.getLastRow();
  if (lastRow > 1) {
    const dataRange = sheet.getRange(2, 1, lastRow - 1, 8);
    dataRange.setWrap(true);
    dataRange.setVerticalAlignment("top");
  }

  Logger.log("Sheet reformatted!");
}
