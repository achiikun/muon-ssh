package tauon.app.ui.containers.session.pages.tools.search;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

import static tauon.app.services.LanguageService.getBundle;

public class SearchTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 7212506492710233442L;
    private final List<SearchResult> list = new ArrayList<>();
    private final String[] columns = new String[]{
            getBundle().getString("app.tools_file_search.label.filename"),
            getBundle().getString("app.tools_file_search.label.type"),
            getBundle().getString("app.tools_file_search.label.path")};

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    public void clear() {
        list.clear();
        fireTableDataChanged();
    }

    public void add(SearchResult res) {
        int index = list.size();
        list.add(res);
        fireTableRowsInserted(index, index);
    }

    public SearchResult getItemAt(int index) {
        return list.get(index);
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    public int getRowCount() {
        return list.size();
    }

    public int getColumnCount() {
        return columns.length;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        SearchResult ent = list.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return ent.getName();
            case 1:
                return ent.getType();
            case 2:
                return ent.getPath();
        }
        return "";
    }

}

