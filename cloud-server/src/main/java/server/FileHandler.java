package server;

import common.CommandsList;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class FileHandler extends ChannelInboundHandlerAdapter {

    private Runnable callback = () -> {
        System.out.println("Операция завершена");
    };

    public enum State {
        IDLE, NAME_LENGTH, NAME, FILE_LENGTH, FILE
    }

    public enum ReceptionStatus {
        IDLE, FILE, COMMAND
    }

    private State currentState = State.IDLE;
    private  ReceptionStatus status = ReceptionStatus.IDLE;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        CommandHandler cmdHandler = new CommandHandler();
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                byte signalByte= buf.readByte();
                if (signalByte == CommandsList.FILE_SIGNAL_BYTE) {
                    status = ReceptionStatus.FILE;//статус получения файла
                    currentState = State.NAME_LENGTH;
                    receivedFileLength = 0L;
                    System.out.println("STATE: Start file receiving");
                } else if (signalByte == CommandsList.CMD_SIGNAL_BYTE) {
                    status = ReceptionStatus.COMMAND;//статус получения команды
                    //то вызываем метод ресив обработчика команд
                    cmdHandler.receive(ctx,buf,callback);// передаем калбэк
                    System.out.println("STATE: Start command receiving ");
                }
            }


            if (currentState == State.NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("STATE: Get filename length");
                    nextLength = buf.readInt();
                    currentState = State.NAME;
                }
            }

            if (currentState == State.NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    System.out.println("STATE: Filename received - " + new String(fileName, "UTF-8"));
                    out = new BufferedOutputStream(new FileOutputStream("server-repository/" + new String(fileName)));
                    currentState = State.FILE_LENGTH;
                }
            }

            if (currentState == State.FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    fileLength = buf.readLong();
                    System.out.println("STATE: File length received - " + fileLength);
                    currentState = State.FILE;
                }
            }

            if (currentState == State.FILE) {
                while (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileLength++;
                    if (fileLength == receivedFileLength) {
                        status = ReceptionStatus.IDLE;//сбросили статус в дефолд так как файл получили
                        currentState = State.IDLE;
                        System.out.println("File received");
                        out.close();
                        break;
                    }
                }
            }
        }
        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
