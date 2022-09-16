package calculator

import calculator.Calculator.calculator
import java.math.BigInteger

object Calculator {

    class InvalidIdentifier : Exception("Invalid identifier")
    class UnknownVariable : Exception("Unknown variable")
    class InvalidAssignment : Exception("Invalid assignment")
    class InvalidExpression : Exception("Invalid expression")

    private val assignMap = mutableMapOf<String, Any>()
    private val numberRegex = """\s*[+-]?\d+\s*""".toRegex()
    private val operatorRegex = "\\++|-+|[*/]".toRegex()

    // assigns a numeral value to an identifier (one or more letters) or throws exception. returns map of identifier to int value
    private fun assignValue(input: String): MutableMap<String, Any> {
        val inputLeftOfEquals = input.trim().split("\\s*=\\s*".toRegex())[0] // input on left side of equal sign
        val inputRightOfEquals = input.trim().split("\\s*=\\s*".toRegex())[1] // input on right side of equal sign
        val key: String = inputLeftOfEquals
        if (!key.matches("[a-zA-Z]+".toRegex())) {
            throw InvalidIdentifier()
        } else if (!input.trim().matches("\\s*\\w+\\s*[=]\\s*-?\\s*\\w+\\s*".toRegex())) {
            throw InvalidAssignment()
        }
        val value =
            if (assignMap.containsKey(inputRightOfEquals)) { // if trying to assign a letter that is already in map, then its value is assigned
                assignMap[inputRightOfEquals]!!
            } else if (!assignMap.containsKey(inputRightOfEquals) && inputRightOfEquals.matches("[a-zA-Z]+".toRegex())) {
                throw UnknownVariable() // if unknown letter is assigned
            } else {
                BigInteger(inputRightOfEquals.split("\\s+".toRegex()).toMutableList().joinToString("")) // whitespace removed and number is assigned to value (or exception is thrown and caught)
            }
        return mutableMapOf(key to value)
    }

    // regex for an expression with two or more operands
    private fun createRegex(inputToList: MutableList<String>): Regex {
        // input is split around operator signs
        val inputSplitByOperatorList: MutableList<String> = inputToList.joinToString("").split("[+-]+|[*/]".toRegex()).toMutableList()
        // base regex for expression with two operands, i.e ((2-----4))
        val patternList = mutableListOf("\\(*-?\\d+\\)*", "\\s*([-+\\/*]+)\\s*", "\\(*-?\\d+\\)*")
        // first position in index may be empty, so it is removed
        while (inputSplitByOperatorList[0] == "") { inputSplitByOperatorList.removeAt(0) }
        // if there are more than 2 numbers in expression regex elements will be added to list
        if (inputSplitByOperatorList.lastIndex > 1) {
            for (i in 2..inputSplitByOperatorList.lastIndex) {
                patternList.add("\\s*([-+\\/*]+)\\s*") // regex for operator is added
                patternList.add("\\(*-?\\d+\\)*") // regex for number is added
            }
        }
        return patternList.joinToString("")
            .toRegex() // regex is formed for expression with 2 or more numbers
    }

    // converts double minus to plus, removes redundant plus sign
    private fun correctOperator(inputToList: MutableList<String>, regex: Regex) {
        if (inputToList.joinToString(" ").matches(regex)) {
            for (i in 0..inputToList.lastIndex) { // corrects operator
                if (inputToList[i].matches("\\W+".toRegex())) { //ignores operands (numbers or letters)
                    if (inputToList[i].take(1) == "-") {
                        when {
                            (inputToList[i].length % 2 == 0) -> inputToList[i] = "+"
                            (inputToList[i].length % 2 == 1) -> inputToList[i] = "-"
                        }
                    } else {
                        inputToList[i] = inputToList[i].take(1)
                    }
                }
            }
        }
    }

    // converts expression to postfix format for correct order of operations
    private fun shuntingAlgorithm(inputToList: MutableList<String>): MutableList<String> {
        val outputQueue = mutableListOf<String>() //this will be expression in postfix format
        val operatorStack = mutableListOf<String>() //temporarily stores operators
        val operatorPrecedence = mapOf("*" to 2, "/" to 2, "+" to 1, "-" to 1, "(" to 0, ")" to 0)

        for (i in inputToList.indices) {
            if (inputToList[i].matches(numberRegex)) { //numbers are added to queue
                outputQueue.add(inputToList[i])
            }
            if (inputToList[i].matches(operatorRegex)) {
                // while there's an operator on the top of the stack with greater precedence it is popped from stack and added to queue
                while (operatorStack.isNotEmpty() && operatorPrecedence[inputToList[i]]!! <= operatorPrecedence[operatorStack.last()]!!) {
                    outputQueue.add(operatorStack.last())
                    operatorStack.removeLast()
                }
                operatorStack.add(inputToList[i]) // add operator to stack
            }
            if (inputToList[i] == "(") operatorStack.add(inputToList[i]) // left brackets pushed to stack
            if (inputToList[i] == ")") {
                // while there's not a left bracket at the top of the stack pop operators from the stack onto the output queue
                while (operatorStack.last() != "(") {
                    outputQueue.add(operatorStack.last())
                    operatorStack.removeLast()
                }
                operatorStack.removeLast() // pops the left bracket from the stack and discards it
            }
        }
        // while there are operators on the stack, pop them to the queue
        while (operatorStack.isNotEmpty()) {
            outputQueue.add(operatorStack.last())
            operatorStack.removeLast()
        }
        return outputQueue //expression is returned as list in postfix format
    }

    // calculates postfix expression
    private fun postfixCalculation(inputToList: MutableList<String>) {
        val postfixExpression = shuntingAlgorithm(inputToList) //conversion to postfix
        val stack = mutableListOf<String>()
        val operatorRegex = "[+\\-/*]".toRegex()
        for (i in postfixExpression.indices) {
            if (postfixExpression[i].matches(numberRegex)) {
                stack.add(postfixExpression[i]) // a number is added to stack
            } else if (postfixExpression[i].matches(operatorRegex)) {
                // when operator is reached the second to last and last numbers are calculated
                val resultOfLastTwoNumbersPopped = when (postfixExpression[i]) {
                    "+" -> BigInteger(stack[stack.lastIndex - 1]) + BigInteger(stack.last())
                    "-" -> BigInteger(stack[stack.lastIndex - 1]) - BigInteger(stack.last())
                    "*" -> BigInteger(stack[stack.lastIndex - 1]) * BigInteger(stack.last())
                    "/" -> BigInteger(stack[stack.lastIndex - 1]) / BigInteger(stack.last())
                    else -> throw InvalidExpression()
                }
                stack.removeLast() // removes the two calculated numbers from stack
                stack.removeLast()
                stack.add(resultOfLastTwoNumbersPopped.toString()) // result is added to stack
            }
        }
        println(BigInteger(stack.joinToString(""))) //the answer is the only remaining number in stack
    }

    // converts input into list without whitespace
    private fun inputToList(input: String) : MutableList<String> {
        var string = input.split("\\s+".toRegex()).toMutableList().joinToString("") //removes whitespace from input
        val list = mutableListOf<String>()
        val regex = """\w+|\*+|/+|-+|\++|\(|\)|=""".toRegex() // number, letter, operator or bracket
        // loop takes match and adds to new list and removes from string
        while (string.isNotEmpty()) {
            val match = regex.find(string)
            list.add(match!!.value)
            string = string.drop(match.value.length) //removes from beginning of string the amount of characters that were matched
        }
        //if input starts with negative number
        try {
            if (list[0] == "-" && list[1].matches("\\w+".toRegex())) {
                list.removeAt(0)
                list[0] = (list[0].toInt() * -1).toString()
            } else if (list[0] == "-" && list[1].matches("\\(".toRegex())) {
                list.removeAt(0)
                list.add(0, "*")
                list.add(0, "-1")
            }
        } catch (e: Exception) {
            println("Invalid expression")
        }

        try {
            //negative number or subtraction?
            for (i in list.indices) {
                // if a minus sign is after an operator then it is removed from list and the following number is turned negative by * -1
                if (list[i] == "-" && list[i - 1].matches("[+*/-]".toRegex())) {
                    list[i + 1] = (list[i + 1].toInt() * -1).toString()
                    list.removeAt(i)
                }
            }
        } catch (e: Exception) { } //exception happens because indices can be more than list size after element is removed

        return list
    }

    //compares input to regex and performs corresponding action
    private fun operation(input: String) {
        // input expression is split into operands and operators, mutable list of string
        val inputToList = inputToList(input)
        // regex to identify expression
        val regex = createRegex(inputToList)

        // replaces identifier with number if it doesn't match assigning regex, i.e. "n = 3"
        if (!input.trim().matches("[a-zA-Z]+\\w*\\s*=.*".toRegex())) {
            for (i in 0..inputToList.lastIndex) {
                if (assignMap.containsKey(inputToList[i])) {
                    inputToList[i] = assignMap[inputToList[i]].toString()
                }
            }
        }
        // replace operator, even number of minus operators is converted to plus operator
        correctOperator(inputToList, regex)

        // checks for unclosed brackets
        var leftBracket = 0
        var rightBracket = 0
        var bracketsAreClosed = false
        for (i in inputToList){
            if (i == "(") leftBracket++
            if (i == ")") rightBracket++
        }
        if (leftBracket == rightBracket) bracketsAreClosed = true

        // input is checked against regex and expression is converted to postfix. result of operations is displayed
        if (inputToList.joinToString("").matches(regex) && bracketsAreClosed) {
            try {
                postfixCalculation(inputToList)
            } catch (e: Exception) {
                println("Invalid expression")
            }
        } else if (input.matches(numberRegex)) { // displays number if single number was entered
            println(input.trim().toInt())
        } else if (assignMap.containsKey(input.trim())) { // displays value of single identifier
            println(assignMap[input.trim()])
        } else if (input.trim().matches("[a-zA-Z]+".toRegex())) { //if letter is not in map
            println("Unknown variable")
        } else if (input.trim().matches("[a-zA-Z]+\\w*\\s*=.*".toRegex())
        ) { // if input starts with letter and equal sign tries to assign value
            try {
                assignMap += assignValue(inputToList.joinToString(" "))
            } catch (e: InvalidIdentifier) {
                println("Invalid identifier")
            } catch (e: UnknownVariable) {
                println("Unknown variable")
            } catch (e: Exception) {
                println("Invalid assignment")
            }
        } else if (input.trim().matches("[a-zA-Z]+\\d+".toRegex())) { //i.e. n1 = 2
            println("Invalid identifier")
        } else {
            println("Invalid expression")
        }
    }

    // recursion is used for program to run until /exit command
    fun calculator() {
        val input = readln() // needs to be inside calculator method to request new input each cycle of recursion

        if (input == "/exit") {
            println("Bye!")
        } else if (input == "/help") {
            println(
                """
            This calculator can add, subtract, divide and multiply numbers in proper order of operation.
            Identifiers may be assigned numeral values.
            Example: "n = -3"
            Enter two or more operands (letter with assigned value or a number) with an operator (+, -, /, *) between the numbers. 
            Parts of expression can be put in brackets to change order of operation. Spaces do not affect result.
            Example: "9 * n+12 *(4 - 2)"
            Two or any even number of minus operators will result in addition ( " -- " equals " + " ).
            An odd number of minus operators will result in subtraction.
            Example: "2 -- 2" is the same as "2 + 2" and the result will be "4"
            Enter /help to display these instructions.
            Enter /exit to quit program.
        """.trimIndent()
            )
            calculator()
        } else if (input.isBlank()) {
            calculator()
        } else if (input[0] == '/') {
            println("Unknown command")
            calculator()
        } else {
            operation(input)
            calculator()
        }
    }
}

fun main() {
    calculator()
}

