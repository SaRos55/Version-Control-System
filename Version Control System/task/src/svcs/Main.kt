package svcs

import java.io.File
import java.security.MessageDigest

val configFile = File("vcs\\config.txt")
val index = File("vcs\\index.txt")
val logFile = File("vcs\\log.txt")

fun main(args: Array<String>) {
    val vcs = File("vcs")
    if (!vcs.exists()) vcs.mkdir()
    val commits = File("vcs\\commits")
    if (!commits.exists()) commits.mkdir()

    if (!configFile.exists()) configFile.createNewFile()
    if (!index.exists()) index.createNewFile()
    if (!logFile.exists()) logFile.createNewFile()

    val command = if (args.isNotEmpty()) args[0] else ""
    val value = if (args.size > 1) args[1] else ""
    if (args.isEmpty()) println(messageHelp()) else println(
        when (command) {
            "config" -> config(value)
            "add" -> add(value)
            "log" -> log()
            "commit" -> commit(value)
            "checkout" -> checkout(value)
            "--help" -> messageHelp()
            else -> "'${command}' is not a SVCS command."
        }
    )
}

fun checkout(idCommit: String): String {
    if (idCommit == "") return "Commit id was not passed."
    val listCommits = File("vcs\\commits").list()
    if (listCommits != null) {
        if (idCommit in listCommits) {
            val listFiles = File("vcs\\commits\\$idCommit").list()
            if (listFiles != null) {
                for (file in listFiles) {
                    File("vcs\\commits\\$idCommit\\$file").copyTo(File(file), true)
                }
            }
            return "Switched to commit $idCommit."
        }
    }
    return "Commit does not exist."
}

fun commit(comment: String): String {
    if (comment == "") return "Message was not passed."
    else {
        val indexFiles = index.readLines().map { it to File(it).readText() }
        if (indexFiles.isNotEmpty()) {
            val lastCommit = if (logFile.readLines().isNotEmpty()) {
                logFile.readLines().first().substringAfter("commit ")
            } else ""
            val newCommit = toSHA1(indexFiles.hashCode())
            if (lastCommit == newCommit) return "Nothing to commit."
            File("vcs\\commits\\$newCommit").mkdir()
            for (file in index.readLines()) {
                File(file).copyTo(File("vcs\\commits\\$newCommit\\$file"))
            }
            val newLogItem = "commit $newCommit\nAuthor: ${configFile.readText()}\n$comment\n"
            logFile.writeText(newLogItem + "\n" + logFile.readText())
            return "Changes are committed."
        }
    }
    return "Nothing to commit."
}

fun log(): String = logFile.readText().ifEmpty { "No commits yet." }

fun config(userName: String): String {
    if (userName != "") configFile.writeText(userName)
    else if (configFile.readText() == "") return "Please, tell me who you are."
    return "The username is ${configFile.readText()}."
}

fun add(fileName: String): String {
    var trackedFiles = index.readLines()
    if (fileName == "") {
        return if (trackedFiles.isEmpty()) "Add a file to the index."
        else printFiles(trackedFiles)
    }
    val file = File(fileName)
    if (file.exists()) {
        if (!trackedFiles.contains(fileName)) {
            index.appendText("$fileName\n")
            return "The file '$fileName' is tracked."
        }
        trackedFiles = index.readLines()
        return printFiles(trackedFiles)
    }
    return "Can't find '$fileName'."
}

private fun printFiles(trackedFiles: List<String>): String {
    var result = "Tracked files:\n"
    for (f in trackedFiles) {
        result += f + "\n"
    }
    return result
}

fun messageHelp() =
    """
        These are SVCS commands:
        config     Get and set a username.
        add        Add a file to the index.
        log        Show commit logs.
        commit     Save changes.
        checkout   Restore a file.
    """.trimIndent()

fun toSHA1(hash: Int): String {
    return MessageDigest
        .getInstance("SHA-1")
        .digest(hash.toString().toByteArray())
        .joinToString(separator = "", transform = { "%02x".format(it) })
}