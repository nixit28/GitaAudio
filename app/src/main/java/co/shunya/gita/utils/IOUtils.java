package co.shunya.gita.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class IOUtils {

	public static String getStringFromInputStream(InputStream inputStream) {
        try {
			BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
			StringBuilder total = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				total.append(line);
			}
			return total.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;

	}
	
	public static <T extends Enum<T>> String[] enumNameToStringArray(T[] values) {  
	    int i = 0;  
	    String[] result = new String[values.length];  
	    for (T value: values) {  
	        result[i++] = value.toString();  
	    }  
	    return result;  
	}  
}
