package ramzanzan.cardpay_test.service;

import lombok.extern.flogger.Flogger;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ramzanzan.cardpay_test.core.JsonLinesItemWriter;
import ramzanzan.cardpay_test.model.ProcessedOrder;
import ramzanzan.cardpay_test.core.FilenameAwareLoggingSkipListener;
import ramzanzan.cardpay_test.core.concrete.OrderItemProcessor;
import ramzanzan.cardpay_test.util.Util;

import java.nio.charset.Charset;
import java.util.Map;

@Service
@Profile("batch")
@Flogger
public class BatchOrderProcessor implements ProcessingService{

    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    @Autowired
    private StepBuilderFactory stepBuilderFactory;
    @Autowired
    private JobLauncher jobLauncher;
    @Autowired
    private MapItemReaderFactory mapItemReaderFactory;
    private int chunkSize = 10;

    @Override
    public void process(Resource resource, Charset charset) {
        Step step = stepBuilderFactory
                .get("processOrdersStep::"+resource.getFilename())
                .<Map<String,String>,ProcessedOrder>chunk(chunkSize)
                .reader(mapItemReaderFactory.get(resource,charset,Util.csvSchemaToMap(new ClassPathResource("order.csv.schema"))))
                .processor(new OrderItemProcessor(resource.getFilename()))
                .writer(new JsonLinesItemWriter<>(Util.getBufferedUncloseableWriter(System.out), ProcessedOrder.class))
                .faultTolerant()
                .skipLimit(Integer.MAX_VALUE)
                .skip(Exception.class)
                .listener(new FilenameAwareLoggingSkipListener(resource.getFilename()))
                .build();
        Job job = jobBuilderFactory
                .get("processOrdersJob::"+resource.getFilename())
                .start(step)
                .listener(new JobExecutionListener() {
                    @Override
                    public void beforeJob(JobExecution jobExecution) { }
                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        log.atInfo().log(jobExecution.toString());
                    }
                })
                .build();
        try {
            JobExecution je  = jobLauncher.run(job, new JobParameters());
        } catch (JobExecutionException e) {
            log.atSevere().withCause(e).log("can't process file: "+resource.getFilename());
        }
    }



}
