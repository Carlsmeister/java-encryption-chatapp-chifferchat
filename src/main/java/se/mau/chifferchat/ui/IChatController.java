package se.mau.chifferchat.ui;

import se.mau.chifferchat.common.Group;

import java.util.List;

/**
 * Interface for chat controllers to receive updates from the Client.
 * Allows different controller implementations (old and new UI).
 */
public interface IChatController {

    /**
     * Called when a message is received from the server.
     */
    void receiveMessage(String message);

    /**
     * Called when connection status changes.
     */
    void setConnectionStatus(boolean online);

    /**
     * Called when a new group is created.
     */
    void onGroupCreated(Group group);

    /**
     * Called when the list of groups is refreshed.
     */
    void refreshGroups(List<Group> groups);

    /**
     * Called when the list of online users is updated.
     */
    void updateOnlineUsers(List<String> users);

    /**
     * Gets the currently selected group (if any).
     */
    Group getCurrentGroup();

    /**
     * Appends a message to the group chat area.
     */
    void appendGroupMessage(String message);

    /**
     * Selects the given group in the UI and refreshes related UI elements
     * (header, member count, encryption icon, member list if open).
     */
    void selectGroup(Group group);

    /**
     * Called when the server provides updated members for a group. UI should refresh
     * member counts and the member drawer (if open) without clearing chat history.
     */
    void onGroupMembersUpdated(Group group);
}

