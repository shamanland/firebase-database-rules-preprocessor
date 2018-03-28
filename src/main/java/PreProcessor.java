import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

            process(map);

            out.append(gson.toJson(map));
        } finally {
            close(in);
            close(out);
        }
    }

    private static void process(Map<String, Object> map) {
        List<Map> children = new LinkedList<>();

        Map<Character, String> substitutions = new HashMap<>();

        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (e.getKey().startsWith("..")) {
                if (e.getKey().length() != 3) {
                    throw new IllegalStateException("Macro '" + e.getKey() + "' is too long");
                }

                substitutions.put(e.getKey().charAt(2), (String) e.getValue());
            } else if (e.getValue() instanceof Map) {
                children.add((Map) e.getValue());
            }
        }

        substitute(map, ".read", substitutions);
        substitute(map, ".write", substitutions);
        substitute(map, ".validate", substitutions);

        for (Character k : substitutions.keySet()) {
            map.remove(".." + k);
        }

        for (Map m : children) {
            process(m);
        }
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
