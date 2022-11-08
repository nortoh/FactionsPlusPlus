package factionsplusplus.models;

import java.util.UUID;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import factionsplusplus.data.beans.PollOptionBean;
import factionsplusplus.services.DataService;

public class PollOption {
    private UUID id;
    @ColumnName("poll_id")
    private UUID pollUUID;
    private String text;

    private final DataService dataService;

    @AssistedInject
    public PollOption(
        @Assisted UUID pollUUID,
        @Assisted String text,
        DataService dataService
    ) {
        this.id = UUID.randomUUID();
        this.pollUUID = pollUUID;
        this.text = text;
        this.dataService = dataService;
    }

    @AssistedInject
    public PollOption(
        @Assisted PollOptionBean bean,
        DataService dataService
    ) {
        this.id = bean.getId();
        this.pollUUID = bean.getPollUUID();
        this.text = bean.getText();
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

    public String getText() {
        return this.text;
    }
}
