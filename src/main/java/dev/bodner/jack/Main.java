package dev.bodner.jack;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 1){
            System.out.println("ERROR: Only argument should be classpath of Optifine jar.");
            return;
        }

        File path = new File(args[0]);
        if (!path.exists()){
            System.out.println("ERROR: File not found.");
            return;
        }

        JarFile jarFile = new JarFile(path);
        File path2 = new File(System.getProperty("user.dir") + File.separator + "OF_patcher_temp_dir");

        path2.mkdir();
        System.out.println("Unpacking jar file...");
        unpackJar(jarFile, path2);
        System.out.println("Jar file unpacked");

        String capeUtilsPath = path2 + File.separator + "net" + File.separator + "optifine" + File.separator + "player" + File.separator + "CapeUtils.class";

        FileInputStream stream = new FileInputStream(capeUtilsPath);

        ClassNode capeUtils = new ClassNode();
        ClassReader classReader = new ClassReader(stream);
        classReader.accept(capeUtils, 0);

        for (MethodNode methodNode : capeUtils.methods){
            for (AbstractInsnNode abstractInsnNode : methodNode.instructions){
                if (abstractInsnNode.getOpcode() == Opcodes.LDC){
                    LdcInsnNode ldc = (LdcInsnNode) abstractInsnNode;
                    if (ldc.cst.equals("[a-zA-Z0-9_]+")){
                        ldc.cst = "";
                    }
                }
            }
        }

        System.out.println("CapeUtils.class Modified");

        ClassWriter out = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        capeUtils.accept(out);
        FileOutputStream outputStream = new FileOutputStream(capeUtilsPath);
        outputStream.write(out.toByteArray());
        outputStream.close();

        System.out.println("Modified CapeUtils.class file written");

        File newPath = new File(path.getAbsolutePath().replace(".jar","_no_cape.jar"));
        if (newPath.exists()){
            newPath.delete();
        }
        JarOutputStream jarOutput = new JarOutputStream(new FileOutputStream(newPath));
        add(path2, jarOutput);
        jarOutput.close();
        System.out.println("Jar file written");
        System.out.println("Cleaning up...");
        deleteDirectory(path2);
        System.out.println("Done");
    }

    //adapted from http://www.devx.com/tips/Tip/22124
    public static void unpackJar(JarFile jar, File dir) throws IOException {
        Enumeration<JarEntry> enumEntries = jar.entries();
        while (enumEntries.hasMoreElements()) {
            JarEntry file = enumEntries.nextElement();
            File f = new File(dir + File.separator + file.getName());
            f.getParentFile().mkdirs();
            if (file.isDirectory()) { // if its a directory, create it
                f.mkdir();
                continue;
            }
            InputStream is = jar.getInputStream(file); // get the input stream
            FileOutputStream fos = new FileOutputStream(f);
            while (is.available() > 0) {  // write contents of 'is' to 'fos'
                fos.write(is.read());
            }
            fos.close();
            is.close();
        }
        jar.close();
    }

    public static void addFiles(JarOutputStream jarOutput, File input) throws IOException {
        File[] files = input.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                addFiles(jarOutput, file); // Calls same method again.
            } else {
                String path = file.getPath();
                int i = path.indexOf("OF_patcher_temp_dir");
                i += "OF_patcher_temp_dir".length() + 1;
                path = path.substring(i);

                JarEntry jarEntry = new JarEntry(path);
                jarOutput.putNextEntry(jarEntry);
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(input));

                byte[] buffer = new byte[1024];
                while (true)
                {
                    int count = in.read(buffer);
                    if (count == -1)
                        break;
                    jarOutput.write(buffer, 0, count);
                }
                jarOutput.closeEntry();
                in.close();
            }
        }
    }

    //from https://stackoverflow.com/questions/1281229/how-to-use-jaroutputstream-to-create-a-jar-file
    public static void add(File source, JarOutputStream target) throws IOException
    {
        BufferedInputStream in = null;
        try {
            if (source.isDirectory())
            {
//                String name = source.getPath().replace("\\", "/");
//                if (!name.isEmpty())
//                {
//                    if (!name.endsWith("/"))
//                        name += "/";
//                    JarEntry entry = new JarEntry(name);
//                    entry.setTime(source.lastModified());
//                    target.putNextEntry(entry);
//                    target.closeEntry();
//                }
                for (File nestedFile: source.listFiles())
                    add(nestedFile, target);
                return;
            }

            String path = source.getPath();
            int i = path.indexOf("OF_patcher_temp_dir");
            i += "OF_patcher_temp_dir".length() + 1;
            path = path.substring(i);

            JarEntry entry = new JarEntry(path.replace("\\", "/"));
            entry.setTime(source.lastModified());
            target.putNextEntry(entry);
            in = new BufferedInputStream(new FileInputStream(source));

            byte[] buffer = new byte[1024];
            while (true)
            {
                int count = in.read(buffer);
                if (count == -1)
                    break;
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        }
        finally {
            if (in != null)
                in.close();
        }
    }

    public static void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }

}
