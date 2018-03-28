import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.*;

@SuppressWarnings("unchecked")
public class PreProcessor {
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

    private static void process(Map<String, Object> map, Map<Character, String> subs) {
        List<Map> children = new LinkedList<>();

        Map<Character, String> localSubs = new HashMap<>();

        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (e.getKey().startsWith("..")) {
                if (e.getKey().length() != 3) {
                    throw new IllegalStateException("Macro '" + e.getKey() + "' is too long");
                }

                localSubs.put(e.getKey().charAt(2), (String) e.getValue());
            } else if (e.getValue() instanceof Map) {
                children.add((Map) e.getValue());
            }
        }

        Map<Character, String> mergedSubs = merge(subs, localSubs);

        substitute(map, ".read", mergedSubs);
        substitute(map, ".write", mergedSubs);
        substitute(map, ".validate", mergedSubs);

        for (Character k : localSubs.keySet()) {
            map.remove(".." + k);
        }

        for (Map m : children) {
            process(m, mergedSubs);
        }
    }

    private static <K, V> Map<K, V> merge(Map<K, V> map1, Map<K, V> map2) {
        Map<K, V> result = new HashMap<>();
        result.putAll(map1);
        result.putAll(map2);
        return Collections.unmodifiableMap(result);
    }

    private static void substitute(Map<String, Object> map, String key, Map<Character, String> substitutions) {
        Object v = map.get(key);
        if (v instanceof String) {
            String value = (String) v;

            StringBuilder result = new StringBuilder();

            for (int i = 0, n = value.length(); i < n; ++i) {
                String s = substitutions.get(value.charAt(i));

                if (s != null) {
                    result.append(s);
                } else {
                    result.append(value.charAt(i));
                }
            }

            map.put(key, result.toString());
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
