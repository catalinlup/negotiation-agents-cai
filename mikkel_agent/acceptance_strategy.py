from geniusweb.issuevalue import Bid
from geniusweb.profileconnection.ProfileInterface import ProfileInterface

from agents.mikkel_agent.boa_element import BoaElement
from agents.mikkel_agent.helpers.negotiation_progress import NegotiationProgress
from agents.mikkel_agent.opponent_model import OpponentModel


class AcceptanceStrategy(BoaElement):
    """
    Accept no offers during exploration.

    Once final bidding begins, start out aggressive and begin to concede as time progresses.
    A higher lower threshold power increases toughness in the end stage, this is also the case for the
    upper threshold power.
    """

    _lower_threshold_power = 5
    _upper_threshold_power = 2

    def __init__(self, progress: NegotiationProgress, profile: ProfileInterface, opponent_model: OpponentModel):
        super().__init__(progress, profile)
        self._opponent_model: OpponentModel = opponent_model

    def get_utility(self, bid: Bid) -> float:
        return self._profile.getProfile().getUtility(bid)

    """
    The lowest acceptable threshold is either equal to the reservation bid, or to a function of progress. 
    """
    def get_acceptable_lower_threshold(self):
        return max(
            self._opponent_model.peek_best_proposed_utility(),
            (1 - self.get_progress() ** self._lower_threshold_power)
        )

    """
    Selects a rational upper threshold for proposing bids. If negotiation is ending, suggests lower and lower
    bids to decrease no-deal likeliness.
    """
    def get_rational_upper_threshold(self):
        return 1 - (1 - self.get_acceptable_lower_threshold()) ** self._upper_threshold_power

    def is_acceptable_bid(self, bid: Bid) -> bool:
        if bid is None:
            return False

        if self.is_random_search_phase():
            # Do not accept any bids during this stage
            return self.get_utility(bid) == 1

        return self.get_utility(bid) > self.get_acceptable_lower_threshold()

    """
    Checks if the bid is fit to be proposed to the opponent.
    
    We propose bids that offer a higher value than the reservation bid, but decrease our greediness over time.
    """
    def is_rational_bid_proposal(self, bid: Bid):
        utility = self.get_utility(bid)
        return self.get_acceptable_lower_threshold() < utility < self.get_rational_upper_threshold()
