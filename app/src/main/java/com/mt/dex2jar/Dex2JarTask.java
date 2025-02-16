package com.mt.dex2jar;

import android.os.AsyncTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.googlecode.d2j.dex.Dex2jar;
import org.objectweb.asm.tree.analysis.AnalyzerException;

/* Author @developer-krushna
Comment and code enhancement by ChatGPT */
public class Dex2JarTask extends AsyncTask<Void, Integer, String> {
	private AlertProgress progressDialog;
	private File inputDexFile;
	private File outputJarFile;
	private TaskListener listener; // Callback to communicate with the activity
	private File tempDir = null;
	
	public interface TaskListener {
		void onTaskCompleted(boolean success, String message);
	}
	
	public Dex2JarTask(AlertProgress progressDialog, File inputDexFile, File outputJarFile, TaskListener listener) {
		this.progressDialog = progressDialog;
		this.inputDexFile = inputDexFile;
		this.outputJarFile = outputJarFile;
		this.listener = listener;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		progressDialog.setTitle("Processing...");
		progressDialog.setMessage("Converting Dex to Jar...");
		progressDialog.setIndeterminate(false);
		progressDialog.show();
	}
	
	@Override
	protected String doInBackground(Void... voids) {
		try {
			File cacheDir = inputDexFile.getParentFile();
			tempDir = createTempDirectory(cacheDir, "dex2jar-output");
			
			// Initialize dex2jar conversion
			Dex2jar dex2jar = Dex2jar.from(inputDexFile)
			.reUseReg(false)
			.topoLogicalSort()
			.skipDebug(true)
			.optimizeSynchronized(true)
			.printIR(false)
			.noCode(false)
			.skipExceptions(false);
			
			// Set up progress listener for dex2jar
			dex2jar.setProgressListener(new Dex2jar.ProgressListener() {
				@Override
				public void onProgressUpdate(int totalClasses, int processedClasses) {
					publishProgress(1, processedClasses, totalClasses);
				}
			});
			
			// Convert .dex to .class files
			dex2jar.to(tempDir);
			
			// Reset progress for JAR creation
			int totalFiles = countFiles(tempDir);
			publishProgress(2, 0, totalFiles);
			
			// Create output .jar
			try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outputJarFile))) {
				final int[] processedFiles = {0};
				processDirectoryForJar(tempDir, tempDir, zipOut, processedFiles, totalFiles);
			}
			
			return "Conversion completed successfully!\nOutput: " + outputJarFile.getAbsolutePath();
		} catch (Exception e) {
			return "Conversion failed: " + e.getMessage(); // Return the exception message
		} finally {
			// Cleanup temp directory
			if (tempDir != null) {
				deleteDirectory(tempDir);
			}
		}
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		int stage = values[0]; // 1 = Dex2Jar, 2 = JAR creation
		int progress = values[1];
		int max = values[2];
		
		if (stage == 1) {
			progressDialog.setTitle("Converting Dex to Classes...");
		} else if (stage == 2) {
			progressDialog.setTitle("Creating JAR file...");
		}
		
		progressDialog.setProgress(progress, max);
	}
	
	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		progressDialog.dismiss();
		
		// Notify activity through listener
		if (listener != null) {
			boolean success = !result.startsWith("Conversion failed");
			listener.onTaskCompleted(success, result);
		}
	}
	
	// Helper method to create a temporary directory
	private File createTempDirectory(File parentDir, String prefix) throws IOException {
		File tempDir = File.createTempFile(prefix, "", parentDir);
		if (!tempDir.delete() || !tempDir.mkdir()) {
			throw new IOException("Failed to create temp directory");
		}
		return tempDir;
	}
	
	// Helper method to count files in a directory
	private int countFiles(File dir) {
		int count = 0;
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					count += countFiles(file);
				} else {
					count++;
				}
			}
		}
		return count;
	}
	
	// Helper method to process directory for JAR creation
	private void processDirectoryForJar(File rootDir, File currentDir, ZipOutputStream zipOut, int[] processedFiles, int totalFiles) throws IOException {
		File[] files = currentDir.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					processDirectoryForJar(rootDir, file, zipOut, processedFiles, totalFiles);
				} else {
					try (InputStream in = new FileInputStream(file)) {
						String entryName = rootDir.toURI().relativize(file.toURI()).getPath();
						zipOut.putNextEntry(new ZipEntry(entryName));
						
						// Determine buffer size based on available memory
						int bufferSize = getOptimalBufferSize();
						byte[] buffer = new byte[bufferSize];
						
						int len;
						while ((len = in.read(buffer)) > 0) {
							zipOut.write(buffer, 0, len);
						}
						zipOut.closeEntry();
						
						// Update JAR creation progress
						processedFiles[0]++;
						publishProgress(2, processedFiles[0], totalFiles);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	* Determines the optimal buffer size based on available memory.
	* @return The buffer size to use (larger if enough memory is available, otherwise smaller).
	*/
	private int getOptimalBufferSize() {
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
	
	// Helper method to delete a directory recursively
	private void deleteDirectory(File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					deleteDirectory(file);
				} else {
					file.delete();
				}
			}
		}
		dir.delete();
	}
}
