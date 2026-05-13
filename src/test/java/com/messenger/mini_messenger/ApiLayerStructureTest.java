package com.messenger.mini_messenger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ApiLayerStructureTest {

    @Test
    void requiredControllersExist() {
        assertClassExists("com.messenger.mini_messenger.controller.AuthController");
        assertClassExists("com.messenger.mini_messenger.controller.UserController");
        assertClassExists("com.messenger.mini_messenger.controller.ConversationController");
        assertClassExists("com.messenger.mini_messenger.controller.MessageController");
    }

    @Test
    void requiredServicesAndImplementationsExist() {
        assertClassExists("com.messenger.mini_messenger.service.AuthService");
        assertClassExists("com.messenger.mini_messenger.service.UserService");
        assertClassExists("com.messenger.mini_messenger.service.ConversationService");
        assertClassExists("com.messenger.mini_messenger.service.MessageService");
        assertClassExists("com.messenger.mini_messenger.service.impl.AuthServiceImpl");
        assertClassExists("com.messenger.mini_messenger.service.impl.UserServiceImpl");
        assertClassExists("com.messenger.mini_messenger.service.impl.ConversationServiceImpl");
        assertClassExists("com.messenger.mini_messenger.service.impl.MessageServiceImpl");
    }

    private static void assertClassExists(String className) {
        assertDoesNotThrow(() -> Class.forName(className), className + " is required");
    }
}
