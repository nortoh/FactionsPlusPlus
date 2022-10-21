package factionsplusplus.models;

import java.lang.annotation.Target;
import java.util.UUID;

public class InteractionContext {
    private Type type;
    private TargetType targetType = null;
    private Gate gate = null;
    private UUID uuid = null;

    public InteractionContext(Type type, TargetType targetType, UUID uuid) {
        this.type = type;
        this.targetType = targetType;
        this.uuid = uuid;
    }

    public InteractionContext(Type type, TargetType targetType) {
        this.type = type;
        this.targetType = targetType;
    }

    public InteractionContext(Type type) {
        this.type = type;
    }

    public InteractionContext(Type type, Gate gate) {
        this.type = type;
        this.gate = gate;
    }

    public boolean isLockedBlockGrant() {
        return this.type == Type.LockedBlockGrant;
    }

    public boolean isLockedBlockInquiry() {
        return this.type == Type.LockedBlockInquiry;
    }

    public boolean isLockedBlockRevoke() {
        return this.type == Type.LockedBlockRevoke;
    }

    public boolean isLockedBlockAccessCommand() {
        return this.type == Type.LockedBlockGrant || this.type == Type.LockedBlockRevoke || this.type == Type.LockedBlockInquiry;
    }

    public boolean isLockedBlockLock() {
        return this.type == Type.LockedBlockLock;
    }

    public boolean isLockedBlockUnlock() {
        return this.type == Type.LockedBlockUnlock;
    }

    public boolean isLockedBlockForceUnlock() {
        return this.type == Type.LockedBlockForceUnlock;
    }

    public boolean isGateCreating() {
        return this.type == Type.GateCreating;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public Gate getGate() {
        return this.gate;
    }

    public TargetType getTargetType() {
        return this.targetType;
    }

    public String toString() {
        // TODO: localize these
        switch(this.type) {
            case LockedBlockGrant:
                return "Grant Access";
            case LockedBlockInquiry:
                return "Check Access";
            case LockedBlockRevoke:
                return "Revoke Access";
            case LockedBlockLock:
                return "Lock Block";
            case LockedBlockUnlock:
                return "Unlock Block";
            case LockedBlockForceUnlock:
                return "Force Unlock Block";
            case GateCreating:
                return "Gate Create";
            default:
                return "Unknown";
        }
    }

    public enum Type {
        LockedBlockLock,
        LockedBlockUnlock,
        LockedBlockForceUnlock,
        LockedBlockGrant,
        LockedBlockInquiry,
        LockedBlockRevoke,
        GateCreating
    }

    public enum TargetType {
        Player,
        Allies,
        FactionMembers
    }
}