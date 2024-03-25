package tauon.app.ui.containers.session.pages.files.remote2remote;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import tauon.app.App;
import tauon.app.exceptions.OperationCancelledException;
import tauon.app.ssh.filesystem.FileInfo;
import tauon.app.ssh.filesystem.FileType;
import tauon.app.ui.components.misc.SkinnedScrollPane;
import tauon.app.ui.components.misc.SkinnedTextField;
import tauon.app.ui.dialogs.sessions.NewSessionDlg;
import tauon.app.ui.containers.session.SessionContentPanel;
import tauon.app.settings.SessionInfo;
import tauon.app.ui.components.misc.FontAwesomeContants;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static tauon.app.services.LanguageService.getBundle;
import static tauon.app.services.LanguageService.getBundle;
import static util.Constants.TRANSFER_HOSTS;
import static util.Constants.CONFIG_DIR;

public class Remote2RemoteTransferDialog extends JDialog {
    private final DefaultListModel<RemoteServerEntry> remoteHostModel;
    private final JList<RemoteServerEntry> remoteHostList;
    private final SessionContentPanel session;
    private final FileInfo[] selectedFiles;
    private final String currentDirectory;
    private final List<RemoteServerEntry> list = new ArrayList<>();

    public Remote2RemoteTransferDialog(Window frame, SessionContentPanel session, FileInfo[] selectedFiles,
                                       String currentDirectory) {
        super(frame);
        setTitle("Server to server SFTP");
        this.session = session;
        this.selectedFiles = selectedFiles;
        this.currentDirectory = currentDirectory;
        setSize(640, 480);
        setModal(true);

        remoteHostModel = new DefaultListModel<RemoteServerEntry>();
        this.list.clear();
        this.list.addAll(load());
        remoteHostModel.addAll(this.list);
        remoteHostList = new JList<RemoteServerEntry>(remoteHostModel);
        remoteHostList.setCellRenderer(new RemoteHostRenderer());

        remoteHostList.setBackground(App.skin.getTextFieldBackground());

        SkinnedScrollPane scrollPane = new SkinnedScrollPane(remoteHostList);
        scrollPane.setBorder(new MatteBorder(0, 0, 1, 0, App.skin.getDefaultBorderColor()));

        this.add(scrollPane);
        if (remoteHostModel.size() > 0) {
            remoteHostList.setSelectedIndex(0);
        }

        Box bottom = Box.createHorizontalBox();
        JButton btnAddKnown = new JButton(getBundle().getString("add_from_manager"));
        JButton btnAdd = new JButton(getBundle().getString("add"));
        JButton btnRemove = new JButton(getBundle().getString("delete"));
        JButton btnEdit = new JButton(getBundle().getString("edit"));
        JButton btnSend = new JButton(getBundle().getString("send_files"));

        btnAddKnown.addActionListener(e -> {
            try {
                SessionInfo info = new NewSessionDlg(this).newSession();
                
                if (info != null) {
                    RemoteServerEntry ent = getEntryDetails(info.getHost(), info.getUser(), info.getRemoteFolder(),
                            info.getPort());
                    if (ent != null) {
                        remoteHostModel.insertElementAt(ent, 0);
                        remoteHostList.setSelectedIndex(0);
                        save();
                    }
                }
                
            } catch (OperationCancelledException ignore) {
            
            }
        });

        btnAdd.addActionListener(e -> {
            RemoteServerEntry ent = getEntryDetails(null, null, null, 22);
            if (ent != null) {
                remoteHostModel.insertElementAt(ent, 0);
                remoteHostList.setSelectedIndex(0);
                save();
            }
        });

        btnEdit.addActionListener(e -> {
            int index = remoteHostList.getSelectedIndex();
            if (index != -1) {
                RemoteServerEntry ent = remoteHostModel.get(index);
                RemoteServerEntry ent2 = getEntryDetails(ent.getHost(), ent.getUser(), ent.getPath(), ent.getPort());
                if (ent2 != null) {
                    ent.setHost(ent2.getHost());
                    ent.setUser(ent2.getUser());
                    ent.setPath(ent2.getPath());
                    ent.setPort(ent2.getPort());
                    save();
                }
            }
        });

        btnRemove.addActionListener(e -> {
            int index = remoteHostList.getSelectedIndex();
            if (index != -1) {
                remoteHostModel.remove(index);
                save();
            }
        });

        btnSend.addActionListener(e -> {
            int index = remoteHostList.getSelectedIndex();
            if (index != -1) {
                RemoteServerEntry ent = remoteHostModel.get(index);
                this.dispose();
                this.session.openTerminal(generateCommand(ent));
            }
        });

        bottom.add(btnAddKnown);
        bottom.add(Box.createHorizontalStrut(5));
        bottom.add(btnAdd);
        bottom.add(Box.createHorizontalStrut(5));
        bottom.add(btnEdit);
        bottom.add(Box.createHorizontalStrut(5));
        bottom.add(btnRemove);
        bottom.add(Box.createHorizontalGlue());
        bottom.add(btnSend);
        bottom.setBorder(new EmptyBorder(10, 10, 10, 10));
        this.add(bottom, BorderLayout.SOUTH);

        Box top = Box.createHorizontalBox();
        JLabel lblSearch = new JLabel(FontAwesomeContants.FA_SEARCH);
        lblSearch.setFont(App.skin.getIconFont());
        JTextField txtSearch = new SkinnedTextField(30);
        txtSearch.setBackground(App.skin.getDefaultBackground());
        txtSearch.setBorder(new EmptyBorder(10, 10, 10, 10));
        top.add(lblSearch);
        top.add(txtSearch);
        top.setBorder(new CompoundBorder(new EmptyBorder(0, 10, 10, 10),
                new MatteBorder(0, 0, 1, 0, App.skin.getDefaultSelectionBackground())));

        this.add(top, BorderLayout.NORTH);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterItems(txtSearch.getText());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterItems(txtSearch.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterItems(txtSearch.getText());
            }
        });
    }

    private void filterItems(String filter) {
        this.remoteHostModel.removeAllElements();
        for (RemoteServerEntry ent : this.list) {
            if (ent.getHost().contains(filter) || ent.getPath().contains(filter)) {
                this.remoteHostModel.addElement(ent);
            }
        }
    }

    private RemoteServerEntry getEntryDetails(String host, String user, String path, int port) {
        JTextField txtHost = new SkinnedTextField(30);
        JTextField txtUser = new SkinnedTextField(30);
        JTextField txtPath = new SkinnedTextField(30);
        JSpinner spPort = new JSpinner(new SpinnerNumberModel(port, 1, 65535, 1));

        if (host != null) {
            txtHost.setText(host);
        }
        if (user != null) {
            txtUser.setText(user);
        }
        if (path != null) {
            txtPath.setText(path);
        }
        if (port > 0) {
            spPort.setValue(port);
        }

        // TODO i18n
        while (JOptionPane.showOptionDialog(this,
                new Object[]{"Host", txtHost, "User", txtUser, "Copy to ( target directory)", txtPath, "Port",
                        spPort},
                "Remote host details", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null,
                null) == JOptionPane.OK_OPTION) {
            host = txtHost.getText();
            user = txtUser.getText();
            path = txtPath.getText();
            port = (Integer) spPort.getValue();
            if (host.isEmpty() || user.isEmpty() || path.length() < 1 || port <= 0) {
                JOptionPane.showMessageDialog(this, "Invalid input: all fields mandatory");
                continue;
            }
            return new RemoteServerEntry(host, port, user, path);
        }
        return null;
    }

    private String generateCommand(RemoteServerEntry e) {
        return createSftpFileList(e);
    }

    private String createSftpFileList(RemoteServerEntry e) {
        StringBuilder sb = new StringBuilder();
        sb.append("sftp " + e.getUser() + "@" + e.getHost() + "<<EOF\n");
        sb.append("lcd \"" + this.currentDirectory + "\"\n");
        sb.append("cd \"" + e.getPath() + "\"\n");

        for (FileInfo finfo : selectedFiles) {
            if (finfo.getType() == FileType.Directory) {
                sb.append("mkdir \"" + finfo.getName() + "\"\n");
                sb.append("put -r \"" + finfo.getName() + "\"\n");
            } else if (finfo.getType() == FileType.File) {
                sb.append("put -P \"" + finfo.getName() + "\"\n");
            }
        }
        sb.append("bye\n");
        sb.append("EOF\n");
        return sb.toString();
    }

    private void save() {
        List<RemoteServerEntry> list = new ArrayList<>();
        for (int i = 0; i < remoteHostModel.size(); i++) {
            list.add(remoteHostModel.get(i));
        }
        File file = new File(CONFIG_DIR, TRANSFER_HOSTS);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(file, list);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.list.clear();
        this.list.addAll(load());
    }

    private List<RemoteServerEntry> load() {
        File file = new File(CONFIG_DIR, TRANSFER_HOSTS);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (file.exists()) {
            try {
                return objectMapper.readValue(file, new TypeReference<List<RemoteServerEntry>>() {
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>();
    }
}
