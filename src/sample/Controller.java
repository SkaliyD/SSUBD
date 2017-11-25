package sample;

import connect.PostgreSQL;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import org.intellij.lang.annotations.Language;

import javax.swing.*;
import java.sql.SQLException;

public class Controller {

    @FXML
    private ComboBox<String> comboQuery, comboParameter;

    @FXML
    private TableView<String[]> table;

    private PostgreSQL db;

    public void initialize() {

        try {
            db = new PostgreSQL("localhost:5432/service_center", "postgres", "masterkey");
        } catch (SQLException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), "Ошибка подключения");
        }

        comboQuery.setItems(FXCollections.observableArrayList("- выбор -",
                "Кол-во выполненных заказов сотрудников начиная с max",
                "Детали работ по указанному номеру заказа",
                "Среднее кол-во заказов за неделю",
                "Сотрудник, совершивший работы по наиболее дорогому заказу",
                "Компоненты, кол-во которых менее 5"));
        comboParameter.getItems().add("- выбор -");
        comboParameter.getItems().addAll(setItemsCombo("SELECT id_repair FROM repairs ORDER BY id_repair"));

        comboQuery.setOnAction(event1 -> {

            table.setDisable(false);
            comboParameter.getSelectionModel().select(0);
            comboParameter.setDisable(true);

            switch (comboQuery.getSelectionModel().getSelectedIndex()) {

                case 0:
                    table.getColumns().clear();
                    table.getItems().clear();
                    table.setDisable(true);
                    break;

                case 1:
                    try {
                        setTable(
                                "SELECT concat(e.last_name, ' ', e.first_name, ' ', e.patr_name) AS employees, " +
                                        "count(r.*) AS count " +
                                        "FROM employees e, repairs r " +
                                        "WHERE e.id_employee = r.id_employee " +
                                        "GROUP BY employees " +
                                        "ORDER BY count(r.id_repair) DESC",
                                new String[]{"Сотрудник", "Кол-во заказов"});
                    } catch (SQLException e) {
                        table.getColumns().add(new TableColumn<String[], String>("Ошибка"));
                    }
                    break;

                case 2:
                    table.getColumns().clear();
                    table.getItems().clear();

                    comboParameter.setDisable(false);
                    comboParameter.setOnAction(event2 -> {
                        if (!comboParameter.getSelectionModel().isSelected(0)) {
                            try {
                                setTable("SELECT DISTINCT array(" +
                                                "   SELECT s.title " +
                                                "   FROM services s " +
                                                "   WHERE s.id_service = ANY (rd.ids_service) " +
                                                "   AND rd.id_repair = r.id_repair " +
                                                "   AND r.id_repair = "
                                                + comboParameter.getSelectionModel().getSelectedItem() + ") :: TEXT, " +
                                                "array(" +
                                                "   SELECT concat(m.name, ' ', c.name) " +
                                                "   FROM components c, manufacturers m " +
                                                "   WHERE c.id_manufacturer = m.id_manufacturer " +
                                                "   AND c.id_component = ANY (rd.ids_component) " +
                                                "   AND rd.id_repair = r.id_repair " +
                                                "   AND r.id_repair = "
                                                + comboParameter.getSelectionModel().getSelectedItem() + ") :: TEXT " +
                                                "FROM repairs r, repair_details rd " +
                                                "WHERE r.id_repair = rd.id_repair " +
                                                "AND r.id_repair = " + comboParameter.getSelectionModel().getSelectedItem(),
                                        new String[]{"Услуги", "Компоненты"});

                                for (int i = 0; i < table.getItems().size(); i++) {
                                    table.getItems().get(i)[0] = table.getItems().get(i)[0]
                                            .replace("{", "- ")
                                            .replace("}", "")
                                            .replaceAll(",", "\n- ")
                                            .replaceAll("\"", "");
                                    table.getItems().get(i)[1] = table.getItems().get(i)[1]
                                            .replace("{", "- ")
                                            .replace("}", "")
                                            .replaceAll(",", "\n- ")
                                            .replaceAll("\"", "");
                                }
                            } catch (SQLException e) {
                                table.getColumns().add(new TableColumn<String[], String>("Ошибка"));
                            }
                        }
                    });
                    break;

                case 3:
                    try {
                        setTable("SELECT avg(" +
                                        "(SELECT count(r2.id_repair) " +
                                        " FROM repairs r2 " +
                                        " WHERE date_part('weeks', r2.date_repair) = date_part('weeks', r1.date_repair))" +
                                        ") FROM repairs r1",
                                new String[]{"Кол-во"});
                    } catch (SQLException e) {
                        table.getColumns().add(new TableColumn<String[], String>("Ошибка"));
                    }
                    break;

                case 4:
                    try {
                        setTable(
                                "SELECT concat(e.last_name, ' ', e.first_name, ' ', e.patr_name) " +
                                        "FROM employees e, repairs r1 " +
                                        "WHERE e.id_employee = r1.id_employee " +
                                        "AND r1.amount = (SELECT max(r2.amount) " +
                                        "                 FROM repairs r2)",
                                new String[]{"Сотрудник"});
                    } catch (SQLException e) {
                        table.getColumns().add(new TableColumn<String[], String>("Ошибка"));
                    }
                    break;

                case 5:
                    try {
                        setTable(
                                "SELECT concat(c.type, ' ', m.name, ' ', c.name) " +
                                        "FROM components c, manufacturers m " +
                                        "WHERE c.id_manufacturer = m.id_manufacturer " +
                                        "      AND c.quantity <= 5",
                                new String[]{"Компоненты"});
                    } catch (SQLException e) {
                        table.getColumns().add(new TableColumn<String[], String>("Ошибка"));
                    }
                    break;

            }
        });

    }

    private void setTable(@Language("SQL") String sql, String[] colName) throws SQLException {
        table.getColumns().clear();
        table.getItems().clear();
        String[][] records = db.query(true, sql);
        for (int i = 0; i < records[0].length; i++) {
            TableColumn<String[], String> tableColumn = new TableColumn<>(colName[i]);
            final int col = i;
            tableColumn.setCellValueFactory(
                    (TableColumn.CellDataFeatures<String[], String> param) -> new SimpleStringProperty(param.getValue()[col]));
            tableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            tableColumn.setEditable(true);
            tableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            table.getColumns().add(tableColumn);
        }
        ObservableList<String[]> items = FXCollections.observableArrayList(records);
        table.setItems(items);
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
