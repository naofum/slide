package trikita.slide.middleware;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.text.TextPaint;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import trikita.jedux.Action;
import trikita.jedux.Store;
import trikita.slide.ActionType;
import trikita.slide.App;
import trikita.slide.Slide;
import trikita.slide.State;
import trikita.slide.ui.Style;

public class Exporter implements Store.Middleware<Action<ActionType, ?>, State> {

    private final Context mContext;

    public Exporter(Context c) {
        mContext = c;
    }

    @Override
    public void dispatch(Store<Action<ActionType, ?>, State> store, Action<ActionType, ?> action, Store.NextDispatcher<Action<ActionType, ?>> next) {
        if (action.type == ActionType.SHARE) {
            File f = savePdf(store, "slide.pdf");
            if (f != null) {
                if (!sharePdf(f)) {
                    Toast.makeText(mContext, "Saved to " + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }
            }
            return;
        }
        next.dispatch(action);
    }

    private boolean sharePdf(File f) {
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf");
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(f), mime);
        try {
            mContext.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private File savePdf(Store<Action<ActionType, ?>, State> store, String filename) {
        Document document = new Document(new Rectangle(640, 640 * 9 / 16));
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory("Documents"), filename);
            if (!file.getParentFile().exists()) {
                file = new File(Environment.getExternalStorageDirectory(), filename);
            }
            FileOutputStream fos = new FileOutputStream(file);

            PdfWriter pdfwriter = PdfWriter.getInstance(document, fos);
            document.open();
            Rectangle rect = document.getPageSize();
            PdfContentByte canvas = pdfwriter.getDirectContentUnder();
            canvas.setColorFill(new BaseColor(Style.COLOR_SCHEMES[store.getState().colorScheme()][1]));
            canvas.rectangle(rect.getLeft(), rect.getBottom(), rect.getWidth(), rect.getHeight());
            canvas.fill();

            String fullText = store.getState().text();
            for (int i = 0; i < Slide.paginate(fullText).length; i++) {
                String text = Slide.pageText(fullText, i);
                text = text.trim().replaceAll("\n\\.", "\n");
                System.out.println("render: " + text);

                if (i > 0) {
                    document.newPage();
                }
                String lines[] = text.split("\n");
                int cols = 0;
                for (int j = 0; j < lines.length; j++) {
                    if (cols < lines[j].length()) {
                        cols = lines[j].length();
                    }
                }
                int size = (640 * 9 / 16 - 72) / lines.length;
                if (size > ((640 - 72) / cols)) {
                    size = ((640 - 72) / cols);
                }
                for (int j = 0; j < lines.length; j++) {
                    BaseFont baseFont;
                    if (Locale.getDefault().getLanguage().equals(Locale.JAPANESE.getLanguage())) {
                        baseFont = BaseFont.createFont("HeiseiMin-W3", "UniJIS-UCS2-HW-H", BaseFont.NOT_EMBEDDED);
                    } else if (Locale.getDefault().getLanguage().equals(Locale.SIMPLIFIED_CHINESE.getLanguage())) {
                        baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
                        if (j == 0) size = (int)(size / 1.1);
                    } else if (Locale.getDefault().getLanguage().equals(Locale.TRADITIONAL_CHINESE.getLanguage())) {
                        baseFont = BaseFont.createFont("MSung-Light", "UniCNS-UCS2-H", BaseFont.NOT_EMBEDDED);
                        if (j == 0) size = (int)(size / 1.1);
                    } else {
                        baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                        if (j == 0) size = (int)(size * 1.2);
                    }
                    Font font = new Font(baseFont, size, Font.NORMAL, new BaseColor(Style.COLOR_SCHEMES[store.getState().colorScheme()][0]));
                    if (lines[j].isEmpty()) {
                        document.add(Chunk.NEWLINE);
                    } else {
                        Paragraph paragraph = new Paragraph(lines[j], font);
                        paragraph.setSpacingBefore(0.0f);
                        paragraph.setSpacingAfter(0.0f);
                        document.add(paragraph);
                    }
                }
                canvas.setColorFill(new BaseColor(Style.COLOR_SCHEMES[store.getState().colorScheme()][1]));
                canvas.rectangle(rect.getLeft(), rect.getBottom(), rect.getWidth(), rect.getHeight());
                canvas.fill();
            }

            document.close();
            return file;
        } catch (DocumentException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            document.close();
        }
    }
}
