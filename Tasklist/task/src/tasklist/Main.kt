package tasklist
import kotlinx.datetime.*
import kotlin.system.exitProcess
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

// private enum class list for demanding input
enum class InputStrings(val inputAction: String, val acceptedReg: Regex, val errorPrinted: Boolean, val errorMessage: String, val type: String) {
    MAIN_MENU("Input an action (add, print, edit, delete, end):", Regex("add|print|edit|delete|end"), true, "The input action is invalid", "general"),
    PRIORITY("Input the task priority (C, H, N, L):", Regex("[nchl]"), false, "", "general"),
    DATE("Input the date (yyyy-mm-dd):", Regex("^(19|20)\\d\\d(-)(0?[1-9]|1[012])\\2(0?[1-9]|[12][0-9]|3[01])\$"), true, "The input date is invalid", "dateTime"),
    TIME("Input the time (hh:mm):", Regex("^([0-1]?[0-9]|2[0-3]):[0-5]?[0-9]\$"), true, "The input time is invalid", "dateTime"),
    TASK("Input a new task (enter a blank line to end):", Regex("(.*?)"), true, "The task is blank", "task"),
    EDIT("Input a field to edit (priority, date, time, task):", Regex("priority|date|time|task"), true, "Invalid field", "general"),
}

// Task Manager Class
class TaskManager {
    // List for tasks to be stored
    private var taskDataFile = File("tasklist.json")
    private val taskList = mutableListOf<Task?>()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(List::class.java, Task::class.java)
    private val taskListAdapter = moshi.adapter<List<Task?>>(type)


    // Read data file and load data if not empty
    fun readDataFile() {
        taskDataFile.createNewFile()
        val fileContent = taskDataFile.readText()
        if (fileContent.isNotEmpty()) {
            val list = taskListAdapter.fromJson(fileContent)
            list!!.forEach { taskList.add(it) }
        }
    }
    //  - Main Menu
    fun mainMenu() {
        while(true) {
            when (demandInputs(InputStrings.MAIN_MENU)) {
                "add" -> addTask()
                "print" -> printTasks()
                "edit" -> editTask()
                "delete" -> deleteTask()
                "end" -> endProgram()
            }
        }
    }
    //  - Add Task
    private fun addTask() {
        val priority = demandInputs(InputStrings.PRIORITY)
        val date = demandInputs(InputStrings.DATE)
        val time = demandInputs(InputStrings.TIME)
        val task = demandInputs(InputStrings.TASK)
        if (task != "") {
            taskList.add(Task(priority, date, time, task))
        }
    }
    //  - Print Tasks using Print table class and its column and lines sub classes
    private fun printTasks() {
        if (taskList.size == 0) return println("No tasks have been input")
        // list to store task content lines
        val contentLines = mutableListOf<PrintTable.Lines>()
        // horizontal divider line
        val horizontalDividerLine = PrintTable.Lines(
            PrintTable.Column("----",'+', false ),
            PrintTable.Column("-".repeat(12), '+', false),
            PrintTable.Column("-".repeat(7), '+', false),
            PrintTable.Column("-".repeat(3), '+', false),
            PrintTable.Column("-".repeat(3), '+', false),
            PrintTable.Column("-".repeat(44), '+', true)
        )
        // table header line
        val headerLine = PrintTable.Lines(
            PrintTable.Column(" N  ",'|', false ),
            PrintTable.Column("    Date    ", '|', false),
            PrintTable.Column(" Time  ", '|', false),
            PrintTable.Column(" P ", '|', false),
            PrintTable.Column(" D ", '|', false),
            PrintTable.Column(" ".repeat(19) + "Task" + " ".repeat(21), '|', true)
        )
        // loop through tasks to create each line of the table
        for (task in taskList) {
            // store for list of the task strings formatter into lines of 44 characters
            val taskStrings = task?.getFormattedTask()
            for(string in taskStrings!!) {
                // for first line of each new task
                if (taskStrings.indexOf(string) == 0) {
                    contentLines.add(PrintTable.Lines(
                        PrintTable.Column(if (taskList.indexOf(task) < 9) " ${taskList.indexOf(task) + 1}  " else " ${taskList.indexOf(task) + 1} ",'|', false ),
                        PrintTable.Column(" ${task.date} ", '|', false),
                        PrintTable.Column(" ${task.time} ", '|', false),
                        PrintTable.Column(" ${task.getPriorityColorBlock()} ", '|', false),
                        PrintTable.Column(" ${task.getDueTagColorBlock()} ", '|', false),
                        PrintTable.Column(string, '|', true)
                        )
                    )
                } else {
                    // for all other lines with columns containing whitespace
                    contentLines.add(PrintTable.Lines(
                        PrintTable.Column("    ",'|', false ),
                        PrintTable.Column(" ".repeat(12), '|', false),
                        PrintTable.Column(" ".repeat(7), '|', false),
                        PrintTable.Column("   ", '|', false),
                        PrintTable.Column("   ", '|', false),
                        PrintTable.Column(string, '|', true)
                        )
                    )
                }
            }
            // add a divider line at the end of each task
            contentLines.add(horizontalDividerLine)
        }
        // print task table to console
        println(horizontalDividerLine.toString())
        println(headerLine.toString())
        println(horizontalDividerLine.toString())
        contentLines.forEach {
            println(it.toString())
        }
        println()
    }
    // - Edit Task
    private fun editTask() {
        printTasks()
        val taskNumber = demandTaskNumber()
        if (taskNumber == 0) return mainMenu()
        when (demandInputs(InputStrings.EDIT)) {
            "priority" -> return taskList[taskNumber - 1]!!.setTaskProperty("priority", demandInputs(InputStrings.PRIORITY))
            "date" -> return taskList[taskNumber - 1]!!.setTaskProperty("date", demandInputs(InputStrings.DATE))
            "time" -> return taskList[taskNumber - 1]!!.setTaskProperty("time", demandInputs(InputStrings.TIME))
            "task" -> return taskList[taskNumber - 1]!!.setTaskProperty("task", demandInputs(InputStrings.TASK))
        }
        println(taskList)
    }
    // - Delete Task
    private fun deleteTask() {
        printTasks()
        val taskNumber = demandTaskNumber()
        if (taskNumber == 0) return mainMenu()
        taskList.removeAt(taskNumber - 1)
        println("The task is deleted")
    }
    // Write data to file
    private fun writeDataFile() {
        if (taskList.isNotEmpty()) {
            taskDataFile.writeText(taskListAdapter.toJson(taskList))
        }
    }
    //  - End Program
    private fun endProgram() {
        writeDataFile()
        println("Tasklist exiting!")
        exitProcess(0)
    }
    //  - Demand Inputs
    private fun demandInputs(demandType: InputStrings): String {
        println(demandType.inputAction)
        return when (demandType.type) {
            "task" -> taskInput(demandType)
            "dateTime" -> dateTimeInput(demandType)
            else -> generalInput(demandType)
        }
    }
    // -- Task input helper function
    private fun taskInput(demandType: InputStrings): String {
        val lineList = mutableListOf<String>()
        while(true) {
            val line = readln().trim()
            if (lineList.isEmpty()) {
                if(line.isEmpty()) {
                    println(demandType.errorMessage)
                    return line
                } else {
                    lineList.add(line)
                }
            } else {
                if(line.isNotEmpty()) {
                    lineList.add(line)
                } else {
                    return lineList.joinToString("\n")
                }
            }
        }
    }
    // -- DateTime input help function
    private fun dateTimeInput(demandType: InputStrings): String {
        return try {
            val inputSplit = generalInput(demandType).split('-', ':')
            if (inputSplit.size == 3) {
                LocalDate(inputSplit[0].toInt(), inputSplit[1].toInt(), inputSplit[2].toInt()).toString()
            } else {
                LocalTime(inputSplit[0].toInt(), inputSplit[1].toInt()).toString()
            }
        } catch (e: Exception) {
            println(demandType.errorMessage)
            demandInputs(demandType)
        }
    }
    // -- General input helper function
    private fun generalInput(demandType: InputStrings): String {
        val input = readln().trim().lowercase()
        if (!input.matches(demandType.acceptedReg)) {
            return if (demandType.errorPrinted) {
                println(demandType.errorMessage)
                demandInputs(demandType)
            } else {
                demandInputs(demandType)
            }
        }
        return input
    }
    // -- Demand task number
    private fun demandTaskNumber(): Int {
        var taskToEdit: String
        if(taskList.size == 0) {
            return 0
        }
        while(true) {
            println("Input the task number (1-${this.taskList.size}):")
            try {
                taskToEdit = readln()
                if (taskToEdit.toInt() >= 1 && taskToEdit.toInt() <= this.taskList.size) {
                    break
                } else {
                    println("Invalid task number")
                }
            } catch (e: Exception) {
                println("Invalid task number")
                continue
            }
        }
        return taskToEdit.toInt()
    }
}

// Task Class
class Task(var priority: String, var date: String, var time: String, var task: String) {
    private val colorMap = mapOf<String, String>(
        "C" to "\u001B[101m \u001B[0m",
        "H" to "\u001B[103m \u001B[0m",
        "N" to "\u001B[102m \u001B[0m",
        "L" to "\u001B[104m \u001B[0m",
        "I" to "\u001B[102m \u001B[0m",
        "T" to "\u001B[103m \u001B[0m",
        "O" to "\u001B[101m \u001B[0m"
    )
    // Getters
    fun getDueTagColorBlock(): String? {
        return colorMap[getDueTag().uppercase()]
    }
    fun getPriorityColorBlock(): String? {
        return colorMap[priority.uppercase()]
    }
    fun getFormattedTask(): List<String> {
        return formattedTask()
    }
    // Split string by newlines and by 44 chars
    private fun formattedTask(): List<String> {
        return task.split("\n").map {it.chunked(44)}.flatten().map {
            "%-44s".format(it)
        }
    }
    // To reset task priority
    fun setTaskProperty(taskProperty: String, newProperty: String) {
        when(taskProperty) {
            "priority" -> this.priority = newProperty
            "date" -> this.date = newProperty
            "time" -> this.time = newProperty
            "task" -> this.task = newProperty
         }
        println("The task is changed")
    }
    // Get the Due tag for task printing
    private fun getDueTag(): String {
        val splitDate = date.split("-")
        val localDate = LocalDate(splitDate[0].toInt(), splitDate[1].toInt(), splitDate[2].toInt())
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+2")).date
        val numberOfDays = currentDate.daysUntil(localDate)
        return if (numberOfDays == 0) "T" else if (numberOfDays < 0) "O" else "I"
    }
}

// Print Table Class
class PrintTable {
    // sub class for a column
    class Column(private val content: String, private val edgeChar: Char, private val trailingEdge: Boolean) {
        override fun toString(): String { return "$edgeChar$content${if (trailingEdge) edgeChar else ""}" }
    }
    // sub class to create lines from all the columns
    data class Lines(private val num: Column, private val date: Column, private val time: Column, private val priority: Column, private val due: Column, private val task: Column) {
        override fun toString(): String { return "${num}${date}${time}${priority}${due}${task}" }
    }
}

// Main function
fun main() {
    val taskManager = TaskManager()
    taskManager.readDataFile()
    taskManager.mainMenu()
}


