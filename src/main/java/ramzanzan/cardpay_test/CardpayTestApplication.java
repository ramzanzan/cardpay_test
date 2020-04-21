package ramzanzan.cardpay_test;

import lombok.extern.flogger.Flogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import ramzanzan.cardpay_test.config.BatchSolutionConfig;
import ramzanzan.cardpay_test.service.BatchOrderProcessor;
import ramzanzan.cardpay_test.service.ProcessingService;

import java.nio.charset.StandardCharsets;

@Flogger
@SpringBootApplication(
        scanBasePackageClasses = {BatchSolutionConfig.class, BatchOrderProcessor.class},
        exclude = {BatchAutoConfiguration.class, DataSourceAutoConfiguration.class })
public class CardpayTestApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(CardpayTestApplication.class, args);
    }

    @Autowired
    public ApplicationContext context;

    @Override
    public void run(String... args) throws Exception {
        ProcessingService ps = context.getBean(ProcessingService.class);
        for(String path : args) {
            FileSystemResource file = new FileSystemResource(path);
            if(!file.exists()){
                log.atSevere().log("file: "+file.getFilename()+" doesn't exist");
                continue;
            }
            if(!file.isReadable()){
                log.atSevere().log("file: "+file.getFilename()+" isn't writable");
                continue;
            }
            ps.process(new FileSystemResource(path), StandardCharsets.UTF_8);
        }
    }
}

//todo shrink stacktraces in logging;
