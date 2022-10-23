package factionsplusplus.models;

import java.util.UUID;

import com.google.inject.Inject;

import factionsplusplus.services.LocaleService;

public class InteractionContext {
    private Type type;
    private TargetType targetType = null;
    private Gate gate = null;
    private UUID uuid = null;
    @Inject private LocaleService localeService;

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
        switch(this.type) {
            case LockedBlockGrant:
                return this.localeService.get("GrantAccess");
            case LockedBlockInquiry:
                return this.localeService.get("CheckAccess");
            case LockedBlockRevoke:
                return this.localeService.get("RevokeAccess");
            case LockedBlockLock:
                return this.localeService.get("LockBlock");
            case LockedBlockUnlock:
                return this.localeService.get("UnlockBlock");
            case LockedBlockForceUnlock:
                return this.localeService.get("ForceUnlockBlock");
            case GateCreating:
                return this.localeService.get("GateCreate");
            default:
                return this.localeService.get("Unknown");
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