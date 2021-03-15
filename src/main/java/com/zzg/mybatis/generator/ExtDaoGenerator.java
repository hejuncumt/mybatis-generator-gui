package com.zzg.mybatis.generator;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @ClassName ExtDaoGenerator
 * @Description ExtDao生成
 * @Author HeJun
 * @Author 2021/3/12 15:28
 * @Version 1.0
 **/
public class ExtDaoGenerator {

    /**
     * Mapper.java所在项目路径，具体到main层的本地路径
     */
//    private static String basePath = "C:\\yeepay-workspace\\wanli-service\\wanli-core\\src\\main\\";
    private static String basePath = "C:\\yeepay-workspace\\yeepay-phb\\phb-persistence\\src\\main\\";
    /**
     * 要生成的实体名称列表，若为空，则全部生成
     */
    private static List<String> includeEntitys = Arrays.asList("BindCard");
    /**
     * 要排除的实体名称列表，可以为空
     */
    private static List<String> excludeEntitys = Arrays.asList("PlatformCoupon", "Shop");
    /**
     * 扩展Dao类后缀名，如：UserExtDao.java
     */
    private static String extDaoSuffix = "Dao";
    /**
     * 原始dao类后缀名，如：UserRepository.java
     */
    private static String daoSuffix = "Mapper";
    /**
     * 原始dao接口包路径
     */
    private static String daoPackage = "com.yeepay.g3.core.phb.dao";
    /**
     * 原始dao实体的包路径
     */
    private static String entityPackage = "com.yeepay.g3.core.phb.po";
    /**
     * 原始Mapper.xml的路径
     */
    private static String xmlPath = "C:\\yeepay-workspace\\yeepay-phb\\phb-hessian\\src\\main\\resources\\mapper\\";
    /**
     * 扩展dao的包名、.xml目录名称
     */
    private static String extName = "ext";

    public static void main(String[] args) throws IOException {
        //所有的实体文件名
        String entityPath = packageToPath(entityPackage);
        File file = new File(basePath + entityPath);
        //排除Query文件
        Stream<File> query = Arrays.stream(file.listFiles()).filter(x -> !x.getName().endsWith("Query.java"));
        List<String> currentEntity = query.map(x -> StringUtils.substringBeforeLast(x.getName(), ".")).collect(Collectors.toList());

        String packageToPath = packageToPath(daoPackage);
        System.out.println("Ext代码生成开始！");
        int num = 0;
        //如果设置了要生成的实体，则只对这些实体生成Ext文件
        if (includeEntitys.size() <= 0) {
            includeEntitys = currentEntity;
        }
        for (String entityName : includeEntitys) {
            //排除例外的
            if (excludeEntitys.contains(entityName)) {
                continue;
            }
            //排除名字写错的
            if (!currentEntity.contains(entityName)) {
                continue;
            }
            num += 1;
            System.out.println(entityName + "开始!");
            String javaContent = genExtDaoJavaContent(entityName);
            //生成ExtDao.java文件
            makeJavaFile(javaContent, basePath + packageToPath + extName + File.separator + entityName + extDaoSuffix + ".java");
            //生成ExtDao.xml文件
            makeXmlFile(entityName);
            System.out.println(entityName + "结束!");
        }
        System.out.println("Ext代码生成结束！共生成" + num + "张表");
    }


    private static String packageToPath(String packagePath) {
        String replace = StringUtils.replace(packagePath, ".", File.separator);
        return "java" + File.separator + replace + File.separator;
    }

    private static void makeJavaFile(String content, String filePath) throws IOException {
        FileWriter fileWriter = new FileWriter(filePath);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write(content);
        bufferedWriter.close();
    }

    /**
     * 生成ExtDao的java类内容
     *
     * @param entityName
     * @return
     */
    private static String genExtDaoJavaContent(String entityName) {
        String packagePath = "package " + daoPackage + "." + extName + ";\n\n";
        String importPath = "import " + daoPackage + "." + entityName + daoSuffix + ";\n\n";
        String content = "public interface " + entityName + extDaoSuffix + " extends " + entityName + daoSuffix + "{\n\n}";
        return packagePath + importPath + content;
    }

    private static String makeXmlFile(String entityName) throws IOException {
        String xmlFilename = entityName + extDaoSuffix + ".xml";
        String xmlFile = xmlPath + extName + File.separator + xmlFilename;
        String newNameSpace = daoPackage + "." + extName + "." + entityName + extDaoSuffix;
        Document document = DocumentHelper.createDocument();
        //先将dtd代码段按注释写入到xml文件，然后再去掉注释符号
        document.addComment("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\" >");
        Element mapper = document.addElement("mapper");
        mapper.addAttribute("namespace", newNameSpace);
        Element resultMap = mapper.addElement("resultMap");
        resultMap.addAttribute("id", "Ext" + entityName + "Map");
        resultMap.addAttribute("type", entityPackage + "." + entityName);
        resultMap.addAttribute("extends", daoPackage + "." + entityName + daoSuffix + "." + "BaseResultMap");
        OutputStream outputStream = new FileOutputStream(xmlFile);
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter xmlWriter = new XMLWriter(outputStream, format);
        xmlWriter.write(document);
        xmlWriter.close();
        outputStream.close();
        alterXmlComment(xmlFile);
        return null;

    }

    /**
     * 删除掉xml文件dtd的注释符号
     *
     * @param xmlFile
     */
    public static void alterXmlComment(String xmlFile) {
        try {
            String oldString = "mybatis-3-mapper.dtd";
            BufferedReader br_File = new BufferedReader(new InputStreamReader(new FileInputStream(xmlFile)));
            CharArrayWriter caw = new CharArrayWriter();
            String string;
            int sum = 0;
            while ((string = br_File.readLine()) != null) {
                //判断是否包含目标字符，包含则替换
                if (StringUtils.isBlank(string)) {
                    //去掉空行
                    continue;
                } else if (string.contains(oldString)) {
                    string = StringUtils.substringBetween(string, "<!--", "-->");
                }
                //写入内容并添加换行
                caw.write(string);
                caw.write("\r\n");
            }
            br_File.close();
            BufferedWriter bw_File = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(xmlFile)));
            caw.writeTo(bw_File);
            caw.close();
            bw_File.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
