package tech.dubs.conversational;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Example {
    static final String openaiKey = "YOUR OPENAI KEY GOES HERE";
    static final String vectoKey = "YOUR WIKIPEDIA VECTO KEY GOES HERE";

    static final OpenAiService openai = new OpenAiService(openaiKey);

    public static String askBot(List<ChatMessage> messages){
        final ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(messages)
                .build();
        final ChatCompletionResult chatCompletion = openai.createChatCompletion(request);
        return chatCompletion.getChoices().get(0).getMessage().getContent();
    }

    public static String askBot(String question){
        return askBot(List.of(new ChatMessage("user", question)));
    }

    public static String askBotWithPrompt(String question, String prompt){
        return askBot(List.of(
                new ChatMessage("system", prompt),
                new ChatMessage("user", question)
        ));
    }

    public static String queryDatabase(String query) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        OkHttpClient client = new OkHttpClient();

        final Map<String, Object> lookupRequest = Map.of(
                "topK", 10,
                "query", query
        );

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, objectMapper.writeValueAsString(lookupRequest));
        Request request = new Request.Builder()
                .url("https://api.vecto.ai/api/v0/space/28325/lookup")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer "+vectoKey)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public static String askBotForDbQuery(String question){
        final String dbQueryPrompt = "You will be provided with a question, and your task is to generate a key phrase to search for sources from a knowledge database.";
        return askBotWithPrompt(question, dbQueryPrompt);
    }

    public static String askBotWithDatabaseAccess(String question) throws IOException {
        final String query = askBotForDbQuery(question);
        final String sources = queryDatabase(query);

        final String answerWithSourcesPrompt = "You will be provided with a question and a json that contains ground-truth information.\n" +
                "Your task is to provide one coherent answer using only the relevant information from the json. \n" +
                "You must cite your sources by providing the urls. Do not repeat a url if it is used multiple times.  If none of the information is relevant to the question say \"I don't know.\"\n" +
                "\n" +
                "Be concise.";

        final String fullQuestion = "Question: " + question + "\n\n```json\n" + sources + "\n```";
        return askBotWithPrompt(fullQuestion, answerWithSourcesPrompt);
    }

    public static void main(String[] args) throws IOException {
        final Scanner scanner = new Scanner(System.in);
        System.out.println("What is your question?");
        final String question = scanner.nextLine();

        System.out.println("Asking Bot...");
        final String simpleAnswer = askBot(question);

        System.out.println("Answer from the Bot:");
        System.out.println(simpleAnswer);

        System.out.println("Asking Bot with database access...");
        final String answerWithDb = askBotWithDatabaseAccess(question);

        System.out.println("Answer from Bot with database access:");
        System.out.println(answerWithDb);
    }
}
