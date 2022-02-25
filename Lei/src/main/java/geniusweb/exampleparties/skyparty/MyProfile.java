package geniusweb.exampleparties.skyparty;

import geniusweb.bidspace.AllPartialBidsList;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.Profile;
import geniusweb.profile.utilityspace.UtilitySpace;

import com.fasterxml.jackson.databind.*;

import java.math.BigInteger;
import java.util.*;


public class MyProfile {
    private Domain domain;
    private AllPartialBidsList bidList;
    private Bid reservationBid;
    private Bid maxBid;
    private double reservationBidValue;
    private HashMap<Bid, Double> AllUtilityMap = new HashMap<>();
    private List<Bid> acceptableBid = new ArrayList();
    private List<Bid> paretoFrontierBid = new ArrayList<>();

    private Map<String, List<Double>> strategy = new HashMap<>();

    public MyProfile(Profile profile) {
        this.domain = profile.getDomain();
        this.bidList = new AllPartialBidsList(this.domain);
        UtilitySpace utilitySpace = (UtilitySpace) profile;
        this.reservationBid = profile.getReservationBid();
        double tempBest = utilitySpace.getUtility(this.reservationBid).doubleValue();
        this.maxBid = this.reservationBid;
        if (profile instanceof UtilitySpace) {
            this.reservationBidValue =  utilitySpace.getUtility(this.reservationBid).doubleValue();
            int total = this.bidList.size().intValue();
            for (int i = 0; i <total ; i++) {
                Bid tempBid = this.bidList.get(BigInteger.valueOf(i));
                double result = utilitySpace.getUtility(tempBid).doubleValue();
                if (result > tempBest){
                    this.maxBid = tempBid;
                    tempBest = result;
                }
                AllUtilityMap.put(tempBid,result);
            }
        }
        this.acceptableBid.add(maxBid);

        //The order in the strategy: selfish, fortunate, silent, nice, concession
        this.strategy.put("selfish", new ArrayList(Arrays.asList(0.0,0.0,0.0,0.0,0.0)));
        this.strategy.put("fortunate", new ArrayList(Arrays.asList(0.0,0.0,0.0,0.0,0.0)));
        this.strategy.put("silent", new ArrayList(Arrays.asList(0.0,0.0,0.0,0.0,0.0)));
        this.strategy.put("nice", new ArrayList(Arrays.asList(0.0,0.0,0.0,0.0,0.0)));
        this.strategy.put("concession", new ArrayList(Arrays.asList(0.0,0.0,0.0,0.0,0.0)));



    }

    public Bid getMaxBid(){
        return this.maxBid;
    }

    public Bid getReservationBid() {
        return this.reservationBid;
    }

    public Bid pickBid() {
        Random rand = new Random();
        return acceptableBid.get(rand.nextInt(acceptableBid.size()));
    }

    public void updateAcceptableBidList(double threshold) {
        for (Bid tempBid:AllUtilityMap.keySet()) {
            if (AllUtilityMap.get(tempBid)>=threshold) {
                if (!acceptableBid.contains(tempBid)) {
                    acceptableBid.add(tempBid);
                }
            }
        }
    }


    public Bid pickStrategyBid(int opponentMove, OpponentModel opponentModel){
        if (paretoFrontierBid.size()==0){
            return pickBid();
        }
        List strategyUse = this.strategy.get(opponentMove);
        int nextMoveType = NextMoveTYpe(strategyUse);

        Bid pickedBid = pickBid();
        while(bidType(pickedBid, opponentModel) != nextMoveType){
            pickedBid = pickBid();
        }

        return pickedBid;
    }

    public static int NextMoveTYpe(List<Double> strategyUse){

        Random rand = new Random();
        double number = rand.nextDouble();
        double firstDivision = strategyUse.get(0);
        double secDivision = firstDivision + strategyUse.get(1);
        double thirdDivision = secDivision + strategyUse.get(2);
        double fourthDivision = thirdDivision + strategyUse.get(3);


        if(number <= firstDivision) return 0;
        else if(number <= secDivision) return 1;
        else if(number <= thirdDivision) return 2;
        else if(number <= fourthDivision) return 3;
        else  return 4;

    }


    public int bidType(Bid newBid, OpponentModel opponentModel) {
        UtilitySpace utilitySpace = (UtilitySpace) this;
        double opponentU = opponentModel.calculateUtility(newBid);
        double myU = utilitySpace.getUtility(newBid).doubleValue();
        double opponentLastU = opponentModel.calculateUtility(acceptableBid.get(acceptableBid.size()-1));
        double myLastU = utilitySpace.getUtility(acceptableBid.get(acceptableBid.size()-1)).doubleValue();

        double deltaO = opponentU - opponentLastU;
        double deltaS = myU - myLastU;

        if(deltaO < 0 && deltaS > 0) return 0;
        else if(deltaO <= 0 && deltaS <= 0) return 1;
        else if(deltaO == 0 && deltaS== 0) return 2;
        else if(deltaO > 0 && deltaS == 0) return 3;
        else if(deltaO > 0 && deltaS < 0) return 4;
        else return 5;
    }



    public boolean isAcceptable(Bid bid) {
        return acceptableBid.contains(bid);
    }




}
