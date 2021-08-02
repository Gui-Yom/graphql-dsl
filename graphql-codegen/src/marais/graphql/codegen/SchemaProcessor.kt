package marais.graphql.codegen

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import kotlin.io.path.Path

@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SupportedAnnotationTypes("marais.graphql.codegen.GraphQLSchemaCodegen")
@SupportedOptions("kapt.kotlin.generated")
class SchemaProcessor : AbstractProcessor() {

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {

        val codegenAnnotation = annotations.singleOrNull()
        if (codegenAnnotation == null || !codegenAnnotation.qualifiedName.contentEquals("marais.graphql.codegen.GraphQLSchemaCodegen"))
            return false

        val annotatedElements = roundEnv.getElementsAnnotatedWith(GraphQLSchemaCodegen::class.java)
        if (annotatedElements.isEmpty()) return false

        println("Found annotated elements : $annotatedElements")

        for (e in annotatedElements) {
            println("${e.simpleName}: ${e.kind.name}")
        }

        FileSpec.get(
            "", TypeSpec.objectBuilder("GeneratedSchema")
                .addProperty(
                    PropertySpec.builder("logs", String::class, KModifier.FINAL)
                        .initializer("%S", annotatedElements.toString())
                        .build()
                )
                .build()
        ).writeTo(Path(processingEnv.options["kapt.kotlin.generated"]!!, "GeneratedSchema.kt"))

        return true
    }

    private fun println(str: String) {
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, str)
    }
}
