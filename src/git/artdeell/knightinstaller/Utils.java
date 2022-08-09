/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package git.artdeell.knightinstaller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author maks
 */
public class Utils {
    public static byte[] getFromWeb(String url, Progress pr) throws IOException{
        boolean sendProgress = true;
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.connect();
        if(conn.getContentLength() != -1) {
            pr.postMaxPart(conn.getContentLength());
        }else{
            pr.setPartIndeterminate(true);
            sendProgress = false;
        }
        InputStream is = conn.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];int i;int cur=0;
        while((i = is.read(buf)) != -1) {
            cur += i;
            if(sendProgress) pr.postStepProgress(cur);
            baos.write(buf, 0, i);
        }
        return baos.toByteArray();
    }
    public static int indexOf(byte[] haystack, byte[] needle)
    {
        // needle is null or empty
        if (needle == null || needle.length == 0)
            return 0;

        // haystack is null, or haystack's length is less than that of needle
        if (haystack == null || needle.length > haystack.length)
            return -1;

        // pre construct failure array for needle pattern
        int[] failure = new int[needle.length];
        int n = needle.length;
        failure[0] = -1;
        for (int j = 1; j < n; j++)
        {
            int i = failure[j - 1];
            while ((needle[j] != needle[i + 1]) && i >= 0)
                i = failure[i];
            if (needle[j] == needle[i + 1])
                failure[j] = i + 1;
            else
                failure[j] = -1;
        }

        // find match
        int i = 0, j = 0;
        int haystackLen = haystack.length;
        int needleLen = needle.length;
        while (i < haystackLen && j < needleLen)
        {
            if (haystack[i] == needle[j])
            {
                i++;
                j++;
            }
            else if (j == 0)
                i++;
            else
                j = failure[j - 1] + 1;
        }
        return ((j == needleLen) ? (i - needleLen) : -1);
    }
    public static int indexOf(byte[] array, int off, byte sym) {
        for(int i = off; i < array.length; i++) {
            if(array[i] == sym) return i;
        }
        return -1;
    }
    public static int findXth(byte[] array, byte sym, int cnt) {
        int xthCount = 0;
        for(int i = 0; i < array.length; i++) {
            if(array[i] == sym) {
                xthCount++;
                if(xthCount == cnt) return i;
            }
        }
        return -1;
    }
}
