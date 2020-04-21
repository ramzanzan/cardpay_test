package ramzanzan.cardpay_test.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ramzanzan.cardpay_test.core.*;
import ramzanzan.cardpay_test.core.concrete.OrderItemProcessor;
import ramzanzan.cardpay_test.model.ProcessedOrder;
import ramzanzan.cardpay_test.util.*;

import java.nio.charset.Charset;
import java.util.Map;

@Service
@Profile("homemade")
public class ProcessingPipeOrderProcessor implements ProcessingService{

    private int inputQueueSize = 16;
    private int threadsCount = Runtime.getRuntime().availableProcessors();
    private int outputQueueSize = inputQueueSize*threadsCount;
    @Autowired
    private MapItemReaderFactory mapItemReaderFactory;

    @Override
    public void process(Resource resource, Charset charset) {
        ItemProcessingPipeline pipeline = ItemProcessingPipeline.Builder
                .<Map<String,String>,ProcessedOrder>get(threadsCount,inputQueueSize,outputQueueSize)
                .id("file: "+resource.getFilename())
                .reader(mapItemReaderFactory.get(resource,charset,Util.csvSchemaToMap(new ClassPathResource("order.csv.schema"))))
                .addProcessor(new OrderItemProcessor(resource.getFilename()))
                .writer(new JsonLinesItemWriter<>(Util.getBufferedUncloseableWriter(System.out), ProcessedOrder.class))
                .build();
        pipeline.start();
        pipeline.waitFor();
    }
}
