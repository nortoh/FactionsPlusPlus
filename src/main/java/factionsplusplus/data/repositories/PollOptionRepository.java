package factionsplusplus.data.repositories;

import com.google.inject.Singleton;

import com.google.inject.Inject;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

import factionsplusplus.models.Poll;
import factionsplusplus.models.PollOption;
import factionsplusplus.services.DataProviderService;
import factionsplusplus.data.beans.PollOptionBean;
import factionsplusplus.data.daos.PollOptionDao;
import factionsplusplus.data.factories.PollOptionFactory;
import factionsplusplus.utils.Logger;

@Singleton
public class PollOptionRepository {
    private Map<UUID, PollOption> pollOptionStore = new ConcurrentHashMap<>();
    private final Logger logger;
    private final DataProviderService dataProviderService;
    private final PollOptionFactory pollOptionFactory;

    @Inject
    public PollOptionRepository(
        Logger logger,
        DataProviderService dataProviderService,
        PollOptionFactory pollOptionFactory
    ) {
        this.logger = logger;
        this.dataProviderService = dataProviderService;
        this.pollOptionFactory = pollOptionFactory;
    }

    // Load polls
    public void load() {
        try {
            this.pollOptionStore.clear();
            this.getDAO().get().stream()
                .forEach(pollOption -> {
                    this.pollOptionStore.put(pollOption.getId(), this.pollOptionFactory.create(pollOption));
                });
        } catch (Exception e) {
            this.logger.error(String.format("Error loading polls: %s", e.getMessage()));
        }
    }

    // Save a poll after creating
    public void create(PollOption pollOption) {
        pollOption = this.getDAO().createNewPollOption(pollOption);
        this.pollOptionStore.put(pollOption.getUUID(), pollOption);
    }

    // Delete a poll
    public void delete(UUID pollUUID) {
        this.getDAO().delete(pollUUID);
        this.pollOptionStore.remove(pollUUID);
    }
    public void delete(PollOption pollOption) {
        this.delete(pollOption.getUUID());
    }

    // Persist poll
    public void persist(PollOption pollOption) {
        this.getDAO().update(pollOption);
    }

    /**
     * @param uuid
     * @return
     */
    public PollOption get(UUID uuid) {
        return this.pollOptionStore.get(uuid);
    }

    /**
     *
     * @param UUID
     * @return
     */
    public List<PollOption> getOptionsForPoll(UUID pollUUID) {
        List<PollOptionBean> data = this.getDAO().getForPoll(pollUUID);

        return data.stream()
            .map(bean -> this.pollOptionFactory
            .create(bean))
            .collect(Collectors.toList());
    }

    /**
     *
     * @param Poll
     * @return
     */
    public List<PollOption> getOptionsForPoll(Poll poll) {
        return this.getOptionsForPoll(poll.getUUID());
    }

    public PollOptionDao getDAO() {
        return this.dataProviderService.getPersistentData().onDemand(PollOptionDao.class);
    }
}