package ramzanzan.cardpay_test.core;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import org.apache.logging.log4j.util.Strings;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.List;

public class JsonLinesItemWriter<T> implements ItemWriter<T>, ItemStream {
    private static Gson gson = new Gson();

    private JsonWriter jsonWriter;
    private Writer writer;
    private Type type;
    public JsonLinesItemWriter(Writer writer, Type type){
        this.jsonWriter = new JsonWriter(writer);
        this.writer = writer;
        this.type = type;
    }

    @Override
    public void write(List<? extends T> items) throws Exception {
        for(T item : items){
            gson.toJson(item,type, jsonWriter);
            writer.write(Strings.LINE_SEPARATOR);
        }
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        //no-op
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        //no-op
    }

    @Override
    public void close() throws ItemStreamException {
        try {
            jsonWriter.flush();
            jsonWriter.close();
            writer.close();
        } catch (IOException e) {
            throw new ItemStreamException(e);
        }
    }
}
