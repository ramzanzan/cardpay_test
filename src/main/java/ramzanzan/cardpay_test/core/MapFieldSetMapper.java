package ramzanzan.cardpay_test.core;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

import java.util.HashMap;
import java.util.Map;

public class MapFieldSetMapper implements FieldSetMapper<Map<String,String>> {

    private Map<Integer,String> columnToKey;
    public MapFieldSetMapper(Map<Integer,String> columnNumToMapKey){
        columnToKey=columnNumToMapKey;
    }

    @Override
    public HashMap<String, String> mapFieldSet(FieldSet fieldSet) throws BindException {
        HashMap<String,String> map = new HashMap<>(columnToKey.size());
        for(Integer col : columnToKey.keySet())
            map.put(columnToKey.get(col),fieldSet.readString(col));
        return map;
    }
}
