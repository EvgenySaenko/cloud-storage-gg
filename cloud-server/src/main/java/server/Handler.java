package server;

import common.CommandsList;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


public class Handler extends ChannelInboundHandlerAdapter {

    public enum ReceptionStatus {
        IDLE, FILE, COMMAND
    }

    private  ReceptionStatus status;
    private CommandHandler cmdHandler;
    private FileHandler fileHandler;
    private CallbackInfo callback;

    public Handler(){//получает дефолт статус и обработчик команд и фалов
        this.status = ReceptionStatus.IDLE;
        this.cmdHandler = new CommandHandler();
        this.fileHandler = new FileHandler();
        this.callback = () -> {
            System.out.println("Файл успешно получен");
            this.status = ReceptionStatus.IDLE;
        };

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (this.status == ReceptionStatus.IDLE) {//если статус дефолт
                byte signalByte = buf.readByte();
                if (signalByte == CommandsList.FILE_SIGNAL_BYTE) {
                    status = ReceptionStatus.FILE;//статус получения файла
                    fileHandler.startReceive();//вызываем обработчик файлов
                }else if (signalByte == CommandsList.CMD_SIGNAL_BYTE) {
                    status = ReceptionStatus.COMMAND;//статус получения команды
                    //то вызываем метод ресив обработчика команд
                    cmdHandler.startReceive();
                }
            }
            if (status == ReceptionStatus.FILE){
                fileHandler.receive(ctx,buf,callback);//вызываем обработчик файлов
            }
            if (status == ReceptionStatus.COMMAND){
                cmdHandler.receive(ctx, buf, callback);//вызываем обработчик команд
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
