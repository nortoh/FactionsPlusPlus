package factionsplusplus.models;

import factionsplusplus.data.beans.PollBean;
import factionsplusplus.data.repositories.PollRepository;
import factionsplusplus.models.interfaces.Identifiable;
import factionsplusplus.services.DataService;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class Poll implements Identifiable {
    private UUID id;
    @ColumnName("faction_id")
    private UUID faction;
    @ColumnName("choiced_allowed")
    private int choicesAllowed;
    private String question;
    @ColumnName("created_at")
    private ZonedDateTime createdAt;

    private final DataService dataService;
    private final PollRepository pollRepository;

    @AssistedInject
    public Poll(
      @Assisted UUID factionUuid,
      @Assisted String question,
      DataService dataService
    ) {
		this.id = UUID.randomUUID();
		this.faction = factionUuid;
		this.choicesAllowed = 0;
		this.question = question;
		this.createdAt = ZonedDateTime.now();
		this.dataService = dataService;
		this.pollRepository = dataService.getPollRepository();
    }

    @AssistedInject
    public Poll(@Assisted PollBean bean, DataService dataService) {
		this.id = bean.getId();
		this.faction = bean.getFaction();
		this.choicesAllowed = bean.getChoicesAllowed();
		this.question = bean.getQuestion();
		this.createdAt = bean.getCreatedAt();
		this.dataService = dataService;
		this.pollRepository = dataService.getPollRepository();
    }

    public UUID getUUID() {
      	return this.id;
    }

    public UUID getFactionUUID() {
      	return this.id;
    }

    public Faction getFaction() {
      	return this.dataService.getFaction(this.faction);
    }

    public String getQuestion() {
      	return this.question;
    }

    public void setQuestion(String question) {
      	this.question = question;
    }

	public void addPollOption(PollOption option) {

	}

    public void persist() {
        this.pollRepository.persist(this);
    }
}