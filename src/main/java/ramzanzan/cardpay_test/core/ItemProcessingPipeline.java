package ramzanzan.cardpay_test.core;

import lombok.SneakyThrows;
import lombok.extern.flogger.Flogger;
import org.springframework.batch.item.*;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Flogger
public class ItemProcessingPipeline {

    private static final String ID = "ID: ";
    private static final String PROCESSING_ERR = " :: processing error while";

    private String id;
    
    private ItemReader reader;
    private ItemWriter writer;
    private List<ItemStream> itemStreams = new LinkedList<>();
    private List<ItemProcessor> processors = new LinkedList<>();
    private ExecutorService readingExecutor, processingExecutor, writingExecutor,
    managingExecutor = Executors.newSingleThreadExecutor();
    private int procThreadCount;
    private ArrayBlockingQueue inputQueue;
    private ArrayBlockingQueue outputQueue;
    private final Object stopToken = new Object();
    private final ExecutionContext execContext = new ExecutionContext();
    
    private long readAllCount, readFailCount, writeAllCount, writeFailCnt;
    private AtomicLong procSuccCount = new AtomicLong(), procFailCount = new AtomicLong();
    private Future managing;
    private boolean started, completed;
    private LocalDateTime startDate, endDate;

    private ItemProcessingPipeline(){}

    public Future start(){
        if(started) throw new IllegalStateException("ProcessingPipe can't be started twice");
        startDate = LocalDateTime.now();
        started=true;
        itemStreams.forEach(itemStream -> itemStream.open(execContext));

        Future writing = writingExecutor.submit(this::writeItems);
        Future[] processing = new Future[procThreadCount];
        for(int i = 0; i< procThreadCount; i++)
            processing[i] = processingExecutor.submit(this::processItems);
        Future reading = readingExecutor.submit(this::readItems);
        managing = managingExecutor.submit(()->{
            try {
                reading.get();
                for(Future proc : processing)
                    proc.get();
                writing.get();
                readingExecutor.shutdown();
                processingExecutor.shutdown();
                writingExecutor.shutdown();
            } catch (InterruptedException | ExecutionException e) {
                shutdownNow();
            } finally {
                itemStreams.forEach(ItemStream::close);
                managingExecutor.shutdown();
                completed=true;
                endDate = LocalDateTime.now();
                log.atInfo().log(getReport());
            }
        });
        return managing;
    }

    private static class Envelop {
        public Object data;
        public long itemNumber;
        Envelop(Object _data, long _itemNum){
            data=_data;
            itemNumber =_itemNum;
        }
    }

    private void readItems(){
        Thread thread = Thread.currentThread();
        for(;;){
            try {
                if(thread.isInterrupted()) throw new InterruptedException();
                readAllCount++;
                Object item = reader.read();
                if(item==null){
                    readAllCount--;
                    for(int i = 0; i< procThreadCount; i++)
                        inputQueue.put(stopToken);
                    break;
                }
                inputQueue.put(new Envelop(item,readAllCount));
            } catch (InterruptedException e) {
                shutdownNow();
                log.atSevere().withCause(e).log(getIdString()+PROCESSING_ERR+" reading :: was interrupted");
                break;
            } catch (Exception e) {
                readFailCount++;
                log.atSevere().withCause(e).log(getIdString()+PROCESSING_ERR+" reading :: "
                        +"at record №: "+readAllCount);
            }
        }
    }

    private void processItems(){
        long failCount=0, succCount=0;
        Thread thread = Thread.currentThread();
        Envelop envelop = null;
        OUTER:
        for (;;){
            try {
                if(thread.isInterrupted()) throw new InterruptedException();
                Object item = inputQueue.take();
                if(item==stopToken) {
                    outputQueue.put(item);
                    break;
                }
                envelop = (Envelop)item;
                item = envelop.data;
                for(ItemProcessor proc : processors) {
                    item = proc.process(item);
                    if(item==null) {
                        succCount++;
                        continue OUTER;
                    }
                }
                succCount++;
                outputQueue.put(item);
            } catch (InterruptedException e) {
                try { outputQueue.put(stopToken); } catch (Exception ex) { shutdownNow(); }
                log.atSevere().withCause(e).log(getIdString()+PROCESSING_ERR+" processing :: was interrupted");
                break;
            } catch (Exception e) {
                failCount++;
                log.atSevere().withCause(e).log(getIdString()+PROCESSING_ERR+" reading :: "
                        +"in at record №: "+envelop.itemNumber);
            }
        }
        procSuccCount.addAndGet(succCount);
        procFailCount.addAndGet(failCount);
    }

    private void writeItems(){
        int stopTokenCount = 0;
        Thread thread = Thread.currentThread();
        for(;;) {
            try {
                if(thread.isInterrupted()) throw new InterruptedException();
                Object item = outputQueue.take();
                if (item==stopToken) {
                    if (++stopTokenCount >= procThreadCount) break;
                    continue;
                }
                writeAllCount++;
                writer.write(Collections.singletonList(item));
            } catch (InterruptedException e) {
                log.atSevere().withCause(e).log(getIdString()+PROCESSING_ERR+" writing :: was interrupted");
                break;
            } catch (Exception e) {
                writeFailCnt++;
                log.atSevere().withCause(e).log(getIdString()+PROCESSING_ERR+" writing :: "
                        +"at record №: "+writeAllCount);
            }
        }
    }

    public void shutdownNow(){
        readingExecutor.shutdownNow();
        processingExecutor.shutdownNow();
        writingExecutor.shutdownNow();
        completed=true;
    }

    public void waitFor(){
        try {
            managing.get();
        } catch (Exception ignored) {}
    }

    public boolean isDone(){
        return managing.isDone();
    }

    public String getReport(){
        StringBuilder sb = new StringBuilder();
        sb.append("Execution summary :: ")
                .append(getIdString())
                .append(", startDate: ").append(startDate)
                .append(", endDate: ").append(endDate)
                .append(", elapsed time: ").append(ChronoUnit.MILLIS.between(startDate,endDate)).append(" mills")
                .append(", read items:: success: ").append(readAllCount-readFailCount).append(" fail: ").append(readFailCount)
                .append(", processed items:: success: ").append(procSuccCount.get()).append(" fail: ").append(procFailCount.get())
                .append(", writen items:: success: ").append(writeAllCount-writeFailCnt).append(" fail: ").append(writeFailCnt);
        return sb.toString();
    }

    private String getIdString(){
        return ID+id;
    }

    public static class Builder<R, W>{
        @SuppressWarnings("uncheked")
        public static <R,W> Builder<R,W> get(int processingThreadsCount, int inputQueueSize,  int outputQueueSize){
            if(inputQueueSize<1) throw new IllegalArgumentException("inputQueueSize < 1");
            if(outputQueueSize<1) throw new IllegalArgumentException("outputQueueSize < 1");
            if(processingThreadsCount<1) throw new IllegalArgumentException("processingThreadsCount < 1");

            Builder<R,W> b = new Builder<>();
            b.pipe = new ItemProcessingPipeline();
            b.pipe.procThreadCount = processingThreadsCount;
            b.pipe.inputQueue =new ArrayBlockingQueue(inputQueueSize);
            b.pipe.outputQueue =new ArrayBlockingQueue(outputQueueSize);
            b.pipe.readingExecutor =Executors.newSingleThreadExecutor();
            b.pipe.writingExecutor =Executors.newSingleThreadExecutor();
            b.pipe.processingExecutor =Executors.newFixedThreadPool(processingThreadsCount);
            return b;
        }

        private enum State{
            CONFIG_OR_READER("name or reader"),
            READER("reader"),
            PROCESSOR_OR_WRITER("processor or writer"),
            READY("ready to build");

            String msg;
            State(String msg){this.msg=msg;}

            @Override
            public String toString() {
                return msg;
            }
        }
        private State state = State.CONFIG_OR_READER;
        private ItemProcessingPipeline pipe;

        public Builder<R,W> id(String id){
            if(state!=State.CONFIG_OR_READER) throw new IllegalStateException("Builder wait for "+state.toString());
            pipe.id=id;
            return this;
        }
        
        @SuppressWarnings("uncheked")
        public <T> Builder<T,?> reader(ItemReader<T> reader){
            if(state!=State.CONFIG_OR_READER) throw new IllegalStateException("Builder wait for "+state.toString());
            if(reader==null) throw new NullPointerException();
            if(reader instanceof ItemStream) pipe.itemStreams.add((ItemStream)reader);
            pipe.reader = reader;
            state=State.PROCESSOR_OR_WRITER;
            return (Builder<T, ?>) this;
        }

        public <T> Builder<R,T> addProcessor(ItemProcessor<R, T> processor){
            if(state!=State.PROCESSOR_OR_WRITER) throw new IllegalStateException("Builder wait for "+state.toString());
            if(processor==null) throw new NullPointerException();
            if(processor instanceof ItemStream) pipe.itemStreams.add((ItemStream)processor);
            pipe.processors.add(processor);
            return (Builder<R,T>) this;
        }

        public Builder<R, W> writer(ItemWriter<W> writer){
            if(state!=State.PROCESSOR_OR_WRITER) throw new IllegalStateException("Builder wait for "+state.toString());
            if(writer==null) throw new NullPointerException();
            if(writer instanceof ItemStream) pipe.itemStreams.add((ItemStream)writer);
            pipe.writer = writer;
            state=State.READY;
            return this;
        }

        public ItemProcessingPipeline build(){
            if(state!=State.READY) throw new IllegalStateException("Builder wait for "+state.toString());
            return pipe;
        }
    }
}
