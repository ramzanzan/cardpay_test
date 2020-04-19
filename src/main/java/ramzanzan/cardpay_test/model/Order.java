package ramzanzan.cardpay_test.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Currency;

@Getter
@Setter
public class Order {
    private Long id;
    private BigDecimal amount;
    private Currency currency;
    private String comment;
}
