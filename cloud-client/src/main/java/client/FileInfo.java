package client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class FileInfo {

    public enum FileType {
        FILE("F"), DIRECTORY("D");
        private String name;
        //гетер возвращает имя директории
        public String getName() {
            return name;
        }

        FileType(String name) {
            this.name = name;
        }
    }

    private String filename;
    private FileType type;
    private long size;
    private LocalDateTime lastModified;

    public String getFilename() {
        return filename;
    }

    public FileType getType() {
        return type;
    }

    public long getSize() {
        return size;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public FileInfo(Path path){//когда создается файл-инфо указываем путь к файлу на диске
        try{//по пути собираем данные об объекте
            this.filename = path.getFileName().toString();
            this.size = Files.size(path);
            this.type = Files.isDirectory(path) ? FileType.DIRECTORY: FileType.FILE;
            if (this.type == FileType.DIRECTORY){
                this.size = -1L;
            }
            //запроси дату последней модиикации
            this.lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3));
        }catch(IOException e){
            throw new RuntimeException("Unable to create file info from path");
        }

    }
}
