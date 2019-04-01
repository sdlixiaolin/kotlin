/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.j2k

import com.intellij.psi.*
import com.intellij.psi.JavaTokenType.SUPER_KEYWORD
import com.intellij.psi.JavaTokenType.THIS_KEYWORD
import com.intellij.psi.impl.source.tree.ChildRole
import com.intellij.psi.impl.source.tree.java.*
import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.JKLiteralExpression.LiteralType.*
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class JavaToJKTreeBuilder(var symbolProvider: JKSymbolProvider) {

    private val expressionTreeMapper = ExpressionTreeMapper()

    private val declarationMapper = DeclarationMapper(expressionTreeMapper)

    private fun PsiJavaFile.toJK(): JKFile =
        JKFileImpl(
            packageStatement?.toJK() ?: JKPackageDeclarationImpl(JKNameIdentifierImpl("")),
            importList?.importStatements?.map { it.toJK() }.orEmpty(),
            with(declarationMapper) { classes.map { it.toJK() } }
        )

   private fun PsiPackageStatement.toJK(): JKPackageDeclaration =
        JKPackageDeclarationImpl(JKNameIdentifierImpl(packageName))

    private fun PsiImportStatement.toJK() =
        JKImportStatementImpl(
            JKNameIdentifierImpl(
                text.substringAfter("import").substringBeforeLast(";").trim()
            )
        )


    private inner class ExpressionTreeMapper {
        fun PsiExpression?.toJK(): JKExpression {
            return when (this) {
                null -> JKStubExpressionImpl()
                is PsiBinaryExpression -> toJK()
                is PsiPrefixExpression -> toJK()
                is PsiPostfixExpression -> toJK()
                is PsiLiteralExpression -> toJK()
                is PsiMethodCallExpression -> toJK()
                is PsiReferenceExpression -> toJK()
                is PsiNewExpression -> toJK()
                is PsiArrayAccessExpression -> toJK()
                is PsiTypeCastExpression -> toJK()
                is PsiParenthesizedExpression -> toJK()
                is PsiAssignmentExpression -> toJK()
                is PsiInstanceOfExpression -> toJK()
                is PsiThisExpression ->
                    JKThisExpressionImpl(
                        qualifier?.referenceName?.let { JKLabelTextImpl(JKNameIdentifierImpl(it)) } ?: JKLabelEmptyImpl()
                    )
                is PsiSuperExpression ->
                    JKSuperExpressionImpl(
                        qualifier?.referenceName?.let { JKLabelTextImpl(JKNameIdentifierImpl(it)) } ?: JKLabelEmptyImpl()
                    )
                is PsiConditionalExpression -> JKIfElseExpressionImpl(
                    condition.toJK(), thenExpression.toJK(), elseExpression.toJK()
                )
                is PsiPolyadicExpression -> JKJavaPolyadicExpressionImpl(
                    operands.map { it.toJK() },
                    Array(operands.lastIndex) { getTokenBeforeOperand(operands[it + 1]) }.map { it?.toJK() ?: TODO() }
                )
                is PsiArrayInitializerExpression -> toJK()
                is PsiLambdaExpression -> toJK()
                is PsiClassObjectAccessExpressionImpl -> toJK()
                else -> {
                    throw RuntimeException("Not supported: ${this::class}")
                }
            }.also {
                if (this != null) (it as PsiOwner).psi = this
            }
        }

        fun PsiClassObjectAccessExpressionImpl.toJK(): JKClassLiteralExpression {
            val type = operand.toJK()
            return JKClassLiteralExpressionImpl(
                type,
                if (type.type is JKJavaPrimitiveType) JKClassLiteralExpression.LiteralType.JAVA_PRIMITIVE_CLASS
                else JKClassLiteralExpression.LiteralType.JAVA_CLASS
            )
        }

        fun PsiInstanceOfExpression.toJK(): JKJavaInstanceOfExpression {
            return JKJavaInstanceOfExpressionImpl(operand.toJK(), checkType?.toJK() ?: TODO())
        }

        fun PsiAssignmentExpression.toJK(): JKJavaAssignmentExpression {
            return JKJavaAssignmentExpressionImpl(
                lExpression.toJK() as? JKAssignableExpression ?: error("Its possible? ${lExpression.toJK().prettyDebugPrintTree()}"),
                rExpression.toJK(),
                operationSign.toJK()
            )
        }

        fun PsiBinaryExpression.toJK(): JKExpression {
            return JKBinaryExpressionImpl(lOperand.toJK(), rOperand.toJK(), operationSign.toJK())
        }

        fun PsiLiteralExpression.toJK(): JKLiteralExpression {
            require(this is PsiLiteralExpressionImpl)

            return when (this.literalElementType) {
                JavaTokenType.NULL_KEYWORD -> JKNullLiteral()
                JavaTokenType.TRUE_KEYWORD -> JKBooleanLiteral(true)
                JavaTokenType.FALSE_KEYWORD -> JKBooleanLiteral(false)
                JavaTokenType.STRING_LITERAL -> JKJavaLiteralExpressionImpl(text, STRING)
                JavaTokenType.CHARACTER_LITERAL -> JKJavaLiteralExpressionImpl(text, CHAR)
                JavaTokenType.INTEGER_LITERAL -> JKJavaLiteralExpressionImpl(text, INT)
                JavaTokenType.LONG_LITERAL -> JKJavaLiteralExpressionImpl(text, LONG)
                JavaTokenType.FLOAT_LITERAL -> JKJavaLiteralExpressionImpl(text, FLOAT)
                JavaTokenType.DOUBLE_LITERAL -> JKJavaLiteralExpressionImpl(text, DOUBLE)
                else -> error("Unknown literal element type: ${this.literalElementType}")
            }
        }

        fun PsiJavaToken.toJK(): JKOperator = JKJavaOperatorImpl.tokenToOperator[tokenType] ?: error("Unsupported token-type: $tokenType")

        fun PsiPrefixExpression.toJK(): JKExpression {
            return JKPrefixExpressionImpl(operand.toJK(), operationSign.toJK()).let {
                if (it.operator.token.text in listOf("+", "-") && it.expression is JKLiteralExpression) {
                    JKJavaLiteralExpressionImpl(
                        it.operator.token.text + (it.expression as JKLiteralExpression).literal,
                        (it.expression as JKLiteralExpression).type
                    )
                } else it
            }
        }

        fun PsiPostfixExpression.toJK(): JKExpression {
            return JKPostfixExpressionImpl(operand.toJK(), operationSign.toJK())
        }

        fun PsiLambdaExpression.toJK(): JKExpression {
            return JKLambdaExpressionImpl(
                with(declarationMapper) { parameterList.parameters.map { it.toJK() } },
                body.let {
                    when (it) {
                        is PsiExpression -> JKExpressionStatementImpl(it.toJK())
                        is PsiCodeBlock -> JKBlockStatementImpl(with(declarationMapper) { it.toJK() })
                        else -> JKBlockStatementImpl(JKBodyStub)
                    }
                })
        }

        fun PsiMethodCallExpression.toJK(): JKExpression {
            val method = methodExpression as PsiReferenceExpressionImpl
            val referenceNameElement = methodExpression.referenceNameElement
            val symbol = symbolProvider.provideSymbol<JKMethodSymbol>(method)
            return if (referenceNameElement is PsiKeyword) {
                val callee = when (referenceNameElement.tokenType) {
                    SUPER_KEYWORD -> JKSuperExpressionImpl()
                    THIS_KEYWORD -> JKThisExpressionImpl(JKLabelEmptyImpl())
                    else -> error("Unknown keyword in callee position")
                }
                JKDelegationConstructorCallImpl(symbol, callee, argumentList.toJK())
            } else {
                val call = JKJavaMethodCallExpressionImpl(symbol, argumentList.toJK(), typeArgumentList.toJK())
                if (method.findChildByRole(ChildRole.DOT) != null) {
                    JKQualifiedExpressionImpl((method.qualifier as PsiExpression).toJK(), JKJavaQualifierImpl.DOT, call)
                } else {
                    call
                }
            }
        }

        fun PsiReferenceExpression.toJK(): JKExpression {
            val symbol = symbolProvider.provideSymbol(this)
            val access = when (symbol) {
                is JKClassSymbol -> JKClassAccessExpressionImpl(symbol)
                is JKFieldSymbol -> JKFieldAccessExpressionImpl(symbol)
                else -> TODO()
            }
            return qualifierExpression?.let { JKQualifiedExpressionImpl(it.toJK(), JKJavaQualifierImpl.DOT, access) } ?: access
        }

        fun PsiArrayInitializerExpression.toJK(): JKExpression {
            return JKJavaNewArrayImpl(
                initializers.map { it.toJK() },
                JKTypeElementImpl(type?.toJK(symbolProvider).safeAs<JKJavaArrayType>()?.type ?: JKContextType)
            )
        }

        fun PsiNewExpression.toJK(): JKExpression {
            require(this is PsiNewExpressionImpl)
            val newExpression =
                if (findChildByRole(ChildRole.LBRACKET) != null) {
                    arrayInitializer?.toJK() ?: run {
                        val dimensions = mutableListOf<PsiExpression?>()
                        var child = firstChild
                        while (child != null) {
                            if (child.node.elementType == JavaTokenType.LBRACKET) {
                                child = child.nextSibling
                                dimensions += if (child.node.elementType == JavaTokenType.RBRACKET) {
                                    null
                                } else {
                                    child as PsiExpression? //TODO
                                }
                            }
                            child = child.nextSibling
                        }
                        JKJavaNewEmptyArrayImpl(
                            dimensions.map { it?.toJK() ?: JKStubExpressionImpl() },
                            JKTypeElementImpl(generateSequence(type?.toJK(symbolProvider)) { it.safeAs<JKJavaArrayType>()?.type }.last())
                        ).also {
                            it.psi = this
                        }
                    }
                } else {
                    val classSymbol =
                        classOrAnonymousClassReference?.resolve()?.let {
                            symbolProvider.provideDirectSymbol(it) as JKClassSymbol
                        } ?: JKUnresolvedClassSymbol(classOrAnonymousClassReference!!.text)

                    JKJavaNewExpressionImpl(
                        classSymbol,
                        argumentList.toJK(),
                        typeArgumentList.toJK(),
                        with(declarationMapper) { anonymousClass?.createClassBody() } ?: JKEmptyClassBodyImpl()
                    )
                }
            return qualifier?.let { JKQualifiedExpressionImpl(it.toJK(), JKJavaQualifierImpl.DOT, newExpression) } ?: newExpression
        }

        fun PsiReferenceParameterList.toJK(): JKTypeArgumentList =
            JKTypeArgumentListImpl(this.typeArguments.map { JKTypeElementImpl(it.toJK(symbolProvider)) })


        fun PsiArrayAccessExpression.toJK(): JKExpression {
            return JKArrayAccessExpressionImpl(arrayExpression.toJK(), indexExpression?.toJK() ?: TODO())
        }

        fun PsiTypeCastExpression.toJK(): JKExpression {
            return JKTypeCastExpressionImpl(operand?.toJK() ?: TODO(), castType?.toJK() ?: TODO())
        }

        fun PsiParenthesizedExpression.toJK(): JKExpression {
            return JKParenthesizedExpressionImpl(expression?.toJK() ?: TODO())
        }

        fun PsiExpressionList?.toJK(): JKExpressionList {
            return JKExpressionListImpl(this?.expressions?.map { it.toJK() } ?: emptyList())
        }

        fun PsiTypeElement.toJK(): JKTypeElement {
            return JKTypeElementImpl(type.toJK(symbolProvider)).also {
                (it as PsiOwner).psi = this
            }
        }
    }

    private inner class DeclarationMapper(val expressionTreeMapper: ExpressionTreeMapper) {
        fun PsiTypeParameterList.toJK(): JKTypeParameterList =
            JKTypeParameterListImpl(typeParameters.map { it.toJK() })

        fun PsiTypeParameter.toJK(): JKTypeParameter =
            JKTypeParameterImpl(JKNameIdentifierImpl(name!!),
                                extendsListTypes.map { JKTypeElementImpl(it.toJK(symbolProvider, Nullability.NotNull)) })

        fun PsiClass.toJK(): JKClass {
            val classKind: JKClass.ClassKind = when {
                isAnnotationType -> JKClass.ClassKind.ANNOTATION
                isEnum -> JKClass.ClassKind.ENUM
                isInterface -> JKClass.ClassKind.INTERFACE
                else -> JKClass.ClassKind.CLASS
            }

            fun PsiReferenceList.mapTypes() =
                this.referencedTypes.map { with(expressionTreeMapper) { JKTypeElementImpl(it.toJK(symbolProvider, Nullability.NotNull)) } }

            val implTypes = this.implementsList?.mapTypes().orEmpty()
            val extensionType = this.extendsList?.mapTypes().orEmpty()
            return JKClassImpl(
                JKNameIdentifierImpl(name!!),
                JKInheritanceInfoImpl(extensionType, implTypes),
                classKind,
                typeParameterList?.toJK() ?: JKTypeParameterListImpl(),
                createClassBody(),
                modifiers(),
                visibility(),
                modality()
            ).also { jkClassImpl ->
                jkClassImpl.psi = this
                symbolProvider.provideUniverseSymbol(this, jkClassImpl)
            }
        }

        fun PsiClass.createClassBody() =
            JKClassBodyImpl(
                children.mapNotNull {
                    when (it) {
                        is PsiEnumConstant -> it.toJK()
                        is PsiClass -> it.toJK()
                        is PsiMethod -> it.toJK()
                        is PsiField -> it.toJK()
                        is PsiClassInitializer -> it.toJK()
                        else -> null
                    }
                }
            )

        fun PsiClassInitializer.toJK() =
            JKKtInitDeclarationImpl(body.toJK())

        fun PsiEnumConstant.toJK(): JKEnumConstant =
            JKEnumConstantImpl(
                JKNameIdentifierImpl(name),
                with(expressionTreeMapper) { argumentList.toJK() },
                initializingClass?.createClassBody() ?: JKEmptyClassBodyImpl(),
                JKTypeElementImpl(JKClassTypeImpl(symbolProvider.provideDirectSymbol(containingClass!!) as JKClassSymbol, emptyList()))
            )

        fun PsiMember.modality() =
            when {
                modifierList == null -> Modality.OPEN
                hasModifierProperty(PsiModifier.FINAL) -> Modality.FINAL
                hasModifierProperty(PsiModifier.ABSTRACT) -> Modality.ABSTRACT
                else -> Modality.OPEN
            }

        fun PsiMember.visibility() =
            when {
                modifierList == null -> Visibility.PACKAGE_PRIVATE
                hasModifierProperty(PsiModifier.PACKAGE_LOCAL) -> Visibility.PACKAGE_PRIVATE
                hasModifierProperty(PsiModifier.PRIVATE) -> Visibility.PRIVATE
                hasModifierProperty(PsiModifier.PROTECTED) -> Visibility.PROTECTED
                hasModifierProperty(PsiModifier.PUBLIC) -> Visibility.PUBLIC
                else -> Visibility.PACKAGE_PRIVATE
            }

        fun PsiMember.modifiers() =
            if (modifierList == null) emptyList()
            else
                PsiModifier.MODIFIERS
                    .filter { this.modifierList!!.hasExplicitModifier(it) }
                    .mapNotNull {
                        when (it) {
                            PsiModifier.NATIVE -> ExtraModifier.NATIVE
                            PsiModifier.STATIC -> ExtraModifier.STATIC
                            PsiModifier.STRICTFP -> ExtraModifier.STRICTFP
                            PsiModifier.SYNCHRONIZED -> ExtraModifier.SYNCHRONIZED
                            PsiModifier.TRANSIENT -> ExtraModifier.TRANSIENT
                            PsiModifier.VOLATILE -> ExtraModifier.VOLATILE

                            PsiModifier.PROTECTED -> null
                            PsiModifier.PUBLIC -> null
                            PsiModifier.PRIVATE -> null
                            PsiModifier.FINAL -> null
                            PsiModifier.ABSTRACT -> null

                            else -> TODO("Not yet supported")
                        }
                    }


        fun PsiField.toJK(): JKJavaField {
            return JKJavaFieldImpl(
                with(expressionTreeMapper) { typeElement?.toJK() } ?: JKTypeElementImpl(JKNoTypeImpl),
                JKNameIdentifierImpl(name),
                with(expressionTreeMapper) { initializer.toJK() },
                JKAnnotationListImpl(annotations.map { it.toJK() }),
                modifiers(),
                visibility(),
                modality(),
                Mutability.UNKNOWN
            ).also {
                symbolProvider.provideUniverseSymbol(this, it)
                it.psi = this
            }
        }

        fun PsiAnnotation.toJK(): JKAnnotation =
            JKAnnotationImpl(
                symbolProvider.provideSymbol(nameReferenceElement!!),
                with(expressionTreeMapper) {
                    JKExpressionListImpl(parameterList.attributes.map { (it.value as? PsiExpression).toJK() })
                }
            )


        fun PsiMethod.toJK(): JKJavaMethod {
            return JKJavaMethodImpl(
                with(expressionTreeMapper) {
                    returnTypeElement?.toJK()
                            ?: JKTypeElementImpl(JKJavaVoidType).takeIf { isConstructor }
                            ?: TODO()
                },
                JKNameIdentifierImpl(name),
                parameterList.parameters.map { it.toJK() },
                body?.toJK() ?: JKBodyStub,
                typeParameterList?.toJK() ?: JKTypeParameterListImpl(),
                JKAnnotationListImpl(),//TODO get real annotations
                throwsList.referencedTypes.map { JKTypeElementImpl(it.toJK(symbolProvider)) },
                modifiers(),
                visibility(),
                modality()
            ).also {
                it.psi = this
                symbolProvider.provideUniverseSymbol(this, it)
            }
        }

        fun PsiMember.toJK(): JKDeclaration? = when (this) {
            is PsiEnumConstant -> this.toJK()
            is PsiField -> this.toJK()
            is PsiMethod -> this.toJK()
            else -> null
        }

        fun PsiParameter.toJK(): JKParameter {
            return JKParameterImpl(
                with(expressionTreeMapper) { typeElement?.toJK() } ?: TODO(),
                JKNameIdentifierImpl(name!!)
            ).also {
                symbolProvider.provideUniverseSymbol(this, it)
                it.psi = this
            }
        }

        fun PsiCodeBlock.toJK(): JKBlock {
            return JKBlockImpl(statements.map { it.toJK() })
        }

        fun Array<out PsiElement>.toJK(): List<JKDeclaration> {
            return this.map {
                if (it is PsiLocalVariable) {
                    it.toJK()
                } else TODO()
            }
        }

        fun PsiLocalVariable.toJK(): JKLocalVariable =
            JKLocalVariableImpl(
                with(expressionTreeMapper) { typeElement.toJK() },
                JKNameIdentifierImpl(this.name ?: TODO()),
                with(expressionTreeMapper) { initializer.toJK() },
                if (hasModifierProperty(PsiModifier.FINAL)) Mutability.IMMUTABLE else Mutability.UNKNOWN
            ).also { i ->
                symbolProvider.provideUniverseSymbol(this, i)
                i.psi = this
            }

        fun PsiStatement?.toJK(): JKStatement {
            return when (this) {
                null -> JKExpressionStatementImpl(JKStubExpressionImpl())
                is PsiExpressionStatement -> JKExpressionStatementImpl(with(expressionTreeMapper) { expression.toJK() })
                is PsiReturnStatement -> JKReturnStatementImpl(with(expressionTreeMapper) { returnValue.toJK() })
                is PsiDeclarationStatement -> JKDeclarationStatementImpl(declaredElements.toJK())
                is PsiAssertStatement ->
                    JKJavaAssertStatementImpl(
                        with(expressionTreeMapper) { assertCondition.toJK() },
                        with(expressionTreeMapper) { assertDescription?.toJK() } ?: JKStubExpressionImpl())
                is PsiIfStatement ->
                    if (elseElement == null)
                        JKIfStatementImpl(with(expressionTreeMapper) { condition.toJK() }, thenBranch.toJK())
                    else
                        JKIfElseStatementImpl(with(expressionTreeMapper) { condition.toJK() }, thenBranch.toJK(), elseBranch.toJK())

                is PsiForStatement -> JKJavaForLoopStatementImpl(
                    initialization.toJK(),
                    with(expressionTreeMapper) { condition.toJK() },
                    when (update) {
                        is PsiExpressionListStatement ->
                            (update as PsiExpressionListStatement).expressionList.expressions.map {
                                JKExpressionStatementImpl(with(expressionTreeMapper) { it.toJK() })
                            }
                        else -> listOf(update.toJK())
                    },
                    body.toJK()
                )
                is PsiForeachStatement ->
                    JKForInStatementImpl(
                        iterationParameter.toJK(),
                        with(expressionTreeMapper) { iteratedValue?.toJK() ?: JKStubExpressionImpl() },
                        body?.toJK() ?: blockStatement()
                    )
                is PsiBlockStatement -> JKBlockStatementImpl(codeBlock.toJK())
                is PsiWhileStatement -> JKWhileStatementImpl(with(expressionTreeMapper) { condition.toJK() }, body.toJK())
                is PsiDoWhileStatement -> JKDoWhileStatementImpl(body.toJK(), with(expressionTreeMapper) { condition.toJK() })

                is PsiSwitchStatement -> {
                    val cases = mutableListOf<JKJavaSwitchCase>()
                    for (statement in body?.statements.orEmpty()) {
                        when (statement) {
                            is PsiSwitchLabelStatement ->
                                cases += if (statement.isDefaultCase)
                                    JKJavaDefaultSwitchCaseImpl(emptyList())
                                else
                                    JKJavaLabelSwitchCaseImpl(
                                        with(expressionTreeMapper) { statement.caseValue.toJK() },
                                        emptyList()
                                    )
                            else ->
                                //TODO Handle case then there is no last case
                                cases.lastOrNull()?.also { it.statements = it.statements + statement.toJK() }
                        }
                    }
                    JKJavaSwitchStatementImpl(with(expressionTreeMapper) { expression.toJK() }, cases)
                }
                is PsiBreakStatement -> {
                    if (labelIdentifier != null)
                        JKBreakWithLabelStatementImpl(JKNameIdentifierImpl(labelIdentifier!!.text))
                    else
                        JKBreakStatementImpl()
                }
                is PsiContinueStatement -> {
                    val label = labelIdentifier?.let {
                        JKLabelTextImpl(JKNameIdentifierImpl(it.text))
                    } ?: JKLabelEmptyImpl()
                    JKContinueStatementImpl(label)
                }
                is PsiLabeledStatement -> {
                    val (labels, statement) = collectLabels()
                    JKLabeledStatementImpl(statement.toJK(), labels.map { JKNameIdentifierImpl(it.text) })
                }
                is PsiEmptyStatement -> JKEmptyStatementImpl()
                is PsiThrowStatement ->
                    JKJavaThrowStatementImpl(with(expressionTreeMapper) { exception.toJK() })
                is PsiTryStatement ->
                    JKJavaTryStatementImpl(
                        resourceList?.toList()?.map { (it as PsiLocalVariable).toJK() }.orEmpty(),
                        tryBlock?.toJK() ?: JKBodyStub,
                        finallyBlock?.toJK() ?: JKBodyStub,
                        catchSections.map { it.toJK() }
                    )
                else -> TODO("for ${this::class}")
            }.also {
                if (this != null) (it as PsiOwner).psi = this
            }
        }

        fun PsiCatchSection.toJK(): JKJavaTryCatchSection =
            JKJavaTryCatchSectionImpl(parameter?.toJK()!!, catchBlock?.toJK() ?: JKBodyStub)
                .also { it.psi = this }
    }

    //TODO better way than generateSequence.last??
    fun PsiLabeledStatement.collectLabels(): Pair<List<PsiIdentifier>, PsiStatement> =
        generateSequence(emptyList<PsiIdentifier>() to this as PsiStatement) { (labels, statement) ->
            if (statement !is PsiLabeledStatementImpl) return@generateSequence null
            (labels + statement.labelIdentifier) to statement.statement!!
        }.last()


    fun buildTree(psi: PsiElement): JKTreeElement? =
        when (psi) {
            is PsiJavaFile -> psi.toJK()
            else -> error("Cannot convert non-java file")
        }

}

