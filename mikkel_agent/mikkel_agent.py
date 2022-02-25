import logging
from typing import cast

from geniusweb.actions.PartyId import PartyId
from geniusweb.actions.Accept import Accept
from geniusweb.actions.Action import Action
from geniusweb.actions.Offer import Offer
from geniusweb.inform.ActionDone import ActionDone
from geniusweb.inform.Finished import Finished
from geniusweb.inform.Inform import Inform
from geniusweb.inform.Settings import Settings
from geniusweb.inform.YourTurn import YourTurn
from geniusweb.issuevalue.Bid import Bid
from geniusweb.party.Capabilities import Capabilities
from geniusweb.party.DefaultParty import DefaultParty
from geniusweb.profileconnection.ProfileConnectionFactory import ProfileConnectionFactory
from geniusweb.profileconnection.ProfileInterface import ProfileInterface
from geniusweb.progress.ProgressRounds import ProgressRounds

from agents.mikkel_agent.acceptance_strategy import AcceptanceStrategy
from agents.mikkel_agent.bidding_strategy import BiddingStrategy
from agents.mikkel_agent.helpers.negotiation_progress import NegotiationProgress
from agents.mikkel_agent.opponent_model import OpponentModel


class MikkelAgent(DefaultParty):
    """
    Select a set of good bids to propose in the exploratory section. Store all bids that the opponent submits, but
    accept none.

    If we run out of good bids, restock them.

    Once the real bidding starts:
    Keep suggesting better (optionally with ML, similar) bids to those that the other agent chose. Eventually,
    suggest the reservation bid?

    As for acceptance, accept anything that is an improvement over the reservation bid but with some notion of time.
    E.g. become less greedy and more agreeable with the proposed bids as negotiation comes to a close.
    """

    _settings: Settings or None
    _me: PartyId or None
    _progress: ProgressRounds or None
    _negotiation_progress: NegotiationProgress
    _profile: ProfileInterface or None
    _last_received_bid: Bid or None
    _bidding_strategy: BiddingStrategy
    _opponent_model: OpponentModel
    _acceptance_strategy: AcceptanceStrategy

    def __init__(self):
        super().__init__()
        self.getReporter().log(logging.INFO, "Party initialized")

    """
    The entry point of all interaction with the agent after is has been initialised.
    
    Args:
        info (Inform): Contains either a request for action or information.
    """

    def notifyChange(self, info: Inform):
        if isinstance(info, Settings):
            self._consume_settings(cast(Settings, info))
        elif isinstance(info, Finished):
            self.terminate()
        elif isinstance(info, ActionDone):
            self._consume_action(cast(ActionDone, info).getAction())
        elif isinstance(info, YourTurn):
            self._execute_turn()
            self._negotiation_progress.advance()
            self._progress = self._negotiation_progress.get_object()
        else:
            self.getReporter().log(logging.WARNING, "Ignoring unknown info " + str(info))

    def getCapabilities(self) -> Capabilities:
        return Capabilities(
            {"SAOP"},
            {"geniusweb.profile.utilityspace.LinearAdditive"},
        )

    def terminate(self):
        self.getReporter().log(logging.INFO, "Party terminating")
        super().terminate()
        if self._profile is not None:
            self._profile.close()
            self._profile = None

    def getDescription(self) -> str:
        return "Area based agent implementation."

    def _consume_settings(self, settings: Settings):
        self._settings = settings
        self._me = self._settings.getID()
        self._progress = self._settings.getProgress()
        self._negotiation_progress = NegotiationProgress(cast(ProgressRounds, self._settings.getProgress()))
        self._profile = ProfileConnectionFactory.create(settings.getProfile().getURI(), self.getReporter())
        self._opponent_model = OpponentModel(
            self._negotiation_progress,
            self._profile
        )
        self._acceptance_strategy = AcceptanceStrategy(
            self._negotiation_progress,
            self._profile,
            self._opponent_model
        )
        self._bidding_strategy = BiddingStrategy(
            self._negotiation_progress,
            self._profile,
            self._acceptance_strategy
        )

    def _consume_action(self, action: Action):
        if isinstance(action, Offer):
            if action.getActor().getName() is not self._me.getName():
                bid = cast(Offer, action).getBid()
                self._last_received_bid = bid
                self._opponent_model.record_bid(bid)

    def _execute_turn(self):
        if self._acceptance_strategy.is_acceptable_bid(self._last_received_bid):
            action = Accept(self._me, self._last_received_bid)
        else:
            bid = self._bidding_strategy.get_next_bid()
            action = Offer(self._me, bid)

        self.getConnection().send(action)
