package dansplugins.factionsystem.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.models.Faction;

@Singleton
public class FactionService {
    private final ConfigService configService;

    @Inject
    public FactionService(
        ConfigService configService
    ) {
        this.configService = configService;
    }

    public void setBonusPower(Faction faction, int power) {
        if (!this.configService.getBoolean("bonusPowerEnabled") || !((boolean) faction.getFlags().getFlag("acceptBonusPower"))) {
            return;
        }
        faction.setBonusPower(power);
    }

    public int calculateMaxOfficers(Faction faction) {
        int officersPerXNumber = this.configService.getInt("officerPerMemberCount");
        int officersFromConfig = faction.getMemberList().size() / officersPerXNumber;
        return 1 + officersFromConfig;
    }
}