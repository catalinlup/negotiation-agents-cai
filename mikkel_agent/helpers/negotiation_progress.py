from geniusweb.progress.ProgressRounds import ProgressRounds


class NegotiationProgress:

    def __init__(self, progress: ProgressRounds):
        self._progress = progress

    def advance(self):
        self._progress = self._progress.advance()

    def get_progress(self) -> float:
        return self._progress.get(0)

    def get_object(self) -> ProgressRounds:
        return self._progress

