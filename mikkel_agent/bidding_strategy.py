import random
from itertools import takewhile
from typing import Dict, List

from geniusweb.bidspace.AllBidsList import AllBidsList
from geniusweb.issuevalue.Bid import Bid
from geniusweb.profileconnection.ProfileInterface import ProfileInterface

from agents.mikkel_agent.acceptance_strategy import AcceptanceStrategy
from agents.mikkel_agent.boa_element import BoaElement
from agents.mikkel_agent.helpers.negotiation_progress import NegotiationProgress


def build_sorted_bid_list(bid_map: Dict[Bid, float]) -> List[Bid]:
    return list(
        map(
            lambda entry: entry[0],
            sorted(
                [(bid, utility) for bid, utility in bid_map.items()],
                key=lambda entry: entry[1],
                reverse=True
            )
        )
    )


class BiddingStrategy(BoaElement):
    """
    Propose bids that are good for us randomly, record overlap with the other agent
    """

    _min_search_utility_range = 0.8

    def __init__(
            self,
            progress: NegotiationProgress,
            profile: ProfileInterface,
            acceptance_strategy: AcceptanceStrategy
    ):
        super().__init__(progress, profile)
        self._bid_map = self._build_bid_map()
        self._sorted_bid_array = build_sorted_bid_list(self._bid_map)
        self._min_search_utility = self._get_min_search_utility()
        self._good_bids = self._build_good_bids()
        self._acceptance_strategy = acceptance_strategy

    def get_next_bid(self):
        if self.is_random_search_phase():
            return self._get_next_exploration_bid()
        return self._get_next_negotiation_bid()

    def _any_good_bids_remaining(self) -> bool:
        return len(self._good_bids) != 0

    def _get_next_exploration_bid(self) -> Bid:
        if not self._any_good_bids_remaining():
            self._good_bids = self._build_good_bids()

        selected_bid = self._good_bids[random.randint(0, (len(self._good_bids) - 1))]
        self._good_bids.remove(selected_bid)
        return selected_bid

    def _get_next_negotiation_bid(self) -> Bid:
        acceptable_bids = list(
            filter(lambda bid: self._acceptance_strategy.is_rational_bid_proposal(bid), self._sorted_bid_array)
        )

        return acceptable_bids[random.randint(0, len(acceptable_bids)-1)]

    def _build_good_bids(self) -> List[Bid]:
        return list(takewhile(lambda x: self._bid_map[x] >= self._min_search_utility, self._sorted_bid_array))

    def _build_bid_map(self) -> Dict[Bid, float]:
        return {bid: self.get_utility(bid) for bid in AllBidsList(self._profile.getProfile().getDomain())}

    def _get_min_search_utility(self):
        return self.get_utility(
            self._sorted_bid_array[int((1 - self._min_search_utility_range) * len(self._sorted_bid_array))]
        )
