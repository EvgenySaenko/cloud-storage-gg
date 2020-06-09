package server;

import common.CommandsList;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class FileHandler {
    public enum State {
        IDLE, NAME_LENGTH, NAME, FILE_LENGTH, FILE
    }

    private State currentState = State.IDLE;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;//создадим поток записи в файл

    public void startReceive() {
        currentState = State.NAME_LENGTH;
        receivedFileLength = 0L;
        System.out.println("STATE: Start file receiving");
    }

    public void receive(ChannelHandlerContext ctx, ByteBuf buf, CallbackInfo callback) throws Exception {
            if (currentState == State.NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("STATE: Get filename length");
                    nextLength = buf.readInt();//вычитываем длину
                    currentState = State.NAME;//в состояние приема имени
                }
            }

            if (currentState == State.NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);//вычитываем имя файла
                    System.out.println("STATE: Filename received - " + new String(fileName, "UTF-8"));
                    out = new BufferedOutputStream(new FileOutputStream("server-repository/" + new String(fileName)));
                    currentState = State.FILE_LENGTH;//состояние длины файла
                }
            }

            if (currentState == State.FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    fileLength = buf.readLong();//вычитываем длину файла
                    System.out.println("STATE: File length received - " + fileLength);
                    currentState = State.FILE;
                }
            }
            if (currentState == State.FILE) {// если в состоянии прием файла
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());//вычитываем из буфера в файл
                    receivedFileLength++;//увеличиваем счетчик
                    if (fileLength == receivedFileLength) {
                        //сбросили статус в дефолд так как файл получили
                        currentState = State.IDLE;
                        System.out.println("File received");
                        out.close();
                        //сообщаем => закончили получать файл, чтобы тот кто кидает кусками данные понял что мы закончили выполнять операцию
                        callback.execute();
                        return;
                    }
                }
            }

    }
}