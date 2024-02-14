import com.cjcrafter.openai.OpenAI;
import com.cjcrafter.openai.chat.ChatMessage;
import com.cjcrafter.openai.chat.ChatRequest;
import com.cjcrafter.openai.chat.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.util.List;

public class Bot extends TelegramLongPollingBot {
    private static final String START_COMMAND = "/start";
    private static final String HELP_COMMAND = "/help";
    private final OpenAI openai = new OpenAI(Tokens.CHAT_GPT);
    private String userMessage;

    @Override
    public String getBotUsername() {
        return Helpers.TELEGRAM_BOT_NAME;
    }

    @Override
    public String getBotToken() {
        return Tokens.TELEGRAM;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            try {
                handleIncomingMessage(update);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleIncomingMessage(Update update) throws Exception {
        String userMessage = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();

        if (userMessage.equals(START_COMMAND) || userMessage.equals(HELP_COMMAND)) {
            sendMessage(chatId, update.getMessage().getMessageId(), userMessage.equals(START_COMMAND) ?
                    Texts.GREETINGS.formatted(update.getMessage().getFrom().getFirstName()) : Texts.HELP);
        } else {
            sendMessage(chatId, update.getMessage().getMessageId(), generateChatGPTResponse(userMessage));
        }
    }






    private void sendMessage(String chatId, Integer replyToMessageId, String text) throws TelegramApiException {
        SendMessage message = createMessage(chatId, replyToMessageId, text);
        execute(message);
    }

    private byte[] generateChatGPTResponse(String userMessage) {
        this.userMessage = userMessage;
        try {
            List<ChatMessage> messages = List.of(ChatMessage.toSystemMessage(userMessage), ChatMessage.toUserMessage(userMessage));
            ChatRequest request = ChatRequest.builder().model("gpt-3.5-turbo").messages(messages).build();
            ChatResponse completions = openai.createChatCompletion(request);

            // ObjectMapper yaratish
            ObjectMapper mapper = new ObjectMapper();

            // ChatResponse obyektini JSON formatiga o'zgartirish
            String json = mapper.writeValueAsString(completions);
            return json.getBytes();
        } catch (Exception e) {
            e.printStackTrace();
            return "Sorry, couldn't generate a response at the moment.".getBytes();
        }
    }

    private void sendMessage(String chatId, Integer messageId, byte[] bytes) {
        SendDocument document = new SendDocument();
        document.setChatId(chatId);
        document.setReplyToMessageId(messageId);
        document.setDocument(new InputFile (new ByteArrayInputStream (bytes), "filename"));
        try {
            execute(document);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }




    private SendMessage createMessage(String chatId, Integer replyToMessageId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setReplyToMessageId(replyToMessageId);
        message.setText(text);

        return message;
    }
}
