import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class PatchApk {
    public static void main(String[] args) throws Exception {
        String src = args[0];
        String dst = args[1];
        Map<String, String> repl = new HashMap<>();
        repl.put("classes4.dex", "classes4_new.dex");
        repl.put("classes5.dex", "classes5_new.dex");
        repl.put("classes6.dex", "classes6_new.dex");
        repl.put("res/xml/network_security_config.xml", "new_network_security_config.xml");

        try (ZipFile zin = new ZipFile(src);
             ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(dst)))) {
            Enumeration<? extends ZipEntry> e = zin.entries();
            byte[] buf = new byte[1<<16];
            while (e.hasMoreElements()) {
                ZipEntry ze = e.nextElement();
                String n = ze.getName();
                if (n.startsWith("META-INF/") &&
                    (n.endsWith(".SF") || n.endsWith(".RSA") || n.endsWith(".DSA") ||
                     n.endsWith(".EC") || n.equals("META-INF/MANIFEST.MF"))) {
                    continue;
                }
                ZipEntry no = new ZipEntry(n);
                no.setMethod(ze.getMethod());
                no.setTime(ze.getTime());
                if (ze.getMethod() == ZipEntry.STORED) {
                    no.setSize(ze.getSize());
                    no.setCompressedSize(ze.getCompressedSize());
                    no.setCrc(ze.getCrc());
                }
                byte[] data;
                if (repl.containsKey(n)) {
                    data = Files.readAllBytes(Paths.get(repl.get(n)));
                    if (ze.getMethod() == ZipEntry.STORED) {
                        CRC32 c = new CRC32(); c.update(data);
                        no.setSize(data.length);
                        no.setCompressedSize(data.length);
                        no.setCrc(c.getValue());
                    }
                } else {
                    try (InputStream is = zin.getInputStream(ze); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                        int r;
                        while ((r = is.read(buf)) > 0) bos.write(buf, 0, r);
                        data = bos.toByteArray();
                    }
                }
                zout.putNextEntry(no);
                zout.write(data);
                zout.closeEntry();
            }
        }
        System.out.println("OK " + new File(dst).length());
    }
}
