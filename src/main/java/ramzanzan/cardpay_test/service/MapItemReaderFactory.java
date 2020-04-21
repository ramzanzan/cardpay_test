package ramzanzan.cardpay_test.service;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.DefaultBufferedReaderFactory;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ramzanzan.cardpay_test.core.JsonLineNumberingLineMapper;
import ramzanzan.cardpay_test.core.LineNumberingLineMapper;
import ramzanzan.cardpay_test.core.MapFieldSetMapper;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.Map;

@Service
public class MapItemReaderFactory {
    public enum FileExtension {
        JSON,
        CSV,
        UNKNOWN
    }

    @Value("${app.processing.csv.separator}")
    private String flatFileSeparator = ",";
    @Value("${app.processing.csv.quote}")
    private String flatFileQuote;
    @Value("${app.processing.default-file-extension}")
    private String fileDefaultExtension = "csv";

    @PostConstruct
    private void init(){
        if(flatFileSeparator.isEmpty()) throw new BeanCreationException("empty app.processing.csv.separator");
        if(fileDefaultExtension.isEmpty()) throw new BeanCreationException("empty app.processing.csv.default-file-extension");
        if(flatFileQuote.length()>1) throw new BeanCreationException("app.processing.csv.quote.length can't be > 1");
    }

    public ItemReader<Map<String,String>> get(Resource resource, Charset charset, Map<Integer,String> colNumToFieldName){
        FileExtension fe = getFileExtension(resource.getFilename());
        if(fe==FileExtension.JSON || fe==FileExtension.CSV) {
            LineMapper<Map<String, String>> mapper = null;
            if (fe == FileExtension.JSON) {
                mapper = new JsonLineNumberingLineMapper();
            }else {
                DefaultLineMapper<Map<String,String>> inrMapper = new DefaultLineMapper<>();
                DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();//todo
                tokenizer.setDelimiter(flatFileSeparator);
                if(!flatFileQuote.isEmpty()) tokenizer.setQuoteCharacter(flatFileQuote.charAt(0));
                inrMapper.setLineTokenizer(tokenizer);
                inrMapper.setFieldSetMapper(new MapFieldSetMapper(colNumToFieldName));
                mapper = new LineNumberingLineMapper(inrMapper);
            }
            return new FlatFileItemReaderBuilder<Map<String,String>>()
                    .saveState(false)
                    .bufferedReaderFactory(new DefaultBufferedReaderFactory())
                    .encoding(charset.name())
                    .lineMapper(mapper)
                    .resource(resource)
                    .build();
        }else
            throw new IllegalArgumentException("Unsupported file extension for file: "+resource.getFilename());
    }



    private FileExtension getFileExtension(String filename){
        int idx = filename.lastIndexOf(".");
        String ext = idx>=0 ? filename.substring(idx+1) : fileDefaultExtension;
        return FileExtension.valueOf(ext.toUpperCase());
    }
}
