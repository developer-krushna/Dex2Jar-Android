package com.googlecode.d2j.dex;

import com.googlecode.d2j.converter.IR2JConverter;
import com.googlecode.d2j.node.DexFileNode;
import com.googlecode.d2j.node.DexMethodNode;
import com.googlecode.d2j.reader.BaseDexFileReader;
import com.googlecode.d2j.reader.DexFileReader;
import com.googlecode.d2j.reader.zip.ZipUtil;
import com.googlecode.dex2jar.ir.IrMethod;
import com.googlecode.dex2jar.ir.stmt.LabelStmt;
import com.googlecode.dex2jar.ir.stmt.Stmt;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Dex2jar {
	public static Dex2jar from(byte[] in) throws IOException {
		return from(new DexFileReader(ZipUtil.readDex(in)));
	}
	
	public static Dex2jar from(ByteBuffer in) throws IOException {
		return from(new DexFileReader(in));
	}
	
	public static Dex2jar from(BaseDexFileReader reader) {
		return new Dex2jar(reader);
	}
	
	public static Dex2jar from(File in) throws IOException {
		return from(ZipUtil.readDex(in));
	}
	
	public static Dex2jar from(InputStream in) throws IOException {
		return from(new DexFileReader(in));
	}
	
	public static Dex2jar from(String in) throws IOException {
		return from(new File(in));
	}
	
	private DexExceptionHandler exceptionHandler;
	
	final private BaseDexFileReader reader;
	private int readerConfig;
	private int v3Config;
	
	private ProgressListener progressListener;
	private int totalClasses = 0;
	private int processedClasses = 0;
	
	public void setProgressListener(ProgressListener progressListener) {
		this.progressListener = progressListener;
	}
	
	private Dex2jar(BaseDexFileReader reader) {
		super();
		this.reader = reader;
		readerConfig |= DexFileReader.SKIP_DEBUG;
	}
	
	private void doTranslate(final File dist) throws IOException {
		DexFileNode fileNode = new DexFileNode();
		try {
			reader.accept(fileNode, readerConfig | DexFileReader.IGNORE_READ_EXCEPTION);
		} catch (Exception ex) {
			exceptionHandler.handleFileException(ex);
		}
		
		// Count total classes
		totalClasses = fileNode.clzs.size();
		
		ClassVisitorFactory cvf = new ClassVisitorFactory() {
			@Override
			public ClassVisitor create(final String name) {
				final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				final LambadaNameSafeClassAdapter rca = new LambadaNameSafeClassAdapter(cw);
				return new ClassVisitor(Opcodes.ASM5, rca) {
					@Override
					public void visitEnd() {
						super.visitEnd();
						String className = rca.getClassName();
						byte[] data;
						try {
							data = cw.toByteArray();
						} catch (Exception ex) {
							System.err.println(String.format("ASM fail to generate .class file: %s", className));
							exceptionHandler.handleFileException(ex);
							return;
						}
						
						// Write the class file to disk
						try{
							writeClassFile(dist, className, data);
						}catch(Exception e) {
							exceptionHandler.handleFileException(e);
							return;
						}
						
						// Increment processed classes and notify listener
						processedClasses++;
						if (progressListener != null) {
							progressListener.onProgressUpdate(totalClasses, processedClasses);
						}
					}
				};
			}
		};
		
		new ExDex2Asm(exceptionHandler) {
			@Override
			public void convertCode(DexMethodNode methodNode, MethodVisitor mv) {
				if ((readerConfig & DexFileReader.SKIP_CODE) != 0 && methodNode.method.getName().equals("<clinit>")) {
					// Skip clinit
					return;
				}
				super.convertCode(methodNode, mv);
			}
			
			@Override
			public void optimize(IrMethod irMethod) {
				T_cleanLabel.transform(irMethod);
				if (0 != (v3Config & V3.TOPOLOGICAL_SORT)) {
					// T_topologicalSort.transform(irMethod);
				}
				T_deadCode.transform(irMethod);
				T_removeLocal.transform(irMethod);
				T_removeConst.transform(irMethod);
				T_zero.transform(irMethod);
				if (T_npe.transformReportChanged(irMethod)) {
					T_deadCode.transform(irMethod);
					T_removeLocal.transform(irMethod);
					T_removeConst.transform(irMethod);
				}
				T_new.transform(irMethod);
				T_fillArray.transform(irMethod);
				T_agg.transform(irMethod);
				T_multiArray.transform(irMethod);
				T_voidInvoke.transform(irMethod);
				if (0 != (v3Config & V3.PRINT_IR)) {
					int i = 0;
					for (Stmt p : irMethod.stmts) {
						if (p.st == Stmt.ST.LABEL) {
							LabelStmt labelStmt = (LabelStmt) p;
							labelStmt.displayName = "L" + i++;
						}
					}
					System.out.println(irMethod);
				}
				T_type.transform(irMethod);
				T_unssa.transform(irMethod);
				T_ir2jRegAssign.transform(irMethod);
				T_trimEx.transform(irMethod);
			}
			
			@Override
			public void ir2j(IrMethod irMethod, MethodVisitor mv) {
				new IR2JConverter(0 != (V3.OPTIMIZE_SYNCHRONIZED & v3Config)).convert(irMethod, mv);
			}
		}.convertDex(fileNode, cvf);
	}
	
	/**
	* Helper method to write class files to disk efficiently.
	*/
	private void writeClassFile(File dist, String className, byte[] data) throws Exception {
		File dist1 = new File(dist, className + ".class");
		File parent = dist1.getParentFile();
		
		// Create parent directories if they don't exist
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		
		// Write the file using BufferedOutputStream for better performance
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		try {
			fos = new FileOutputStream(dist1);
			bos = new BufferedOutputStream(fos);
			bos.write(data);
		} finally {
			if (bos != null) bos.close();
			if (fos != null) fos.close();
		}
	}
	
	public DexExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}
	
	public BaseDexFileReader getReader() {
		return reader;
	}
	
	public Dex2jar reUseReg(boolean b) {
		if (b) {
			this.v3Config |= V3.REUSE_REGISTER;
		} else {
			this.v3Config &= ~V3.REUSE_REGISTER;
		}
		return this;
	}
	
	public Dex2jar topoLogicalSort(boolean b) {
		if (b) {
			this.v3Config |= V3.TOPOLOGICAL_SORT;
		} else {
			this.v3Config &= ~V3.TOPOLOGICAL_SORT;
		}
		return this;
	}
	
	public Dex2jar noCode(boolean b) {
		if (b) {
			this.readerConfig |= DexFileReader.SKIP_CODE | DexFileReader.KEEP_CLINIT;
		} else {
			this.readerConfig &= ~(DexFileReader.SKIP_CODE | DexFileReader.KEEP_CLINIT);
		}
		return this;
	}
	
	public Dex2jar optimizeSynchronized(boolean b) {
		if (b) {
			this.v3Config |= V3.OPTIMIZE_SYNCHRONIZED;
		} else {
			this.v3Config &= ~V3.OPTIMIZE_SYNCHRONIZED;
		}
		return this;
	}
	
	public Dex2jar printIR(boolean b) {
		if (b) {
			this.v3Config |= V3.PRINT_IR;
		} else {
			this.v3Config &= ~V3.PRINT_IR;
		}
		return this;
	}
	
	public Dex2jar reUseReg() {
		this.v3Config |= V3.REUSE_REGISTER;
		return this;
	}
	
	public Dex2jar optimizeSynchronized() {
		this.v3Config |= V3.OPTIMIZE_SYNCHRONIZED;
		return this;
	}
	
	public Dex2jar printIR() {
		this.v3Config |= V3.PRINT_IR;
		return this;
	}
	
	public Dex2jar topoLogicalSort() {
		this.v3Config |= V3.TOPOLOGICAL_SORT;
		return this;
	}
	
	public void setExceptionHandler(DexExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}
	
	public Dex2jar skipDebug(boolean b) {
		if (b) {
			this.readerConfig |= DexFileReader.SKIP_DEBUG;
		} else {
			this.readerConfig &= ~DexFileReader.SKIP_DEBUG;
		}
		return this;
	}
	
	public Dex2jar skipDebug() {
		this.readerConfig |= DexFileReader.SKIP_DEBUG;
		return this;
	}
	
	public void to(File file) throws IOException {
		if (file.isDirectory()) {
			doTranslate(file);
		} else {
			try (ZipOutputStream zipOutput = new ZipOutputStream(new FileOutputStream(file))) {
				// No need to add entries here; just create an empty ZIP file
			}
		}
	}
	
	public Dex2jar withExceptionHandler(DexExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
		return this;
	}
	
	public interface ProgressListener {
		void onProgressUpdate(int totalClasses, int processedClasses);
	}
	
	public Dex2jar skipExceptions(boolean b) {
		if (b) {
			this.readerConfig |= DexFileReader.SKIP_EXCEPTION;
		} else {
			this.readerConfig &= ~DexFileReader.SKIP_EXCEPTION;
		}
		return this;
	}
}
