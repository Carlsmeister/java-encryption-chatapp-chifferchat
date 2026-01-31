package se.mau.chifferchat.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Group implements Serializable {
    private final String groupId;
    private final String groupName;
    private final String creator;
    private final List<String> members;
    private final long createdAt;

    public Group(String groupName, String creator) {
        this.groupId = UUID.randomUUID().toString();
        this.groupName = groupName;
        this.creator = creator;
        this.members = new ArrayList<>();
        this.members.add(creator);
        this.createdAt = System.currentTimeMillis();
    }

    // For deserialization
    public Group(String groupId, String groupName, String creator, List<String> members, long createdAt) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.creator = creator;
        this.members = new ArrayList<>(members);
        this.createdAt = createdAt;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getCreator() {
        return creator;
    }

    public List<String> getMembers() {
        return new ArrayList<>(members);
    }

    /**
     * Replaces the member list with the provided members.
     * Ensures no duplicates and preserves order of the provided list.
     */
    public void setMembers(List<String> newMembers) {
        this.members.clear();
        for (String m : newMembers) {
            if (!this.members.contains(m)) {
                this.members.add(m);
            }
        }
    }

    public void addMember(String username) {
        if (!members.contains(username)) {
            members.add(username);
        }
    }

    public void removeMember(String username) {
        members.remove(username);
    }

    public boolean hasMember(String username) {
        return members.contains(username);
    }

    public int getMemberCount() {
        return members.size();
    }

    @Override
    public String toString() {
        return groupName + " (" + members.size() + " members)";
    }
}

