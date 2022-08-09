/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package git.artdeell.knightinstaller;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.json.JSONObject;

/**
 *
 * @author maks
 */
public class KnightInstaller implements Runnable {

    private static final String HEAD_N = "`head -n ";
    private static final String CODE_LINE = "code = ";
    private static final String ARG_LINE = "jvmarg = ";
    private static final String CLASS_LINE = "class = ";
    private final Object installLock = new Object();
    private final Progress pr;
    private final File destination;
    private final File spiral;
    private final File getdown;

    public KnightInstaller(Progress pr) {
        this.pr = pr;
        this.destination = new File(System.getProperty("user.home") + "/.minecraft");
        this.spiral = new File(destination, "spiral");
        this.getdown = new File(spiral, "getdown-pro.jar");
    }

    @Override
    public void run() {
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission permission) {
                if ("exitVM.0".equals(permission.getName())) {
                    synchronized (installLock) {
                        installLock.notifyAll();
                    }
                    throw new IllegalStateException();
                }
            }

            @Override
            public void checkExec(String cmd) {
                synchronized (installLock) {
                    installLock.notifyAll();
                }
                throw new IllegalStateException();
            }
        });
        int progressStep = 0;
        if (!getdown.exists()) {
            pr.postMaxSteps(5);
            byte[] installer;
            try {
                pr.postLogLine("Downloading Linux installer...", null);
                installer = Utils.getFromWeb("https://gamemedia.spiralknights.com/spiral/client/spiral-install.bin", pr);
                pr.postStepProgress(++progressStep);
            } catch (IOException e) {
                pr.postLogLine("Failed to download Linux installer", e);
                pr.unlockExit();
                return;
            }

            int gzStart;
            int gzSize;
            try {
                pr.postLogLine("Processing installer...", null);
                pr.postMaxPart(1);
                pr.postPartProgress(0);
                pr.setPartIndeterminate(true);
                byte[] offsetEquals = "offset=`head".getBytes();
                byte[] filesizesEquals = "filesizes=".getBytes();
                int offsetAt = Utils.indexOf(installer, offsetEquals);
                int sizeAt = Utils.indexOf(installer, filesizesEquals);
                if (offsetAt == -1 || sizeAt == -1) {
                    pr.postLogLine("Failed to find necessary data", null);
                    pr.setPartIndeterminate(false);
                    return;
                }
                int offsetSz = Utils.indexOf(installer, offsetAt, (byte) 0x0A);
                int sizeSz = Utils.indexOf(installer, sizeAt, (byte) 0x0A);
                offsetSz = offsetSz - offsetAt;
                sizeSz = sizeSz - sizeAt;
                String offset = new String(installer, offsetAt, offsetSz);
                String size = new String(installer, sizeAt, sizeSz);
                System.out.println(offset);
                System.out.println(size);

                int headNumStart = offset.indexOf(HEAD_N) + HEAD_N.length();
                int headNumEnd = -1;
                for (int i = headNumStart; i < offset.length(); i++) {
                    if (offset.charAt(i) == ' ') {
                        headNumEnd = i;
                        break;
                    }
                }

                if (headNumEnd == -1) {
                    pr.postLogLine("Failed to find necessary data", null);
                    pr.setPartIndeterminate(false);
                    return;
                }
                gzStart = Integer.parseInt(offset.substring(headNumStart, headNumEnd));
                gzSize = Integer.parseInt(size.substring(size.indexOf("\"") + 1, size.lastIndexOf(("\""))));
                gzStart = Utils.findXth(installer, (byte) 0x0A, gzStart) + 1;
                pr.postStepProgress(++progressStep);
            } catch (Exception e) {
                pr.postLogLine("Failed to read necessary data", e);
                pr.setPartIndeterminate(false);
                pr.unlockExit();
                return;
            }
            try {
                pr.postLogLine("Decompressing...", null);
                pr.postMaxPart(1);
                pr.postPartProgress(0);
                pr.setPartIndeterminate(true);
                try ( TarArchiveInputStream tarIn = new TarArchiveInputStream(new GZIPInputStream(new ByteArrayInputStream(installer, gzStart, gzSize)))) {
                    TarArchiveEntry entry = tarIn.getNextTarEntry();
                    while (entry != null) {
                        String entryName = entry.getName();
                        pr.postLogLine("Decompressing " + entryName, null);
                        File dest = new File(spiral, entry.getName());
                        if (entry.isDirectory()) {
                            dest.mkdirs();
                        } else if (entry.isFile()) {
                            dest.getParentFile().mkdirs();
                            try ( FileOutputStream fos = new FileOutputStream(dest)) {
                                byte[] buf = new byte[65535];
                                int i;
                                while ((i = tarIn.read(buf)) != -1) {
                                    fos.write(buf, 0, i);
                                }
                            }
                        }
                        entry = tarIn.getNextTarEntry();
                    }
                }
                pr.postStepProgress(++progressStep);
            } catch (Exception e) {
                pr.postLogLine("Failed to decompress", e);
                pr.setPartIndeterminate(false);
                pr.unlockExit();
                return;
            }
        } else {
            pr.postMaxSteps(2);
        }
        try {
            pr.postLogLine("Starting getdown...", null);
            pr.postMaxPart(1);
            pr.postPartProgress(0);
            pr.setPartIndeterminate(true);
            if (!getdown.exists()) {
                pr.postLogLine("Can't find getdown-pro.jar", null);
                pr.setPartIndeterminate(false);
                return;
            }
            JarFile zf = new JarFile(getdown);
            Manifest mf = zf.getManifest();
            if (mf == null) {
                pr.postLogLine("Can't find Manifest in getdown-pro.jar", null);
                pr.setPartIndeterminate(false);
                return;
            }
            String mainClass = mf.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
            if (mainClass == null) {
                pr.postLogLine("No main class in getdown-pro.jar", null);
                pr.setPartIndeterminate(false);
                return;
            }
            URLClassLoader cl = new URLClassLoader(new URL[]{getdown.toURI().toURL()});
            Class main = cl.loadClass(mainClass);
            Method mainMethod = main.getDeclaredMethod("main", new Class[]{String[].class});
            mainMethod.invoke(null, (Object) new String[]{spiral.getAbsolutePath()});
            pr.postLogLine("Locking until the install is finished...", null);
            synchronized (installLock) {
                installLock.wait();
            }
            pr.moveToTop();
            System.setSecurityManager(null);
            pr.postStepProgress(++progressStep);
        } catch (Exception e) {
            pr.postLogLine("Failed to start getdown", e);
            pr.setPartIndeterminate(false);
            pr.unlockExit();
            return;
        }
        try {
            pr.postLogLine("Generating JSON...", null);
            List<String> codeJars = new ArrayList<>();
            List<String> jvmArgs = new ArrayList<>();
            String mainClass = null;
            BufferedReader rdr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(spiral, "getdown.txt"))));
            String line;
            while ((line = rdr.readLine()) != null) {
                if (line.startsWith(CODE_LINE)) {
                    String codeJar = line.substring(CODE_LINE.length());
                    if (!codeJar.contains("jinput") && !codeJar.contains("lwjgl")) {
                        codeJars.add(codeJar);
                    }
                } else if (line.startsWith(ARG_LINE)) {
                    String arg = line.substring(ARG_LINE.length());
                    if (!arg.startsWith("-Xm") && !arg.startsWith("-Djava.library.path") && !arg.startsWith("[")) {
                        jvmArgs.add(arg);
                    }
                } else if (line.startsWith(CLASS_LINE)) {
                    mainClass = line.substring(CLASS_LINE.length());
                }
            }
            jvmArgs.add("-Dorg.lwjgl.opengl.disableStaticInit=true");
            JSONObject outputJson = new JSONObject();
            outputJson.put("minecraftArguments", "");
            for (String s : codeJars) {
                File source = new File(spiral, s);
                String fileName = source.getName();
                String extension = fileName.substring(fileName.lastIndexOf("."));
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
                String libName = "spiral:" + fileName + ":0.0";
                File libDestination = new File(destination, "libraries/spiral/" + fileName + "/0.0/" + fileName + "-0.0" + extension);
                libDestination.getParentFile().mkdirs();
                Files.copy(source.toPath(), libDestination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                JSONObject library = new JSONObject();
                library.put("name", libName);
                outputJson.append("libraries", library);
            }
            outputJson.put("id", "SpiralKnights");
            outputJson.put("releaseTime", "2009-05-13T20:11:00+00:00");
            outputJson.put("time", "2009-05-13T20:11:00+00:00");
            outputJson.put("type", "release");
            outputJson.put("mainClass", mainClass);
            File versionPath = new File(destination, "versions/SpiralKnights/SpiralKnights.json");
            versionPath.getParentFile().mkdirs();
            try ( FileOutputStream fos = new FileOutputStream(versionPath)) {
                fos.write(outputJson.toString().getBytes());
            }

            String sprofiles = null;
            String b64Default = null;
            try {
                byte[] bprofiles = Files.readAllBytes(new File(destination, "launcher_profiles.json").toPath());
                sprofiles = new String(bprofiles, 0, bprofiles.length);
            } catch (Exception ignored) {
            }
            try {
                b64Default = Base64.getEncoder().encodeToString(Files.readAllBytes(new File(spiral, "desktop.png").toPath()));
            } catch (Exception ignored) {
            }

            JSONObject profiles = sprofiles == null ? new JSONObject() : new JSONObject(sprofiles);
            JSONObject spiralKnightsProfile = new JSONObject();
            StringBuilder sb = new StringBuilder();
            int sz = jvmArgs.size();
            for (int i = 0; i < sz; i++) {
                sb.append(jvmArgs.get(i).replace("%APPDIR%", "./spiral/"));
                if (i < sz - 1) {
                    sb.append(" ");
                }
            }
            spiralKnightsProfile.put("javaArgs", sb.toString());
            spiralKnightsProfile.put("lastVersionId", "SpiralKnights");
            spiralKnightsProfile.put("name", "Spiral Knights");
            if (b64Default != null) {
                spiralKnightsProfile.put("icon", "data:image/png;base64," + b64Default);
            }
            if (profiles.has("profiles")) {
                profiles.getJSONObject("profiles").put("SpiralKnights", spiralKnightsProfile);
            } else {
                JSONObject newProfiles = new JSONObject();
                newProfiles.put("SpiralKnights", spiralKnightsProfile);
                profiles.put("profiles", newProfiles);
            }

            Files.write(new File(destination, "launcher_profiles.json").toPath(), profiles.toString().getBytes());
            pr.postStepProgress(++progressStep);
            pr.postLogLine("All done!", null);
            pr.setPartIndeterminate(false);
        } catch (Exception e) {
            pr.postLogLine("Failed to generate JSON", e);
            pr.setPartIndeterminate(false);
        }
        pr.unlockExit();
    }
}
