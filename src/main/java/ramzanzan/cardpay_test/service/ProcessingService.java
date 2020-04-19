package ramzanzan.cardpay_test.service;

import org.springframework.core.io.Resource;

import java.nio.charset.Charset;

public interface ProcessingService {
    void process(Resource source, Charset charset);
}
