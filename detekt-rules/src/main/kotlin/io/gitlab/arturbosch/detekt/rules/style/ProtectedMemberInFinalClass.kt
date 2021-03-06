package io.gitlab.arturbosch.detekt.rules.style

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.DetektVisitor
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration

class ProtectedMemberInFinalClass(config: Config = Config.empty) : Rule(config) {

	override val issue = Issue(javaClass.simpleName, Severity.Warning,
			"Member with protected visibility in final class is private. Consider using private or internal as modifier.",
			Debt.TEN_MINS)

	private val visitor = DeclarationVisitor()

	/**
	 * Only classes and companion objects can contain protected members.
	 */
	override fun visitClass(klass: KtClass) {
		if (hasModifiers(klass)) {
			klass.primaryConstructor?.accept(visitor)
			klass.getBody()?.declarations?.forEach { it.accept(visitor) }
			klass.companionObjects.forEach { it.accept(visitor) }
		}
		super.visitClassOrObject(klass)
	}

	private fun hasModifiers(klass: KtClass): Boolean {
		val isNotAbstract = !klass.hasModifier(KtTokens.ABSTRACT_KEYWORD)
		val isFinal = !klass.hasModifier(KtTokens.OPEN_KEYWORD)
		val isNotSealed = !klass.hasModifier(KtTokens.SEALED_KEYWORD)
		return isNotAbstract && isFinal && isNotSealed
	}

	internal inner class DeclarationVisitor : DetektVisitor() {

		override fun visitDeclaration(dcl: KtDeclaration) {
			val isProtected = dcl.hasModifier(KtTokens.PROTECTED_KEYWORD)
			val isNotOverridden = !dcl.hasModifier(KtTokens.OVERRIDE_KEYWORD)
			if (isProtected && isNotOverridden) {
				report(CodeSmell(issue, Entity.from(dcl)))
			}
		}
	}
}
