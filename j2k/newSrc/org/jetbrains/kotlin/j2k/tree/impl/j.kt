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

package org.jetbrains.kotlin.j2k.tree.impl

import org.jetbrains.kotlin.j2k.tree.JKExpression
import org.jetbrains.kotlin.j2k.tree.JKJavaField
import org.jetbrains.kotlin.j2k.tree.JKJavaVisitor
import org.jetbrains.kotlin.j2k.tree.JKVisitor

class JKJavaFieldImpl(override val name: String,
                      override var initializer: JKExpression?) : JKJavaField, JKElementBase() {

    override fun <R, D> accept(visitor: JKVisitor<R, D>, data: D): R {
        return if (visitor is JKJavaVisitor) {
            visitor.visitJavaField(this, data)
        }
        else {
            visitor.visitDeclaration(this, data)
        }
    }

    override fun <D> acceptChildren(visitor: JKVisitor<Unit, D>, data: D)
            = initializer?.accept(visitor, data) ?: Unit
}