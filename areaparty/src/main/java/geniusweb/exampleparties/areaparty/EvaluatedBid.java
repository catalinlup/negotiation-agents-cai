package geniusweb.exampleparties.areaparty;

import geniusweb.issuevalue.Bid;
import lombok.Value;

@Value
public class EvaluatedBid {
    Bid bid;
    double utility;
}
