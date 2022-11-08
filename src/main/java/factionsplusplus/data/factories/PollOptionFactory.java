package factionsplusplus.data.factories;

import java.util.UUID;

import com.google.inject.assistedinject.Assisted;
import factionsplusplus.data.beans.PollOptionBean;
import factionsplusplus.models.PollOption;

public interface PollOptionFactory {
    PollOption create(
        @Assisted UUID pollUUID,
        @Assisted String text
    );
    PollOption create(PollOptionBean bean);
}
