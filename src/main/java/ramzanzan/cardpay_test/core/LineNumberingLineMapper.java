package ramzanzan.cardpay_test.core;

import lombok.Setter;
import org.springframework.batch.item.file.LineMapper;

import java.util.Map;

@Setter
public class LineNumberingLineMapper implements LineMapper<Map<String,String>> {


    public static final String LINE_NUM = "LINE_NUM";
    private LineMapper<Map<String,String>> mapper;

    public LineNumberingLineMapper(LineMapper<Map<String,String>> mapper){
        this.mapper=mapper;
    }

    @Override
    public Map<String, String> mapLine(String line, int lineNumber) throws Exception {
        Map<String,String> map = mapper.mapLine(line,lineNumber);
        if(map.containsKey(LINE_NUM)) throw new IllegalStateException("Already has "+LINE_NUM);
        map.put(LINE_NUM,String.valueOf(lineNumber));
        return map;
    }
}
