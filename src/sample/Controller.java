package sample;

import connect.PostgreSQL;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import org.intellij.lang.annotations.Language;

import javax.swing.*;
import java.sql.SQLException;

public class Controller {

    @FXML
    private TextField textSearch;

    @FXML
    private ComboBox<String> comboTable;

    @FXML
    private Button buttonAdd, buttonDelete;

    @FXML
    private TableView<String[]> table;

    private PostgreSQL db;

    public void initialize() {

        try {
            db = new PostgreSQL("localhost:5432/service_center", "postgres", "masterkey");
        } catch (SQLException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Ошибка подключения");
        }

        comboTable.setItems(FXCollections.observableArrayList(
                "- выбор -", "Сотрудники", "Должности", "Компоненты", "Производители", "Услуги", "Ремонты"));

        comboTable.setOnAction(event -> {

            table.setDisable(false);

            switch (comboTable.getSelectionModel().getSelectedIndex()) {

                case 0:
                    table.getColumns().clear();
                    table.getItems().clear();
                    table.setDisable(true);
                    break;

                case 1:

                    setTable("SELECT e.id_employee, e.key_employee, p.title, " +
                                    "e.last_name, e.first_name, e.patr_name, e.phone_number, e.address, e.email " +
                                    "FROM employees e, positions p " +
                                    "WHERE e.id_position = p.id_position " +
                                    "ORDER BY e.id_employee",
                            new String[]{"", "", "Должность", "Фамилия", "Имя", "Отчевство",
                                    "Номер телефона", "Адрес", "Email"},
                            new ObservableList[]{setItemsCombo("SELECT title FROM positions ORDER BY title")},
                            new int[]{2});
                    table.getColumns().get(1).setVisible(false);

                    break;

                case 2:

                    setTable("SELECT * FROM positions ORDER BY id_position",
                            new String[]{"", "Название", "Зарплата"},
                            null, null);

                    break;

                case 3:

                    setTable("SELECT c.id_component, m.name, c.type, c.name, c.price, " +
                                    "c.quantity, c.state, c.detail " +
                                    "FROM components c, manufacturers m " +
                                    "WHERE c.id_manufacturer = m.id_manufacturer " +
                                    "ORDER BY c.id_component",
                            new String[]{"", "Производитель", "Тип", "Название", "Цена",
                                    "Кол-во", "Состояние", "Подробно"},
                            new ObservableList[]{setItemsCombo("SELECT name FROM manufacturers ORDER BY name")},
                            new int[]{1});

                    break;

                case 4:

                    setTable("SELECT * FROM manufacturers ORDER BY name",
                            new String[]{"", "Название", "Страна"},
                            null, null);

                    break;

                case 5:

                    setTable("SELECT * FROM services ORDER BY id_service",
                            new String[]{"", "Название", "Цена", "Описание"},
                            null, null);

                    break;

                case 6:

                    setTable("SELECT DISTINCT r.id_repair, r.date_repair, r.amount, " +
                                    "concat(e.last_name, ' ', e.first_name, ' ', e.patr_name), " +
                                    "array(" +
                                    "   SELECT s.title " +
                                    "   FROM services s, repair_details rd " +
                                    "   WHERE s.id_service = ANY (rd.ids_service) " +
                                    "   AND rd.id_repair = r.id_repair) :: TEXT,  " +
                                    "array(" +
                                    "   SELECT concat(m.name, ' ', c.name) " +
                                    "   FROM components c, manufacturers m, repair_details rd " +
                                    "   WHERE c.id_manufacturer = m.id_manufacturer " +
                                    "   AND c.id_component = ANY (rd.ids_component)" +
                                    "   AND rd.id_repair = r.id_repair) :: TEXT " +
                                    "FROM repairs r, employees e, repair_details rd " +
                                    "WHERE r.id_employee = e.id_employee " +
                                    "ORDER BY r.id_repair",
                            new String[]{"", "Дата ремонта", "Сумма", "Работник", "Услуги", "Компоненты"},
                            new ObservableList[]{
                                    setItemsCombo("SELECT concat(last_name, ' ', first_name, ' ', patr_name) " +
                                            "FROM employees ORDER BY id_employee")},
                            new int[]{3});

                    table.getColumns().get(4).setEditable(false);
                    table.getColumns().get(5).setEditable(false);

                    for (int i = 0; i < table.getItems().size(); i++) {
                        table.getItems().get(i)[4] = table.getItems().get(i)[4]
                                .replace("{", "- ")
                                .replace("}", "")
                                .replaceAll(",", "\n- ");
                        table.getItems().get(i)[5] = table.getItems().get(i)[5]
                                .replace("{", "- ")
                                .replace("}", "")
                                .replaceAll(",", "\n- ")
                                .replaceAll("\"", "");
                    }

                    break;

            }
        });

    }

    private void setTable(@Language("SQL") String sql, String[] colName,
                          ObservableList<String>[] comboItems, int[] indexColumnCombo) {

        table.getColumns().clear();
        table.getItems().clear();

        String[][] records = null;
        try {
            records = db.query(true, sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (int i = 0, j = 0; i < records[0].length; i++) {
            TableColumn<String[], String> tableColumn = new TableColumn<>(colName[i]);
            final int col = i;
            tableColumn.setCellValueFactory(
                    (TableColumn.CellDataFeatures<String[], String> param) -> new SimpleStringProperty(param.getValue()[col]));

            boolean combo = false;

            if (comboItems == null && indexColumnCombo == null) {
                tableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
                tableColumn.setEditable(true);
            } else {
                for (int indexColumn : indexColumnCombo)
                    if (i == indexColumn) {
                        combo = true;
                        break;
                    }
            }
            if (combo) {
                tableColumn.setCellFactory(ComboBoxTableCell.forTableColumn(comboItems[j++]));
            } else {
                tableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            }
            table.getColumns().add(tableColumn);
        }

        ObservableList<String[]> items = FXCollections.observableArrayList(records);

        table.setItems(items);
        table.getColumns().get(0).setVisible(false);
        table.setEditable(true);

    }

    private ObservableList<String> setItemsCombo(@Language("SQL") String sql) {
        String[][] records = new String[0][];

        try {
            records = db.query(true, sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        ObservableList<String> result = FXCollections.observableArrayList();

        for (String[] record : records) {
            result.add(record[0]);
        }

        return result;
    }

}
