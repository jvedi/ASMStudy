package com.jianglei.asmplugin

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter


public class TraceClassVisitor extends ClassVisitor {

    private String className

    TraceClassVisitor(int i, ClassVisitor classVisitor) {
        super(i, classVisitor)
    }

    @Override
    void visit(int version, int access, String name, String signature, String superName, String[] exceptions) {
        super.visit(version, access, name, signature, superName, exceptions)
        this.className = name
    }

    @Override
    MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        def methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions)
        return new TraceMethodVisitor(api, methodVisitor, access, name, desc, className)
    }
}

public class TraceMethodVisitor extends AdviceAdapter {

    private String className
    private String methodName

    protected TraceMethodVisitor(int api, MethodVisitor mv, int access, String name, String desc, String className) {
        super(api, mv, access, name, desc)
        this.className = className
        this.methodName = name
    }

    @Override
    void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        super.visitMethodInsn(opcode, owner, name, desc, itf)
    }

    private int timeLocalIndex = 0

    @Override
    protected void onMethodEnter() {
        LogUtils.i(" insert method:" + className + " : " + methodName)
        super.onMethodEnter()
        timeLocalIndex = newLocal(Type.LONG_TYPE)
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        //将结果保存在变量表中
        mv.visitVarInsn(LSTORE, timeLocalIndex)
    }

    @Override
    protected void onMethodExit(int opcode) {
        super.onMethodExit(opcode)
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        //将方法进入是的记录的时间入栈
        mv.visitVarInsn(LLOAD, timeLocalIndex)
        //相减
        mv.visitInsn(LSUB)
        //将相减的结果保存在变量表中
        mv.visitVarInsn(LSTORE, timeLocalIndex)

        //下面开始插入Log.d("longyi","$methodName:45 ms")

        mv.visitLdcInsn("longyi")
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder")
        mv.visitInsn(DUP)
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
        mv.visitLdcInsn(className+" : ")
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
        mv.visitLdcInsn(methodName + ": ")
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
        mv.visitVarInsn(LLOAD, timeLocalIndex)
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false)
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
        mv.visitMethodInsn(INVOKESTATIC, "android/util/Log", "d", "(Ljava/lang/String;Ljava/lang/String;)I", false)
        mv.visitInsn(POP)


    }
}