package factionsplusplus.data.factories;

import java.util.UUID;

import com.google.inject.assistedinject.Assisted;
import factionsplusplus.data.beans.PollBean;
import factionsplusplus.models.Poll;

public interface PollFactory {
    Poll create(
        @Assisted UUID factionUuid,
        @Assisted String question
    );
    Poll create(PollBean bean);
}
