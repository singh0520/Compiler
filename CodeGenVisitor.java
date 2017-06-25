package cop5556sp17;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

import cop5556sp17.Scanner.Kind;
import cop5556sp17.Scanner.Token;
import cop5556sp17.AST.ASTVisitor;
import cop5556sp17.AST.AssignmentStatement;
import cop5556sp17.AST.BinaryChain;
import cop5556sp17.AST.BinaryExpression;
import cop5556sp17.AST.Block;
import cop5556sp17.AST.BooleanLitExpression;
import cop5556sp17.AST.Chain;
import cop5556sp17.AST.ChainElem;
import cop5556sp17.AST.ConstantExpression;
import cop5556sp17.AST.Dec;
import cop5556sp17.AST.Expression;
import cop5556sp17.AST.FilterOpChain;
import cop5556sp17.AST.FrameOpChain;
import cop5556sp17.AST.IdentChain;
import cop5556sp17.AST.IdentExpression;
import cop5556sp17.AST.IdentLValue;
import cop5556sp17.AST.IfStatement;
import cop5556sp17.AST.ImageOpChain;
import cop5556sp17.AST.IntLitExpression;
import cop5556sp17.AST.ParamDec;
import cop5556sp17.AST.Program;
import cop5556sp17.AST.SleepStatement;
import cop5556sp17.AST.Statement;
import cop5556sp17.AST.Tuple;
import cop5556sp17.AST.Type;
import cop5556sp17.AST.Type.TypeName;
import cop5556sp17.AST.WhileStatement;

import static cop5556sp17.AST.Type.TypeName.FRAME;
import static cop5556sp17.AST.Type.TypeName.IMAGE;
import static cop5556sp17.AST.Type.TypeName.URL;
import static cop5556sp17.Scanner.Kind.*;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	/**
	 * @param DEVEL
	 *            used as parameter to genPrint and genPrintTOS
	 * @param GRADE
	 *            used as parameter to genPrint and genPrintTOS
	 * @param Source_File
	 *            name of source file, may be null.
	 */
	public CodeGenVisitor(boolean DEVEL, boolean GRADE, String Source_File) {
		super();
		this.DEVEL = DEVEL;
		this.GRADE = GRADE;
		this.Source_File = Source_File;
	}

	ClassWriter cw;
	String className;
	String Class_Desc;
	String Source_File;

	MethodVisitor mv; // visitor of method currently under construction

	/** Indicates whether genPrint and genPrintTOS should generate code. */
	final boolean DEVEL;
	final boolean GRADE;

	private int slot = 1;
	private int iteration = 0;

	boolean temp_binary = true;

	Label start_current;
	Label end_current;

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		// cw = new ClassWriter(0);
		className = program.getName();
		Class_Desc = "L" + className + ";";
		String Source_File = (String) arg;
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object",
				new String[] { "java/lang/Runnable" });
		cw.visitSource(Source_File, null);

		// generate constructor code
		// get a MethodVisitor
		mv = cw.visitMethod(ACC_PUBLIC, "<init>", "([Ljava/lang/String;)V", null, null);
		mv.visitCode();
		// Create label at start of code
		Label constructorStart = new Label();
		mv.visitLabel(constructorStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering <init>");
		// generate code to call superclass constructor
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		// visit parameter decs to add each as field to the class
		// pass in mv so decs can add their initialization code to the
		// constructor.
		ArrayList<ParamDec> params = program.getParams();
		for (ParamDec dec : params) {
			dec.visit(this, mv);
			iteration++;
		}
		mv.visitInsn(RETURN);
		// create label at end of code
		Label constructorEnd = new Label();
		mv.visitLabel(constructorEnd);
		// finish up by visiting local vars of constructor
		// the fourth and fifth arguments are the region of code where the local
		// variable is defined as represented by the labels we inserted.
		mv.visitLocalVariable("this", Class_Desc, null, constructorStart, constructorEnd, 0);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, constructorStart, constructorEnd, 1);
		// indicates the max stack size for the method.
		// because we used the COMPUTE_FRAMES parameter in the classwriter
		// constructor, asm
		// will do this for us. The parameters to visitMaxs don't matter, but
		// the method must
		// be called.
		mv.visitMaxs(1, 1);
		// finish up code generation for this method.
		mv.visitEnd();
		// end of constructor

		// create main method which does the following
		// 1. instantiate an instance of the class being generated, passing the
		// String[] with command line arguments
		// 2. invoke the run method.
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		mv.visitCode();
		Label mainStart = new Label();
		mv.visitLabel(mainStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering main");
		mv.visitTypeInsn(NEW, className);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "([Ljava/lang/String;)V", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, className, "run", "()V", false);
		mv.visitInsn(RETURN);
		Label mainEnd = new Label();
		mv.visitLabel(mainEnd);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart, mainEnd, 0);
		mv.visitLocalVariable("instance", Class_Desc, null, mainStart, mainEnd, 1);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		// create run method
		mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
		mv.visitCode();
		Label startRun = new Label();
		mv.visitLabel(startRun);
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering run");
		program.getB().visit(this, null);
		mv.visitInsn(RETURN);
		Label endRun = new Label();
		mv.visitLabel(endRun);
		mv.visitLocalVariable("this", Class_Desc, null, startRun, endRun, 0);

		// mv.visitLocalVariable("args", "[Ljava/lang/String;", null, startRun,
		// endRun, 1);
		for (Dec d : program.getB().getDecs())
			mv.visitLocalVariable(d.getIdent().getText(), d.getVal().getJVMTypeDesc(), null, startRun, endRun,
					d.getSlot());

		mv.visitMaxs(1, 1);
		mv.visitEnd(); // end of run method

		cw.visitEnd();// end of class

		// generate classfile and return it
		return cw.toByteArray();
	}

	@Override
	public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception {
		if (assignStatement.getVar().getDec() instanceof ParamDec) {
			mv.visitVarInsn(ALOAD, 0);
		}
		assignStatement.getE().visit(this, arg);
		CodeGenUtils.genPrint(DEVEL, mv, "\nassignment: " + assignStatement.var.getText() + "=");
		CodeGenUtils.genPrintTOS(GRADE, mv, assignStatement.getE().getType());

		if (assignStatement.getVar().getDec().getVal().equals(TypeName.IMAGE)) {
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "copyImage", PLPRuntimeImageOps.copyImageSig,
					false);
		}
		assignStatement.getVar().visit(this, arg);
		return null;
	}

	@Override
	public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception {
		// assert false : "not yet implemented";
		if (binaryChain.getE1().getFirstToken().kind.equals(OP_GRAY)) {
			if (binaryChain.getArrow().kind.equals(BARARROW)) {
				temp_binary = true;
			} else {
				temp_binary = false;
			}

		}
		binaryChain.getE0().setLeft(true);
		binaryChain.getE0().visit(this, arg);
		binaryChain.getE1().setLeft(false);
		binaryChain.getE1().visit(this, arg);

		return null;
	}

	@Override
	public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception {
		Label start_binary = new Label();
		Label complete = new Label();
		Kind temp = binaryExpression.getOp().kind;

		TypeName val1 = binaryExpression.getE0().getType();
		TypeName val2 = binaryExpression.getE1().getType();

		if (binaryExpression.getE1().getType().equals(TypeName.IMAGE)
				&& binaryExpression.getE0().getType().equals(TypeName.INTEGER)) {
			binaryExpression.getE1().visit(this, arg);
			binaryExpression.getE0().visit(this, arg);
		} else {
			binaryExpression.getE0().visit(this, arg);
			binaryExpression.getE1().visit(this, arg);
		}
		if (temp.equals(PLUS)) {
			if (val1.equals(TypeName.INTEGER) && val2.equals(TypeName.INTEGER))
				mv.visitInsn(IADD);
			else if (val1.equals(TypeName.IMAGE) && val2.equals(TypeName.IMAGE))
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "add", PLPRuntimeImageOps.addSig, false);
		}

		else if (temp.equals(MINUS)) {
			if (val1.equals(TypeName.INTEGER) && val2.equals(TypeName.INTEGER))
				mv.visitInsn(ISUB);
			else if (val1.equals(TypeName.IMAGE) && val2.equals(TypeName.IMAGE))
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "sub", PLPRuntimeImageOps.subSig, false);

		}

		else if (temp.equals(AND)) {
			mv.visitInsn(IAND);
		}

		else if (temp.equals(OR)) {
			mv.visitInsn(IOR);
		}

		else if (temp.equals(TIMES)) {
			if (binaryExpression.getE0().getType().equals(TypeName.IMAGE)
					|| binaryExpression.getE1().getType().equals(TypeName.IMAGE))
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "mul", PLPRuntimeImageOps.mulSig, false);
			else
				mv.visitInsn(IMUL);
		}

		else if (temp.equals(DIV)) {
			if (val1.equals(TypeName.INTEGER) && val2.equals(TypeName.INTEGER))
				mv.visitInsn(IDIV);
			else if (val1.equals(TypeName.IMAGE) && val2.equals(TypeName.INTEGER))
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "div", PLPRuntimeImageOps.divSig, false);
		}

		else if (temp.equals(MOD)) {
			if (val1.equals(TypeName.INTEGER) && val2.equals(TypeName.INTEGER))
				mv.visitInsn(IREM);
			else if (val1.equals(TypeName.IMAGE) && val2.equals(TypeName.INTEGER))
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "mod", PLPRuntimeImageOps.modSig, false);

		}

		else if (temp.equals(LT)) {
			mv.visitJumpInsn(IF_ICMPLT, start_binary);
			mv.visitLdcInsn(false);
		}

		else if (temp.equals(LE)) {
			mv.visitJumpInsn(IF_ICMPLE, start_binary);
			mv.visitLdcInsn(false);
		}

		else if (temp.equals(GT)) {
			mv.visitJumpInsn(IF_ICMPGT, start_binary);
			mv.visitLdcInsn(false);
		}

		else if (temp.equals(GE)) {
			mv.visitJumpInsn(IF_ICMPGE, start_binary);
			mv.visitLdcInsn(false);
		}

		else if (temp.equals(EQUAL)) {
			if ((val1.equals(TypeName.INTEGER) && val2.equals(TypeName.INTEGER))
					|| (val1.equals(TypeName.BOOLEAN) || val2.equals(TypeName.BOOLEAN)))
				mv.visitJumpInsn(IF_ICMPEQ, start_binary);
			else
				mv.visitJumpInsn(IF_ACMPEQ, start_binary);
			mv.visitLdcInsn(false);
		}

		else if (temp.equals(NOTEQUAL)) {
			if ((val1.equals(TypeName.INTEGER) && val2.equals(TypeName.INTEGER))
					|| (val1.equals(TypeName.BOOLEAN) || val2.equals(TypeName.BOOLEAN)))
				mv.visitJumpInsn(IF_ICMPNE, start_binary);
			else
				mv.visitJumpInsn(IF_ACMPNE, start_binary);
			mv.visitLdcInsn(false);
		}

		mv.visitJumpInsn(GOTO, complete);
		mv.visitLabel(start_binary);
		mv.visitLdcInsn(true);
		mv.visitLabel(complete);
		return null;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		Label BEGIN_BLOCK = new Label();
		Label END_BLOCK = new Label();

		for (Dec d : block.getDecs()) {
			if (d.getFirstToken().kind.equals(KW_IMAGE) || d.getFirstToken().kind.equals(KW_FRAME)) {
				mv.visitInsn(ACONST_NULL);
				mv.visitVarInsn(ASTORE, slot);
			}
			d.setSlot(slot++);
		}
		start_current = BEGIN_BLOCK;
		mv.visitLabel(BEGIN_BLOCK);
		for (Statement s : block.getStatements()) {
			s.visit(this, arg);
			if (s instanceof BinaryChain && (!((BinaryChain) s).getE0().getTypename().isType(TypeName.INTEGER)))
				mv.visitInsn(POP);
		}
		end_current = END_BLOCK;
		mv.visitLabel(END_BLOCK);
		for (Dec d : block.getDecs())
			d.visit(this, mv);
		return null;
	}

	@Override
	public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception {
		mv.visitLdcInsn(booleanLitExpression.getValue());
		return null;
	}

	@Override
	public Object visitConstantExpression(ConstantExpression constantExpression, Object arg) {
		if (constantExpression.getFirstToken().kind.equals(KW_SCREENHEIGHT))
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "getScreenHeight",
					PLPRuntimeFrame.getScreenHeightSig, false);

		else if (constantExpression.getFirstToken().kind.equals(KW_SCREENWIDTH))
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "getScreenWidth",
					PLPRuntimeFrame.getScreenWidthSig, false);

		return null;
	}

	@Override
	public Object visitDec(Dec declaration, Object arg) throws Exception {
		mv.visitLocalVariable(declaration.getIdent().getText(),
				Type.getTypeName(declaration.getType()).getJVMTypeDesc(), null, start_current, end_current,
				declaration.getSlot());
		return null;
	}

	@Override
	public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception {
		if (filterOpChain.getFirstToken().kind.equals(OP_BLUR)) {
			mv.visitInsn(ACONST_NULL);
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "blurOp", PLPRuntimeFilterOps.opSig, false);
		}

		else if (filterOpChain.getFirstToken().kind.equals(OP_CONVOLVE)) {
			mv.visitInsn(ACONST_NULL);
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "convolveOp", PLPRuntimeFilterOps.opSig,
					false);
		}

		else if (filterOpChain.getFirstToken().kind.equals(OP_GRAY)) {
			if (temp_binary == true)
				mv.visitInsn(DUP);
			else
				mv.visitInsn(ACONST_NULL);
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "grayOp", PLPRuntimeFilterOps.opSig, false);
		}

		return null;
	}

	@Override
	public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception {
		frameOpChain.getArg().visit(this, arg);
		Kind temp = frameOpChain.getFirstToken().kind;
		if (temp.equals(KW_XLOC)) {
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "getXVal", PLPRuntimeFrame.getXValDesc,
					false);

		} else if (temp.equals(KW_YLOC)) {
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "getYVal", PLPRuntimeFrame.getYValDesc,
					false);

		}

		else if (temp.equals(KW_HIDE)) {
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "hideImage", PLPRuntimeFrame.hideImageDesc,
					false);

		}

		else if (temp.equals(KW_SHOW)) {
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "showImage", PLPRuntimeFrame.showImageDesc,
					false);

		}

		else if (temp.equals(KW_MOVE)) {
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "moveFrame", PLPRuntimeFrame.moveFrameDesc,
					false);

		}

		return null;
	}

	@Override
	public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception {

		TypeName type = identChain.getTypename();

		if (identChain.isLeft()) {
			if (identChain.getDec() instanceof ParamDec) {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, className, identChain.getFirstToken().getText(),
						identChain.getTypename().getJVMTypeDesc());
				switch (type) {
				case URL:
					mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "readFromURL",
							PLPRuntimeImageIO.readFromURLSig, false);
					break;
				case FILE:
					mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "readFromFile",
							PLPRuntimeImageIO.readFromFileDesc, false);
					break;
				default:
					break;
				}
			} else {
				if (identChain.getTypename().equals(TypeName.INTEGER)
						|| identChain.getTypename().equals(TypeName.BOOLEAN))
					mv.visitVarInsn(ILOAD, identChain.getDec().getSlot());
				else {
					switch (type) {
					case URL:
						mv.visitVarInsn(ALOAD, identChain.getDec().getSlot());
						mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "readFromURL",
								PLPRuntimeImageIO.readFromURLSig, false);
						break;
					case FILE:
						mv.visitVarInsn(ALOAD, identChain.getDec().getSlot());
						mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "readFromFile",
								PLPRuntimeImageIO.readFromFileDesc, false);
						break;
					case FRAME:
					case IMAGE:
						mv.visitVarInsn(ALOAD, identChain.getDec().getSlot());
						break;
					default:
					}
				}
			}
		} else {
			if (identChain.getTypename().equals(TypeName.FILE)) {
				if (identChain.getDec() instanceof ParamDec) {
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, className, identChain.getDec().getIdent().getText(),
							type.getJVMTypeDesc());
				} else
					mv.visitVarInsn(ALOAD, identChain.getDec().getSlot());
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "write", PLPRuntimeImageIO.writeImageDesc,
						false);
			} else if (identChain.getTypename().equals(TypeName.FRAME)) {
				mv.visitVarInsn(ALOAD, identChain.getDec().getSlot());
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "createOrSetFrame",
						PLPRuntimeFrame.createOrSetFrameSig, false);
				mv.visitInsn(DUP);
				mv.visitVarInsn(ASTORE, identChain.getDec().getSlot());
			}
			if (!identChain.getTypename().equals(TypeName.FRAME))
				mv.visitInsn(DUP);
			if (identChain.getTypename().equals(TypeName.IMAGE) || identChain.getTypename().equals(TypeName.INTEGER)
					|| identChain.getTypename().equals(TypeName.BOOLEAN)) {
				if (identChain.getDec() instanceof ParamDec) {
					mv.visitVarInsn(ALOAD, 0);
					mv.visitInsn(SWAP);
					mv.visitFieldInsn(PUTFIELD, className, identChain.getFirstToken().getText(),
							identChain.getTypename().getJVMTypeDesc());
				} else {
					if (identChain.getTypename().equals(TypeName.IMAGE))
						mv.visitVarInsn(ASTORE, identChain.getDec().getSlot());
					else if (identChain.getTypename().equals(TypeName.INTEGER)
							|| identChain.getTypename().equals(TypeName.BOOLEAN))
						mv.visitVarInsn(ISTORE, identChain.getDec().getSlot());
				}
			}
		}
		return null;

	}

	@Override
	public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception {
		if (identExpression.getDec() instanceof ParamDec) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, className, identExpression.getFirstToken().getText(),
					identExpression.getType().getJVMTypeDesc());
		} else {
			switch (identExpression.getType()) {
			case INTEGER:
			case BOOLEAN:
				mv.visitVarInsn(ILOAD, identExpression.getDec().getSlot());
				break;
			case IMAGE:
			case FRAME:
			case FILE:
			case URL:
				mv.visitVarInsn(ALOAD, identExpression.getDec().getSlot());
				break;
			default:
				break;
			}

		}

		return null;
	}

	@Override
	public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception {
		if (identX.getDec() instanceof ParamDec)
			mv.visitFieldInsn(PUTFIELD, className, identX.getFirstToken().getText(),
					identX.getDec().getVal().getJVMTypeDesc());
		else {
			switch (identX.getDec().getVal()) {
			case IMAGE: {
				mv.visitVarInsn(ASTORE, identX.getDec().getSlot());
			}
				break;
			case INTEGER:
			case BOOLEAN:
				mv.visitVarInsn(ISTORE, identX.getDec().getSlot());
				break;
			default:
				break;
			}

		}
		return null;

	}

	@Override
	public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception {
		ifStatement.getE().visit(this, arg);
		Label AFTER = new Label();
		mv.visitJumpInsn(IFEQ, AFTER);
		ifStatement.getB().visit(this, arg);
		mv.visitLabel(AFTER);
		return null;

	}

	@Override
	public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception {
		imageOpChain.getArg().visit(this, arg);
		switch (imageOpChain.getFirstToken().kind) {
		case OP_WIDTH:
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeImageIO.BufferedImageClassName, "getWidth",
					PLPRuntimeImageOps.getWidthSig, false);
			break;

		case OP_HEIGHT:
			mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeImageIO.BufferedImageClassName, "getHeight",
					PLPRuntimeImageOps.getHeightSig, false);
			break;

		case KW_SCALE:
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "scale", PLPRuntimeImageOps.scaleSig, false);
			break;
		default:
			break;
		}
		return null;
	}

	@Override
	public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception {
		mv.visitLdcInsn(intLitExpression.value);
		return null;
	}

	@Override
	public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception {
		cw.visitField(ACC_PUBLIC, paramDec.getIdent().getText(), Type.getTypeName(paramDec.getType()).getJVMTypeDesc(),
				null, null);

		TypeName t = Type.getTypeName(paramDec.getType());
		mv.visitVarInsn(ALOAD, 0);

		if (t.equals(TypeName.INTEGER)) {
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(iteration);
			mv.visitInsn(AALOAD);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
		} else if (t.equals(TypeName.BOOLEAN)) {
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(iteration);
			mv.visitInsn(AALOAD);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
		} else if (t.equals(TypeName.FILE)) {
			mv.visitTypeInsn(NEW, "java/io/File");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(iteration);
			mv.visitInsn(AALOAD);
			mv.visitMethodInsn(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V", false);
		} else if (t.equals(TypeName.URL)) {
			mv.visitVarInsn(ALOAD, 1);
			mv.visitLdcInsn(iteration);
			mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "getURL", PLPRuntimeImageIO.getURLSig, false);
		}
		mv.visitFieldInsn(PUTFIELD, className, paramDec.getIdent().getText(), t.getJVMTypeDesc());

		return null;

	}

	@Override
	public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception {
		sleepStatement.getE().visit(this, arg);
		mv.visitInsn(I2L);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false);

		return null;
	}

	@Override
	public Object visitTuple(Tuple tuple, Object arg) throws Exception {
		for (Expression e : tuple.getExprList())
			e.visit(this, arg);
		return null;
	}

	@Override
	public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception {
		Label GUARD = new Label();
		mv.visitJumpInsn(GOTO, GUARD);
		Label BODY = new Label();
		mv.visitLabel(BODY);
		whileStatement.getB().visit(this, arg);
		mv.visitLabel(GUARD);
		whileStatement.getE().visit(this, arg);
		mv.visitJumpInsn(IFNE, BODY);
		return null;

	}

}
