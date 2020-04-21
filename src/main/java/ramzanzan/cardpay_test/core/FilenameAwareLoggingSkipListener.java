package ramzanzan.cardpay_test.core;

import lombok.extern.flogger.Flogger;
import org.springframework.batch.core.SkipListener;

@Flogger
public class FilenameAwareLoggingSkipListener implements SkipListener {

    private String filename;
    public FilenameAwareLoggingSkipListener(String filename){
        this.filename =filename;
    }

    @Override
    public void onSkipInRead(Throwable t) {
        log.atSevere().withCause(t).log("file: "+ filename+" :: error while reading");
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        log.atSevere().withCause(t).log("file: "+ filename+" :: error while processing, item: "+item);
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        log.atSevere().withCause(t).log("file: "+ filename+" :: error while writing, item: "+item);
    }
}
