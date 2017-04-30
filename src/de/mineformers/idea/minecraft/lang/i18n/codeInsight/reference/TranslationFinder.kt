package de.mineformers.idea.minecraft.lang.i18n.codeInsight.reference

import com.google.common.collect.ImmutableList
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import de.mineformers.idea.minecraft.lang.i18n.codeInsight.reference.identifiers.LiteralTranslationIdentifier
import de.mineformers.idea.minecraft.lang.i18n.codeInsight.reference.identifiers.ReferenceTranslationIdentifier
import de.mineformers.idea.minecraft.lang.i18n.codeInsight.reference.identifiers.TranslationIdentifier
import de.mineformers.idea.minecraft.lang.i18n.reference.I18nReference
import java.util.*

/**
 * TranslationFinder

 * @author PaleoCrafter
 */
object TranslationFinder {
    val translationFunctions = ImmutableList.of(
            TranslationFunction("net.minecraft.client.resources.I18n",
                    "format",
                    "Ljava.lang.String;[Ljava.lang.Object;",
                    0,
                    formatting = true),
            TranslationFunction("net.minecraft.util.text.translation.I18n",
                    "translateToLocal",
                    "Ljava.lang.String;",
                    0,
                    formatting = false),
            TranslationFunction("net.minecraft.util.text.translation.I18n",
                    "translateToLocalFormatted",
                    "Ljava.lang.String;[Ljava.lang.Object;",
                    0,
                    formatting = true),
            TranslationFunction("net.minecraft.util.text.TextComponentTranslation",
                    "TextComponentTranslation",
                    "Ljava.lang.String;[Ljava.lang.Object;",
                    0,
                    formatting = true,
                    foldParameters = true),
            TranslationFunction("net.minecraft.command.CommandException",
                    "CommandException",
                    "Ljava.lang.String;[Ljava.lang.Object;",
                    0,
                    formatting = true,
                    foldParameters = true),
            TranslationFunction("net.minecraft.block.Block",
                    "setUnlocalizedName",
                    "Ljava.lang.String;",
                    0,
                    formatting = false,
                    setter = true,
                    foldParameters = true,
                    prefix = "tile.",
                    suffix = ".name"),
            TranslationFunction("net.minecraft.item.Item",
                    "setUnlocalizedName",
                    "Ljava.lang.String;",
                    0,
                    formatting = false,
                    setter = true,
                    foldParameters = true,
                    prefix = "item.",
                    suffix = ".name"))

    private val identifiers = ImmutableList.of<TranslationIdentifier<*>>(
            LiteralTranslationIdentifier(),
            ReferenceTranslationIdentifier())

    fun find(element: PsiElement): Translation? {
        for (identifier in identifiers) {
            if (identifier.elementClass().isAssignableFrom(element.javaClass))
                return identifier.identifyUnsafe(element)
        }
        return null
    }

    fun fold(root: PsiElement): Array<FoldingDescriptor> {
        val descriptors = ArrayList<FoldingDescriptor>()
        for (identifier in identifiers) {
            val elements = PsiTreeUtil.findChildrenOfType(root, identifier.elementClass())
            for (element in elements) {
                val translation = identifier.identifyUnsafe(element)
                if (translation != null && translation.foldingElement != null) {
                    val range =
                            if (translation.foldingElement is PsiExpressionList)
                                TextRange(translation.foldingElement.textRange.startOffset + 1,
                                        translation.foldingElement.textRange.endOffset - 1)
                            else
                                TextRange(translation.foldingElement.textRange.startOffset,
                                        translation.foldingElement.textRange.endOffset)
                    descriptors.add(object : FoldingDescriptor(translation.foldingElement.node,
                            range,
                            FoldingGroup.newGroup("mc.i18n." + translation.key)) {
                        override fun getPlaceholderText(): String? {
                            return "\"" + translation.text + "\""
                        }
                    })
                }
            }
        }
        return descriptors.toArray(arrayOfNulls(descriptors.size))
    }

    fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        for (identifier in identifiers) {
            registrar.registerReferenceProvider(
                    PlatformPatterns.psiElement(identifier.elementClass()),
                    object : PsiReferenceProvider() {
                        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                            val result = identifier.identifyUnsafe(element)
                            if (result != null) {
                                val referenceElement = result.referenceElement ?: return emptyArray()
                                return arrayOf(I18nReference(referenceElement,
                                        TextRange(1, referenceElement.textLength - 1),
                                        false,
                                        result.key,
                                        result.varKey))
                            }
                            return emptyArray()
                        }
                    })
        }
    }
}
