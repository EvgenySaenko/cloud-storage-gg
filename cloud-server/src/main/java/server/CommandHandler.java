package server;

import common.FileSender;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.nio.file.Paths;

public class CommandHandler {
    public enum State {
        IDLE, COMMAND_LENGTH, COMMAND
    }

    private State currentState = State.IDLE;
    private int commandTypeLength;//длина типа команды
    private int receivedLength;
    private StringBuilder cmd;//создадим стрингбилдер, чтобы собрать строку

    public void startReceive() {
        currentState = State.COMMAND_LENGTH;
        cmd = new StringBuilder();
    }

    public void receive(ChannelHandlerContext ctx, ByteBuf buf, CallbackInfo callback) throws Exception {
        //77 / 14 (символов) = (/request 1.txt)<= тут 14 символов
        //сигнальный байт/ /request / 1.txt
        if (currentState == State.COMMAND_LENGTH) {//начали ожидать длину команды
            if (buf.readableBytes() >= 4) {
                System.out.println("STATE: Get command length");
                commandTypeLength = buf.readInt();
                currentState = State.COMMAND;//переходим в ожидание получения команды
                receivedLength = 0;//мы ничего еще не получали
                cmd.setLength(0);//чистим
            }
        }
        if (currentState ==  State.COMMAND) {//если в состоянии (ожидания типа команды)
            System.out.println("STATE: Get COMMAND");
            while (buf.readableBytes() > 0) {//и в буфере что то есть
                cmd.append((char)buf.readByte());
                receivedLength++;

                if (receivedLength == commandTypeLength) {//если получили столько байт сколько и ждали
                    parseCmd(ctx, cmd.toString());//парсим строчку
                    currentState = State.IDLE;
                    System.out.println("STATE: IDLE - закончили получение команды");
                    //сообщаем => закончили получать файл, чтобы тот кто кидает кусками данные понял
                    // что мы закончили выполнять операцию
                    callback.execute();
                    return;
                }
            }
        }
    }
    //метод парсит команды
    public void parseCmd(ChannelHandlerContext ctx, String cmd) throws Exception {
        if (cmd.startsWith("/request ")){ //если пришел = реквест файл
            String filenameToSend = cmd.split("\\s")[1];//выдернули имя файла
            //взяли файл из серверного репозитория, отправили на клиент
            FileSender.sendFile(Paths.get("server-repository",filenameToSend),ctx.channel(),null);
        }
    }
}
