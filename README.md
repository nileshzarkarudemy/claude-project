# METAR Weather Reader

A Quarkus-based REST API and web UI that fetches live aviation weather reports (METARs) and translates them from cryptic aviation shorthand into plain-English summaries using OpenAI GPT-4o.

## What is a METAR?

A METAR (Meteorological Aerodrome Report) is the standard format used worldwide to report current weather conditions at airports. They look like this:

```
KJFK 191551Z 27015KT 10SM FEW055 BKN250 12/M01 A2992 RMK AO2
```

This app decodes that into something like:

> *"It's a cool, mostly clear afternoon at JFK with a moderate breeze from the west at about 17 mph. Visibility is excellent at 10 miles, with a few clouds at 5,500 ft and a broken layer high up at 25,000 ft. The temperature is a brisk 12°C (54°F) with a dew point well below freezing, so the air feels very dry. Barometric pressure is 29.92 inHg — standard."*

## How It Works

```
Browser / API client
      │
      ▼
MetarResource  (JAX-RS — GET /api/weather?airport=KJFK)
      │
      ▼
MetarService
  ├── AviationWeatherClient ──► aviationweather.gov  (fetch raw METAR)
  └── OpenAI GPT-4o          ──► plain-English summary
      │
      ▼
WeatherReportDto  (JSON response)
```

## Prerequisites

- **Java 17+**
- **Maven 3.9+**
- An **OpenAI API key** — get one at [platform.openai.com](https://platform.openai.com)

## Setup

1. **Clone the repository**

   ```bash
   git clone https://github.com/your-username/metar-reader.git
   cd metar-reader
   ```

2. **Set your OpenAI API key**

   ```bash
   export OPENAI_API_KEY=sk-...
   ```

   Or create a `.env` file in the project root (never commit this file):

   ```
   OPENAI_API_KEY=sk-...
   ```

## Running the App

**Development mode** (live reload enabled):

```bash
./mvnw quarkus:dev
```

**Production build and run:**

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

The server starts on **http://localhost:8080**.

## Usage

### Web UI

Open **http://localhost:8080** in your browser. Type any ICAO airport code or click one of the quick-pick chips to get an instant weather report.

### REST API

```
GET /api/weather?airport={ICAO_CODE}
```

**Example:**

```bash
curl "http://localhost:8080/api/weather?airport=KJFK"
```

**Success response (`200 OK`):**

```json
{
  "airportCode": "KJFK",
  "rawMetar": "KJFK 191551Z 27015KT 10SM FEW055 BKN250 12/M01 A2992 RMK AO2",
  "friendlyReport": "It's a cool, mostly clear afternoon at JFK with a moderate westerly breeze...",
  "error": null
}
```

**Error response (`400` / `404`):**

```json
{
  "airportCode": "XXXX",
  "rawMetar": null,
  "friendlyReport": null,
  "error": "No METAR data found for airport code 'XXXX'. Please verify this is a valid ICAO code."
}
```

### ICAO Code Format

ICAO codes are 4-letter identifiers used globally:

| Code | Airport |
|------|---------|
| `KJFK` | New York John F. Kennedy |
| `KLAX` | Los Angeles International |
| `EGLL` | London Heathrow |
| `YSSY` | Sydney Kingsford Smith |
| `RJTT` | Tokyo Haneda |
| `EDDF` | Frankfurt |

## Configuration

All configuration lives in `src/main/resources/application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `openai.api.key` | `${OPENAI_API_KEY}` | OpenAI API key (from env var) |
| `quarkus.rest-client.aviation-weather.url` | `https://aviationweather.gov` | Weather data source |
| `quarkus.http.port` | `8080` | HTTP server port |

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Framework | [Quarkus](https://quarkus.io) 3.17.7 |
| Language | Java 17 |
| AI Model | OpenAI GPT-4o via [openai-java](https://github.com/openai/openai-java) 4.21.0 |
| Weather Data | [Aviation Weather Center API](https://aviationweather.gov) |
| REST | JAX-RS (Quarkus REST) + MicroProfile REST Client |

## License

MIT
