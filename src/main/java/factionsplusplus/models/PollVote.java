package factionsplusplus.models;

import java.util.UUID;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import factionsplusplus.data.beans.PollVoteBean;
import factionsplusplus.services.DataService;

public class PollVote {
    private UUID id;
    @ColumnName("player_id")
    private UUID playerUUID;
    @ColumnName("poll_id")
    private UUID pollUUID;
    @ColumnName("option_id")
    private UUID pollOptionUUID;

    private final DataService dataService;

    @AssistedInject
    public PollVote(
        @Assisted UUID pollUUID,
        @Assisted UUID playerUUID,
        @Assisted UUID pollOptionUUID,
        DataService dataService
    ) {
        this.id = UUID.randomUUID();
        this.pollUUID = pollUUID;
        this.playerUUID = playerUUID;
        this.pollOptionUUID = pollOptionUUID;
        this.dataService = dataService;
    }

    @AssistedInject
    public PollVote(
        @Assisted PollVoteBean bean,
        DataService dataService
    ) {
        this.id = bean.getId();
        this.pollUUID = bean.getPollUUID();
        this.playerUUID = bean.getPlayerUUID();
        this.pollOptionUUID = bean.getPollOptionUUID();
        this.dataService = dataService;
    }

    public UUID getUUID() {
        return this.id;
    }

    public UUID getPollUUID() {
        return this.pollUUID;
    }

    public Poll getPoll() {
        return this.dataService.getPoll(this.pollUUID);
    }

    public UUID getPollOptionUUID() {
        return this.pollOptionUUID;
    }

    public PollOption getPollOption() {
        return this.dataService.getPollOption(this.id);
    }
}
