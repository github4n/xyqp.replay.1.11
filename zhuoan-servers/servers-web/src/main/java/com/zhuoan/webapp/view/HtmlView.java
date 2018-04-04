package com.zhuoan.webapp.view;

import org.apache.commons.io.IOUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;

public class HtmlView extends AbstractUrlBasedView {

    public HtmlView() {
        setContentType("text/html;charset=utf-8");
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model,
                                           HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String rootPath = getServletContext().getRealPath("/") + getUrl();
        File file = new File(rootPath);
        response.setContentType(getContentType());
        InputStream inputStream = new FileInputStream(file);
        OutputStream outputStream = response.getOutputStream();
        IOUtils.copy(inputStream, outputStream);
        outputStream.flush();
        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(outputStream);
    }

    @Override
    public boolean checkResource(Locale locale) {
        String rootPath = getServletContext().getRealPath("/") + getUrl();
        File file = new File(rootPath);
        return file.exists();
    }
}