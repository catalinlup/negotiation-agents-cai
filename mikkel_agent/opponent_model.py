from geniusweb.issuevalue.Bid import Bid
from geniusweb.profileconnection.ProfileInterface import ProfileInterface

from agents.mikkel_agent.boa_element import BoaElement
from agents.mikkel_agent.helpers.negotiation_progress import NegotiationProgress


class OpponentModel(BoaElement):

    _proposed_bids: [(Bid, float)] = []

    def __init__(self, progress: NegotiationProgress, profile: ProfileInterface):
        super().__init__(progress, profile)

    def record_bid(self, bid: Bid):
        self._proposed_bids.append((bid, self.get_utility(bid)))

    def peek_best_proposed_utility(self) -> float:
        return self._get_best_proposed_bid()[1]

    def get_best_proposed_bid(self) -> Bid:
        best_entry = self._get_best_proposed_bid()
        self._proposed_bids.remove(best_entry)
        return best_entry[0]

    def _get_best_proposed_bid(self) -> (Bid, float):
        return max(self._proposed_bids, key=lambda proposal: proposal[1])

