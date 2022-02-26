import json
import logging
from random import randint, randrange, uniform
from typing import cast

from geniusweb.actions.Accept import Accept
from geniusweb.actions.Action import Action
from geniusweb.actions.Offer import Offer
from geniusweb.actions.PartyId import PartyId
from geniusweb.bidspace.AllBidsList import AllBidsList
from geniusweb.inform.ActionDone import ActionDone
from geniusweb.inform.Finished import Finished
from geniusweb.inform.Inform import Inform
from geniusweb.inform.Settings import Settings
from geniusweb.inform.YourTurn import YourTurn
from geniusweb.issuevalue.Bid import Bid
from geniusweb.issuevalue.Domain import Domain
from geniusweb.issuevalue.Value import Value
from geniusweb.issuevalue.ValueSet import ValueSet
from geniusweb.party.Capabilities import Capabilities
from geniusweb.party.DefaultParty import DefaultParty
from geniusweb.profile.utilityspace.LinearAdditiveUtilitySpace import \
    LinearAdditiveUtilitySpace
from geniusweb.profile.utilityspace.UtilitySpace import UtilitySpace
from geniusweb.profileconnection.ProfileConnectionFactory import \
    ProfileConnectionFactory
from geniusweb.progress.ProgressRounds import ProgressRounds

STRATEGY_PATH = '/home/catalinlup/MyWorkspace/ColabAI/TU-Delft-Collaborative-AI-Negotiation/agents/improved_time_dependent_agent/strategies/strategy2.json'


class ImprovedTimeDependentAgent(DefaultParty):
    """
    Template agent that offers random bids until a bid with sufficient utility is offered.
    """

    def __init__(self):
        super().__init__()
        self.getReporter().log(logging.INFO, "party is initialized")
        self._profile = None
        self._oponent_profile = None
        self._bids_table = None
        self._last_received_bid: Bid = None

        self._bidding_scheme = json.load(open(STRATEGY_PATH, 'r'))


    def notifyChange(self, info: Inform):
        """This is the entry point of all interaction with your agent after is has been initialised.

        Args:
            info (Inform): Contains either a request for action or information.
        """

        # a Settings message is the first message that will be send to your
        # agent containing all the information about the negotiation session.
        if isinstance(info, Settings):
            self._settings: Settings = cast(Settings, info)
            self._me = self._settings.getID()


            # progress towards the deadline has to be tracked manually through the use of the Progress object
            self._progress: ProgressRounds = self._settings.getProgress()

            # the profile contains the preferences of the agent over the domain
            self._profile = ProfileConnectionFactory.create(
                info.getProfile().getURI(), self.getReporter()
            )

            self._bids_table = self._generate_bids_table(0.01)

            

        # ActionDone is an action send by an opponent (an offer or an accept)
        elif isinstance(info, ActionDone):
            action: Action = cast(ActionDone, info).getAction()

            # if it is an offer, set the last received bid
            if isinstance(action, Offer):
                self._last_received_bid = cast(Offer, action).getBid()
        # YourTurn notifies you that it is your turn to act
        elif isinstance(info, YourTurn):
            # execute a turn
            self._myTurn()

            # log that we advanced a turn
            self._progress = self._progress.advance()

        # Finished will be send if the negotiation has ended (through agreement or deadline)
        elif isinstance(info, Finished):
            # terminate the agent MUST BE CALLED
            self.terminate()
        else:
            self.getReporter().log(
                logging.WARNING, "Ignoring unknown info " + str(info)
            )

    # lets the geniusweb system know what settings this agent can handle
    # leave it as it is for this course
    def getCapabilities(self) -> Capabilities:
        return Capabilities(
            set(["SAOP"]),
            set(["geniusweb.profile.utilityspace.LinearAdditive"]),
        )

    # terminates the agent and its connections
    # leave it as it is for this course
    def terminate(self):
        self.getReporter().log(logging.INFO, "party is terminating:")
        super().terminate()
        if self._profile is not None:
            self._profile.close()
            self._profile = None

    #######################################################################################
    ########## THE METHODS BELOW THIS COMMENT ARE OF MAIN INTEREST TO THE COURSE ##########
    #######################################################################################

    # give a description of your agent
    def getDescription(self) -> str:
        return "Template agent for Collaborative AI course"

    # execute a turn
    def _myTurn(self):
        # check if the last received offer if the opponent is good enough
        if self._isGood(self._last_received_bid):
            # if so, accept the offer
            action = Accept(self._me, self._last_received_bid)
        else:
            # if not, find a bid to propose as counter offer
            bid = self._findBid()
            action = Offer(self._me, bid)

        # send the action
        self.getConnection().send(action)

    # method that checks if we would agree with an offer
    def _isGood(self, bid: Bid) -> bool:
        if bid is None:
            return False
        profile = self._profile.getProfile()

        progress = self._progress.get(0)

        # very basic approach that accepts if the offer is valued above 0.6 and
        # 80% of the rounds towards the deadline have passed
        return profile.getUtility(bid) > 0.6 and progress > 0.8

    def _findBid(self) -> Bid:
        # compose a list of all possible bids
        domain = self._profile.getProfile().getDomain()
        all_bids = AllBidsList(domain)
        progress = self._progress.get(0)
        

        bid = self._bids_table[self._bidding_scheme['upper_limit']][0]

        for rule in self._bidding_scheme['rules']:
            if progress >= rule['min_progress'] and progress < rule['max_progress']:
                if rule['type'] == 'polynomial':
                    utility = self._compute_polynomial_utility(
                        progress, rule['min_progress'], rule['max_utility'], rule['max_progress'], rule['min_utility'], rule['degree'])
                    
                    utility = max(utility, self._bidding_scheme['lower_limit'])
                    utility = self._to_bucket_entry(utility, 0.01)
                    

                    if len(self._bids_table[utility]) > 0:
                        bid = self._bids_table[utility][0]
                    
                elif rule['type'] == 'random':
                    utility = uniform(rule['min_utility'], rule['max_utility'])
                    utility = max(utility, self._bidding_scheme['lower_limit'])
                    utility = self._to_bucket_entry(utility, 0.01)

                    bid = self._bids_table[utility][0]

        profile = self._profile.getProfile()
        print(bid, profile.getUtility(bid))
        return bid


        


    def _compute_polynomial_utility(self, progress, p1, u1, p2, u2, n):
        a = (u1 - u2) / (p1 ** n - p2 ** n)
        b = u1 - a * (p1 ** n)
        # 0.2 / (0.1 ^ 3 - 0.5 ^ 3) * 0.1 + 
      
        return a * (progress ** n) + b

    def _to_bucket_entry(self, value, precision) -> float:
        for k in range(0, int(1 / precision)):
            
            if value >= k * precision and value < (k + 1) * precision:
                return round(k * precision, int(1 / precision))
        raise Exception('Could not be cast to a bucket ' + str(value))

    
    def _generate_bids_table(self, precision) -> dict:
      domain = self._profile.getProfile().getDomain()
      profile = self._profile.getProfile()
      all_bids = AllBidsList(domain)

      bids_table = {}

      for k in range(0, int(1 / precision)):
        bids_table[k * precision] = []


      for bid in all_bids:
        utility = profile.getUtility(bid)

        for k in range(0, int(1 / precision)):
          if utility >= k * precision and utility < (k + 1) * precision:
            bids_table[round(k * precision, int(1 / precision))].append(bid)

      
      return bids_table
            



