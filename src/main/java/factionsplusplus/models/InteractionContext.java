package factionsplusplus.models;

import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import factionsplusplus.services.LocaleService;

public class InteractionContext {
    private Type type;
    private TargetType targetType = null;
    private Gate gate = null;
    private UUID uuid = null;
    @Inject private final LocaleService localeService;

    @AssistedInject
    public InteractionContext(
        @Assisted Type type,
        @Assisted TargetType targetType,
        @Assisted UUID uuid,
        LocaleService localeService
    ) {
        this.type = type;
        this.targetType = targetType;
        this.uuid = uuid;
        this.localeService = localeService;
    }

    @AssistedInject
    public InteractionContext(
        @Assisted Type type,
        @Assisted TargetType targetType,
        LocaleService localeService
    ) {
        this.type = type;
        this.targetType = targetType;
        this.localeService = localeService;
    }

    @AssistedInject
    public InteractionContext(
        @Assisted Type type,
        LocaleService localeService
    ) {
        this.type = type;
        this.localeService = localeService;
    }

    @AssistedInject
    public InteractionContext(
        @Assisted Type type,
        @Assisted Gate gate,
        LocaleService localeService
    ) {
        this.type = type;
        this.gate = gate;
        this.localeService = localeService;
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
                return this.localeService.get("InteractionContextInfo.GrantAccess");
            case LockedBlockInquiry:
                return this.localeService.get("InteractionContextInfo.CheckAccess");
            case LockedBlockRevoke:
                return this.localeService.get("InteractionContextInfo.RevokeAccess");
            case LockedBlockLock:
                return this.localeService.get("InteractionContextInfo.LockBlock");
            case LockedBlockUnlock:
                return this.localeService.get("InteractionContextInfo.UnlockBlock");
            case LockedBlockForceUnlock:
                return this.localeService.get("InteractionContextInfo.ForceUnlockBlock");
            case GateCreating:
                return this.localeService.get("InteractionContextInfo.GateCreate");
            default:
                return this.localeService.get("InteractionContextInfo.Unknown");
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