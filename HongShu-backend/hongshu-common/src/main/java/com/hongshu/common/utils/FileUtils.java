package com.hongshu.common.utils;

import com.hongshu.common.constant.Constantss;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiImageType;
import com.vladsch.flexmark.ext.emoji.EmojiShortcutType;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import jakarta.servlet.http.HttpServletRequest;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * 文件操作工具类
 *
 * @author hongshu
 */
@Slf4j
public class FileUtils {

    // 图片类型
    private static final List<String> pictureTypes = new ArrayList<>();

    static {
        pictureTypes.add("jpg");
        pictureTypes.add("jpeg");
        pictureTypes.add("bmp");
        pictureTypes.add("gif");
        pictureTypes.add("png");
    }

    /**
     * 判断是否是图片
     *
     */
    public static Boolean isPicture(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return false;
        }
        String expandName = getPicExpandedName(fileName);
        return pictureTypes.contains(expandName);
    }

    /**
     * 判断是否为markdown文件
     *
     */
    public static Boolean isMarkdown(String fileName) {
        return fileName.contains(".md");
    }

    /**
     * Markdown转Html
     *
     */
    public static String markdownToHtml(String markdown) {
        MutableDataSet options = new MutableDataSet().set(Parser.EXTENSIONS, Arrays.asList(
                        AutolinkExtension.create(),
                        EmojiExtension.create(),
                        StrikethroughExtension.create(),
                        TaskListExtension.create(),
                        TablesExtension.create()
                ))
                // set GitHub table parsing options
                .set(TablesExtension.WITH_CAPTION, false)
                .set(TablesExtension.COLUMN_SPANS, false)
                .set(TablesExtension.MIN_HEADER_ROWS, 1)
                .set(TablesExtension.MAX_HEADER_ROWS, 1)
                .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
                .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
                .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true)

                // setup emoji shortcut options
                // uncomment and change to your image directory for emoji images if you have it setup
                //.set(EmojiExtension.ROOT_IMAGE_PATH, emojiInstallDirectory())
                .set(EmojiExtension.USE_SHORTCUT_TYPE, EmojiShortcutType.GITHUB)
                .set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.IMAGE_ONLY)
                // other options
                ;
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    /**
     * Html 转 Markdown
     *
     */
    public static String htmlToMarkdown(String html) {
        MutableDataSet options = new MutableDataSet();
        return FlexmarkHtmlConverter.builder(options).build().convert(html);
    }

    /**
     * 获取后缀名
     *
     */
    public static String getPicExpandedName(String fileName) {
        String ext = "";
        if (StringUtils.isNotBlank(fileName) &&
                StringUtils.contains(fileName, ".")) {
            ext = StringUtils.substring(fileName, fileName.lastIndexOf(".") + 1);
        }
        ext = ext.toLowerCase();
        if (ext.isEmpty()) {
            ext = "jpg";
        }

        return ext;
    }

    /**
     * 根据OriginalFilename获取FileName【去除文件后缀】
     *
     */
    public static String getFileName(String originalFilename) {
        String fileName = "";
        if (StringUtils.isNotBlank(originalFilename) &&
                StringUtils.contains(originalFilename, ".")) {
            fileName = originalFilename.substring(0, originalFilename.lastIndexOf("."));
        }
        return fileName;
    }

    /**
     * 从Request中获取文件
     *
     */
    public static List<MultipartFile> getMultipartFileList(HttpServletRequest request) {
        List<MultipartFile> files = new ArrayList<>();
        try {
            StandardServletMultipartResolver multipartResolver = new StandardServletMultipartResolver();
            if (request instanceof MultipartHttpServletRequest multiRequest) {
                // 将request变成多部分request

                Iterator<String> iter = multiRequest.getFileNames();
                // 检查form中是否有enctype="multipart/form-data"
                if (multipartResolver.isMultipart(request) && iter.hasNext()) {
                    // 获取multiRequest 中所有的文件名
                    while (iter.hasNext()) {
                        List<MultipartFile> fileRows = multiRequest
                                .getFiles(iter.next());
                        if (!fileRows.isEmpty()) {
                            for (MultipartFile file : fileRows) {
                                if (file != null && !file.isEmpty()) {
                                    files.add(file);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("解析MultipartRequest错误", ex);
        }
        return files;
    }

    public static final String[] IMG_FILE = {
            Constantss.FILE_SUFFIX_BMP,
            Constantss.FILE_SUFFIX_JPG,
            Constantss.FILE_SUFFIX_PNG,
            Constantss.FILE_SUFFIX_TIF,
            Constantss.FILE_SUFFIX_GIF,
            Constantss.FILE_SUFFIX_JPEG,
            Constantss.FILE_SUFFIX_WEBP};
    public static final String[] DOC_FILE = {
            Constantss.FILE_SUFFIX_DOC,
            Constantss.FILE_SUFFIX_DOCX,
            Constantss.FILE_SUFFIX_TXT,
            Constantss.FILE_SUFFIX_HLP,
            Constantss.FILE_SUFFIX_WPS,
            Constantss.FILE_SUFFIX_RTF,
            Constantss.FILE_SUFFIX_XLS,
            Constantss.FILE_SUFFIX_XLSX,
            Constantss.FILE_SUFFIX_PPT,
            Constantss.FILE_SUFFIX_PPTX,
            Constantss.FILE_SUFFIX_JAVA,
            Constantss.FILE_SUFFIX_HTML,
            Constantss.FILE_SUFFIX_PDF,
            Constantss.FILE_SUFFIX_MD,
            Constantss.FILE_SUFFIX_SQL,
            Constantss.FILE_SUFFIX_CSS,
            Constantss.FILE_SUFFIX_JS,
            Constantss.FILE_SUFFIX_VUE,
            Constantss.FILE_SUFFIX_JAVA};
    public static final String[] VIDEO_FILE = {
            Constantss.FILE_SUFFIX_AVI,
            Constantss.FILE_SUFFIX_MP4,
            Constantss.FILE_SUFFIX_MPG,
            Constantss.FILE_SUFFIX_MOV,
            Constantss.FILE_SUFFIX_SWF,
            Constantss.FILE_SUFFIX_3GP,
            Constantss.FILE_SUFFIX_RM,
            Constantss.FILE_SUFFIX_RMVB,
            Constantss.FILE_SUFFIX_WMV,
            Constantss.FILE_SUFFIX_MKV};
    public static final String[] MUSIC_FILE = {
            Constantss.FILE_SUFFIX_WAV,
            Constantss.FILE_SUFFIX_AIF,
            Constantss.FILE_SUFFIX_AU,
            Constantss.FILE_SUFFIX_MP3,
            Constantss.FILE_SUFFIX_RAM,
            Constantss.FILE_SUFFIX_WMA,
            Constantss.FILE_SUFFIX_MMF,
            Constantss.FILE_SUFFIX_AMR,
            Constantss.FILE_SUFFIX_AAC,
            Constantss.FILE_SUFFIX_FLAC};
    public static final String[] ALL_FILE = {
            Constantss.FILE_SUFFIX_BMP,
            Constantss.FILE_SUFFIX_JPG,
            Constantss.FILE_SUFFIX_PNG,
            Constantss.FILE_SUFFIX_TIF,
            Constantss.FILE_SUFFIX_GIF,
            Constantss.FILE_SUFFIX_JPEG,
            Constantss.FILE_SUFFIX_WEBP,

            Constantss.FILE_SUFFIX_DOC,
            Constantss.FILE_SUFFIX_DOCX,
            Constantss.FILE_SUFFIX_TXT,
            Constantss.FILE_SUFFIX_HLP,
            Constantss.FILE_SUFFIX_WPS,
            Constantss.FILE_SUFFIX_RTF,
            Constantss.FILE_SUFFIX_XLS,
            Constantss.FILE_SUFFIX_XLSX,
            Constantss.FILE_SUFFIX_PPT,
            Constantss.FILE_SUFFIX_PPTX,
            Constantss.FILE_SUFFIX_JAVA,
            Constantss.FILE_SUFFIX_HTML,
            Constantss.FILE_SUFFIX_PDF,
            Constantss.FILE_SUFFIX_MD,
            Constantss.FILE_SUFFIX_SQL,
            Constantss.FILE_SUFFIX_CSS,
            Constantss.FILE_SUFFIX_JS,
            Constantss.FILE_SUFFIX_VUE,
            Constantss.FILE_SUFFIX_JAVA,

            Constantss.FILE_SUFFIX_AVI,
            Constantss.FILE_SUFFIX_MP4,
            Constantss.FILE_SUFFIX_MPG,
            Constantss.FILE_SUFFIX_MOV,
            Constantss.FILE_SUFFIX_SWF,
            Constantss.FILE_SUFFIX_3GP,
            Constantss.FILE_SUFFIX_RM,
            Constantss.FILE_SUFFIX_RMVB,
            Constantss.FILE_SUFFIX_WMV,
            Constantss.FILE_SUFFIX_MKV,

            Constantss.FILE_SUFFIX_WAV,
            Constantss.FILE_SUFFIX_AIF,
            Constantss.FILE_SUFFIX_AU,
            Constantss.FILE_SUFFIX_MP3,
            Constantss.FILE_SUFFIX_RAM,
            Constantss.FILE_SUFFIX_WMA,
            Constantss.FILE_SUFFIX_MMF,
            Constantss.FILE_SUFFIX_AMR,
            Constantss.FILE_SUFFIX_AAC,
            Constantss.FILE_SUFFIX_FLAC
    };

    /**
     * 判断一个文件是否存在，不存在就创建它 Method execute,只能建最后面那个目录
     *
     */
    public void creatFile(String path) {
        File file = new File(path);
        if (!file.isDirectory() && !file.mkdir() && !file.isDirectory()) {
            throw new IllegalStateException("无法创建目录: " + path);
        }
    }

    /**
     * 从文件名中得到其后缀名
     *
     * @return 后缀名
     */
    public static String getFileSuffix(String filename) {
        String suffix;
        suffix = filename.substring(
                filename.lastIndexOf(Constantss.SYMBOL_POINT) + 1);
        return suffix;
    }

    /**
     * 通过其后缀名判断其是否合法,合法后缀名为常见的
     *
     * @param suffix 后缀名
     * @return 合法返回true，不合法返回false
     */
    public static boolean isSafe(String suffix) {
        suffix = suffix.toLowerCase();
        for (String fileExtension : ALL_FILE) {
            if (fileExtension.equals(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 通过其后缀名判断其是否是图片
     *
     * @param suffix 后缀名
     * @return 合法返回true，不合法返回false
     */
    public static boolean isPic(String suffix) {
        suffix = suffix.toLowerCase();
        for (String imageExtension : IMG_FILE) {
            if (imageExtension.equals(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算文件大小，将long类型转换为String类型
     *
     */
    public static String getFileStringSize(long fileSize) {
        //size不能为0？
        double temp;
        String ssize;
        temp = (double) fileSize / Constantss.NUM_1024;
        if (temp >= Constantss.NUM_1024) {
            temp = temp / Constantss.NUM_1024;
            if (temp >= Constantss.NUM_1024) {
                temp = temp / Constantss.NUM_1024;
                ssize = temp + "000";
                ssize = ssize.substring(Constantss.NUM_ZERO, ssize.indexOf(Constantss.SYMBOL_POINT) + Constantss.NUM_THREE) + "GB";
            } else {
                ssize = temp + "000";
                ssize = ssize.substring(Constantss.NUM_ZERO, ssize.indexOf(Constantss.SYMBOL_POINT) + Constantss.NUM_THREE) + "MB";
            }
        } else {
            ssize = temp + "000";
            ssize = ssize.substring(Constantss.NUM_ZERO, ssize.indexOf(Constantss.SYMBOL_POINT) + Constantss.NUM_THREE) + "KB";
        }
        return ssize;
    }

    public static void main(String[] args) {
        System.out.println(pictureTypes.contains("png"));
    }
}
