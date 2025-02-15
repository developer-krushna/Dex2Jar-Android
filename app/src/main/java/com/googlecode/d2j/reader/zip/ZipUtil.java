/*
 * Copyright (c) 2009-2012 Panxiaobo
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.d2j.reader.zip;

import com.googlecode.d2j.util.zip.AccessBufByteArrayOutputStream;
import com.googlecode.d2j.util.zip.ZipEntry;
import com.googlecode.d2j.util.zip.ZipFile;

import java.io.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author bob
 * 
 */

public class ZipUtil {

    /**
     * Reads the entire InputStream into a byte array.
     *
     * @param is The InputStream to read from.
     * @return The byte array containing the data from the InputStream.
     * @throws IOException If an I/O error occurs.
     */
    public static byte[] toByteArray(InputStream is) throws IOException {
        AccessBufByteArrayOutputStream out = new AccessBufByteArrayOutputStream();
        int bufferSize = getOptimalBufferSize();
		byte[] buff = new byte[bufferSize];
        for (int c = is.read(buff); c > 0; c = is.read(buff)) {
            out.write(buff, 0, c);
        }
        return out.getBuf();
    }
    
    /**
	* Determines the optimal buffer size based on available memory.
	* @return The buffer size to use (larger if enough memory is available, otherwise smaller).
	*/
	public static int getOptimalBufferSize() {
		// Define default and large buffer sizes
		final int DEFAULT_BUFFER_SIZE = 2048; // 2 KB
		final int LARGE_BUFFER_SIZE = 8192;    // 8 KB
		
		// Check available memory
		Runtime runtime = Runtime.getRuntime();
		long freeMemory = runtime.freeMemory(); // Free memory in the JVM
		long requiredMemory = LARGE_BUFFER_SIZE * 10; // Reserve some memory for other operations
		
		// Use a larger buffer if there is enough free memory
		if (freeMemory > requiredMemory) {
			return LARGE_BUFFER_SIZE;
		} else {
			return DEFAULT_BUFFER_SIZE;
		}
	}

    /**
     * Reads the dex file from a File. If the file is a ZIP file, it returns the content of `classes.dex` in the ZIP.
     *
     * @param file The file to read from.
     * @return The content of the dex file or `classes.dex` from the ZIP.
     * @throws IOException If an I/O error occurs or the file is not a valid dex/ZIP.
     */
    public static byte[] readDex(File file) throws IOException {
        return readDex(new FileInputStream(file));
    }

    /**
     * Reads the dex file from an InputStream. If the stream is a ZIP stream, it returns the content of `classes.dex`.
     *
     * @param in The InputStream to read from.
     * @return The content of the dex file or `classes.dex` from the ZIP.
     * @throws IOException If an I/O error occurs or the stream is not a valid dex/ZIP.
     */
    public static byte[] readDex(InputStream in) throws IOException {
        return readDex(toByteArray(in));
    }

    /**
     * Reads the dex file from a byte array. If the byte array is a ZIP stream, it returns the content of `classes.dex`.
     *
     * @param data The byte array to read from.
     * @return The content of the dex file or `classes.dex` from the ZIP.
     * @throws IOException If the byte array is not a valid dex/ZIP.
     */
    public static byte[] readDex(byte[] data) throws IOException {
        if (data.length < 3) {
            throw new IOException("File too small to be a dex/zip");
        }
        if ("dex".equals(new String(data, 0, 3, StandardCharsets.ISO_8859_1))) {// dex
            return data;
        } else if ("PK".equals(new String(data, 0, 2, StandardCharsets.ISO_8859_1))) {// ZIP
            try (ZipFile zipFile = new ZipFile(data)) {
                ZipEntry classes = zipFile.findFirstEntry("classes.dex");
                if (classes != null) {
                    return toByteArray(zipFile.getInputStream(classes));
                } else {
                    throw new IOException("Can not find classes.dex in zip file");
                }
            }
        }
        throw new IOException("the src file not a .dex or zip file");
    }
}
