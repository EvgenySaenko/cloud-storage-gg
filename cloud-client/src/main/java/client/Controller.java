package client;

import common.CommandsList;
import common.FileSender;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class Controller implements Initializable {

    @FXML
    TableView <FileInfo> filesTable;

    @FXML
    ComboBox disksBox;

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
        Network.getInstance().stop();
        System.out.println("Клиент отключился");
    }

    //метод собирает из какого то пути по любому пути собирает список файлов
    public void updateList(Path path){
        filesTable.getItems().clear();//вначале чистим список файлов
        //получаем ссылку на список файлов в таблице, добавляем пачку файлов,получаем поток путей в директории,
        //map(это преобразование данных)мы берем каждый путь из этого потока и отдаем в конструктор fileinfo
        //collect - собираем все это в лист
        try {
            //pathField.setText(path.normalize().toAbsolutePath().toString());
            filesTable.getItems().addAll(Files.list(path).map(FileInfo :: new).collect(Collectors.toList()));
            filesTable.sort();//отсортировать таблицу по умолчанию
        } catch (IOException e) {
            //всплывающее окно с сообщением
            Alert alert = new Alert(Alert.AlertType.WARNING,"Не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();//показать окно и подождать пока не нажмут ОК
        }
    }

//    //метод при нажатии на кнопку UP переходит в верхнюю директорию если таковая есть
//    public void btnPathUpAction(ActionEvent actionEvent) {
//        //запрашиваем весь путь в строке пути, и у него запрашиваем ссылку на папку где он лежит
//        Path upperPath = Paths.get(pathField.getText()).getParent();
//        if (upperPath != null){//если есть еще выше дирректория
//            updateList(upperPath);
//        }
//    }
//    //метод переходит в директорию выбранного из комбобокса
//    public void selectDiskAction(ActionEvent actionEvent) {
//        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();//получили ссылку на источник события => комбобокс
//        updateList(Paths.get(element.getSelectionModel().getSelectedItem()));//вызываем updateList у выбраного диска
//    }
//
//    //метод определяющий в какой панели мы что выделили или нажали
//    public String getSelectedFileName(){
//        if (!filesTable.isFocused()){//если таблица не выбрана(не выделена)
//            return null;
//        }
//        return filesTable.getSelectionModel().getSelectedItem().getFilename();//вернет имя файла на которую смотрим
//    }
//
//    //запрос текущего пути
//    public String getCurrentPath(){
//        return pathField.getText();
//    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //создадим столбец которые будет содержать тип директории файл или директория F D
        TableColumn<FileInfo,String> fileTypeColumn = new TableColumn<>();

        //ниже узнаем D или F
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);

        //создадим столбец которые будет содержать тип директории файл или директория F D
        TableColumn<FileInfo,String> filenameColumn = new TableColumn<>("name");
        //ниже узнаем D или F
        filenameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFilename()));
        filenameColumn.setPrefWidth(240);

        // создадим столбец размера файла
        TableColumn<FileInfo,Long> fileSizeColumn = new TableColumn<>("size");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setPrefWidth(120);
        fileSizeColumn.setCellFactory(column ->{//показывает как выглядит ячейка в столбце
            return new TableCell<FileInfo, Long>(){
                @Override
                protected void updateItem(Long item, boolean empty) {//item - значение ячейки, empty - информация пустая или нет ячейка
                    super.updateItem(item, empty);
                    if (item == null || empty){//если лонг не заполнен или ячейка пустая
                        setText(null);//то в ячеке ничего не пишем
                        setStyle("");//и она никак не выглядит

                    }else {//если же что то есть => формируем как она должна это отобразить
                        this.setTextFill(Color.LIMEGREEN);
                        this.setStyle("-fx-background-color: #353535;");
                        String text = String.format("%,d bytes", item);
                        if (item == -1L){
                            text = "[DIR]";
                        }

                        setText(text);
                    }
                }
            };
        });

        //создадим столбец даты
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");//создали свой формат даты
        TableColumn<FileInfo,String> fileDateColumn = new TableColumn<>("date modified");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(120);


        filesTable.getColumns().addAll(fileTypeColumn,filenameColumn,fileSizeColumn,fileDateColumn);//добавляем в список столбцы выше созданные
        filesTable.getSortOrder().add(fileTypeColumn);//стартовая сортировка использует столбец с типами файлов

        disksBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()){//запрашиваем список корневых дирректорий
            disksBox.getItems().add(p.toString());//добавляем ссылку на каждый из дисков
        }
        disksBox.getSelectionModel().select(0);//по умолчанию выбираем первый из них

//        filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent event) {
//                if (event.getClickCount() ==2){//если был клик мышью два раза
//                    //строим путь к корню каталога resolve(добавляем к нему имя файла на который кликнули)
//                    Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFilename());
//                    if (Files.isDirectory(path)){
//                        updateList(path);
//                    }
//                }
//            }
//        });
        updateList(Paths.get(".","client-repository"));




        //////////////////////////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////////////////////////



        /***
         * Запуск сети => подключение к серверу
         */

        CountDownLatch connectionOpened = new CountDownLatch(1);
        new Thread(() ->  Network.getInstance().start(connectionOpened)).start();// TODO: 03.06.2020 Позже сделать подключения после авторизации
        try {
            connectionOpened.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }





    }

    //посылает запрос на сервер
    public static void sendFileRequest(String filename, Channel outChannel){
        byte [] filenameBytes = ("/request " + filename).getBytes(StandardCharsets.UTF_8);//вычитываем байты из имени файла
        //1 (SIGNAL BYTES) + 4 FILENAME_LENGTH(int) + FILENAME
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameBytes.length);//собираем количество байт в посылке
        buf.writeByte(CommandsList.CMD_SIGNAL_BYTE);//кладем сигнальный байт в буфер
        buf.writeInt(filenameBytes.length);//в буфер длину имени
        buf.writeBytes(filenameBytes);//само имя
        outChannel.writeAndFlush(buf);
    }
}
