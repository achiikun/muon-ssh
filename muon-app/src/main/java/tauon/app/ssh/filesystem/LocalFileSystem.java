package tauon.app.ssh.filesystem;

import org.jetbrains.annotations.NotNull;
import tauon.app.util.misc.PathUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LocalFileSystem implements FileSystem {
    public static final String PROTO_LOCAL_FILE = "local";

    public void chmod(int perm, String path) throws Exception {
    }

    @Override
    public FileInfo getInfo(String path) throws IOException {
        File f = new File(path);
        if (!f.exists()) {
            throw new FileNotFoundException(path);
        }
        
        return getFileInfo(f);
    }
    
    @NotNull
    private static FileInfo getFileInfo(File f) throws IOException {
        Path p = f.toPath();
        
        // TODO follow links to get parameters of the real file (note that the real file may not exist)
        BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        
        FileType type = Files.isSymbolicLink(p) ?
                (f.isDirectory() ? FileType.DIR_LINK : FileType.FILE_LINK) :
                (f.isDirectory() ? FileType.DIR : FileType.FILE);
        
        return new FileInfo(f.getName(), f.getAbsolutePath(), f.length(),
                type, f.lastModified(), -1, PROTO_LOCAL_FILE,
                "", attrs.creationTime().toMillis(), "", f.isHidden());
    }
    
    @Override
    public String getHome() throws IOException {
        return System.getProperty("user.home");
    }

    @Override
    public List<FileInfo> list(String path) throws Exception {
        if (path == null || path.length() < 1) {
            path = System.getProperty("user.home");
        }
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }
        File[] childs = new File(path).listFiles();
        List<FileInfo> list = new ArrayList<>();
        if (childs == null || childs.length < 1) {
            return list;
        }
        for (File f : childs) {
            try {
                list.add(getFileInfo(f));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    public InputStream getInputStream(String file, long offset) throws Exception {
        FileInputStream fout = new FileInputStream(file);
        fout.skip(offset);
        return fout;
    }

    public OutputStream getOutputStream(String file) throws Exception {
        return new FileOutputStream(file);
    }

    @Override
    public void rename(String oldName, String newName) throws Exception {
        System.out.println("Renaming from " + oldName + " to: " + newName);
        if (!new File(oldName).renameTo(new File(newName))) {
            throw new FileNotFoundException();
        }
    }

    public synchronized void delete(FileInfo f) throws Exception {
        if (f.getType() == FileType.DIR) {
            List<FileInfo> list = list(f.getPath());
            if (list != null && list.size() > 0) {
                for (FileInfo fc : list) {
                    delete(fc);
                }
            }
            new File(f.getPath()).delete();
        } else {
            new File(f.getPath()).delete();
        }
    }

    @Override
    public void mkdir(String path) throws Exception {
        System.out.println("Creating folder: " + path);
        new File(path).mkdirs();
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean mkdirs(String absPath) throws Exception {
        return new File(absPath).mkdirs();
    }

    @Override
    public long getAllFiles(String dir, String baseDir, Map<String, String> fileMap, Map<String, String> folderMap)
            throws Exception {
        long size = 0;
        System.out.println("get files: " + dir);
        String parentFolder = PathUtils.combineUnix(baseDir, PathUtils.getFileName(dir));

        folderMap.put(dir, parentFolder);

        List<FileInfo> list = list(dir);
        for (FileInfo f : list) {
            if (f.getType() == FileType.DIR) {
                folderMap.put(f.getPath(), PathUtils.combineUnix(parentFolder, f.getName()));
                size += getAllFiles(f.getPath(), parentFolder, fileMap, folderMap);
            } else {
                fileMap.put(f.getPath(), PathUtils.combineUnix(parentFolder, f.getName()));
                size += f.getSize();
            }
        }
        return size;
    }

    /*
     * (non-Javadoc)
     *
     * @see nixexplorer.core.FileSystemProvider#deleteFile(java.lang.String)
     */
    @Override
    public void deleteFile(String f) throws Exception {
        new File(f).delete();
    }

    @Override
    public String getProtocol() {
        return PROTO_LOCAL_FILE;
    }

    /*
     * (non-Javadoc)
     *
     * @see nixexplorer.core.FileSystemProvider#createFile(java.lang.String)
     */
    @Override
    public void createFile(String path) throws Exception {
        Files.createFile(Paths.get(path));
    }

    public void createLink(String src, String dst, boolean hardLink) throws Exception {

    }

    @Override
    public String getName() {
        return "Local files";
    }

    @Override
    public String[] getRoots() throws Exception {
        File[] roots = File.listRoots();
        String[] arr = new String[roots.length];
        int i = 0;
        for (File f : roots) {
            arr[i++] = f.getAbsolutePath();
        }
        return arr;
    }

    public InputTransferChannel inputTransferChannel() throws Exception {
        return new InputTransferChannel() {
            @Override
            public InputStream getInputStream(String path) throws Exception {
                return new FileInputStream(path);
            }

            @Override
            public String getSeparator() {
                return File.separator;
            }

            @Override
            public long getSize(String path) throws Exception {
                return getInfo(path).getSize();
            }

        };
    }

    public OutputTransferChannel outputTransferChannel() throws Exception {
        return new OutputTransferChannel() {
            @Override
            public OutputStream getOutputStream(String path) throws Exception {
                return new FileOutputStream(path);
            }

            @Override
            public String getSeparator() {
                return File.separator;
            }
        };
    }

    public String getSeparator() {
        return File.separator;
    }
}
