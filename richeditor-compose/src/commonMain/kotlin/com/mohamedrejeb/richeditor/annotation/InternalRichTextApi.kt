package com.mohamedrejeb.richeditor.annotation

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is internal API that may change frequently and without warning."
)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY
)
@Retention(AnnotationRetention.BINARY)
public annotation class InternalRichTextApi