package prip.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public interface Jsonable {
    public default String toJson() {
        try {
            return new ObjectMapper()
                    .setDateFormat(DateUtils.instance().getDateSimpleTimeFmt())
                    .writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }
}
