package geniusweb.exampleparties.areaparty;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.logging.Level;

import geniusweb.actions.*;
import geniusweb.inform.ActionDone;
import geniusweb.inform.Finished;
import geniusweb.inform.Inform;
import geniusweb.inform.Settings;
import geniusweb.inform.YourTurn;
import geniusweb.issuevalue.Bid;
import geniusweb.party.Capabilities;
import geniusweb.party.DefaultParty;
import geniusweb.profile.Profile;
import geniusweb.profile.utilityspace.LinearAdditiveUtilitySpace;
import geniusweb.profileconnection.ProfileConnectionFactory;
import geniusweb.profileconnection.ProfileInterface;
import geniusweb.progress.Progress;
import geniusweb.progress.ProgressRounds;
import lombok.NoArgsConstructor;
import tudelft.utilities.logging.Reporter;

import javax.websocket.DeploymentException;

@NoArgsConstructor
public class AreaParty extends DefaultParty {

	private Bid lastReceivedBid;
	private PartyId me;
	protected ProfileInterface profile;
	private Progress progress;
    private NegotiationProgress negotiationProgress;
	private Settings settings;
    private OpponentModel opponentModel;
    private AcceptanceStrategy acceptanceStrategy;
    private BiddingStrategy biddingStrategy;

	public AreaParty(Reporter reporter) {
		super(reporter); // for debugging
	}

	@Override
	public void notifyChange(Inform info) {
		try {
			if (info instanceof Settings) {
                this.consumeSettings((Settings) info);
			} else if (info instanceof ActionDone) {
				this.consumeAction(((ActionDone) info).getAction());
			} else if (info instanceof YourTurn) {
				makeOffer();
			} else if (info instanceof Finished) {
                getReporter().log(Level.INFO, "Final outcome:" + info);
                terminate();
            }
		} catch (Exception e) {
			throw new RuntimeException("Failed to handle info", e);
		}
		updateRound(info);
	}

	@Override
	public Capabilities getCapabilities() {
		return new Capabilities(new HashSet<>(
                Collections.singletonList("SAOP")),
				Collections.singleton(Profile.class));
	}

	@Override
	public String getDescription() {
		return "Area based agent implementation.";
	}

	@Override
	public void terminate() {
		super.terminate();
		if (this.profile != null) {
			this.profile.close();
			this.profile = null;
		}
	}

    private void consumeSettings(Settings settings) throws DeploymentException, IOException {
        this.settings = settings;
        this.me = settings.getID();
        this.progress = settings.getProgress();
        this.negotiationProgress = new NegotiationProgress(settings.getProgress());
        this.profile = ProfileConnectionFactory.create(settings.getProfile().getURI(), getReporter());
        final LinearAdditiveUtilitySpace utilitySpace = (LinearAdditiveUtilitySpace) profile.getProfile();
        this.opponentModel = OpponentModel.builder()
                .progress(this.negotiationProgress)
                .profile(utilitySpace)
                .build();
        this.acceptanceStrategy = AcceptanceStrategy.builder()
                .progress(this.negotiationProgress)
                .profile(utilitySpace)
                .opponentModel(this.opponentModel)
                .build();
        this.biddingStrategy = new BiddingStrategy(
                this.negotiationProgress,
                utilitySpace,
                this.acceptanceStrategy
        );
    }

    private void consumeAction(Action action) {
        if (action instanceof Offer && !action.getActor().getName().equals(this.me.getName())) {
            final Bid bid = ((Offer) action).getBid();
            this.lastReceivedBid = bid;
            this.opponentModel.recordBid(bid);
        }
    }

	private void updateRound(Inform info) {
		if (!(info instanceof YourTurn) || !(progress instanceof ProgressRounds)) {
            return;
        }
        progress = ((ProgressRounds) progress).advance();
        negotiationProgress.advance();
	}

	private void makeOffer() throws IOException {
        final boolean shouldMakeDesperateBid = this.opponentModel.isConcessionPhase() &&
        (
                this.acceptanceStrategy.getAcceptableConcessionUtility() < this.opponentModel.peekBestProposedUtility()
        );
        if (shouldMakeDesperateBid) {
            makeDesperateOffer();
            return;
        }
		getConnection().send(
                this.acceptanceStrategy.isAcceptableBid(lastReceivedBid)
                        ? new Accept(me, lastReceivedBid)
                        : new Offer(me, this.biddingStrategy.getNextBid())
        );
	}

    private void makeDesperateOffer() throws IOException {
        getConnection().send(new Offer(me, this.opponentModel.getBestProposedBid()));
    }
}
