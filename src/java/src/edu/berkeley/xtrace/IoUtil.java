/*
 * Copyright (c) 2005,2006,2007 The Regents of the University of California.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the University of California, nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY OF CALIFORNIA ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.berkeley.xtrace;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

//import org.apache.log4j.Logger;

/**
 * X-trace input/output utilities.
 *
 * @author George Porter
 */
public final class IoUtil
{
   //private static final Logger LOG = Logger.getLogger(IoUtil.class);
   private static final char[] hex = "0123456789ABCDEF".toCharArray();
   
   /**
    * This function returns the subset of the given serialized metadata
    * string representing the operation id.  It is designed to be
    * very fast, and to avoid manifesting the metadata as a
    * complete object.
    * 
    * @param md the serialized metadata to extract the opid from
    * @return a string representing the operation id
    */
   public static String fastOpIdExtraction(String md) {
	   if (md.length() < 18) return "00000000";
	   
	   int opidlength = 4;
	   int taskidlength = 4;
	   
	   byte flag = (byte) Integer.parseInt(md.substring(0, 2), 16);
	   if ((flag & 0x08) != 0) {
		   opidlength = 8;
	   }
	   
	   switch (flag & 0x03) {
	   case 0x00: taskidlength = 4; break;
	   case 0x01: taskidlength = 8; break;
	   case 0x02: taskidlength = 12; break;
	   case 0x03: taskidlength = 20; break;
	   default: // can't happen
	   }
	   
	   return md.substring(2 + 2*taskidlength, 2 + 2*taskidlength + 2*opidlength);
   }

   /**
    * Converts a byte array into a String, according to the rules in
    * the X-Trace specification
    *
    * @param bytes An array of bytes
    * @return a String representing this byte array
    */
   public static String bytesToString(final byte[] bytes)
   throws IOException {
	   return bytesToString(bytes, 0, bytes.length);
   }
   
   /**
    * Converts a byte array into a String, according to the rules in
    * the X-Trace specification
    *
    * @param bytes An array of bytes
    * @param offset The offset to start from
    * @param length the number of bytes to read
    * @return a String representing this byte array
    */
   public static String bytesToString(final byte[] bytes,
		                              final int offset,
		                              final int length)
   throws IOException {
	   final StringBuilder buf = new StringBuilder();

	   for (int i = 0; i < length; i++) {
		   byte b = bytes[offset + i];
		   buf.append(""+hex[b>>4&0x0f] + hex[b&0x0f]);
	   }

	   return buf.toString();
   }

   /**
    * Converts a String into a byte array, according to the rules in the
    * X-Trace specification
    *
    * @param str A hex string
    * @return an array of bytes representing the string
    */
   public static byte[] stringToBytes(final String str)
     throws IOException {

      if (str.length() % 2 != 0)
		throw new IOException("Length of " + str + " must be even");

      final byte[] bytes = new byte[str.length() / 2];

      for (int i = 0; i < bytes.length; i++) {
         bytes[i] = (byte) Integer.parseInt(str.substring(2*i, 2*i+2), 16);
      }

      return bytes;
   }

   /**
    *  Ensure that the given String is a valid string (only 0-9 and A-F,
    *  uppercase, and an even length)
    *
    * @param hex the String to validate
    */
   public static boolean validateHexString(final String hex)
   {
      // must be of even length
      if (hex.length() % 2 != 0)
         return false;
      
      boolean isValid = false;
      try {
         isValid = hex.matches("[0-9A-F]*");
      } catch (final PatternSyntaxException pse) {
    	  //LOG.warn("Internal I/O error", pse);
    	  return false;
      }

      return isValid;
   }

   public static String intToString(int i) {
      byte[] b = new byte[4];
      b[0] = (byte) (i >>> 24);
      b[1] = (byte) (i >>> 16);
      b[2] = (byte) (i >>> 8);
      b[3] = (byte) (i);
      String s = null;
      try {
         s = bytesToString(b);
      } catch (IOException e) { } // exception can't happen
      return s;
   }
   
   public static String longToString(long l) {
	      byte[] b = new byte[8];
	      b[0] = (byte) (l >>> 56);
	      b[1] = (byte) (l >>> 48);
	      b[2] = (byte) (l >>> 40);
	      b[3] = (byte) (l >>> 32);
	      b[4] = (byte) (l >>> 24);
	      b[5] = (byte) (l >>> 16);
	      b[6] = (byte) (l >>> 8);
	      b[7] = (byte) (l);
	      String s = null;
	      try {
	         s = bytesToString(b);
	      } catch (IOException e) { } // exception can't happen
	      return s;
	   }
   
   public static long hexStringToLong(String s) throws NumberFormatException {
	   if (s == null || s.length() < 2) {
		   throw new NumberFormatException(s);
	   }
	   char firstChar = s.charAt(0);
	   if (firstChar >= '0' && firstChar <= '7') {
		   return Long.parseLong(s, 16);
	   } else {
		   // Slower method when s is negative
		   java.math.BigInteger big = new java.math.BigInteger(s, 16);
		   return big.longValue();
	   }
   }
   
	 private static final Pattern NEWLINES = Pattern.compile("[\r\n]+");
	 
   public static String escapeNewlines(String str) {
  	 return NEWLINES.matcher(str).replaceAll("\\\\n");
   }
}
