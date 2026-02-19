package com.metar;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Application-scoped service that orchestrates METAR retrieval and AI decoding.
 * <p>
 * On each request it:
 * <ol>
 *   <li>Fetches the raw METAR string from the Aviation Weather Center API via
 *       {@link AviationWeatherClient}.</li>
 *   <li>Sends the raw METAR to the OpenAI GPT-4o model with a structured prompt
 *       asking for a plain-English weather summary.</li>
 *   <li>Returns the result wrapped in a {@link WeatherReportDto}.</li>
 * </ol>
 * The OpenAI API key is read from the {@code OPENAI_API_KEY} environment variable.
 */
@ApplicationScoped
public class MetarService {

    private static final Logger LOG = Logger.getLogger(MetarService.class);

    /** REST client for fetching raw METAR data from aviationweather.gov. */
    @Inject
    @RestClient
    AviationWeatherClient weatherClient;

    /** OpenAI API key, injected from the {@code OPENAI_API_KEY} environment variable. */
    @ConfigProperty(name = "openai.api.key")
    String apiKey;

    /** Lazily initialised OpenAI client, created once at startup. */
    private OpenAIClient openAiClient;

    /** Builds the OpenAI client after dependency injection is complete. */
    @PostConstruct
    void init() {
        openAiClient = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * Fetches and decodes the METAR report for the given airport code.
     *
     * @param airportCode a valid ICAO airport identifier (e.g. {@code KJFK})
     * @return a {@link WeatherReportDto} containing the raw METAR and decoded summary,
     *         or an error DTO if the weather service or AI decoding step fails
     */
    public WeatherReportDto getWeatherReport(String airportCode) {
        String code = airportCode.trim().toUpperCase();

        // 1. Fetch raw METAR from Aviation Weather API
        String rawMetar;
        try {
            rawMetar = weatherClient.getMetar(code).trim();
        } catch (Exception e) {
            LOG.errorf("Failed to fetch METAR for %s: %s", code, e.getMessage());
            return WeatherReportDto.error(code,
                    "Could not reach the aviation weather service. Please try again later.");
        }

        if (rawMetar.isEmpty()) {
            return WeatherReportDto.error(code,
                    "No METAR data found for airport code '" + code +
                    "'. Please verify this is a valid ICAO code (4 letters, e.g. KJFK, EGLL, YSSY).");
        }

        // 2. Decode with OpenAI
        String friendlyReport;
        try {
            friendlyReport = decodeMeta(rawMetar);
        } catch (Exception e) {
            LOG.error("OpenAI decoding failed for " + code, e);
            return WeatherReportDto.error(code,
                    "Retrieved the METAR but could not decode it (" +
                    e.getClass().getSimpleName() + ": " + e.getMessage() +
                    "). Raw data: " + rawMetar);
        }

        return new WeatherReportDto(code, rawMetar, friendlyReport);
    }

    /**
     * Calls the OpenAI Chat Completions API to translate a raw METAR into
     * a friendly, plain-English weather summary.
     *
     * @param rawMetar the raw METAR string to decode
     * @return a human-readable weather description
     */
    private String decodeMeta(String rawMetar) {
        String prompt = """
                You are a friendly aviation weather decoder. I have a raw METAR report from an airport,
                and I need you to translate it into plain English that anyone — including people with no
                aviation knowledge — can easily understand.

                Decode the METAR below into a conversational, friendly weather summary. Cover:
                - Overall sky conditions (clear, partly cloudy, overcast, stormy, etc.)
                - Temperature in both Celsius and Fahrenheit
                - Wind: speed (in mph and km/h) and direction in plain terms like "from the north"
                  or "calm". If gusting, mention it.
                - Visibility (in miles and km, note if reduced by fog/haze/rain)
                - Any precipitation or significant weather (rain, snow, thunderstorms, fog, etc.)
                - Dew point and whether it feels humid
                - Barometric pressure in inHg

                Start with a one-sentence overall summary (e.g. "It's a clear, cool morning at JFK
                with light winds."), then give the detailed breakdown. Keep the tone friendly and
                approachable, like you're telling a friend what the weather is like before a trip.

                METAR: %s
                """.formatted(rawMetar);

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_4O)
                .maxCompletionTokens(1024)
                .addUserMessage(prompt)
                .build();

        ChatCompletion response = openAiClient.chat().completions().create(params);

        return response.choices().stream()
                .findFirst()
                .flatMap(choice -> choice.message().content())
                .orElse("Unable to decode the METAR. Raw data: " + rawMetar);
    }
}
