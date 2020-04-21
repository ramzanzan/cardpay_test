package ramzanzan.cardpay_test.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.batch.item.file.LineMapper;

import java.lang.reflect.Type;
import java.util.Map;

public class JsonLineNumberingLineMapper implements LineMapper<Map<String, String>> {


    private static final Gson gson = new Gson();
    private static final Type type = new TypeToken<Map<String, String>>() {
    }.getType();
    public static final String LINE_NUM = "LINE_NUM";

    @Override
    public Map<String, String> mapLine(String line, int lineNumber) throws Exception {
        Map<String, String> map = gson.fromJson(line, type);
        map.put(LINE_NUM, String.valueOf(lineNumber));
        return map;
    }
}
