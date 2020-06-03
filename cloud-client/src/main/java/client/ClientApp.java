package client;

import common.CommandsList;
import common.FileSender;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class ClientApp{

    public static void main(String[] args) {
        CountDownLatch connectionOpened = new CountDownLatch(1);
        new Thread(() ->  Network.getInstance().start(connectionOpened)).start();
        try {
            connectionOpened.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Scanner sc = new Scanner(System.in);
        while (true){
            String cmd = sc.nextLine();
            if (cmd.equals("/exit ")){
                break;
            }
            if (cmd.startsWith("/send ")){
                String filename = cmd.split("\\s")[1];
                Path filePath = Paths.get("client-repository",filename);
                if (!Files.exists(filePath)){
                    System.out.println("Файл не найден для отправки");
                    continue;
                }
                try {
                    FileSender.sendFile(filePath, Network.getInstance().getCurrentChannel(), future -> {
                        if (!future.isSuccess()) {
                            System.out.println("Не удалось отправить файл");

                            future.cause().printStackTrace();
                        }
                        if (future.isSuccess()) {
                            System.out.println("Файл успешно передан");
                        }
                    });
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (cmd.startsWith("/download ")){
                String filename = cmd.split("\\s")[1];
                sendFileRequest(filename,Network.getInstance().getCurrentChannel());
                continue;
            }
            System.out.println("Введена неверная команда попробуйте снова");
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
