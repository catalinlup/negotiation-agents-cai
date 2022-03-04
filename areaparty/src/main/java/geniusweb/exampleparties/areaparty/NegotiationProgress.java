package geniusweb.exampleparties.areaparty;

import geniusweb.progress.Progress;
import geniusweb.progress.ProgressRounds;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NegotiationProgress {

    private Progress progress;

    public double getProgress() {
        if (this.progress == null) {
            return 0;
        }
        return this.progress.get(System.currentTimeMillis());
    }

    public void advance() {
        this.progress = this.getProgressRounds().advance();
    }

    private ProgressRounds getProgressRounds() {
        return (ProgressRounds) progress;
    }
}
