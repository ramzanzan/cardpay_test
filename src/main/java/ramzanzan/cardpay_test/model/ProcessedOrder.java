package ramzanzan.cardpay_test.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString(callSuper = true)
public class ProcessedOrder{
    private Long id;
    private BigDecimal amount;
    private String currency;
    private String comment;
    private String filename;
    private Long line;
    private String result;
}
