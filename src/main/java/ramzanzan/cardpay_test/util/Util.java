package ramzanzan.cardpay_test.util;

import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class Util {

    public static Writer getBufferedUncloseableWriter(OutputStream stream){
        class UncloseableWriter extends OutputStreamWriter{
            private UncloseableWriter(OutputStream out) {
                super(out);
            }
            @Override
            public void close() throws IOException { /*no-op*/ }
        }
//        return new UncloseableWriter(stream);
        return new BufferedWriter(new UncloseableWriter(stream));
    }

    public static Map<Integer,String> csvSchemaToMap(Resource resource){
        try {
            File f = resource.getFile();
            String line = new String(Files.readAllBytes(f.toPath()));
            String[] fields = line.split(",");
            HashMap<Integer, String> map = new HashMap<>(fields.length);
            for (int i = 0; i < fields.length; i++)
                map.put(i,fields[i]);
            return map;
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

}
