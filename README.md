# CV Generator - Powered by Claude AI

An interview portfolio project that generates ATS-optimized, recruiter-ready CVs using LaTeX and Claude Sonnet 4 API.

## Author
**Pranav Prasanna Ghorpade**
- Email: pranav.ghorpade3108@gmail.com
- LinkedIn: [linkedin.com/in/pranav-ire](https://linkedin.com/in/pranav-ire)
- GitHub: [github.com/ghorpadeire](https://github.com/ghorpadeire)

## Features

- **AI-Powered CV Generation**: Uses Claude Sonnet 4 for intelligent JD analysis
- **ATS-Optimized Output**: Professional LaTeX templates that pass ATS systems
- **Keyword Analysis**: Detects must-have, nice-to-have, and soft skills from JDs
- **Match Scoring**: Shows keyword coverage and recruiter fit percentages
- **Coach Brief**: Provides skill gaps, learning roadmap, and interview prep
- **PDF & LaTeX Downloads**: Get both compiled PDF and source LaTeX files

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.x
- **Frontend**: Thymeleaf, HTML5, CSS3, Vanilla JavaScript
- **AI Engine**: Claude Sonnet 4 API (Anthropic)
- **PDF Engine**: YtoTech LaTeX API
- **Database**: H2 (in-memory)
- **Build**: Maven

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Claude API key from [Anthropic](https://console.anthropic.com)

### Running Locally

1. **Clone the repository**
   ```bash
   cd cv-generator
   ```

2. **Set your Claude API key**
   ```bash
   # Windows (CMD)
   set CLAUDE_API_KEY=sk-ant-api03-your-key-here

   # Windows (PowerShell)
   $env:CLAUDE_API_KEY="sk-ant-api03-your-key-here"

   # Linux/Mac
   export CLAUDE_API_KEY=sk-ant-api03-your-key-here
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Open in browser**
   ```
   http://localhost:8055
   ```

### Using the Desktop Launcher (Windows)

Double-click `run-cv-generator.bat` on your desktop to start the application.

## Project Structure

```
cv-generator/
├── src/main/java/com/pranav/cvgenerator/
│   ├── CvGeneratorApplication.java    # Main entry point
│   ├── config/                        # Configuration classes
│   ├── controller/                    # REST & web controllers
│   ├── service/                       # Business logic
│   ├── model/                         # Data models & DTOs
│   ├── repository/                    # Database access
│   └── util/                          # Utility classes
├── src/main/resources/
│   ├── application.properties         # App configuration
│   ├── candidate-data.json            # Candidate profile
│   ├── cv-gen-system-prompt.txt       # Claude system prompt
│   ├── templates/                     # Thymeleaf HTML templates
│   └── static/                        # CSS & JavaScript
└── src/test/                          # Unit tests
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

### Web Pages

| Route | Description |
|-------|-------------|
| `/` | Main input page |
| `/generating/{id}` | Progress page |
| `/result/{id}` | Result page |
| `/history` | Generation history |
| `/coach/{id}` | Coach brief page |

## Configuration

Key configuration options in `application.properties`:

```properties
# Server port
server.port=8055

# Claude API settings
claude.api.key=${CLAUDE_API_KEY}
claude.api.model=claude-sonnet-4-20250514
claude.api.max-tokens=8000

# LaTeX API settings
latex.api.url=https://latex.ytotech.com/builds/sync
latex.api.timeout=60000
```

## How It Works

1. **User pastes Job Description** in the web form
2. **App calls Claude API** with:
   - CV_GEN system prompt (defines CV generation behavior)
   - Candidate profile data (from JSON)
   - Job description (from user)
3. **Claude analyzes JD** and generates:
   - Tailored LaTeX CV
   - Keyword analysis
   - Match scores
   - Coach brief
4. **App compiles LaTeX to PDF** using YtoTech API
5. **User downloads** the PDF and/or LaTeX source

## Interview Talking Points

This project demonstrates:

1. **Java Best Practices**: Clean code, design patterns, proper OOP
2. **Spring Boot Expertise**: Dependency injection, REST APIs, JPA
3. **API Integration**: External API calls, error handling, retries
4. **Frontend Skills**: Responsive design, AJAX, user experience
5. **DevOps Awareness**: Docker, environment variables, deployment
6. **Testing**: Unit tests, integration tests, MockMvc

## License

This is an interview portfolio project. Feel free to use it as inspiration for your own projects.

---

*Created with care by Pranav Ghorpade | 2025*
# CvGenTool
