# CV Generator - Powered by Claude AI

An ATS-optimized CV generation app using LaTeX and the Claude Sonnet 4 API.

## Author

**Pranav Prasanna Ghorpade**
- Email: ghorpade.ire@gmail.com
- LinkedIn: [linkedin.com/in/pranav-ire](https://linkedin.com/in/pranav-ire)
- GitHub: [github.com/ghorpadeire](https://github.com/ghorpadeire)

## Features

- **AI-Powered CV Generation** - Claude Sonnet 4 analyses a job description and produces a tailored CV
- **ATS-Optimized Output** - Professional LaTeX templates that pass ATS systems
- **Keyword Analysis** - Detects must-have, nice-to-have, and soft skills from JDs
- **Match Scoring** - Shows keyword coverage and recruiter fit percentages
- **Coach Brief** - Skill gaps, learning roadmap, and interview prep
- **PDF & LaTeX Downloads** - Compiled PDF and source `.tex` files
- **Profile Editor** - Edit your candidate data and system prompt from the UI; changes persist in a writable `./data/` directory

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.2 |
| Frontend | Thymeleaf, HTML5, CSS3, Vanilla JS |
| AI Engine | Claude Sonnet 4 API (Anthropic) |
| PDF Engine | YtoTech LaTeX API |
| Database | H2 (in-memory) |
| Build | Maven |

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- A Claude API key from [Anthropic](https://console.anthropic.com)

### Running Locally

```bash
cd cv-generator

# Set your API key
# Windows CMD:  set CLAUDE_API_KEY=sk-ant-api03-your-key
# PowerShell:   $env:CLAUDE_API_KEY="sk-ant-api03-your-key"
# Linux/Mac:    export CLAUDE_API_KEY=sk-ant-api03-your-key

mvn spring-boot:run
```

Open <http://localhost:8055>.

### Writable Data Directory

On first run the app copies two default files from resources into `./data/`:

| File | Purpose |
|------|---------|
| `data/candidate-data.json` | Your personal profile (name, experience, skills, etc.) |
| `data/cv-gen-system-prompt.txt` | The system prompt sent to Claude |

Edit these files via the **Profile Editor** page (`/profile`) or directly on disk.
The app always reads from `./data/` (falling back to bundled defaults if a file is missing).
Set `APP_DATA_DIR` to change the location.

## Project Structure

```
cv-generator/
├── src/main/java/com/pranav/cvgenerator/
│   ├── CvGeneratorApplication.java    # Entry point
│   ├── config/                        # Configuration classes
│   ├── controller/                    # REST & MVC controllers
│   │   ├── CvGenerationController.java
│   │   ├── DownloadController.java
│   │   ├── HomeController.java
│   │   └── ProfileController.java     # Profile editor
│   ├── model/                         # Data models & DTOs
│   ├── repository/                    # Spring Data JPA
│   ├── service/
│   │   ├── CandidateDataService.java
│   │   ├── ClaudeApiService.java
│   │   ├── CvGenerationService.java
│   │   ├── DataDirectoryService.java  # Writable data dir
│   │   └── ...
│   └── util/
│       └── PromptBuilder.java
├── src/main/resources/
│   ├── application.properties
│   ├── candidate-data.json            # Default candidate profile
│   ├── cv-gen-system-prompt.txt       # Default system prompt
│   ├── templates/                     # Thymeleaf templates
│   └── static/                        # CSS & JS
├── data/                              # Runtime-writable (git-ignored)
└── src/test/                          # Tests
```

## API Endpoints

### REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/generate` | Start CV generation |
| GET | `/api/status/{id}` | Check generation status |
| GET | `/api/result/{id}` | Get generation result |
| GET | `/api/download/{id}/pdf` | Download PDF |
| GET | `/api/download/{id}/tex` | Download LaTeX |
| GET | `/api/profile/candidate-data` | Read candidate JSON |
| PUT | `/api/profile/candidate-data` | Save candidate JSON |
| GET | `/api/profile/system-prompt` | Read system prompt |
| PUT | `/api/profile/system-prompt` | Save system prompt |

### Web Pages

| Route | Description |
|-------|-------------|
| `/` | Main input page |
| `/profile` | Profile & prompt editor |
| `/generating/{id}` | Progress page |
| `/result/{id}` | Result page |
| `/history` | Generation history |
| `/coach/{id}` | Coach brief page |

## Configuration

Key properties in `application.properties`:

```properties
server.port=${PORT:8055}

claude.api.key=${CLAUDE_API_KEY:}
claude.api.model=claude-sonnet-4-20250514
claude.api.max-tokens=8000
claude.api.timeout=120000

latex.api.url=https://latex.ytotech.com/builds/sync

app.data.dir=${APP_DATA_DIR:./data}
```

## How It Works

1. User pastes a Job Description in the web form
2. App calls the Claude API with the system prompt + candidate profile + JD
3. Claude generates a tailored LaTeX CV, keyword analysis, match scores, and a coach brief
4. App compiles the LaTeX to PDF via the YtoTech API
5. User downloads the PDF and/or LaTeX source

## License

This is a portfolio project. Feel free to use it as inspiration for your own.

---

*Created by Pranav Ghorpade*
