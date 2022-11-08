package factionsplusplus.data.factories;

import java.util.UUID;

import com.google.inject.assistedinject.Assisted;
import factionsplusplus.data.beans.PollVoteBean;
import factionsplusplus.models.PollOption;

public interface PollVoteFactory {
    PollOption create(
        @Assisted UUID pollUUID,
        @Assisted UUID playerUUID,
        @Assisted UUID pollOptionUUID
    );
    PollOption create(PollVoteBean bean);
}
