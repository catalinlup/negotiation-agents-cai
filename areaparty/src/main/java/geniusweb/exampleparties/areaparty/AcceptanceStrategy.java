package geniusweb.exampleparties.areaparty;

import geniusweb.issuevalue.Bid;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class AcceptanceStrategy extends BoaElement {

    private static final double UPPER_THRESHOLD_POWER = 5;
    private static final double LOWER_THRESHOLD_POWER = 1.5;

    private final OpponentModel opponentModel;

    public double getAcceptableLowerThreshold() {
        return Math.max(
                this.opponentModel.peekBestProposedUtility(),
                this.getAcceptableConcessionUtility()
        );
    }

    public double getRationalUpperThreshold() {
        return 1 - Math.pow((1 - this.getAcceptableLowerThreshold()), UPPER_THRESHOLD_POWER);
    }

    public boolean isAcceptableBid(Bid bid) {
        if (bid == null) {
            return false;
        } else if (this.isRandomSearchPhase()) {
            return this.getUtility(bid) == 1;
        } else {
            return this.getUtility(bid) > this.getAcceptableLowerThreshold();
        }
    }

    public boolean isRationalBidProposal(Bid bid) {
        final double utility = this.getUtility(bid);
        return this.getRationalUpperThreshold() > utility && this.getAcceptableLowerThreshold() < utility;
    }

    public double getAcceptableConcessionUtility() {
        return 1 - Math.pow(this.getProgress()/2, LOWER_THRESHOLD_POWER);
    }
}
