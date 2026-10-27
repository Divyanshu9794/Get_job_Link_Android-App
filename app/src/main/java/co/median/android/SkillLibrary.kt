package co.median.android

object SkillLibrary {
    val categories: Map<String, List<String>> = mapOf(
        "Programming Languages" to listOf("C", "C++", "Java", "JavaScript", "TypeScript", "Python", "Kotlin", "C#", "Go", "Rust", "PHP", "Ruby", "Swift"),
        "Frontend" to listOf("HTML", "CSS", "React.js", "Next.js", "Angular", "Vue.js", "Tailwind CSS", "Bootstrap"),
        "Backend" to listOf("Node.js", "Express.js", "Spring Boot", ".NET", "Django", "Flask", "REST API", "GraphQL"),
        "Databases" to listOf("SQL", "MySQL", "PostgreSQL", "MongoDB", "SQL Server", "Firebase", "Redis"),
        "Cloud / DevOps" to listOf("Microsoft Azure", "AWS", "Google Cloud", "Docker", "Kubernetes", "Git", "GitHub", "CI/CD"),
        "Microsoft / Power Platform" to listOf("Power Apps", "Power Automate", "Power Platform", "Azure Logic Apps", "SharePoint"),
        "Tools / Technologies" to listOf("Postman", "Figma", "Linux", "GitHub Actions", "Firebase", "Android", "REST APIs")
    )

    val allSkills: List<String>
        get() = categories.values.flatten().distinct().sorted()

    fun normalize(skill: String): String {
        val trimmed = skill.trim()
        val match = allSkills.firstOrNull { it.equals(trimmed, ignoreCase = true) }
        return match ?: trimmed
    }
}
