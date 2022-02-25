from geniusweb.issuevalue.Bid import Bid
from geniusweb.profileconnection.ProfileInterface import ProfileInterface

from agents.mikkel_agent.helpers.negotiation_progress import NegotiationProgress


class BoaElement:

    _random_search_phase = 0.75

    def __init__(self, progress: NegotiationProgress, profile: ProfileInterface):
        self._progress = progress
        self._profile = profile

    def get_utility(self, bid: Bid) -> float:
        return self._profile.getProfile().getUtility(bid)

    def get_progress(self) -> float:
        return self._progress.get_progress()

    def is_random_search_phase(self) -> bool:
        return self.get_progress() < self._random_search_phase
