package ramzanzan.cardpay_test.config;

import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("batch")
public class BatchSolutionConfig {

    @Bean
    public BatchConfigurer batchConfigurer(){
        return new DefaultBatchConfigurer(){
            @Override
            protected JobRepository createJobRepository() throws Exception {
                MapJobRepositoryFactoryBean factory = new MapJobRepositoryFactoryBean();
                return factory.getObject();
            }
        };
    }

    @Bean
    public JobBuilderFactory jobBuilderFactory(BatchConfigurer batchConfigurer) throws Exception {
        return new JobBuilderFactory(batchConfigurer.getJobRepository());
    }

    @Bean
    public StepBuilderFactory stepBuilderFactory(BatchConfigurer batchConfigurer) throws Exception {
        return new StepBuilderFactory(batchConfigurer.getJobRepository(), batchConfigurer.getTransactionManager());
    }

    @Bean
    public JobLauncher jobLauncher(BatchConfigurer batchConfigurer) throws Exception {
        return batchConfigurer.getJobLauncher();
    }

}
