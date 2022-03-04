package geniusweb.exampleparties.areaparty;

import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.LinearAdditiveUtilitySpace;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@RequiredArgsConstructor
public class BoaElement {

    private static final double RANDOM_SEARCH_PHASE = 0.75;
    private static final double CONCESSION_PHASE = 0.9;

    private final NegotiationProgress progress;

    @Getter
    private final LinearAdditiveUtilitySpace profile;

    public double getUtility(Bid bid) {
        return this.profile.getUtility(bid).doubleValue();
    }

    public double getProgress() {
        return this.progress.getProgress();
    }

    public boolean isRandomSearchPhase() {
        return this.getProgress() < RANDOM_SEARCH_PHASE;
    }

    public boolean isConcessionPhase() {
        return this.getProgress() > CONCESSION_PHASE;
    }
}
