package org.jetbrains.kotlin.j2k.tree.visitors

import org.jetbrains.kotlin.j2k.tree.*

interface JKTransformer<in D> : JKVisitor<JKElement, D> {
    override fun visitElement(element: JKElement, data: D): JKElement 
    override fun visitDeclaration(declaration: JKDeclaration, data: D): JKDeclaration = visitElement(declaration, data) as JKDeclaration
    override fun visitClass(klass: JKClass, data: D): JKClass = visitDeclaration(klass, data) as JKClass
    override fun visitMethod(method: JKMethod, data: D): JKMethod = visitDeclaration(method, data) as JKMethod
    override fun visitField(field: JKField, data: D): JKField = visitDeclaration(field, data) as JKField
    override fun visitModifier(modifier: JKModifier, data: D): JKModifier = visitElement(modifier, data) as JKModifier
    override fun visitModifierList(modifierList: JKModifierList, data: D): JKModifierList = visitElement(modifierList, data) as JKModifierList
    override fun visitAccessModifier(accessModifier: JKAccessModifier, data: D): JKAccessModifier = visitModifier(accessModifier, data) as JKAccessModifier
    override fun visitModalityModifier(modalityModifier: JKModalityModifier, data: D): JKModalityModifier = visitModifier(modalityModifier, data) as JKModalityModifier
    override fun visitReference(reference: JKReference, data: D): JKReference = visitElement(reference, data) as JKReference
    override fun visitMethodReference(methodReference: JKMethodReference, data: D): JKMethodReference = visitReference(methodReference, data) as JKMethodReference
    override fun visitFieldReference(fieldReference: JKFieldReference, data: D): JKFieldReference = visitReference(fieldReference, data) as JKFieldReference
    override fun visitClassReference(classReference: JKClassReference, data: D): JKClassReference = visitReference(classReference, data) as JKClassReference
    override fun visitType(type: JKType, data: D): JKType = visitElement(type, data) as JKType
    override fun visitClassType(classType: JKClassType, data: D): JKClassType = visitType(classType, data) as JKClassType
    override fun visitStatement(statement: JKStatement, data: D): JKStatement = visitElement(statement, data) as JKStatement
    override fun visitLoop(loop: JKLoop, data: D): JKLoop = visitStatement(loop, data) as JKLoop
    override fun visitBlock(block: JKBlock, data: D): JKBlock = visitElement(block, data) as JKBlock
    override fun visitIdentifier(identifier: JKIdentifier, data: D): JKIdentifier = visitElement(identifier, data) as JKIdentifier
    override fun visitNameIdentifier(nameIdentifier: JKNameIdentifier, data: D): JKNameIdentifier = visitIdentifier(nameIdentifier, data) as JKNameIdentifier
    override fun visitExpression(expression: JKExpression, data: D): JKExpression = visitElement(expression, data) as JKExpression
    override fun visitExpressionStatement(expressionStatement: JKExpressionStatement, data: D): JKExpressionStatement = visitStatement(expressionStatement, data) as JKExpressionStatement
    override fun visitBinaryExpression(binaryExpression: JKBinaryExpression, data: D): JKBinaryExpression = visitExpression(binaryExpression, data) as JKBinaryExpression
    override fun visitUnaryExpression(unaryExpression: JKUnaryExpression, data: D): JKUnaryExpression = visitExpression(unaryExpression, data) as JKUnaryExpression
    override fun visitPrefixExpression(prefixExpression: JKPrefixExpression, data: D): JKPrefixExpression = visitUnaryExpression(prefixExpression, data) as JKPrefixExpression
    override fun visitPostfixExpression(postfixExpression: JKPostfixExpression, data: D): JKPostfixExpression = visitUnaryExpression(postfixExpression, data) as JKPostfixExpression
    override fun visitQualifiedExpression(qualifiedExpression: JKQualifiedExpression, data: D): JKQualifiedExpression = visitExpression(qualifiedExpression, data) as JKQualifiedExpression
    override fun visitMethodCallExpression(methodCallExpression: JKMethodCallExpression, data: D): JKMethodCallExpression = visitExpression(methodCallExpression, data) as JKMethodCallExpression
    override fun visitFieldAccessExpression(fieldAccessExpression: JKFieldAccessExpression, data: D): JKFieldAccessExpression = visitExpression(fieldAccessExpression, data) as JKFieldAccessExpression
    override fun visitArrayAccessExpression(arrayAccessExpression: JKArrayAccessExpression, data: D): JKArrayAccessExpression = visitExpression(arrayAccessExpression, data) as JKArrayAccessExpression
    override fun visitParenthesizedExpression(parenthesizedExpression: JKParenthesizedExpression, data: D): JKParenthesizedExpression = visitExpression(parenthesizedExpression, data) as JKParenthesizedExpression
    override fun visitTypeCastExpression(typeCastExpression: JKTypeCastExpression, data: D): JKTypeCastExpression = visitExpression(typeCastExpression, data) as JKTypeCastExpression
    override fun visitExpressionList(expressionList: JKExpressionList, data: D): JKExpressionList = visitElement(expressionList, data) as JKExpressionList
    override fun visitLiteralExpression(literalExpression: JKLiteralExpression, data: D): JKLiteralExpression = visitExpression(literalExpression, data) as JKLiteralExpression
    override fun visitValueArgument(valueArgument: JKValueArgument, data: D): JKValueArgument = visitElement(valueArgument, data) as JKValueArgument
    override fun visitStringLiteralExpression(stringLiteralExpression: JKStringLiteralExpression, data: D): JKStringLiteralExpression = visitLiteralExpression(stringLiteralExpression, data) as JKStringLiteralExpression
    override fun visitJavaField(javaField: JKJavaField, data: D): JKField = visitField(javaField, data) 
    override fun visitJavaMethod(javaMethod: JKJavaMethod, data: D): JKMethod = visitMethod(javaMethod, data) 
    override fun visitJavaForLoop(javaForLoop: JKJavaForLoop, data: D): JKLoop = visitLoop(javaForLoop, data) 
    override fun visitJavaAssignmentExpression(javaAssignmentExpression: JKJavaAssignmentExpression, data: D): JKExpression = visitExpression(javaAssignmentExpression, data) 
    override fun visitJavaPrimitiveType(javaPrimitiveType: JKJavaPrimitiveType, data: D): JKType = visitType(javaPrimitiveType, data) 
    override fun visitJavaArrayType(javaArrayType: JKJavaArrayType, data: D): JKType = visitType(javaArrayType, data) 
    override fun visitJavaMethodCallExpression(javaMethodCallExpression: JKJavaMethodCallExpression, data: D): JKMethodCallExpression = visitMethodCallExpression(javaMethodCallExpression, data) 
    override fun visitJavaFieldAccessExpression(javaFieldAccessExpression: JKJavaFieldAccessExpression, data: D): JKFieldAccessExpression = visitFieldAccessExpression(javaFieldAccessExpression, data) 
    override fun visitJavaNewExpression(javaNewExpression: JKJavaNewExpression, data: D): JKExpression = visitExpression(javaNewExpression, data) 
    override fun visitJavaAccessModifier(javaAccessModifier: JKJavaAccessModifier, data: D): JKAccessModifier = visitAccessModifier(javaAccessModifier, data) 
    override fun visitJavaModifier(javaModifier: JKJavaModifier, data: D): JKModifier = visitModifier(javaModifier, data) 
    override fun visitJavaNewEmptyArray(javaNewEmptyArray: JKJavaNewEmptyArray, data: D): JKExpression = visitExpression(javaNewEmptyArray, data) 
    override fun visitJavaNewArray(javaNewArray: JKJavaNewArray, data: D): JKExpression = visitExpression(javaNewArray, data) 
    override fun visitJavaLiteralExpression(javaLiteralExpression: JKJavaLiteralExpression, data: D): JKLiteralExpression = visitLiteralExpression(javaLiteralExpression, data) 
    override fun visitKtProperty(ktProperty: JKKtProperty, data: D): JKField = visitField(ktProperty, data) 
    override fun visitKtFunction(ktFunction: JKKtFunction, data: D): JKMethod = visitMethod(ktFunction, data) 
    override fun visitKtConstructor(ktConstructor: JKKtConstructor, data: D): JKDeclaration = visitDeclaration(ktConstructor, data) 
    override fun visitKtPrimaryConstructor(ktPrimaryConstructor: JKKtPrimaryConstructor, data: D): JKDeclaration = visitKtConstructor(ktPrimaryConstructor, data) 
    override fun visitKtAssignmentStatement(ktAssignmentStatement: JKKtAssignmentStatement, data: D): JKStatement = visitStatement(ktAssignmentStatement, data) 
    override fun visitKtCall(ktCall: JKKtCall, data: D): JKMethodCallExpression = visitMethodCallExpression(ktCall, data) 
    override fun visitKtModifier(ktModifier: JKKtModifier, data: D): JKModifier = visitModifier(ktModifier, data) 
    override fun visitKtMethodCallExpression(ktMethodCallExpression: JKKtMethodCallExpression, data: D): JKMethodCallExpression = visitMethodCallExpression(ktMethodCallExpression, data) 
    override fun visitKtFieldAccessExpression(ktFieldAccessExpression: JKKtFieldAccessExpression, data: D): JKFieldAccessExpression = visitFieldAccessExpression(ktFieldAccessExpression, data) 
    override fun visitKtLiteralExpression(ktLiteralExpression: JKKtLiteralExpression, data: D): JKLiteralExpression = visitLiteralExpression(ktLiteralExpression, data) 
}
