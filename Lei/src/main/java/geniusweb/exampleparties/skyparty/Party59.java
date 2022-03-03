package geniusweb.exampleparties.skyparty;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.logging.Level;

import geniusweb.bidspace.AllBidsList;
import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.LearningDone;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.actions.Vote;
import geniusweb.actions.Votes;
import geniusweb.actions.VotesWithValue;
import geniusweb.bidspace.AllPartialBidsList;
import geniusweb.inform.ActionDone;
import geniusweb.inform.Finished;
import geniusweb.inform.Inform;
import geniusweb.inform.OptIn;
import geniusweb.inform.OptInWithValue;
import geniusweb.inform.Settings;
import geniusweb.inform.YourTurn;
import geniusweb.issuevalue.Bid;
import geniusweb.party.Capabilities;
import geniusweb.party.DefaultParty;
import geniusweb.profile.PartialOrdering;
import geniusweb.profile.Profile;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.profileconnection.ProfileConnectionFactory;
import geniusweb.profileconnection.ProfileInterface;
import geniusweb.progress.Progress;
import geniusweb.progress.ProgressRounds;
import tudelft.utilities.logging.Reporter;

/**
 * A party that places random bids and accepts when it receives an offer with
 * sufficient utility. This party is also a demo on how to support the various
 * protocols, which causes this party to look a bit complex.
 * 
 * <h2>parameters</h2>
 * <table >
 * <caption>parameters</caption>
 * <tr>
 * <td>minPower</td>
 * <td>This value is used as minPower for placed {@link Vote}s. Default value is
 * 2.</td>
 * </tr>
 * <tr>
 * <td>maxPower</td>
 * <td>This value is used as maxPower for placed {@link Vote}s. Default value is
 * infinity.</td>
 * </tr>
 * </table>
 */
public class Party59 extends DefaultParty {

	private Bid lastReceivedBid = null;
	private Bid bestBid = null;
	private PartyId me;
	private final Random random = new Random();
	protected ProfileInterface profileint = null;
	private ProgressRounds progress;
	private Settings settings;
	private Votes lastvotes;
	private VotesWithValue lastvoteswithvalue;
	private String protocol;
	private int exploreRounds = 100;
	private AllBidsList bistList = null;
	private MyProfile profile = null;
	private int exploration = 50;
	private double threshold = 1.0;//first threshold, max.
	private double minimumUtility = 0.8;
	private double offerThreshold = 0.99;
	private double estimateNashPoint;
	private OpponentModel opponentModel;

	public Party59() {
	}

	public Party59(Reporter reporter) {
		super(reporter); // for debugging
	}

	// create opponent model
	@Override
	public void notifyChange(Inform info) {
		try {
			if (info instanceof Settings) {
				Settings sett = (Settings) info;
				this.me = sett.getID();
				this.progress = (ProgressRounds) sett.getProgress();
				this.settings = sett;
				this.protocol = sett.getProtocol().getURI().getPath();
				if ("Learn".equals(protocol)) {
					getConnection().send(new LearningDone(me));
				} else {
					this.profileint = ProfileConnectionFactory
							.create(sett.getProfile().getURI(), getReporter());
					this.profile = new MyProfile(profileint.getProfile());
					this.opponentModel = new OpponentModel(profileint.getProfile().getDomain());
				}
			} else if (info instanceof ActionDone) {
				Action otheract = ((ActionDone) info).getAction();
				if (otheract instanceof Offer) {
					lastReceivedBid = ((Offer) otheract).getBid();
				}
			} else if (info instanceof YourTurn) {
				makeOffer(lastReceivedBid);
			} else if (info instanceof Finished) {
				getReporter().log(Level.INFO, "Final ourcome:" + info);
				terminate(); // stop this party and free resources.
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to handle info"+ e.getLocalizedMessage(), e);
		}
		updateRound(info);
	}


	@Override
	public Capabilities getCapabilities() {
		return new Capabilities(new HashSet<>(
				Arrays.asList("SAOP", "AMOP", "MOPAC", "MOPAC2", "Learn")),
				Collections.singleton(Profile.class));
	}

	@Override
	public String getDescription() {
		return "places random bids until it can accept an offer with utility >0.6. "
				+ "Parameters minPower and maxPower can be used to control voting behaviour.";
	}

	@Override
	public void terminate() {
		super.terminate();
		if (this.profileint != null) {
			this.profileint.close();
			this.profileint = null;
		}
	}

//	private void setThreshold(int round){
//		if (round < 100) {
//			this.offerThreshold -= 0.0005;
//			profile.updateAcceptableBidList(this.offerThreshold);
//			//round for exploring opponent model.
//			//the threshold for these rounds must be more than 0.95, which prevent we get deal too early
//			//however might some students use random party that offer us good bid in the early time. so we are going to accept them :D
//		} else if (round < 200) {
//			this.offerThreshold -= 0.001;
//			profile.updateAcceptableBidList(this.offerThreshold);
//		} else if (round >= 200 && round < 380){
//			profile.predictParetoFrontier(this.opponentModel.getHistoryBid());
//			//add nash point fomular to here.
//		} else if (round >= 380) {
//			this.offerThreshold -=0.02;
//			profile.updateAcceptableBidList(this.offerThreshold);
//		}
//
//	}

	/******************* private support funcs ************************/

	/**
	 * Update {@link #progress}
	 * 
	 * @param info the received info. Used to determine if this is the last info
	 *             of the round
	 */
	private void updateRound(Inform info) {
		if (protocol == null)
			return;
		switch (protocol) {
		case "SAOP":
		case "SHAOP":
			if (!(info instanceof YourTurn))
				return;
			break;
		case "MOPAC":
			if (!(info instanceof OptIn))
				return;
			break;
		case "MOPAC2":
			if (!(info instanceof OptInWithValue))
				return;
			break;
		default:
			return;
		}
		// if we get here, round must be increased.
		if (progress instanceof ProgressRounds) {
			progress = ((ProgressRounds) progress).advance();
		}

	}

	/**
	 * send our next offer
	 */
	private void makeOffer(Bid lastReceivedBid) throws IOException {
		Action action = null;
		int round = progress.getCurrentRound();

		if ((protocol.equals("SAOP") || protocol.equals("SHAOP"))
				&& isGood(lastReceivedBid)) {
			action = new Accept(me, lastReceivedBid);
		} else {
			int opponentMove = bidType(lastReceivedBid);
			if (round<=100 && opponentMove == 5) {
				action = new Offer(me, this.profile.pickBid());
			} else {
				action = new Offer(me,this.profile.pickStrategyBid(opponentMove, opponentModel));
			}
		}
		getConnection().send(action);

	}

	/**
	 * @param bid the bid to check
	 * @return true iff bid is good for us.
	 */
	private boolean isGood(Bid bid) {
		if (bid == null)
			return false;
		return profile.isAcceptable(bid);
	}

	/**
	 * @param bid the bid to check
	 * @return 0 is selfish, 1 is fortunate, 2 is silent, 3 is nice, 4 is concession, other 5
	 */
	private int bidType(Bid bid) {
		if(bid == null) return 5;
		UtilitySpace utilitySpace = (UtilitySpace) profile.getProfile();
		double opponentU = opponentModel.calculateUtility(bid);
		double myU = utilitySpace.getUtility(bid).doubleValue();
		double opponentLastU = opponentModel.calculateUtility(opponentModel.getLastBid());
		double myLastU = utilitySpace.getUtility(opponentModel.getLastBid()).doubleValue();

		double deltaO = opponentU - opponentLastU;
		double deltaS = myU - myLastU;

		if(deltaO > 0 && deltaS < 0) return 0;
		else if(deltaO >= 0 && deltaS >= 0) return 1;
		else  if(deltaO == 0 && deltaS== 0) return 2;
		else if(deltaO == 0 && deltaS > 0) return 3;
		else if(deltaO < 0 && deltaS > 0) return 4;
		else return 5;
	}



}
