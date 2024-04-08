package tauon.app.ui.containers.session.pages.files.ssh;

import tauon.app.App;
import tauon.app.services.SettingsService;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ssh.filesystem.FileType;
import tauon.app.ssh.filesystem.LocalFileSystem;
import tauon.app.ui.components.misc.NativeFileChooser;
import tauon.app.services.BookmarkManager;
import tauon.app.ui.containers.session.pages.files.FileBrowser;
import tauon.app.ui.containers.session.pages.files.remote2remote.LocalPipeTransfer;
import tauon.app.ui.containers.session.pages.files.remote2remote.Remote2RemoteTransferDialog;
import tauon.app.ui.containers.session.pages.files.transfer.DndTransferData;
import tauon.app.ui.containers.session.pages.files.transfer.DndTransferHandler;
import tauon.app.ui.containers.session.pages.files.view.folderview.FolderView;
import tauon.app.ui.components.editortablemodel.EditorEntry;
import tauon.app.ui.dialogs.settings.SettingsPageName;
import tauon.app.util.misc.PathUtils;
import tauon.app.util.misc.PlatformUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static tauon.app.services.LanguageService.getBundle;

public class SshMenuHandler {
    private final FileBrowser fileBrowser;
    private final SshFileOperations fileOperations;
    private final SshFileBrowserView fileBrowserView;
    private final ArchiveOperation archiveOperation;
    private AbstractAction aOpenInTab, aOpen, aRename, aDelete, aNewFile, aNewFolder, aCopy, aPaste, aCut, aAddToFav,
            aChangePerm, aSendFiles, aUpload, aDownload, aCreateLink, aCopyPath;
    private KeyStroke ksOpenInTab, ksOpen, ksRename, ksDelete, ksNewFile, ksNewFolder, ksCopy, ksPaste, ksCut,
            ksAddToFav, ksChangePerm, ksSendFiles, ksUpload, ksDownload, ksCreateLink, ksCopyPath;
    private JMenuItem mOpenInTab, mOpen, mRename, mDelete, mNewFile, mNewFolder, mCopy, mPaste, mCut, mAddToFav,
            mChangePerm, mSendFiles, mUpload, mOpenWithDefApp, mOpenWthInternalEdit, mEditorConfig, mOpenWithLogView,
            mDownload, mCreateLink, mCopyPath, mOpenFolderInTerminal, mOpenTerminalHere, mRunScriptInTerminal,
            mRunScriptInBackground, mExtractHere, mExtractTo, mCreateArchive, mOpenWithMenu;
    private JMenu mEditWith, mSendTo;
    private Map<String, String> extractCommands;
    private FolderView folderView;

    public SshMenuHandler(FileBrowser fileBrowser, SshFileBrowserView fileBrowserView) {
        this.fileBrowser = fileBrowser;
        this.fileOperations = new SshFileOperations();
        this.fileBrowserView = fileBrowserView;
        this.archiveOperation = new ArchiveOperation();
    }

    public void initMenuHandler(FolderView folderView) {
        this.folderView = folderView;
        InputMap map = folderView.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap act = folderView.getActionMap();
        this.initMenuItems(map, act);
    }

    private void initMenuItems(InputMap map, ActionMap act) {
        ksOpenInTab = KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK);
        mOpenInTab = new JMenuItem(getBundle().getString("open_in_tab"));
        mOpenInTab.setAccelerator(ksOpenInTab);
        aOpenInTab = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openNewTab();
            }
        };
        mOpenInTab.addActionListener(aOpenInTab);
        map.put(ksOpenInTab, "ksOpenInTab");
        act.put("ksOpenInTab", aOpenInTab);

        aOpen = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Open app");
                FileInfo fileInfo = folderView.getSelectedFiles()[0];
                try {
                    App.getExternalEditorHandler().openRemoteFile(fileInfo, fileBrowser.getSSHFileSystem(),
                            fileBrowser.getHolder(), false, null);
                } catch (IOException e1) {
                    // TODO handle exception
                    e1.printStackTrace();
                }
            }
        };
        ksOpen = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        mOpen = new JMenuItem(getBundle().getString("open"));
        mOpen.addActionListener(aOpen);
        map.put(ksOpen, "mOpen");
        act.put("mOpen", aOpen);
        mOpen.setAccelerator(ksOpen);


        if (PlatformUtils.IS_WINDOWS) {
            mOpenWithMenu = new JMenuItem(getBundle().getString("open_with"));
            mOpenWithMenu.addActionListener(e -> {
                FileInfo fileInfo = folderView.getSelectedFiles()[0];
                try {
                    System.out.println("Called open with");
                    App.getExternalEditorHandler().openRemoteFile(fileInfo, fileBrowser.getSSHFileSystem(),
                            fileBrowser.getHolder(), true, null);
                } catch (IOException e1) {
                    // TODO handle exception
                    e1.printStackTrace();
                }
            });
        }

        mEditorConfig = new JMenuItem(getBundle().getString("configure_editor"));
        mEditorConfig.addActionListener(e -> openEditorConfig());

        mOpenWithLogView = new JMenuItem(getBundle().getString("open_log_viewer"));
        mOpenWithLogView.addActionListener(e -> openLogViewer());

        mEditWith = new JMenu(getBundle().getString("edit_with"));

        mSendTo = new JMenu(getBundle().getString("send_another_server"));

        JMenuItem mSendViaSSH = new JMenuItem(getBundle().getString("send_over_ftp"));
        mSendViaSSH.addActionListener(e -> {
            this.sendFilesViaSSH();
        });
        JMenuItem mSendViaLocal = new JMenuItem(getBundle().getString("send_this_computer"));
        mSendViaLocal.addActionListener(e -> {
            this.sendFilesViaLocal();
        });

        mSendTo.add(mSendViaSSH);
        mSendTo.add(mSendViaLocal);

        mRunScriptInTerminal = new JMenuItem(getBundle().getString("run_in_terminal"));
        mRunScriptInTerminal.addActionListener(e -> {

        });

        mOpenFolderInTerminal = new JMenuItem(getBundle().getString("open_folder_terminal"));
        mOpenFolderInTerminal.addActionListener(e -> {
            openFolderInTerminal(folderView.getSelectedFiles()[0].getPath());
        });

        mOpenTerminalHere = new JMenuItem(getBundle().getString("open_terminal_here"));
        mOpenTerminalHere.addActionListener(e -> {
            openFolderInTerminal(fileBrowserView.getCurrentDirectory());
        });

        mRunScriptInTerminal = new JMenuItem(getBundle().getString("run_file_in_terminal"));
        mRunScriptInTerminal.addActionListener(e -> {
            openRunInTerminal(fileBrowserView.getCurrentDirectory(), folderView.getSelectedFiles()[0].getPath());
        });

        mRunScriptInBackground = new JMenuItem(getBundle().getString("run_file_in_background"));
        mRunScriptInBackground.addActionListener(e -> {
            openRunInBackground(fileBrowserView.getCurrentDirectory(), folderView.getSelectedFiles()[0].getPath());
        });

        aRename = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rename(folderView.getSelectedFiles()[0], fileBrowserView.getCurrentDirectory());
            }
        };
        ksRename = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
        mRename = new JMenuItem(getBundle().getString("rename"));
        mRename.addActionListener(aRename);
        map.put(ksRename, "mRename");
        act.put("mRename", aRename);
        mRename.setAccelerator(ksRename);

        ksDelete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
        aDelete = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                delete(folderView.getSelectedFiles(), fileBrowserView.getCurrentDirectory());
            }
        };
        mDelete = new JMenuItem(getBundle().getString("delete"));
        mDelete.addActionListener(aDelete);
        map.put(ksDelete, "ksDelete");
        act.put("ksDelete", aDelete);
        mDelete.setAccelerator(ksDelete);

        ksNewFile = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
        aNewFile = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newFile(fileBrowserView.getCurrentDirectory(), folderView.getFiles());
            }
        };
        mNewFile = new JMenuItem(getBundle().getString("new_file"));
        mNewFile.addActionListener(aNewFile);
        map.put(ksNewFile, "ksNewFile");
        act.put("ksNewFile", aNewFile);
        mNewFile.setAccelerator(ksNewFile);

        ksNewFolder = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);
        aNewFolder = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newFolder(fileBrowserView.getCurrentDirectory(), folderView.getFiles());
            }
        };
        mNewFolder = new JMenuItem(getBundle().getString("new_folder"));
        mNewFolder.addActionListener(aNewFolder);
        mNewFolder.setAccelerator(ksNewFolder);
        map.put(ksNewFolder, "ksNewFolder");
        act.put("ksNewFolder", aNewFolder);

        ksCopy = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK);
        aCopy = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyToClipboard(false);
            }
        };
        mCopy = new JMenuItem(getBundle().getString("copy"));
        mCopy.addActionListener(aCopy);
        map.put(ksCopy, "ksCopy");
        act.put("ksCopy", aCopy);
        mCopy.setAccelerator(ksCopy);

        ksCopyPath = KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
        aCopyPath = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyPathToClipboard();
            }
        };
        mCopyPath = new JMenuItem(getBundle().getString("copy_path"));
        mCopyPath.addActionListener(aCopyPath);
        map.put(ksCopyPath, "ksCopyPath");
        act.put("ksCopyPath", aCopyPath);
        mCopyPath.setAccelerator(ksCopyPath);

        ksPaste = KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK);
        aPaste = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handlePaste();
            }
        };
        mPaste = new JMenuItem(getBundle().getString("paste"));
        mPaste.addActionListener(aPaste);
        map.put(ksPaste, "ksPaste");
        act.put("ksPaste", aPaste);
        mPaste.setAccelerator(ksPaste);

        ksCut = KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK);
        aCut = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyToClipboard(true);
            }
        };
        mCut = new JMenuItem(getBundle().getString("cut"));
        mCut.addActionListener(aCut);
        map.put(ksCut, "ksCut");
        act.put("ksCut", aCut);
        mCut.setAccelerator(ksCut);

        ksAddToFav = KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
        aAddToFav = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addToFavourites();
            }
        };
        mAddToFav = new JMenuItem(getBundle().getString("bookmark"));
        mAddToFav.addActionListener(aAddToFav);
        map.put(ksAddToFav, "ksAddToFav");
        act.put("ksAddToFav", aAddToFav);
        mAddToFav.setAccelerator(ksAddToFav);

        ksChangePerm = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK);
        aChangePerm = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changePermission(folderView.getSelectedFiles(), fileBrowserView.getCurrentDirectory());
            }
        };
        mChangePerm = new JMenuItem(getBundle().getString("properties"));
        mChangePerm.addActionListener(aChangePerm);
        map.put(ksChangePerm, "ksChangePerm");
        act.put("ksChangePerm", aChangePerm);
        mChangePerm.setAccelerator(ksChangePerm);

        ksCreateLink = KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK);
        aCreateLink = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createLink(fileBrowserView.getCurrentDirectory(), folderView.getSelectedFiles());
            }
        };
        mCreateLink = new JMenuItem(getBundle().getString("create_link"));
        mCreateLink.addActionListener(aCreateLink);
        map.put(ksCreateLink, "ksCreateLink");
        act.put("ksCreateLink", aCreateLink);
        mCreateLink.setAccelerator(ksCreateLink);

        mExtractHere = new JMenuItem(getBundle().getString("extract_here"));
        mExtractHere.addActionListener(e -> {
            extractArchive(folderView.getSelectedFiles()[0].getPath(), fileBrowserView.getCurrentDirectory(),
                    fileBrowserView.getCurrentDirectory());
        });

        mExtractTo = new JMenuItem(getBundle().getString("extract_to"));
        mExtractTo.addActionListener(e -> {
            String text = JOptionPane.showInputDialog(getBundle().getString("select_target"),
                    fileBrowserView.getCurrentDirectory());
            if (text == null || text.length() < 1) {
                return;
            }
            extractArchive(folderView.getSelectedFiles()[0].getPath(), text, fileBrowserView.getCurrentDirectory());
        });

        mCreateArchive = new JMenuItem(getBundle().getString("create_archive"));
        mCreateArchive.addActionListener(e -> {
            List<String> files = new ArrayList<>();
            for (FileInfo fileInfo : folderView.getSelectedFiles()) {
                files.add(fileInfo.getName());
            }
            createArchive(files, fileBrowserView.getCurrentDirectory(), fileBrowserView.getCurrentDirectory());
        });

        mDownload = new JMenuItem(getBundle().getString("download_files"));
        mDownload.addActionListener(e -> {
            downloadFiles(folderView.getSelectedFiles(), fileBrowserView.getCurrentDirectory());
        });

        mUpload = new JMenuItem(getBundle().getString("upload_here"));
        mUpload.addActionListener(e -> {
            try {
                uploadFiles();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void changePermission(FileInfo[] selectedFiles, String currentDirectory) {
        System.out.println("Showing property of: " + selectedFiles.length);
        PropertiesDialog propertiesDialog = new PropertiesDialog(fileBrowser,
                SwingUtilities.windowForComponent(fileBrowserView), selectedFiles.length > 1);
        if (selectedFiles.length > 1) {
            propertiesDialog.setMultipleDetails(selectedFiles);
        } else if (selectedFiles.length == 1) {
            propertiesDialog.setDetails(selectedFiles[0]);
        } else {
            return;
        }
        propertiesDialog.setVisible(true);
    }

    private void copyToClipboard(boolean cut) {
        FileInfo[] selectedFiles = folderView.getSelectedFiles();
        DndTransferData transferData = new DndTransferData(fileBrowserView, selectedFiles, null);
        transferData.setTransferAction(cut ? DndTransferData.TransferAction.Cut : DndTransferData.TransferAction.Copy);

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{DndTransferHandler.DATA_FLAVOR_DATA_FILE};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return flavor.equals(DndTransferHandler.DATA_FLAVOR_DATA_FILE);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                return transferData;
            }
        }, (a, b) -> {
        });
    }

    private void copyPathToClipboard() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (FileInfo f : folderView.getSelectedFiles()) {
            if (!first) {
                sb.append("\n");
            }
            sb.append(f.getPath());
            if (first) {
                first = false;
            }
        }
        if (sb.length() > 0) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
        }
    }

    private void openLogViewer() {
        fileBrowser.getHolder().openLog(folderView.getSelectedFiles()[0]);
    }

    public boolean createMenu(JPopupMenu popup, FileInfo[] files) {
        popup.removeAll();
        int selectionCount = files.length;
        int count = 0;
        count += createBuiltInItems1(selectionCount, popup, files);
        count += createBuiltInItems2(selectionCount, popup, files);
        return count > 0;
    }

    private int createBuiltInItems1(int selectionCount, JPopupMenu popup, FileInfo[] selectedFiles) {
        int count = 0;
        if (selectionCount == 1) {
            if (selectedFiles[0].getType() == FileType.Directory || selectedFiles[0].getType() == FileType.DirLink) {
                popup.add(mOpenInTab);
                count++;
                popup.add(mOpenFolderInTerminal);
                count++;
            }

            if ((selectedFiles[0].getType() == FileType.File || selectedFiles[0].getType() == FileType.FileLink)) {
                popup.add(mOpen);
                count++;

                if (PlatformUtils.IS_WINDOWS) {
                    popup.add(mOpenWithMenu);
                    count++;
                }

                loadEditors();
                popup.add(mEditWith);
                count++;

                popup.add(mRunScriptInTerminal);
                count++;

                popup.add(mRunScriptInBackground);
                count++;

                popup.add(mOpenWithLogView);
                count++;
            }
        }

        if (selectionCount > 0) {
            popup.add(mCut);
            popup.add(mCopy);
            popup.add(mCopyPath);
            popup.add(mDownload);
            count += 3;
        }

        if (hasSupportedContentOnClipboard()) {
            popup.add(mPaste);
        }

        if (selectionCount == 1) {
            popup.add(mRename);
            count++;
        }

        return count;
    }

    private int createBuiltInItems2(int selectionCount, JPopupMenu popup, FileInfo[] selectedFiles) {
        int count = 0;
        if (selectionCount > 0) {
            popup.add(mDelete);
            popup.add(mCreateArchive);
            popup.add(mSendTo);
            count += 3;
        }

        if (selectionCount == 1) {
            FileInfo fileInfo = selectedFiles[0];
            if ((selectedFiles[0].getType() == FileType.File || selectedFiles[0].getType() == FileType.FileLink)
                    && this.archiveOperation.isSupportedArchive(fileInfo.getName())) {
                popup.add(mExtractHere);
                popup.add(mExtractTo);
            }
            count += 2;
        }

        if (selectionCount < 1) {
            popup.add(mNewFolder);
            popup.add(mNewFile);
            popup.add(mOpenTerminalHere);
            count += 2;
        }

        if (selectionCount < 1 || (selectionCount == 1
                && (selectedFiles[0].getType() == FileType.File || selectedFiles[0].getType() == FileType.FileLink))) {
            popup.add(mUpload);
            count += 1;
        }

        // check only if folder is selected
        boolean allFolder = true;
        for (FileInfo f : selectedFiles) {
            if (f.getType() != FileType.Directory && f.getType() != FileType.DirLink) {
                allFolder = false;
                break;
            }
        }

        popup.add(mAddToFav);
        count++;

        if (selectionCount <= 1) {
            popup.add(mCreateLink);
            count++;
        }

        if (selectionCount >= 1) {
            popup.add(mChangePerm);
            count++;
        }
        return count;
    }

    public void openNewTab() {
        FileInfo[] files = folderView.getSelectedFiles();
        if (files.length == 1) {
            FileInfo file = files[0];
            if (file.getType() == FileType.Directory || file.getType() == FileType.DirLink) {
                fileBrowser.openSshFileBrowserView(file.getPath(), this.fileBrowserView.getOrientation());
            } else {

            }
        }
    }

    private void rename(FileInfo info, String baseFolder) {
        String text = JOptionPane.showInputDialog(getBundle().getString("please_new_name"), info.getName());
        if (text != null && text.length() > 0) {
            renameAsync(info.getPath(), PathUtils.combineUnix(PathUtils.getParent(info.getPath()), text), baseFolder);
        }
    }

    private void renameAsync(String oldName, String newName, String baseFolder) {
        
        fileBrowser.getHolder().submitSSHOperation(instance -> {
            if (fileOperations.rename(oldName, newName, fileBrowserView.getFileSystem(),
                    instance, fileBrowser.getInfo().getPassword())) {
                fileBrowserView.render(baseFolder);
            }
        });
        
//        fileBrowser.getHolder().executor.submit(() -> {
//            fileBrowser.disableUi();
//            try {
//                if (fileOperations.rename(oldName, newName, fileBrowserView.getFileSystem(),
//                        fileBrowserView.getSshClient(), fileBrowser.getInfo().getPassword())) {
//                    fileBrowserView.render(baseFolder);
//                } else {
//                    fileBrowser.enableUi();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                fileBrowser.enableUi();
//            }
//        });
    }

    private void delete(FileInfo[] targetList, String baseFolder) {
        boolean delete = true;
        if (SettingsService.getSettings().isConfirmBeforeDelete()) {
            delete = JOptionPane.showConfirmDialog(null, "Delete selected files?") == JOptionPane.YES_OPTION;
        }
        if (!delete)
            return;
        
        fileBrowser.getHolder().submitSSHOperation(instance -> {
            if (fileOperations.delete(targetList, fileBrowserView.getFileSystem(),
                    instance, fileBrowser.getInfo().getPassword())) {
                fileBrowserView.render(baseFolder);
            }
        });
        
//        fileBrowser.getHolder().executor.submit(() -> {
//            fileBrowser.disableUi();
//            try {
//                if (fileOperations.delete(targetList, fileBrowserView.getFileSystem(),
//                        fileBrowserView.getSshClient(), fileBrowser.getInfo().getPassword())) {
//                    fileBrowserView.render(baseFolder);
//                } else {
//                    fileBrowser.enableUi();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                fileBrowser.enableUi();
//            }
//
//        });
    }

    public void newFile(String baseFolder, FileInfo[] files) {
        
        fileBrowser.getHolder().submitSSHOperation(instance -> {
            if (fileOperations.newFile(files, fileBrowserView.getFileSystem(), baseFolder,
                    instance, fileBrowser.getInfo().getPassword())) {
                fileBrowserView.render(baseFolder);
            }
        });
        
//        fileBrowser.getHolder().executor.submit(() -> {
//            fileBrowser.disableUi();
//            try {
//                if (fileOperations.newFile(files, fileBrowserView.getFileSystem(), baseFolder,
//                        fileBrowserView.getSshClient(), fileBrowser.getInfo().getPassword())) {
//                    fileBrowserView.render(baseFolder);
//                } else {
//                    fileBrowser.enableUi();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                fileBrowser.enableUi();
//            }
//
//        });
    }

    public void newFolder(String baseFolder, FileInfo[] files) {
        
        fileBrowser.getHolder().submitSSHOperation(instance -> {
            if (fileOperations.newFolder(files, baseFolder, fileBrowserView.getFileSystem(),
                    instance, fileBrowser.getInfo().getPassword())) {
                fileBrowserView.render(baseFolder);
            }
        });
        
//        fileBrowser.getHolder().executor.submit(() -> {
//            fileBrowser.disableUi();
//            try {
//                if (fileOperations.newFolder(files, baseFolder, fileBrowserView.getFileSystem(),
//                        fileBrowserView.getSshClient(), fileBrowser.getInfo().getPassword())) {
//                    fileBrowserView.render(baseFolder);
//                } else {
//                    fileBrowser.enableUi();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                fileBrowser.enableUi();
//            }
//
//        });
    }

    public void createLink(String baseFolder, FileInfo[] files) {
        
        fileBrowser.getHolder().submitSSHOperation(instance -> {
            if (fileOperations.createLink(files, fileBrowserView.getFileSystem(), instance)) {
                fileBrowserView.render(baseFolder);
            }
        });
        
//        fileBrowser.getHolder().executor.submit(() -> {
//            fileBrowser.disableUi();
//            try {
//                if (fileOperations.createLink(files, fileBrowserView.getFileSystem(), fileBrowserView.getSshClient())) {
//                    fileBrowserView.render(baseFolder);
//                } else {
//                    fileBrowser.enableUi();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                fileBrowser.enableUi();
//            }
//        });
    }

    private void handlePaste() {
        if (Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DndTransferHandler.DATA_FLAVOR_DATA_FILE)) {
            try {
                DndTransferData transferData = (DndTransferData) Toolkit.getDefaultToolkit().getSystemClipboard()
                        .getData(DndTransferHandler.DATA_FLAVOR_DATA_FILE);
                if (transferData != null) {
                    fileBrowserView.handleDrop(transferData);
                }
            } catch (UnsupportedFlavorException | IOException e1) {
                e1.printStackTrace();
            }
        } else {
            DataFlavor[] flavors = Toolkit.getDefaultToolkit().getSystemClipboard().getAvailableDataFlavors();
            for (DataFlavor flavor : flavors) {
                if (flavor.isFlavorJavaFileListType()) {

                }
            }
        }
    }

    private boolean hasSupportedContentOnClipboard() {
        boolean ret = (Toolkit.getDefaultToolkit().getSystemClipboard()
                .isDataFlavorAvailable(DndTransferHandler.DATA_FLAVOR_DATA_FILE)
                || Toolkit.getDefaultToolkit().getSystemClipboard()
                .isDataFlavorAvailable(DataFlavor.javaFileListFlavor));
        if (!ret)
            System.out.println("Nothing on clipboard");
        return ret;
    }

    public void copy(List<FileInfo> files, String targetFolder) {
        
        fileBrowser.getHolder().submitSSHOperation(instance -> {
            if (fileOperations.copyTo(instance, files, targetFolder,
                    fileBrowserView.getFileSystem(), fileBrowser.getInfo().getPassword())) {
                fileBrowserView.render(targetFolder);
            }
        });
        
//        fileBrowser.getHolder().executor.submit(() -> {
//            fileBrowser.disableUi();
//            try {
//                if (fileOperations.copyTo(fileBrowserView.getSshClient(), files, targetFolder,
//                        fileBrowserView.getFileSystem(), fileBrowser.getInfo().getPassword())) {
//                    fileBrowserView.render(targetFolder);
//                } else {
//                    fileBrowser.enableUi();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
    }

    public void move(List<FileInfo> files, String targetFolder) {
        
        fileBrowser.getHolder().submitSSHOperation(instance -> {
            if (fileOperations.moveTo(instance, files, targetFolder,
                    fileBrowserView.getFileSystem(), fileBrowser.getInfo().getPassword())) {
                fileBrowserView.render(targetFolder);
            }
        });
        
//        fileBrowser.getHolder().executor.submit(() -> {
//            fileBrowser.disableUi();
//            try {
//                if (fileOperations.moveTo(fileBrowserView.getSshClient(), files, targetFolder,
//                        fileBrowserView.getFileSystem(), fileBrowser.getInfo().getPassword())) {
//                    fileBrowserView.render(targetFolder);
//                } else {
//                    fileBrowser.enableUi();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
    }

    private void addToFavourites() {
        FileInfo[] arr = folderView.getSelectedFiles();

        if (arr.length > 0) {
            BookmarkManager.getInstance().addEntry(fileBrowser.getInfo().getId(),
                    Arrays.asList(arr).stream()
                            .filter(a -> a.getType() == FileType.DirLink || a.getType() == FileType.Directory)
                            .map(a -> a.getPath()).collect(Collectors.toList()));
        } else if (arr.length == 0) {
            BookmarkManager.getInstance().addEntry(fileBrowser.getInfo().getId(), fileBrowserView.getCurrentDirectory());
        }

        this.fileBrowserView.getOverflowMenuHandler().loadFavourites();
    }

    public JPopupMenu createAddressPopup() {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem mOpenInNewTab = new JMenuItem(getBundle().getString("open_new_tab"));
        JMenuItem mCopyPath = new JMenuItem(getBundle().getString("copy_path"));
        JMenuItem mOpenInTerminal = new JMenuItem(getBundle().getString("open_in_terminal"));
        JMenuItem mBookmark = new JMenuItem(getBundle().getString("bookmark"));
        popupMenu.add(mOpenInNewTab);
        popupMenu.add(mCopyPath);
        popupMenu.add(mOpenInTerminal);
        popupMenu.add(mBookmark);

        mOpenInNewTab.addActionListener(e -> {
            String path = popupMenu.getName();
            fileBrowser.openSshFileBrowserView(path, this.fileBrowserView.getOrientation());
        });

        mOpenInTerminal.addActionListener(e -> {
            String path = popupMenu.getName();
            this.openFolderInTerminal(path);
        });

        mCopyPath.addActionListener(e -> {
            String path = popupMenu.getName();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(path), null);
        });

        mBookmark.addActionListener(e -> addToFavourites());
        return popupMenu;
    }

    private void openFolderInTerminal(String folder) {
        fileBrowser.getHolder().openTerminal("cd \"" + folder + "\"");
    }

    private void openRunInTerminal(String folder, String file) {
        fileBrowser.getHolder().openTerminal("cd \"" + folder + "\"; \"" + file + "\"");
    }

    private void openRunInBackground(String folder, String file) {
        
        fileBrowser.getHolder().submitSSHOperation(instance -> {
            if (fileOperations.runScriptInBackground(instance,
                    "cd \"" + folder + "\"; nohup \"" + file + "\" &", new AtomicBoolean())) {
            }
        });
        
//        fileBrowser.getHolder().executor.submit(() -> {
//            fileBrowser.disableUi();
//            try {
//                if (fileOperations.runScriptInBackground(fileBrowserView.getSshClient(),
//                        "cd \"" + folder + "\"; nohup \"" + file + "\" &", new AtomicBoolean())) {
//                }
//                fileBrowser.enableUi();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
    }

    private void extractArchive(String archive, String folder, String currentFolder) {
        AtomicBoolean stopFlag = new AtomicBoolean(false);
        fileBrowser.getHolder().submitSSHOperationStoppable(instance -> {
            if (!archiveOperation.extractArchive(instance, archive, folder, stopFlag)) {
                if (!fileBrowser.isSessionClosed()) {
                    JOptionPane.showMessageDialog(null, getBundle().getString("operation_failed"));
                }
            }
            fileBrowserView.render(currentFolder);
        }, stopFlag);
        
//        fileBrowser.getHolder().executor.submit(() -> {
//            AtomicBoolean stopFlag = new AtomicBoolean(false);
//            fileBrowser.disableUi(stopFlag);
//            try {
//                if (!archiveOperation.extractArchive(fileBrowserView.getSshClient(), archive, folder, stopFlag)) {
//                    if (!fileBrowser.isSessionClosed()) {
//                        JOptionPane.showMessageDialog(null, getBundle().getString("operation_failed"));
//                    }
//                }
//                fileBrowserView.render(currentFolder);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
    }

    private void createArchive(List<String> files, String folder, String currentFolder) {
        AtomicBoolean stopFlag = new AtomicBoolean(false);
        fileBrowser.getHolder().submitSSHOperationStoppable(instance -> {
            if (!archiveOperation.createArchive(instance, files, folder, stopFlag)) {
                if (!fileBrowser.isSessionClosed()) {
                    JOptionPane.showMessageDialog(null, getBundle().getString("operation_failed"));
                }
            }
            fileBrowserView.render(currentFolder);
        }, stopFlag);
        
        
//        fileBrowser.getHolder().executor.submit(() -> {
//            AtomicBoolean stopFlag = new AtomicBoolean(false);
//            fileBrowser.disableUi(stopFlag);
//            try {
//                if (!archiveOperation.createArchive(fileBrowserView.getSshClient(), files, folder, stopFlag)) {
//                    if (!fileBrowser.isSessionClosed()) {
//                        JOptionPane.showMessageDialog(null, getBundle().getString("operation_failed"));
//                    }
//                }
//                fileBrowserView.render(currentFolder);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
    }

    private void downloadFiles(FileInfo[] files, String currentDirectory) {
        throw new RuntimeException("Not implemented");
    }

    private void uploadFiles() throws IOException {
        NativeFileChooser jfc = new NativeFileChooser();
        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        jfc.setMultiSelectionEnabled(true);
        if (jfc.showOpenDialog(SwingUtilities.getWindowAncestor(fileBrowser)) == JFileChooser.APPROVE_OPTION) {
            System.out.println("After file selection");
            File[] files = jfc.getSelectedFiles();
            if (files.length > 0) {
                List<FileInfo> list = new ArrayList<>();

                try (LocalFileSystem localFileSystem = new LocalFileSystem()) {
                    for (File file : files) {
                        FileInfo fileInfo = localFileSystem.getInfo(file.getAbsolutePath());
                        list.add(fileInfo);
                    }
                }
                DndTransferData uploadData = new DndTransferData(
                        null,
                        list.toArray(new FileInfo[0]),
                        fileBrowserView
                );
                fileBrowserView.handleDrop(uploadData);
            }
        }
    }

    private void openWithEditor(String path) {
        FileInfo fileInfo = folderView.getSelectedFiles()[0];
        try {
            App.getExternalEditorHandler().openRemoteFile(fileInfo, fileBrowser.getSSHFileSystem(),
                    fileBrowser.getHolder(), false, path);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void openEditorConfig() {
        fileBrowserView.getFileBrowser().getHolder().getAppWindow()
                .openSettings(SettingsPageName.EDITOR);
    }

    private void sendFilesViaLocal() {
        LocalPipeTransfer pipTransfer = new LocalPipeTransfer();
        pipTransfer.transferFiles(fileBrowser, fileBrowserView.getCurrentDirectory(), folderView.getSelectedFiles());
    }

    private void sendFilesViaSSH() {
        Remote2RemoteTransferDialog r2rt = new Remote2RemoteTransferDialog(
                SwingUtilities.getWindowAncestor(fileBrowserView),
                this.fileBrowser.getHolder(),
                folderView.getSelectedFiles(),
                fileBrowserView.getCurrentDirectory()
        );
        r2rt.setLocationRelativeTo(
                SwingUtilities.getWindowAncestor(fileBrowserView)
        );
        r2rt.setVisible(true);
    }

    private void loadEditors() {
        mEditWith.removeAll();
        for (EditorEntry ent : SettingsService.getSettings().getEditors()) {
            JMenuItem mEditorItem = new JMenuItem(ent.getName());
            mEditorItem.addActionListener(e -> openWithEditor(ent.getPath()));
            mEditWith.add(mEditorItem);
        }
        mEditWith.add(mEditorConfig);
    }

}
