package org.dean.idea.plugin.mybatis;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnActionButton;
import org.apache.commons.lang.StringUtils;


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creted by dean <deanzhg@gmail.com> on 2018/3/6.
 */
public class FormatMybatisInsert extends AnAction {

    private Project project;

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        project = anActionEvent.getData(PlatformDataKeys.PROJECT);
        Editor editor = anActionEvent.getData(PlatformDataKeys.EDITOR);
        Document document = editor.getDocument();

        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();

        String afterFormat = "";
        if (StringUtils.isNotEmpty(selectedText)) {
            String trim = selectedText.trim();
            if (trim.split(" ")[0].toUpperCase().equals("INSERT")){
                if(trim.indexOf("into") <= 0 ||
                trim.indexOf("INTO") <= 0 ||
                trim.indexOf("values") <= 0 ||
                trim.indexOf("VALUES") <= 0) {
                    Messages.showMessageDialog(project, "SQL语句不合法，缺少 into 或 values 关键字", "Information", Messages.getInformationIcon());
                    return ;
                }
                afterFormat = afterFormat(trim);
            } else {
                Messages.showMessageDialog(project, "SQL语句不合法，缺少 insert 关键字", "Information", Messages.getInformationIcon());
                return ;
            }
        } else {
            return ;
        }

        if (StringUtils.isNotEmpty(afterFormat)) {
            final String replaceText = afterFormat;
            final int start = selectionModel.getSelectionStart();
            final int end = selectionModel.getSelectionEnd();
            //New instance of Runnable to make a replacement

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    document.replaceString(start, end, replaceText);
                }
            };
            //Making the replacement
            WriteCommandAction.runWriteCommandAction(project, runnable);
            selectionModel.removeSelection();
        }

    }

    @Override
    public void update(AnActionEvent anActionEvent) {
        project = anActionEvent.getData(PlatformDataKeys.PROJECT);

        Editor editor = anActionEvent.getData(PlatformDataKeys.EDITOR);//怎么获取当前编辑文件的文件名

        VirtualFile vFile = anActionEvent.getData(PlatformDataKeys.VIRTUAL_FILE);
        String fileName = vFile != null ? vFile.getName() : null;
        //System.out.println(fileName);
        //System.out.println(StringUtils.isNotEmpty(fileName) && fileName.endsWith(".xml"));

        System.out.println();

        if (editor != null && editor.getDocument() != null && editor.getDocument().isWritable() && StringUtils.isNotEmpty(fileName) && fileName.endsWith(".xml")) {
            //Messages.showMessageDialog(project, editor.getDocument().getText(), "Information", Messages.getInformationIcon());
            //anActionEvent.getPresentation().setEnabled(true);//可用性
            if (editor.getSelectionModel().hasSelection()){
                anActionEvent.getPresentation().setVisible(true);//可见性
            } else {
                anActionEvent.getPresentation().setVisible(false);
            }
        } else {
            //anActionEvent.getPresentation().setEnabled(false);
            anActionEvent.getPresentation().setVisible(false);
        }
    }

    public static void main(String[] args) {
        String selectedText = "  insert   into   air_flight_info    (ID,ORDER_ID)\n" +
                "        values  (#id#,#orderId#)";
        FormatMybatisInsert demo = new FormatMybatisInsert();
        String afterFormat = demo.afterFormat(selectedText);
        //System.out.println("afterFormat = " + afterFormat);
    }


    private String afterFormat(String beforeFormat){
        if (StringUtils.isNotEmpty(beforeFormat)){
            String sqlTrimed = beforeFormat.trim();
            String afterReplace = sqlTrimed.replaceAll("\\s+", " ").replaceAll(", ", ",");//去掉多余空格
            //System.out.println(afterReplace);

            String insertHead = afterReplace.substring(0, 11).toUpperCase();//insert语法头
            //System.out.println(insertHead);

            int firstIndexBracket = afterReplace.indexOf('(');
            String tableName = afterReplace.substring(12, firstIndexBracket - 1).trim().toUpperCase();//表名
            //System.out.println(tableName);

            int firstIndexBracketMatch = afterReplace.indexOf(')');

            String columnString = afterReplace.substring(firstIndexBracket + 1, firstIndexBracketMatch);//字段串
            //System.out.println(columnString);

            String valuesString = afterReplace.substring(firstIndexBracketMatch + 10, afterReplace.length() -1 );//字段值串
            //System.out.println(valuesString);

            String[] columnArr = columnString.trim().split(",");//字段集合
            List<String> columnList = Arrays.asList(columnArr);
            //columnList.forEach(item -> System.out.println(item));

            String[] valueArr = valuesString.trim().split(",");//字段值集合
            List<String> valueList = Arrays.asList(valueArr);
            //valueList.forEach(item -> System.out.println(item));

            if (valueList.size() != columnList.size()) {
                Messages.showMessageDialog(project, "sql语句非法，字段和字段值数量不匹配", "Information", Messages.getInformationIcon());
                return null;
            }

            //int max = maxColumnLength > maxValueLength ? maxColumnLength : maxValueLength;
            //int tabNum = max % 4 == 0 ? max / 4 : max / 4 + 1 + 1;
            //System.out.println("tabNum = " + tabNum);

            StringBuffer sqlSBuffer = new StringBuffer();

            sqlSBuffer.append(insertHead).append(" ").append(tableName).append(" (\n\t");

            for (int i = 0; i < columnList.size(); i++) {
                String column = columnList.get(i);
                column = " " + column;
                int lengthColumn = column.length();
                int lengthValue = valueList.get(i).length();
                sqlSBuffer.append(column + "," + StringUtils.repeat(" ", lengthValue - lengthColumn) + "\t");
            }

            sqlSBuffer.delete(sqlSBuffer.lastIndexOf(","), sqlSBuffer.length());//,最后一个逗号及后面的空格要删除

            sqlSBuffer.append(")");

            sqlSBuffer.append("\nVALUES (\n\t");

            for (int i = 0; i < valueList.size(); i++) {
                String column = columnList.get(i);
                int lengthColumn = column.length() + 1;//前面的空格
                String value = valueList.get(i);
                if (! value.startsWith("#")){
                    value = " " + value;
                }
                int lengthValue = value.length();
                sqlSBuffer.append(value + "," + StringUtils.repeat(" ", lengthColumn - lengthValue) + "\t");
            }

            sqlSBuffer.delete(sqlSBuffer.lastIndexOf(","), sqlSBuffer.length());//,最后一个逗号及后面的空格要删除

            sqlSBuffer.append(")");

            String sql = sqlSBuffer.toString();
            sql = sql.replaceAll("\t", "  ");
            return sql;
        }

        return null;
    }

}