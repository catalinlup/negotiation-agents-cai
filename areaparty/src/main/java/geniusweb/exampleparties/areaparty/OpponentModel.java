package geniusweb.exampleparties.areaparty;

import geniusweb.issuevalue.Bid;
import lombok.experimental.SuperBuilder;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;


@SuperBuilder
public class OpponentModel extends BoaElement {

    private final List<EvaluatedBid> proposedBids = new LinkedList<>();

    public void recordBid(Bid bid) {
        this.proposedBids.add(new EvaluatedBid(bid, this.getUtility(bid)));
    }

    public Bid getBestProposedBid() {
        return this.peekBestProposedBid().getBid();
    }

    public double peekBestProposedUtility() {
        return this.peekBestProposedBid().getUtility();
    }

    private EvaluatedBid peekBestProposedBid() {
        Optional<EvaluatedBid> bestBid = this.proposedBids.stream()
                .max(Comparator.comparingDouble(EvaluatedBid::getUtility));
        return bestBid.orElseThrow(() -> new RuntimeException("No best bid"));
    }
}
