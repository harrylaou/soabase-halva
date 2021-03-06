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
package io.soabase.halva.processor.caseclass;

import io.soabase.halva.processor.AnnotationReader;
import javax.lang.model.element.TypeElement;
import java.util.List;

class CaseClassSpec
{
    private final TypeElement element;
    private final AnnotationReader annotationReader;
    private final List<CaseClassItem> items;

    CaseClassSpec(TypeElement element, AnnotationReader annotationReader, List<CaseClassItem> items)
    {
        this.element = element;
        this.annotationReader = annotationReader;
        this.items = items;
    }

    AnnotationReader getAnnotationReader()
    {
        return annotationReader;
    }

    TypeElement getAnnotatedElement()
    {
        return element;
    }

    List<CaseClassItem> getItems()
    {
        return items;
    }
}
