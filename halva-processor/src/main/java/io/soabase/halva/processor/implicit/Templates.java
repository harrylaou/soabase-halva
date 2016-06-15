/**
 * Copyright 2016 Jordan Zimmerman
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
package io.soabase.halva.processor.implicit;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import io.soabase.halva.any.Any;
import io.soabase.halva.implicit.Implicit;
import io.soabase.halva.tuple.Pair;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.soabase.halva.comprehension.For.forComp;
import static io.soabase.halva.tuple.Tuple.Pair;

class Templates
{
    private final ProcessingEnvironment processingEnv;

    Templates(ProcessingEnvironment processingEnv)
    {
        this.processingEnv = processingEnv;
    }

    void addImplicitInterface(TypeSpec.Builder builder, TypeMirror implicitInterface, List<ContextSpec> specs)
    {
        Pair<ContextSpec, Element> implicit = findImplicit(implicitInterface, specs);
        if ( implicit == null )
        {
            return;
        }

        builder.addSuperinterface(ClassName.get(implicitInterface));
        processingEnv.getTypeUtils().asElement(implicitInterface).getEnclosedElements().forEach(implicitElement -> {
            if ( implicitElement.getKind() == ElementKind.METHOD )
            {
                ExecutableElement implicitMethod = (ExecutableElement)implicitElement;

                CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
                if ( implicitMethod.getReturnType().getKind() != TypeKind.VOID )
                {
                    codeBlockBuilder.add("return ");
                }

                applyImplicitParameter(codeBlockBuilder, implicit, specs);
                codeBlockBuilder.add(".$L(", implicitMethod.getSimpleName());
                boolean first = true;
                for ( VariableElement parameter : implicitMethod.getParameters() )
                {
                    if ( first )
                    {
                        first = false;
                    }
                    else
                    {
                        codeBlockBuilder.add(", ");
                    }
                    codeBlockBuilder.add(parameter.getSimpleName().toString());
                }
                codeBlockBuilder.addStatement(")");

                MethodSpec methodSpec = MethodSpec.overriding(implicitMethod).addCode(codeBlockBuilder.build()).build();
                builder.addMethod(methodSpec);
            }
        });
    }

    void addItem(TypeSpec.Builder builder, ExecutableElement element, List<ContextSpec> specs)
    {
        MethodSpec.Builder methodSpecBuilder = (element.getKind() == ElementKind.CONSTRUCTOR) ? MethodSpec.constructorBuilder() : MethodSpec.methodBuilder(element.getSimpleName().toString());
        methodSpecBuilder.addModifiers(element.getModifiers());
        if ( element.getReturnType().getKind() != TypeKind.VOID )
        {
            methodSpecBuilder.returns(ClassName.get(element.getReturnType()));
        }

        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
        if ( element.getKind() == ElementKind.CONSTRUCTOR )
        {
            codeBlockBuilder.add("super(");
        }
        else if ( element.getReturnType().getKind() == TypeKind.VOID )
        {
            codeBlockBuilder.add("super.$L(", element.getSimpleName());
        }
        else
        {
            codeBlockBuilder.add("return super.$L(", element.getSimpleName());
        }
        AtomicBoolean isFirst = new AtomicBoolean(false);
        element.getParameters().forEach(parameter -> {
            if ( !isFirst.compareAndSet(false, true) )
            {
                codeBlockBuilder.add(", ");
            }
            if ( parameter.getAnnotation(Implicit.class) != null )
            {
                Pair<ContextSpec, Element> implicit = findImplicit(parameter.asType(), specs);
                applyImplicitParameter(codeBlockBuilder, implicit, specs);
            }
            else
            {
                ParameterSpec.Builder parameterSpec = ParameterSpec.builder(ClassName.get(parameter.asType()), parameter.getSimpleName().toString(), parameter.getModifiers().toArray(new javax.lang.model.element.Modifier[parameter.getModifiers().size()]));
                methodSpecBuilder.addParameter(parameterSpec.build());
                codeBlockBuilder.add(parameter.getSimpleName().toString());
            }
        });

        methodSpecBuilder.addCode(codeBlockBuilder.add(");\n").build());
        builder.addMethod(methodSpecBuilder.build());
    }

    private void applyImplicitParameter(CodeBlock.Builder builder, Pair<ContextSpec, Element> implicit, List<ContextSpec> specs)
    {
        if ( implicit == null )
        {
            builder.add("null");
        }
        else
        {
            TypeElement annotatedElement = implicit._1.getAnnotatedElement();
            Element element = implicit._2;
            if ( element.getKind() == ElementKind.FIELD )
            {
                builder.add("$T.$L", annotatedElement, element.getSimpleName());
            }
            else
            {
                ExecutableElement method = (ExecutableElement)element;
                builder.add("$T.$L(", annotatedElement, method.getSimpleName());
                AtomicBoolean isFirst = new AtomicBoolean(false);
                method.getParameters().forEach(methodParameter -> {
                    if ( !isFirst.compareAndSet(false, true) )
                    {
                        builder.add(", ");
                    }
                    applyImplicitParameter(builder, findImplicit(methodParameter.asType(), specs), specs);
                    builder.add(")");
                });
            }
        }
    }

    private Pair<ContextSpec, Element> findImplicit(TypeMirror implicitType, List<ContextSpec> specs)
    {
        Any<ContextSpec> spec = Any.define(ContextSpec.class);
        Any<ContextItem> item = Any.define(ContextItem.class);
        List<Pair<ContextSpec, Element>> matchingSpecs = forComp(spec, specs)
            .forComp(item, () -> spec.val().getItems())
            .filter(() -> {
                Element element = item.val().getElement();
                if ( element.getKind() == ElementKind.FIELD )
                {
                    return processingEnv.getTypeUtils().isAssignable(element.asType(), implicitType);
                }
                return processingEnv.getTypeUtils().isAssignable(((ExecutableElement)element).getReturnType(), implicitType);
            })
            .yield(() -> Pair(spec.val(), item.val().getElement()));
        if ( matchingSpecs.size() == 1 )
        {
            return matchingSpecs.get(0);
        }

        String message = (matchingSpecs.size() == 0) ? "No matches found for implicit for " : "Multiple matches found for implicit for ";
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message + implicitType, processingEnv.getTypeUtils().asElement(implicitType));
        return null;
    }
}
