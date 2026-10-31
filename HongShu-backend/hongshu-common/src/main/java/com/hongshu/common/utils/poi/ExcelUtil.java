package com.hongshu.common.utils.poi;

import com.hongshu.common.annotation.Excel;
import com.hongshu.common.annotation.Excels;
import com.hongshu.common.config.HongshuConfig;
import com.hongshu.common.core.domain.AjaxResult;
import com.hongshu.common.core.text.Convert;
import com.hongshu.common.exception.UtilException;
import com.hongshu.common.utils.DateUtils;
import com.hongshu.common.utils.DictUtils;
import com.hongshu.common.utils.StringUtils;
import com.hongshu.common.utils.file.FileTypeUtils;
import com.hongshu.common.utils.file.FileUtils;
import com.hongshu.common.utils.file.ImageUtils;
import com.hongshu.common.utils.reflect.ReflectUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Excel相关处理
 *
 * @author hongshu
 */
public class ExcelUtil<T> {

    private static final Logger log = LoggerFactory.getLogger(ExcelUtil.class);

    public static final String FORMULA_REGEX_STR = "=|-|\\+|@";

    public static final String[] FORMULA_STR = {"=", "-", "+", "@"};

    /**
     * 用于dictType属性数据存储，避免重复查缓存
     */
    public final Map<String, String> sysDictMap = new HashMap<>();

    /**
     * Excel sheet最大行数，默认65536
     */
    public static final int sheetSize = 65536;

    /**
     * 工作表名称
     */
    private String sheetName;

    /**
     * 导出类型（EXPORT:导出数据；IMPORT：导入模板）
     */
    private Excel.Type type;

    /**
     * 工作薄对象
     */
    private Workbook wb;

    /**
     * 工作表对象
     */
    private Sheet sheet;

    /**
     * 样式列表
     */
    private Map<String, CellStyle> styles;

    /**
     * 导入导出数据列表
     */
    private List<T> list;

    /**
     * 注解列表
     */
    private List<Object[]> fields;

    /**
     * 当前行号
     */
    private int rownum;

    /**
     * 标题
     */
    private String title;

    /**
     * 最大高度
     */
    private short maxHeight;

    /**
     * 合并后最后行数
     */
    private int subMergedLastRowNum = 0;

    /**
     * 合并后开始行数
     */
    private int subMergedFirstRowNum = 1;

    /**
     * 对象的子列表方法
     */
    private Method subMethod;

    /**
     * 对象的子列表属性
     */
    private List<Field> subFields;

    /**
     * 统计列表
     */
    private final Map<Integer, Double> statistics = new HashMap<>();

    /**
     * 数字格式
     */
    private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("######0.00");

    /**
     * 实体对象
     */
    public final Class<T> clazz;

    /**
     * 需要排除列属性
     */
    public String[] excludeFields;

    public ExcelUtil(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * 隐藏Excel中列属性
     *
     * @param fields 列属性名 示例[单个"name"/多个"id","name"]
     */
    public void hideColumn(String... fields) {
        this.excludeFields = fields;
    }

    public void init(List<T> list, String sheetName, String title, Excel.Type type) {
        if (list == null) {
            list = new ArrayList<>();
        }
        this.list = list;
        this.sheetName = sheetName;
        this.type = type;
        this.title = title;
        createExcelField();
        createWorkbook();
        createTitle();
        createSubHead();
    }

    /**
     * 创建excel第一行标题
     */
    public void createTitle() {
        if (StringUtils.isNotEmpty(title)) {
            subMergedFirstRowNum++;
            subMergedLastRowNum++;
            int titleLastCol = this.fields.size() - 1;
            if (isSubList()) {
                titleLastCol = titleLastCol + subFields.size() - 1;
            }
            Row titleRow = sheet.createRow(rownum == 0 ? rownum++ : 0);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellStyle(styles.get("title"));
            titleCell.setCellValue(title);
            sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), titleRow.getRowNum(), titleLastCol));
        }
    }

    /**
     * 创建对象的子列表名称
     */
    public void createSubHead() {
        if (isSubList()) {
            subMergedFirstRowNum++;
            subMergedLastRowNum++;
            Row subRow = sheet.createRow(rownum);
            int excelNum = 0;
            for (Object[] objects : fields) {
                Excel attr = (Excel) objects[1];
                Cell headCell1 = subRow.createCell(excelNum);
                headCell1.setCellValue(attr.name());
                headCell1.setCellStyle(styles.get(StringUtils.format("header_{}_{}", attr.headerColor(), attr.headerBackgroundColor())));
                excelNum++;
            }
            int headFirstRow = excelNum - 1;
            int headLastRow = headFirstRow + subFields.size() - 1;
            if (headLastRow > headFirstRow) {
                sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, headFirstRow, headLastRow));
            }
            rownum++;
        }
    }

    /**
     * 对excel表单默认第一个索引名转换成list
     *
     * @param is 输入流
     * @return 转换后集合
     */
    public List<T> importExcel(InputStream is) {
        List<T> list;
        try {
            list = importExcel(is, 0);
        } catch (Exception e) {
            log.error("导入Excel异常{}", e.getMessage());
            throw new UtilException(e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
        return list;
    }

    /**
     * 对excel表单默认第一个索引名转换成list
     *
     * @param is       输入流
     * @param titleNum 标题占用行数
     * @return 转换后集合
     */
    public List<T> importExcel(InputStream is, int titleNum) throws Exception {
        return importExcel(StringUtils.EMPTY, is, titleNum);
    }

    /**
     * 对excel表单指定表格索引名转换成list
     *
     * @param sheetName 表格索引名
     * @param titleNum  标题占用行数
     * @param is        输入流
     * @return 转换后集合
     */
    public List<T> importExcel(String sheetName, InputStream is, int titleNum) throws Exception {
        this.type = Excel.Type.IMPORT;
        try (Workbook workbook = WorkbookFactory.create(is)) {
            this.wb = workbook;
            return readImportSheet(sheetName, titleNum);
        }
    }

    /**
     * 在工作簿关闭前完成数据及图片读取，避免导入异常时遗留底层资源。
     */
    private List<T> readImportSheet(String sheetName, int titleNum) throws Exception {
        List<T> list = new ArrayList<>();
        // 如果指定sheet名,则取指定sheet中的内容 否则默认指向第1个sheet
        Sheet sheet = StringUtils.isNotEmpty(sheetName) ? wb.getSheet(sheetName) : wb.getSheetAt(0);
        if (sheet == null) {
            throw new IOException("文件sheet不存在");
        }
        boolean isXSSFWorkbook = !(wb instanceof HSSFWorkbook);
        Map<String, PictureData> pictures;
        if (isXSSFWorkbook) {
            pictures = getSheetPictures07((XSSFSheet) sheet, (XSSFWorkbook) wb);
        } else {
            pictures = getSheetPictures03((HSSFSheet) sheet, (HSSFWorkbook) wb);
        }
        // 获取最后一个非空行的行下标，比如总行数为n，则返回的为n-1
        int rows = sheet.getLastRowNum();
        if (rows > 0) {
            // 定义一个map用于存放excel列的序号和field.
            Map<String, Integer> cellMap = new HashMap<>();
            // 获取表头
            Row heard = sheet.getRow(titleNum);
            for (int i = 0; i < heard.getPhysicalNumberOfCells(); i++) {
                Cell cell = heard.getCell(i);
                if (StringUtils.isNotNull(cell)) {
                    String value = this.getCellValue(heard, i).toString();
                    cellMap.put(value, i);
                } else {
                    cellMap.put(null, i);
                }
            }
            // 有数据时才处理 得到类的所有field.
            List<Object[]> fields = this.getFields();
            Map<Integer, Object[]> fieldsMap = new HashMap<>();
            for (Object[] objects : fields) {
                Excel attr = (Excel) objects[1];
                Integer column = cellMap.get(attr.name());
                if (column != null) {
                    fieldsMap.put(column, objects);
                }
            }
            for (int i = titleNum + 1; i <= rows; i++) {
                // 从第2行开始取数据,默认第一行是表头.
                Row row = sheet.getRow(i);
                // 判断当前行是否是空行
                if (isRowEmpty(row)) {
                    continue;
                }
                T entity = null;
                for (Map.Entry<Integer, Object[]> entry : fieldsMap.entrySet()) {
                    Object val = this.getCellValue(row, entry.getKey());

                    // 如果不存在实例则新建.
                    entity = (entity == null ? clazz.getDeclaredConstructor().newInstance() : entity);
                    // 从map中得到对应列的field.
                    Field field = (Field) entry.getValue()[0];
                    Excel attr = (Excel) entry.getValue()[1];
                    // 取得类型,并根据对象类型设置值.
                    Class<?> fieldType = field.getType();
                    if (String.class == fieldType) {
                        String s = Convert.toStr(val);
                        if (StringUtils.endsWith(s, ".0")) {
                            val = StringUtils.substringBefore(s, ".0");
                        } else {
                            String dateFormat = field.getAnnotation(Excel.class).dateFormat();
                            if (StringUtils.isNotEmpty(dateFormat)) {
                                val = parseDateToStr(dateFormat, val);
                            } else {
                                val = Convert.toStr(val);
                            }
                        }
                    } else if ((Integer.TYPE == fieldType || Integer.class == fieldType) && StringUtils.isNumeric(Convert.toStr(val))) {
                        val = Convert.toInt(val);
                    } else if ((Long.TYPE == fieldType || Long.class == fieldType) && StringUtils.isNumeric(Convert.toStr(val))) {
                        val = Convert.toLong(val);
                    } else if (Double.TYPE == fieldType || Double.class == fieldType) {
                        val = Convert.toDouble(val);
                    } else if (Float.TYPE == fieldType || Float.class == fieldType) {
                        val = Convert.toFloat(val);
                    } else if (BigDecimal.class == fieldType) {
                        val = Convert.toBigDecimal(val);
                    } else if (Date.class == fieldType) {
                        if (val instanceof String) {
                            val = DateUtils.parseDate(val);
                        } else if (val instanceof Double) {
                            val = DateUtil.getJavaDate((Double) val);
                        }
                    } else if (Boolean.TYPE == fieldType || Boolean.class == fieldType) {
                        val = Convert.toBool(val, false);
                    }
                    String propertyName = field.getName();
                    if (StringUtils.isNotEmpty(attr.targetAttr())) {
                        propertyName = field.getName() + "." + attr.targetAttr();
                    }
                    if (StringUtils.isNotEmpty(attr.readConverterExp())) {
                        val = reverseByExp(Convert.toStr(val), attr.readConverterExp(), attr.separator());
                    } else if (StringUtils.isNotEmpty(attr.dictType())) {
                        val = reverseDictByExp(Convert.toStr(val), attr.dictType(), attr.separator());
                    } else if (!attr.handler().equals(ExcelHandlerAdapter.class)) {
                        val = dataFormatHandlerAdapter(val, attr, null);
                    } else if (Excel.ColumnType.IMAGE == attr.cellType() && StringUtils.isNotEmpty(pictures)) {
                        PictureData image = pictures.get(row.getRowNum() + "_" + entry.getKey());
                        if (image == null) {
                            val = "";
                        } else {
                            byte[] data = image.getData();
                            val = FileUtils.writeImportBytes(data);
                        }
                    }
                    ReflectUtils.invokeSetter(entity, propertyName, val);
                }
                list.add(entity);
            }
        }
        return list;
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     * @return 结果
     */
    public AjaxResult exportExcel(List<T> list, String sheetName) {
        return exportExcel(list, sheetName, StringUtils.EMPTY);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     * @param title     标题
     * @return 结果
     */
    public AjaxResult exportExcel(List<T> list, String sheetName, String title) {
        this.init(list, sheetName, title, Excel.Type.EXPORT);
        return exportExcel();
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param response  返回数据
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     */
    public void exportExcel(HttpServletResponse response, List<T> list, String sheetName) {
        exportExcel(response, list, sheetName, StringUtils.EMPTY);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param response  返回数据
     * @param list      导出数据集合
     * @param sheetName 工作表的名称
     * @param title     标题
     */
    public void exportExcel(HttpServletResponse response, List<T> list, String sheetName, String title) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        this.init(list, sheetName, title, Excel.Type.EXPORT);
        exportExcel(response);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param sheetName 工作表的名称
     * @return 结果
     */
    public AjaxResult importTemplateExcel(String sheetName) {
        return importTemplateExcel(sheetName, StringUtils.EMPTY);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param sheetName 工作表的名称
     * @param title     标题
     * @return 结果
     */
    public AjaxResult importTemplateExcel(String sheetName, String title) {
        this.init(null, sheetName, title, Excel.Type.IMPORT);
        return exportExcel();
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param sheetName 工作表的名称
     */
    public void importTemplateExcel(HttpServletResponse response, String sheetName) {
        importTemplateExcel(response, sheetName, StringUtils.EMPTY);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @param sheetName 工作表的名称
     * @param title     标题
     */
    public void importTemplateExcel(HttpServletResponse response, String sheetName, String title) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        this.init(null, sheetName, title, Excel.Type.IMPORT);
        exportExcel(response);
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     */
    public void exportExcel(HttpServletResponse response) {
        try {
            writeSheet();
            wb.write(response.getOutputStream());
        } catch (Exception e) {
            log.error("向 HTTP 响应导出 Excel 失败", e);
        } finally {
            IOUtils.closeQuietly(wb);
        }
    }

    /**
     * 对list数据源将其里面的数据导入到excel表单
     *
     * @return 结果
     */
    public AjaxResult exportExcel() {
        OutputStream out = null;
        try {
            writeSheet();
            String filename = encodingFilename(sheetName);
            out = new FileOutputStream(getAbsoluteFile(filename));
            wb.write(out);
            return AjaxResult.success(filename);
        } catch (Exception e) {
            log.error("向文件导出 Excel 失败，sheetName={}", sheetName, e);
            throw new UtilException("导出Excel失败，请联系网站管理员！");
        } finally {
            IOUtils.closeQuietly(wb);
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * 创建写入数据到Sheet
     */
    public void writeSheet() {
        // 取出一共有多少个sheet.
        int sheetNo = Math.max(1, (int) Math.ceil(list.size() * 1.0 / sheetSize));
        for (int index = 0; index < sheetNo; index++) {
            createSheet(sheetNo, index);

            // 产生一行
            Row row = sheet.createRow(rownum);
            int column = 0;
            // 写入各个字段的列头名称
            for (Object[] os : fields) {
                Field field = (Field) os[0];
                Excel excel = (Excel) os[1];
                if (Collection.class.isAssignableFrom(field.getType())) {
                    for (Field subField : subFields) {
                        Excel subExcel = subField.getAnnotation(Excel.class);
                        this.createHeadCell(subExcel, row, column++);
                    }
                } else {
                    this.createHeadCell(excel, row, column++);
                }
            }
            if (Excel.Type.EXPORT.equals(type)) {
                fillExcelData(index);
                addStatisticsRow();
            }
        }
    }
}
