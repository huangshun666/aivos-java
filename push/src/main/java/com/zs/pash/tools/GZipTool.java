package com.zs.pash.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZipTool {
   public static final int BUFFER = 1024;
   public static final String EXT = ".gz";

   public static byte[] compress(String str, String encoding) {
      if (str != null && str.length() != 0) {
         ByteArrayOutputStream out = new ByteArrayOutputStream();

         try {
            GZIPOutputStream gzip = new GZIPOutputStream(out);
            gzip.write(str.getBytes(encoding));
            gzip.close();
         } catch (Exception var5) {
            var5.printStackTrace();
         }

         return out.toByteArray();
      } else {
         return null;
      }
   }

   public byte[] compress(byte[] data) throws Exception {
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      compress((InputStream)bais, (OutputStream)baos);
      byte[] output = baos.toByteArray();
      baos.flush();
      baos.close();
      bais.close();
      return output;
   }

   public void compress(File file) throws Exception {
      this.compress(file, true);
   }

   public void compress(File file, boolean delete) throws Exception {
      FileInputStream fis = new FileInputStream(file);
      FileOutputStream fos = new FileOutputStream(file.getPath() + ".gz");
      compress((InputStream)fis, (OutputStream)fos);
      fis.close();
      fos.flush();
      fos.close();
      if (delete) {
         file.delete();
      }

   }

   public static void compress(InputStream is, OutputStream os) throws Exception {
      GZIPOutputStream gos = new GZIPOutputStream(os);
      byte[] data = new byte[1024];

      int count;
      while((count = is.read(data, 0, 1024)) != -1) {
         gos.write(data, 0, count);
      }

      gos.finish();
      gos.flush();
      gos.close();
   }

   public void compress(String path) throws Exception {
      this.compress(path, true);
   }

   public void compress(String path, boolean delete) throws Exception {
      File file = new File(path);
      this.compress(file, delete);
   }

   public static byte[] decompress(byte[] data) throws Exception {
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      decompress(bais, baos);
      data = baos.toByteArray();
      baos.flush();
      baos.close();
      bais.close();
      return data;
   }

   public void decompress(File file) throws Exception {
      this.decompress(file, true);
   }

   public void decompress(File file, boolean delete) throws Exception {
      FileInputStream fis = new FileInputStream(file);
      FileOutputStream fos = new FileOutputStream(file.getPath().replace(".gz", ""));
      decompress(fis, fos);
      fis.close();
      fos.flush();
      fos.close();
      if (delete) {
         file.delete();
      }

   }

   public static void decompress(InputStream is, OutputStream os) throws Exception {
      GZIPInputStream gis = new GZIPInputStream(is);
      byte[] data = new byte[1024];

      int count;
      while((count = gis.read(data, 0, 1024)) != -1) {
         os.write(data, 0, count);
      }

      gis.close();
   }

   public void decompress(String path) throws Exception {
      this.decompress(path, true);
   }

   public void decompress(String path, boolean delete) throws Exception {
      File file = new File(path);
      this.decompress(file, delete);
   }
}
