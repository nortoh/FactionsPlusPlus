package factionsplusplus.data.repositories;

import com.google.inject.Singleton;

import com.google.inject.Inject;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Optional;

import factionsplusplus.models.Poll;
import factionsplusplus.services.DataProviderService;
import factionsplusplus.data.daos.PollDao;
import factionsplusplus.data.factories.PollFactory;
import factionsplusplus.utils.Logger;

@Singleton
public class PollRepository {
    private Map<UUID, Poll> pollStore = new ConcurrentHashMap<>();
    private final Logger logger;
    private final DataProviderService dataProviderService;
    private final PollFactory pollFactory;

    @Inject
    public PollRepository(
        Logger logger,
        DataProviderService dataProviderService,
        PollFactory pollFactory
    ) {
        this.logger = logger;
        this.dataProviderService = dataProviderService;
        this.pollFactory = pollFactory;
    }

    // Load polls
    public void load() {
        try {
            this.pollStore.clear();
            this.getDAO().get().stream()
                .forEach(poll -> {
                    this.pollStore.put(poll.getId(), this.pollFactory.create(poll));
                });
        } catch (Exception e) {
            this.logger.error(String.format("Error loading polls: %s", e.getMessage()));
        }
    }

    // Save a poll after creating
    public void create(Poll poll) {
        poll = this.getDAO().createNewPoll(poll);
        this.pollStore.put(poll.getUUID(), poll);
    }

    // Delete a poll
    public void delete(UUID pollUUID) {
        this.getDAO().delete(pollUUID);
        this.pollStore.remove(pollUUID);
    }
    public void delete(Poll poll) {
        this.delete(poll.getUUID());
    }

    // Persist poll
    public void persist(Poll poll) {
        this.getDAO().update(poll);
    }

    /**
     * @note We only want to get this by ID, not by anything else
     *
     * @param uuid
     * @return
     */
    public Poll get(UUID uuid) {
        return this.pollStore.get(uuid);
    }

    /**
     * @note Find first enables only one active poll per player
     *
     * @param playerUUID
     * @return
     */
    public Poll getForPlayer(UUID playerUUID) {
        Optional<Poll> poll = this.pollStore
            .values()
            .stream()
            .filter(entry -> entry.getFaction().isMember(playerUUID))
            .findFirst();

        return poll.orElse(null);
    }

    public PollDao getDAO() {
        return this.dataProviderService.getPersistentData().onDemand(PollDao.class);
    }
}