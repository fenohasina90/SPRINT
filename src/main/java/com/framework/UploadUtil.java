package main.java.com.framework;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;

public class UploadUtil {

    /**
     * Variante utilisable côté contrôleur avec le type UploadedFile du framework
     * (sans référence directe à jakarta.servlet dans la signature du contrôleur).
     */
    public static File saveFile(UploadedFile file, String baseDir, String fileName) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file ne doit pas être null");
        }
        if (baseDir == null || baseDir.isEmpty()) {
            throw new IllegalArgumentException("baseDir ne doit pas être vide");
        }

        String originalName = file.getOriginalFilename();
        String chosenName = (fileName != null && !fileName.isBlank()) ? fileName : originalName;

        String safeName = Paths.get(chosenName).getFileName().toString();

        long timestamp = System.currentTimeMillis();
        String finalName = timestamp + "_" + safeName;

        File dir = new File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File dest = new File(dir, finalName);

        try (InputStream in = file.getInputStream();
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }

        return dest;
    }
}
