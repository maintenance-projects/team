package com.gitnx.common.util;

import java.util.Map;

public class FileTypeUtils {

    private static final Map<String, String> EXTENSION_TO_LANGUAGE = Map.ofEntries(
            Map.entry("java", "java"),
            Map.entry("py", "python"),
            Map.entry("js", "javascript"),
            Map.entry("ts", "typescript"),
            Map.entry("tsx", "typescript"),
            Map.entry("jsx", "javascript"),
            Map.entry("html", "html"),
            Map.entry("htm", "html"),
            Map.entry("css", "css"),
            Map.entry("scss", "scss"),
            Map.entry("json", "json"),
            Map.entry("xml", "xml"),
            Map.entry("yml", "yaml"),
            Map.entry("yaml", "yaml"),
            Map.entry("md", "markdown"),
            Map.entry("sql", "sql"),
            Map.entry("sh", "bash"),
            Map.entry("bash", "bash"),
            Map.entry("zsh", "bash"),
            Map.entry("rb", "ruby"),
            Map.entry("go", "go"),
            Map.entry("rs", "rust"),
            Map.entry("c", "c"),
            Map.entry("cpp", "cpp"),
            Map.entry("h", "c"),
            Map.entry("hpp", "cpp"),
            Map.entry("cs", "csharp"),
            Map.entry("kt", "kotlin"),
            Map.entry("swift", "swift"),
            Map.entry("php", "php"),
            Map.entry("gradle", "groovy"),
            Map.entry("groovy", "groovy"),
            Map.entry("properties", "properties"),
            Map.entry("toml", "toml"),
            Map.entry("dockerfile", "dockerfile"),
            Map.entry("txt", "plaintext")
    );

    public static String detectLanguage(String filename) {
        if (filename == null) return "plaintext";

        String lower = filename.toLowerCase();

        // Special filenames
        if (lower.equals("dockerfile")) return "dockerfile";
        if (lower.equals("makefile")) return "makefile";
        if (lower.equals("jenkinsfile")) return "groovy";

        int dotIndex = lower.lastIndexOf('.');
        if (dotIndex < 0) return "plaintext";

        String ext = lower.substring(dotIndex + 1);
        return EXTENSION_TO_LANGUAGE.getOrDefault(ext, "plaintext");
    }

    public static boolean isBinary(byte[] content) {
        if (content == null || content.length == 0) return false;
        int checkLength = Math.min(content.length, 8000);
        for (int i = 0; i < checkLength; i++) {
            if (content[i] == 0) return true;
        }
        return false;
    }
}
