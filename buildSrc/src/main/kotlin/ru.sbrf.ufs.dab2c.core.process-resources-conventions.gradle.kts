import org.apache.tools.ant.filters.ReplaceTokens

tasks.withType<ProcessResources> {
    val props = project.properties
        .entries
        .filter { it.value != null }
        .associate { it.key to it.value.toString() }

    filesMatching(listOf("**/*")) {
        filter(
            ReplaceTokens::class,
            "tokens" to props
        )

        props.forEach { (key, value) ->
            if (path.contains("@${key}@")) {
                path = path.replace("@${key}@", value)
            }
        }
    }
}