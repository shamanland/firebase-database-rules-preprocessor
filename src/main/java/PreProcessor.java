import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class PreProcessor {
    private static final Pattern pattern = Pattern.compile("\\.(\\{.*})");

    public static void main(String[] args) throws Exception {
        Reader in = args.length > 0 ? new InputStreamReader(new FileInputStream(args[0])) : new InputStreamReader(System.in);
        PrintStream out = args.length > 1 ? new PrintStream(args[1]) : System.out;

        try {
            Gson gson = new GsonBuilder()
                    .disableHtmlEscaping()
                    .setPrettyPrinting()
                    .create();

            Map<String, Object> map = gson.fromJson(in, Map.class);

            process(map, Collections.emptyMap());

            out.append(gson.toJson(map));
        } finally {
            close(in);
            close(out);
        }
    }

    private static void process(Map<String, Object> map, Map<String, String> subs) {
        List<Map> children = new LinkedList<>();

        Map<String, String> localSubs = new HashMap<>();

        for (Map.Entry<String, Object> e : map.entrySet()) {
            Matcher m = pattern.matcher(e.getKey());

            if (m.matches()) {
                localSubs.put(m.group(1), (String) e.getValue());
            } else if (e.getValue() instanceof Map) {
                children.add((Map) e.getValue());
            }
        }

        Map<String, String> mergedSubs = merge(subs, localSubs);

        substitute(map, ".read", mergedSubs);
        substitute(map, ".write", mergedSubs);
        substitute(map, ".validate", mergedSubs);

        for (String k : localSubs.keySet()) {
            map.remove("." + k);
        }

        for (Map m : children) {
            process(m, mergedSubs);
        }
    }

    private static <K, V> Map<K, V> merge(Map<K, V> map1, Map<K, V> map2) {
        Map<K, V> result = new TreeMap<>();
        result.putAll(map1);
        result.putAll(map2);
        return Collections.unmodifiableMap(result);
    }

    private static void substitute(Map<String, Object> map, String key, Map<String, String> substitutions) {
        Object v = map.get(key);
        if (v instanceof String) {
            String value = (String) v;

            for (Map.Entry<String, String> e : substitutions.entrySet()) {
                value = StringUtils.replace(value, e.getKey(), e.getValue(), -1);
            }

            map.put(key, value);
        }
    }

    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }
}
