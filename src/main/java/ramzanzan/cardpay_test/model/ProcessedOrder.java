package ramzanzan.cardpay_test.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessedOrder extends Order{
    private String filename;
    private Long line;
    private String result;
}
