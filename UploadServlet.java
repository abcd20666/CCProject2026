package com.avcoe.backup;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.ObjectMetadata;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.util.Collection;

@WebServlet("/upload")
@MultipartConfig
public class UploadServlet extends HttpServlet {

    private static final String ACCESS_KEY = "AKIA2OHXAR23TG223QPT";
    private static final String SECRET_KEY = "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";
    private static final String REGION = "ap-south-1";
    private static final String MAIN_BUCKET = "main-backup-system123";
    private static final String BACKUP_BUCKET = "backup-storage-system123";

    private static final String LOCAL_PATH = "C:\\temp\\";

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("upload.jsp").forward(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Get logged-in username from session
        HttpSession session = request.getSession(false);
        String username = (session != null) ? (String) session.getAttribute("username") : "guest";

        // Create local folder if not exists
        File dir = new File(LOCAL_PATH);
        if (!dir.exists()) dir.mkdirs();

        BasicAWSCredentials creds = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);

        AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(creds))
                .withRegion(REGION)
                .build();

        try {
            Collection<Part> parts = request.getParts();

            for (Part part : parts) {
                if (part.getName().equals("file") && part.getSize() > 0) {

                    String fileName = part.getSubmittedFileName();

                    // =========================
                    // 1. SAVE TO LOCAL (C DRIVE)
                    // =========================
                    String filePath = LOCAL_PATH + fileName;
                    part.write(filePath);

                    // =========================
                    // 2. UPLOAD TO S3 (User-specific folder)
                    // =========================
                    File file = new File(filePath);
                    String userFileKey = username + "/" + fileName; // <--- user-specific folder
                    s3.putObject(MAIN_BUCKET, userFileKey, file);

                    // =========================
                    // 3. COPY TO BACKUP BUCKET (User-specific folder)
                    // =========================
                    s3.copyObject(MAIN_BUCKET, userFileKey, BACKUP_BUCKET, userFileKey);
                }
            }

            response.sendRedirect("success.jsp");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("error.jsp");
        }
    }
} 