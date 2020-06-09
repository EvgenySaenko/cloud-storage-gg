package common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;


public class FileSender {
    //отправляем файлы
    public static void sendFile(Path path, Channel outChannel, ChannelFutureListener finishListener) throws IOException {
        FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));
        byte [] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        //1 (SIGNAL BYTES) + 4 FILENAME_LENGTH(int) + FILENAME + FILE_LENGTH(long)
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameBytes.length + 8);
        buf.writeByte(CommandsList.FILE_SIGNAL_BYTE);
        buf.writeInt(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        buf.writeLong(Files.size(path));
        outChannel.writeAndFlush(buf);

        ChannelFuture transferOperationFuture = outChannel.writeAndFlush(region);

        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }
}