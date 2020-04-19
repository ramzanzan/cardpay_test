package ramzanzan.cardpay_test.config;

import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.context.annotation.*;

@Configuration
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
}
