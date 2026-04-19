rootProject.name = "ai-parsing-web"

fun includeIfExists(projectName: String) {
    val dir = file(projectName)
    if (dir.isDirectory) {
        include(projectName)
    }
}

includeIfExists("internal-api")
includeIfExists("extraction-results-microservice")
includeIfExists("users-info-microservice")
includeIfExists("api-gateway-microservice")
includeIfExists("parsing-task-orchestrator-microservice")
