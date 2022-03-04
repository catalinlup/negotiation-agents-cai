package geniusweb.exampleparties.areaparty;

import geniusweb.bidspace.AllPartialBidsList;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.LinearAdditiveUtilitySpace;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class BiddingStrategy extends BoaElement {

    private static final double MIN_UTILITY_RANGE = 0.9;
    private final List<Bid> sortedBids;
    private final double minSearchUtility;
    private final List<Bid> goodBids;
    private final AcceptanceStrategy acceptanceStrategy;


    public BiddingStrategy(
            NegotiationProgress progress,
            LinearAdditiveUtilitySpace profile,
            AcceptanceStrategy acceptanceStrategy
    ) {
        super(progress, profile);
        this.acceptanceStrategy = acceptanceStrategy;
        this.sortedBids = this.buildSortedBids();
        this.minSearchUtility = this.getMinSearchUtility();
        this.goodBids = this.buildGoodBids();
    }

    public Bid getNextBid() {
        if (this.isRandomSearchPhase()) {
            return this.getNextExplorationBid();
        }
        return this.getNextNegotiationBid();
    }

    private Bid getNextNegotiationBid() {
        final List<Bid> acceptableBids = this.sortedBids.stream()
                .filter(this.acceptanceStrategy::isRationalBidProposal)
                .collect(Collectors.toList());
        if (acceptableBids.size() == 0) {
            // fallback
            return this.getNextExplorationBid();
        }
        return acceptableBids.get((int) (Math.random() * (acceptableBids.size() - 1)));
    }

    private Bid getNextExplorationBid() {
        if (this.goodBids.isEmpty()) {
            this.goodBids.addAll(this.buildGoodBids());
        }

        final Bid selectedBid = this.goodBids.get((int) (Math.random() * (this.goodBids.size() - 1)));
        this.goodBids.remove(selectedBid);
        return selectedBid;
    }

    private List<Bid> buildGoodBids() {
        return this.sortedBids.stream()
                .takeWhile(bid -> this.getUtility(bid) > this.minSearchUtility)
                .collect(Collectors.toList());
    }

    private double getMinSearchUtility() {
        return this.getUtility(
                this.sortedBids.get((int) ((1 - MIN_UTILITY_RANGE) * this.sortedBids.size()))
        );
    }

    private List<Bid> buildSortedBids() {
        final AllPartialBidsList bidSpace = new AllPartialBidsList(this.getProfile().getDomain());
        return StreamSupport.stream(bidSpace.spliterator(), false)
                .sorted(Comparator.comparingDouble(this::getUtility).reversed())
                .collect(Collectors.toList());
    }
}
