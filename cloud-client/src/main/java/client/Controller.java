package client;


import common.FileSender;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    @FXML
    TableView<FileInfo> filesTableClient, filesTableServer;

    @FXML
    ComboBox disksBoxClient, disksBoxServer;

    @FXML
    TextField pathFieldClient, pathFieldServer;




    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
        Network.getInstance().stop();
        System.out.println("Клиент отключился");
    }

    //метод собирает из какого то пути по любому пути собирает список файлов
    public void updateList(Path path, TableView<FileInfo> tableView, TextField textField) {
        Platform.runLater(() ->{// TODO: 07.06.2020 Доработать допонять

        tableView.getItems().clear();//вначале чистим список файлов
        //получаем ссылку на список файлов в таблице, добавляем пачку файлов,получаем поток путей в директории,
        //map(это преобразование данных)мы берем каждый путь из этого потока и отдаем в конструктор fileinfo
        //collect - собираем все это в лист

        try {
            textField.setText(path.normalize().toAbsolutePath().toString());
            tableView.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            tableView.sort();
        } catch (IOException e) {
            //всплывающее окно с сообщением
            System.out.println(e);
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();//показать окно и подождать пока не нажмут ОК
        }
        });
    }

    //при нажатии кнопки UP клиентской левой части приложения
    public void btnPathUpActionClient(ActionEvent actionEvent) {
        //запрашиваем весь путь в строке пути, и у него запрашиваем путь в какой папке он лежит
        Path upperPath = Paths.get(pathFieldClient.getText()).getParent();
        if (upperPath != null) {//если есть еще выше дирректория
            updateList(upperPath, filesTableClient, pathFieldClient);
        }
    }

    //при нажатии кнопки UP серверной правой части приложения
    public void btnPathUpActionServer(ActionEvent actionEvent) {
        //запрашиваем весь путь в строке пути, и у него запрашиваем путь в какой папке он лежит
        Path upperPath = Paths.get(pathFieldServer.getText()).getParent();
        if (upperPath != null) {//если есть еще выше дирректория
            updateList(upperPath, filesTableServer, pathFieldServer);
        }
    }

    //метод переходит в директорию выбранного из комбобокса
    public void selectClientDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();//получили ссылку на источник события => комбобокс
        updateList(Paths.get(element.getSelectionModel().getSelectedItem()), filesTableClient, pathFieldClient);//вызываем updateList у выбраного диска
    }

    //метод переходит в директорию выбранного из комбобокса
    public void selectServerDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();//получили ссылку на источник события => комбобокс
        updateList(Paths.get(element.getSelectionModel().getSelectedItem()), filesTableServer, pathFieldServer);//вызываем updateList у выбраного диска
    }


    //кнопка RENAME
    public void btnRename(ActionEvent actionEvent) {
        if (filesTableClient.isFocused()) {//если выбрана клиентская сторона
            System.out.println("выбрана клиентская сторона");
            Path renamePathClient = Paths.get(pathFieldClient.getText()).resolve(filesTableClient.getSelectionModel().getSelectedItem().getFilename());
            renameWindow(renamePathClient,filesTableClient,pathFieldClient);
            updateList(Paths.get(".", "client-repository"), filesTableClient, pathFieldClient);

            if (Files.isDirectory(renamePathClient)){
                Alert alert = new Alert(Alert.AlertType.ERROR, "Файл не выбран", ButtonType.OK);
                alert.showAndWait();
                return;
            }
        }
        if (filesTableServer.isFocused()) {
            System.out.println("выбрана серверная сторона");
            Path renamePathServer = Paths.get(pathFieldServer.getText()).resolve(filesTableServer.getSelectionModel().getSelectedItem().getFilename());
            renameWindow(renamePathServer,filesTableServer,pathFieldServer);
            updateList(Paths.get(".", "server-repository"), filesTableServer, pathFieldServer);//передаем выбраную сторону таблицы и путь

            if (Files.isDirectory(renamePathServer)){
                Alert alert = new Alert(Alert.AlertType.ERROR, "Файл не выбран", ButtonType.OK);
                alert.showAndWait();
                return;
            }
        }
    }

    public void renameWindow(Path newPathFile, TableView<FileInfo> selectedTable, TextField selectedPath ) {
        if (Files.exists(newPathFile)) {
            final Stage rename = new Stage();
            Label lbl = new Label("Введите новое имя файла");
            lbl.setFont(Font.font("System",16));

            HBox forLabel = new HBox();
            forLabel.getChildren().add(lbl);

            TextField textField = new TextField();
            textField.setPrefColumnCount(20);

            Button btnOk = new Button("RENAME");
            btnOk.setOnAction(event -> {
                try {// TODO: 09.06.2020 ошибка доделать правильный путь
                    Path newFileName = Paths.get(selectedPath.getText()).resolve(textField.getText());
                    System.out.println(newFileName);
                    System.out.println(newFileName.resolveSibling(newFileName));
                    Files.move(newFileName, newFileName.resolveSibling(newFileName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                rename.close();
            });

            HBox root = new HBox();
            root.getChildren().addAll(textField, btnOk);

            HBox.setHgrow(textField, Priority.ALWAYS);
            HBox.setHgrow(btnOk, Priority.ALWAYS);
            HBox.setHgrow(lbl,Priority.ALWAYS);
            VBox vBox = new VBox(forLabel,root);

            Scene scene = new Scene(vBox, 400, 100);
            rename.setScene(scene);
            rename.setTitle("RENAME FILE");
            rename.show();
        }
    }


    //кнопка SEND
    public void btnSend(ActionEvent actionEvent) {
        //если не выбрано файла или файл является директорией то ничего не делаем
        FileInfo selectedFile = filesTableClient.getSelectionModel().getSelectedItem();
        if (selectedFile == null || selectedFile.getType() == FileInfo.FileType.DIRECTORY){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Файл не выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        Path pathWithFileName = Paths.get(pathFieldClient.getText())
                .resolve(filesTableClient.getSelectionModel().getSelectedItem().getFilename());
        System.out.println(pathWithFileName);
        if (!Files.exists(pathWithFileName)) {
            System.out.println("Файл не найден для отправки");
            return;// TODO: 05.06.2020 правильно ли здесь return??
        }
        try {
            FileSender.sendFile(pathWithFileName, Network.getInstance().getCurrentChannel(), future -> {
                if (!future.isSuccess()) {
                    System.out.println("Не удалось отправить файл");
                    future.cause().printStackTrace();
                }
                if (future.isSuccess()) {
                    System.out.println("Файл успешно передан");

                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        updateList(Paths.get(".", "server-repository"), filesTableServer, pathFieldServer);
        System.out.println(getCurrentPath(pathFieldServer));
    }

    //кнопка DOWNLOAD
    public void btnDownload(ActionEvent actionEvent) {
        FileInfo selectedFile = filesTableServer.getSelectionModel().getSelectedItem();
        if (selectedFile == null || selectedFile.getType() == FileInfo.FileType.DIRECTORY){
            Alert alert = new Alert(Alert.AlertType.ERROR, "Файл не выбран", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        //полный путь к файлу c именем файла вконце
        Path pathWithFileName = Paths.get(pathFieldServer.getText()).resolve(filesTableServer.getSelectionModel().getSelectedItem().getFilename());
        String filename = getSelectedFileName(filesTableServer);
        System.out.println(filename);
        System.out.println("полный путь к файлу = "+ pathWithFileName);
        if (!Files.exists(pathWithFileName)) {
            System.out.println("Файл не найден для скачивания");
            return;// TODO: 05.06.2020 правильно ли здесь return??
        }
        Network.sendFileRequest(filename,Network.getInstance().getCurrentChannel());
        updateList(Paths.get(".", "client-repository"), filesTableClient, pathFieldClient);

    }
    //кнопка DELETE
    public void btnDelete(ActionEvent event) {
        if (filesTableClient.isFocused()) {//если выбрана клиентская сторона
            System.out.println("выбрана клиентская сторона");
            Path deletePathClient = Paths.get(pathFieldClient.getText()).resolve(filesTableClient.getSelectionModel().getSelectedItem().getFilename());
            if (Files.isDirectory(deletePathClient)){
                Alert alert = new Alert(Alert.AlertType.ERROR, "Файл не выбран", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            if (Files.exists(deletePathClient)) {
                System.out.println("по указанному пути = на Клиенте файл существует");
                try {
                    Files.delete(deletePathClient);
                    updateList(Paths.get(".", "client-repository"), filesTableClient, pathFieldClient);
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно удалить выбраный файл => "+ deletePathClient.getFileName(), ButtonType.OK);
                    alert.showAndWait();
                }
            }
        }

        if(filesTableServer.isFocused()){//если выбрана серверная сторона
            Path deletePathServer = Paths.get(pathFieldServer.getText()).resolve(filesTableServer.getSelectionModel().getSelectedItem().getFilename());
            System.out.println("выбрана сторона сервера");
            if (Files.isDirectory(deletePathServer)){
                Alert alert = new Alert(Alert.AlertType.ERROR, "Файл не выбран", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            if (Files.exists(deletePathServer)) {
                System.out.println("по указанному пути = на Сервере файл существует");

                try {
                    Files.delete(deletePathServer);
                    updateList(Paths.get(".", "server-repository"), filesTableServer, pathFieldServer);
                } catch (IOException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно удалить выбраный файл => "+ deletePathServer.getFileName(), ButtonType.OK);
                    alert.showAndWait();
                }
            }
        }
    }

    //метод определяющий в какой панели мы что выделили или нажали
    public String getSelectedFileName(TableView<FileInfo> filesTable) {
        return filesTable.getSelectionModel().getSelectedItem().getFilename();//вернет имя файла на который смотрим
    }



    //запрос текущего пути
    public String getCurrentPath(TextField pathField) {
        return pathField.getText();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        /**
         * //////// Левая панель клиента////////////////////
         */

        //создадим столбец которые будет содержать тип директории файл или директория F D
        TableColumn<FileInfo, String> fileTypeColumnClient = new TableColumn<>();

        //ниже узнаем D или F
        fileTypeColumnClient.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumnClient.setPrefWidth(24);

        //создадим столбец которые будет содержать тип директории файл или директория F D
        TableColumn<FileInfo, String> filenameColumnClient = new TableColumn<>("name");
        //ниже узнаем D или F
        filenameColumnClient.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumnClient.setPrefWidth(320);

        // создадим столбец размера файла
        TableColumn<FileInfo, Long> fileSizeColumnClient = new TableColumn<>("size");
        fileSizeColumnClient.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumnClient.setPrefWidth(120);
        fileSizeColumnClient.setCellFactory(column -> {//показывает как выглядит ячейка в столбце
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {//item - значение ячейки, empty - информация пустая или нет ячейка
                    super.updateItem(item, empty);
                    this.setStyle("-fx-background-color: #353535;");
                    if (item == null || empty) {//если лонг не заполнен или ячейка пустая
                        setText(null);//то в ячеке ничего не пишем
                        setStyle("");//и она никак не выглядит

                    } else {//если же что то есть => формируем как она должна это отобразить
                        this.setTextFill(Color.LIMEGREEN);

                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });

        //создадим столбец даты
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");//создали свой формат даты
        TableColumn<FileInfo, String> fileDateColumnClient = new TableColumn<>("date modified");
        fileDateColumnClient.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumnClient.setPrefWidth(120);


        filesTableClient.getColumns().addAll(fileTypeColumnClient, filenameColumnClient, fileSizeColumnClient, fileDateColumnClient);//добавляем в список столбцы выше созданные
        filesTableClient.getSortOrder().add(fileTypeColumnClient);//стартовая сортировка использует столбец с типами файлов

        disksBoxClient.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {//запрашиваем список корневых дирректорий
            disksBoxClient.getItems().add(p.toString());//добавляем ссылку на каждый из дисков
        }
        disksBoxClient.getSelectionModel().select(0);//по умолчанию выбираем первый из них

        filesTableClient.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {//если был клик мышью два раза
                    Path path = Paths.get(pathFieldClient.getText()).resolve(filesTableClient.getSelectionModel().getSelectedItem().getFilename());
                    if (Files.isDirectory(path)) {
                        updateList(path, filesTableClient, pathFieldClient);
                    }
                }
            }
        });
        updateList(Paths.get(".", "client-repository"), filesTableClient, pathFieldClient);


        /**
         * //////// Правая панель сервера ////////////////////
         */

        //создадим столбец которые будет содержать тип директории файл или директория F D
        TableColumn<FileInfo, String> fileTypeColumnServer = new TableColumn<>();

        //ниже узнаем D или F
        fileTypeColumnServer.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumnServer.setPrefWidth(24);

        //создадим столбец которые будет содержать тип директории файл или директория F D
        TableColumn<FileInfo, String> filenameColumnServer = new TableColumn<>("name");
        //ниже узнаем D или F
        filenameColumnServer.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumnServer.setPrefWidth(320);

        // создадим столбец размера файла
        TableColumn<FileInfo, Long> fileSizeColumnServer = new TableColumn<>("size");
        fileSizeColumnServer.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumnServer.setPrefWidth(120);
        fileSizeColumnServer.setCellFactory(column -> {//показывает как выглядит ячейка в столбце
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {//item - значение ячейки, empty - информация пустая или нет ячейка
                    super.updateItem(item, empty);
                    if (item == null || empty) {//если лонг не заполнен или ячейка пустая
                        setText(null);//то в ячеке ничего не пишем
                        setStyle("");//и она никак не выглядит

                    } else {//если же что то есть => формируем как она должна это отобразить
                        this.setTextFill(Color.LIMEGREEN);
                        this.setStyle("-fx-background-color: #353535;");
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "[DIR]";
                        }

                        setText(text);
                    }
                }
            };
        });

        //создадим столбец даты
        DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");//создали свой формат даты
        TableColumn<FileInfo, String> fileDateColumnServer = new TableColumn<>("date modified");
        fileDateColumnServer.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumnServer.setPrefWidth(120);


        filesTableServer.getColumns().addAll(fileTypeColumnServer, filenameColumnServer, fileSizeColumnServer, fileDateColumnServer);//добавляем в список столбцы выше созданные
        filesTableServer.getSortOrder().add(fileTypeColumnServer);//стартовая сортировка использует столбец с типами файлов

        disksBoxServer.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {//запрашиваем список корневых дирректорий
            disksBoxServer.getItems().add(p.toString());//добавляем ссылку на каждый из дисков
        }
        disksBoxServer.getSelectionModel().select(0);//по умолчанию выбираем первый из них

        filesTableServer.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {//если был клик мышью два раза
                    //строим путь к корню каталога resolve(добавляем к нему имя файла на который кликнули)
                    Path path = Paths.get(pathFieldServer.getText()).resolve(filesTableServer.getSelectionModel().getSelectedItem().getFilename());
                    if (Files.isDirectory(path)) {
                        updateList(path, filesTableServer, pathFieldServer);
                    }
                }
            }
        });
        updateList(Paths.get(".", "server-repository"), filesTableServer, pathFieldServer);


        /***
         * Запуск сети => подключение к серверу
         */

        CountDownLatch connectionOpened = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(connectionOpened)).start();// TODO: 03.06.2020 Позже сделать подключения после авторизации
        try {
            connectionOpened.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }



}
