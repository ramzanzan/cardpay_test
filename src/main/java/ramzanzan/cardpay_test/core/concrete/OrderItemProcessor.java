package ramzanzan.cardpay_test.core.concrete;

import lombok.Getter;
import org.springframework.batch.item.ItemProcessor;
import ramzanzan.cardpay_test.model.ProcessedOrder;
import ramzanzan.cardpay_test.core.LineNumberingLineMapper;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class OrderItemProcessor implements ItemProcessor<Map<String,String>, ProcessedOrder> {

    private static final String ID = "orderId";
    private static final String AMOUNT = "amount";
    private static final String CURRENCY = "currency";
    private static final String COMMENT = "comment";
    private static final String OK = "OK";
    private static final int FIELD_COUNT = 4+1;
    private static final Set<String> CURRENCY_CODES =
            Currency.getAvailableCurrencies().stream().map(Currency::getCurrencyCode).collect(Collectors.toSet());

    private final String filename;

    public OrderItemProcessor(String filename){
        this.filename=filename;
    }

    @Override
    public ProcessedOrder process(Map<String, String> orderFields) throws Exception {
        if(orderFields.size()<FIELD_COUNT) throw new IllegalArgumentException("too few fields in record");
        ProcessedOrder order = new ProcessedOrder();
        String errMsgPrefix = "NOT OK: ";
        StringJoiner errMsgSj = new StringJoiner(" && ",errMsgPrefix,"");
        try {
            order.setId(Long.valueOf(orderFields.get(ID)));
            if(order.getId()<0)
                errMsgSj.add("orderId < 0 : "+order.getId());
        }catch (NumberFormatException e){
            errMsgSj.add("orderId not a number");
        }
        try {
            order.setAmount(new BigDecimal(orderFields.get(AMOUNT)));
            if(order.getAmount().compareTo(BigDecimal.ZERO)==-1)
                errMsgSj.add("amount < 0.0 : "+order.getAmount());
        }catch (NumberFormatException e){
            errMsgSj.add("amount not a number");
        }
        order.setCurrency(orderFields.get(CURRENCY).toUpperCase());
        if(!CURRENCY_CODES.contains(order.getCurrency()))
            errMsgSj.add("unsupported currency code : "+order.getCurrency());
        order.setComment(orderFields.get(COMMENT));
        order.setFilename(filename);
        order.setLine(Long.valueOf(orderFields.get(LineNumberingLineMapper.LINE_NUM)));
        String validation = errMsgSj.toString();
        order.setResult(validation.length()==errMsgPrefix.length() ? OK : validation);
        return order;
    }
}
