# 🚀 AI Blog Studio

A **fully autonomous** AI-powered blog creation and Medium publishing system built with **Spring Boot 3.3** + **LangChain4j** + **Playwright**.

> Enter a topic → System researches, writes, optimizes, humanizes, and publishes — **zero human intervention**.

---

## ⚡ Architecture

```
User enters topic
       │
       ▼
┌──────────────────┐
│ ResearcherAgent   │ ── Uses Tavily to search the web for latest data
└────────┬─────────┘
         ▼
┌──────────────────┐
│ WriterAgent       │ ── Writes 1200-1500 word blog (Seth Godin + Tim Ferriss style)
└────────┬─────────┘
         ▼
┌──────────────────┐
│ SeoAgent          │ ── Optimizes keywords, meta description, tags
└────────┬─────────┘
         ▼
┌──────────────────┐
│ EditorAgent       │ ── Removes AI smell, humanizes, polishes
└────────┬─────────┘
         ▼
┌──────────────────┐
│ PublisherAgent    │ ── Publishes to Medium via Playwright
└──────────────────┘
```

---

## 📋 Prerequisites

- **Java 21+** ([Download](https://adoptium.net/))
- **Maven 3.9+** ([Download](https://maven.apache.org/))
- **OpenAI API Key** ([Get one](https://platform.openai.com/api-keys))
- **Tavily API Key** ([Get one](https://tavily.com/))
- **Medium Account** (email + password)

---

## 🛠️ Setup & Run

### 1. Clone & Navigate

```bash
cd medium-ai-autonomous-publisher
```

### 2. Set Environment Variables

**Windows (PowerShell):**
```powershell
$env:OPENAI_API_KEY = "sk-your-openai-key"
$env:TAVILY_API_KEY = "tvly-your-tavily-key"
$env:MEDIUM_EMAIL = "your@email.com"
$env:MEDIUM_PASSWORD = "your-password"
$env:MEDIUM_AUTO_PUBLISH = "false"
```

**Linux/Mac:**
```bash
export OPENAI_API_KEY="sk-your-openai-key"
export TAVILY_API_KEY="tvly-your-tavily-key"
export MEDIUM_EMAIL="your@email.com"
export MEDIUM_PASSWORD="your-password"
export MEDIUM_AUTO_PUBLISH="false"
```

### 3. Install Playwright Browsers

```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

### 4. Build & Run

```bash
# Compile
mvn clean compile

# Run the application
mvn spring-boot:run
```

### 5. Open Dashboard

Navigate to **http://localhost:8080** in your browser.

---

## 🎮 Usage

1. Open the dashboard at `http://localhost:8080`
2. Enter a topic (e.g., *"The Future of AI in Healthcare"*)
3. Click **"Generate & Publish"**
4. Wait 2-3 minutes while the 5-agent pipeline runs
5. View the generated blog in the preview section
6. Check your Medium drafts (if `MEDIUM_AUTO_PUBLISH=false`) or published posts

---

## ⚙️ Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `OPENAI_API_KEY` | — | Your OpenAI API key |
| `TAVILY_API_KEY` | — | Your Tavily search API key |
| `MEDIUM_EMAIL` | — | Medium login email |
| `MEDIUM_PASSWORD` | — | Medium login password |
| `MEDIUM_AUTO_PUBLISH` | `false` | Set to `true` to publish immediately |
| `playwright.headless` | `false` | Set to `true` for headless browser |

---

## 🗄️ Database

Uses **H2** file-based database by default. Access the H2 console at:

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/medium-publisher-db`
- Username: `sa`
- Password: *(empty)*

To switch to **PostgreSQL**, update `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/medium_publisher
    driver-class-name: org.postgresql.Driver
    username: postgres
    password: your-password
```

---

## 📁 Project Structure

```
src/main/java/com/sumit/mediumaipublisher/
├── MediumAiPublisherApplication.java
├── config/
│   ├── LangChainConfig.java          # AI agent + Tavily beans
│   └── PlaywrightConfig.java         # Browser automation config
├── model/
│   └── Blog.java                     # JPA entity
├── repository/
│   └── BlogRepository.java           # Spring Data repo
├── tools/
│   ├── TavilySearchTool.java         # Web search via Tavily
│   └── MediumPublisherTool.java      # Playwright Medium automation
├── agents/
│   ├── ResearcherAgent.java          # Researches topics
│   ├── WriterAgent.java              # Writes compelling blogs
│   ├── SeoAgent.java                 # SEO optimization
│   ├── EditorAgent.java              # Humanizes content
│   └── PublisherAgent.java           # Publishes to Medium
├── service/
│   └── BlogOrchestratorService.java  # Chains all 5 agents
├── controller/
│   └── BlogController.java           # Web dashboard
└── dto/
    └── BlogResponse.java             # API response DTO
```

---

## 🔧 Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 3.3.7 | Application framework |
| LangChain4j | 1.0.0-beta4 | AI agent framework |
| OpenAI GPT-4o | Latest | Blog writing & editing |
| Tavily | Latest | Web search for research |
| Playwright | 1.49 | Medium browser automation |
| H2 Database | Latest | Data persistence |
| Thymeleaf | Latest | Dashboard UI |
| Lombok | Latest | Boilerplate reduction |

---

## 📝 License

MIT License — use freely for personal and commercial projects.
