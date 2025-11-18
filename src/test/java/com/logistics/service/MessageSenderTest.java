package com.logistics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageSenderTest {

    @Mock
    private TelegramLongPollingBot bot;

    @InjectMocks
    private MessageSender messageSender;

    private final Long CHAT_ID = 123456789L;
    private final String TEST_MESSAGE = "Test message";

    @BeforeEach
    void setUp() {
        // Устанавливаем бота через статический метод
        MessageSender.setBot(bot);
    }

    @Test
    void testSetBot() {
        // Arrange
        TelegramLongPollingBot newBot = mock(TelegramLongPollingBot.class);

        // Act
        MessageSender.setBot(newBot);

        // Assert - проверяем, что бот установлен (косвенно через вызовы)
        assertDoesNotThrow(() -> messageSender.sendMessage(CHAT_ID, TEST_MESSAGE));
    }

    @Test
    void testSendMessage_WithoutMenu() throws TelegramApiException {
        // Act
        messageSender.sendMessage(CHAT_ID, TEST_MESSAGE);

        // Assert
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage sentMessage = messageCaptor.getValue();
        assertEquals(String.valueOf(CHAT_ID), sentMessage.getChatId());
        assertEquals(TEST_MESSAGE, sentMessage.getText());
        assertNull(sentMessage.getReplyMarkup());
    }

    @Test
    void testSendMessage_WithMenu() throws TelegramApiException {
        // Act
        messageSender.sendMessage(CHAT_ID, TEST_MESSAGE, true);

        // Assert
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage sentMessage = messageCaptor.getValue();
        assertEquals(String.valueOf(CHAT_ID), sentMessage.getChatId());
        assertEquals(TEST_MESSAGE, sentMessage.getText());

        assertNotNull(sentMessage.getReplyMarkup());
        assertTrue(sentMessage.getReplyMarkup() instanceof ReplyKeyboardMarkup);

        ReplyKeyboardMarkup keyboard = (ReplyKeyboardMarkup) sentMessage.getReplyMarkup();
        assertTrue(keyboard.getResizeKeyboard());
        assertFalse(keyboard.getOneTimeKeyboard());
    }

    @Test
    void testSendMessage_WithCancel() throws TelegramApiException {
        // Act
        messageSender.sendMessageWithCancel(CHAT_ID, TEST_MESSAGE);

        // Assert
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage sentMessage = messageCaptor.getValue();
        assertEquals(String.valueOf(CHAT_ID), sentMessage.getChatId());
        assertEquals(TEST_MESSAGE, sentMessage.getText());

        assertNotNull(sentMessage.getReplyMarkup());
        assertTrue(sentMessage.getReplyMarkup() instanceof ReplyKeyboardMarkup);

        ReplyKeyboardMarkup keyboard = (ReplyKeyboardMarkup) sentMessage.getReplyMarkup();
        assertTrue(keyboard.getResizeKeyboard());
        assertTrue(keyboard.getOneTimeKeyboard());
    }

    @Test
    void testSendMessage_TelegramApiException() throws TelegramApiException {
        // Arrange
        doThrow(new TelegramApiException("API error")).when(bot).execute(any(SendMessage.class));

        // Act & Assert - не должно бросать исключение, только логировать
        assertDoesNotThrow(() -> messageSender.sendMessage(CHAT_ID, TEST_MESSAGE));

        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void testSendMessageWithCancel_TelegramApiException() throws TelegramApiException {
        // Arrange
        doThrow(new TelegramApiException("API error")).when(bot).execute(any(SendMessage.class));

        // Act & Assert - не должно бросать исключение, только логировать
        assertDoesNotThrow(() -> messageSender.sendMessageWithCancel(CHAT_ID, TEST_MESSAGE));

        verify(bot).execute(any(SendMessage.class));
    }

    @Test
    void testSendMessageWithCancel_KeyboardStructure() throws TelegramApiException {
        // Act
        messageSender.sendMessageWithCancel(CHAT_ID, TEST_MESSAGE);

        // Assert
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage sentMessage = messageCaptor.getValue();
        ReplyKeyboardMarkup keyboard = (ReplyKeyboardMarkup) sentMessage.getReplyMarkup();
        List<KeyboardRow> keyboardRows = keyboard.getKeyboard();

        assertEquals(1, keyboardRows.size());

        KeyboardRow row = keyboardRows.get(0);
        assertEquals(1, row.size());
        assertEquals("❌ Отмена", row.get(0).getText());
    }

    @Test
    void testSendMessage_EmptyMessage() throws TelegramApiException {
        // Act
        messageSender.sendMessage(CHAT_ID, "");

        // Assert
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage sentMessage = messageCaptor.getValue();
        assertEquals("", sentMessage.getText());
    }

    @Test
    void testSendMessage_VeryLongMessage() throws TelegramApiException {
        // Arrange
        String longMessage = "A".repeat(4096); // Максимальная длина сообщения в Telegram

        // Act
        messageSender.sendMessage(CHAT_ID, longMessage);

        // Assert
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage sentMessage = messageCaptor.getValue();
        assertEquals(longMessage, sentMessage.getText());
    }

    @Test
    void testMultipleSendMessages() throws TelegramApiException {
        // Act
        messageSender.sendMessage(CHAT_ID, "Message 1");
        messageSender.sendMessage(CHAT_ID, "Message 2");
        messageSender.sendMessage(CHAT_ID, "Message 3", true);

        // Assert
        verify(bot, times(3)).execute(any(SendMessage.class));
    }

    @Test
    void testSendMessage_DifferentChatIds() throws TelegramApiException {
        // Arrange
        Long chatId1 = 111111111L;
        Long chatId2 = 222222222L;

        // Act
        messageSender.sendMessage(chatId1, "Message for user 1");
        messageSender.sendMessage(chatId2, "Message for user 2");

        // Assert
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot, times(2)).execute(messageCaptor.capture());

        List<SendMessage> allMessages = messageCaptor.getAllValues();
        assertEquals(String.valueOf(chatId1), allMessages.get(0).getChatId());
        assertEquals(String.valueOf(chatId2), allMessages.get(1).getChatId());
    }

    @Test
    void testSendMessageWithCancel_Properties() throws TelegramApiException {
        // Act
        messageSender.sendMessageWithCancel(CHAT_ID, TEST_MESSAGE);

        // Assert
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage sentMessage = messageCaptor.getValue();
        ReplyKeyboardMarkup keyboard = (ReplyKeyboardMarkup) sentMessage.getReplyMarkup();

        assertTrue(keyboard.getSelective());
        assertTrue(keyboard.getResizeKeyboard());
        assertTrue(keyboard.getOneTimeKeyboard());
    }

    @Test
    void testMainMenuKeyboard_Properties() throws TelegramApiException {
        // Act
        messageSender.sendMessage(CHAT_ID, TEST_MESSAGE, true);

        // Assert
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage sentMessage = messageCaptor.getValue();
        ReplyKeyboardMarkup keyboard = (ReplyKeyboardMarkup) sentMessage.getReplyMarkup();

        assertTrue(keyboard.getSelective());
        assertTrue(keyboard.getResizeKeyboard());
        assertFalse(keyboard.getOneTimeKeyboard());
    }

    @Test
    void testSendMessage_WithoutMenu_NoKeyboard() throws TelegramApiException {
        // Act
        messageSender.sendMessage(CHAT_ID, TEST_MESSAGE, false);

        // Assert
        ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(bot).execute(messageCaptor.capture());

        SendMessage sentMessage = messageCaptor.getValue();
        assertNull(sentMessage.getReplyMarkup());
    }

    @Test
    void testStaticBotInitialization() {
        // Arrange
        TelegramLongPollingBot newBot = mock(TelegramLongPollingBot.class);

        // Act
        MessageSender.setBot(newBot);
        MessageSender newMessageSender = new MessageSender();

        // Assert - проверяем, что новый экземпляр использует установленного бота
        assertDoesNotThrow(() -> newMessageSender.sendMessage(CHAT_ID, TEST_MESSAGE));
    }
}