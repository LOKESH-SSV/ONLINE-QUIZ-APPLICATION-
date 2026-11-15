package com.collegequiz;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public final class ReportExporter {
    private ReportExporter() {}

    public static void exportCsv(Frame owner, String sql, String suggestedName) {
        FileDialog dialog = new FileDialog(owner, "Save report as CSV", FileDialog.SAVE);
        dialog.setFile(suggestedName + ".csv");
        dialog.setVisible(true);
        if (dialog.getFile() == null) return;
        File file = new File(dialog.getDirectory(), dialog.getFile());
        try (Connection c = DB.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(
                     new FileOutputStream(file), StandardCharsets.UTF_8))) {
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                if (i > 1) out.print(",");
                out.print(csv(md.getColumnLabel(i)));
            }
            out.println();
            while (rs.next()) {
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    if (i > 1) out.print(",");
                    out.print(csv(rs.getString(i)));
                }
                out.println();
            }
            Msg.show(owner, "CSV report saved to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            Msg.show(owner, "Could not export CSV: " + ex.getMessage());
        }
    }

    public static void exportPdf(Frame owner, String title, String body, String suggestedName) {
        FileDialog dialog = new FileDialog(owner, "Save report as PDF", FileDialog.SAVE);
        dialog.setFile(suggestedName + ".pdf");
        dialog.setVisible(true);
        if (dialog.getFile() == null) return;
        File file = new File(dialog.getDirectory(), dialog.getFile());
        try {
            writeSimplePdf(file, title, body);
            Msg.show(owner, "PDF report saved to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            Msg.show(owner, "Could not export PDF: " + ex.getMessage());
        }
    }

    private static String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    // Small dependency-free, single-page PDF writer suitable for the project's text reports.
    private static void writeSimplePdf(File file, String title, String body) throws IOException {
        StringBuilder stream = new StringBuilder("BT /F1 10 Tf 45 800 Td ");
        appendPdfText(stream, title);
        stream.append(" 0 -22 Td ");
        String[] lines = body.replace("\r", "").split("\n");
        int count = 0;
        for (String line : lines) {
            if (count++ >= 48) break;
            appendPdfText(stream, line.length() > 105 ? line.substring(0, 105) : line);
            stream.append(" 0 -15 Td ");
        }
        stream.append("ET");
        byte[] content = stream.toString().getBytes(StandardCharsets.US_ASCII);
        String[] objects = new String[5];
        objects[0] = "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n";
        objects[1] = "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n";
        objects[2] = "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n";
        objects[3] = "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n";
        objects[4] = "5 0 obj << /Length " + content.length + " >> stream\n" +
                stream + "\nendstream endobj\n";
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
            long[] offsets = new long[6];
            for (int i = 0; i < objects.length; i++) {
                offsets[i + 1] = file.length();
                out.write(objects[i].getBytes(StandardCharsets.US_ASCII));
            }
            long xref = file.length();
            out.write(("xref\n0 6\n0000000000 65535 f \n").getBytes(StandardCharsets.US_ASCII));
            for (int i = 1; i <= 5; i++)
                out.write(String.format("%010d 00000 n \n", offsets[i]).getBytes(StandardCharsets.US_ASCII));
            out.write(("trailer << /Size 6 /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF")
                    .getBytes(StandardCharsets.US_ASCII));
        }
    }

    private static void appendPdfText(StringBuilder out, String text) {
        String safe = text == null ? "" : text.replaceAll("[^\\x20-\\x7E]", "?")
                .replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        out.append("(").append(safe).append(") Tj");
    }
}
