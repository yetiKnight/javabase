package priv.captain.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 各种IO流方式对比
 */
public class IOCompareDemo {

    public static void main(String[] args) throws IOException {
        // 获取项目根目录的绝对路径
        String projectRoot = System.getProperty("user.dir");
        String sourceFileName = projectRoot + "/src/main/java/priv/captain/io/source.txt";
        String targetFileName = projectRoot + "/src/main/java/priv/captain/io/target.txt";

        // basicIO(sourceFileName, targetFileName);
        // bufferIo(sourceFileName, targetFileName);
        // nio1(sourceFileName, targetFileName);
        nioFileChannel(sourceFileName, targetFileName);
        System.out.println("拷贝完成！");
    }

    /**
     * 基础阻塞式IO流，直接IO
     * 
     * @param sourceFileName
     * @param targetFileName
     */
    private static void basicIO(String sourceFileName, String targetFileName) {

        try (FileInputStream fis = new FileInputStream(sourceFileName);
                FileOutputStream fos = new FileOutputStream(targetFileName)) {
            /**
             * 缓冲区大小一般是1KB、4KB、8KB、16KB，系统兼容性的话8KB比较好
             * 缓冲区的目的是减少IO操作，优化性能，如果设置过小则IO频繁，性能下降，如果设置过大则会浪费内存。
             * 缓冲区的大小由实际情况决定，也可以考虑使用动态的缓冲区值
             */
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 面向缓冲流的IO流，间接IO，先经过内部自动管理的缓冲区，不会直接和设备交互
     * 
     * @param sourceFileName
     * @param targetFileName
     */
    private static void bufferIo(String sourceFileName, String targetFileName) {

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFileName));
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFileName))) {
            int data;
            while ((data = bis.read()) != -1) {
                bos.write(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * NIO 方式拷贝，java 1.7+
     * Files.copy 由JVM根据操作系统和文件系统自主选择最佳的复制方式，在大多数情况下，它会利用底层的 FileChannel.transferTo() 实现零拷贝
     * 
     * @param sourceFileName
     * @param targetFileName
     * @throws IOException
     */
    private static void nio1(String sourceFileName, String targetFileName) throws IOException {

        Path sourcePath = Path.of(sourceFileName);
        Path targePath = Path.of(targetFileName);
        Files.copy(sourcePath, targePath, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * NIO FileChannel写法，适用于需要极致性能或特殊控制的专家级场景
     * 
     * @param sourceFileName
     * @param targetFileName
     * @throws IOException
     */
    private static void nioFileChannel(String sourceFileName, String targetFileName) throws IOException {
        // 分开声明，否则会有资源泄漏
        try (
                FileInputStream fis = new FileInputStream(sourceFileName);
                FileOutputStream fos = new FileOutputStream(targetFileName);
                FileChannel sourceChannel = fis.getChannel();
                FileChannel targetChannel = fos.getChannel()) {
            sourceChannel.transferTo(0, sourceChannel.size(), targetChannel);
        }
    }
}
