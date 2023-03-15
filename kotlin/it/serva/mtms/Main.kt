package it.serva.mtms

import java.nio.file.Files
import java.nio.file.Paths
import java.util.ArrayList

class Main
/*
    * Min Tardiness Machine Scheduling (1 persona) Sono dati n jobs, J={1..n} di cui è noto il tempo di esecuzione "D"
    * sull’unica macchina disponibile e la scadenza per il loro completamento "delta i". Ogni job una volta iniziato va portato a
    * termine (no pre-emption). Si determini l’ordine di esecuzione dei jobs sulla macchina in modo tale che sia minima la somma
    * dei ritardi di tutti i job la cui lavorazione termina oltre la propria scadenza.
    */
fun main() {
    val iterations = getIterationsFromFile()
    iterations.forEachIndexed { index, jobs ->
        if (index>=30) {
            println("Begin iteration $index")
            var timeUsed = 0
            jobs.forEach { job -> timeUsed += job.tempoExec }
            println("total time used $timeUsed")
            //println()
            val op1 = orderByShortestProcessingTime(jobs)
            val op2 = orderByEarlierDueDate(jobs)
            val op3 = firstComeFirstServe(jobs)
            val t1 = firstComeFirstServe(op1)
            val t2 = firstComeFirstServe(op2)
            println("SPT: $t1")
            println("EDD: $t2")
            println("FCFS: $op3")
            val eddr = eddLateReorder(jobs)
            val t3 = firstComeFirstServe(eddr)
            println("### EDDR ### $t3")
            //println()
            if (t1!=0&&t2!=0&&t3!=0) {
                println("### reorderLS SPT ###")
                val rLS = reorderLS(op1, firstComeFirstServe(op1), op1.lastIndex)
                println(firstComeFirstServe(rLS))
                //println("### end SPT ###")
                //println()

                println("### reorderLS EDD ###")
                val rLS2 = reorderLS(op2, firstComeFirstServe(op2), op2.lastIndex)
                println(firstComeFirstServe(rLS2))
                //println("### end EDD ###")

                println("### reorderLS fcfs ###")
                val rLS4 = reorderLS(jobs, firstComeFirstServe(jobs), jobs.lastIndex)
                println(firstComeFirstServe(rLS4))
                //println("### end fcfs ###")
                //println()
                //println()

                println("### reorderLS EDDR ###")
                val rLS3 = reorderLS(eddr, firstComeFirstServe(eddr), eddr.lastIndex)
                println(firstComeFirstServe(rLS3))
                //println("### end EDDR ###")
                //println()

                println("### single job move SPT ###")
                val rLS5 = singleJobMove(op1, firstComeFirstServe(op1), op1.lastIndex)
                println(firstComeFirstServe(rLS5))
                //println("### end EDD ###")
                //println()

                println("### single job move EDD ###")
                val rLS6 = singleJobMove(op2, firstComeFirstServe(op2), op2.lastIndex)
                println(firstComeFirstServe(rLS6))
                //println("### end EDD ###")
                //println()

                println("### single job move FCFS ###")
                val rLS7 = singleJobMove(jobs, firstComeFirstServe(jobs), jobs.lastIndex)
                println(firstComeFirstServe(rLS7))
                //println("### end FCFS ###")
                //println()

                println("### single job move EDDR ###")
                val rLS8 = singleJobMove(eddr, firstComeFirstServe(eddr), eddr.lastIndex)
                println(firstComeFirstServe(rLS8))
                //println("### end EDD ###")
                //println()

                /*println("BRUTE FORCE")
                brute(jobs, minOf(firstComeFirstServe(rLS),firstComeFirstServe(rLS2),firstComeFirstServe(rLS3)))*/

            }
            println("-----------------------------")
        }
    }
}

/*fun brute(toOrder: ArrayList<Job>, upperBound: Int){
    var total = upperBound+1
    for (i in 0..10000) {
        val order = ArrayList(toOrder)
        order.shuffle()
        val t = firstComeFirstServe(order)
        if (t<total) {
            total = t
            println(total)
            firstComeFirstServe(order, true)
            order.forEachIndexed { ind, x -> println("$ind - durata ${x.tempoExec} - scade ${x.scadenza}") }
        }
        if (t==0)
            break
    }
}
*/

fun singleJobMove(toOrder:ArrayList<Job>, upperBound:Int, actualPosition:Int, reorderLS: Boolean = true): ArrayList<Job> {
    val reordered:ArrayList<Job> = if (reorderLS)
        reorderLS(toOrder, upperBound, toOrder.lastIndex)
    else ArrayList(toOrder)
    var tmp = ArrayList(reordered)
    val toReposition = tmp[actualPosition]
    var newCost = firstComeFirstServe(tmp)
    var found: ArrayList<Job> = arrayListOf()
    tmp.remove(toReposition)
    for (i in 0..reordered.lastIndex) {
        tmp.add(i, toReposition)
        val actualCost = firstComeFirstServe(tmp)
        if (actualCost<newCost){
            found = tmp
            newCost = actualCost
        }
        tmp = ArrayList(reordered)
        tmp.remove(toReposition)
    }
    return when {
        found.size>0 -> singleJobMove(found, newCost, found.lastIndex)
        0==actualPosition-1 -> ArrayList(reordered)
        else -> singleJobMove(reordered, newCost, actualPosition-1, false)
    }
}

fun twoSwap(toOrder:ArrayList<Job>, upperBound:Int, actualPosition:Int): ArrayList<Job> {
    var tmp = ArrayList(toOrder)
    val toReposition = toOrder[actualPosition]
    tmp.remove(toReposition)
    var newCost = upperBound
    var found: ArrayList<Job> = arrayListOf()
    for (i in actualPosition-1 downTo 0) {
        val replace = tmp[i]
        tmp.remove(replace)
        tmp.add(i, toReposition)
        tmp.add(actualPosition, replace)
        val actualCost = firstComeFirstServe(tmp)
        if (actualCost<newCost){
            found = tmp
            newCost = actualCost
        }
        tmp = ArrayList(toOrder)
        tmp.remove(toReposition)
    }
    return when {
        found.size>0 -> twoSwap(found, newCost, found.lastIndex)
        0==actualPosition-1 -> ArrayList(toOrder)
        else -> twoSwap(toOrder, upperBound, actualPosition-1)
    }
}

fun reorderLS(toOrder:ArrayList<Job>, upperBound:Int, actualPosition:Int, debug:Boolean = false, internalDebug:Boolean = false ): ArrayList<Job> {
    //val lateJobsCount = numberOfLateJobs(toOrder)
    var tmp = ArrayList(toOrder)
    val toReposition = toOrder[actualPosition]
    tmp.remove(toReposition)
    var newCost = upperBound
    var found: ArrayList<Job> = arrayListOf()
    for (i in actualPosition-1 downTo 0) {
        val replace = tmp[i]
        tmp.remove(replace)
        tmp.add(i, toReposition)
        tmp.add(actualPosition, replace)
        val internal = twoSwap(tmp, newCost, tmp.lastIndex)
        /*original version

        val actualCost = firstComeFirstServe(tmp)
        if (internalDebug) {
            tmp.forEachIndexed { ind, x -> println("$ind - durata ${x.tempoExec} - scade ${x.scadenza} - peso ${x.peso}") }
            firstComeFirstServe(tmp, true)
            println()
        }
        if (actualCost<newCost){
            found = tmp
            newCost = actualCost
            if (debug) {
                firstComeFirstServe(tmp, true)
                println()
                println("nuovo costo trovato: $newCost")
                println()
            }
        }
        tmp = ArrayList(toOrder)
        tmp.remove(toReposition)
         */
        val actualCost = firstComeFirstServe(internal)
        if (internalDebug) {
            internal.forEachIndexed { ind, x -> println("$ind - durata ${x.tempoExec} - scade ${x.scadenza} - peso ${x.peso}") }
            firstComeFirstServe(internal, true)
            println()
        }
        if (actualCost<newCost){
            found = internal
            newCost = actualCost
            if (debug) {
                firstComeFirstServe(internal, true)
                println()
                println("nuovo costo trovato: $newCost")
                println()
            }
        }
        tmp = ArrayList(toOrder)
        tmp.remove(toReposition)
    }
    return when {
        found.size>0 -> reorderLS(found, newCost, found.lastIndex)
        //toOrder.lastIndex-lateJobsCount==actualPosition-1 -> toOrder
        0==actualPosition-1 -> ArrayList(toOrder)
        else -> reorderLS(toOrder, upperBound, actualPosition-1)
    }
}

fun eddLateReorder(toOrder:ArrayList<Job>, debug:Boolean = false, internalDebug:Boolean = false ): ArrayList<Job> {
    val ordered = ArrayList(toOrder)
    ordered.sortWith { o1, o2 -> o1.tempoExec.compareTo(o2.tempoExec) }
    ordered.sortWith { o1, o2 -> o1.scadenza.compareTo(o2.scadenza) }
    var timeUsed = 0
    var tardiness = 0
    val notLate = ArrayList(ordered)
    val late: ArrayList<Job> = arrayListOf()
    ordered.forEach { job ->
        timeUsed += job.tempoExec
        if (timeUsed > job.scadenza) {
            tardiness += (timeUsed - job.scadenza)
            late.add(job)
            notLate.remove(job)
        }
    }
    late.sortWith { o1, o2 -> o1.tempoExec.compareTo(o2.tempoExec) }
    ordered.removeAll(ordered)
    ordered.addAll(notLate)
    ordered.addAll(late)
    if (internalDebug)
        ordered.forEachIndexed { i, x -> println("$i - durata ${x.tempoExec} - scade ${x.scadenza} - peso ${x.peso}") }
    tardiness=0
    timeUsed=0
    ordered.forEachIndexed { index, job ->
        timeUsed += job.tempoExec
        if (timeUsed > job.scadenza) {
            tardiness += (timeUsed - job.scadenza)
            late.add(job)
            notLate.remove(job)
            if (debug) {
                print("$index - durata ${job.tempoExec} - scaduto da ${timeUsed - job.scadenza} - scadenza ${job.scadenza} - usato $timeUsed")
                println(" - tardiness attuale $tardiness")
            }
        }
    }
    return ordered
}

fun firstComeFirstServe(jobs:ArrayList<Job>, debug:Boolean = false, internalDebug:Boolean = false): Int {
    var timeUsed = 0
    var tardiness = 0
    jobs.forEachIndexed { index, job ->
        timeUsed += job.tempoExec
        if (timeUsed > job.scadenza) {
            tardiness += (timeUsed - job.scadenza)*job.peso
            if (debug) {
                print("$index - durata ${job.tempoExec} - scaduto da ${timeUsed - job.scadenza} - scadenza ${job.scadenza} - usato $timeUsed")
                println(" - tardiness attuale $tardiness")
            }
        }
        if (internalDebug) {
            println("$index - durata ${job.tempoExec} - scaduto da ${timeUsed - job.scadenza} - scadenza ${job.scadenza} - usato $timeUsed")
        }
    }
    return tardiness
}

fun orderByShortestProcessingTime(toOrder:ArrayList<Job>, internalDebug:Boolean = false ): ArrayList<Job> {
    val ordered = ArrayList(toOrder)
    ordered.sortWith { o1, o2 -> o1.scadenza.compareTo(o2.scadenza) }
    ordered.sortWith { o1, o2 -> o1.tempoExec.compareTo(o2.tempoExec) }
    if (internalDebug)
        ordered.forEachIndexed { i, x -> println("$i - durata ${x.tempoExec} - scade ${x.scadenza} - peso ${x.peso}") }
    return ordered
}

fun orderByEarlierDueDate(toOrder:ArrayList<Job>, internalDebug:Boolean = false ): ArrayList<Job> {
    val ordered = ArrayList(toOrder)
    ordered.sortWith { o1, o2 -> o1.tempoExec.compareTo(o2.tempoExec) }
    ordered.sortWith { o1, o2 -> o1.scadenza.compareTo(o2.scadenza) }
    if (ordered[0].scadenza==0){
        val reorder1 = ArrayList(ordered)
        val reorder2: ArrayList<Job> = arrayListOf()
        ordered.forEach { job ->
            if (job.scadenza==0) {
                reorder1.remove(job)
                reorder2.add(job)
            }
        }
        reorder2.sortWith { o1, o2 -> o1.tempoExec.compareTo(o2.tempoExec) }
        ordered.removeAll(ordered)
        ordered.addAll(reorder2)
        ordered.addAll(reorder1)
    }
    if (internalDebug)
        ordered.forEachIndexed { i, x -> println("$i - durata ${x.tempoExec} - scade ${x.scadenza} - peso ${x.peso}") }
    return ordered
}

fun readFile(src:String, iterationNumber:Int): ArrayList<String>{
    val file = Files.lines(Paths.get(src))
    var line1: String? = null
    var line2: String? = null
    var line3: String? = null
    var line4: String? = null
    val list: ArrayList<String> = arrayListOf()
    try {
        file.forEach { x: String ->
            val parser = x.trim().replace(Regex("  +"), ",")
            when (iterationNumber) {
                40 -> {
                    line1 = if (line1.isNullOrBlank()) parser
                    else {
                        list.add("$line1,$parser")
                        null
                    }
                }
                100 -> {
                    when {
                        line1.isNullOrBlank() -> line1 = parser
                        line2.isNullOrBlank() -> line2 = parser
                        line3.isNullOrBlank() -> line3 = parser
                        line4.isNullOrBlank() -> line4 = parser
                        else -> {
                            list.add("$line1,$line2,$line3,$line4,$parser")
                            line1 = null
                            line2 = null
                            line3 = null
                            line4 = null
                        }
                    }
                }
            }
        }
        file.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

fun getIterationsFromFile(): ArrayList<ArrayList<Job>> {
    val list = readFile("src/main/resources/40", 40)
    val iterations: ArrayList<ArrayList<Job>> = arrayListOf()
    for (i in list.indices step 3){
        val tempi = list[i].split(",")
        val pesi = list[i+1].split(",")
        val scadenze = list[i+2].split(",")
        val jobs: ArrayList<Job> = arrayListOf()
        for (j in tempi.indices) {
            val job = Job(Integer.parseInt(tempi[j]), Integer.parseInt(pesi[j]), Integer.parseInt(scadenze[j]))
            jobs.add(job)
        }
        iterations.add(jobs)
    }
    return iterations
}