package com.messenger.mini_messenger;

import com.messenger.mini_messenger.controller.MessageController;
import com.messenger.mini_messenger.controller.SessionController;
import com.messenger.mini_messenger.controller.UserController;
import com.messenger.mini_messenger.dto.request.UpdateMessageRequest;
import com.messenger.mini_messenger.dto.request.UpdateUserRequest;
import com.messenger.mini_messenger.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiLayerStructureTest {

    @Test
    void requiredControllersExist() {
        assertClassExists("com.messenger.mini_messenger.controller.AuthController");
        assertClassExists("com.messenger.mini_messenger.controller.UserController");
        assertClassExists("com.messenger.mini_messenger.controller.SessionController");
        assertClassExists("com.messenger.mini_messenger.controller.ConversationController");
        assertClassExists("com.messenger.mini_messenger.controller.MessageController");
    }

    @Test
    void requiredServicesAndImplementationsExist() {
        assertClassExists("com.messenger.mini_messenger.service.AuthService");
        assertClassExists("com.messenger.mini_messenger.service.UserService");
        assertClassExists("com.messenger.mini_messenger.service.SessionService");
        assertClassExists("com.messenger.mini_messenger.service.ConversationService");
        assertClassExists("com.messenger.mini_messenger.service.MessageService");
        assertClassExists("com.messenger.mini_messenger.service.impl.AuthServiceImpl");
        assertClassExists("com.messenger.mini_messenger.service.impl.UserServiceImpl");
        assertClassExists("com.messenger.mini_messenger.service.impl.SessionServiceImpl");
        assertClassExists("com.messenger.mini_messenger.service.impl.ConversationServiceImpl");
        assertClassExists("com.messenger.mini_messenger.service.impl.MessageServiceImpl");
    }

    @Test
    void userControllerExposesUpdateMeEndpoint() throws NoSuchMethodException {
        var updateMeMethod = UserController.class.getDeclaredMethod(
                "updateMe",
                CurrentUser.class,
                UpdateUserRequest.class
        );

        assertTrue(updateMeMethod.isAnnotationPresent(PatchMapping.class));
    }

    @Test
    void sessionControllerExposesManageOwnSessionsEndpoints() throws NoSuchMethodException {
        var listMethod = SessionController.class.getDeclaredMethod("listMine", CurrentUser.class);
        var revokeMethod = SessionController.class.getDeclaredMethod(
                "revoke",
                CurrentUser.class,
                UUID.class
        );

        assertTrue(listMethod.isAnnotationPresent(GetMapping.class));
        assertTrue(revokeMethod.isAnnotationPresent(DeleteMapping.class));
    }

    @Test
    void messageControllerExposesSenderEditAndDeleteEndpoints() throws NoSuchMethodException {
        var updateMethod = MessageController.class.getDeclaredMethod(
                "updateMessage",
                CurrentUser.class,
                UUID.class,
                UUID.class,
                UpdateMessageRequest.class
        );
        var deleteMethod = MessageController.class.getDeclaredMethod(
                "deleteMessage",
                CurrentUser.class,
                UUID.class,
                UUID.class
        );

        assertTrue(updateMethod.isAnnotationPresent(PatchMapping.class));
        assertTrue(deleteMethod.isAnnotationPresent(DeleteMapping.class));
    }

    private static void assertClassExists(String className) {
        assertDoesNotThrow(() -> Class.forName(className), className + " is required");
    }
}
