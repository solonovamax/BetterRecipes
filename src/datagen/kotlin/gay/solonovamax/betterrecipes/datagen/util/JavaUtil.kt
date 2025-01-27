package gay.solonovamax.betterrecipes.datagen.util

import java.util.Optional

fun <T : Any> optionalOf(): Optional<T> = Optional.empty<T>()

fun <T : Any> optionalOf(value: T?): Optional<T> = Optional.ofNullable(value)
