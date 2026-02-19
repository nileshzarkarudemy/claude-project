package com.metar;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Sends a raw METAR string to the OpenAI Chat Completions API and returns
 * a plain-English weather summary.
 */
@ApplicationScoped
public class AiDecoder {

    @ConfigProperty(name = "openai.api.key")
    String apiKey;

    @ConfigProperty(name = "openai.base.url", defaultValue = "https://api.openai.com")
    String baseUrl;

    private OpenAIClient client;

    @PostConstruct
    void init() {
        client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Decodes a raw METAR into a friendly, plain-English weather summary.
     *
     * @param rawMetar the raw METAR string
     * @return a human-readable weather description
     */
    public String decode(String rawMetar) {
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

        ChatCompletion response = client.chat().completions().create(params);

        return response.choices().stream()
                .findFirst()
                .flatMap(choice -> choice.message().content())
                .orElse("Unable to decode the METAR. Raw data: " + rawMetar);
    }
}
