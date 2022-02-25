package geniusweb.exampleparties.skyparty;


import geniusweb.bidspace.IssueInfo;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.Value;
import geniusweb.issuevalue.ValueSet;

import java.util.*;

public class OpponentModel {
    private Map<String,Map<Value,Integer>> issueInfoFrequency = new HashMap<>();
    private Domain domain;
    private Map<String,Double> weights = new HashMap<>();
    private Map<Integer, Bid> historyBid = new HashMap<>();

    private int bidNumber = 0;


    public OpponentModel(Domain domain){
        this.domain = domain;
        for (String temp:domain.getIssues()) {
            Map<Value,Integer> tempMap = new HashMap<>();
            for(Value tempValue:domain.getValues(temp)){
                tempMap.put(tempValue,0);
            }
            weights.put(temp,0.0);
            issueInfoFrequency.put(temp,tempMap);
        }
    }

    public void addHistoryBid(Bid bid) {
        this.historyBid.put(bidNumber, bid);
    }

    public Map<Integer, Bid> getHistoryBid() {
        return this.historyBid;
    }

    public Bid getLastBid() {return this.historyBid.get(bidNumber);}




    public void update(Bid bid) throws Exception {
        addHistoryBid(bid);
        if (bid == null) {
            throw new Exception("null bid");
        }
        for (String temp:bid.getIssues()) {
            Map<Value,Integer> currentMap = issueInfoFrequency.get(temp);
            Value tempValue = bid.getValue(temp);
            Integer tempInt = currentMap.get(tempValue);
            tempInt += 1;
            currentMap.put(tempValue,tempInt);
            int numArray[] = new int[currentMap.size()];
            int index = 0;
            for (Integer i : currentMap.values()) {
                numArray[index] = i;
                index++;
            }
            double result = calculateSD(numArray);
            weights.put(temp,result);
            this.issueInfoFrequency.put(temp,currentMap);
        }
        bidNumber++;
    }


    public static double calculateSD(int numArray[])
    {
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.length;

        for(int num : numArray) {
            sum += (double) num;
        }

        double mean = sum/length;

        for(double num: numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }

        return Math.sqrt(standardDeviation/length);
    }

    public double calculateUtility(Bid bid) {
        double issueScore = 0.0;
        double issueWeight = 0.0;
        for (String issue:bid.getIssues()){
            issueWeight += this.weights.get(issue);
        }
        for (String issue:bid.getIssues()){
            int tempValue =  issueInfoFrequency.get(issue).get(bid.getValue(issue));
            issueScore += ((double) tempValue / (double) bidNumber) * (weights.get(issue)/issueWeight);
        }
        return issueScore/ (double)issueInfoFrequency.size();
    }
}
